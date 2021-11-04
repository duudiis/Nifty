const Commands = require("../../structures/Commands");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class Announce extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "announce";
        this.aliases = ["announces", "announcement", "announcements", "an", "ann", "a"];

        this.description = "Toggles whether Nifty will announce when songs start playing";
        this.category = "music";

        this.usage = "announce";
        this.options = []

        this.enabled = true;
    }

    async runAsMessage(message) {

        const input = message.array.slice(1).join(" ");

        let mode = undefined;

        if (input.includes("on") || input.includes("enabled") || input.includes("enable") || input.includes("yes")) { mode = "enabled" };
        if (input.includes("off") || input.includes("disabled") || input.includes("disable") || input.includes("no")) { mode = "disabled" };

        if (!mode) {

            const guildData = await this.client.database.db("default").collection("guilds").findOne({ id: message.guild.id });
            let guildAnnounce = guildData?.announce;

            if (!guildAnnounce) { guildAnnounce = "enabled" };

            let nextAnnounce = {
                "disabled": "enabled",
                "enabled": "disabled",
            }

            mode = nextAnnounce[guildAnnounce];

        };

        const response = await this.announce(mode, message);
        return message.reply({ embeds: [response.embed] });

    }

    async runAsInteraction(interaction) {

        const guildData = await this.client.database.db("default").collection("guilds").findOne({ id: interaction.guild.id });
        let guildAnnounce = guildData?.announce;

        if (!guildAnnounce) { guildAnnounce = "enabled" };

        let nextAnnounce = {
            "disabled": "enabled",
            "enabled": "disabled",
        }

        let mode = nextAnnounce[guildAnnounce];

        const response = await this.announce(mode, interaction);
        return interaction.editReply({ embeds: [response.embed] });

    }

    async announce(mode, command) {

        if (!command.member.permissions.has("MANAGE_GUILD")) {

            const missingPermsEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
                .setDescription("You must have the `Manage Server` permission to use this command!")

            return { code: "error", embed: missingPermsEmbed };
        };

        let existingConnection = DiscordVoice.getVoiceConnection(command.guild.id);

        if (mode == "disabled") {
            const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: command.guild.id });

            if (playerData?.channelId && playerData?.messageId) {
                const announcesChannel = this.client.channels.cache.get(playerData.channelId);
                try { let lastNowPlayingMessage = await announcesChannel.messages.fetch(playerData.messageId); lastNowPlayingMessage.delete().catch(o_O => { }) } catch (e) { };
            }
        }

        if (mode == "enabled") {
            const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: command.guild.id });

            if (existingConnection?.state?.subscription?.player?.state?.resource?.metadata && playerData?.channelId) {
                const npMetadata = existingConnection.state.subscription.player.state.resource.metadata;

                const announcesChannel = this.client.channels.cache.get(playerData.channelId);

                let nowPlayingEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
                    .setTitle("Now Playing")
                    .setDescription(`[${await this.removeFormatting(npMetadata.title)}](${npMetadata.url}) [<@${npMetadata.user}>]`)

                setTimeout(async () => {
                    const newNowPlayingMessage = await announcesChannel.send({ embeds: [nowPlayingEmbed] });
                    this.client.database.db("guilds").collection("players").updateOne({ channelId: playerData.channelId }, { $set: { messageId: newNowPlayingMessage.id } }, { upsert: true });
                }, 500)
            }
        }

        await this.client.database.db("default").collection("guilds").updateOne({ id: command.guild.id }, { $set: { announce: mode } }, { upsert: true });

        let announcesMessage = {
            "disabled": "**Announcing of tracks** is now **disabled**.",
            "enabled": "**Announcing of tracks** is now **enabled**."
        }

        const announceEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
            .setDescription(announcesMessage[mode])

        return { code: "success", embed: announceEmbed };

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