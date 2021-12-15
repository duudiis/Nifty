const Events = require("../structures/Events");

const { MessageEmbed } = require("discord.js");

module.exports = class extends Events {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "GuildDelete"
    }

    async run(guild) {

        this.client.database.db("default").collection("guilds").deleteMany({ id: guild.id });

        this.client.database.db("guilds").collection("players").deleteMany({ guildId: guild.id });
        this.client.database.db("queues").collection(guild.id).drop().catch(e => {});

    }

}