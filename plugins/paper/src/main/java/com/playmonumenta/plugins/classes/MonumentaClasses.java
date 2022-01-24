package com.playmonumenta.plugins.classes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import org.bukkit.entity.Player;
import javax.annotation.Nullable;

public class MonumentaClasses extends ClassList {
	public MonumentaClasses(Plugin plugin, @Nullable Player player) {
		mClasses.add(new Alchemist(plugin, player));
		mClasses.add(new Cleric(plugin, player));
		mClasses.add(new Mage(plugin, player));
		mClasses.add(new Rogue(plugin, player));
		mClasses.add(new Scout(plugin, player));
		mClasses.add(new Warlock(plugin, player));
		mClasses.add(new Warrior(plugin, player));
	}

	public JsonObject toJson() {
		JsonArray classes = new JsonArray();
		for (PlayerClass playerClass : mClasses) {
			classes.add(playerClass.toJson());
		}

		JsonObject obj = new JsonObject();
		obj.add("classes", classes);
		return obj;
	}
}
