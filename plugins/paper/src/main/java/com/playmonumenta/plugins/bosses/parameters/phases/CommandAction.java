package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.NmsUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

public class CommandAction implements Action {
	public static final String IDENTIFIER = "COMMAND";

	private final String mCommand;

	public CommandAction(String command) {
		mCommand = command;
	}

	@Override
	public void runAction(LivingEntity boss) {
		// There are several times when even though the command is run in the context of a mob, that mob is no longer present in the world, causing "execute as <mob> to fail"
		final String cmdStr;
		if (boss.isDead()) {
			// If a mob has died, can't execute a command as them. Instead, run this as the server but positioned where the mob was located
			cmdStr = "execute in " + (boss.getWorld() == Bukkit.getWorlds().get(0) ? "minecraft:overworld" : boss.getWorld().getName()) +
				" positioned " + boss.getX() + " " + boss.getY() + " " + boss.getZ() +
				" run " + mCommand;
			Plugin.getInstance().getLogger().finer("Running command for dead boss as server '" + boss.getName() + "': " + cmdStr);
			NmsUtils.getVersionAdapter().runConsoleCommandSilently(cmdStr);
		} else if (boss.getWorld().getEntity(boss.getUniqueId()) == null) {
			// If a mob isn't dead but hasn't been added to the world yet, run the command as soon as possible, which should be after world addition is completed
			cmdStr = "execute as " + boss.getUniqueId() + " at @s run " + mCommand;
			Plugin.getInstance().getLogger().finer("Running command on next tick for mob '" + boss.getName() + "' being added to world: " + cmdStr);
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
				NmsUtils.getVersionAdapter().runConsoleCommandSilently(cmdStr);
			});
		} else {
			cmdStr = "execute as " + boss.getUniqueId() + " at @s run " + mCommand;
			Plugin.getInstance().getLogger().finer("Running command as boss '" + boss.getName() + "': " + cmdStr);
			NmsUtils.getVersionAdapter().runConsoleCommandSilently(cmdStr);
		}
	}

}
