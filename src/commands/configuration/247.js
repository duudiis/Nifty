const Commands = require("../../structures/Commands");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class Forever extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "247";
        this.aliases = ["24/7", "forever", "infinity", "24.7"];

        this.description = "Toggles 24/7 mode";
        this.category = "music";

        this.usage = "247";
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
            let guild247 = guildData?.forever;

            if (!guild247) { guild247 = "disabled" };

            let next247 = {
                "disabled": "enabled",
                "enabled": "disabled",
            }

            mode = next247[guild247];

        };

        const response = await this.c247(mode, message);
        return message.reply({ embeds: [response.embed] });

    }

    async runAsInteraction(interaction) {

        const guildData = await this.client.database.db("default").collection("guilds").findOne({ id: interaction.guild.id });
        let guild247 = guildData?.forever;

        if (!guild247) { guild247 = "disabled" };

        let next247 = {
            "disabled": "enabled",
            "enabled": "disabled",
        }

        let mode = next247[guild247];

        const response = await this.c247(mode, interaction);
        return interaction.editReply({ embeds: [response.embed] });

    }

    async c247(mode, command) {

        if (!(await this.client.checkPremium(command.guild.id))) {

            const missingPremiumEmbed = new MessageEmbed({ color: this.client.constants.colors.error })
                .setDescription("You need to upgrade this server to use **Premium** commands here!")

            return { code: "error", embed: missingPremiumEmbed };

        }

        if (!command.member.permissions.has("MANAGE_GUILD")) {

            const missingPermsEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
                .setDescription("You must have the `Manage Server` permission to use this command!")

            return { code: "error", embed: missingPermsEmbed };
        };

        await this.client.database.db("default").collection("guilds").updateOne({ id: command.guild.id }, { $set: { forever: mode } }, { upsert: true });

        let foreverMessage = {
            "disabled": "24/7 mode is now **disabled** in this server.",
            "enabled": "24/7 mode is now **enabled** in this server."
        }

        const foreverEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
            .setDescription(foreverMessage[mode])

        return { code: "success", embed: foreverEmbed };

    }

}