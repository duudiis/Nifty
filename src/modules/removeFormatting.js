const Modules = require("../structures/Modules");

module.exports = class extends Modules {

	constructor(client) {
		super(client);
		this.client = client;

		this.name = "removeFormatting";
	}

	async run(string, maxLength) {

		// string = string.replace(/ *\([^)]*\) */g, "");
		// string = string.replace(/ *\[[^\]]*\] */g, "");

		if (string.length >= maxLength) { string = string.slice(0, maxLength - 4).trimEnd() + "…"; };

		string = string.replaceAll("*", "\\*");
		string = string.replaceAll("_", "\\_");
		string = string.replaceAll("~", "\\~");
		string = string.replaceAll("`", "\\`");
		string = string.replaceAll("|", "\\|");
		
		string = string.replaceAll("[", "(");
		string = string.replaceAll("]", ")");

		return string;

	}

}