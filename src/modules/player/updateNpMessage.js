const Modules = require("../../structures/Modules");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class UpdateNpMessage extends Modules {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "updateNpMessage";
        this.subcategory = "player";
    }

    async run(guildId, action) {

        const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: guildId });
        if (!playerData?.announcesId) { return; };

        if (action.includes("delete")) {

            if (playerData.channelId && playerData.messageId) {
                let oldNpChannel = await this.client.channels.fetch(playerData.channelId).catch(e => { });

                if(oldNpChannel) {
                    let oldNpMessage = await oldNpChannel.messages.fetch(playerData.messageId).catch(e => { });
                    if (oldNpMessage) { oldNpMessage.delete().catch(e => { }); };
                }
 
                await this.client.database.db("guilds").collection("players").updateOne({ guildId: guildId }, { $set: { channelId: null, messageId: null } }, { upsert: true });
            }

        }

        if (action.includes("send")) {

            let existingConnection = DiscordVoice.getVoiceConnection(guildId);

            const npMetadata = existingConnection?.state?.subscription?.player?.state?.resource?.metadata;
            if (!npMetadata) { return; };

            const guildData = await this.client.database.db("default").collection("guilds").findOne({ id: guildId });
            if (guildData?.announce == "disabled") { return; };

            const guild = this.client.guilds.cache.get(guildId);

            const announcesChannel = await this.client.channels.fetch(playerData.announcesId).catch(e => { });
            if (!announcesChannel || !announcesChannel.permissionsFor(this.client.user.id).has("SEND_MESSAGES") || !announcesChannel.permissionsFor(this.client.user.id).has("EMBED_LINKS")) { return; };

            let nowPlayingEmbed = new MessageEmbed({ color: guild.me.displayHexColor })
                .setTitle("Now Playing")
                .setDescription(`[${await this.client.removeFormatting(npMetadata.title, 56)}](${npMetadata.url}) [<@${npMetadata.user}>]`)

            const newNpMessage = await announcesChannel.send({ embeds: [nowPlayingEmbed] });
            await this.client.database.db("guilds").collection("players").updateOne({ guildId: guildId }, { $set: { channelId: newNpMessage.channel.id, messageId: newNpMessage.id } }, { upsert: true });

        }

    }

}