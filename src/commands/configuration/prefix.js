const Commands = require("../../structures/Commands");

const { MessageEmbed } = require("discord.js");

module.exports = class Prefix extends Commands {

	constructor(client) {
		super(client);
		this.client = client;

		this.name = "prefix";
		this.aliases = ["pr"];

		this.description = "Changes the prefix";
		this.category = "configuration";

		this.usage = "prefix [ new prefix ]";
		this.options = [
			{
				"name": "input",
				"description": "The new prefix you want to use",
				"type": "STRING",
				"required": false
			}
		]

		this.enabled = true;
		this.ignoreSlash = true;
	}

	async runAsMessage(message) {

		let newPrefix = message.array[1];

		if (!newPrefix) {

			const currentPrefixEmbed = new MessageEmbed({ color: message.guild.me.displayHexColor })
				.setDescription(`This server's prefix is **${await this.client.getPrefix(message.guildId)}**`)

			return message.reply({ embeds: [currentPrefixEmbed] });
		};

		if (!message.member.permissions.has("MANAGE_GUILD")) {

			const missingPermsEmbed = new MessageEmbed({ color: message.guild.me.displayHexColor })
				.setDescription("You must have the `Manage Server` permission to use this command!")

			return message.reply({ embeds: [missingPermsEmbed] });
		};

		const prefixEmbed = new MessageEmbed({ color: message.guild.me.displayHexColor })
			.setDescription(`This server's prefix is now **${newPrefix}**. Commands must now use **${newPrefix}** as their prefix. For example, \`${newPrefix}play\`.`)

		if (newPrefix == "reset") { newPrefix = process.env.DEFAULT_PREFIX; prefixEmbed.setDescription("This server's prefix has been reset.") };

		await this.client.database.db("default").collection("guilds").updateOne({ id: message.guildId }, { $set: { prefix: newPrefix } }, { upsert: true })

		return message.reply({ embeds: [prefixEmbed] });

	}

}