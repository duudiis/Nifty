-- 001_init.sql — initial shared PostgreSQL schema for Nifty
-- Applied to the `nifty` database (owner: nifty) on the shared postgres server.
-- Apply with: psql -1 -v ON_ERROR_STOP=1 -U nifty -d nifty -f 001_init.sql

-- ============================== identity & config ==============================

CREATE TABLE users (
  id            BIGINT PRIMARY KEY,           -- Discord snowflake
  username      TEXT,
  display_name  TEXT,
  avatar_url    TEXT,
  first_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_seen_at  TIMESTAMPTZ
);

-- Bot instances (e.g. Nifty, Nifty 2). Each row is one bot account; bots
-- upsert themselves at startup. Players, queues and guild settings are
-- per-bot; users, tracks, analytics and the library are shared.
CREATE TABLE bots (
  id   BIGINT PRIMARY KEY,                    -- the bot account's Discord user id
  name TEXT
);

CREATE TABLE guilds (
  id BIGINT PRIMARY KEY                       -- guild entity, shared across bots
);

-- NULL on any setting means "not configured" — the bot applies its default.
CREATE TABLE guild_settings (
  bot_id                BIGINT NOT NULL REFERENCES bots(id) ON DELETE CASCADE,
  guild_id              BIGINT NOT NULL REFERENCES guilds(id) ON DELETE CASCADE,
  prefix                TEXT,
  inactivity_disconnect BOOLEAN,
  announcements         BOOLEAN,
  PRIMARY KEY (bot_id, guild_id)
);

CREATE TABLE guild_permissions (
  bot_id      BIGINT   NOT NULL REFERENCES bots(id) ON DELETE CASCADE,
  guild_id    BIGINT   NOT NULL REFERENCES guilds(id) ON DELETE CASCADE,
  entity_id   BIGINT   NOT NULL,              -- role or member id
  entity_type SMALLINT NOT NULL,
  permission  SMALLINT NOT NULL,
  PRIMARY KEY (bot_id, guild_id, entity_id)
);

-- ============================== track catalog ==============================

CREATE TABLE tracks (
  id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  source        TEXT   NOT NULL,              -- 'youtube', 'spotify', 'soundcloud', ...
  source_id     TEXT   NOT NULL,              -- lavaplayer identifier on that platform
  title         TEXT   NOT NULL,
  artist        TEXT   NOT NULL,
  duration_ms   BIGINT,
  url           TEXT,
  artwork_url   TEXT,
  isrc          TEXT,                         -- cross-platform matching
  encoded       TEXT,                         -- lavaplayer base64 blob (playback cache)
  first_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (source, source_id)
);

CREATE INDEX tracks_isrc_idx ON tracks (isrc) WHERE isrc IS NOT NULL;

-- ============================== live playback ==============================

-- One row per queue lifetime in a guild (bot joins voice -> bot leaves/stops).
-- Groups queue_history and track_plays so past queues can be shown whole.
CREATE TABLE queue_sessions (
  id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  bot_id           BIGINT NOT NULL REFERENCES bots(id),   -- which instance ran it
  guild_id         BIGINT NOT NULL REFERENCES guilds(id),
  voice_channel_id BIGINT,
  started_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  ended_at         TIMESTAMPTZ                -- NULL while active
);

CREATE INDEX queue_sessions_guild_idx ON queue_sessions (guild_id, started_at DESC);

CREATE TABLE players (
  bot_id           BIGINT NOT NULL REFERENCES bots(id) ON DELETE CASCADE,
  guild_id         BIGINT NOT NULL REFERENCES guilds(id) ON DELETE CASCADE,
  session_id       BIGINT REFERENCES queue_sessions(id), -- active session, NULL when idle
  text_channel_id  BIGINT,
  voice_channel_id BIGINT,
  queue_position   INT     NOT NULL DEFAULT 0, -- index of the current track in the queue
  playing          BOOLEAN NOT NULL DEFAULT FALSE,
  track_loaded     BOOLEAN NOT NULL DEFAULT FALSE, -- FALSE = stopped/idle, distinguishes from paused
  loop_mode        TEXT NOT NULL DEFAULT 'disabled' CHECK (loop_mode IN ('disabled', 'track', 'queue')),
  shuffle          TEXT NOT NULL DEFAULT 'disabled' CHECK (shuffle   IN ('disabled', 'enabled')),
  autoplay         TEXT NOT NULL DEFAULT 'disabled' CHECK (autoplay  IN ('disabled', 'enabled')),
  volume           SMALLINT NOT NULL DEFAULT 100,
  speed            REAL NOT NULL DEFAULT 1.0,
  pitch            REAL NOT NULL DEFAULT 1.0,
  bass_boost       REAL NOT NULL DEFAULT 0.0,
  rotation         BOOLEAN NOT NULL DEFAULT FALSE,
  -- wall-clock anchor, written on events only (play/pause/seek/track change):
  -- current position = position_ms + (playing ? now() - position_at : 0)
  position_ms      BIGINT      NOT NULL DEFAULT 0,
  position_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (bot_id, guild_id)
);

CREATE TABLE queue_tracks (
  id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  bot_id    BIGINT NOT NULL REFERENCES bots(id) ON DELETE CASCADE,
  guild_id  BIGINT NOT NULL REFERENCES guilds(id) ON DELETE CASCADE,
  position  INT    NOT NULL,
  track_id  BIGINT NOT NULL REFERENCES tracks(id),
  queued_by BIGINT NOT NULL REFERENCES users(id),
  queued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (bot_id, guild_id, position) DEFERRABLE INITIALLY DEFERRED
);

-- ============================== analytics (append-only) ==============================

CREATE TABLE queue_history (
  id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  session_id BIGINT NOT NULL REFERENCES queue_sessions(id),
  guild_id   BIGINT NOT NULL REFERENCES guilds(id),
  user_id    BIGINT NOT NULL REFERENCES users(id),
  track_id   BIGINT NOT NULL REFERENCES tracks(id),
  via        TEXT   NOT NULL DEFAULT 'command' CHECK (via IN ('command', 'dashboard', 'autoplay')),
  queued_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX queue_history_session_idx ON queue_history (session_id, queued_at);
CREATE INDEX queue_history_user_idx    ON queue_history (user_id,  queued_at DESC);
CREATE INDEX queue_history_guild_idx   ON queue_history (guild_id, queued_at DESC);

CREATE TABLE track_plays (
  id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  session_id BIGINT NOT NULL REFERENCES queue_sessions(id),
  guild_id   BIGINT NOT NULL REFERENCES guilds(id),
  track_id   BIGINT NOT NULL REFERENCES tracks(id),
  queued_by  BIGINT REFERENCES users(id),     -- NULL for autoplay
  started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  ended_at   TIMESTAMPTZ,
  played_ms  BIGINT,                          -- actual playback time, pauses excluded
  end_reason TEXT CHECK (end_reason IN ('finished', 'skipped', 'stopped', 'replaced', 'error'))
);

CREATE INDEX track_plays_session_idx ON track_plays (session_id, started_at);
CREATE INDEX track_plays_guild_idx   ON track_plays (guild_id, started_at DESC);
CREATE INDEX track_plays_track_idx   ON track_plays (track_id);

-- One row per continuous span a user actually heard a play. A user gets a new
-- segment each time hearing resumes (unpause, undeafen, rejoin, ...); listened
-- time = sum(ended_at - started_at) over a user's segments.
CREATE TABLE listening_segments (
  id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  play_id      BIGINT NOT NULL REFERENCES track_plays(id) ON DELETE CASCADE,
  user_id      BIGINT NOT NULL REFERENCES users(id),
  started_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  ended_at     TIMESTAMPTZ,                   -- NULL while the span is live
  start_reason TEXT NOT NULL CHECK (start_reason IN (
    'track_start', 'unpause', 'bot_unmute', 'user_undeafen',
    'bot_join', 'bot_move', 'user_join', 'user_move')),
  end_reason   TEXT CHECK (end_reason IN (
    'track_finish', 'stop', 'error', 'replace', 'pause',
    'bot_mute', 'user_deafen', 'bot_leave', 'user_leave', 'bot_move', 'user_move'))
);

CREATE INDEX listening_segments_user_idx ON listening_segments (user_id, started_at DESC);
CREATE INDEX listening_segments_play_idx ON listening_segments (play_id);

-- ============================== library ==============================

CREATE TABLE liked_tracks (
  user_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  track_id BIGINT NOT NULL REFERENCES tracks(id),
  position INT    NOT NULL,                    -- manual order; new likes append at the end
  liked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, track_id),
  UNIQUE (user_id, position) DEFERRABLE INITIALLY DEFERRED
);

-- Custom playlists: owned and editable, items live in playlist_tracks.
-- Cloning a saved external playlist creates one of these (resolved at clone time).
CREATE TABLE playlists (
  id          UUID   PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name        TEXT   NOT NULL,
  description TEXT,
  artwork_url TEXT,
  visibility  TEXT NOT NULL DEFAULT 'private' CHECK (visibility IN ('private', 'unlisted', 'public')),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- External platform collections (playlists, albums, artists) saved to the
-- library as-is: just a pointer, no items stored — contents are resolved live
-- at queue time, so they are always in sync with the platform by construction.
CREATE TABLE saved_collections (
  id          UUID   PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  kind        TEXT   NOT NULL CHECK (kind IN ('playlist', 'album', 'artist')),
  source      TEXT   NOT NULL CHECK (source IN ('spotify', 'deezer', 'youtube', 'apple_music')),
  source_url  TEXT   NOT NULL,
  browse_ref  TEXT,                           -- dashboard entity reference (source:kind:id)
  name        TEXT,                           -- display cache only, not source of truth
  subtitle    TEXT,                           -- display cache only
  artwork_url TEXT,                           -- display cache only
  saved_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id, source_url)
);

-- Collection-level enqueues (an album/playlist/artist queued whole), feeding
-- the dashboard's "recently queued" suggestions. Individual tracks are
-- already covered by queue_history.
CREATE TABLE queued_collections (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  kind        TEXT   NOT NULL CHECK (kind IN ('playlist', 'album', 'artist')),
  source      TEXT   NOT NULL,
  source_url  TEXT   NOT NULL,
  browse_ref  TEXT,                           -- dashboard entity reference (source:kind:id)
  name        TEXT,
  subtitle    TEXT,
  artwork_url TEXT,
  queued_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX queued_collections_user_idx ON queued_collections (user_id, queued_at DESC);

-- The user's library shelf: one ordered list mixing their own playlists and
-- saved external collections (Liked songs is a fixed entry, not a row).
-- Deleting the underlying playlist/saved collection removes the shelf entry.
CREATE TABLE library_items (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  position    INT    NOT NULL,
  playlist_id UUID REFERENCES playlists(id) ON DELETE CASCADE,
  saved_id    UUID REFERENCES saved_collections(id) ON DELETE CASCADE,
  added_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK ((playlist_id IS NULL) != (saved_id IS NULL)),
  UNIQUE (user_id, position) DEFERRABLE INITIALLY DEFERRED,
  UNIQUE NULLS NOT DISTINCT (user_id, playlist_id, saved_id)
);

CREATE TABLE playlist_tracks (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  playlist_id UUID   NOT NULL REFERENCES playlists(id) ON DELETE CASCADE,
  position    INT    NOT NULL,
  track_id    BIGINT NOT NULL REFERENCES tracks(id),
  added_by    BIGINT REFERENCES users(id),
  added_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (playlist_id, position) DEFERRABLE INITIALLY DEFERRED
);

-- Per-user sort preference for any track collection. Playlists are shareable,
-- so every user keeps their own view of each one; playlist_id NULL means the
-- user's liked songs. 'custom' = the collection's canonical position order.
CREATE TABLE collection_sorting (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  playlist_id UUID REFERENCES playlists(id) ON DELETE CASCADE,
  sort_by     TEXT NOT NULL DEFAULT 'custom' CHECK (sort_by IN ('custom', 'added', 'title', 'artist', 'duration')),
  sort_desc   BOOLEAN NOT NULL DEFAULT FALSE,
  UNIQUE NULLS NOT DISTINCT (user_id, playlist_id)
);
