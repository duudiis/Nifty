const Events = require("../structures/Events");

const { MessageEmbed } = require("discord.js");

module.exports = class InteractionCreate extends Events {

	constructor(client) {
		super(client);
		this.client = client;

		this.name = "interactionCreate"
	}

	async run(interaction) {

		if (interaction.isCommand()) {

			const command = this.client.commands.get(interaction.commandName);

			if (command.ignoreSlash) { return };

			if (!command) { return this.unknownCommand(interaction) };
			if (!command.enabled) { return this.commandDisabled(interaction) };
			if (interaction.channel.type === "DM") { return this.DmCommand(interaction) };

			await interaction.deferReply();
			try { await command.runAsInteraction(interaction) } catch (error) { this.commandError(interaction); console.log(error) };

		} else {

			let interactionId = interaction.customId;

			if (interaction.customId.includes("queue")) { interactionId = "queue" };
			if (interaction.customId.includes("search")) { interactionId = "search" };

			const interactionFile = this.client.interactions.get(interactionId);
			if (!interactionFile) { return };

			try { await interactionFile.run(interaction) } catch (error) { console.log(error) };

		}

	}

	async unknownCommand(interaction) {

		const unknownEmbed = new MessageEmbed({ color: this.client.constants.colors.error })
			.setDescription(`Unknown command`)

		interaction.reply({ embeds: [unknownEmbed] })

	}

	async commandDisabled(interaction) {

		const disabledEmbed = new MessageEmbed({ color: this.client.constants.colors.error })
			.setDescription(`This command is currently disabled!`)

		interaction.reply({ embeds: [disabledEmbed] })

	}

	async DmCommand(interaction) {

		const DmEmbed = new MessageEmbed({ color: this.client.constants.colors.error })
			.setDescription(`You can't use commands in my DMs`)

		interaction.reply({ embeds: [DmEmbed] })

	}

	async commandError(interaction) {

		const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error })
			.setDescription(`An error occurred`)

		interaction.editReply({ embeds: [errorEmbed] })

	}

}