const Modules = require("../../structures/Modules");

const DiscordVoice = require('@discordjs/voice');

module.exports = class extends Modules {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "reconnectPlayers";
        this.subcategory = "player";
    }

    async run() {

        const playersData = await this.client.database.db("guilds").collection("players").find({}).toArray();

        for (const playerData of playersData) {

            const guild = this.client.guilds.cache.get(playerData.guildId);
            if (!guild) { this.killPlayer(guild.id); continue; };

            await this.client.player.updateNpMessage(guild.id, "delete");

            if (!playerData.voiceId) { this.killPlayer(guild.id); continue; }

            const voiceChannel = await guild.channels.fetch(playerData.voiceId).catch(e => { });;
            if (!voiceChannel) { this.killPlayer(guild.id); continue; };

            if (!voiceChannel.permissionsFor(this.client.user.id).has("CONNECT")) { this.killPlayer(guild.id); continue; };
            if (!voiceChannel.permissionsFor(this.client.user.id).has("SPEAK")) { this.killPlayer(guild.id); continue; };

            const connection = DiscordVoice.joinVoiceChannel({
                channelId: voiceChannel.id,
                guildId: voiceChannel.guild.id,
                adapterCreator: voiceChannel.guild.voiceAdapterCreator,
            })

            const player = DiscordVoice.createAudioPlayer();
            connection.subscribe(player);

            this.client.player.connectionEvents(connection, guild.id).catch(O_o => { });

            voiceChannel.guild.me.voice.setDeaf().catch(e => { });

            if (voiceChannel.type == "GUILD_STAGE_VOICE") {
                await voiceChannel.guild.me.voice.setSuppressed(false).catch(e => { voiceChannel.guild.me.voice.setRequestToSpeak(true).catch(e => { }); });
            }

            connection.playTimer = setTimeout(() => { this.client.player.inactivityDisconnect(voiceChannel.guild.id); }, 900000);

            if (!player.stopped) { this.client.player.updatePlayer(connection, guild.id); };

        }

    }

    async killPlayer(guildId) {

        this.client.database.db("guilds").collection("players").deleteMany({ guildId: guildId });
        this.client.database.db("queues").collection(guildId).drop().catch(e => {});

    }

}