const Interactions = require("../structures/Interactions");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class extends Interactions {

	constructor(client) {
		super(client);
		this.client = client;

		this.name = "Add to Queue";
        this.type = "MESSAGE";
	}

	async run(interaction) {

		return await this.client.commands.get("play").runAsInteraction(interaction);

	}

}