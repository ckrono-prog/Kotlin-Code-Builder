# VibeHub — Android Studio Setup Guide

## Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK (API 26+ min, API 35 target)

---

## Step 1 — Open in Android Studio

1. Unzip the project
2. **File → Open** → select the `android-vibehub/` folder
3. Wait for Gradle sync to complete (first sync downloads ~500 MB)

---

## Step 2 — Configure Supabase

1. Create a project at [supabase.com](https://supabase.com)
2. Go to **Project Settings → API**
3. Open `app/build.gradle.kts` and replace:
   ```kotlin
   buildConfigField("String", "SUPABASE_URL",     "\"https://YOUR_PROJECT.supabase.co\"")
   buildConfigField("String", "SUPABASE_ANON_KEY", "\"YOUR_ANON_KEY\"")
   ```
4. Resync Gradle

---

## Step 3 — Set up Supabase Database

Run these SQL statements in your Supabase SQL editor:

```sql
-- Users (extends Supabase auth.users)
CREATE TABLE public.users (
  id              UUID REFERENCES auth.users PRIMARY KEY,
  username        TEXT UNIQUE NOT NULL,
  display_name    TEXT NOT NULL DEFAULT '',
  avatar_url      TEXT DEFAULT '',
  cover_url       TEXT DEFAULT '',
  bio             TEXT DEFAULT '',
  location        TEXT DEFAULT '',
  website         TEXT DEFAULT '',
  pronouns        TEXT DEFAULT '',
  is_verified     BOOLEAN DEFAULT FALSE,
  is_premium      BOOLEAN DEFAULT FALSE,
  is_private      BOOLEAN DEFAULT FALSE,
  followers_count INT DEFAULT 0,
  following_count INT DEFAULT 0,
  posts_count     INT DEFAULT 0,
  creator_level   INT DEFAULT 0,
  total_earnings  DOUBLE PRECISION DEFAULT 0,
  total_likes     BIGINT DEFAULT 0,
  total_views     BIGINT DEFAULT 0,
  joined_at       TIMESTAMPTZ DEFAULT NOW()
);

-- Posts
CREATE TABLE public.posts (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  author_id      UUID REFERENCES public.users NOT NULL,
  caption        TEXT DEFAULT '',
  media_urls     TEXT[] DEFAULT '{}',
  media_type     TEXT DEFAULT 'IMAGE',
  likes_count    INT DEFAULT 0,
  comments_count INT DEFAULT 0,
  saves_count    INT DEFAULT 0,
  shares_count   INT DEFAULT 0,
  views_count    BIGINT DEFAULT 0,
  hashtags       TEXT[] DEFAULT '{}',
  mentions       TEXT[] DEFAULT '{}',
  location       TEXT DEFAULT '',
  visibility     TEXT DEFAULT 'PUBLIC',
  is_pinned      BOOLEAN DEFAULT FALSE,
  aspect_ratio   FLOAT DEFAULT 1.0,
  created_at     TIMESTAMPTZ DEFAULT NOW()
);

-- Stories (auto-expire after 24h)
CREATE TABLE public.stories (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  author_id   UUID REFERENCES public.users NOT NULL,
  media_url   TEXT NOT NULL,
  media_type  TEXT DEFAULT 'IMAGE',
  link        TEXT,
  views_count INT DEFAULT 0,
  expires_at  TIMESTAMPTZ DEFAULT NOW() + INTERVAL '24 hours',
  created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- Story views
CREATE TABLE public.story_views (
  story_id   UUID REFERENCES public.stories,
  viewer_id  UUID REFERENCES public.users,
  viewed_at  TIMESTAMPTZ DEFAULT NOW(),
  PRIMARY KEY (story_id, viewer_id)
);

-- Follows
CREATE TABLE public.follows (
  follower_id  UUID REFERENCES public.users,
  following_id UUID REFERENCES public.users,
  created_at   TIMESTAMPTZ DEFAULT NOW(),
  PRIMARY KEY (follower_id, following_id)
);

-- Likes
CREATE TABLE public.likes (
  user_id    UUID REFERENCES public.users,
  post_id    UUID REFERENCES public.posts,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  PRIMARY KEY (user_id, post_id)
);

-- Saves
CREATE TABLE public.saves (
  user_id    UUID REFERENCES public.users,
  post_id    UUID REFERENCES public.posts,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  PRIMARY KEY (user_id, post_id)
);

-- Comments
CREATE TABLE public.comments (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  post_id     UUID REFERENCES public.posts NOT NULL,
  author_id   UUID REFERENCES public.users NOT NULL,
  text        TEXT NOT NULL,
  likes_count INT DEFAULT 0,
  parent_id   UUID REFERENCES public.comments,
  created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- Conversations
CREATE TABLE public.conversations (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  type             TEXT DEFAULT 'DIRECT',
  participant_ids  UUID[] NOT NULL,
  group_name       TEXT,
  group_avatar_url TEXT,
  is_encrypted     BOOLEAN DEFAULT TRUE,
  created_at       TIMESTAMPTZ DEFAULT NOW()
);

-- Messages
CREATE TABLE public.messages (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  conversation_id     UUID REFERENCES public.conversations NOT NULL,
  sender_id           UUID REFERENCES public.users NOT NULL,
  text                TEXT DEFAULT '',
  media_url           TEXT,
  media_type          TEXT,
  is_read             BOOLEAN DEFAULT FALSE,
  is_deleted          BOOLEAN DEFAULT FALSE,
  is_edited           BOOLEAN DEFAULT FALSE,
  reply_to_message_id UUID REFERENCES public.messages,
  expires_at          TIMESTAMPTZ,
  created_at          TIMESTAMPTZ DEFAULT NOW()
);

-- Notifications
CREATE TABLE public.notifications (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    UUID REFERENCES public.users NOT NULL,
  type       TEXT NOT NULL,
  actor_id   UUID REFERENCES public.users NOT NULL,
  post_id    UUID REFERENCES public.posts,
  comment_id UUID REFERENCES public.comments,
  text       TEXT DEFAULT '',
  is_read    BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Enable RLS on all tables
ALTER TABLE public.users         ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.posts         ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.stories       ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.follows       ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.likes         ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.saves         ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.messages      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

-- Basic RLS policies (customize as needed)
CREATE POLICY "Public read users"   ON public.users   FOR SELECT USING (TRUE);
CREATE POLICY "Public read posts"   ON public.posts   FOR SELECT USING (visibility = 'PUBLIC');
CREATE POLICY "Auth users read all" ON public.stories FOR SELECT USING (auth.role() = 'authenticated');
```

---

## Step 4 — Enable Supabase Auth Providers

In your Supabase Dashboard → **Authentication → Providers**:
- Enable **Email**
- Enable **Google** (add your OAuth credentials)
- Enable **Phone** (for OTP)

---

## Step 5 — Run the App

- Connect a device or start an emulator (API 26+)
- Click **Run** (▶) in Android Studio

---

## Architecture Overview

```
com.vibehub/
├── data/
│   ├── local/          ← Room DB (offline cache, never touches UI)
│   │   ├── dao/        ← DAOs for CRUD
│   │   ├── database/   ← VibeHubDatabase
│   │   └── entities/   ← Room entity classes
│   ├── remote/         ← Supabase DTOs & client config
│   └── repository/     ← Repositories (bridge local ↔ remote)
│
├── domain/
│   └── model/          ← Pure Kotlin domain models (UI-safe)
│
├── di/                 ← Hilt dependency injection modules
│
└── ui/
    ├── components/     ← Reusable composables
    ├── navigation/     ← NavGraph
    ├── screens/        ← All screens
    │   ├── auth/       ← Splash, Login, Register
    │   ├── home/       ← Feed, Main shell
    │   ├── profile/    ← Profile page
    │   ├── messages/   ← Inbox + Chat
    │   ├── explore/    ← Explore + Search
    │   ├── reels/      ← Vertical reels feed
    │   ├── notifications/
    │   └── creator/    ← Creator dashboard
    ├── theme/          ← Colors, Typography, Theme
    └── viewmodel/      ← ViewModels for each screen
```

**Key design decision:** The backend (Supabase) never touches the UI directly.
Flow is always: `Supabase → Repository → Room DB → ViewModel → Composable`
