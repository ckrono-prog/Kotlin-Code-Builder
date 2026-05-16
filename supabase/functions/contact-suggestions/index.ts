/**
 * VibeHub Edge Function — Contact Suggestions
 * Accepts a list of hashed phone numbers from the device contacts,
 * returns matching VibeHub users and location-based suggestions.
 *
 * Deploy: supabase functions deploy contact-suggestions
 */
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", {
      headers: { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "*" },
    });
  }

  try {
    const authHeader = req.headers.get("authorization") || "";
    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
    );

    // Get current user from JWT
    const { data: { user }, error: authErr } = await supabase.auth.getUser(
      authHeader.replace("Bearer ", ""),
    );
    if (authErr || !user) return json({ error: "Unauthorized" }, 401);

    const { phone_hashes = [], lat, lng, radius_km = 50 } = await req.json();

    let contactSuggestions: unknown[] = [];

    // ── Contact matches ────────────────────────────────────────────────────
    if (phone_hashes.length > 0) {
      const { data: matches } = await supabase
        .from("contact_hashes")
        .select("user_id, profiles!inner(id, username, display_name, avatar_url, followers_count, is_verified)")
        .in("phone_hash", phone_hashes.slice(0, 500))
        .neq("user_id", user.id)
        .limit(50);

      contactSuggestions = (matches || []).map((m: Record<string, unknown>) => ({
        ...(m.profiles as object),
        suggestion_reason: "in_contacts",
      }));
    }

    // ── Location-based suggestions ─────────────────────────────────────────
    let locationSuggestions: unknown[] = [];
    if (lat && lng) {
      // Users within radius_km who have public profiles and aren't already followed
      const { data: nearby } = await supabase.rpc("users_near_location", {
        p_lat: lat,
        p_lng: lng,
        p_radius_km: radius_km,
        p_user_id: user.id,
        p_limit: 20,
      });
      locationSuggestions = (nearby || []).map((u: Record<string, unknown>) => ({
        ...u,
        suggestion_reason: "near_you",
      }));
    }

    // ── Mutual follow suggestions ──────────────────────────────────────────
    const { data: mutuals } = await supabase.rpc("mutual_follow_suggestions", {
      p_user_id: user.id,
      p_limit: 20,
    });
    const mutualSuggestions = (mutuals || []).map((u: Record<string, unknown>) => ({
      ...u,
      suggestion_reason: "mutual_follows",
    }));

    return json({
      contact_suggestions: contactSuggestions,
      location_suggestions: locationSuggestions,
      mutual_suggestions: mutualSuggestions,
    });
  } catch (err) {
    return json({ error: String(err) }, 500);
  }
});

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
  });
}
