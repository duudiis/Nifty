const Events = require("../structures/Events");

const { MessageEmbed } = require("discord.js");

module.exports = class extends Events {

	constructor(client) {
		super(client);
		this.client = client;

		this.name = "interactionCreate"
	}

	async run(interaction) {

		const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error });

		if (interaction?.guild?.me?.isCommunicationDisabled()) {
			return interaction.reply({ embeds: [errorEmbed.setDescription("I do not have permission to **communicate** in this server!")] });
		};

		if (interaction.isCommand()) {

			const command = this.client.commands.get(interaction.commandName);

			if (!command || command.ignoreSlash) { return interaction.reply({ embeds: [errorEmbed.setDescription("Unknown command.")] }) };
			if (!command.enabled) { return interaction.reply({ embeds: [errorEmbed.setDescription("This command is currently disabled!")] }) };
			if (interaction.channel.type === "DM") { return interaction.reply({ embeds: [errorEmbed.setDescription("This command can only be run in a server!")] }) };

			let userPermissions = await this.checkUserPermissions(command, interaction);
			if (userPermissions.code != "success") { return };

			if (command.category == "music") {
				this.client.database.db("guilds").collection("players").updateOne({ guildId: interaction.guild.id }, { $set: { announcesId: interaction.channel.id } });
			}

			await interaction.deferReply();
			try { await command.runAsInteraction(interaction) } catch (error) { interaction.editReply({ embeds: [errorEmbed.setDescription("An error occurred")] }); console.log(error); };

		}

		if (interaction.isMessageComponent()) {

			interaction.array = interaction.customId.split("_");

			let interactionId = interaction.array[0];

			const interactionFile = this.client.interactions.get(interactionId);
			if (!interactionFile) { return };

			try { await interactionFile.run(interaction) } catch (error) { console.log(error); };

		}

		if (interaction.isMessageContextMenu()) {

			if (interaction.channel.type === "DM") { return interaction.reply({ embeds: [errorEmbed.setDescription("This command can only be run in a server!")] }) };

			const interactionFile = this.client.interactions.get(interaction.commandName);
			if (!interactionFile) { return };

			await interaction.deferReply();
			try { await interactionFile.run(interaction) } catch (error) { console.log(error); };

		}

	}

	async checkUserPermissions(command, interaction) {

		const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error });

		const userPerms = await this.client.getUserPerms(interaction.guild.id, interaction.member.id);

		if (!command.requiredPermissions.every(permission => userPerms.array.includes(permission))) {
			try { await interaction.reply({ embeds: [errorEmbed.setDescription("You do not have permission to use this command!")] }); } catch (error) { };
			return { code: "error" };
		}

		return { code: "success" };

	}

}