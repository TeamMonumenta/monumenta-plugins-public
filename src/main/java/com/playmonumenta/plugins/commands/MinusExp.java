package com.playmonumenta.plugins.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;

import org.bukkit.ChatColor;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.CommandUtils;


public class MinusExp implements CommandExecutor {
	Plugin mPlugin;

	public MinusExp(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length != 1) {
			sender.sendMessage(ChatColor.RED + "Invalid number of parameters!");
			return false;
		}

		/* Figure out which player is the target */
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
			sender.sendMessage(ChatColor.RED + "Target is not a player!");
			return false;
		}

		/* Parse how much experience should be removed from that player */
		int levels = 0;
		try {
			levels = CommandUtils.parseIntFromString(player, arg3[0]);
		} catch (Exception e) {
			return false;
		}

		_setTotalExp(player, _getTotalExp(player) - _levelToExp(levels));

		return true;
	}

	private float _levelToExp(int level) {
		float levelF = (float)level;

		if (level <= 0) {
			return 0.0F;
		} else if (level <= 16) {
			return levelF * levelF + 6.0F * levelF;
		} else if (level <= 31) {
			return 2.5F * levelF * levelF - 40.5F * levelF + 360.0F;
		} else {
			return 4.5F * levelF * levelF - 162.5F * levelF + 2220.0F;
		}
	}

	private float _getTotalExp(Player player) {
		int level = player.getLevel();
		float exp = _levelToExp(level);

		// Get the component from current progression to the next level
		exp += (_levelToExp(level + 1) - _levelToExp(level)) * player.getExp();

		return exp;
	}

	private void _setTotalExp(Player player, float exp) {
		if (exp < 0) {
			exp = 0.0F;
		}

		int level = 0;
		while (_levelToExp(level + 1) < exp) {
			level++;
		}

		player.setLevel(level);
		player.setExp((exp - _levelToExp(level)) / (_levelToExp(level + 1) - _levelToExp(level)));
	}
}
