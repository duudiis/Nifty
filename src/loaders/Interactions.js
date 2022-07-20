const Loaders = require("../structures/Loaders");

const Interactions = require("../structures/Interactions");

const path = require("path");
const fs = require("fs").promises;

module.exports = class extends Loaders {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "Interactions";
    }

    async load() {

        this.client.interactions = new Map();

        await this.loadFolder("../interactions");

    }

    async loadFolder(directory) {
        const filePath = path.join(__dirname, directory);
        const files = await fs.readdir(filePath);
    
        for (const file of files) {
            const fileStat = await fs.lstat(path.join(filePath, file));

            if (fileStat.isDirectory()) { await this.loadFolder(path.join(directory, file)); }
            else if (file.endsWith(".js")) {
                const Interaction = require(path.join(filePath, file));
                
                if (Interaction.prototype instanceof Interactions) {
                    await this.registerInteraction(Interaction);
                }
            }
        }
    }

    async registerInteraction(Interaction) {

        const interaction = new Interaction(this.client);
        this.client.interactions.set(interaction.name, interaction);

    }

}