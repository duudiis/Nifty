const Events = require("../structures/Events");

const { MessageEmbed } = require("discord.js");

module.exports = class extends Events {

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
            let permission = await this.checkBotPermissions(message);
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

        let botPermissions = await this.checkBotPermissions(message);
        if (botPermissions.code != "success") { return };

        let userPermissions = await this.checkUserPermissions(command, message);
        if (userPermissions.code != "success") { return };

        if (command.category == "music") {
            this.client.database.db("guilds").collection("players").updateOne({ guildId: message.guild.id }, { $set: { announcesId: message.channel.id } });
        }

        try { await command.runAsMessage(message) } catch (error) { this.commandError(message); this.client.logError(error, message).catch(e => { });; };
        this.slashCommands(message);

    }

    async getStarted(message) {

        let playCommand = "/play"
        if (!message.channel.permissionsFor(message.member.id).has("USE_APPLICATION_COMMANDS")) { playCommand = `${await this.client.getPrefix(message.guild.id)}play`; };

        const getStartedEmbed = new MessageEmbed({ color: "#202225" })
            .setDescription(`You can play music by joining a voice channel and typing \`${playCommand}\`. The command accepts song names, video links, and playlist links.`)

        message.channel.send({ embeds: [getStartedEmbed] });

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

    async checkBotPermissions(message) {

        const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error });

        if (message.guild.me.isCommunicationDisabled()) {
            try { await message.author.send({ embeds: [errorEmbed.setDescription("I do not have permission to **communicate** in that server!")] }); } catch (error) { };
            return { code: "error", missing: "COMMUNICATE" };
        };

        if (!message.channel.permissionsFor(this.client.user.id).has("SEND_MESSAGES")) {
            try { await message.author.send({ embeds: [errorEmbed.setDescription("I do not have permission to **send messages** in that channel!")] }); } catch (error) { };
            return { code: "error", missing: "SEND_MESSAGES" };
        };

        if (!message.channel.permissionsFor(this.client.user.id).has("READ_MESSAGE_HISTORY")) {
            try { await message.author.send({ embeds: [errorEmbed.setDescription("I do not have permission to **read message history** in that channel!")] }); } catch (error) { };
            return { code: "error", missing: "READ_MESSAGE_HISTORY" };
        };

        if (!message.channel.permissionsFor(this.client.user.id).has("EMBED_LINKS")) {
            try { await message.author.send({ embeds: [errorEmbed.setDescription("I do not have permission to **embed links** in that channel!")] }); } catch (error) { };
            return { code: "error", missing: "EMBED_LINKS" };
        };

        if (!message.channel.permissionsFor(this.client.user.id).has("ADD_REACTIONS")) {
            try { await message.channel.send({ embeds: [errorEmbed.setDescription("I do not have permission to **add reactions** in this channel!")] }); } catch (error) { };
            return { code: "error", missing: "ADD_REACTIONS" };
        };

        return { code: "success" };

    }

    async checkUserPermissions(command, message) {

        const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error });

        const userPerms = await this.client.getUserPerms(message.guild.id, message.member.id);

        if (!command.requiredPermissions.every(permission => userPerms.array.includes(permission))) {
            try { await message.channel.send({ embeds: [errorEmbed.setDescription("You do not have permission to use this command!")] }); } catch (error) { };
            return { code: "error" };
        }

        return { code: "success" };

    }

    async commandError(message) {

        const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error })
            .setDescription(`An error occurred`)

        message.channel.send({ embeds: [errorEmbed] });

    }

    async slashCommands(message) {

        const slashEmbed = new MessageEmbed({ color: message.guild.me.displayHexColor })
            .setTitle("Slash commands are here!")
            .setDescription("Discord added a cool new way to use bot commands right inside your Discord! To use them, just press `/` :blush: For example, `/play`. To learn more about slash commands, [click here](https://support.discord.com/hc/en-us/articles/1500000368501).")

        let random = Math.random();

        if (random < 0.05) {
            return message.channel.send({ embeds: [slashEmbed] });
        }

    }

}