const Commands = require("../../structures/Commands");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "back";
        this.aliases = ["b"];

        this.description = "Goes back a song";
        this.category = "music";

        this.usage = "back";
        this.options = [];

        this.requiredPermissions = ["Manage Player"];

        this.enabled = true;
    }

    async runAsMessage(message) {

        const response = await this.back(message);

        if (response.code == "error") { return message.reply({ embeds: [response.embed] }); };
        if (response.code == "success") { return message.react("👌") };

    }

    async runAsInteraction(interaction) {

        const response = await this.back(interaction);
        return interaction.editReply({ embeds: [response.embed] });

    }

    async back(command) {

        const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error });

        const voiceChannel = command.member.voice.channel;
        if (!voiceChannel) { return { code: "error", embed: errorEmbed.setDescription("You have to be connected to a voice channel before you can use this command!") } };

        let existingConnection = DiscordVoice.getVoiceConnection(command.guild.id);
        if (existingConnection && existingConnection.joinConfig.channelId != voiceChannel.id) { return { code: "error", embed: errorEmbed.setDescription("Someone else is already listening to music in different channel!") } };

        const skippedTrackEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
            .setDescription(`Skipped to the previous song :blush:`)

        if (!existingConnection) {
            try { await this.client.player.joinChannel(voiceChannel, command) } catch (error) { return { code: "error", embed: errorEmbed.setDescription(`${error.message ? error.message : error}`) }; };
            return { code: "success", embed: skippedTrackEmbed };
        };

        const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: command.guild.id });
        const queueData = await this.client.database.db("queues").collection(command.guild.id).find({}).toArray();

        let nextQueueID = playerData.queueID - 1;
        let nextQueue = queueData[nextQueueID];

        if (!nextQueue && playerData.loop == "queue") {

            nextQueueID = queueData.length - 1;
            nextQueue = queueData[nextQueueID];

        }

        if (!nextQueue) {
            await this.client.database.db("guilds").collection("players").updateOne({ guildId: command.guild.id }, { $set: { stopped: true } }, { upsert: true });
            existingConnection.state.subscription.player.skipExecute = true;

            existingConnection.state.subscription.player.stop();

            setTimeout(async () => { existingConnection.state.subscription.player.skipExecute = false; }, 2000);

            return { code: "success", embed: skippedTrackEmbed };
        }

        await this.client.database.db("guilds").collection("players").updateOne({ guildId: command.guild.id }, { $set: { queueID: nextQueueID } });
        existingConnection.state.subscription.player.skipExecute = true;

        existingConnection.state.subscription.player.stop();
        this.client.player.updatePlayer(existingConnection, command.guild.id);

        setTimeout(async () => { existingConnection.state.subscription.player.skipExecute = false; }, 2000);
        
        return { code: "success", embed: skippedTrackEmbed };

    }

}