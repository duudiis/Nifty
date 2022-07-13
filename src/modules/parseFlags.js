const Modules = require("../structures/Modules");

module.exports = class extends Modules {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "parseFlags";

        this.flags = {
            all: ["-a", "-all"],
            choose: ["-c", "-choose", "-search"],
            jump: ["-j", "-jump"],
            next: ["-n", "-next"],
            reverse: ["-r", "-reverse"],
            shuffle: ["-s", "-shuffle"]
        }
    }

    async run(string) {

        let words = string.split(" ");

        let flags = [];

        for (let word of words) {

            for (let [name, values] of Object.entries(this.flags)) {

                if (values.includes(word)) {

                    flags.push(name);
                    words = words.filter(s => s != word);

                }

            }

        }

        string = words.join(" ");

        return { code: "success", string, flags };

    }

}