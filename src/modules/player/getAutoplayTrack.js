const Modules = require("../../structures/Modules");

const ytdl = require('ytdl-core');

module.exports = class GetAutoplayTrack extends Modules {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "getAutoplayTrack";
        this.subcategory = "player";
    }

    async run(guildId) {

        const queueData = await this.client.database.db("queues").collection(guildId).find({}).toArray();
        const lastTrack = queueData[queueData.length - 1];

        if (lastTrack.type != "youtube") { throw "Could not AutoPlay from the previous track!"; };

        const trackInfo = await ytdl.getInfo(lastTrack.url);

        const relatedTracks = trackInfo?.related_videos;
        if (!relatedTracks || relatedTracks.length == 0) { throw "Could not AutoPlay from the previous track!"; };

        const relatedTrack = relatedTracks[Math.floor(Math.random() * relatedTracks.length)];
        if (!relatedTrack.id || !relatedTrack.length_seconds) { throw "Could not AutoPlay from the previous track!"; };

        let track = [{
            title: relatedTrack.title,
            id: relatedTrack.id,
            type: "youtube",
            url: `https://www.youtube.com/watch?v=${relatedTrack.id}`,
            duration: relatedTrack.length_seconds,
            user: this.client.user.id
        }]

        await this.client.player.addToQueue(track, guildId);
        return { code: "success" };

    }

}