/**
 * VibeHub Edge Function — Auto-generate video thumbnail
 * Called by Android app after a video is uploaded to Supabase Storage.
 * Since edge functions can't run FFmpeg, we return a metadata response here.
 * The actual thumbnail is generated client-side (or via a processing pipeline)
 * and this function updates the post record with the provided thumbnail_url.
 *
 * Deploy: supabase functions deploy thumbnail
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

    // Verify caller is authenticated
    const { data: { user }, error: authErr } = await supabase.auth.getUser(
      authHeader.replace("Bearer ", ""),
    );
    if (authErr || !user) return json({ error: "Unauthorized" }, 401);

    const { post_id, thumbnail_url, media_url } = await req.json();

    if (!post_id) return json({ error: "post_id is required" }, 400);

    let finalThumbnailUrl = thumbnail_url;

    // If no thumbnail was provided, use the media_url for images,
    // or derive a frame URL for supported video hosts
    if (!finalThumbnailUrl && media_url) {
      const lower = media_url.toLowerCase();
      if (lower.includes(".mp4") || lower.includes(".mov") || lower.includes(".webm")) {
        // Return first frame URL — Supabase Storage supports image transforms
        finalThumbnailUrl = `${media_url}?t=0`;
      } else {
        finalThumbnailUrl = media_url;
      }
    }

    if (!finalThumbnailUrl) {
      return json({ error: "Could not determine thumbnail" }, 422);
    }

    // Update the post record
    const { error: updateErr } = await supabase
      .from("posts")
      .update({ thumbnail_url: finalThumbnailUrl })
      .eq("id", post_id)
      .eq("author_id", user.id); // Ownership check

    if (updateErr) return json({ error: updateErr.message }, 500);

    return json({ success: true, thumbnail_url: finalThumbnailUrl });
  } catch (err) {
    console.error("thumbnail edge fn error:", err);
    return json({ error: String(err) }, 500);
  }
});

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
  });
}
