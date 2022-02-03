const Commands = require("../../structures/Commands");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "skip";
        this.aliases = ["next", "n"];

        this.description = "Skips to the next song";
        this.category = "music";

        this.usage = "skip";
        this.options = [];

        this.requiredPermissions = ["MANAGE_PLAYER"];

        this.enabled = true;
    }

    async runAsMessage(message) {

        const response = await this.skip(message);

        if (response.code == "error") { return message.channel.send({ embeds: [response.embed] }); };
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

        this.stopPlayer(existingConnection);

        let playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: command.guild.id });
        let queueData = await this.client.database.db("queues").collection(command.guild.id).find({}).toArray();

        let nextQueueID = playerData.queueID + 1;
        let nextQueue = queueData[nextQueueID];

        if (!nextQueue && playerData.autoplay == "on") {

            try { await this.client.player.getAutoplayTrack(command.guild.id); } catch (error) {

                await this.client.database.db("guilds").collection("players").updateOne({ guildId: command.guild.id }, { $set: { stopped: true } }, { upsert: true })

                let errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error })
                    .setDescription(`${error.message ? error.message : error}`)

                return { code: "error", embed: errorEmbed };

            };

            queueData = await this.client.database.db("queues").collection(command.guild.id).find({}).toArray();

            nextQueueID = queueData.length - 1;
            nextQueue = queueData[nextQueueID];

        }

        if (!nextQueue && playerData.loop == "queue") {

            if (playerData.shuffle == "on") {
                let newQueue = await this.client.shuffleArray(queueData);
                
                await this.client.database.db("queues").collection(command.guild.id).drop().catch(e => {});
                await this.client.database.db("queues").collection(command.guild.id).insertMany(newQueue);
            }

            nextQueueID = 0;
            nextQueue = queueData[0];

        }

        if (!nextQueue) {
            await this.client.database.db("guilds").collection("players").updateOne({ guildId: command.guild.id }, { $set: { stopped: true } }, { upsert: true });
            return { code: "success", embed: skippedTrackEmbed };
        }

        await this.client.database.db("guilds").collection("players").updateOne({ guildId: command.guild.id }, { $set: { queueID: nextQueueID } });

        this.client.player.updatePlayer(existingConnection, command.guild.id);

        return { code: "success", embed: skippedTrackEmbed };

    }

    async stopPlayer(connection) {

        connection.state.subscription.player.skipExecute = true;

        connection.state.subscription.player.stop();

        setTimeout(async () => { connection.state.subscription.player.skipExecute = false; }, 2000);

    }

}