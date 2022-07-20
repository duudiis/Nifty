const Clients = require("../structures/Clients");

const fetch = require("node-fetch");

module.exports = class extends Clients {

    constructor() {
        super();

        this.name = "deezer";

        this.baseUrl = "https://api.deezer.com";
    }

    async getTrack(id, userId = "") {

        let response = await fetch(`${this.baseUrl}/track/${id}`, {
            method: "GET",
            headers: {
                "Accept": "application/json",
                "Content-Type": "application/json"
            }
        });

        if (response.status != 200) { return { code: "error", message: response?.data?.error?.message ?? "An error occurred." }; };

        let track = await response.json();
        if (!track?.title) { return { code: "error", message: "No matches found! (731)" }; };

        let trackInfo = [{
            title: `${track.artist.name} - ${track.title}`,
            id: track.id,
            type: "deezer",
            url: track.link,
            duration: track.duration,
            user: userId
        }];

        return { code: "success", track: trackInfo };

    }

    async getPlaylist(id, userId = "") {

        let tracks = [];
        let tracksTotal = 1;

        let limit = 25;
        let index = 0;

        while (tracks.length < tracksTotal) {

            let response = await fetch(`${this.baseUrl}/playlist/${id}/tracks?index=${index}`, {
                method: "GET",
                headers: {
                    "Accept": "application/json",
                    "Content-Type": "application/json"
                }
            });

            if (response.status != 200) { break; };

            let json = await response.json();
            if (!json.data) { return { code: "error", message: "No matches found! (732)" }; };

            tracksTotal = json.total;

            for (let track of json.data) {

                let trackInfo = {
                    title: `${track.artist.name} - ${track.title}`,
                    id: track.id,
                    type: "deezer",
                    url: track.link,
                    duration: track.duration,
                    user: userId
                };

                tracks.push(trackInfo);

            };

            index += limit;

        };

        if (tracks.length == 0) { return { code: "error", message: "This playlist is private." }; }
        return { code: "success", tracks: tracks };

    }

    async getAlbum(id, userId = "") {

        let tracks = [];
        let tracksTotal = 1;

        let limit = 25;
        let index = 0;

        while (tracks.length < tracksTotal) {

            let response = await fetch(`${this.baseUrl}/album/${id}/tracks?index=${index}`, {
                method: "GET",
                headers: {
                    "Accept": "application/json",
                    "Content-Type": "application/json"
                }
            });

            if (response.status != 200) { break; };

            let json = await response.json();
            if (!json.data) { return { code: "error", message: "No matches found! (733)" }; };

            tracksTotal = json.total;

            for (let track of json.data) {

                let trackInfo = {
                    title: `${track.artist.name} - ${track.title}`,
                    id: track.id,
                    type: "deezer",
                    url: track.link,
                    duration: track.duration,
                    user: userId
                };

                tracks.push(trackInfo);

            };

            index += limit;

        };

        if (tracks.length == 0) { return { code: "error", message: "Something went wrong when fetching this album data." }; }
        return { code: "success", tracks: tracks };

    }

    async getArtist(id, userId = "") {

        let tracks = [];

        let response = await fetch(`${this.baseUrl}/artist/${id}/top`, {
            method: "GET",
            headers: {
                "Accept": "application/json",
                "Content-Type": "application/json"
            }
        });

        if (response.status != 200) { return { code: "error", message: "Something went wrong when fetching this artist's top tracks." }; };

        let json = await response.json();

        for (let track of json.data) {

            let trackInfo = {
                title: `${track.artist.name} - ${track.title}`,
                id: track.id,
                type: "deezer",
                url: track.link,
                duration: track.duration,
                user: userId
            };

            tracks.push(trackInfo);

        }

        if (tracks.length == 0) { return { code: "error", message: "Something went wrong when fetching this artist's top tracks." }; }
        return { code: "success", tracks };

    }

}