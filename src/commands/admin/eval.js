const Commands = require("../../structures/Commands");

const os = require('os');
const util = require('util')
const nodePackage = require('../../../package.json');

const { MessageEmbed } = require("discord.js");

module.exports = class extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "eval";
        this.aliases = ["evaluate", "ev"];

        this.description = "Evaluates a JavaScript code";
        this.category = "admin";

        this.usage = "eval { code }";
        this.options = [];

        this.requiredPermissions = [];

        this.enabled = true;
        this.ownersOnly = true;
        this.ignoreSlash = true;
    }

    async runAsMessage(message) {

        const code = message.array.slice(1).join(" ");
        const result = await eval(code);
        
        const evalEmbed = new MessageEmbed({ color: message.guild.me.displayHexColor })
            .setDescription(`${util.inspect(result)}`)

        message.channel.send({ embeds: [evalEmbed] });

    }

}