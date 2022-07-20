const Commands = require("../../structures/Commands");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "unpause";
        this.aliases = ["resume", "continue", "up"];

        this.description = "Unpauses the player";
        this.category = "music";

        this.usage = "unpause";
        this.options = [];

        this.requiredPermissions = ["MANAGE_PLAYER"];

        this.enabled = true;
    }

    async runAsMessage(message, fromPlay = false) {

        const response = await this.unpause(message);

        if (response.code == "error") { return message.channel.send({ embeds: [response.embed] }); };
        if (response.code == "success") { return fromPlay ? message.react("👌") : message.react("▶️") };

    }

    async runAsInteraction(interaction) {

        const response = await this.unpause(interaction);
        return interaction.editReply({ embeds: [response.embed] });

    }

    async unpause(command) {

        const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error });

        const voiceChannel = command.member.voice.channel;
        if (!voiceChannel) { return { code: "error", embed: errorEmbed.setDescription("You have to be connected to a voice channel before you can use this command!") } };

        let existingConnection = DiscordVoice.getVoiceConnection(command.guild.id);
        if (existingConnection && existingConnection.joinConfig.channelId != voiceChannel.id) { return { code: "error", embed: errorEmbed.setDescription("Someone else is already listening to music in different channel!") } };

        const unpausedEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
            .setDescription(`:arrow_forward: Unpaused the player`)

        if (!existingConnection) {
            try { await this.client.player.joinChannel(voiceChannel, command) } catch (error) { return { code: "error", embed: errorEmbed.setDescription(`${error.message ? error.message : error}`) }; };
            return { code: "success", embed: unpausedEmbed };
        }

        if (existingConnection.state.subscription.player.state.status == "paused") {
            existingConnection.state.subscription.player.unpause();
            return { code: "success", embed: unpausedEmbed };
        }

        const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: command.guild.id });

        if (playerData.stopped) {
            await this.client.player.updatePlayer(existingConnection, command.guild.id);
            return { code: "success", embed: unpausedEmbed };
        }

        return { code: "success", embed: unpausedEmbed };

    }

}