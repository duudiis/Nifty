const Modules = require("../structures/Modules");

const { MessageEmbed } = require("discord.js");

module.exports = class extends Modules {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "logError";
    }

    async run(error, command) {

        const guild = await this.client.guilds.fetch(this.client.constants.logs.guildId);
        const channel = await guild.channels.fetch(this.client.constants.logs.channelId);

        let errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error })
            .setTitle("An error ocurred")
            .setDescription(`\`\`\`${error}\`\`\``)

        if (command) {
            errorEmbed.addField("Server", `${command?.guild?.name}`, true);
            errorEmbed.addField("Channel", `${command?.channel?.name}`, true);
            errorEmbed.addField("User", `${command?.member?.user?.tag}`, true);
        } else {
            errorEmbed.setFooter({ text: "No further information." })
        }
        
        await channel.send({ embeds: [errorEmbed] }).catch(e => { });

    }

}