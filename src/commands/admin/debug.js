const Commands = require("../../structures/Commands");

const os = require('os');
const nodePackage = require('../../../package.json');

const { MessageEmbed } = require("discord.js");

module.exports = class extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "debug";
        this.aliases = [];

        this.description = "Shows current bot stats";
        this.category = "admin";

        this.usage = "debug";
        this.options = [];

        this.requiredPermissions = [];

        this.enabled = true;
        this.ownersOnly = true;
        this.ignoreSlash = true;
    }

    async runAsMessage(message) {

        const ramTotal = (os.totalmem() / 1024 / 1024 / 1024).toFixed(0) * 1024;
        const ramUsage = (process.memoryUsage().rss / 1024 / 1024).toFixed(2);

        const debugEmbed = new MessageEmbed({ color: message.guild.me.displayHexColor })
            .setAuthor({ name: `${this.client.user.username} v${nodePackage.version}`, iconURL: this.client.user.displayAvatarURL() })
            .setDescription(`Running for **${Math.round((this.client.uptime / 1000) / 3600)} hours** on **${os.hostname()}**.`)
            .addField(`${(ramUsage / ramTotal * 100).toFixed(1)}% of RAM`, `${ramUsage.toLocaleString()} / ${ramTotal.toLocaleString()} MB`, true)
            .addField(`${(os.loadavg()[0] * 100).toFixed(1)}% of CPU`, `${os.cpus().length}c @ ${(os.cpus()[0].speed / 1000).toFixed(1)}GHz`, true)
            .setFooter({ text: `using Node.js ${process.version}` })

        return message.channel.send({ embeds: [debugEmbed] })

    }

}