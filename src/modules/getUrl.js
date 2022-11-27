const Modules = require("../structures/Modules");

const fetch = require("node-fetch");

module.exports = class extends Modules {

	constructor(client) {
		super(client);
		this.client = client;

		this.name = "getUrl";
	}

	async run(url) {
		
		let response = await fetch(`${url}`, {
            method: "GET"
        }).catch(() => { throw "An error occurred."; });

        if (response.status == 200) {
            return response.url;
        } else {
            throw "No matches found!";
        };

	}

}