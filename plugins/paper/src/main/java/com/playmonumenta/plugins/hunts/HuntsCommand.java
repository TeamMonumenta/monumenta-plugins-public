package com.playmonumenta.plugins.hunts;

import com.playmonumenta.networkrelay.NetworkRelayAPI;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.LongArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

public class HuntsCommand {
	private static final String COMMAND = "hunts";
	private static final String PERM = "monumenta.command.hunts";

	public static void register(Plugin plugin) {
		if (ServerProperties.getShardName().contains("build")) {
			return;
		}

		EntitySelectorArgument.OnePlayer playerArg = new EntitySelectorArgument.OnePlayer("player");
		List<String> quarries = new ArrayList<>();
		for (HuntsManager.QuarryType quarry : HuntsManager.QuarryType.values()) {
			quarries.add(quarry.name());
		}
		MultiLiteralArgument quarryArg = new MultiLiteralArgument("quarry", quarries.toArray(String[]::new));
		LongArgument timeArg = new LongArgument("seconds", 0);
		StringArgument shardArg = new StringArgument("shard");

		new CommandAPICommand(COMMAND)
			.withPermission(PERM)
			.withArguments(new LiteralArgument("track"))
			.withArguments(playerArg)
			.executes((sender, args) -> {
				plugin.mHuntsManager.track(args.getByArgument(playerArg));
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERM)
			.withArguments(new LiteralArgument("bait"))
			.withArguments(playerArg)
			.withArguments(quarryArg)
			.executes((sender, args) -> {
				plugin.mHuntsManager.bait(args.getByArgument(playerArg), HuntsManager.QuarryType.valueOf(args.getByArgument(quarryArg)));
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERM)
			.withArguments(new LiteralArgument("warn"))
			.executes((sender, args) -> {
				plugin.mHuntsManager.warn(new ArrayList<>(Bukkit.getOnlinePlayers()));
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERM)
			.withArguments(new LiteralArgument("refresh"))
			.executes((sender, args) -> {
				plugin.mHuntsManager.refresh();
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERM)
			.withArguments(new LiteralArgument("forcesummon"))
			.withArguments(quarryArg)
			.withArguments(shardArg)
			.executes((sender, args) -> {
				String shard = args.getByArgument(shardArg);
				if (!NetworkRelayAPI.getShardName().equals(shard)) {
					return;
				}
				plugin.mHuntsManager.forceSummon(HuntsManager.QuarryType.valueOf(args.getByArgument(quarryArg)));
			}).register();

		// This command should be used by mods if the hunts system has broken (probably because a hunt started on a shard that was down)
		new CommandAPICommand(COMMAND)
			.withPermission(PERM)
			.withArguments(new LiteralArgument("startnew"))
			.executes((sender, args) -> {
				plugin.mHuntsManager.startRandomHunt();
			}).register();

		// This command should be used by mods if the hunts system has broken (probably because a hunt started on a shard that was down)
		new CommandAPICommand(COMMAND)
			.withPermission(PERM)
			.withArguments(new LiteralArgument("time"))
			.executesPlayer((player, args) -> {
				player.sendMessage("The next hunt is in " + StringUtils.longToOptionalHoursMinuteAndSeconds(plugin.mHuntsManager.getRemainingTime()));
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERM)
			.withArguments(new LiteralArgument("settime"))
			.withArguments(timeArg)
			.executes((sender, args) -> {
				plugin.mHuntsManager.setTime(args.getByArgument(timeArg)).thenRun(plugin.mHuntsManager::refreshOthers);
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERM)
			.withArguments(new LiteralArgument("teleport"))
			.executes((sender, args) -> {
				if (CommandUtils.getCallee(sender) instanceof Player player) {
					Location loc = plugin.mHuntsManager.getNextLocation();
					if (loc != null) {
						player.teleport(loc, PlayerTeleportEvent.TeleportCause.COMMAND);
					}
				}
			}).register();
	}
}
