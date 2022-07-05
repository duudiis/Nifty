const Commands = require("../../structures/Commands");

const { MessageEmbed } = require("discord.js");

module.exports = class extends Commands {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "perms";
        this.aliases = ["perm", "permission", "permissions"];

        this.description = "View and modify the permissions of a user or role";
        this.category = "music";

        this.usage = "perms @entity [ action ] [ permission ]";
        this.options = [
            {
                "name": "view",
                "description": "View the permissions of a user or role",
                "type": "SUB_COMMAND",
                "options": [
                    {
                        "name": "entity",
                        "description": "View the perms of a user or role",
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
                                "value": "ALLOW"
                            },
                            {
                                "name": "deny",
                                "value": "DENY"
                            },
                            {
                                "name": "clear",
                                "value": "CLEAR"
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
                                "value": "MANAGE_PLAYER"
                            },
                            {
                                "name": "manage queue",
                                "value": "MANAGE_QUEUE"
                            },
                            {
                                "name": "view queue",
                                "value": "VIEW_QUEUE"
                            },
                            {
                                "name": "add to queue",
                                "value": "ADD_TO_QUEUE"
                            },
                            {
                                "name": "all",
                                "value": "ALL"
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

        const deprecatedEmbed = new MessageEmbed({ color: message.guild.me.displayHexColor })
            .setDescription("This command has been deprecated! Please use /perms instead.")

        return message.channel.send({ embeds: [deprecatedEmbed] });

    }

    async runAsInteraction(interaction) {

        if (interaction.options._subcommand == "view") {

            const entity = interaction.options.get("entity").value;

            const response = await this.view(entity, interaction);
            return interaction.editReply({ embeds: [response.embed] });

        } else if (interaction.options._subcommand == "modify") {

            const entity = interaction.options.get("entity").value;
            const action = interaction.options.get("action").value;
            const permission = interaction.options.get("permission").value;

            const response = await this.modify(entity, action, permission, interaction);
            return interaction.editReply({ embeds: [response.embed] });

        };

    }

    async view(entity, command) {

        if (!command.member.permissions.has("MANAGE_GUILD")) {

            const missingPermsEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
                .setDescription("You must have the `Manage Server` permission to use this command!")

            return { code: "error", embed: missingPermsEmbed };
        };

        let permsDict = {
            "ALLOW": "✅ allowed",
            "DENY": "❌ denied",
            "CLEAR": "⚪ unspecified"
        }

        if (command.guild.roles.cache.has(entity)) {

            const roleData = await this.client.database.db("perms").collection(command.guild.id).findOne({ id: entity });

            let rolePerms = {
                addToQueue: roleData?.ADD_TO_QUEUE ? permsDict[roleData.ADD_TO_QUEUE] : permsDict["CLEAR"],
                viewQueue: roleData?.VIEW_QUEUE ? permsDict[roleData.VIEW_QUEUE] : permsDict["CLEAR"],
                manageQueue: roleData?.MANAGE_QUEUE ? permsDict[roleData.MANAGE_QUEUE] : permsDict["CLEAR"],
                managePlayer: roleData?.MANAGE_PLAYER ? permsDict[roleData.MANAGE_PLAYER] : permsDict["CLEAR"]
            };

            if (entity == command.guild.id) { entity = "@everyone"; } else { entity = `<@&${entity}>` };

            const permsViewRoleEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
                .setDescription(`${entity}'s Permissions\n\n**Add to Queue**\n${rolePerms.addToQueue}\n**View Queue**\n${rolePerms.viewQueue}\n**Manage Queue**\n${rolePerms.manageQueue}\n**Manage Player**\n${rolePerms.managePlayer}`)

            return { code: "success", embed: permsViewRoleEmbed };

        } else {

            const userPerms = await this.client.getUserPerms(command.guild.id, entity, true);

            const permsViewUserEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
                .setDescription(`<@${entity}>**'s Permissions**\n\n**Add to Queue**\n${permsDict[userPerms.addToQueue.value]} ${await this.getEntityInformation(command.guild, userPerms.addToQueue.entity)}\n**View Queue**\n${permsDict[userPerms.viewQueue.value]} ${await this.getEntityInformation(command.guild, userPerms.viewQueue.entity)}\n**Manage Queue**\n${permsDict[userPerms.manageQueue.value]} ${await this.getEntityInformation(command.guild, userPerms.manageQueue.entity)}\n**Manage Player**\n${permsDict[userPerms.managePlayer.value]} ${await this.getEntityInformation(command.guild, userPerms.managePlayer.entity)}`)

            return { code: "success", embed: permsViewUserEmbed };

        };

    }

    async modify(entity, action, permission, command) {

        if (!command.member.permissions.has("MANAGE_GUILD")) {

            const missingPermsEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
                .setDescription("You must have the `Manage Server` permission to use this command!")

            return { code: "error", embed: missingPermsEmbed };
        };

        if (permission == "ALL") {
            await this.client.database.db("perms").collection(command.guild.id).updateOne({ id: entity }, { $set: { "MANAGE_PLAYER": action, "MANAGE_QUEUE": action, "VIEW_QUEUE": action, "ADD_TO_QUEUE": action } }, { upsert: true });
        } else {
            await this.client.database.db("perms").collection(command.guild.id).updateOne({ id: entity }, { $set: { [permission]: action } }, { upsert: true });
        }

        this.client.getUserPerms(command.guild.id, entity, true, true);

        let actionDict = {
            "ALLOW": "**Granted** these permissions to",
            "DENY": "**Denied** these permissions from",
            "CLEAR": "**Cleared** these permissions from"
        }

        let permsDict = {
            "ADD_TO_QUEUE": "Add to Queue",
            "VIEW_QUEUE": "View Queue",
            "MANAGE_QUEUE": "Manage Queue",
            "MANAGE_PLAYER": "Manage Player",
            "ALL": "Add to Queue\nView Queue\nManage Queue\nManage Player",
        }

        let entityMention = await this.getEntityMention(command.guild, entity);

        const permsModifiedEmbed = new MessageEmbed({ color: command.guild.me.displayHexColor })
            .setDescription(`${actionDict[action]} ${entityMention}\n\`\`\`nim\n${permsDict[permission]}\`\`\``)

        return { code: "success", embed: permsModifiedEmbed };

    }

    async getEntityMention(guild, entityId) {

        let entityMention = `<@${entityId}>`;

        if (guild.roles.cache.has(entityId)) { entityMention = `<@&${entityId}>`; };
        if (entityId == guild.id) { entityMention = `@everyone`; };

        return entityMention;

    }

    async getEntityInformation(guild, entityId) {

        if (entityId == "default") { return "by default"; };

        if (entityId == guild.id) { return "by role @everyone" };
        if (guild.roles.cache.has(entityId)) { return `by role <@&${entityId}>`; };

        return `by direct override on <@${entityId}>`

    }

}