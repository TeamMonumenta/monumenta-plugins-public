package pe.project.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import pe.project.Plugin;

public class GetServerVersionCommand implements CommandExecutor {
	private Plugin mPlugin;
	
	public GetServerVersionCommand(Plugin plugin) {
		mPlugin = plugin;
	}
	
    @Override
    public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {	
    	arg0.sendMessage(ChatColor.GREEN + "Version: " + mPlugin.mServerVersion);
    	
    	return true;
    }
}
