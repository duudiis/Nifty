package me.nifty.utils.formatting;

import me.nifty.managers.JDAManager;
import me.nifty.websocket.WebSocketClientEndpoint;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import org.json.JSONObject;

public class WsGuild {

    public static Long getGuildForUserId(long userId) {

        JDA jda = JDAManager.getJDA();

        Long guildId = null;

        for (Guild guild : jda.getGuilds()) {

            Member member = guild.getMemberById(userId);
            if (member == null) { continue; }

            GuildVoiceState voiceState = member.getVoiceState();
            if (voiceState == null || !voiceState.inAudioChannel()) { continue; }

            guildId = guild.getIdLong();

        }

        return guildId;

    }

    public static void setGuildForUserId(long userId, Long guildId) {

        JSONObject returnMessage = new JSONObject();
        returnMessage.put("operation", "update_guild");

        JSONObject data = new JSONObject();
        data.put("userId", String.valueOf(userId));
        data.put("guildId", String.valueOf(guildId));

        returnMessage.put("data", data);

        WebSocketClientEndpoint.send(returnMessage.toString());

    }

}
