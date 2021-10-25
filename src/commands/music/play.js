const Commands = require("../../structures/Commands");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class Play extends Commands {

	constructor(client) {
		super(client);
		this.client = client;

		this.name = "play";
		this.aliases = ["p"];

		this.description = "Play a song in your voice channel";
		this.category = "music";

		this.usage = "play { input }";
		this.options = [
			{
				"name": "input",
				"description": "A search term or a link",
				"type": "STRING",
				"required": true
			}
		]

		this.enabled = true;
	}

	async runAsMessage(message) {

		const input = message.array.slice(1).join(" ");
		if (!input) { return this.client.commands.get("unpause").runAsMessage(message, true) };

		const response = await this.play(input, message);
		return message.reply({ embeds: [response.embed] });

	}

	async runAsInteraction(interaction) {

		const input = interaction.options.get("input").value;

		const response = await this.play(input, interaction);
		return interaction.editReply({ embeds: [response.embed] });

	}

	async play(input, command) {

		const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error });

		const voiceChannel = command.member.voice.channel;
		if (!voiceChannel) { return { code: "error", embed: errorEmbed.setDescription("You have to be connected to a voice channel before you can use this command!") } };

		let existingConnection = DiscordVoice.getVoiceConnection(command.guild.id);
		if (existingConnection && existingConnection.joinConfig.channelId != voiceChannel.id) { return { code: "error", embed: errorEmbed.setDescription("Someone else is already listening to music in different channel!") } };

		if (!existingConnection) {
			try { await this.client.player.joinChannel(voiceChannel, command) } catch (error) { return { code: "error", embed: errorEmbed.setDescription(`${error.message ? error.message : error}`) }; };
			existingConnection = DiscordVoice.getVoiceConnection(command.guild.id);
		};

		let inputTracks = [];
		try { inputTracks = await this.client.player.getInputTrack(input, command.member) } catch (error) { return { code: "error", embed: errorEmbed.setDescription(`${error.message ? error.message : error}`) }; };

		await this.client.player.addToQueue(inputTracks, command.guild.id);

		const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: command.guild.id });
		const queueData = await this.client.database.db("queues").collection(command.guild.id).find({}).toArray();

		if (playerData.stopped == true) {
			await this.client.database.db("guilds").collection("players").updateOne({ guildId: command.guild.id }, { $set: { queueID: Math.max(0, queueData.length - inputTracks.length) } }, { upsert: true })
			this.client.player.updatePlayer(existingConnection, command.guild.id);
		}

		console.log(`[commands/play] stopped: ${playerData.stopped} | new: ${Math.max(0, queueData.length - inputTracks.length)} | old: ${queueData.length - inputTracks.length}`);

		if (inputTracks.length == 1) {

			let queuedEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
				.setDescription(`Queued [${await this.removeFormatting(inputTracks[0].title)}](${inputTracks[0].url}) [<@${inputTracks[0].user}>]`)

			return { code: "success", embed: queuedEmbed };

		}

		if (inputTracks.length >= 2) {

			let queuedEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
				.setDescription(`Queued **${inputTracks.length}** tracks [<@${inputTracks[0].user}>]`)

			return { code: "success", embed: queuedEmbed };

		}

	}

	async removeFormatting(string) {

		if (string.length >= 56) { string = string.slice(0, 52).trimEnd() + "…" };

		string = string.replaceAll("*", "\\*");
		string = string.replaceAll("_", "\\_");
		string = string.replaceAll("~", "\\~");
		string = string.replaceAll("`", "\\`");
		string = string.replaceAll("[", "\\[");
		string = string.replaceAll("]", "\\]");

		return string;

	}

}