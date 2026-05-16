# VibeHub

A full-featured social media Android app for creators — share posts & reels, chat, discover music, go live, and connect with people nearby.

## Run & Operate

- `pnpm --filter @workspace/api-server run dev` — run the API server (port 5000)
- `pnpm run typecheck` — full typecheck across all packages
- `pnpm run build` — typecheck + build all packages

## Stack

- **Android**: Kotlin, Jetpack Compose + Material 3, Hilt, Room, Supabase-kt, CameraX, ExoPlayer Media3, Coil
- **Backend**: Supabase (PostgreSQL + Auth + Storage + Realtime + Edge Functions)
- **Architecture**: MVVM + Clean Architecture, offline-first with Room cache
- **API server**: Express 5, pnpm workspaces, TypeScript

## Where things live

- `android-vibehub/` — full Android Studio project
- `android-vibehub/app/src/main/java/com/vibehub/` — all Kotlin source files
- `android-vibehub/local.properties` — Supabase credentials (never commit)
- `supabase/migrations/001_initial_schema.sql` — all DB tables + RLS + triggers
- `supabase/functions/` — Edge Functions: songs, validate-email, thumbnail, contact-suggestions
- `supabase/config.toml` — local Supabase dev config

## Architecture decisions

- **Offline-first**: Room is the single source of truth; remote data syncs into Room asynchronously.
- **Credentials**: Supabase URL/key are read from `local.properties` via `buildConfigField` — never in source.
- **Music**: Songs are fetched from Jamendo (CC-licensed, free) via an Edge Function — never directly from Android.
- **Contact privacy**: Only SHA-256 hashes of phone numbers leave the device, never raw numbers.
- **Disposable email**: Rejected in the `validate-email` Edge Function using a known domain blocklist.
- **Device fingerprint**: Stable ANDROID_ID + package name SHA-256; stored in `device_sessions` for anomaly detection.
- **Biometric lock**: Implemented via AndroidX Biometric library; enables fingerprint/face lock for app entry.

## Product

- Email + OTP sign-up with disposable email rejection
- 3-step profile setup (avatar, username suggestions, phone + location)
- Home feed (images, carousels, reels, text posts) with likes, saves, comments, shares
- Stories with polls, quizzes, stickers, links
- Reels vertical scroller with ExoPlayer
- Post creation: media picker, video trim, music (Jamendo CC), location, @tagging, #hashtags, privacy/audience picker
- Inbox: end-to-end encrypted DMs, view-once media, voice notes, reactions, replies
- Audio & video calls
- Explore: search users/posts/hashtags, trending
- Notifications: likes, comments, mentions, follows
- Profile: highlights, analytics/insights, pinned post, creator level badge
- Settings: biometric lock, blocked accounts, download/storage, privacy

## User preferences

- No mocks anywhere — all data from real Supabase backend
- Real Supabase project: `aoeilzrcmbamyfhccxdp` at `https://aoeilzrcmbamyfhccxdp.supabase.co`
- Glassmorphism / neon UI: VibePink #E8356D, VibeOrange #F4722B, VibeGold #F9B234

## Gotchas

- `local.properties` is git-ignored; CI needs `SUPABASE_URL` / `SUPABASE_ANON_KEY` env vars injected.
- Run `supabase db push` to apply migrations to your Supabase project.
- Run `supabase functions deploy songs validate-email thumbnail contact-suggestions` to deploy edge functions.
- The `JAMENDO_CLIENT_ID` secret must be set in Supabase: `supabase secrets set JAMENDO_CLIENT_ID=<your_id>`.
- Biometric lock requires `FragmentActivity` (all Compose activities satisfy this).

## Pointers

- See the `pnpm-workspace` skill for workspace structure, TypeScript setup, and package details.
