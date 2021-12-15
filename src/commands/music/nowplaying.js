const Commands = require("../../structures/Commands");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "now";
        this.aliases = ["nowplaying", "np"];

        this.description = "Display the playing track";
        this.category = "music";

        this.usage = "now";
        this.options = [
            {
                "name": "playing",
                "description": "Display the playing track",
                "type": "SUB_COMMAND"
            }
        ]

        this.enabled = true;
    }

    async runAsMessage(message) {

        const response = await this.nowPlaying(message);
        return message.reply({ embeds: [response.embed] });

    }

    async runAsInteraction(interaction) {

        const response = await this.nowPlaying(interaction);
        return interaction.editReply({ embeds: [response.embed] });

    }

    async nowPlaying(command) {

        const errorEmbed = new MessageEmbed({ color: this.client.constants.colors.error });

        let existingConnection = DiscordVoice.getVoiceConnection(command.guild.id);
        if (!existingConnection?.state?.subscription?.player?.state?.resource?.metadata) { return { code: "error", embed: errorEmbed.setDescription("You must be playing a track to use this command!") } };

        const nowPlaying = existingConnection.state.subscription.player.state.resource.metadata;
        const timeBar = await this.getTimeBar(existingConnection.state.subscription.player.state.resource.playbackDuration / 1000, nowPlaying.duration);

        let nowPlayingEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
            .setDescription(`[${await this.client.removeFormatting(nowPlaying.title, 56)}](${nowPlaying.url}) [<@${nowPlaying.user}>]`)
            .setFooter(timeBar)

        return { code: "success", embed: nowPlayingEmbed };

    }

    async getTimeBar(played, limit) {

        const percentage = Math.floor((played / limit) * 20);
        
        let bar = "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";
        bar = bar.split("")

        bar[percentage] = "🔵";
        bar = bar.join("")

        return `${bar} ${await this.toHHMMSS(played)} / ${await this.toHHMMSS(limit)}`

    }

    async toHHMMSS(secs) {
		let sec_num = parseInt(secs, 10);
		let hours = Math.floor(sec_num / 3600);
		let minutes = Math.floor(sec_num / 60) % 60;
		let seconds = sec_num % 60;
	
		return [`${hours}h`,`${minutes}m`,`${seconds}s`].filter((v,i) => v.slice(0, -1) !== "0").join(" ");

	}

}