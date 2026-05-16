/**
 * VibeHub Edge Function — Song Search
 * Searches Jamendo (CC-licensed music, free API) and caches results
 * in the song_cache table so repeated queries are instant.
 *
 * Deploy: supabase functions deploy songs
 * Env vars needed (supabase secrets set):
 *   JAMENDO_CLIENT_ID — from https://developer.jamendo.com
 */
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const JAMENDO_API = "https://api.jamendo.com/v3.0";
const CACHE_TTL_HOURS = 6;

serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", {
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
      },
    });
  }

  try {
    const url = new URL(req.url);
    const query = (url.searchParams.get("q") || "").trim();
    const limit = Math.min(parseInt(url.searchParams.get("limit") || "25"), 50);

    if (query.length < 2) {
      return json({ tracks: [] });
    }

    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
    );

    // ── Try cache first ────────────────────────────────────────────────────
    const cutoff = new Date(Date.now() - CACHE_TTL_HOURS * 3600_000).toISOString();
    const { data: cached } = await supabase
      .from("song_cache")
      .select("*")
      .eq("query", query.toLowerCase())
      .gt("cached_at", cutoff)
      .limit(limit);

    if (cached && cached.length > 0) {
      return json({ tracks: cached, source: "cache" });
    }

    // ── Fetch from Jamendo ─────────────────────────────────────────────────
    const clientId = Deno.env.get("JAMENDO_CLIENT_ID") || "b6747d04"; // public demo key
    const jamendoUrl = new URL(`${JAMENDO_API}/tracks/`);
    jamendoUrl.searchParams.set("client_id", clientId);
    jamendoUrl.searchParams.set("format", "json");
    jamendoUrl.searchParams.set("limit", limit.toString());
    jamendoUrl.searchParams.set("search", query);
    jamendoUrl.searchParams.set("audioformat", "mp32");
    jamendoUrl.searchParams.set("include", "musicinfo");
    jamendoUrl.searchParams.set("imagesize", "200");

    const resp = await fetch(jamendoUrl.toString());
    if (!resp.ok) throw new Error(`Jamendo error: ${resp.status}`);

    const data = await resp.json();
    const tracks = (data.results || []).map((t: Record<string, unknown>) => ({
      id: String(t.id),
      title: String(t.name || ""),
      artist: String(t.artist_name || ""),
      album: String(t.album_name || ""),
      cover_url: String(t.image || ""),
      preview_url: String(t.audio || ""),
      duration_sec: Number(t.duration || 0),
      license: String(t.license_ccurl || "cc"),
      query: query.toLowerCase(),
    }));

    // Cache results
    if (tracks.length > 0) {
      await supabase.from("song_cache").upsert(tracks, { onConflict: "id" });
    }

    return json({ tracks, source: "jamendo" });
  } catch (err) {
    console.error("songs edge fn error:", err);
    return json({ error: String(err), tracks: [] }, 500);
  }
});

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json",
      "Access-Control-Allow-Origin": "*",
    },
  });
}
