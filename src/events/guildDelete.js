const Events = require("../structures/Events");

const { MessageEmbed } = require("discord.js");

module.exports = class GuildDelete extends Events {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "GuildDelete"
    }

    async run(guild) {

        this.client.database.db("default").collection("guilds").deleteOne({ id: guild.id });

        this.client.database.db("guilds").collection("players").deleteOne({ guildId: guild.id });
        this.client.database.db("queues").collection(guild.id).deleteMany({});

    }

}