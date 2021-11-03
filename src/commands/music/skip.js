const Commands = require("../../structures/Commands");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class Skip extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "skip";
        this.aliases = ["next", "n"];

        this.description = "Skips to the next song";
        this.category = "music";

        this.usage = "skip";
        this.options = []

        this.enabled = true;
    }

    async runAsMessage(message) {

        const response = await this.skip(message);

        if (response.code == "error") { return message.reply({ embeds: [response.embed] }); };
        if (response.code == "success") { return message.react("👌") };

    }

    async runAsInteraction(interaction) {

        const response = await this.skip(interaction);
        return interaction.editReply({ embeds: [response.embed] });

    }

    async skip(command) {

        const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error });

        const voiceChannel = command.member.voice.channel;
        if (!voiceChannel) { return { code: "error", embed: errorEmbed.setDescription("You have to be connected to a voice channel before you can use this command!") } };

        let existingConnection = DiscordVoice.getVoiceConnection(command.guild.id);
        if (existingConnection && existingConnection.joinConfig.channelId != voiceChannel.id) { return { code: "error", embed: errorEmbed.setDescription("Someone else is already listening to music in different channel!") } };

        const skippedTrackEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
            .setDescription(`Skipped to the next song :blush:`)

        if (!existingConnection) {
            try { await this.client.player.joinChannel(voiceChannel, command) } catch (error) { return { code: "error", embed: errorEmbed.setDescription(`${error.message ? error.message : error}`) }; };
            return { code: "success", embed: skippedTrackEmbed };
        };

        existingConnection.state.subscription.player.stop();

        return { code: "success", embed: skippedTrackEmbed };

    }

}