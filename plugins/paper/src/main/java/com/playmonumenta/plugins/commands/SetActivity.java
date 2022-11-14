package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import org.bukkit.entity.Player;

public class SetActivity {
	private static final String COMMAND = "setactivity";
	private static final String PERM = "monumenta.commands.setactivity";

	public static void register(Plugin plugin) {
		new CommandAPICommand(COMMAND)
			.withPermission(PERM)
			.withArguments(new PlayerArgument("player"), new IntegerArgument("int"))
			.executes((sender, args) -> {
				plugin.mActivityManager.mActivity.put(((Player) args[0]).getUniqueId(), (int) args[1]);
			}).register();
	}
}
