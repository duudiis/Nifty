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

        if (mode == "enabled") {
            setTimeout(() => { this.client.player.updateNpMessage(command.guild.id, "send"); }, 500);
        }

        if (mode == "disabled") {
            this.client.player.updateNpMessage(command.guild.id, "delete");
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

}