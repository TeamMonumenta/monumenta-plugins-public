package com.playmonumenta.plugins.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.FileUtils;
import java.io.File;
import java.io.IOException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DebugInfo extends GenericCommand {
	public static void register(Plugin plugin) {
		File debugDumpPath = new File(plugin.getDataFolder(), "debug_dumps");

		registerPlayerCommand("debuginfo", "monumenta.command.debuginfo",
			(sender, player) -> run(plugin, debugDumpPath, sender, player));
	}

	private static void run(Plugin plugin, File debugDumpPath, CommandSender sender, Player player) {
		JsonObject potionManagerJson;
		if (plugin.mPotionManager != null) {
			potionManagerJson = plugin.mPotionManager.getAsJsonObject(player, true);
		} else {
			potionManagerJson = null;
		}

		JsonElement abilityManagerJson;
		if (plugin.mAbilityManager != null) {
			abilityManagerJson = plugin.mAbilityManager.getAsJson(player);
		} else {
			abilityManagerJson = null;
		}

		JsonObject effectManagerJson;
		if (plugin.mEffectManager != null) {
			effectManagerJson = plugin.mEffectManager.getAsJsonObject(player);
		} else {
			effectManagerJson = null;
		}

		JsonObject debugInfo = new JsonObject();

		if (plugin.mPotionManager != null) {
			debugInfo.add("Potion Manager", potionManagerJson);
		}
		if (plugin.mAbilityManager != null) {
			debugInfo.add("Ability Manager", abilityManagerJson);
		}
		if (plugin.mEffectManager != null) {
			debugInfo.add("Effect Manager", effectManagerJson);
		}

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		sender.sendMessage(gson.toJson(debugInfo));

		File currentDebugDumpPath = new File(debugDumpPath, DateUtils.trueUtcDateTime() + "_" + player.getName());

		try {
			JsonObject safetyWrapper = new JsonObject();
			safetyWrapper.add("data", potionManagerJson);

			FileUtils.writeJson(
				new File(currentDebugDumpPath, "Potion Manager.json")
					.getAbsolutePath(),
				safetyWrapper);
		} catch (IOException ex) {
			sender.sendMessage(Component.text(
				"Failed to dump potion manager info to json: " + ex.getMessage(),
				NamedTextColor.RED));
		}

		try {
			JsonObject safetyWrapper = new JsonObject();
			safetyWrapper.add("data", abilityManagerJson);

			FileUtils.writeJson(
				new File(currentDebugDumpPath, "Ability Manager.json")
					.getAbsolutePath(),
				safetyWrapper);
		} catch (IOException ex) {
			sender.sendMessage(Component.text(
				"Failed to dump ability manager info to json: " + ex.getMessage(),
				NamedTextColor.RED));
		}

		try {
			JsonObject safetyWrapper = new JsonObject();
			safetyWrapper.add("data", effectManagerJson);

			FileUtils.writeJson(
				new File(currentDebugDumpPath, "Effect Manager.json")
					.getAbsolutePath(),
				safetyWrapper);
		} catch (IOException ex) {
			sender.sendMessage(Component.text(
				"Failed to dump effect manager info to json: " + ex.getMessage(),
				NamedTextColor.RED));
		}

		try {
			JsonObject safetyWrapper = new JsonObject();

			if (plugin.mAbilityManager != null) {
				safetyWrapper.add("data", plugin.mAbilityManager.getDebugState(player));
			} else {
				safetyWrapper.add("data", null);
			}

			FileUtils.writeJson(
				new File(currentDebugDumpPath, "Ability Debug State.json")
					.getAbsolutePath(),
				safetyWrapper);
		} catch (IOException ex) {
			sender.sendMessage(Component.text(
				"Failed to dump ability debug state to json: " + ex.getMessage(),
				NamedTextColor.RED));
		}
	}
}
