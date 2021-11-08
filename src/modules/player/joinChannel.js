const Modules = require("../../structures/Modules");

const DiscordVoice = require('@discordjs/voice');

module.exports = class JoinChannel extends Modules {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "joinChannel";
        this.subcategory = "player";
    }

    async run(voiceChannel, command) {

        if (!voiceChannel.permissionsFor(this.client.user.id).has("CONNECT")) { throw "I do not have permission to **connect** to your voice channel!"; };
        if (!voiceChannel.permissionsFor(this.client.user.id).has("SPEAK")) { throw "I do not have permission to **speak** in your voice channel!"; };

        const connection = DiscordVoice.joinVoiceChannel({
            channelId: voiceChannel.id,
            guildId: voiceChannel.guild.id,
            adapterCreator: voiceChannel.guild.voiceAdapterCreator,
        })

        const player = DiscordVoice.createAudioPlayer()
        connection.subscribe(player)

        await this.client.database.db("guilds").collection("players").updateOne({ guildId: command.guild.id }, { $set: { queueID: 0, stopped: true, guildId: command.guild.id, announcesId: command.channel.id } }, { upsert: true });
        this.client.player.connectionEvents(connection, command.guild.id).catch(O_o => { });

        voiceChannel.guild.me.voice.setDeaf().catch(e => { });

        if(voiceChannel.type == "GUILD_STAGE_VOICE") {
            await voiceChannel.guild.me.voice.setSuppressed(false).catch(e => { voiceChannel.guild.me.voice.setRequestToSpeak(true); });
        }

        connection.playTimer = setTimeout(() => { this.client.player.inactivityDisconnect(voiceChannel.guild.id); }, 900000);

    }

}