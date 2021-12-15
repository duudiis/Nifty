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
        this.options = []

        this.enabled = true;
        this.ignoreSlash = true;
    }

    async runAsMessage(message) {

        const response = await this.ping(message);
        message.reply({ embeds: [response.embed] });

    }

    async runAsInteraction(interaction) {

        const response = await this.ping(interaction);
        interaction.editReply({ embeds: [response.embed] })

    }

    async ping(command) {

        const pingEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
            .setDescription(`${Math.round(this.client.ws.ping)}ms`)

        return { code: "success", embed: pingEmbed };

    }

}