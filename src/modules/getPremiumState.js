const Modules = require("../structures/Modules");

module.exports = class extends Modules {

	constructor(client) {
		super(client);
		this.client = client;

		this.name = "getPremiumState";
	}

	async run(guildId) {

		return this.client.constants.premium.includes(guildId);

	}

}