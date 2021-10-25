const Modules = require("../structures/Modules");

module.exports = class GetPrefix extends Modules {

	constructor(client) {
		super(client);
		this.client = client;

        this.name = "getPrefix";
	}

	async run(guildId) {

		const guildData = await this.client.database.db("default").collection("guilds").findOne({ id: guildId });
        return guildData?.prefix ? guildData.prefix : process.env.DEFAULT_PREFIX;

    }

}