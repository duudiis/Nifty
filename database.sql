CREATE TABLE Guilds (
    guild_id VARCHAR(32) NOT NULL UNIQUE,
    prefix VARCHAR(16),
    inactivity_disconnect BOOLEAN,
    announcements VARCHAR(32),
    PRIMARY KEY (guild_id)
);

CREATE TABLE Perms (
    guild_id VARCHAR(32) NOT NULL UNIQUE,
    entity_id VARCHAR(32) NOT NULL,
    entity_type INT NOT NULL,
    permission INT NOT NULL,
    PRIMARY KEY (guild_id)
);

CREATE TABLE Players (
    guild_id VARCHAR(32) NOT NULL UNIQUE,
    channel_id VARCHAR(32),
    voice_id VARCHAR(32),
    position INT NOT NULL,
    playing BOOLEAN NOT NULL,
    looping VARCHAR(32),
    shuffle VARCHAR(32),
    autoplay VARCHAR(32),
    PRIMARY KEY (guild_id)
);

CREATE TABLE Queues (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    guild_id VARCHAR(32) NOT NULL,
    track_id INT NOT NULL,
    track_name VARCHAR(256) NOT NULL,
    member_id VARCHAR(32) NOT NULL,
    encoded_track VARCHAR(1024) NOT NULL
);