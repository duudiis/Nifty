const Commands = require("../../structures/Commands");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class extends Commands {

	constructor(client) {
		super(client);
		this.client = client;

		this.name = "stop";
		this.aliases = ["st"];

		this.description = "Stops the music";
		this.category = "music";

		this.usage = "stop";
		this.options = []

		this.enabled = true;
	}

	async runAsMessage(message) {

		const response = await this.stop(message);

		if (response.code == "error") { return message.reply({ embeds: [response.embed] }); };
		if (response.code == "success") { return message.react("🛑") };

	}

	async runAsInteraction(interaction) {

		const response = await this.stop(interaction);
		return interaction.editReply({ embeds: [response.embed] });

	}

	async stop(command) {

		const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error });

		const voiceChannel = command.member.voice.channel;
		if (!voiceChannel) { return { code: "error", embed: errorEmbed.setDescription("You have to be connected to a voice channel before you can use this command!") } };

		let existingConnection = DiscordVoice.getVoiceConnection(command.guild.id);
		if (existingConnection && existingConnection.joinConfig.channelId != voiceChannel.id) { return { code: "error", embed: errorEmbed.setDescription("Someone else is already listening to music in different channel!") } };

		let stoppedEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
			.setDescription(":octagonal_sign: Stopped the music")

		if (!existingConnection) {
			try { await this.client.player.joinChannel(voiceChannel, command) } catch (error) { return { code: "error", embed: errorEmbed.setDescription(`${error.message ? error.message : error}`) }; };
			return { code: "success", embed: stoppedEmbed };
		}

		await this.client.database.db("guilds").collection("players").updateOne({ guildId: command.guild.id }, { $set: { stopped: true } }, { upsert: true })
		existingConnection.state.subscription.player.skipExecute = true;

		existingConnection.state.subscription.player.stop();

		setTimeout(async () => { existingConnection.state.subscription.player.skipExecute = false; }, 2000);

		return { code: "success", embed: stoppedEmbed };

	}

}