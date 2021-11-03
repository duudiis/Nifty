const Commands = require("../../structures/Commands");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class Autoplay extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "autoplay";
        this.aliases = ["ap", "auto"];

        this.description = "Toggles whether Nifty will automatically play related songs after the queue has run out";
        this.category = "music";

        this.usage = "autoplay";
        this.options = []

        this.enabled = true;
    }

    async runAsMessage(message) {

        const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error });

        const input = message.array.slice(1).join(" ");

        const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: message.guild.id });
        if(playerData?.loop && playerData?.loop != "disabled") { return message.reply({ embeds: [ errorEmbed.setDescription("AutoPlay and Loop cannot both be enabled at the same time!") ] }); };

        let mode = undefined;

        if (input.includes("on") || input.includes("enable")) { mode = "on" };
        if (input.includes("off") || input.includes("disable")) { mode = "off" };

        if (!mode) {

            let playerAutoplay = playerData?.autoplay;

            if (!playerAutoplay) { playerAutoplay = "off" };

            let nextAutoplay = {
                "on": "off",
                "off": "on"
            }

            mode = nextAutoplay[playerAutoplay];

        };

        const response = await this.autoplay(mode, message);
        return message.reply({ embeds: [response.embed] });

    }

    async runAsInteraction(interaction) {

        const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error });

        const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: message.guild.id });
        if(playerData?.loop && playerData?.loop != "disabled") { return interaction.editReply({ embeds: [ errorEmbed.setDescription("AutoPlay and Loop cannot both be enabled at the same time!") ] }); };

        let playerAutoplay = playerData?.autoplay;

        if (!playerAutoplay) { playerAutoplay = "off" };

        let nextAutoplay = {
            "on": "off",
            "off": "on"
        }

        mode = nextAutoplay[playerAutoplay];

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

        await this.client.database.db("guilds").collection("players").updateOne({ guildId: command.guild.id }, { $set: { autoplay: mode } }, { upsert: true });

        let autoplayMessage = {
            "on": "AutoPlay is now **enabled**",
            "off": "AutoPlay is now **disabled**"
        }

        const autoplayEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
            .setDescription(autoplayMessage[mode])

        return { code: "success", embed: autoplayEmbed };

    }

}