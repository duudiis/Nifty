const Events = require("../structures/Events");

const Constants = require("../../config/Constants")
const Slash = require("../utils/Slash");

module.exports = class extends Events {

	constructor(client) {
		super(client);
		this.client = client;

		this.name = "ready"
	}

	async run() {

		this.client.constants = Constants;

		this.client.slash = new Slash(this.client);
		await this.client.slash.verifyCommands();

		console.log(`${this.client.user.username} is now Online`);
		this.client.user.setActivity('/play', { type: 'LISTENING' });

		setTimeout(async () => { await this.client.player.reconnectPlayers(); }, 2500);

	}

}