package com.playmonumenta.plugins.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.tracking.PlayerTracking;

public class DebugInfo extends GenericCommand {
	public static void register(Plugin plugin) {
		registerPlayerCommand("debuginfo", "monumenta.command.debuginfo",
		                      (sender, player) -> {
		                          run(plugin, sender, player);
		                      });
	}

	private static void run(Plugin plugin, CommandSender sender, Player player) {
		JsonObject debugInfo = new JsonObject();

		if (plugin.mPotionManager != null) {
			debugInfo.add("Potion Manager", plugin.mPotionManager.getAsJsonObject(player, true));
		}
		if (plugin.mAbilityManager != null) {
			debugInfo.add("Ability Manager", plugin.mAbilityManager.getAsJson(player));
		}
		if (plugin.mEffectManager != null) {
			debugInfo.add("Effect Manager", plugin.mEffectManager.getAsJsonObject(player));
		}
		if (PlayerTracking.getInstance() != null) {
			debugInfo.add("Enchants", PlayerTracking.getInstance().getAsJsonObject(player));
		}

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		sender.sendMessage(gson.toJson(debugInfo));
	}
}
