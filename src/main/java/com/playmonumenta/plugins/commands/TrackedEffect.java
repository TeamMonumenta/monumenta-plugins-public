package com.playmonumenta.plugins.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.CommandUtils;

public class TrackedEffect implements CommandExecutor {
	Plugin mPlugin;

	public TrackedEffect(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		/* Parse arguments */
		if (arg3.length != 3) {
			sender.sendMessage(ChatColor.RED + "Expected 3 parameters: <effect> <seconds> <amplifier>");
			return false;
		}

		Player player = null;
		if (sender instanceof Player) {
			player = (Player)sender;
		} else if (sender instanceof ProxiedCommandSender) {
			CommandSender callee = ((ProxiedCommandSender)sender).getCallee();
			if (callee instanceof Player) {
				player = (Player)callee;
			}
		}

		if (player == null) {
			sender.sendMessage(ChatColor.RED + "This command must be run by/on a player!");
			return false;
		}

		PotionEffectType type = PotionEffectType.getByName(arg3[0]);
		if (type == null) {
			sender.sendMessage(ChatColor.RED + "Invalid PotionEffectType '" + arg3[0] + "'");
			sender.sendMessage(ChatColor.RED + "Valid values: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/potion/PotionEffectType.html");
			return false;
		}
		int seconds;
		int amplifier;
		try {
			seconds = CommandUtils.parseIntFromString(sender, arg3[1]);
			amplifier = CommandUtils.parseIntFromString(sender, arg3[2]);
		} catch (Exception e) {
			return false;
		}
		if (seconds <= 0) {
			sender.sendMessage(ChatColor.RED + "Seconds must be >= 0");
			return false;
		}
		if (amplifier < 0 || amplifier > 255) {
			sender.sendMessage(ChatColor.RED + "Amplifier must be between 0 and 255 (inclusive)");
			return false;
		}

		/* Apply potion via potion manager */
		mPlugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION,
		                                 new PotionEffect(type, seconds * 20, amplifier, true, true));
		mPlugin.mPotionManager.applyBestPotionEffect(player);

		sender.sendMessage("Applied " + type.toString() + ":" + Integer.toString(amplifier + 1) +
		                   " to player '" + player.getName() + "' for " + Integer.toString(seconds) + "s");

		return true;
	}
}
