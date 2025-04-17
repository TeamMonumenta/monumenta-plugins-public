package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.ParseResult;
import com.playmonumenta.plugins.bosses.parameters.StringReader;
import com.playmonumenta.plugins.utils.NmsUtils;
import dev.jorel.commandapi.Tooltip;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.StringUtil;

public class CommandAction implements Action {

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

	public static ParseResult<Action> fromReader(StringReader reader) {
		if (!reader.advance("(")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "(", "(...)")));
		}

		String soFar = reader.readSoFar();
		String commandFull = reader.readUntil(")");
		if (commandFull == null) {
			commandFull = reader.remaining();
		}

		String[] cmd = commandFull.split(" ", Integer.MAX_VALUE);
		List<Tooltip<String>> tooltips = new ArrayList<>();
		if (cmd.length == 1) {
			String prefix = cmd[0];
			ArrayList<String> completions = new ArrayList<>();
			for (String name : Bukkit.getCommandMap().getKnownCommands().keySet()) {
				if (StringUtil.startsWithIgnoreCase(name, prefix)) {
					completions.add(name);
				}
			}
			if (Bukkit.getCommandMap().getCommand(cmd[0]) != null) {
				tooltips.add(Tooltip.ofString(soFar + commandFull + ")", "end command"));
			}
			completions.stream()
				.sorted(String.CASE_INSENSITIVE_ORDER)
				.map(name -> Tooltip.ofString(soFar + name, "command"))
				.forEach(tooltips::add);
		} else {
			Command commandC = Bukkit.getCommandMap().getCommand(cmd[0]);
			if (commandC == null) {
				return ParseResult.of(Tooltip.arrayOf());
			}
			tooltips.add(Tooltip.ofString(soFar + commandFull + ")", "end command"));

			List<String> tabComplete = commandC.tabComplete(Bukkit.getConsoleSender(), cmd[0], Arrays.copyOfRange(cmd, 1, cmd.length), null);
			// This shouldn't be null - but somehow it is sometimes based on observed exceptions on the play server
			if (tabComplete != null) {
				String lastArg = cmd[cmd.length - 1];
				String suggestionsStart = tabComplete.stream().allMatch(tc -> StringUtils.startsWithIgnoreCase(tc, lastArg))
					                          ? soFar + commandFull.substring(0, commandFull.lastIndexOf(' ')) + ' ' : soFar + commandFull;
				tabComplete.stream()
					.map(s -> Tooltip.ofString(suggestionsStart + s, "command"))
					.forEach(tooltips::add);
			}
		}

		if (!reader.advance(")")) {
			return ParseResult.of(tooltips.toArray(Tooltip.arrayOf()));
		}
		return ParseResult.of(new CommandAction(commandFull));

	}

}
