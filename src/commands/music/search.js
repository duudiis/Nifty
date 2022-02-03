const Commands = require("../../structures/Commands");

const DiscordVoice = require('@discordjs/voice');
const { MessageSelectMenu, MessageActionRow, MessageEmbed } = require("discord.js");

const ytsr = require('ytsr');

module.exports = class extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "search";
        this.aliases = ["s", "src"];

        this.description = "Searches for the input and returns a list of results for you to pick from";
        this.category = "music";

        this.usage = "search { input }";
        this.options = [
            {
                "name": "input",
                "description": "The artist name or track title to search for",
                "type": "STRING",
                "required": true
            }
        ];

        this.requiredPermissions = ["ADD_TO_QUEUE"];

        this.enabled = true;
    }

    async runAsMessage(message) {

        const input = message.array.slice(1).join(" ");
        if (!input) { return };

        const response = await this.search(input, message);
        return message.channel.send(response.reply);

    }

    async runAsInteraction(interaction) {

        const input = interaction.options.get("input").value;

        const response = await this.search(input, interaction);
        return interaction.editReply(response.reply);

    }

    async search(input, command) {

        const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error });

        const voiceChannel = command.member.voice.channel;
        if (!voiceChannel) { return { code: "error", reply: { embeds: [errorEmbed.setDescription("You have to be connected to a voice channel before you can use this command!")] } } };

        let existingConnection = DiscordVoice.getVoiceConnection(command.guild.id);
        if (existingConnection && existingConnection.joinConfig.channelId != voiceChannel.id) { return { code: "error", reply: { embeds: [errorEmbed.setDescription("Someone else is already listening to music in different channel!")] } } };

        if (!existingConnection) {
            try { await this.client.player.joinChannel(voiceChannel, command) } catch (error) { return { code: "error", reply: { embeds: [errorEmbed.setDescription(`${error.message ? error.message : error}`)] } }; };
            existingConnection = DiscordVoice.getVoiceConnection(command.guild.id);
        };

        const searchResults = await ytsr(input, { pages: 1 });

        const videos = searchResults.items.filter(video => video.type == "video");
        if (!videos) { return { code: "error", reply: { embeds: [errorEmbed.setDescription("No matches found!")] } } };

        const SmOptions = [];

        for (const video of videos) {

            if (SmOptions.length >= 25) { continue; };

            let option = {
                label: video.title.length > 80 ? video.title.slice(0, 80) + "..." : video.title,
                description: `${video.author.name} · ${video.duration}`,
                value: video.url
            }

            SmOptions.push(option);

        }

        const searchEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
            .setDescription("Select the tracks you want to add to the queue.")

        const searchSelectMenu = new MessageSelectMenu()
            .setCustomId(`search${command.member.id}`)
            .setMaxValues(SmOptions.length)
            .setPlaceholder('Make a selection')
            .addOptions(SmOptions)

        const searchRow = new MessageActionRow().addComponents(searchSelectMenu);

        return { code: "success", reply: { embeds: [searchEmbed], components: [searchRow] } };


    }

}