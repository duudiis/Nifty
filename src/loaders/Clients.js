const Loaders = require("../structures/Loaders");

const Clients = require("../structures/Clients");

const path = require("path");
const fs = require("fs").promises;

module.exports = class extends Loaders {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "Clients";
    }

    async load() {

        await this.loadFolder("../clients");

    }

    async loadFolder(directory) {
        const filePath = path.join(__dirname, directory);
        const files = await fs.readdir(filePath);
    
        for (const file of files) {
            const fileStat = await fs.lstat(path.join(filePath, file));

            if (fileStat.isDirectory()) { await this.loadFolder(path.join(directory, file)); }
            else if (file.endsWith(".js")) {
                const Client = require(path.join(filePath, file));
                
                if (Client.prototype instanceof Clients) {
                    await this.registerClient(Client);
                }
            }
        }
    }

    async registerClient(Client) {

        const api = new Client();
        this.client[api.name] = api;

    }

}