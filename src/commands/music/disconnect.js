const Commands = require("../../structures/Commands");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "disconnect";
        this.aliases = ["dc", "fuckoff", "die", "leave", "quit", "reset"];

        this.description = "Resets the player, clears the queue, and leaves the voice channel";
        this.category = "music";

        this.usage = "disconnect";
        this.options = [];

        this.requiredPermissions = ["MANAGE_QUEUE", "MANAGE_PLAYER"];

        this.enabled = true;
    }

    async runAsMessage(message) {

        const response = await this.disconnect(message);

        if (response.code == "error") { return message.channel.send({ embeds: [response.embed] }); };
        if (response.code == "success") { return message.react("👋") };

    }

    async runAsInteraction(interaction) {

        const response = await this.disconnect(interaction);
        return interaction.editReply({ embeds: [response.embed] });

    }

    async disconnect(command) {

        const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error });

        const voiceChannel = command.member.voice.channel;
        if (!voiceChannel) { return { code: "error", embed: errorEmbed.setDescription("You have to be connected to a voice channel before you can use this command!") } };

        let existingConnection = DiscordVoice.getVoiceConnection(command.guild.id);
        if (existingConnection && existingConnection.joinConfig.channelId != voiceChannel.id) { return { code: "error", embed: errorEmbed.setDescription("Someone else is already listening to music in different channel!") } };

        let resetEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
            .setDescription("Reset the player")

        if (!existingConnection) { return { code: "success", embed: resetEmbed } };

        await this.client.player.updateNpMessage(command.guild.id, "delete");

        this.client.database.db("guilds").collection("players").deleteMany({ guildId: command.guild.id });
        this.client.database.db("queues").collection(command.guild.id).drop().catch(e => {});

        try { existingConnection.state.subscription.player.stop(); } catch (e) { }

        clearTimeout(existingConnection.playTimer); clearTimeout(existingConnection.pauseTimer); clearTimeout(existingConnection.aloneTimer);
        existingConnection.destroy();

        return { code: "success", embed: resetEmbed };

    }

}