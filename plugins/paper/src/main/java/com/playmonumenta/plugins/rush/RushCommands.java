package com.playmonumenta.plugins.rush;

import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class RushCommands {
	private static final String COMMAND = "rush";
	private static final CommandPermission PERMISSION = CommandPermission.fromString("monumenta.r2.rush");
	private static final Component RUSH_OCCUPIED = Component.text("Round is in progress!", NamedTextColor.RED);


	public static void register() {
		if (isRushShard()) {
			registerPlayerCommands();
		}

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withSubcommand(new CommandAPICommand("init")
				.withArguments(new EntitySelectorArgument.ManyPlayers("players"))
				.executes((sender, args) -> {
					List<Player> players = args.getUnchecked("players");
					if (!isRushShard() || players == null) {
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
					if (player != null && isRushShard()) {
						PersistentDataContainer spawnData = RushArenaUtils.getStandOrThrow(player, RushArenaUtils.RUSH_SPAWN_TAG).getPersistentDataContainer();
						int round = spawnData.getOrDefault(RushManager.RUSH_WAVE_KEY, PersistentDataType.INTEGER, 1);
						boolean isMultiplayer = spawnData.getOrDefault(RushManager.RUSH_IS_MULTIPLAYER, PersistentDataType.BOOLEAN, true);
						RushManager.updatePlayerStats(player, round, isMultiplayer);
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
					new IntegerArgument("round")
				).executes((sender, args) -> {
					Player player = args.getUnchecked("player");
					int round = args.getOrDefaultUnchecked("round", 0);
					if (player == null) {
						return -1;
					}
					RushReward.generateLoot(player.getLocation(), round);
					return 1;
				}))
			.withSubcommand(new CommandAPICommand("setwave")
				.withArguments(
					new IntegerArgument("round")
				).executes((sender, args) -> {
					int round = args.getOrDefaultUnchecked("round", 1);
					if (sender instanceof Player player && isRushShard()) {
						RushArenaUtils.getStandOrThrow(player, RushArenaUtils.RUSH_SPAWN_TAG)
							.getPersistentDataContainer()
							.set(RushManager.RUSH_WAVE_KEY, PersistentDataType.INTEGER, round);
						return 1;
					}
					return -1;
				}))
			.withSubcommand(new CommandAPICommand("refund")
				.withArguments(
					new EntitySelectorArgument.OnePlayer("player")
				).executes((sender, args) -> {
					Player player = args.getUnchecked("player");
					if (player == null) {
						return -1;
					}
					// Round of 0 cannot exist
					int round = ScoreboardUtils.getScoreboardValue(player, RushManager.RUSH_FINISHED_SCOREBOARD).orElse(0);

					boolean isRefundable = round > 0 &&
						ScoreboardUtils.getScoreboardValue(player, "DRDAccess").orElse(0) == 0;

					if (isRefundable) {
						ScoreboardUtils.setScoreboardValue(player, RushManager.RUSH_FINISHED_SCOREBOARD, 0);
						// genLoot accounts for completed wave only, so add 1
						RushReward.generateLoot(player.getLocation(), round + 1);
						return 1;
					}

					return -1;
				})).register();
	}

	// Should be enabled in Rush Shard only
	private static void registerPlayerCommands() {
		new CommandAPICommand("rushpause")
			.executes((sender, args) -> {
				if (sender instanceof Player pl) {
					RushArena arena = RushManager.mPlayerArenaMap.get(pl);
					if (arena == null || !arena.mWorld.getUID().equals(pl.getWorld().getUID()) || !arena.mRequestWindow) {
						pl.sendMessage(RushManager.BREAK_PASS);
						return -1;
					}

					arena.mPlayers.forEach(pEach -> pEach.sendMessage(MessagingUtils.fromMiniMessage(String.format("<gray><yellow>%s</yellow> wants to take a break!", pl.getName()))));
					arena.mRequestWindow = false;

					return 1;
				}
				return -1;
			}).register();
	}

	private static boolean isRushShard() {
		String name = ServerProperties.getShardName();
		return name.equals("rush") || name.contains("dev");
	}
}
