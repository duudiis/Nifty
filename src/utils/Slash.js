module.exports = class Slash {

    constructor(client) {
        this.client = client;
    }

    async verifyCommands() {

        const applicationCommands = await this.client.application.commands.fetch()

        for (const localCommand of this.client.commands.values()) {

            if (localCommand.ignoreSlash) { continue };

            const command = {
                name: localCommand.name,
                description: localCommand.description,
                options: localCommand.options
            }

            const discordCommand = applicationCommands.find(c => c.name == command.name);
            if (!discordCommand) { this.client.application.commands.create(command); console.log(`[utils/slash] Created /${command.name} command.`); continue };

            const commandIntegrity = await this.compareCommands(discordCommand, command);
            if (commandIntegrity == false) { this.client.application.commands.create(command); console.log(`[utils/slash] Updated /${command.name} command.`); continue };

        }

        for (const discordCommand of applicationCommands) {

            const originalCommand = this.client.commands.has(discordCommand[1].name)
            if (!originalCommand) { this.client.application.commands.delete(discordCommand[0]); console.log(`[utils/slash] Deleted /${discordCommand[1].name} command.`); continue };

        }

    }

    async compareCommands(discordCommand, originalCommand) {

        if (discordCommand.name != originalCommand.name) { return false };
        if (discordCommand.description != originalCommand.description) { return false };

        if (!originalCommand.options) { originalCommand.options = [] };
        if (discordCommand.options.length != originalCommand.options.length) { return false };

        const optionsIntegrity = await this.compareOptions(discordCommand.options, originalCommand.options);
        if (optionsIntegrity == false) { return false }

        return true

    }

    async compareOptions(discordOptions, originalOptions) {

        for (const option of originalOptions) {
            const discordOption = discordOptions.find(o => o.name == option.name);
            if (!discordOption) { return false };

            if (discordOption.name != option.name) { return false };
            if (discordOption.description != option.description) { return false };
            if (option.type && discordOption.type != option.type) { return false };
            if (option.required && discordOption.required != option.required) { return false };

            if (!discordOption.choices) { discordOption.choices = [] }; if (!option.choices) { option.choices = [] };
            if (discordOption.choices.length != option.choices.length) { return false };

            for (const choice of option.choices) {
                const discordChoice = discordOption.choices.find(c => c.name == choice.name);
                if (!discordChoice) { return false };

                if (discordChoice.name != choice.name) { return false };
                if (discordChoice.value != choice.value) { return false };

            }

            if (!discordOption.options) { discordOption.options = [] }; if (!option.options) { option.options = [] };
            if (discordOption.options.length != option.options.length) { return false };

            const optionIntegrity = await this.compareOptions(discordOption.options, option.options);
            if (optionIntegrity == false) { return false };
        }

    }

}