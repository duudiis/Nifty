const Commands = require("../../structures/Commands");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class extends Commands {

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
        ];

        this.requiredPermissions = ["Manage Player", "View Queue"];

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

        let jumpTarget = undefined;
        try { jumpTarget = await this.client.player.findTrack(input, command.guild.id) } catch (error) { return { code: "error", embed: errorEmbed.setDescription(`${error.message ? error.message : error}`) }; };

        if (!jumpTarget) { return { code: "error", embed: errorEmbed.setDescription(`A track could not be found for "${input}"!`) }; };

        await this.client.database.db("guilds").collection("players").updateOne({ guildId: command.guild.id }, { $set: { queueID: jumpTarget.id } });
        existingConnection.state.subscription.player.skipExecute = true;

        existingConnection.state.subscription.player.stop();
        this.client.player.updatePlayer(existingConnection, command.guild.id);

        setTimeout(async () => { existingConnection.state.subscription.player.skipExecute = false; }, 2000);

        const jumpedEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
            .setDescription(`Jumped to [${await this.client.removeFormatting(jumpTarget.track.title, 54)}](${jumpTarget.track.url}) [<@${jumpTarget.track.user}>]`)

        return { code: "success", embed: jumpedEmbed };

    }

}