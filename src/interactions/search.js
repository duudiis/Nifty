const Interactions = require("../structures/Interactions");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

const ytdl = require('ytdl-core');

module.exports = class extends Interactions {

	constructor(client) {
		super(client);
		this.client = client;

		this.name = "search";
	}

	async run(interaction) {

		const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error });

		if (interaction.user.id != interaction.array[1]) { return interaction.reply({ embeds: [errorEmbed.setDescription("This command doesn't belong to you!")], ephemeral: true }) }

		const voiceChannel = interaction.member.voice.channel;
		if (!voiceChannel) { return interaction.reply({ embeds: [errorEmbed.setDescription("You have to be connected to a voice channel before you can use this command!")], ephemeral: true }) };

		let existingConnection = DiscordVoice.getVoiceConnection(interaction.guild.id);
		if (existingConnection && existingConnection.joinConfig.channelId != voiceChannel.id) { return interaction.reply({ embeds: [errorEmbed.setDescription("Someone else is already listening to music in different channel!")], ephemeral: true }) };

		if (!existingConnection) {
			try { await this.client.player.joinChannel(voiceChannel, interaction) } catch (error) { return interaction.reply({ embeds: [errorEmbed.setDescription(`${error.message ? error.message : error}`)], ephemeral: true }) };
			existingConnection = DiscordVoice.getVoiceConnection(interaction.guild.id);
		};

		await interaction.deferUpdate();

		let flags = interaction.array[2].split(",");

		const inputTracks = [];

		for (const value of interaction.values) {

			let trackInfo = await ytdl.getBasicInfo(value);

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

		let addToQueue = await this.client.player.addToQueue(inputTracks, interaction.guild.id, flags.includes("shuffle"), flags.includes("next"), flags.includes("reverse"));

		const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: interaction.guild.id });
		const queueData = await this.client.database.db("queues").collection(interaction.guild.id).find({}).toArray();

		if (playerData.stopped == true || flags.includes("jump")) {
			await this.client.database.db("guilds").collection("players").updateOne({ guildId: interaction.guild.id }, { $set: { queueID: flags.includes("next") && !addToQueue.wasEmpty ? Math.min((queueData.length - 1), (playerData.queueID + 1)) : Math.max(0, queueData.length - inputTracks.length) } }, { upsert: true })
			this.client.player.updatePlayer(existingConnection, interaction.guild.id);
		};

		let queuedEmbed = new MessageEmbed({ color: interaction.guild.me.displayHexColor });

		if (existingConnection?.state?.subscription?.player?.state?.status == "paused") {
			queuedEmbed.setFooter({ text: "The bot is currently paused." });
		}

		if (inputTracks.length == 1) {

			queuedEmbed.setDescription(`Queued [${await this.client.removeFormatting(inputTracks[0].title, 54)}](${inputTracks[0].url}) [<@${inputTracks[0].user}>]`)

			return interaction.editReply({ embeds: [queuedEmbed], components: [] });

		}

		if (inputTracks.length >= 2) {

			queuedEmbed.setDescription(`Queued **${inputTracks.length}** tracks [<@${inputTracks[0].user}>]`)

			return interaction.editReply({ embeds: [queuedEmbed], components: [] });

		}

	}

}