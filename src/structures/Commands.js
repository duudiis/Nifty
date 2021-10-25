module.exports = class Commands {

    constructor(client) {
        this.client = client;

        this.name = "";
        this.aliases = [""];

        this.description = "";
        this.category = "";

        this.usage = "";
        this.options = []

        this.enabled = true;
        this.ownersOnly = false;
        this.ignoreSlash = false;
    }

}