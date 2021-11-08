const Events = require("../structures/Events");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class VoiceStateUpdate extends Events {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "voiceStateUpdate"
    }

    async run(oldState, newState) {

        let existingConnection = DiscordVoice.getVoiceConnection(oldState.guild.id);
        if (!existingConnection) { return; };

        if (oldState.member.id == this.client.user.id) { return this.botStateChange(oldState, newState, existingConnection); };

        if (!oldState.channel && newState.channel) {

            if (newState.channel.id == existingConnection.joinConfig.channelId) {
                this.updateTimer(existingConnection, newState.guild.id, "reset");
            }

        } else if (!newState.channel && oldState.channel) {

            if (oldState.channel.id == existingConnection.joinConfig.channelId && oldState.channel.members.size == 1) {
                this.updateTimer(existingConnection, oldState.guild.id, "create");
            }

        } else if (newState.channel && oldState.channel && newState.channelId != oldState.channelId) {

            if (oldState.channel.id == existingConnection.joinConfig.channelId && oldState.channel.members.size == 1) {
                this.updateTimer(existingConnection, oldState.guild.id, "create");
            }

            if (newState.channel.id == existingConnection.joinConfig.channelId) {
                this.updateTimer(existingConnection, newState.guild.id, "reset");
            }

        }

    }

    async botStateChange(oldState, newState, existingConnection) {

        if (!newState.channel && oldState.channel) {

            await this.client.player.updateNpMessage(oldState.guild.id, "delete");

            this.client.database.db("guilds").collection("players").deleteOne({ guildId: oldState.guild.id });
            this.client.database.db("queues").collection(oldState.guild.id).deleteMany({});

            try { existingConnection.state.subscription.player.stop(); } catch (e) { }

            clearTimeout(existingConnection.playTimer); clearTimeout(existingConnection.pauseTimer); clearTimeout(existingConnection.aloneTimer);
            existingConnection.destroy();

        } else if (newState.channel && oldState.channel && newState.channelId != oldState.channelId) {

            if (newState.channel && newState.channel.members.size == 1) {

                this.updateTimer(existingConnection, newState.guild.id, "create");

            } else if (newState.channel && newState.channel.members.size > 1) {

                this.updateTimer(existingConnection, newState.guild.id, "reset");

            }

            if (newState.channel.type == "GUILD_STAGE_VOICE") {
                await newState.guild.me.voice.setSuppressed(false).catch(e => { newState.guild.me.voice.setRequestToSpeak(true); });
            }

        }

    }

    async updateTimer(connection, guildId, action) {

        if (action == "create") {

            clearTimeout(connection.aloneTimer);
            connection.aloneTimer = setTimeout(() => { this.client.player.inactivityDisconnect(guildId); }, 300000);

        } else if (action == "reset") {

            clearTimeout(connection.aloneTimer);

        }

    }

}