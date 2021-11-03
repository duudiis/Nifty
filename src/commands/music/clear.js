const Commands = require("../../structures/Commands");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class Clear extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "clear";
        this.aliases = ["cls"];

        this.description = "Makes the bot join your voice channel";
        this.category = "music";

        this.usage = "clear";
        this.options = []

        this.enabled = true;
    }

    async runAsMessage(message) {

        const response = await this.clear(message);

        if (response.code == "error") { return message.reply({ embeds: [response.embed] }); };
        if (response.code == "success") { return message.react("👌") };

    }

    async runAsInteraction(interaction) {

        const response = await this.clear(interaction);
        return interaction.editReply({ embeds: [response.embed] });

    }

    async clear(command) {

        const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error });

        const voiceChannel = command.member.voice.channel;
        if (!voiceChannel) { return { code: "error", embed: errorEmbed.setDescription("You have to be connected to a voice channel before you can use this command!") } };

        let existingConnection = DiscordVoice.getVoiceConnection(command.guild.id);
        if (existingConnection && existingConnection.joinConfig.channelId != voiceChannel.id) { return { code: "error", embed: errorEmbed.setDescription("Someone else is already listening to music in different channel!") } };

        const clearedEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
            .setDescription(`Cleared the queue`)

        if (!existingConnection) {
            try { await this.client.player.joinChannel(voiceChannel, command) } catch (error) { return { code: "error", embed: errorEmbed.setDescription(`${error.message ? error.message : error}`) }; };
            return { code: "success", embed: clearedEmbed };
        }

        await this.client.database.db("guilds").collection("players").updateOne({ guildId: command.guild.id }, { $set: { queueID: 0, stopped: true, guildId: command.guild.id, channelId: command.channel.id } }, { upsert: true });
        this.client.database.db("queues").collection(command.guild.id).deleteMany({});

        existingConnection.state.subscription.player.skipExecute = true;

        existingConnection.state.subscription.player.stop()

        setTimeout(async () => { existingConnection.state.subscription.player.skipExecute = false; }, 4000);

        return { code: "success", embed: clearedEmbed };

    }

}