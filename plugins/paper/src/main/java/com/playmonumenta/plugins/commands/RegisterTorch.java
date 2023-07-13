package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.SpawnerUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.LocationArgument;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;

public class RegisterTorch {
	private static final String COMMAND = "registertorch";
	private static final String PERM = "monumenta.command.registertorch";

	public static void register() {
		new CommandAPICommand(COMMAND)
			.withPermission(PERM)
			.withArguments(new LocationArgument("location"))
			.executes((sender, args) -> {
				Location loc = (Location) args[0];
				Block block = loc.getBlock();
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
					if (BlockUtils.isTorch(block)) {
						SpawnerUtils.addTorch(block);
					}
				}, 1);
			}).register();
	}
}
