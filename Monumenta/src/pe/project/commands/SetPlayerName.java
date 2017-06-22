package pe.project.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

public class SetPlayerName implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		
		Player player = Bukkit.getPlayer(arg3[0]);
		if (player != null) {
			ChatColor color = ChatColor.valueOf(arg3[1]);
			if (color != null) {
				String name = player.getName();
				player.setDisplayName(color + name + ChatColor.RESET);
				return true;
			}
		}
		
		return false;
	}

}
