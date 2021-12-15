const Commands = require("../../structures/Commands");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "shuffle";
        this.aliases = ["shu", "sh", "random", "randomize", "randomizer"];

        this.description = "Shuffles the queue";
        this.category = "music";

        this.usage = "shuffle";
        this.options = []

        this.enabled = true;
    }

    async runAsMessage(message) {

        const input = message.array.slice(1).join(" ");

        let mode = undefined;

        if (this.client.constants.keywords.enabled.includes(input.toLowerCase())) { mode = "on" };
        if (this.client.constants.keywords.disabled.includes(input.toLowerCase())) { mode = "off" };

        if (!mode) {

            const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: message.guild.id });

            let playerShuffle = playerData?.shuffle;
            if (!playerShuffle) { playerShuffle = "off" };

            let nextShuffle = {
                "on": "off",
                "off": "on"
            }

            mode = nextShuffle[playerShuffle];

        };

        const response = await this.shuffle(mode, message);
        return message.reply({ embeds: [response.embed] });

    }

    async runAsInteraction(interaction) {

        const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: interaction.guild.id });

        let playerShuffle = playerData?.shuffle;
        if (!playerShuffle) { playerShuffle = "off" };

        let nextShuffle = {
            "on": "off",
            "off": "on"
        }

        let mode = nextShuffle[playerShuffle];

        const response = await this.shuffle(mode, interaction);
        return interaction.editReply({ embeds: [response.embed] });

    }

    async shuffle(mode, command) {

        const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error });

        const voiceChannel = command.member.voice.channel;
        if (!voiceChannel) { return { code: "error", embed: errorEmbed.setDescription("You have to be connected to a voice channel before you can use this command!") } };

        let existingConnection = DiscordVoice.getVoiceConnection(command.guild.id);
        if (existingConnection && existingConnection.joinConfig.channelId != voiceChannel.id) { return { code: "error", embed: errorEmbed.setDescription("Someone else is already listening to music in different channel!") } };

        if (!existingConnection) {
            try { await this.client.player.joinChannel(voiceChannel, command) } catch (error) { return { code: "error", embed: errorEmbed.setDescription(`${error.message ? error.message : error}`) }; };
            existingConnection = DiscordVoice.getVoiceConnection(command.guild.id);
        };

        await this.client.database.db("guilds").collection("players").updateOne({ guildId: command.guild.id }, { $set: { shuffle: mode } }, { upsert: true });
        if (mode == "on") { await this.shuffleQueue(command.guild.id); };

        let shuffleMessage = {
            "on": "Shuffle mode has been **enabled**",
            "off": "Shuffle mode has been **disabled**"
        }

        const shuffleEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
            .setDescription(shuffleMessage[mode])

        return { code: "success", embed: shuffleEmbed };

    }

    async shuffleQueue(guildId) {

        const playerData = await this.client.database.db("guilds").collection("players").findOne({ guildId: guildId });
        const queueData = await this.client.database.db("queues").collection(guildId).find({}).toArray();

        if (queueData.length == 0) { return; };
        if (playerData.queueID == queueData.length - 1) { return; };

        let queueBefore = queueData.slice(0, -((queueData.length - playerData.queueID) - 1));
        let shuffledQueue = queueData.slice((playerData.queueID + 1));

        shuffledQueue = await this.client.shuffleArray(shuffledQueue);

        let newQueue = [...queueBefore, ...shuffledQueue];

        await this.client.database.db("queues").collection(guildId).drop().catch(e => {});
        await this.client.database.db("queues").collection(guildId).insertMany(newQueue);

    }

}