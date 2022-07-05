require('dotenv').config({ path: ".env" });

const Discord = require("discord.js");
const { MongoClient } = require("mongodb");

const client = new Discord.Client(require("../config/Discord"));

MongoClient.connect(process.env.MONGO_URI, { useUnifiedTopology: true }, function (error, response) { client.database = response; });
// MongoClient.connect(process.env.GLOBAL_MONGO_URI, { useUnifiedTopology: true }, function (error, response) { client.global = response; });

process.on('unhandledRejection', error => { console.log(error); });

const path = require("path");
const fs = require("fs").promises;

const Loaders = require("./structures/Loaders");

async function startLoaders(directory) {

    const filePath = path.join(__dirname, directory);
    const files = await fs.readdir(filePath);

    for (const file of files) {
        const fileStat = await fs.lstat(path.join(filePath, file));

        if (fileStat.isDirectory()) { await startLoaders(path.join(directory, file)); }

        else if (file.endsWith(".js")) {
            const Loader = require(path.join(filePath, file));

            if (Loader.prototype instanceof Loaders) {
                const loader = new Loader(client);
                await loader.load();
            }
        }
    }

}

(async () => {

    await startLoaders("./loaders");
    client.login(process.env.DISCORD_TOKEN);

})();