/**
 * VibeHub Edge Function — Email Validation
 * Rejects disposable/temporary email addresses.
 * Also records device fingerprint + IP on signup.
 *
 * Deploy: supabase functions deploy validate-email
 */
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

// Known disposable email domains — expand as needed
const DISPOSABLE_DOMAINS = new Set([
  "mailinator.com", "guerrillamail.com", "tempmail.com", "throwam.com",
  "10minutemail.com", "yopmail.com", "sharklasers.com", "guerrillamailblock.com",
  "grr.la", "guerrillamail.info", "guerrillamail.biz", "guerrillamail.de",
  "guerrillamail.net", "guerrillamail.org", "spam4.me", "trashmail.at",
  "trashmail.io", "trashmail.me", "trashmail.net", "dispostable.com",
  "mailnull.com", "spamgourmet.com", "spamgourmet.net", "spamgourmet.org",
  "maildrop.cc", "spamherelots.com", "spamhereplease.com", "spamthisplease.com",
  "fakeinbox.com", "mailnew.com", "mailscrap.com", "mailzilla.com",
  "inboxstore.me", "throwam.com", "wegwerfmail.de", "wegwerfmail.net",
  "wegwerfmail.org", "discard.email", "discardmail.com", "discardmail.de",
  "mailtemp.info", "33mail.com", "anonymail.dk", "binkmail.com",
  "bobmail.info", "chammy.info", "devnullmail.com", "letthemeatspam.com",
  "mytempemail.com", "tempe-mail.com", "tempm.com", "spamspot.com",
  "tempinbox.co.uk", "tempinbox.com", "smellfear.com", "thrma.com",
  "throwam.com", "trbvm.com", "uggsrock.com", "junk1.tk",
]);

serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", {
      headers: { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "*" },
    });
  }

  try {
    const { email, device_id, device_name, ip_address } = await req.json();

    if (!email || typeof email !== "string") {
      return json({ valid: false, reason: "Email is required" });
    }

    const normalized = email.toLowerCase().trim();
    const atIdx = normalized.lastIndexOf("@");
    if (atIdx < 1) return json({ valid: false, reason: "Invalid email format" });

    const domain = normalized.slice(atIdx + 1);

    // Check disposable
    if (DISPOSABLE_DOMAINS.has(domain)) {
      return json({ valid: false, reason: "Disposable email addresses are not allowed" });
    }

    // Basic format check
    const emailRegex = /^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$/;
    if (!emailRegex.test(normalized)) {
      return json({ valid: false, reason: "Invalid email format" });
    }

    // Get real IP from request headers (Supabase Edge Runtime)
    const clientIp = req.headers.get("x-forwarded-for")?.split(",")[0]?.trim() ||
      ip_address || "unknown";

    return json({
      valid: true,
      normalized_email: normalized,
      domain,
      client_ip: clientIp,
      device_id: device_id || null,
    });
  } catch (err) {
    return json({ valid: false, reason: "Validation error", error: String(err) }, 500);
  }
});

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
  });
}
