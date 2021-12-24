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

		this.requiredPermissions = ["View Queue"];

		this.enabled = true;
	}

	async runAsMessage(message) {

		const response = await this.queue(message);
		return message.reply(response.reply);

	}

	async runAsInteraction(interaction) {

		const response = await this.queue(interaction);
		return interaction.editReply(response.reply);

	}

	async queue(command) {

		const existingConnection = DiscordVoice.getVoiceConnection(command.guild.id);

		const firstButton = new MessageButton()
			.setLabel("First")
			.setStyle("SECONDARY")
			.setCustomId("queuefirst1")

		const backButton = new MessageButton()
			.setLabel("Back")
			.setStyle("SECONDARY")
			.setCustomId("queueback1")

		const nextButton = new MessageButton()
			.setLabel("Next")
			.setStyle("SECONDARY")
			.setCustomId("queuenext1")

		const lastButton = new MessageButton()
			.setLabel("Last")
			.setStyle("SECONDARY")
			.setCustomId("queuelast1")

		const buttonsRow = new MessageActionRow().addComponents(firstButton, backButton, nextButton, lastButton);

		const queueData = await this.client.database.db("queues").collection(command.guild.id).find({}).toArray();

		if (!queueData || queueData.length == 0) {

			let queueMessage = "```nim\nThe queue is empty ;-;```"
			return { reply: { content: queueMessage, components: [buttonsRow] } }

		}

		let queueMax = Math.ceil(queueData.length / 10);
		let tracksArray = queueData.slice(0, 10);

		let trackList = "";

		for (const trackId in tracksArray) {

			let trackNumber = `${(parseInt(trackId) + 1) + ((1 - 1) * 10)}`.padStart(2, ' '); let currentTop = ""; let currentBottom = "\n"
			let trackTime = await this.toHHMMSS(tracksArray[trackId].duration);

			if (existingConnection?.state?.subscription?.player?.state?.resource?.metadata?.url == tracksArray[trackId].url) { currentTop = "     ⬐ current track\n"; currentBottom = "\n     ⬑ current track\n"; trackTime = await this.toHHMMSS(tracksArray[trackId].duration - existingConnection.state.subscription.player.state.resource.playbackDuration / 1000) + " left" }

			trackList = trackList + `${currentTop}${trackNumber}) ${tracksArray[trackId].title.length > 36 ? (tracksArray[trackId].title.slice(0, 36).trimEnd() + "…").padEnd(37, ' ') : tracksArray[trackId].title.padEnd(37, ' ')} ${trackTime} ${currentBottom}`

		}

		if (1 == queueMax) { trackList = trackList + "\n    This is the end of the queue!" } else { trackList = trackList + `\n    ${queueData.length - (1 * 10)} more track(s)` };

		return { code: "success", reply: { content: `\`\`\`nim\n${trackList}\`\`\``, components: [buttonsRow] } };

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