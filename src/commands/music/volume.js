const Commands = require("../../structures/Commands");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "volume";
        this.aliases = ["vol", "v"];

        this.description = "Change or display the volume";
        this.category = "music";

        this.usage = "volume [ value ]";
        this.options = [
            {
                "name": "value",
                "description": "The value to set the volume to",
                "type": "INTEGER"
            }
        ]

        this.enabled = true;
    }

    async runAsMessage(message) {

        const value = message.array[1];

        if (!value) {

            const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: message.guild.id });

            let currentVolume = playerData?.volume;
            if (!currentVolume) { currentVolume = 100; };

            const volumeEmbed = new MessageEmbed({ color: message.guild.me.displayHexColor })
                .setDescription(`**${currentVolume}%**`)

            return message.reply({ embeds: [volumeEmbed] });
        }

        const response = await this.volume(value, message);
        return message.reply({ embeds: [response.embed] });

    }

    async runAsInteraction(interaction) {

        const value = interaction.options.get("value");

        if (!value) {

            const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: interaction.guild.id });

            let currentVolume = playerData?.volume;
            if (!currentVolume) { currentVolume = 100; };

            const volumeEmbed = new MessageEmbed({ color: interaction.guild.me.displayHexColor })
                .setDescription(`**${currentVolume}%**`)

            return interaction.editReply({ embeds: [volumeEmbed] });
        }

        const response = await this.volume(value.value.toString(), interaction);
        return interaction.editReply({ embeds: [response.embed] });

    }

    async volume(value, command) {

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

        let currentVolume = playerData?.volume;
        if (!currentVolume) { currentVolume = 100; };

        let newVolume = undefined;

        if (value.match(/^(\+|-)[0-9]+/)) {

            parseInt(value) > 0 ? newVolume = currentVolume + parseInt(value) : newVolume = currentVolume - Math.abs(parseInt(value));

        } else if (parseInt(value) == value) {

            newVolume = parseInt(value);

        } else if (value.includes("reset")) { newVolume = 100; };

        if (!newVolume && newVolume != 0) { return { code: "error", embed: errorEmbed.setDescription("The value you inputted is not a valid number!") } };

        if (newVolume > 400) { newVolume = 400 };
        if (newVolume < 0) { newVolume = 0 };

        const currentResource = existingConnection?.state?.subscription?.player?.state?.resource;
        if (currentResource) { currentResource.volume.setVolume(newVolume / 100); };

        await this.client.database.db("guilds").collection("players").updateOne({ guildId: command.guild.id }, { $set: { volume: newVolume } }, { upsert: true });

        const newVolumeEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
            .setDescription(`The volume has been changed from **${currentVolume}%** to **${newVolume}%**`)

        return { code: "success", embed: newVolumeEmbed };

    }

}