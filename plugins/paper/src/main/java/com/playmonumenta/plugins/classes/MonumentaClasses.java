package com.playmonumenta.plugins.classes;

import org.bukkit.entity.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;

public class MonumentaClasses extends ClassList {
	public MonumentaClasses(Plugin plugin, Player player) {
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
