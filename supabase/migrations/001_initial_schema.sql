-- ============================================================
-- VibeHub — Full Schema Migration
-- Run: supabase db push
-- ============================================================

-- Extensions
create extension if not exists "uuid-ossp";
create extension if not exists "pg_trgm";
create extension if not exists "postgis";   -- for location-based queries

-- ============================================================
-- PROFILES (extends auth.users)
-- ============================================================
create table if not exists public.profiles (
  id              uuid primary key references auth.users(id) on delete cascade,
  username        text unique not null,
  display_name    text not null default '',
  avatar_url      text not null default '',
  cover_url       text not null default '',
  bio             text not null default '',
  website         text not null default '',
  location        text not null default '',
  location_lat    double precision,
  location_lng    double precision,
  country_code    text not null default '',
  phone           text,
  pronouns        text not null default '',
  gender          text not null default '',
  birthday        date,
  is_verified     boolean not null default false,
  is_premium      boolean not null default false,
  is_private      boolean not null default false,
  followers_count int not null default 0,
  following_count int not null default 0,
  posts_count     int not null default 0,
  total_likes     bigint not null default 0,
  total_views     bigint not null default 0,
  creator_level   int not null default 0,
  onboarding_done boolean not null default false,
  device_id       text,
  last_ip         text,
  created_at      timestamptz not null default now(),
  updated_at      timestamptz not null default now()
);

create index if not exists profiles_username_trgm on public.profiles using gin (username gin_trgm_ops);
create index if not exists profiles_display_name_trgm on public.profiles using gin (display_name gin_trgm_ops);
create index if not exists profiles_location_gist on public.profiles using gist (
  st_makepoint(location_lng, location_lat) gist_geometry_ops_2d
) where location_lat is not null and location_lng is not null;

-- Row Level Security
alter table public.profiles enable row level security;
create policy "profiles_select" on public.profiles for select using (true);
create policy "profiles_update" on public.profiles for update using (auth.uid() = id);

-- ============================================================
-- POSTS
-- ============================================================
create table if not exists public.posts (
  id               uuid primary key default uuid_generate_v4(),
  author_id        uuid not null references public.profiles(id) on delete cascade,
  caption          text not null default '',
  media_urls       text[] not null default '{}',
  media_type       text not null default 'image',
  thumbnail_url    text,
  aspect_ratio     float not null default 1.0,
  duration_sec     int,
  hashtags         text[] not null default '{}',
  mentions         text[] not null default '{}',
  location         text not null default '',
  location_lat     double precision,
  location_lng     double precision,
  music_track      jsonb,
  likes_count      int not null default 0,
  comments_count   int not null default 0,
  saves_count      int not null default 0,
  shares_count     int not null default 0,
  views_count      bigint not null default 0,
  visibility       text not null default 'public',
  comment_perm     text not null default 'everyone',
  is_pinned        boolean not null default false,
  is_archived      boolean not null default false,
  is_deleted       boolean not null default false,
  created_at       timestamptz not null default now(),
  updated_at       timestamptz not null default now()
);

create index if not exists posts_author_id on public.posts(author_id);
create index if not exists posts_created_at on public.posts(created_at desc);
create index if not exists posts_hashtags on public.posts using gin(hashtags);
create index if not exists posts_mentions on public.posts using gin(mentions);

alter table public.posts enable row level security;
create policy "posts_select" on public.posts for select using (
  is_deleted = false and (
    visibility = 'public' or
    author_id = auth.uid() or
    (visibility = 'followers' and exists (
      select 1 from public.follows where follower_id = auth.uid() and following_id = author_id
    ))
  )
);
create policy "posts_insert" on public.posts for insert with check (auth.uid() = author_id);
create policy "posts_update" on public.posts for update using (auth.uid() = author_id);
create policy "posts_delete" on public.posts for delete using (auth.uid() = author_id);

-- ============================================================
-- FOLLOWS
-- ============================================================
create table if not exists public.follows (
  id           uuid primary key default uuid_generate_v4(),
  follower_id  uuid not null references public.profiles(id) on delete cascade,
  following_id uuid not null references public.profiles(id) on delete cascade,
  created_at   timestamptz not null default now(),
  unique(follower_id, following_id)
);

create index if not exists follows_follower on public.follows(follower_id);
create index if not exists follows_following on public.follows(following_id);

alter table public.follows enable row level security;
create policy "follows_select" on public.follows for select using (true);
create policy "follows_insert" on public.follows for insert with check (auth.uid() = follower_id);
create policy "follows_delete" on public.follows for delete using (auth.uid() = follower_id);

-- Auto-update follower/following counts
create or replace function update_follow_counts()
returns trigger language plpgsql security definer as $$
begin
  if tg_op = 'INSERT' then
    update public.profiles set followers_count = followers_count + 1 where id = new.following_id;
    update public.profiles set following_count = following_count + 1 where id = new.follower_id;
  elsif tg_op = 'DELETE' then
    update public.profiles set followers_count = greatest(0, followers_count - 1) where id = old.following_id;
    update public.profiles set following_count = greatest(0, following_count - 1) where id = old.follower_id;
  end if;
  return null;
end;
$$;
create trigger follow_count_trigger after insert or delete on public.follows
  for each row execute function update_follow_counts();

-- ============================================================
-- BLOCKS
-- ============================================================
create table if not exists public.blocks (
  id         uuid primary key default uuid_generate_v4(),
  blocker_id uuid not null references public.profiles(id) on delete cascade,
  blocked_id uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  unique(blocker_id, blocked_id)
);

alter table public.blocks enable row level security;
create policy "blocks_select" on public.blocks for select using (auth.uid() = blocker_id);
create policy "blocks_insert" on public.blocks for insert with check (auth.uid() = blocker_id);
create policy "blocks_delete" on public.blocks for delete using (auth.uid() = blocker_id);

-- ============================================================
-- COMMENTS
-- ============================================================
create table if not exists public.comments (
  id          uuid primary key default uuid_generate_v4(),
  post_id     uuid not null references public.posts(id) on delete cascade,
  author_id   uuid not null references public.profiles(id) on delete cascade,
  parent_id   uuid references public.comments(id) on delete cascade,
  text        text not null,
  likes_count int not null default 0,
  is_deleted  boolean not null default false,
  created_at  timestamptz not null default now()
);

create index if not exists comments_post_id on public.comments(post_id);
create index if not exists comments_parent_id on public.comments(parent_id);

alter table public.comments enable row level security;
create policy "comments_select" on public.comments for select using (is_deleted = false);
create policy "comments_insert" on public.comments for insert with check (auth.uid() = author_id);
create policy "comments_update" on public.comments for update using (auth.uid() = author_id);
create policy "comments_delete" on public.comments for delete using (auth.uid() = author_id);

-- Auto-update post comment count
create or replace function update_comment_count()
returns trigger language plpgsql security definer as $$
begin
  if tg_op = 'INSERT' then
    update public.posts set comments_count = comments_count + 1 where id = new.post_id;
  elsif tg_op = 'DELETE' then
    update public.posts set comments_count = greatest(0, comments_count - 1) where id = old.post_id;
  end if;
  return null;
end;
$$;
create trigger comment_count_trigger after insert or delete on public.comments
  for each row execute function update_comment_count();

-- ============================================================
-- COMMENT LIKES
-- ============================================================
create table if not exists public.comment_likes (
  id         uuid primary key default uuid_generate_v4(),
  comment_id uuid not null references public.comments(id) on delete cascade,
  user_id    uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  unique(comment_id, user_id)
);
alter table public.comment_likes enable row level security;
create policy "comment_likes_all" on public.comment_likes for all using (auth.uid() = user_id);

-- ============================================================
-- POST LIKES
-- ============================================================
create table if not exists public.post_likes (
  id         uuid primary key default uuid_generate_v4(),
  post_id    uuid not null references public.posts(id) on delete cascade,
  user_id    uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  unique(post_id, user_id)
);
alter table public.post_likes enable row level security;
create policy "post_likes_all" on public.post_likes for all using (auth.uid() = user_id);

-- ============================================================
-- POST SAVES
-- ============================================================
create table if not exists public.post_saves (
  id         uuid primary key default uuid_generate_v4(),
  post_id    uuid not null references public.posts(id) on delete cascade,
  user_id    uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  unique(post_id, user_id)
);
alter table public.post_saves enable row level security;
create policy "post_saves_all" on public.post_saves for all using (auth.uid() = user_id);

-- ============================================================
-- POST VIEWS
-- ============================================================
create table if not exists public.post_views (
  id         uuid primary key default uuid_generate_v4(),
  post_id    uuid not null references public.posts(id) on delete cascade,
  viewer_id  uuid references public.profiles(id) on delete set null,
  viewed_at  timestamptz not null default now()
);
create index if not exists post_views_post_id on public.post_views(post_id);
alter table public.post_views enable row level security;
create policy "post_views_insert" on public.post_views for insert with check (true);
create policy "post_views_select" on public.post_views for select using (
  exists (select 1 from public.posts where id = post_id and author_id = auth.uid())
);

-- ============================================================
-- STORIES
-- ============================================================
create table if not exists public.stories (
  id          uuid primary key default uuid_generate_v4(),
  author_id   uuid not null references public.profiles(id) on delete cascade,
  media_url   text not null,
  media_type  text not null default 'image',
  thumbnail_url text,
  music_track jsonb,
  stickers    jsonb not null default '[]',
  poll        jsonb,
  quiz        jsonb,
  link        text,
  views_count int not null default 0,
  expires_at  timestamptz not null default (now() + interval '24 hours'),
  created_at  timestamptz not null default now()
);

create index if not exists stories_author_id on public.stories(author_id);
create index if not exists stories_expires_at on public.stories(expires_at);

alter table public.stories enable row level security;
create policy "stories_select" on public.stories for select using (expires_at > now());
create policy "stories_insert" on public.stories for insert with check (auth.uid() = author_id);
create policy "stories_delete" on public.stories for delete using (auth.uid() = author_id);

-- ============================================================
-- STORY VIEWS
-- ============================================================
create table if not exists public.story_views (
  id         uuid primary key default uuid_generate_v4(),
  story_id   uuid not null references public.stories(id) on delete cascade,
  viewer_id  uuid references public.profiles(id) on delete set null,
  viewed_at  timestamptz not null default now(),
  unique(story_id, viewer_id)
);
alter table public.story_views enable row level security;
create policy "story_views_all" on public.story_views for all using (true);

-- ============================================================
-- CONVERSATIONS
-- ============================================================
create table if not exists public.conversations (
  id              uuid primary key default uuid_generate_v4(),
  type            text not null default 'direct',
  group_name      text,
  group_avatar    text,
  theme           text not null default 'default',
  is_encrypted    boolean not null default true,
  created_by      uuid references public.profiles(id),
  created_at      timestamptz not null default now()
);
alter table public.conversations enable row level security;
create policy "conversations_select" on public.conversations for select using (
  exists (select 1 from public.conversation_members where conversation_id = id and user_id = auth.uid())
);

-- ============================================================
-- CONVERSATION MEMBERS
-- ============================================================
create table if not exists public.conversation_members (
  id              uuid primary key default uuid_generate_v4(),
  conversation_id uuid not null references public.conversations(id) on delete cascade,
  user_id         uuid not null references public.profiles(id) on delete cascade,
  nickname        text,
  is_muted        boolean not null default false,
  is_archived     boolean not null default false,
  unread_count    int not null default 0,
  joined_at       timestamptz not null default now(),
  unique(conversation_id, user_id)
);
alter table public.conversation_members enable row level security;
create policy "members_select" on public.conversation_members for select using (auth.uid() = user_id);

-- ============================================================
-- MESSAGES
-- ============================================================
create table if not exists public.messages (
  id              uuid primary key default uuid_generate_v4(),
  conversation_id uuid not null references public.conversations(id) on delete cascade,
  sender_id       uuid not null references public.profiles(id) on delete cascade,
  text            text not null default '',
  media_url       text,
  media_type      text,
  is_view_once    boolean not null default false,
  view_once_seen  boolean not null default false,
  reply_to_id     uuid references public.messages(id),
  reactions       jsonb not null default '{}',
  is_deleted      boolean not null default false,
  is_edited       boolean not null default false,
  expires_at      timestamptz,
  created_at      timestamptz not null default now()
);

create index if not exists messages_conversation_id on public.messages(conversation_id, created_at desc);

alter table public.messages enable row level security;
create policy "messages_select" on public.messages for select using (
  exists (select 1 from public.conversation_members where conversation_id = messages.conversation_id and user_id = auth.uid())
);
create policy "messages_insert" on public.messages for insert with check (
  auth.uid() = sender_id and
  exists (select 1 from public.conversation_members where conversation_id = messages.conversation_id and user_id = auth.uid())
);
create policy "messages_update" on public.messages for update using (auth.uid() = sender_id);

-- ============================================================
-- NOTIFICATIONS
-- ============================================================
create table if not exists public.notifications (
  id          uuid primary key default uuid_generate_v4(),
  user_id     uuid not null references public.profiles(id) on delete cascade,
  actor_id    uuid references public.profiles(id) on delete set null,
  type        text not null,
  post_id     uuid references public.posts(id) on delete set null,
  comment_id  uuid references public.comments(id) on delete set null,
  story_id    uuid references public.stories(id) on delete set null,
  text        text not null default '',
  is_read     boolean not null default false,
  created_at  timestamptz not null default now()
);

create index if not exists notifications_user_id on public.notifications(user_id, created_at desc);

alter table public.notifications enable row level security;
create policy "notifications_select" on public.notifications for select using (auth.uid() = user_id);
create policy "notifications_update" on public.notifications for update using (auth.uid() = user_id);

-- ============================================================
-- SONG CACHE (Jamendo tracks served via Edge Function)
-- ============================================================
create table if not exists public.song_cache (
  id          text primary key,
  title       text not null,
  artist      text not null,
  album       text not null default '',
  cover_url   text not null default '',
  preview_url text not null,
  duration_sec int not null default 0,
  license     text not null default '',
  query       text not null,
  cached_at   timestamptz not null default now()
);
create index if not exists song_cache_query on public.song_cache(query);
create index if not exists song_cache_cached_at on public.song_cache(cached_at);

alter table public.song_cache enable row level security;
create policy "song_cache_select" on public.song_cache for select using (true);
create policy "song_cache_insert" on public.song_cache for insert with check (true);

-- ============================================================
-- DEVICE SESSIONS (device fingerprint + IP tracking)
-- ============================================================
create table if not exists public.device_sessions (
  id          uuid primary key default uuid_generate_v4(),
  user_id     uuid references public.profiles(id) on delete cascade,
  device_id   text not null,
  device_name text not null default '',
  platform    text not null default 'android',
  ip_address  text,
  user_agent  text,
  is_active   boolean not null default true,
  last_seen   timestamptz not null default now(),
  created_at  timestamptz not null default now()
);
create index if not exists device_sessions_user on public.device_sessions(user_id);
alter table public.device_sessions enable row level security;
create policy "device_sessions_own" on public.device_sessions for all using (auth.uid() = user_id);

-- ============================================================
-- CONTACT LOOKUP (hashed phone numbers for contact import)
-- ============================================================
create table if not exists public.contact_hashes (
  id          uuid primary key default uuid_generate_v4(),
  user_id     uuid not null references public.profiles(id) on delete cascade,
  phone_hash  text not null,  -- SHA-256 of E.164 phone
  created_at  timestamptz not null default now(),
  unique(user_id, phone_hash)
);
alter table public.contact_hashes enable row level security;
create policy "contact_hashes_own" on public.contact_hashes for all using (auth.uid() = user_id);

-- ============================================================
-- HIGHLIGHTS
-- ============================================================
create table if not exists public.highlights (
  id         uuid primary key default uuid_generate_v4(),
  user_id    uuid not null references public.profiles(id) on delete cascade,
  title      text not null,
  cover_url  text not null default '',
  story_ids  uuid[] not null default '{}',
  created_at timestamptz not null default now()
);
alter table public.highlights enable row level security;
create policy "highlights_select" on public.highlights for select using (true);
create policy "highlights_manage" on public.highlights for all using (auth.uid() = user_id);

-- ============================================================
-- USERNAME SUGGESTIONS helper function
-- ============================================================
create or replace function generate_username_suggestions(email_local text)
returns text[] language plpgsql as $$
declare
  base text := lower(regexp_replace(email_local, '[^a-z0-9]', '', 'g'));
  suggestions text[] := '{}';
  candidate text;
  i int;
begin
  base := substring(base, 1, 20);
  if length(base) < 3 then base := base || 'user'; end if;

  -- Direct base
  if not exists (select 1 from public.profiles where username = base) then
    suggestions := suggestions || base;
  end if;

  -- Base + random numbers
  for i in 1..10 loop
    candidate := base || floor(random() * 9999)::text;
    if not exists (select 1 from public.profiles where username = candidate) then
      suggestions := suggestions || candidate;
      if array_length(suggestions, 1) >= 5 then exit; end if;
    end if;
  end loop;

  return suggestions;
end;
$$;

-- ============================================================
-- Updated_at trigger (shared)
-- ============================================================
create or replace function set_updated_at()
returns trigger language plpgsql as $$
begin new.updated_at := now(); return new; end;
$$;
create trigger profiles_updated_at before update on public.profiles
  for each row execute function set_updated_at();
create trigger posts_updated_at before update on public.posts
  for each row execute function set_updated_at();

-- Enable Realtime for live feed
alter publication supabase_realtime add table public.messages;
alter publication supabase_realtime add table public.notifications;
alter publication supabase_realtime add table public.follows;
