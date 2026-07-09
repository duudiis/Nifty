-- 002_autoplay.sql — the autoplay recommendation buffer + negative feedback
-- Applied to the `nifty` database on the shared postgres server.
-- Apply with: psql -1 -v ON_ERROR_STOP=1 -U nifty -d nifty -f 002_autoplay.sql

-- The upcoming auto-recommended tracks for a (bot, guild) player, shown on the
-- dashboard as the "Next from: Autoplay" queue section. The bot keeps this
-- topped up while autoplay is enabled; when the real queue runs out it moves
-- the head row into queue_tracks and plays it. Positions are contiguous from 0,
-- same contract as queue_tracks.
CREATE TABLE autoplay_tracks (
  id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  bot_id        BIGINT NOT NULL REFERENCES bots(id) ON DELETE CASCADE,
  guild_id      BIGINT NOT NULL REFERENCES guilds(id) ON DELETE CASCADE,
  position      INT    NOT NULL,
  track_id      BIGINT NOT NULL REFERENCES tracks(id),
  provider      TEXT,                          -- which recommender produced it ('deezer', 'youtube', 'spotify')
  seed_track_id BIGINT REFERENCES tracks(id),  -- the queue track it was derived from
  added_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (bot_id, guild_id, position) DEFERRABLE INITIALLY DEFERRED
);

CREATE INDEX autoplay_tracks_guild_idx ON autoplay_tracks (bot_id, guild_id, position);

-- Explicit negative feedback on recommendations: a user removed a track from
-- the autoplay buffer on the dashboard. Skips of autoplayed tracks are NOT
-- duplicated here — they are derived from track_plays (queued_by IS NULL AND
-- end_reason = 'skipped'). Both feed the recommender's exclusion set.
CREATE TABLE autoplay_feedback (
  id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  guild_id   BIGINT NOT NULL REFERENCES guilds(id) ON DELETE CASCADE,
  track_id   BIGINT NOT NULL REFERENCES tracks(id),
  user_id    BIGINT REFERENCES users(id),      -- who removed it (NULL if unknown)
  kind       TEXT NOT NULL CHECK (kind IN ('removed')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX autoplay_feedback_guild_idx ON autoplay_feedback (guild_id, created_at DESC);
