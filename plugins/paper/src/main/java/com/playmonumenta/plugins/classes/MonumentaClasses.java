package com.playmonumenta.plugins.classes;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;

public class MonumentaClasses extends ClassList {
	MonumentaClasses(Plugin plugin, Player player) {
		mClasses.add(new Alchemist(plugin, player));
		mClasses.add(new Cleric(plugin, player));
		mClasses.add(new Mage(plugin, player));
		mClasses.add(new Rogue(plugin, player));
		mClasses.add(new Scout(plugin, player));
		mClasses.add(new Warlock(plugin, player));
		mClasses.add(new Warrior(plugin, player));
	}
}
