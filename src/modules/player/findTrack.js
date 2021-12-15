const Modules = require("../../structures/Modules");

module.exports = class extends Modules {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "findTrack";
        this.subcategory = "player";
    }

    async run(query, guildId) {

        const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: guildId });
        const queueData = await this.client.database.db("queues").collection(guildId).find({}).toArray();

        let resultTrack = null;
        let resultId = null;

        let originalQuery = query;

        if (this.client.constants.keywords.first.includes(query.toLowerCase())) { query = "1" };
        if (this.client.constants.keywords.next.includes(query.toLowerCase())) { query = "+1" };
        if (this.client.constants.keywords.current.includes(query.toLowerCase())) { query = `${playerData.queueID + 1}` };
        if (this.client.constants.keywords.back.includes(query.toLowerCase())) { query = "-1" };
        if (this.client.constants.keywords.last.includes(query.toLowerCase())) { query = `${queueData.length}` };

        if (query.match(/^(\+|-)[0-9]+/)) {

            const queryInt = parseInt(query);
            if (queryInt == 0) { throw `A track could not be found for "${originalQuery}"!` };

            queryInt > 0 ? resultId = playerData.queueID + queryInt : resultId = playerData.queueID - Math.abs(queryInt);
            resultTrack = queueData[resultId];

        } else if (parseInt(query) == query) {

            resultId = parseInt(query) - 1;
            resultTrack = queueData[resultId];

        } else {

            resultTrack = queueData.find(track => track.title.toLowerCase().includes(query.toLowerCase()));
            if (!resultTrack) { throw `A track could not be found for "${originalQuery}"!` };

            resultId = queueData.map(track => track.url).indexOf(resultTrack.url);
            if (!resultId) { throw `A track could not be found for "${originalQuery}"!` };
        }

        if (!resultTrack) { throw `A track could not be found for "${originalQuery}"!` };

        return { track: resultTrack, id: resultId };

    }

}