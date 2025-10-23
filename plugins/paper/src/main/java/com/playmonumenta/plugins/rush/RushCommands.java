package com.playmonumenta.plugins.rush;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class RushCommands {
	private static final String COMMAND = "rush";
	private static final CommandPermission PERMISSION = CommandPermission.fromString("monumenta.r2.rush");
	private static final Component RUSH_OCCUPIED = Component.text("Round is in progress!", NamedTextColor.RED);


	public static void register() {
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withSubcommand(new CommandAPICommand("init")
				.withArguments(new EntitySelectorArgument.ManyPlayers("players"))
				.executes((sender, args) -> {
					List<Player> players = args.getUnchecked("players");
					if (players == null) {
						return -1;
					}
					players.removeIf(p -> p.getGameMode() == GameMode.SPECTATOR);
					if (players.isEmpty()) {
						return -1;
					}
					if (RushManager.checkRushOccupation(players)) {
						players.forEach(p -> p.sendMessage(RUSH_OCCUPIED));
						return -1;
					}
					new RushArena(players);
					return 1;
				}))
			.withSubcommand(new CommandAPICommand("eject")
				.withArguments(new EntitySelectorArgument.OnePlayer("player"))
				.executes((sender, args) -> {
					Player player = args.getUnchecked("player");
					if (player != null) {
						PersistentDataContainer spawnData = RushArenaUtils.getStandOrThrow(player, RushArenaUtils.RUSH_SPAWN_TAG).getPersistentDataContainer();
						int round = spawnData.getOrDefault(RushManager.RUSH_WAVE_KEY, PersistentDataType.INTEGER, 1);
						int count = spawnData.getOrDefault(RushManager.RUSH_PLAYER_COUNT_KEY, PersistentDataType.INTEGER, 1);
						RushManager.updatePlayerStats(player, round, count);
						Location lootLoc = RushManager.teleportPlayerToLootroom(player);
						if (lootLoc == null) {
							return -1;
						}
						RushReward.generateLoot(lootLoc, round);
						return 1;
					}
					return -1;
				}))
			.withSubcommand(new CommandAPICommand("genloot")
				.withArguments(
					new EntitySelectorArgument.OnePlayer("player"),
					new IntegerArgument("wave")
				).executes((sender, args) -> {
					Player player = args.getUnchecked("player");
					int wave = args.getOrDefaultUnchecked("wave", 0);
					if (player == null || wave <= 1) {
						return -1;
					}
					RushReward.generateLoot(player.getLocation(), wave);
					return 1;
				}))
			.withSubcommand(new CommandAPICommand("setwave")
				.withArguments(
					new IntegerArgument("wave")
				).executes((sender, args) -> {
					int wave = args.getOrDefaultUnchecked("wave", 1);
					if (sender instanceof Player player) {
						RushArenaUtils.getStandOrThrow(player, RushArenaUtils.RUSH_SPAWN_TAG)
							.getPersistentDataContainer()
							.set(RushManager.RUSH_WAVE_KEY, PersistentDataType.INTEGER, wave);
						return 1;
					}
					return -1;
				})
			).register();
	}
}
