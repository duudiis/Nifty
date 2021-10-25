const Loaders = require("../structures/Loaders");

const Events = require("../structures/Events");

const path = require("path");
const fs = require("fs").promises;

module.exports = class extends Loaders {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "Events";
    }

    async load() {

        await this.loadFolder("../events");

    }

    async loadFolder(directory) {
        const filePath = path.join(__dirname, directory);
        const files = await fs.readdir(filePath);

        for (const file of files) {
            const fileStat = await fs.lstat(path.join(filePath, file));

            if (fileStat.isDirectory()) { await this.loadFolder(path.join(directory, file)) }

            else if (file.endsWith(".js")) {
                const Event = require(path.join(filePath, file));

                if (Event.prototype instanceof Events) {
                    await this.listenEvent(Event)
                }
            }
        }
    }

    async listenEvent(Event) {

        const event = new Event(this.client);
        this.client.on(event.name, (...args) => event.run(...args));

    }

}