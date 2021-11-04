const Modules = require("../../structures/Modules");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class InactivityDisconnect extends Modules {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "inactivityDisconnect";
        this.subcategory = "player";
    }

    async run(guildId) {

        let existingConnection = DiscordVoice.getVoiceConnection(guildId);
        if (!existingConnection) { return; };

        const guildData = await this.client.database.db("default").collection("guilds").findOne({ id: guildId });
        const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: guildId });

        if (guildData?.forever == "enabled") { return; };

        const guild = this.client.guilds.cache.get(guildId);

        if (playerData?.channelId) {

            const announcesChannel = this.client.channels.cache.get(playerData.channelId);

            try { let lastNowPlayingMessage = await announcesChannel.messages.fetch(playerData.messageId); await lastNowPlayingMessage.delete() } catch (e) { };

            const inactivityEmbed = new MessageEmbed({ color: guild.me.displayHexColor })
                .setDescription(`I left the voice channel because I was inactive for too long.\nIf you are a **Premium** member, you can disable this by typing \`/247\`.`)

            announcesChannel.send({ embeds: [inactivityEmbed] }).then(m => { setTimeout(() => { m.delete().catch(e => { }) }, 120000) })

        }

        this.client.database.db("guilds").collection("players").deleteOne({ guildId: guildId });
        this.client.database.db("queues").collection(guildId).deleteMany({});

        try { existingConnection.state.subscription.player.stop(); } catch (e) { }
        existingConnection.destroy();

    }

}