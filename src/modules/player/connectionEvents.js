const Modules = require("../../structures/Modules");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class extends Modules {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "connectionEvents";
        this.subcategory = "player";
    }

    async run(connection, guildId) {

        connection.state.subscription.player.on("idle", async () => {

            this.updateTimer(connection, guildId, "create", "playTimer");

            this.client.player.queueNext(connection, guildId);

            this.client.player.updateNpMessage(guildId, "delete");

        })

        connection.state.subscription.player.on("playing", async () => {

            this.updateTimer(connection, guildId, "reset", "playTimer");
            this.updateTimer(connection, guildId, "reset", "pauseTimer");

            if (!connection?.state?.subscription?.player?.state?.resource?.metadata) { return };
            if ((connection.state.subscription.player.state.resource.playbackDuration / 1000) > 2) { return };

            this.client.player.updateNpMessage(guildId, "send/delete");

        })

        connection.state.subscription.player.on("paused", async () => {

            this.updateTimer(connection, guildId, "create", "pauseTimer");

        })

        connection.state.subscription.player.on("error", async (error) => {

            this.updateTimer(connection, guildId, "create", "playTimer");

            this.client.player.queueNext(connection, guildId);

            const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: guildId });

            if (playerData.announcesId) {

                const announcesChannel = await this.client.channels.fetch(playerData.announcesId).catch(e => { });
                if (!announcesChannel || !announcesChannel.permissionsFor(this.client.user.id).has("SEND_MESSAGES") || !announcesChannel.permissionsFor(this.client.user.id).has("EMBED_LINKS")) { return; };

                let trackData = "";

                if (error?.resource?.metadata) {
                    trackData = `[${await this.client.removeFormatting(error.resource.metadata.title, 56)}](${error.resource.metadata.url}) [<@${error.resource.metadata.user}>]\n`
                }

                const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error })
                    .setTitle("An error occurred while playing")
                    .setDescription(`${trackData}${error.toString()}`)

                announcesChannel.send({ embeds: [errorEmbed] }).then(m => { setTimeout(() => { m.delete().catch(e => { }) }, 120000) });

            }

        })

    }

    async updateTimer(connection, guildId, action, type) {

        const timersTime = {
            playTimer: 900000,
            pauseTimer: 3600000
        }

        if (action == "create") {

            clearTimeout(connection[type]);
            connection[type] = setTimeout(() => { this.client.player.inactivityDisconnect(guildId); }, timersTime[type]);

        } else if (action == "reset") {

            clearTimeout(connection[type]);

        }

    }

}