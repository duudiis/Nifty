const Modules = require("../structures/Modules");

module.exports = class RemoveFormatting extends Modules {

	constructor(client) {
		super(client);
		this.client = client;

		this.name = "removeFormatting";
	}

	async run(string, maxLength) {

		if (string.length >= maxLength) { string = string.slice(0, maxLength - 4).trimEnd() + "…" };

		string = string.replaceAll("*", "\\*");
		string = string.replaceAll("_", "\\_");
		string = string.replaceAll("~", "\\~");
		string = string.replaceAll("`", "\\`");
		
		if (string.includes("[") && string.includes("]")) {

			string = string.replaceAll("[", "\\[");
			string = string.replaceAll("]", "\\]");

		} else if (!string.includes("[") && string.includes("]")) {

			string = string.replaceAll("]", "\\]");

		}

		return string;

	}

}