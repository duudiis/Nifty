const Modules = require("../structures/Modules");

module.exports = class CheckPremium extends Modules {

	constructor(client) {
		super(client);
		this.client = client;

		this.name = "checkPremium";
	}

	async run(guildId) {

		return this.client.constants.premium.includes(guildId);

	}

}