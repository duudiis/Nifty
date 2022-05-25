const Modules = require("../../structures/Modules");

const ytdl = require('ytdl-core');

const fetch = require('isomorphic-unfetch');
const Spotify = require('spotify-url-info')(fetch);

const ytsr = require('ytsr');
const ytpl = require('ytpl');

module.exports = class extends Modules {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "getInputTrack";
        this.subcategory = "player";
    }

    async run(input, user) {

        if (input.includes("http://") || input.includes("https://")) {

            input = input.replace("<", "").replace(">", "");

            if (input.includes("youtube.com") || input.includes("youtu.be")) {

                if (input.includes("?list=")) {

                    const tracksInfo = await ytpl(input, { limit: Infinity });

                    let tracks = [];

                    for (const video of tracksInfo.items) {

                        let videoInfo = {
                            title: video.title,
                            id: video.id,
                            type: "youtube",
                            url: video.shortUrl,
                            duration: video.durationSec,
                            user: user.id
                        }

                        tracks.push(videoInfo);

                    }

                    if (tracks.length == 0) { throw "Failed to read playlist videos" };
                    return tracks;

                } else {

                    const trackInfo = await ytdl.getBasicInfo(input);

                    let track = [{
                        title: trackInfo.videoDetails.title,
                        id: trackInfo.videoDetails.videoId,
                        type: "youtube",
                        url: trackInfo.videoDetails.video_url,
                        duration: trackInfo.videoDetails.lengthSeconds,
                        user: user.id
                    }]

                    return track;

                }

            }

            if (input.includes("spotify.com")) {

                if (input.includes("episode")) { throw "Podcasts are not supported" };

                if (input.includes("album") || input.includes("playlist")) {

                    const tracksInfo = await Spotify.getTracks(input);

                    let tracks = [];

                    for (const track of tracksInfo) {

                        let trackInfo = {
                            title: `${track.artists.map(i => i.name).join(", ")} - ${track.name}`,
                            id: track.id,
                            type: "spotify",
                            url: track.external_urls.spotify,
                            duration: Math.round(track.duration_ms / 1000),
                            user: user.id
                        }

                        tracks.push(trackInfo);

                    }

                    if (tracks.length == 0) { throw "Failed to read playlist tracks" };
                    return tracks;

                } else {

                    const trackInfo = await Spotify.getData(input);

                    let track = [{
                        title: `${trackInfo.artists.map(i => i.name).join(", ")} - ${trackInfo.name}`,
                        id: trackInfo.id,
                        type: "spotify",
                        url: trackInfo.external_urls.spotify,
                        duration: Math.round(trackInfo.duration_ms / 1000),
                        user: user.id
                    }]

                    return track;

                }

            }

        }

        const searchResults = await ytsr(input, { pages: 1 });
        if (!searchResults) { throw "No matches found!" };

        const video = searchResults.items.find(video => video.type == "video" && video.duration);
        if (!video) { throw "No matches found!" };

        let durationArray = video.duration.toString().split(':');

        let lengthSeconds = 0;
        if (durationArray.length == 1) { lengthSeconds = durationArray[0] };
        if (durationArray.length == 2) { lengthSeconds = (+durationArray[0]) * 60 + (+durationArray[1]) };
        if (durationArray.length == 3) { lengthSeconds = (+durationArray[0]) * 60 * 60 + (+durationArray[1]) * 60 + (+durationArray[2]) };

        let track = [{
            title: video.title,
            id: video.id,
            type: "youtube",
            url: video.url,
            duration: lengthSeconds,
            user: user.id
        }]

        return track;

    }

}