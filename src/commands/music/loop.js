const Commands = require("../../structures/Commands");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "loop";
        this.aliases = ["l", "looping"];

        this.description = "Changes the looping mode";
        this.category = "music";

        this.usage = "loop";
        this.options = [
            {
                "name": "mode",
                "description": "The new looping mode",
                "type": "STRING",
                "choices": [
                    {
                        "name": "track",
                        "value": "track"
                    },
                    {
                        "name": "queue",
                        "value": "queue"
                    },
                    {
                        "name": "disabled",
                        "value": "disabled"
                    }
                ],
                "required": true
            }
        ];

        this.requiredPermissions = ["Manage Player"];

        this.enabled = true;
    }

    async runAsMessage(message) {

        const input = message.array.slice(1).join(" ");

        let mode = undefined;

        if (this.client.constants.keywords.loop.queue.includes(input.toLowerCase())) { mode = "queue" };
        if (this.client.constants.keywords.loop.track.includes(input.toLowerCase())) { mode = "track" };
        if (this.client.constants.keywords.disabled.includes(input.toLowerCase())) { mode = "disabled" };

        if (!mode) {

            const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: message.guild.id });

            let playerLoop = playerData?.loop;
            if (!playerLoop) { playerLoop = "disabled" };

            let nextLoop = {
                "disabled": "queue",
                "queue": "track",
                "track": "disabled"
            }

            mode = nextLoop[playerLoop];

        };

        const response = await this.loop(mode, message);
        return message.reply({ embeds: [response.embed] });

    }

    async runAsInteraction(interaction) {

        const mode = interaction.options.get("mode").value;

        const response = await this.loop(mode, interaction);
        return interaction.editReply({ embeds: [response.embed] });

    }

    async loop(mode, command) {

        const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error });

        const voiceChannel = command.member.voice.channel;
        if (!voiceChannel) { return { code: "error", embed: errorEmbed.setDescription("You have to be connected to a voice channel before you can use this command!") } };

        let existingConnection = DiscordVoice.getVoiceConnection(command.guild.id);
        if (existingConnection && existingConnection.joinConfig.channelId != voiceChannel.id) { return { code: "error", embed: errorEmbed.setDescription("Someone else is already listening to music in different channel!") } };

        const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: command.guild.id });
        if (playerData?.autoplay == "on") { return { code: "error", embed: errorEmbed.setDescription("AutoPlay and Loop cannot both be enabled at the same time!") }; };

        if (!existingConnection) {
            try { await this.client.player.joinChannel(voiceChannel, command) } catch (error) { return { code: "error", embed: errorEmbed.setDescription(`${error.message ? error.message : error}`) }; };
            existingConnection = DiscordVoice.getVoiceConnection(command.guild.id);
        };

        await this.client.database.db("guilds").collection("players").updateOne({ guildId: command.guild.id }, { $set: { loop: mode } }, { upsert: true });

        let loopingMessage = {
            "disabled": "Looping is now **disabled**.",
            "queue": "Now looping the **queue**.",
            "track": "Now looping the **current track**."
        }

        const loopEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
            .setDescription(loopingMessage[mode])

        return { code: "success", embed: loopEmbed };

    }

}