const Commands = require("../../structures/Commands");

const { MessageButton, MessageActionRow } = require("discord.js");

module.exports = class extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "invite";
        this.aliases = ["inv"];

        this.description = "Add the bot to another server";
        this.category = "utility";

        this.usage = "invite";
        this.options = [];

        this.requiredPermissions = [];

        this.enabled = true;
    }

    async runAsMessage(message) {

        const response = await this.invite(message);
        return message.channel.send(response.reply);

    }

    async runAsInteraction(interaction) {

        const response = await this.invite(interaction);
        return interaction.editReply(response.reply)

    }

    async invite(command) {

        const inviteButton = new MessageButton()
            .setLabel("Invite")
            .setStyle("LINK")
            .setURL(`https://discord.com/oauth2/authorize?client_id=${this.client.user.id}&permissions=8&scope=bot%20applications.commands`)

        const buttonsRow = new MessageActionRow().addComponents(inviteButton);

        return { code: "success", reply: { content: "​", components: [buttonsRow] } };

    }

}