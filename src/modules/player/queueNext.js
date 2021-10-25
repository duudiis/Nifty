const Modules = require("../../structures/Modules");

const DiscordVoice = require('@discordjs/voice');

const ytdl = require('ytdl-core');

module.exports = class QueueNext extends Modules {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "queueNext";
        this.subcategory = "player";
    }

    async run(connection, guildId) {

        const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: guildId });
        const queueData = await this.client.database.db("queues").collection(guildId).find({}).toArray();

        if (!playerData || !queueData) { return };

        if (connection.state.subscription.player.skipExecute == true) { return connection.state.subscription.player.skipExecute = false; };

        let nextQueueID = playerData.queueID + 1;
        let nextQueue = queueData[nextQueueID];

        if(!nextQueue && playerData.loop == "queue") {

            nextQueueID = 0;
            nextQueue = queueData[0];

        }

        if (playerData.loop == "track") {

            nextQueueID = playerData.queueID;
            nextQueue = queueData[nextQueueID];

        }

        if (!nextQueue) { return await this.client.database.db("guilds").collection("players").updateOne({ guildId: guildId }, { $set: { stopped: true } }, { upsert: true }) };

        await this.client.database.db("guilds").collection("players").updateOne({ guildId: guildId }, { $set: { queueID: nextQueueID } }, { upsert: true });

        return this.client.player.updatePlayer(connection, guildId);

    }

}