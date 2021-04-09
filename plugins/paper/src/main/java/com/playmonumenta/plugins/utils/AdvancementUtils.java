package com.playmonumenta.plugins.utils;

import java.util.Iterator;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class AdvancementUtils {
	public static JsonObject getAsJsonObject(Player player) {
		Gson gson = new Gson();
		JsonObject returnData = new JsonObject();
		Iterator<Advancement> iterator = Bukkit.advancementIterator();
		while (iterator.hasNext()) {
			Advancement advancement = iterator.next();
			AdvancementProgress progress = player.getAdvancementProgress(advancement);
			JsonElement awardedCriteria = gson.toJsonTree(progress.getAwardedCriteria(), progress.getAwardedCriteria().getClass());
			if (awardedCriteria.isJsonArray()) {
				returnData.add(advancement.getKey().getNamespace() + ":" + advancement.getKey().getKey(), awardedCriteria);
			}
		}
		return returnData;
	}

	public static void loadFromJsonObject(Player player, JsonObject object) {
		if (object != null) {
			for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
				JsonArray awardedCriteria = entry.getValue().getAsJsonArray();
				String[] keys = entry.getKey().split(":", 2);
				Advancement advancement = Bukkit.getAdvancement(new NamespacedKey(keys[0], keys[1]));
				if (advancement != null) {
					AdvancementProgress progress = player.getAdvancementProgress(advancement);
					for (String criteria : advancement.getCriteria()) {
						if (awardedCriteria.contains(new JsonPrimitive(criteria))) {
							progress.awardCriteria(criteria);
						} else {
							progress.revokeCriteria(criteria);
						}
					}
				}
			}
		}
	}
}
