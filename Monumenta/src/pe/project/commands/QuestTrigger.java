package pe.project.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import pe.project.Plugin;

public class QuestTrigger implements CommandExecutor {
	private Plugin mPlugin;
	
	public QuestTrigger(Plugin plugin) {
		mPlugin = plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		//	Because this is something that should ONLY be triggered via the scripted system, I'm going to be lazy
		//	and ignore error checking....I know...such a badass.
		Player player = mPlugin.getServer().getPlayer(arg3[0]);
		
		mPlugin.mNpcManager.triggerEvent(player, arg3[1], arg3[2], arg3[3]);

		return true;
	}
}
