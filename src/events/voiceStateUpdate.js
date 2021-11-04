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

    async updateTimer(connection, guildId, action) {

        if (action == "create") {

            connection.aloneTimer = setTimeout(() => { this.client.player.inactivityDisconnect(guildId); }, 300000);

        } else if (action == "reset") {

            clearTimeout(connection.aloneTimer);

        }

    }

}