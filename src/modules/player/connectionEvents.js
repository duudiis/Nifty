const Modules = require("../../structures/Modules");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class ConnectionEvents extends Modules {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "connectionEvents";
        this.subcategory = "player";
    }

    async run(connection, guildId) {

        connection.state.subscription.player.on("idle", async () => {

            this.client.player.queueNext(connection, guildId);

            const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: guildId });

            if (playerData.channelId && playerData.messageId) {

                const announcesChannel = this.client.channels.cache.get(playerData.channelId);

                try { let lastNowPlayingMessage = await announcesChannel.messages.fetch(playerData.messageId); lastNowPlayingMessage.delete().catch(o_O => { }) } catch (e) { };

                this.client.database.db("guilds").collection("players").updateOne({ messageId: playerData.messageId }, { $set: { messageId: null } }, { upsert: true });

            }

        })

        connection.state.subscription.player.on("playing", async () => {

            if (!connection?.state?.subscription?.player?.state?.resource?.metadata) { return };
            const npMetadata = connection.state.subscription.player.state.resource.metadata;

            const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: guildId });

            if (playerData.channelId) {

                const announcesChannel = this.client.channels.cache.get(playerData.channelId);

                if (playerData.messageId) {
                    try { let lastNowPlayingMessage = await announcesChannel.messages.fetch(playerData.messageId); lastNowPlayingMessage.delete().catch(o_O => { }) } catch (e) { };
                }

                const guildData = await this.client.database.db("default").collection("guilds").findOne({ id: guildId });
                let guildAnnounce = guildData?.announce;

                if (!guildAnnounce) { guildAnnounce = "enabled" };
                if (guildAnnounce == "disabled") { return };

                const guild = this.client.guilds.cache.get(guildId)

                let nowPlayingEmbed = new MessageEmbed({ color: guild.me.displayHexColor })
                    .setTitle("Now Playing")
                    .setDescription(`[${await this.removeFormatting(npMetadata.title)}](${npMetadata.url}) [<@${npMetadata.user}>]`)

                const newNowPlayingMessage = await announcesChannel.send({ embeds: [nowPlayingEmbed] });
                this.client.database.db("guilds").collection("players").updateOne({ channelId: playerData.channelId }, { $set: { messageId: newNowPlayingMessage.id } }, { upsert: true });

            }

        })

        connection.state.subscription.player.on("paused", async () => {

            const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: guildId });

            if (playerData.channelId && playerData.messageId) {

                const announcesChannel = this.client.channels.cache.get(playerData.channelId);

                try { let lastNowPlayingMessage = await announcesChannel.messages.fetch(playerData.messageId); lastNowPlayingMessage.delete().catch(o_O => { }) } catch (e) { };

                this.client.database.db("guilds").collection("players").updateOne({ messageId: playerData.messageId }, { $set: { messageId: null } }, { upsert: true });

            }

        })

        connection.state.subscription.player.on("error", async (error) => {

            this.client.player.queueNext(connection, guildId);

            const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: guildId });

            if (playerData.channelId) {

                const announcesChannel = this.client.channels.cache.get(playerData.channelId)

                let trackData = "";

                if (error?.resource?.metadata) {
                    trackData = `[${await this.removeFormatting(error.resource.metadata.title)}](${error.resource.metadata.url}) [<@${error.resource.metadata.user}>]\n`
                }

                let errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error })
                    .setTitle("An error occurred while playing")
                    .setDescription(`${trackData}${error.toString()}`)

                announcesChannel.send({ embeds: [errorEmbed] });

            }

        })

        connection.on("disconnected", async () => {

            const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: guildId });

            if (playerData.channelId && playerData.messageId) {

                const announcesChannel = this.client.channels.cache.get(playerData.channelId);
                try { let lastNowPlayingMessage = await announcesChannel.messages.fetch(playerData.messageId); lastNowPlayingMessage.delete().catch(o_O => { }) } catch (e) { };

            }

            this.client.database.db("guilds").collection("players").deleteOne({ guildId: guildId });
            this.client.database.db("queues").collection(guildId).deleteMany({});

            try { connection.state.subscription.player.stop(); } catch (e) { }
            connection.destroy();

        })

    }

    async removeFormatting(string) {

        if (string.length >= 64) { string = string.slice(0, 60).trimEnd() + "…" };

        string = string.replaceAll("*", "\\*");
        string = string.replaceAll("_", "\\_");
        string = string.replaceAll("~", "\\~");
        string = string.replaceAll("`", "\\`");
        string = string.replaceAll("[", "\\[");
        string = string.replaceAll("]", "\\]");

        return string;

    }

}