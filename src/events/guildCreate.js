const Events = require("../structures/Events");

const { MessageEmbed } = require("discord.js");

module.exports = class extends Events {

	constructor(client) {
		super(client);
		this.client = client;

		this.name = "guildCreate"
	}

	async run(guild) {

		const welcomeEmbed = new MessageEmbed({ color: "#202225" })
			.setTitle("Thanks for adding me to your server! :blush:")
			.setDescription("To get started, join a voice channel and type `/play` to play a song! You can use song names, video links, and playlist links.")

		const channel = guild.channels.cache.find(channel => channel.type == "GUILD_TEXT" && channel.permissionsFor(guild.me).has("SEND_MESSAGES"));
		if (!channel) { return };
		
		channel.send({ embeds: [welcomeEmbed] });

	}

}