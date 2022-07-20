const Interactions = require("../structures/Interactions");

const DiscordVoice = require('@discordjs/voice');
const { MessageButton, MessageActionRow } = require("discord.js");

module.exports = class extends Interactions {

	constructor(client) {
		super(client);
		this.client = client;

		this.name = "queue";
	}

	async run(button) {

		const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: button.guild.id });
		const queueData = await this.client.database.db("queues").collection(button.guild.id).find({}).toArray();

		const existingConnection = DiscordVoice.getVoiceConnection(button.guild.id);

		if (!queueData || queueData.length == 0) {

			const buttonsRow = await this.updatedButtons(1);

			let queueMessage = "```nim\nThe queue is empty ;-;```";
			return button.update({ content: queueMessage, components: [buttonsRow] });

		}

		let queueMax = Math.ceil(queueData.length / 10);

		const page = await this.getPage(button, queueMax);
		if (page.updated > queueMax) { page.updated = queueMax; };

		let tracksArray = [];

		let lastMinus = 0;

		if (page.updated == 1) { tracksArray = queueData.slice(0, 10) }
		else if (page.updated == queueMax) { tracksArray = queueData.slice(-10); lastMinus = 10 - (queueData.length % 10) }
		else { tracksArray = queueData.slice((page.updated - 1) * 10, -(queueData.length - (page.updated * 10))) }

		if (lastMinus == 10) { lastMinus = 0; };

		let padStart = (((parseInt(tracksArray.length - 1) + 1) + ((page.updated - 1) * 10)) - lastMinus).toString().length;
		let trackList = "";

		for (let trackId in tracksArray) {

			let trackNumber = `${((parseInt(trackId) + 1) + ((page.updated - 1) * 10)) - lastMinus}`.padStart(padStart, ' '); let currentTop = ""; let currentBottom = "\n";
			let trackTime = await this.toHHMMSS(tracksArray[trackId].duration);

			if ((trackNumber - 1) == playerData.queueID && existingConnection?.state?.subscription?.player?.state?.status != "idle") { currentTop = "     ⬐ current track\n"; currentBottom = "\n     ⬑ current track\n"; trackTime = await this.toHHMMSS(tracksArray[trackId].duration - existingConnection.state.subscription.player.state.resource.playbackDuration / 1000) + " left" }

			trackList = trackList + `${currentTop}${trackNumber}) ${tracksArray[trackId].title.length > 36 ? (tracksArray[trackId].title.slice(0, 36).trimEnd() + "…").padEnd(37, ' ') : tracksArray[trackId].title.padEnd(37, ' ')} ${trackTime} ${currentBottom}`

		}

		if (page.updated == queueMax) { trackList = trackList + "\n" + "This is the end of the queue!".padStart(padStart + 31, ' '); } else { trackList = trackList + "\n" + `${queueData.length - (page.updated * 10)} more track(s)`.padStart(padStart + 16 + ((queueData.length - (page.updated * 10)).toString().length), ' '); }

		const buttonsRow = await this.updatedButtons(page.updated);
		return button.update({ content: `\`\`\`nim\n${trackList}\`\`\``, components: [buttonsRow] })

	}

	async updatedButtons(page) {

		const firstButton = new MessageButton({ label: "First", style: "SECONDARY", customId: `queue_first_${page}` });
		const backButton = new MessageButton({ label: "Back", style: "SECONDARY", customId: `queue_back_${page}` });
		const nextButton = new MessageButton({ label: "Next", style: "SECONDARY", customId: `queue_next_${page}` });
		const lastButton = new MessageButton({ label: "Last", style: "SECONDARY", customId: `queue_last_${page}` });

		const buttonsRow = new MessageActionRow().addComponents(firstButton, backButton, nextButton, lastButton);

		return buttonsRow;

	}

	async getPage(button, maxPage) {

		let currentPage = null;
		let updatePage = null;

		if (button.array[1] == "first") { currentPage = parseInt(button.array[2]); updatePage = 1 };
		if (button.array[1] == "back") { currentPage = parseInt(button.array[2]); currentPage == 1 ? updatePage = currentPage : updatePage = currentPage - 1 };
		if (button.array[1] == "next") { currentPage = parseInt(button.array[2]); currentPage == maxPage ? updatePage = currentPage : updatePage = currentPage + 1 };
		if (button.array[1] == "last") { currentPage = parseInt(button.array[2]); updatePage = maxPage };

		return { code: "success", updated: updatePage, current: currentPage };

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