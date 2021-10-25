const Events = require("../structures/Events");

const { MessageEmbed } = require("discord.js");

module.exports = class MessageCreate extends Events {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "messageCreate"
    }

    async run(message) {

        if (message.author.bot) { return };
        if (message.channel.type === "DM") { return };

        if (message.content == `<@!${this.client.user.id}>`) { return this.getStarted(message) };

        const prefix = message.content.startsWith(`<@!${this.client.user.id}>`) ? `<@!${this.client.user.id}>` : await this.client.getPrefix(message.guildId);
        if (!message.content.startsWith(prefix)) { return };

        message.formatted = message.content.slice(prefix.length).trim().replace(/ +/g, " ");
        message.array = message.formatted.split(" ");

        const commandName = message.array[0].toLowerCase();
        const command = this.client.commands.get(commandName) || this.client.commands.get(this.client.aliases.get(commandName));

        if (!command) { return };
        if (!command.enabled) { return };
        if (command.ownersOnly && !this.client.constants.owners.includes(message.author.id)) { return };

        try { await command.runAsMessage(message) } catch (error) { this.commandError(message); console.log(error) };

    }

    async getStarted(message) {

        const getStartedEmbed = new MessageEmbed({ color: "#202225" })
            .setDescription(`You can play music by joining a voice channel and typing \`/play\`. The command accepts song names, video links, and playlist links.`)

        message.reply({ embeds: [getStartedEmbed] })

    }

    async commandError(message) {

        const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error })
            .setDescription(`An error occurred`)

        message.reply({ embeds: [errorEmbed] })

    }

}