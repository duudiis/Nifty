const Clients = require("../structures/Clients");

const fetch = require("node-fetch");

module.exports = class extends Clients {

    constructor() {
        super();

        this.name = "spotify";

        this.baseUrl = "https://api.spotify.com/v1";

        this.clientId = process.env.SPOTIFY_CLIENT_ID;
        this.clientSecret = process.env.SPOTIFY_SECRET;

        this.token;
        this.expiresAt;
    }

    async getToken() {

        let response = await fetch("https://accounts.spotify.com/api/token?grant_type=client_credentials", {
            method: "post",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded",
                "Authorization": "Basic " + Buffer.from(this.clientId + ":" + this.clientSecret).toString("base64")
            }
        });

        if (response.status == 200) {
            let json = await response.json();

            this.token = json.access_token;
            this.expiresAt = Date.now() + (json.expires_in * 1000);
        } else {
            this.token = null;
            this.expiresAt = null;
        }

        return { code: "success" };

    }

    async getTrack(id, userId="") {
        if (!this.token || Date.now() >= this.expiresAt) { await this.getToken(); };

        let response = await fetch(`${this.baseUrl}/tracks/${id}`, {
            method: "GET",
            headers: {
                "Accept": "application/json",
                "Content-Type": "application/json",
                "Authorization": `Bearer ${this.token}`
            }
        });

        if (response.status != 200) { return { code: "error", message: response?.data?.error?.message ?? "Invalid track." }; };

        let json = await response.json();

        let trackInfo = [{
            title: `${json.artists.map(a => a.name).join(", ")} - ${json.name}`,
            id: json.id,
            type: "spotify",
            url: json.external_urls.spotify,
            duration: Math.round(json.duration_ms / 1000),
            user: userId
        }];

        return { code: "success", track: trackInfo };

    }

    async getPlaylist(id, userId="") {
        if (!this.token || Date.now() >= this.expiresAt) { await this.getToken(); };
        let fields = "total%2Citems(track(name%2C%20id%2C%20artists(name)%2Cexternal_urls(spotify)%2Cduration_ms))";

        let tracks = [];
        let tracksTotal = 1;

        let limit = 100;
        let offset = 0;

        while (tracks.length < tracksTotal) {

            let response = await fetch(`${this.baseUrl}/playlists/${id}/tracks?fields=${fields}&limit=${limit}&offset=${offset}`, {
                method: "GET",
                headers: {
                    "Accept": "application/json",
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${this.token}`
                }
            });

            if (response.status != 200) { break; };

            let json = await response.json();
            tracksTotal = json.total;

            for (let track of json.items) {
                if (track.track.is_local) { continue; };

                let trackInfo = {
                    title: `${track.track.artists.map(a => a.name).join(", ")} - ${track.track.name}`,
                    id: track.track.id,
                    type: "spotify",
                    url: track.track.external_urls.spotify,
                    duration: Math.round(track.track.duration_ms / 1000),
                    user: userId
                };

                tracks.push(trackInfo);

            };

            offset += limit;

        }

        if (tracks.length == 0) { return { code: "error", message: "This playlist is private." }; }
        return { code: "success", tracks: tracks };

    }

    async getAlbum(id, userId="") {
        if (!this.token || Date.now() >= this.expiresAt) { await this.getToken(); };

        let tracks = [];
        let tracksTotal = 1;

        let limit = 50;
        let offset = 0;

        while (tracks.length < tracksTotal) {

            let response = await fetch(`${this.baseUrl}/albums/${id}/tracks?limit=${limit}&offset=${offset}`, {
                method: "GET",
                headers: {
                    "Accept": "application/json",
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${this.token}`
                }
            });

            if (response.status != 200) { break; };

            let json = await response.json();
            tracksTotal = json.total;

            for (let track of json.items) {

                let trackInfo = {
                    title: `${track.artists.map(a => a.name).join(", ")} - ${track.name}`,
                    id: track.id,
                    type: "spotify",
                    url: track.external_urls.spotify,
                    duration: Math.round(track.duration_ms / 1000),
                    user: userId
                };

                tracks.push(trackInfo);

            };

            offset += limit;

        }

        if (tracks.length == 0) { return { code: "error", message: "Something went wrong when fetching this album data." }; }
        return { code: "success", tracks };

    }

    async getArtist(id, userId="") {
        if (!this.token || Date.now() >= this.expiresAt) { await this.getToken(); };

        let tracks = [];

        let response = await fetch(`${this.baseUrl}/artists/${id}/top-tracks?market=US`, {
            method: "GET",
            headers: {
                "Accept": "application/json",
                "Content-Type": "application/json",
                "Authorization": `Bearer ${this.token}`
            }
        });

        if (response.status != 200) { return { code: "error", message: "Something went wrong when fetching this artist's top tracks." }; };

        let json = await response.json();

        for (let track of json.tracks) {

            let trackInfo = {
                title: `${track.artists.map(a => a.name).join(", ")} - ${track.name}`,
                id: track.id,
                type: "spotify",
                url: track.external_urls.spotify,
                duration: Math.round(track.duration_ms / 1000),
                user: userId
            };

            tracks.push(trackInfo);

        }

        if (tracks.length == 0) { return { code: "error", message: "Something went wrong when fetching this artist's top tracks." }; }
        return { code: "success", tracks };

    }

}