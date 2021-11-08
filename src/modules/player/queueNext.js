const Modules = require("../../structures/Modules");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class QueueNext extends Modules {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "queueNext";
        this.subcategory = "player";
    }

    async run(connection, guildId) {

        let playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: guildId });
        let queueData = await this.client.database.db("queues").collection(guildId).find({}).toArray();

        if (!playerData || !queueData) { return };

        if (connection.state.subscription.player.skipExecute == true) { return connection.state.subscription.player.skipExecute = false; };

        let nextQueueID = playerData.queueID + 1;
        let nextQueue = queueData[nextQueueID];

        if (!nextQueue && playerData.autoplay == "on") {

            try { await this.client.player.getAutoplayTrack(guildId); } catch (error) {

                await this.client.database.db("guilds").collection("players").updateOne({ guildId: guildId }, { $set: { stopped: true } }, { upsert: true })

                const announcesChannel = await this.client.channels.fetch(playerData.announcesId).catch(e => { });

                const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error })
                    .setDescription(`${error.message ? error.message : error}`)

                if (announcesChannel && announcesChannel.permissionsFor(this.client.user.id).has("SEND_MESSAGES") && !announcesChannel.permissionsFor(this.client.user.id).has("EMBED_LINKS")) {
                    announcesChannel.send({ embeds: [errorEmbed] });
                }

                return await this.client.database.db("guilds").collection("players").updateOne({ guildId: guildId }, { $set: { stopped: true } }, { upsert: true });
            }

            queueData = await this.client.database.db("queues").collection(guildId).find({}).toArray();

            nextQueueID = queueData.length - 1;
            nextQueue = queueData[nextQueueID];

        }

        if (!nextQueue && playerData.loop == "queue") {

            nextQueueID = 0;
            nextQueue = queueData[0];

        }

        if (playerData.loop == "track") {

            nextQueueID = playerData.queueID;
            nextQueue = queueData[nextQueueID];

        }

        if (!nextQueue) { return await this.client.database.db("guilds").collection("players").updateOne({ guildId: guildId }, { $set: { stopped: true } }, { upsert: true }) };

        await this.client.database.db("guilds").collection("players").updateOne({ guildId: guildId }, { $set: { queueID: nextQueueID } }, { upsert: true });

        return this.client.player.updatePlayer(connection, guildId);

    }

}