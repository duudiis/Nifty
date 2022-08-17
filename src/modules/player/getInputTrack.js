const Modules = require("../../structures/Modules");

const ytdl = require('ytdl-core');
const scdl = require('soundcloud-downloader').default;

const ytsr = require('ytsr');
const ytpl = require('ytpl');

module.exports = class extends Modules {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "getInputTrack";
        this.subcategory = "player";

        this.regex = {
            youtube: /(?:https?:)\/\/(?:(?:www|m|music)\.)??(?:youtube(?:-nocookie)?\.com|youtu.be)\/(?:embed\/|shorts\/)?(?:[\w?&=#%-]+)/i,
            spotify: /https?:\/\/open.spotify.com\/(track|artist|playlist|album|show)\/([a-zA-Z0-9]+)/i,
            deezer: /https?:\/\/(?:www\.)?deezer\.com\/(?:[a-z]+\/)?(track|artist|album|playlist|show|radio)\/([0-9]+)/i,
            deezerShort: /https?:\/\/deezer\.page\.link\/[A-z0-9]+/i,
            soundcloud: /https?:\/\/(?:(?:www|m)\.)?soundcloud\.com\/([A-z0-9-_]+)\/(sets|[A-z0-9-_]+)(?:\/)?([\w?&=#_-]+)?(?:\/)?(?:[\w\/?&=#_-]+)?/i,
            soundcloudShort: /https?:\/\/soundcloud\.app\.goo\.gl\/[A-z0-9]+/i
        }
    }

    async run(input, user, allFlag = false) {

        if (input.includes("https://")) {

            let youtubeUrl = this.regex.youtube.exec(input);
            if (youtubeUrl) {
                input = youtubeUrl[0];

                if (input.includes("?list=") || allFlag) {

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

                    if (tracks.length == 0) { throw "Failed to read playlist videos"; };
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

            let spotifyUrl = this.regex.spotify.exec(input);
            if (spotifyUrl) {
                input = spotifyUrl[0];

                let type = spotifyUrl[1];
                let id = spotifyUrl[2];

                if (type == "show") { throw "Podcasts are not supported" };

                if (type == "track") {

                    let track = await this.client.spotify.getTrack(id, user.id);
                    if (track.code == "error") { throw track?.message || "An error occurred"; };

                    return track.track;

                };

                if (type == "playlist") {

                    let playlist = await this.client.spotify.getPlaylist(id, user.id);
                    if (playlist.code == "error") { throw playlist?.message || "An error occurred"; };

                    return playlist.tracks;

                };

                if (type == "album") {

                    let album = await this.client.spotify.getAlbum(id, user.id);
                    if (album.code == "error") { throw album?.message || "An error occurred"; };

                    return album.tracks;

                }

                if (type == "artist") {

                    let artist = await this.client.spotify.getArtist(id, user.id);
                    if (artist.code == "error") { throw artist?.message || "An error occurred"; };

                    return artist.tracks;

                };

            }

            let deezerShortUrl = this.regex.deezerShort.exec(input);
            if (deezerShortUrl) { input = await this.client.getUrl(deezerShortUrl[0]); };

            let deezerUrl = this.regex.deezer.exec(input);
            if (deezerUrl) {
                input = deezerUrl[0];

                let type = deezerUrl[1];
                let id = deezerUrl[2];

                if (type == "show" || type == "radio") { throw "Podcasts are not supported"; };

                if (type == "track") {

                    let track = await this.client.deezer.getTrack(id, user.id);
                    if (track.code == "error") { throw track?.message || "An error occurred"; };

                    return track.track;

                }

                if (type == "playlist") {

                    let playlist = await this.client.deezer.getPlaylist(id, user.id);
                    if (playlist.code == "error") { throw playlist?.message || "An error occurred"; };

                    return playlist.tracks;

                }

                if (type == "album") {

                    let album = await this.client.deezer.getAlbum(id, user.id);
                    if (album.code == "error") { throw album?.message || "An error occurred"; };

                    return album.tracks;

                }

                if (type == "artist") {

                    let artist = await this.client.deezer.getArtist(id, user.id);
                    if (artist.code == "error") { throw artist?.message || "An error occurred"; };

                    return artist.tracks;

                }

            }

            let soundcloudShortUrl = this.regex.soundcloudShort.exec(input);
            if (soundcloudShortUrl) { input = await this.client.getUrl(soundcloudShortUrl[0]); };

            let soundcloudUrl = this.regex.soundcloud.exec(input);
            if (soundcloudUrl) {

                let url = soundcloudUrl[0];
                let type = soundcloudUrl[2] == "sets" ? "set" : "track";

                if (type == "track") {

                    let trackInfo = await scdl.getInfo(url);

                    let track = [{
                        title: trackInfo.title,
                        id: trackInfo.id,
                        type: "soundcloud",
                        url: trackInfo.permalink_url,
                        duration: Math.round(trackInfo.duration / 1000),
                        user: user.id
                    }];

                    return track;

                }

                if (type == "set") {

                    let set = await scdl.getSetInfo(url);

                    let tracks = [];

                    for (let track of set.tracks) {

                        let trackInfo = {
                            title: track.title,
                            id: track.id,
                            type: "soundcloud",
                            url: track.permalink_url,
                            duration: Math.round(track.duration / 1000),
                            user: user.id
                        }

                        tracks.push(trackInfo);

                    }

                    if (tracks.length == 0) { throw "Failed to read set tracks"; };
                    return tracks;

                }

            }

        }

        if (!input) { throw "No matches found!"; };

        const searchResults = await ytsr(input);
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
        }];

        return track;

    }

}