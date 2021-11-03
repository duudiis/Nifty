const Interactions = require("../structures/Interactions");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

const ytdl = require('ytdl-core');

module.exports = class Search extends Interactions {

	constructor(client) {
		super(client);
		this.client = client;

		this.name = "search";
	}

	async run(interaction) {

		const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error });

		if (interaction.user.id != interaction.customId.slice(6)) { return interaction.reply({ embeds: [errorEmbed.setDescription("This command doesn't belong to you!")], ephemeral: true }) }

		const voiceChannel = interaction.member.voice.channel;
		if (!voiceChannel) { return interaction.reply({ embeds: [errorEmbed.setDescription("You have to be connected to a voice channel before you can use this command!")], ephemeral: true }) };

		let existingConnection = DiscordVoice.getVoiceConnection(interaction.guild.id);
		if (existingConnection && existingConnection.joinConfig.channelId != voiceChannel.id) { return interaction.reply({ embeds: [errorEmbed.setDescription("Someone else is already listening to music in different channel!")], ephemeral: true }) };

		if (!existingConnection) {
			try { await this.client.player.joinChannel(voiceChannel, interaction) } catch (error) { return interaction.reply({ embeds: [errorEmbed.setDescription(`${error.message ? error.message : error}`)], ephemeral: true }) };
			existingConnection = DiscordVoice.getVoiceConnection(interaction.guild.id);
		};

		await interaction.deferUpdate();

		const inputTracks = [];

		for (const value of interaction.values) {

			let trackInfo = await ytdl.getInfo(value);

			let track = {
				title: trackInfo.videoDetails.title,
				id: trackInfo.videoDetails.videoId,
				type: "youtube",
				url: trackInfo.videoDetails.video_url,
				duration: trackInfo.videoDetails.lengthSeconds,
				user: interaction.user.id
			};

			inputTracks.push(track);

		}

		await this.client.player.addToQueue(inputTracks, interaction.guild.id);

		const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: interaction.guild.id });
		const queueData = await this.client.database.db("queues").collection(interaction.guild.id).find({}).toArray();

		if (playerData.stopped == true) {
			await this.client.database.db("guilds").collection("players").updateOne({ guildId: interaction.guild.id }, { $set: { queueID: Math.max(0, queueData.length - inputTracks.length) } }, { upsert: true })
			this.client.player.updatePlayer(existingConnection, interaction.guild.id);
		};

		if (inputTracks.length == 1) {

			let queuedEmbed = new MessageEmbed({ color: interaction.guild.me.displayHexColor })
				.setDescription(`Queued [${await this.removeFormatting(inputTracks[0].title)}](${inputTracks[0].url}) [<@${inputTracks[0].user}>]`)

			return interaction.editReply({ embeds: [queuedEmbed], components: [] });

		}

		if (inputTracks.length >= 2) {

			let queuedEmbed = new MessageEmbed({ color: interaction.guild.me.displayHexColor })
				.setDescription(`Queued **${inputTracks.length}** tracks [<@${inputTracks[0].user}>]`)

			return interaction.editReply({ embeds: [queuedEmbed], components: [] });

		}

	}

	async removeFormatting(string) {

		if (string.length >= 52) { string = string.slice(0, 48).trimEnd() + "…" };

		string = string.replaceAll("*", "\\*");
		string = string.replaceAll("_", "\\_");
		string = string.replaceAll("~", "\\~");
		string = string.replaceAll("`", "\\`");
		string = string.replaceAll("[", "\\[");
		string = string.replaceAll("]", "\\]");

		return string;

	}

}