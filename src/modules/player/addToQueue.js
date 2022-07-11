const Modules = require("../../structures/Modules");

module.exports = class extends Modules {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "addToQueue";
        this.subcategory = "player";
    }

    async run(tracksArray, guildId, shuffleFlag = false, nextFlag = false) {

        //      const queueData = await this.client.database.db("queues").collection(guildId).find({}).toArray();

        //      for (const trackNumber in tracksArray) {
        //
        //          if (queueData.some(track => track.url == tracksArray[trackNumber].url)) {
        //              this.client.database.db("queues").collection(guildId).deleteOne({ url: tracksArray[trackNumber].url });
        //          }
        //
        //      }

        const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: guildId });

        let playerShuffle = playerData?.shuffle ?? "off";
        if (playerShuffle == "on" || shuffleFlag) { tracksArray = await this.client.shuffleArray(tracksArray); };

        let wasEmpty = false;

        const queueData = await this.client.database.db("queues").collection(guildId).find({}).toArray();
        if (queueData.length == 0) { wasEmpty = true; };

        if (nextFlag) {
            await this.addToNext(queueData, tracksArray, guildId, playerData.queueID);
        } else {
            await this.client.database.db("queues").collection(guildId).insertMany(tracksArray);
        }

        return { code: "success", wasEmpty };

    }

    async addToNext(queueData, tracksArray, guildId, queueID) {

        queueData.splice((queueID + 1), 0, ...tracksArray);

        await this.client.database.db("queues").collection(guildId).drop().catch(e => { });
        await this.client.database.db("queues").collection(guildId).insertMany(queueData);

    }

}