const Commands = require("../../structures/Commands");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "join";
        this.aliases = ["summon"];

        this.description = "Makes the bot join your voice channel";
        this.category = "music";

        this.usage = "join";
        this.options = [];

        this.requiredPermissions = ["Manage Player"];

        this.enabled = true;
    }

    async runAsMessage(message) {

        const response = await this.join(message);

        if (response.code == "error") { return message.reply({ embeds: [response.embed] }); };
        if (response.code == "success") { return message.react("👌") };

    }

    async runAsInteraction(interaction) {

        const response = await this.join(interaction);
        return interaction.editReply({ embeds: [response.embed] });

    }

    async join(command) {

        const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error });

        const voiceChannel = command.member.voice.channel;
        if (!voiceChannel) { return { code: "error", embed: errorEmbed.setDescription("You have to be connected to a voice channel before you can use this command!") } };

        let existingConnection = DiscordVoice.getVoiceConnection(command.guild.id);
        if (existingConnection && existingConnection.joinConfig.channelId != voiceChannel.id) { return { code: "error", embed: errorEmbed.setDescription("Someone else is already listening to music in different channel!") } };

        const joinedEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
            .setDescription(`Joined your voice channel :blush:`)

        if (existingConnection) { return { code: "success", embed: joinedEmbed } };

        try { await this.client.player.joinChannel(voiceChannel, command) } catch (error) { return { code: "error", embed: errorEmbed.setDescription(`${error.message ? error.message : error}`) }; };

        return { code: "success", embed: joinedEmbed };

    }

}