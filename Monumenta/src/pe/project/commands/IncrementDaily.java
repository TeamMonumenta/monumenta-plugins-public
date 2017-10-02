package pe.project.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import pe.project.Main;
import pe.project.server.reset.DailyReset;

public class IncrementDaily implements CommandExecutor {
	Main mMain;

	public IncrementDaily(Main main) {
		mMain = main;
	}

	@Override
	public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		//	Increment the servers Daily version.
		mMain.incrementDailyVersion();

		//	Loop through all online players, reset their scoreboards and message them about the Daily reset.
		for (Player player : mMain.getServer().getOnlinePlayers()) {
			DailyReset.handle(mMain, player);
		}

		return true;
	}
}
