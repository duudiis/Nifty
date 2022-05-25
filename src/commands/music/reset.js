const Commands = require("../../structures/Commands");

module.exports = class extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "reset";
        this.aliases = [];

        this.description = "Resets the player, clears the queue, and leaves the voice channel";
        this.category = "music";

        this.usage = "reset";
        this.options = [];

        this.requiredPermissions = ["MANAGE_QUEUE", "MANAGE_PLAYER"];

        this.enabled = true;
    }

    async runAsMessage(message) {

        return await this.client.commands.get("disconnect").runAsMessage(message);

    }

    async runAsInteraction(interaction) {

        return await this.client.commands.get("disconnect").runAsInteraction(interaction);

    }

}