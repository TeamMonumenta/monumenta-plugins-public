package pe.project.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import pe.project.Plugin;

public class SetServerVersionCommand implements CommandExecutor {
	private Plugin mPlugin;
	
	public SetServerVersionCommand(Plugin plugin) {
		mPlugin = plugin;
	}
	
    @Override
    public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {	
    	if (arg0 instanceof Player) {
	    	if (arg3.length < 1) {
	    		arg0.sendMessage(ChatColor.RED + "Please specify a version number!");
	    		arg0.sendMessage(ChatColor.RED + "Usage: " + arg1.getUsage());
	    		return true;
	    	} else if (arg3.length > 1) {
	    		arg0.sendMessage(ChatColor.RED + "Too many parameters!");
	    		arg0.sendMessage(ChatColor.RED + "Usage: " + arg1.getUsage());
	    		return true;
	    	}
	    	
	    	int version;
	    	
	    	try{
	    		version = Integer.parseInt(arg3[0]);
	    	} catch (NumberFormatException e) {
	    		arg0.sendMessage(ChatColor.RED + "Invalid parameter. Must be whole number value between 0 and " + Integer.MAX_VALUE);
	    		arg0.sendMessage(ChatColor.RED + "Usage: " + arg1.getUsage());
	    		return true;
	    	}
	    	
	    	if (version < 0 || version > Integer.MAX_VALUE) {
	    		arg0.sendMessage(ChatColor.RED + "Invalid parameter. Must be whole number value between 0 and " + Integer.MAX_VALUE);
	    		arg0.sendMessage(ChatColor.RED + "Usage: " + arg1.getUsage());
	    		return true;
	    	}
	    	
	    	mPlugin.updateVersion(version);
	        arg0.sendMessage(ChatColor.GREEN + "The server version has been updated to " + version);
    	}
    	
    	 return true;
    }
}
