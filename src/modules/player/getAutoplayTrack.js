const Modules = require("../../structures/Modules");

const ytdl = require('ytdl-core');

module.exports = class extends Modules {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "getAutoplayTrack";
        this.subcategory = "player";

        this.blacklisted = ["concert", "live", "tour", "cover", "full show", "performance", "halftime", "trailer", "movie", "clip"];
    }

    async run(guildId) {

        let queueData = await this.client.database.db("queues").collection(guildId).find({}).toArray();

        queueData = queueData.filter(track => track.type != "soundcloud");
        if (!queueData || queueData.length == 0) { throw "Could not AutoPlay from the previous track!"; };

        let queuedIds = queueData.map(track => track.id);

        const lastTrack = queueData[Math.floor(Math.random() * queueData.length)];

        let lastTrackUrl = lastTrack.url;

        if (["spotify", "deezer"].includes(lastTrack.type)) {
            try {
                lastTrackUrl = await this.client.player.youtubeFuzzySearch(lastTrack);
            } catch (error) {
                throw "Could not AutoPlay from the previous track!";
            }
        };

        const trackInfo = await ytdl.getBasicInfo(lastTrackUrl);

        let relatedTracks = trackInfo?.related_videos;
        if (!relatedTracks || relatedTracks.length == 0) { throw "Could not AutoPlay from the previous track!"; };

        relatedTracks = relatedTracks.filter(track => !this.blacklisted.some(keyword => track.title.toLowerCase().includes(keyword)) && !queuedIds.some(id => id.includes(track.id)) && track.length_seconds <= 360);
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