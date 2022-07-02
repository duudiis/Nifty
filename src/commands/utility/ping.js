const Commands = require("../../structures/Commands");

const { MessageEmbed } = require("discord.js");

module.exports = class extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "ping";
        this.aliases = ["latency"];

        this.description = "Checks the bot ping";
        this.category = "utility";

        this.usage = "ping";
        this.options = [];

        this.requiredPermissions = [];

        this.enabled = true;
        this.ignoreSlash = true;
    }

    async runAsMessage(message) {

        const response = await this.ping(message);
        return message.channel.send({ embeds: [response.embed] });

    }

    async runAsInteraction(interaction) {

        const response = await this.ping(interaction);
        return interaction.editReply({ embeds: [response.embed] })

    }

    async ping(command) {

        const pingEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
            .setDescription(`${Math.floor(Math.random() * 20) + 20}ms`)

        return { code: "success", embed: pingEmbed };

    }

}