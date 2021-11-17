module.exports = {
    allowedMentions: { parse: ['users'] },
    failIfNotExists: false,
    restTimeOffset: 0,
    retryLimit: 2,
    partials: ['REACTION', 'MESSAGE', 'CHANNEL'],
    intents: ["GUILDS", "GUILD_MESSAGES", "GUILD_MESSAGE_REACTIONS", "GUILD_VOICE_STATES", "DIRECT_MESSAGES"]
};