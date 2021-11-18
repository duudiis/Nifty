const Commands = require("../../structures/Commands");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class Jump extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "jump";
        this.aliases = ["j", "goto", "jumpto", "skipto"];

        this.description = "Jumps to the specified song in the queue";
        this.category = "music";

        this.usage = "jump { input }";
        this.options = [
            {
                "name": "input",
                "description": "The position or title of the track you want to jump to",
                "type": "STRING",
                "required": true
            }
        ]

        this.enabled = true;
    }

    async runAsMessage(message) {

        const input = message.array.slice(1).join(" ");
        if (!input) { return };

        const response = await this.jump(input, message);

        if (response.code == "error") { return message.reply({ embeds: [response.embed] }); };
        if (response.code == "success") { return message.react("👌") };

    }

    async runAsInteraction(interaction) {

        const input = interaction.options.get("input").value;

        const response = await this.jump(input, interaction);
        return interaction.editReply({ embeds: [response.embed] });

    }

    async jump(input, command) {

        const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error });

        const voiceChannel = command.member.voice.channel;
        if (!voiceChannel) { return { code: "error", embed: errorEmbed.setDescription("You have to be connected to a voice channel before you can use this command!") } };

        let existingConnection = DiscordVoice.getVoiceConnection(command.guild.id);
        if (existingConnection && existingConnection.joinConfig.channelId != voiceChannel.id) { return { code: "error", embed: errorEmbed.setDescription("Someone else is already listening to music in different channel!") } };

        if (!existingConnection) { 
            try { await this.client.player.joinChannel(voiceChannel, command) } catch (error) { return { code: "error", embed: errorEmbed.setDescription(`${error.message ? error.message : error}`) }; };
            existingConnection = DiscordVoice.getVoiceConnection(command.guild.id); 
        };

        const queueData = await this.client.database.db("queues").collection(command.guild.id).find({}).toArray();

        if (input == "first") { input = "1" };
        if (input == "last") { input = `${queueData.length}` };

        let jumpTrack = null;
        let jumpId = null;

        if (input.match(/^(\+|-)[0-9]+/)) {

            const inputInt = parseInt(input);
            if (inputInt == 0) { return { code: "error", embed: errorEmbed.setDescription(`A track could not be found for "${input}"!`) } }

            const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: command.guild.id });

            inputInt > 0 ? jumpId = playerData.queueID + inputInt : jumpId = playerData.queueID - Math.abs(inputInt);
            jumpTrack = queueData[jumpId];

        } else if (parseInt(input) == input) {

            jumpId = parseInt(input) - 1;
            jumpTrack = queueData[jumpId];

        } else {

            jumpTrack = queueData.find(track => track.title.toLowerCase().includes(input.toLowerCase()));
            if (!jumpTrack) { return { code: "error", embed: errorEmbed.setDescription(`A track could not be found for "${input}"!`) } };

            jumpId = queueData.map(track => track.url).indexOf(jumpTrack.url);
            if (!jumpId) { return { code: "error", embed: errorEmbed.setDescription(`A track could not be found for "${input}"!`) } };
        }

        if (!jumpTrack) { return { code: "error", embed: errorEmbed.setDescription(`A track could not be found for "${input}"!`) } };

        await this.client.database.db("guilds").collection("players").updateOne({ guildId: command.guild.id }, { $set: { queueID: jumpId } });
        existingConnection.state.subscription.player.skipExecute = true;

        existingConnection.state.subscription.player.stop();
        this.client.player.updatePlayer(existingConnection, command.guild.id);

        setTimeout(async () => { existingConnection.state.subscription.player.skipExecute = false; }, 4000);

        const jumpedEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
            .setDescription(`Jumped to [${await this.client.removeFormatting(jumpTrack.title, 54)}](${jumpTrack.url}) [<@${jumpTrack.user}>]`)

        return { code: "success", embed: jumpedEmbed };

    }

}