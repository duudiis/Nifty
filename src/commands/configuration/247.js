const Commands = require("../../structures/Commands");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "247";
        this.aliases = ["24/7", "forever", "stay", "infinity", "infinite"];

        this.description = "Toggles 24/7 mode";
        this.category = "music";

        this.usage = "247";
        this.options = []

        this.enabled = true;
    }

    async runAsMessage(message) {

        const input = message.array.slice(1).join(" ");

        let mode = undefined;

        if (this.client.constants.keywords.enabled.includes(input.toLowerCase())) { mode = "enabled" };
        if (this.client.constants.keywords.disabled.includes(input.toLowerCase())) { mode = "disabled" };

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

        if (!command.member.permissions.has("MANAGE_GUILD")) {

            const missingPermsEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
                .setDescription("You must have the `Manage Server` permission to use this command!")

            return { code: "error", embed: missingPermsEmbed };
        };

        let existingConnection = DiscordVoice.getVoiceConnection(command.guild.id);

        if (mode == "enabled" && existingConnection) {
            clearTimeout(existingConnection.playTimer); clearTimeout(existingConnection.pauseTimer); clearTimeout(existingConnection.aloneTimer);
        }

        if (mode == "disabled" && existingConnection) {

            if (command?.guild?.me?.voice?.channel?.members?.filter(member => !member.user.bot).size == 0) {
                clearTimeout(existingConnection.aloneTimer);
                existingConnection.aloneTimer = setTimeout(() => { this.client.player.inactivityDisconnect(command.guild.id); }, 300000);
            }

            if (existingConnection?.state?.subscription?.player?.state?.status == "idle") {
                clearTimeout(existingConnection.playTimer);
                existingConnection.playTimer = setTimeout(() => { this.client.player.inactivityDisconnect(guildId); }, 900000);
            }

            if (existingConnection?.state?.subscription?.player?.state?.status == "paused") {
                clearTimeout(existingConnection.pauseTimer);
                existingConnection.pauseTimer = setTimeout(() => { this.client.player.inactivityDisconnect(guildId); }, 3600000);
            }

        }

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