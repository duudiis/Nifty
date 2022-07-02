const Commands = require("../../structures/Commands");

const DiscordVoice = require('@discordjs/voice');
const { MessageButton, MessageActionRow } = require("discord.js");

module.exports = class extends Commands {

	constructor(client) {
		super(client);
		this.client = client;

		this.name = "queue";
		this.aliases = ["q", "que"];

		this.description = "Displays the current queue of tracks";
		this.category = "music";

		this.usage = "queue";
		this.options = [];

		this.requiredPermissions = ["VIEW_QUEUE"];

		this.enabled = true;
	}

	async runAsMessage(message) {

		const input = message.array.slice(1).join(" ");
		if (input) { return await this.client.commands.get("play").runAsMessage(message); };

		const response = await this.queue(message);
		return message.channel.send(response.reply);

	}

	async runAsInteraction(interaction) {

		const response = await this.queue(interaction);
		return interaction.editReply(response.reply);

	}

	async queue(command) {

		const existingConnection = DiscordVoice.getVoiceConnection(command.guild.id);

		const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: command.guild.id });
		const queueData = await this.client.database.db("queues").collection(command.guild.id).find({}).toArray();

		if (!queueData || queueData.length == 0) {

			let queueMessage = "```nim\nThe queue is empty ;-;```"
			const buttonsRow = await this.getButtonsRow(1);

			return { reply: { content: queueMessage, components: [buttonsRow] } }

		}

		const trackPage = Math.ceil((playerData.queueID + 1) / 10);
		const buttonsRow = await this.getButtonsRow(trackPage);

		let queueMax = Math.ceil(queueData.length / 10);

		let tracksArray = [];

		let lastMinus = 0;

		if (trackPage == 1) { tracksArray = queueData.slice(0, 10) }
		else if (trackPage == queueMax) { tracksArray = queueData.slice(-10); lastMinus = 10 - (queueData.length % 10) }
		else { tracksArray = queueData.slice((trackPage - 1) * 10, -(queueData.length - (trackPage * 10))) }

		if (lastMinus == 10) { lastMinus = 0; };

		let padStart = (((parseInt(tracksArray.length - 1) + 1) + ((trackPage - 1) * 10)) - lastMinus).toString().length;
		let trackList = "";

		for (let trackId in tracksArray) {

			let trackNumber = `${((parseInt(trackId) + 1) + ((trackPage - 1) * 10)) - lastMinus}`.padStart(padStart, ' '); let currentTop = ""; let currentBottom = "\n";
			let trackTime = await this.toHHMMSS(tracksArray[trackId].duration);

			if ((trackNumber - 1) == playerData.queueID && existingConnection?.state?.subscription?.player?.state?.status != "idle") { currentTop = "     ⬐ current track\n"; currentBottom = "\n     ⬑ current track\n"; trackTime = await this.toHHMMSS(tracksArray[trackId].duration - existingConnection.state.subscription.player.state.resource.playbackDuration / 1000) + " left" }

			trackList = trackList + `${currentTop}${trackNumber}) ${tracksArray[trackId].title.length > 36 ? (tracksArray[trackId].title.slice(0, 36).trimEnd() + "…").padEnd(37, ' ') : tracksArray[trackId].title.padEnd(37, ' ')} ${trackTime} ${currentBottom}`

		}

		if (trackPage == queueMax) { trackList = trackList + "\n" + "This is the end of the queue!".padStart(padStart + 31, ' '); } else { trackList = trackList + "\n" + `${queueData.length - (trackPage * 10)} more track(s)`.padStart(padStart + 16 + ((queueData.length - (trackPage * 10)).toString().length), ' '); }

		return { code: "success", reply: { content: `\`\`\`nim\n${trackList}\`\`\``, components: [buttonsRow] } };

	}

	async getButtonsRow(queuePage) {
		
		const firstButton = new MessageButton({ label: "First", style: "SECONDARY", customId: `queuefirst${queuePage}` })
		const backButton = new MessageButton({ label: "Back", style: "SECONDARY", customId: `queueback${queuePage}` })
		const nextButton = new MessageButton({ label: "Next", style: "SECONDARY", customId: `queuenext${queuePage}` })
		const lastButton = new MessageButton({ label: "Last", style: "SECONDARY", customId: `queuelast${queuePage}` })

		const buttonsRow = new MessageActionRow().addComponents(firstButton, backButton, nextButton, lastButton);
		return buttonsRow;

	}

	async toHHMMSS(secs) {
		let sec_num = parseInt(secs, 10);
		let hours = Math.floor(sec_num / 3600);
		let minutes = Math.floor(sec_num / 60) % 60;
		let seconds = sec_num % 60;

		return [hours, minutes, seconds]
			.map(v => v < 10 ? "0" + v : v)
			.filter((v, i) => v !== "00" || i > 0)
			.join(":")
	}

}