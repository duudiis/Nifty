const Loaders = require("../structures/Loaders");

const Modules = require("../structures/Modules");

const path = require("path");
const fs = require("fs").promises;

module.exports = class extends Loaders {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "Modules";
    }

    async load() {

        await this.loadFolder("../modules");

    }

    async loadFolder(directory) {
        const filePath = path.join(__dirname, directory);
        const files = await fs.readdir(filePath);

        for (const file of files) {
            const fileStat = await fs.lstat(path.join(filePath, file));

            if (fileStat.isDirectory()) { await this.loadFolder(path.join(directory, file)) }
            else if (file.endsWith(".js")) {
                const Module = require(path.join(filePath, file));

                if (Module.prototype instanceof Modules) {
                    await this.registerModule(Module);
                }
            }
        }
    }

    async registerModule(Module) {

        const module = new Module(this.client);

        if (module.subcategory) {

            if (!this.client[module.subcategory]) { this.client[module.subcategory] = {} };
            this.client[module.subcategory][module.name] = async (...args) => module.run(...args);

        } else {

            this.client[module.name] = async (...args) => module.run(...args);

        }

    }

}