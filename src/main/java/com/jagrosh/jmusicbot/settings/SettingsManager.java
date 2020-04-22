/*
 * Copyright 2018 John Grosh <john.a.grosh@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.jmusicbot.settings;

import com.jagrosh.jdautilities.command.GuildSettingsManager;
import com.jagrosh.jmusicbot.utils.OtherUtil;
import net.dv8tion.jda.core.entities.Guild;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class SettingsManager implements GuildSettingsManager
{
    private final HashMap<Long,Settings> settings;

    public SettingsManager()
    {
        this.settings = new HashMap<>();
        try {
            JSONObject loadedSettings = new JSONObject(new String(Files.readAllBytes(OtherUtil.getPath("serversettings.json"))));
            loadedSettings.keySet().forEach((id) -> {
                JSONObject o = loadedSettings.getJSONObject(id);
                settings.put(Long.parseLong(id), new Settings(this,
                        o.has("text_channel_id") ? o.getString("text_channel_id")    : null,
                        o.has("voice_channel_id")? o.getString("voice_channel_id")   : null,
                        o.has("dj_role_id")      ? o.getString("dj_role_id")         : null,
                        o.has("volume")          ? o.getInt("volume")                : 100,
                        o.has("default_playlist")? o.getString("default_playlist")   : null,
                        o.has("repeat")          ? RepeatMode.fromObject(o.get("repeat")): RepeatMode.off,
                        o.has("prefix")          ? o.getString("prefix")             : null));
            });
        } catch(IOException | JSONException e) {
            LoggerFactory.getLogger("Settings").warn("Failed to load server settings (this is normal if no settings have been set yet): "+e);
        }
    }

    /**
     * Gets non-null settings for a Guild
     *
     * @param guild the guild to get settings for
     * @return the existing settings, or new settings for that guild
     */
    @Override
    public Settings getSettings(Guild guild)
    {
        return getSettings(guild.getIdLong());
    }

    public Settings getSettings(long guildId)
    {
        return settings.computeIfAbsent(guildId, id -> createDefaultSettings());
    }

    private Settings createDefaultSettings()
    {
        return new Settings(this, 0, 0, 0, 100, null, RepeatMode.off, null);
    }

    protected void writeSettings()
    {
        JSONObject obj = new JSONObject();
        settings.keySet().forEach(key -> {
            JSONObject o = new JSONObject();
            Settings s = settings.get(key);
            if(s.textId!=0)
                o.put("text_channel_id", Long.toString(s.textId));
            if(s.voiceId!=0)
                o.put("voice_channel_id", Long.toString(s.voiceId));
            if(s.roleId!=0)
                o.put("dj_role_id", Long.toString(s.roleId));
            if(s.getVolume()!=100)
                o.put("volume",s.getVolume());
            if(s.getDefaultPlaylist() != null)
                o.put("default_playlist", s.getDefaultPlaylist());
            o.put("repeat", s.getRepeatMode().toString());
            if(s.getPrefix() != null)
                o.put("prefix", s.getPrefix());
            obj.put(Long.toString(key), o);
        });
        try {
            Files.write(OtherUtil.getPath("serversettings.json"), obj.toString(4).getBytes());
        } catch(IOException ex){
            LoggerFactory.getLogger("Settings").warn("Failed to write to file: "+ex);
        }
    }
}
