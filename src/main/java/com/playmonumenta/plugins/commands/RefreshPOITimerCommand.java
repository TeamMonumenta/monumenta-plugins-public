package com.playmonumenta.plugins.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.playmonumenta.plugins.Plugin;

public class RefreshPOITimerCommand implements CommandExecutor {
	Plugin mPlugin;
	
	public RefreshPOITimerCommand(Plugin plugin) {
		mPlugin = plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		if (arg3.length < 1 || arg3.length > 2) {
    		return false;
    	}
		
		int value;
    	
    	try{
    		value = Integer.parseInt(arg3[1]);
    	} catch (NumberFormatException e) {
    		arg0.sendMessage(ChatColor.RED + "Invalid parameter. Must be whole number value between 0 and " + Integer.MAX_VALUE);
    		arg0.sendMessage(ChatColor.RED + "Usage: " + arg1.getUsage());
    		return false;
    	}
		
		mPlugin.mPOIManager.refreshPOI(arg3[0], value);
		
		return true;
	}
}
