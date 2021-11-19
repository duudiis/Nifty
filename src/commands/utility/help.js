const Commands = require("../../structures/Commands");

const { MessageEmbed } = require("discord.js");

module.exports = class Help extends Commands {

	constructor(client) {
		super(client);
		this.client = client;

		this.name = "help";
		this.aliases = ["h", "botinfo", "info"];

		this.description = "Displays basic info about Nifty";
		this.category = "utility";

		this.usage = "help";
		this.options = []

		this.enabled = true;
	}

	async runAsMessage(message) {

		const response = await this.help(message);
		message.reply({ embeds: [ response.embed ] });

	}

	async runAsInteraction(interaction) {

		const response = await this.help(interaction);
		interaction.editReply({ embeds: [response.embed] })

	}

	async help(command) {

		let playCommand = "/play"

		const helpEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
			.setAuthor(this.client.user.username, this.client.user.displayAvatarURL())
			.setDescription(`${this.client.user.username} is the easiest way to play music in your Discord server. It supports YouTube and Spotify!\n\nTo get started, join a voice channel and \`${playCommand}\` a song. You can use song names, video links, and playlist links.\n᲼`)
			.addField("Invite", `${this.client.user.username} can be added to as many servers as you want! [Click here to add it to yours.](https://discord.com/oauth2/authorize?client_id=${this.client.user.id}&permissions=8&scope=bot%20applications.commands)`)

		return { code: "success", embed: helpEmbed };

	}

}