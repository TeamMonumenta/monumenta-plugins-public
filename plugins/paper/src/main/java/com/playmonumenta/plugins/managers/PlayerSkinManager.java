package com.playmonumenta.plugins.managers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.HashMap;
import java.util.Map;

public class PlayerSkinManager {

	// villager2.png
	public static final SkinData FALLBACK_SKIN = new SkinData("fallback", "fallback", "ewogICJ0aW1lc3RhbXAiIDogMTU5MDE2NTA3MjI2MiwKICAicHJvZmlsZUlkIiA6ICJiNjJhM2Q2YmM0NGM0OGU4OTNjODViOTkxNzc1NWI2YyIsCiAgInByb2ZpbGVOYW1lIiA6ICJQZXBlQ29tZVBhcGEiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjg3OWUzNjYxYjUwZjIwMzE3ZmVhMTFhNGU3NzVjODBjMDU1OWMxMjQyZTY1NWY4MTY4MGUwOGE0ZWRlMzQzMiIKICAgIH0KICB9Cn0=", "uSOSe8xCAdZ1qRds1DY7AwLCUHsMRhdCIMglElGDACBBTvcZoKTw9HZDMXfa0bsfTK73vFIyyi/SZuItdopNOIA3zLSND+6aoaeeZiMJ2UjtAyh8Fo+rXtsHmUt9VkOY3ED/uE8fhumcNhcI1ahowBrCOCO2A5Gu/Ij3VP9y8XVl7d4P2qVBmeVpAvua+3IaCjSiiFPTWSbP56hfO/C+2yQcK03BPLmMvjSqH47V7UQ9LsbBC175UpxBRkQbZW9ZlRTAfR/1DTVyOOcBa0KIZn+V5tvLsJFkDIJXvcueEkIwFnBFj4CASTbpu7/gNqRl5C0owDhBz+zzak3gXT8/ufGTufFmriU3dBzJJ9b8nMc25UvSMWIMqqbaPVyg6kcwNE40OFkdKv2iRCHrjk3GQzENbG/lOcHtaO+0WM5iySe/6iXoQasgiSJtAuDphtCAri9ZKjKBnjEYQcUdg6DvPCkLezXD8HpDEMqIBiAU/odsrTzqhH++RBRAstP57ovEvJcMO2mabr4SphkHjWH7vctD1ByMohQknv9noB5Y7IRQlFEy3PHD/pUooJ3DWmcc3IslJDynws8pbepOb60f7Mq0wIJwyik09W+G9NtRjei9WLbHjdaeNIem75tqiLNAIPa2isxLvKQF4/ep3EWfqz6qCidcQ54D9RF0q3RhW5k=");
	public static Map<String, SkinData> textureMap = new HashMap<>();

	public record SkinData(String key, String name, String texture, String signature) {
	}

	public PlayerSkinManager() {
		readFile();
	}

	public static SkinData create(String key, JsonObject input) {
		String name = input.get("name").getAsString();
		String texture = input.get("texture").getAsString();
		String signature = input.get("signature").getAsString();
		return new SkinData(key, name, texture, signature);
	}

	public static boolean readFile() {
		JsonObject textures;
		try {
			JsonObject json = FileUtils.readJson(Plugin.getInstance().getDataFolder() + "/skins.json");
			textures = json.get("textures").getAsJsonObject();
		} catch (Exception ex) {
			MMLog.severe("Skins failed to load! :c " + ex);
			return false;
		}
		Map<String, SkinData> temp = new HashMap<>();
		for (Map.Entry<String, JsonElement> t : textures.entrySet()) {
			SkinData skinData = create(t.getKey(), t.getValue().getAsJsonObject());
			temp.put(skinData.name, skinData);
		}
		textureMap = temp;
		return true;
	}

	public static SkinData fetchSkinByName(String name) {
		SkinData response = textureMap.get(name);
		return response != null ? response : fetchFallbackSkin();
	}

	// Calder's skin for now, should be swapped to a variety
	public static SkinData fetchFallbackSkin() {
		SkinData response = textureMap.get("villager174");
		return response != null ? response : FALLBACK_SKIN;
	}
}
