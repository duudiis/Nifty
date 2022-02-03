const Commands = require("../../structures/Commands");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "remove";
        this.aliases = ["r", "rmv"];

        this.description = "Removes the specified song";
        this.category = "music";

        this.usage = "remove";
        this.options = [
            {
                "name": "input",
                "description": "The position or title of the track you want to remove",
                "type": "STRING",
                "required": true
            }
        ];

        this.requiredPermissions = ["MANAGE_QUEUE"];

        this.enabled = true;
    }

    async runAsMessage(message) {

        const input = message.array.slice(1).join(" ");
        if (!input) { return };

        const response = await this.remove(input, message);
        return message.channel.send({ embeds: [response.embed] });

    }

    async runAsInteraction(interaction) {

        const input = interaction.options.get("input").value;

        const response = await this.remove(input, interaction);
        return interaction.editReply({ embeds: [response.embed] });

    }

    async remove(input, command) {

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

        let removeTarget = undefined;
        try { removeTarget = await this.client.player.findTrack(input, command.guild.id) } catch (error) { return { code: "error", embed: errorEmbed.setDescription(`${error.message ? error.message : error}`) }; };

        if (!removeTarget) { return { code: "error", embed: errorEmbed.setDescription(`A track could not be found for "${input}"!`) }; };

        await this.client.database.db("queues").collection(command.guild.id).deleteMany({ url: removeTarget.track.url });

        if (playerData.queueID >= removeTarget.id) { await this.client.database.db("guilds").collection("players").updateOne({ guildId: command.guild.id }, { $set: { queueID: playerData.queueID - 1 } }, { upsert: true }); };
        if (existingConnection?.state?.subscription?.player?.state?.resource?.metadata?.url == removeTarget.track.url) { existingConnection.state.subscription.player.stop() };

        const removedEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
            .setDescription(`Removed [${await this.client.removeFormatting(removeTarget.track.title, 54)}](${removeTarget.track.url}) [<@${removeTarget.track.user}>]`)

        return { code: "success", embed: removedEmbed };

    }

}