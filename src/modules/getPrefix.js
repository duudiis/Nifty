const Modules = require("../structures/Modules");

module.exports = class extends Modules {

	constructor(client) {
		super(client);
		this.client = client;

		this.name = "getPrefix";

		this.prefixes = new Map();
	}

	async run(guildId, force = false) {
		
		if (this.prefixes.has(guildId) && !force) {

			return this.prefixes.get(guildId);

		} else {

			const guildData = await this.client.database.db("default").collection("guilds").findOne({ id: guildId });
			this.prefixes.set(guildId, guildData?.prefix ? guildData.prefix : process.env.DEFAULT_PREFIX);

			return guildData?.prefix ? guildData.prefix : process.env.DEFAULT_PREFIX;

		}

	}

}