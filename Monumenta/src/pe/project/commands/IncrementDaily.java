package pe.project.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import pe.project.Plugin;
import pe.project.server.reset.DailyReset;

public class IncrementDaily implements CommandExecutor {
	Plugin mPlugin;

	public IncrementDaily(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		//	Increment the servers Daily version.
		mPlugin.incrementDailyVersion();

		//	Loop through all online players, reset their scoreboards and message them about the Daily reset.
		for (Player player : mPlugin.getServer().getOnlinePlayers()) {
			DailyReset.handle(mPlugin, player);
		}

		return true;
	}
}
