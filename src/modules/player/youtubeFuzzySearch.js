const Modules = require("../../structures/Modules");

const ytsr = require('ytsr');

module.exports = class YoutubeFuzzySearch extends Modules {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "youtubeFuzzySearch";
        this.subcategory = "player";
    }

    async run(trackInfo) {

        const searchResults = await ytsr(trackInfo.title, { pages: 1 });
        if (!searchResults.items) { throw "An equivalent video on YouTube was unable to be found!" };

        let matchingResults = [];

        for (const video of searchResults.items) {

            if (video.type == "video" && video.duration) {

                let durationArray = video.duration.toString().split(':');

                let lengthSeconds = 0;
                if (durationArray.length == 1) { lengthSeconds = durationArray[0] };
                if (durationArray.length == 2) { lengthSeconds = (+durationArray[0]) * 60 + (+durationArray[1]) };
                if (durationArray.length == 3) { lengthSeconds = (+durationArray[0]) * 60 * 60 + (+durationArray[1]) * 60 + (+durationArray[2]) };

                matchingResults.push({ videoUrl: video.url, videoDuration: lengthSeconds, videoId: video.id });

            }

        }

        if (matchingResults.length == 0) { throw "An equivalent video on YouTube was unable to be found!" };

        const mostPopular = matchingResults.reduce((x, y) => {
            return Math.abs(x.videoDuration - trackInfo.duration) < Math.abs(y.videoDuration - trackInfo.duration) ? x : y;
        })

        return mostPopular.videoUrl;

    }

}