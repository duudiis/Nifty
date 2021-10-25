const Interactions = require("../structures/Interactions");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed, MessageButton, MessageActionRow } = require("discord.js");

module.exports = class Queue extends Interactions {

	constructor(client) {
		super(client);
		this.client = client;

		this.name = "queue";
	}

	async run(button) {

		const queueData = await this.client.database.db("queues").collection(button.guild.id).find({}).toArray();
		const existingConnection = DiscordVoice.getVoiceConnection(button.guild.id);

		if (!queueData || queueData.length == 0) {

			const buttonsRow = await this.updatedButtons(1);

			let queueMessage = "```nim\nThe queue is empty ;-;```";
			return button.update({ content: queueMessage, components: [buttonsRow] });

		}

		let queueMax = Math.ceil(queueData.length / 10);

		const page = await this.getPage(button, queueMax);

		let tracksArray = [];

		if (page.updated == 1) { tracksArray = queueData.slice(0, 10) }
		else if (page.updated == queueMax) { tracksArray = queueData.slice(-10) }
		else { tracksArray = queueData.slice((page.updated - 1) * 10, -(queueData.length - (page.updated * 10))) }

		let trackList = ""

		for (let trackId in tracksArray) {

			let trackNumber = `${(parseInt(trackId) + 1) + ((page.updated - 1) * 10)}`.padStart(2, ' '); let currentTop = ""; let currentBottom = "\n"
			let trackTime = await this.toHHMMSS(tracksArray[trackId].duration);

			if (existingConnection?.state?.subscription?.player?.state?.resource?.metadata?.url == tracksArray[trackId].url && existingConnection?.state?.subscription?.player?.state?.status == "playing") { currentTop = "     ⬐ current track\n"; currentBottom = "\n     ⬑ current track\n"; trackTime = await this.toHHMMSS(tracksArray[trackId].duration - existingConnection.state.subscription.player.state.resource.playbackDuration / 1000) + " left" }

			trackList = trackList + `${currentTop}${trackNumber}) ${tracksArray[trackId].title.length > 36 ? (tracksArray[trackId].title.slice(0, 36).trimEnd() + "…").padEnd(37, ' ') : tracksArray[trackId].title.padEnd(37, ' ')} ${trackTime} ${currentBottom}`

		}

		if(page.updated == queueMax) { trackList = trackList + "\n    This is the end of the queue!" }else { trackList = trackList + `\n    ${queueData.length - (page.updated * 10)} more track(s)` }

		const buttonsRow = await this.updatedButtons(page.updated);
		return button.update({ content: `\`\`\`nim\n${trackList}\`\`\``, components: [buttonsRow] })

	}

	async updatedButtons(page) {

		const firstButton = new MessageButton({ label: "First", style: "SECONDARY", customId: `queuefirst${page}` });
		const backButton = new MessageButton({ label: "Back", style: "SECONDARY", customId: `queueback${page}` });
		const nextButton = new MessageButton({ label: "Next", style: "SECONDARY", customId: `queuenext${page}` });
		const lastButton = new MessageButton({ label: "Last", style: "SECONDARY", customId: `queuelast${page}` });

		const buttonsRow = new MessageActionRow().addComponents(firstButton, backButton, nextButton, lastButton);

		return buttonsRow;

	}

	async getPage(button, maxPage) {

		let currentPage = null;
		let updatePage = null;

		if (button.customId.includes("first")) { currentPage = parseInt(button.customId.replace("queuefirst", "")); updatePage = 1 };
		if (button.customId.includes("back")) { currentPage = parseInt(button.customId.replace("queueback", "")); currentPage == 1 ? updatePage = currentPage : updatePage = currentPage - 1 };
		if (button.customId.includes("next")) { currentPage = parseInt(button.customId.replace("queuenext", "")); currentPage == maxPage ? updatePage = currentPage : updatePage = currentPage + 1 };
		if (button.customId.includes("last")) { currentPage = parseInt(button.customId.replace("queuelast", "")); updatePage = maxPage };

		return { code: "success", updated: updatePage, current: currentPage };

	}

	async toHHMMSS(secs) {
		let sec_num = parseInt(secs, 10);
		let hours = Math.floor(sec_num / 3600);
		let minutes = Math.floor(sec_num / 60) % 60;
		let seconds = sec_num % 60;
	
		return [hours,minutes,seconds]
			.map(v => v < 10 ? "0" + v : v)
			.filter((v,i) => v !== "00" || i > 0)
			.join(":")
	}

}