const Modules = require("../../structures/Modules");

const DiscordVoice = require('@discordjs/voice');

const scdl = require('soundcloud-downloader').default;
const ytdl = require('ytdl-core');

module.exports = class extends Modules {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "updatePlayer";
        this.subcategory = "player";
    }

    async run(connection, guildId) {

        const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: guildId });
        const queueData = await this.client.database.db("queues").collection(guildId).find({}).toArray();

        let currentVolume = playerData?.volume || 100;

        let playQueueID = playerData.queueID;
        let playQueue = queueData[playQueueID];

        if (!playQueue) { return; };

        let playQueueUrl = playQueue.url;

        if (["spotify", "deezer"].includes(playQueue.type)) {
            try {
                playQueueUrl = await this.client.player.youtubeFuzzySearch(playQueue);
            } catch (error) {
                return connection.state.subscription.player.emit("error", error);
            }
        };

        try {
            
            if (playQueue.type == "soundcloud") {
                var stream = await scdl.download(playQueueUrl);
            } else {
                var stream = ytdl(playQueueUrl, { quality: 'highestaudio', dlChunkSize: 1 << 30, highWaterMark: 1 << 21 });
            };

        } catch (error) {
            return connection.state.subscription.player.emit("error", error?.message ?? "An error ocurred");
        }

        const playResource = DiscordVoice.createAudioResource(stream, { inlineVolume: true, metadata: playQueue });
        playResource.volume.setVolume(currentVolume / 100);

        connection.state.subscription.player.play(playResource);

        this.destroyStream(stream, connection.state.subscription.player).catch(O_o => { });
        await this.client.database.db("guilds").collection("players").updateOne({ guildId: guildId }, { $set: { queueID: playQueueID, stopped: false } }, { upsert: true });

    }

    async destroyStream(stream, player) {

        stream.once("close", () => { stream.destroy() });
        player.once("idle", () => { stream.destroy() });

    }

}