const Loaders = require("../structures/Loaders");

const Commands = require("../structures/Commands");

const path = require("path");
const fs = require("fs").promises;

module.exports = class extends Loaders {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "Commands";
    }

    async load() {

        this.client.commands = new Map();
        this.client.aliases = new Map();

        await this.loadFolder("../commands");

    }

    async loadFolder(directory) {
        const filePath = path.join(__dirname, directory);
        const files = await fs.readdir(filePath);
    
        for (const file of files) {
            const fileStat = await fs.lstat(path.join(filePath, file));

            if (fileStat.isDirectory()) { await this.loadFolder(path.join(directory, file)); }
    
            else if (file.endsWith(".js")) {
                const Command = require(path.join(filePath, file));
                
                if (Command.prototype instanceof Commands) {
                    await this.registerCommand(Command);
                }
            }
        }
    }

    async registerCommand(Command) {

        const command = new Command(this.client);
        
        this.client.commands.set(command.name, command);

        for (const alias of command.aliases) {
            this.client.aliases.set(alias, command.name);
        }

    }

}