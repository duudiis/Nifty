const Modules = require("../../structures/Modules");

module.exports = class AddToQueue extends Modules {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "addToQueue";
        this.subcategory = "player";
    }

    async run(tracksArray, guildId) {

        const queueData = await this.client.database.db("queues").collection(guildId).find({}).toArray();

        for (const trackNumber in tracksArray) {

            if (queueData.some(track => track.url == tracksArray[trackNumber].url)) {
                this.client.database.db("queues").collection(guildId).deleteOne({ url: tracksArray[trackNumber].url });
            }

        }

        await this.client.database.db("queues").collection(guildId).insertMany(tracksArray);

    }

}