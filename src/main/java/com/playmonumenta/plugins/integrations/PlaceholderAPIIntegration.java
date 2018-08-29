package com.playmonumenta.plugins.integrations;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlaceholderAPIIntegration extends PlaceholderExpansion {
	Plugin mPlugin;

	public PlaceholderAPIIntegration(Plugin plugin) {
		super();
		mPlugin = plugin;
	}

	@Override
	public String getIdentifier() {
		return "monumenta";
	}

	@Override
	public String getPlugin() {
		return null;
	}

	@Override
	public String getAuthor() {
		return "Team Epic";
	}

	@Override
	public String getVersion() {
		return "1.0.0";
	}

	@Override
	public String onPlaceholderRequest(Player player, String identifier) {
		if (player == null) {
			return "";
		}

		// %monumenta_class%
		if (identifier.equalsIgnoreCase("class")) {
			// TODO: This really should use the standard thing in Plugin.java... but it's
			// currently a pile of crap and this is actually less awful
			switch (ScoreboardUtils.getScoreboardValue(player, "Class")) {
			case 0:
				return "No class";
			case 1:
				return "Mage";
			case 2:
				return "Warrior";
			case 3:
				return "Cleric";
			case 4:
				return "Rogue";
			case 5:
				return "Alchemist";
			case 6:
				return "Scout";
			case 7:
				return "Warlock";
			default:
				return "Unknown class";

			}
		}

		// %monumenta_level%
		if (identifier.equalsIgnoreCase("level")) {
			return Integer.toString(ScoreboardUtils.getScoreboardValue(player, "TotalLevel"));
		}

		return null;
	}
}
