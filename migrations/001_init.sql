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

CREATE TABLE guilds (
  id                    BIGINT PRIMARY KEY,
  prefix                TEXT,
  inactivity_disconnect BOOLEAN NOT NULL DEFAULT TRUE,
  announcements         TEXT
);

CREATE TABLE guild_permissions (
  guild_id    BIGINT   NOT NULL REFERENCES guilds(id) ON DELETE CASCADE,
  entity_id   BIGINT   NOT NULL,              -- role or member id
  entity_type SMALLINT NOT NULL,
  permission  SMALLINT NOT NULL,
  PRIMARY KEY (guild_id, entity_id)
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

CREATE TABLE players (
  guild_id         BIGINT PRIMARY KEY REFERENCES guilds(id) ON DELETE CASCADE,
  text_channel_id  BIGINT,
  voice_channel_id BIGINT,
  queue_position   INT     NOT NULL DEFAULT 0, -- index of the current track in the queue
  playing          BOOLEAN NOT NULL DEFAULT FALSE,
  loop_mode        TEXT NOT NULL DEFAULT 'disabled' CHECK (loop_mode IN ('disabled', 'track', 'queue')),
  shuffle          TEXT NOT NULL DEFAULT 'disabled' CHECK (shuffle   IN ('disabled', 'enabled')),
  autoplay         TEXT NOT NULL DEFAULT 'disabled' CHECK (autoplay  IN ('disabled', 'enabled')),
  speed            REAL NOT NULL DEFAULT 1.0,
  pitch            REAL NOT NULL DEFAULT 1.0,
  bass_boost       REAL NOT NULL DEFAULT 0.0,
  rotation         BOOLEAN NOT NULL DEFAULT FALSE,
  -- wall-clock anchor, written on events only (play/pause/seek/track change):
  -- current position = position_ms + (playing ? now() - position_at : 0)
  position_ms      BIGINT      NOT NULL DEFAULT 0,
  position_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE queue_tracks (
  id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  guild_id  BIGINT NOT NULL REFERENCES guilds(id) ON DELETE CASCADE,
  position  INT    NOT NULL,
  track_id  BIGINT NOT NULL REFERENCES tracks(id),
  queued_by BIGINT NOT NULL REFERENCES users(id),
  queued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (guild_id, position) DEFERRABLE INITIALLY DEFERRED
);

-- ============================== analytics (append-only) ==============================

CREATE TABLE queue_history (
  id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  guild_id  BIGINT NOT NULL REFERENCES guilds(id),
  user_id   BIGINT NOT NULL REFERENCES users(id),
  track_id  BIGINT NOT NULL REFERENCES tracks(id),
  via       TEXT   NOT NULL DEFAULT 'command' CHECK (via IN ('command', 'dashboard', 'autoplay')),
  queued_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX queue_history_user_idx  ON queue_history (user_id,  queued_at DESC);
CREATE INDEX queue_history_guild_idx ON queue_history (guild_id, queued_at DESC);

CREATE TABLE track_plays (
  id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  guild_id   BIGINT NOT NULL REFERENCES guilds(id),
  track_id   BIGINT NOT NULL REFERENCES tracks(id),
  queued_by  BIGINT REFERENCES users(id),     -- NULL for autoplay
  started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  ended_at   TIMESTAMPTZ,
  played_ms  BIGINT,
  end_reason TEXT CHECK (end_reason IN ('finished', 'skipped', 'stopped', 'replaced', 'error'))
);

CREATE INDEX track_plays_guild_idx ON track_plays (guild_id, started_at DESC);
CREATE INDEX track_plays_track_idx ON track_plays (track_id);

CREATE TABLE play_listeners (
  play_id     BIGINT NOT NULL REFERENCES track_plays(id) ON DELETE CASCADE,
  user_id     BIGINT NOT NULL REFERENCES users(id),
  listened_ms BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (play_id, user_id)
);

CREATE INDEX play_listeners_user_idx ON play_listeners (user_id, play_id DESC);

-- ============================== library ==============================

CREATE TABLE liked_tracks (
  user_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  track_id BIGINT NOT NULL REFERENCES tracks(id),
  liked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, track_id)
);

CREATE TABLE playlists (
  id          UUID   PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name        TEXT   NOT NULL,
  description TEXT,
  artwork_url TEXT,
  visibility  TEXT NOT NULL DEFAULT 'private' CHECK (visibility IN ('private', 'unlisted', 'public')),
  source      TEXT NOT NULL DEFAULT 'custom'  CHECK (source IN ('custom', 'spotify', 'deezer', 'youtube', 'apple_music')),
  source_url  TEXT,                           -- original link for imported playlists
  synced_at   TIMESTAMPTZ,                    -- last re-import from source
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
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
