const Commands = require("../../structures/Commands");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "pause";
        this.aliases = [];

        this.description = "Pauses the player";
        this.category = "music";

        this.usage = "pause";
        this.options = [];

        this.requiredPermissions = ["MANAGE_PLAYER"];

        this.enabled = true;
    }

    async runAsMessage(message) {

        const response = await this.pause(message);

        if (response.code == "error") { return message.channel.send({ embeds: [response.embed] }); };
        if (response.code == "success") { return message.react("⏸") };

    }

    async runAsInteraction(interaction) {

        const response = await this.pause(interaction);
        return interaction.editReply({ embeds: [response.embed] });

    }

    async pause(command) {

        const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error });

        const voiceChannel = command.member.voice.channel;
        if (!voiceChannel) { return { code: "error", embed: errorEmbed.setDescription("You have to be connected to a voice channel before you can use this command!") } };

        let existingConnection = DiscordVoice.getVoiceConnection(command.guild.id);
        if (existingConnection && existingConnection.joinConfig.channelId != voiceChannel.id) { return { code: "error", embed: errorEmbed.setDescription("Someone else is already listening to music in different channel!") } };

        const pausedEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
            .setDescription(`:pause_button: Paused the player`)

        if (!existingConnection) {
            try { await this.client.player.joinChannel(voiceChannel, command) } catch (error) { return { code: "error", embed: errorEmbed.setDescription(`${error.message ? error.message : error}`) }; };
            return { code: "success", embed: pausedEmbed };
        }

        if (existingConnection.state.subscription.player.state.status == "paused" || existingConnection.state.subscription.player.state.status != "playing") { return { code: "success", embed: pausedEmbed } };

        existingConnection.state.subscription.player.pause();

        return { code: "success", embed: pausedEmbed };

    }

}