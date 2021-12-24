const Commands = require("../../structures/Commands");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "move";
        this.aliases = ["m", "moveto", 'mov'];

        this.description = "Move a song to a new position";
        this.category = "music";

        this.usage = "move { track } { position }";
        this.options = [
            {
                "name": "track",
                "description": "The track to move",
                "type": "STRING",
                "required": true
            },
            {
                "name": "position",
                "description": "The new position to move the track to",
                "type": "INTEGER",
                "required": true
            }
        ];

        this.requiredPermissions = ["Manage Queue"];

        this.enabled = true;
    }

    async runAsMessage(message) {

        const track = message.array.slice(1, -1).join(" ");
        const position = message.array[message.array.length - 1];

        if (!track || track == "") { return; };
        if (!position) { return; };

        const response = await this.move(track, position, message);
        return message.reply({ embeds: [response.embed] });

    }

    async runAsInteraction(interaction) {

        const track = interaction.options.get("track").value;
        const position = interaction.options.get("position").value.toString();

        const response = await this.move(track, position, interaction);
        return interaction.editReply({ embeds: [response.embed] });

    }

    async move(track, position, command) {

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
        const queueData = await this.client.database.db("queues").collection(command.guild.id).find({}).toArray();

        let moveTarget = undefined;
        try { moveTarget = await this.client.player.findTrack(track, command.guild.id) } catch (error) { return { code: "error", embed: errorEmbed.setDescription(`${error.message ? error.message : error}`) }; };

        if (!moveTarget) { return { code: "error", embed: errorEmbed.setDescription(`A track could not be found for "${input}"!`) }; };

        let originalPosition = position;

        if (this.client.constants.keywords.first.includes(position.toLowerCase())) { position = "1" };
        if (this.client.constants.keywords.next.includes(position.toLowerCase())) { position = `${playerData.queueID + 1}` };
        if (this.client.constants.keywords.current.includes(position.toLowerCase())) { position = `${playerData.queueID + 1}` };
        if (this.client.constants.keywords.back.includes(position.toLowerCase())) { position = `${playerData.queueID}` };
        if (this.client.constants.keywords.last.includes(position.toLowerCase())) { position = `${queueData.length}` };

        if (parseInt(position) != position) { return { code: "error", embed: errorEmbed.setDescription(`The new position "${originalPosition}" is not valid!`) }; }
        if (parseInt(position) > queueData.length || parseInt(position) < 1) { return { code: "error", embed: errorEmbed.setDescription(`The new position "${originalPosition}" is not valid!`) }; }

        await this.moveTrack(command.guild.id, moveTarget, position - 1);

        const moveEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
            .setDescription(`Moved **${await this.client.removeFormatting(moveTarget.track.title, Infinity)}** to position **${parseInt(position)}**`)

        return { code: "success", embed: moveEmbed };

    }

    async moveTrack(guildId, moveTarget, newPosition) {

        if (moveTarget.id == newPosition) { return; };

        await this.client.database.db("queues").collection(guildId).deleteOne({ _id: moveTarget.track._id });

        const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: guildId });
        const queueData = await this.client.database.db("queues").collection(guildId).find({}).toArray();

        queueData.splice(newPosition, 0, moveTarget.track);

        await this.client.database.db("queues").collection(guildId).drop().catch(e => { });
        await this.client.database.db("queues").collection(guildId).insertMany(queueData);

        if (moveTarget.id == playerData.queueID) { await this.client.database.db("guilds").collection("players").updateOne({ guildId: guildId }, { $set: { queueID: newPosition } }, { upsert: true }); }
        else if (moveTarget.id < playerData.queueID && newPosition >= playerData.queueID) { await this.client.database.db("guilds").collection("players").updateOne({ guildId: guildId }, { $set: { queueID: playerData.queueID - 1 } }, { upsert: true }); }
        else if (moveTarget.id > playerData.queueID && newPosition <= playerData.queueID) { await this.client.database.db("guilds").collection("players").updateOne({ guildId: guildId }, { $set: { queueID: playerData.queueID + 1 } }, { upsert: true }); }

    }

}