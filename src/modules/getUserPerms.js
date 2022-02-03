const { CommandFailedEvent } = require("mongodb");
const Modules = require("../structures/Modules");

module.exports = class extends Modules {

    constructor(client) {
        super(client);
        this.client = client;

        this.name = "getUserPerms";
    }

    async run(guildId, memberId) {

        const guild = this.client.guilds.cache.get(guildId);
        const member = await guild.members.fetch(memberId);

        let userPerms = {
            addToQueue: {
                value: "CLEAR",
                entity: "default"
            },
            viewQueue: {
                value: "CLEAR",
                entity: "default"
            },
            manageQueue: {
                value: "CLEAR",
                entity: "default"
            },
            managePlayer: {
                value: "CLEAR",
                entity: "default"
            },
            array: []
        };

        const userData = await this.client.database.db("perms").collection(guildId).findOne({ id: memberId });

        if (userData?.ADD_TO_QUEUE && userData?.ADD_TO_QUEUE != "CLEAR" && userPerms.addToQueue.value == "CLEAR") {
            userPerms.addToQueue.value = userData?.ADD_TO_QUEUE || "ALLOW";
            userPerms.addToQueue.entity = memberId;
        };

        if (userData?.VIEW_QUEUE && userData?.VIEW_QUEUE != "CLEAR" && userPerms.viewQueue.value == "CLEAR") {
            userPerms.viewQueue.value = userData?.VIEW_QUEUE || "ALLOW";
            userPerms.viewQueue.entity = memberId;
        };

        if (userData?.MANAGE_QUEUE && userData?.MANAGE_QUEUE != "CLEAR" && userPerms.manageQueue.value == "CLEAR") {
            userPerms.manageQueue.value = userData?.MANAGE_QUEUE || "ALLOW";
            userPerms.manageQueue.entity = memberId;
        };

        if (userData?.MANAGE_PLAYER && userData?.MANAGE_PLAYER != "CLEAR" && userPerms.managePlayer.value == "CLEAR") {
            userPerms.managePlayer.value = userData?.MANAGE_PLAYER || "ALLOW";
            userPerms.managePlayer.entity = memberId;
        };

        for (const memberRole of member.roles.cache.values()) {

            const roleData = await this.client.database.db("perms").collection(guildId).findOne({ id: memberRole.id });

            if (roleData?.ADD_TO_QUEUE && roleData?.ADD_TO_QUEUE != "CLEAR" && userPerms.addToQueue.value == "CLEAR") {
                userPerms.addToQueue.value = roleData?.ADD_TO_QUEUE || "ALLOW";
                userPerms.addToQueue.entity = memberRole.id;
            };

            if (roleData?.VIEW_QUEUE && roleData?.VIEW_QUEUE != "CLEAR" && userPerms.viewQueue.value == "CLEAR") {
                userPerms.viewQueue.value = roleData?.VIEW_QUEUE || "ALLOW";
                userPerms.viewQueue.entity = memberRole.id;
            };

            if (roleData?.MANAGE_QUEUE && roleData?.MANAGE_QUEUE != "CLEAR" && userPerms.manageQueue.value == "CLEAR") {
                userPerms.manageQueue.value = roleData?.MANAGE_QUEUE || "ALLOW";
                userPerms.manageQueue.entity = memberRole.id;
            };

            if (roleData?.MANAGE_PLAYER && roleData?.MANAGE_PLAYER != "CLEAR" && userPerms.managePlayer.value == "CLEAR") {
                userPerms.managePlayer.value = roleData?.MANAGE_PLAYER || "ALLOW";
                userPerms.managePlayer.entity = memberRole.id;
            };

        };

        if (userPerms.addToQueue.value == "CLEAR") { userPerms.addToQueue.value = "ALLOW"; };
        if (userPerms.viewQueue.value == "CLEAR") { userPerms.viewQueue.value = "ALLOW"; };
        if (userPerms.manageQueue.value == "CLEAR") { userPerms.manageQueue.value = "ALLOW"; };
        if (userPerms.managePlayer.value == "CLEAR") { userPerms.managePlayer.value = "ALLOW"; };

        if (userPerms.addToQueue.value == "ALLOW") { userPerms.array.push("ADD_TO_QUEUE"); };
        if (userPerms.viewQueue.value == "ALLOW") { userPerms.array.push("VIEW_QUEUE"); };
        if (userPerms.manageQueue.value == "ALLOW") { userPerms.array.push("MANAGE_QUEUE"); };
        if (userPerms.managePlayer.value == "ALLOW") { userPerms.array.push("MANAGE_PLAYER"); };

        return userPerms;

    }

}