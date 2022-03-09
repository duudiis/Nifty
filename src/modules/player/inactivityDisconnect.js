const Modules = require("../../structures/Modules");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class extends Modules {

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

        await this.client.player.updateNpMessage(guildId, "delete");

        const guild = this.client.guilds.cache.get(guildId);

        try { existingConnection.state.subscription.player.stop(); } catch (e) { };

        clearTimeout(existingConnection.playTimer); clearTimeout(existingConnection.pauseTimer); clearTimeout(existingConnection.aloneTimer);
        existingConnection.destroy();

        if (playerData?.announcesId) {
 
            const announcesChannel = await this.client.channels.fetch(playerData.announcesId).catch(e => { });
            if (!announcesChannel || !announcesChannel.permissionsFor(this.client.user.id).has("SEND_MESSAGES") || !announcesChannel.permissionsFor(this.client.user.id).has("EMBED_LINKS")) { return; };

            const inactivityEmbed = new MessageEmbed({ color: guild.me.displayHexColor })
                .setDescription(`I left the voice channel because I was inactive for too long.\nIf you are a **Premium** member, you can disable this by typing \`/247\`.`)

            announcesChannel.send({ embeds: [inactivityEmbed] }).then(m => { setTimeout(() => { m.delete().catch(e => { }) }, 120000) });

        }

        this.client.database.db("guilds").collection("players").deleteMany({ guildId: guildId });
        this.client.database.db("queues").collection(guildId).drop().catch(e => { });

    }

}