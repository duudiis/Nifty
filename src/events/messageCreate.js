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

        let mention = await this.checkMention(message);

        if (mention.only) {
            let permission = await this.checkPermissions(message);
            if (permission.code != "success") { return };

            return this.getStarted(message);
        };

        const prefix = mention.starts ? mention.mention : await this.client.getPrefix(message.guildId);
        if (!message.content.startsWith(prefix)) { return; };

        message.formatted = message.content.slice(prefix.length).trim().replace(/ +/g, " ");
        message.array = message.formatted.split(" ");

        const commandName = message.array[0].toLowerCase();
        const command = this.client.commands.get(commandName) || this.client.commands.get(this.client.aliases.get(commandName));

        if (!command) { return };
        if (!command.enabled) { return };
        if (command.ownersOnly && !this.client.constants.owners.includes(message.author.id)) { return };

        let permission = await this.checkPermissions(message);
        if (permission.code != "success") { return };

        if (command.category == "music") {
            this.client.database.db("guilds").collection("players").updateOne({ guildId: message.guild.id }, { $set: { announcesId: message.channel.id } });
        }

        try { await command.runAsMessage(message) } catch (error) { this.commandError(message); console.log(error) };

    }

    async getStarted(message) {

        let playCommand = "/play"
        if (!message.channel.permissionsFor(command.member.id).has("USE_APPLICATION_COMMANDS")) { playCommand = `${await this.client.getPrefix(command.guild.id)}play`; };

        const getStartedEmbed = new MessageEmbed({ color: "#202225" })
            .setDescription(`You can play music by joining a voice channel and typing \`${playCommand}\`. The command accepts song names, video links, and playlist links.`)

        message.reply({ embeds: [getStartedEmbed] })

    }

    async checkMention(message) {

        let mention = null;

        let only = false;
        let starts = false;

        if (message.content == `<@${this.client.user.id}>`) { mention = `<@${this.client.user.id}>`; only = true; };
        if (message.content == `<@!${this.client.user.id}>`) { mention = `<@!${this.client.user.id}>`; only = true; };

        if (message.content.startsWith(`<@${this.client.user.id}>`)) { mention = `<@${this.client.user.id}>`; starts = true };
        if (message.content.startsWith(`<@!${this.client.user.id}>`)) { mention = `<@!${this.client.user.id}>`; starts = true };

        return { mention, only, starts };

    }

    async checkPermissions(message) {

        const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error });

        if (!message.channel.permissionsFor(this.client.user.id).has("SEND_MESSAGES")) {
            try { await message.author.send({ embeds: [errorEmbed.setDescription("I do not have permission to **send messages** in that channel!")] }); } catch (error) { }
            return { code: "error", missing: "SEND_MESSAGES" };
        };

        if (!message.channel.permissionsFor(this.client.user.id).has("EMBED_LINKS")) {
            try { await message.author.send({ embeds: [errorEmbed.setDescription("I do not have permission to **embed links** in this channel!")] }); } catch (error) { }
            return { code: "error", missing: "EMBED_LINKS" };
        };

        if (!message.channel.permissionsFor(this.client.user.id).has("ADD_REACTIONS")) {
            try { await message.reply({ embeds: [errorEmbed.setDescription("I do not have permission to **add reactions** in this channel!")] }); } catch (error) { }
            return { code: "error", missing: "ADD_REACTIONS" };
        };

        return { code: "success" };

    }

    async commandError(message) {

        const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error })
            .setDescription(`An error occurred`)

        message.reply({ embeds: [errorEmbed] })

    }

}