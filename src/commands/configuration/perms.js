const Commands = require("../../structures/Commands");

const DiscordVoice = require('@discordjs/voice');
const { MessageEmbed } = require("discord.js");

module.exports = class extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "perms";
        this.aliases = ["perm", "permission", "permissions"];

        this.description = "View and modify the permissions of a user or role";
        this.category = "music";

        this.usage = "perms @entity [ action permission ]";
        this.options = [
            {
                "name": "view",
                "description": "View the permissions of a user or role",
                "type": "SUB_COMMAND",
                "options": [
                    {
                        "name": "entity",
                        "description": "View the permissions of a user or role",
                        "type": "MENTIONABLE",
                        "required": true
                    }
                ]
            },
            {
                "name": "modify",
                "description": "Modify the permissions of a user or role",
                "type": "SUB_COMMAND",
                "options": [
                    {
                        "name": "entity",
                        "description": "The user or role to modify the perms of",
                        "type": "MENTIONABLE",
                        "required": true
                    },
                    {
                        "name": "action",
                        "description": "The action to take",
                        "type": "STRING",
                        "choices": [
                            {
                                "name": "allow",
                                "value": "allow"
                            },
                            {
                                "name": "deny",
                                "value": "deny"
                            },
                            {
                                "name": "clear",
                                "value": "clear"
                            }
                        ],
                        "required": true
                    },
                    {
                        "name": "permission",
                        "description": "The permissions to update",
                        "type": "STRING",
                        "choices": [
                            {
                                "name": "manage player",
                                "value": "Manage Player"
                            },
                            {
                                "name": "manage queue",
                                "value": "Manage Queue"
                            },
                            {
                                "name": "view queue",
                                "value": "View Queue"
                            },
                            {
                                "name": "add to queue",
                                "value": "Add to Queue"
                            },
                            {
                                "name": "all",
                                "value": "All"
                            }
                        ],
                        "required": true
                    },
                ]
            }
        ];

        this.requiredPermissions = [];

        this.enabled = true;
    }

    async runAsMessage(message) {



    }

    async runAsInteraction(interaction) {



    }

}