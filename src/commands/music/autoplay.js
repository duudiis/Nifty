const Commands = require("../../structures/Commands");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "autoplay";
        this.aliases = ["ap", "auto"];

        this.description = "Toggles whether Nifty will automatically play related songs after the queue has run out";
        this.category = "music";

        this.usage = "autoplay";
        this.options = [];

        this.requiredPermissions = ["MANAGE_PLAYER"];

        this.enabled = true;
    }

    async runAsMessage(message) {

        const input = message.array.slice(1).join(" ");

        let mode = undefined;

        if (this.client.constants.keywords.enabled.includes(input.toLowerCase())) { mode = "on" };
        if (this.client.constants.keywords.disabled.includes(input.toLowerCase())) { mode = "off" };

        if (!mode) {

            const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: message.guild.id });

            let playerAutoplay = playerData?.autoplay;
            if (!playerAutoplay) { playerAutoplay = "off" };

            let nextAutoplay = {
                "on": "off",
                "off": "on"
            }

            mode = nextAutoplay[playerAutoplay];

        };

        const response = await this.autoplay(mode, message);
        return message.channel.send({ embeds: [response.embed] });

    }

    async runAsInteraction(interaction) {

        const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: interaction.guild.id });

        let playerAutoplay = playerData?.autoplay;
        if (!playerAutoplay) { playerAutoplay = "off" };

        let nextAutoplay = {
            "on": "off",
            "off": "on"
        }

        let mode = nextAutoplay[playerAutoplay];

        const response = await this.autoplay(mode, interaction);
        return interaction.editReply({ embeds: [response.embed] });

    }

    async autoplay(mode, command) {

        const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error });

        const voiceChannel = command.member.voice.channel;
        if (!voiceChannel) { return { code: "error", embed: errorEmbed.setDescription("You have to be connected to a voice channel before you can use this command!") } };

        let existingConnection = DiscordVoice.getVoiceConnection(command.guild.id);
        if (existingConnection && existingConnection.joinConfig.channelId != voiceChannel.id) { return { code: "error", embed: errorEmbed.setDescription("Someone else is already listening to music in different channel!") } };

        if (!existingConnection) {
            try { await this.client.player.joinChannel(voiceChannel, command) } catch (error) { return { code: "error", embed: errorEmbed.setDescription(`${error.message ? error.message : error}`) }; };
            existingConnection = DiscordVoice.getVoiceConnection(command.guild.id);
        };

        const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: command.guild.id });
        if (playerData?.loop && playerData?.loop != "disabled") { return { code: "error", embed: errorEmbed.setDescription("AutoPlay and Loop cannot both be enabled at the same time!") } };

        await this.client.database.db("guilds").collection("players").updateOne({ guildId: command.guild.id }, { $set: { autoplay: mode } }, { upsert: true });

        if (mode == "on" && existingConnection?.state?.subscription?.player?.state?.status == "idle") {
            this.client.player.queueNext(existingConnection, command.guild.id);
        }

        let autoplayMessage = {
            "on": "AutoPlay is now **enabled**",
            "off": "AutoPlay is now **disabled**"
        }

        const autoplayEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
            .setDescription(autoplayMessage[mode])

        return { code: "success", embed: autoplayEmbed };

    }

}