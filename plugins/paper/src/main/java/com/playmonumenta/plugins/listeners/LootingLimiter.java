package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ChestUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.Comparator;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

public class LootingLimiter implements Listener {

	public static final String SPAWNERS_SCORE = "LootingLimiterSpawners";
	public static final String KILLS_SCORE = "LootingLimiterKills";

	private static final int MAX_BANKED_CHESTS = 4;

	private static final int PLAYER_SEARCH_RADIUS = 20;
	private static final int MOB_AND_SPAWNER_SEARCH_RADIUS = 12;

	// spawner break checks

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void blockBreakEventMonitor(BlockBreakEvent event) {
		Player player = event.getPlayer();
		if (ServerProperties.getLootingLimiterSpawners() <= 0) {
			return;
		}
		if (event.getBlock().getType() == Material.SPAWNER) {
			spawnerBroken(player);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityExplodeEventMonitor(EntityExplodeEvent event) {
		if (ServerProperties.getLootingLimiterSpawners() <= 0) {
			return;
		}
		for (Block b : event.blockList()) {
			if (b.getType() == Material.SPAWNER) {
				Player player = EntityUtils.getNearestPlayer(b.getLocation(), PLAYER_SEARCH_RADIUS);
				if (player != null) {
					spawnerBroken(player);
				}
			}
		}
	}

	private void spawnerBroken(Player player) {
		if (player.getGameMode() == GameMode.CREATIVE) {
			return;
		}
		ScoreboardUtils.setScoreboardValue(player, SPAWNERS_SCORE,
			Math.min(ScoreboardUtils.getScoreboardValue(player, SPAWNERS_SCORE).orElse(0) + 1, MAX_BANKED_CHESTS * ServerProperties.getLootingLimiterSpawners()));
	}

	// mob kills

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityDeathEvent(EntityDeathEvent event) {
		if (ServerProperties.getLootingLimiterMobKills() <= 0) {
			return;
		}
		LivingEntity entity = event.getEntity();
		if (!EntityUtils.isHostileMob(entity)) {
			return;
		}
		Player player = entity.getKiller();
		if (player == null || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
			return;
		}
		int score = EntityUtils.isBoss(entity) ? 5 : EntityUtils.isElite(entity) ? 3 : 1;
		ScoreboardUtils.setScoreboardValue(player, KILLS_SCORE,
			Math.min(ScoreboardUtils.getScoreboardValue(player, KILLS_SCORE).orElse(0) + score, MAX_BANKED_CHESTS * ServerProperties.getLootingLimiterMobKills()));
	}

	// chest break checks

	// explode events are handled by ChestOverride, as that code is called before this one would be

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockBreakEventEarly(BlockBreakEvent event) {
		if (ServerProperties.getLootingLimiterSpawners() <= 0 && ServerProperties.getLootingLimiterMobKills() <= 0) {
			return;
		}
		if (!checkChest(event.getBlock(), event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void playerInteractEvent(PlayerInteractEvent event) {
		if (event.useInteractedBlock() == Event.Result.DENY
			    || event.getAction() != Action.RIGHT_CLICK_BLOCK
			    || (ServerProperties.getLootingLimiterSpawners() <= 0 && ServerProperties.getLootingLimiterMobKills() <= 0)) {
			return;
		}
		Block block = event.getClickedBlock();
		if (block != null && !checkChest(block, event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	public static boolean checkChest(Block block, @Nullable Player player) {
		if (player != null && player.getGameMode() == GameMode.CREATIVE) {
			return true;
		}
		if (ChestUtils.isChestWithLootTable(block)) {
			List<Player> players = PlayerUtils.playersInRange(block.getLocation(), PLAYER_SEARCH_RADIUS, true, true);
			if (players.isEmpty()) {
				return false;
			}
			players.sort(Comparator.comparing(p -> p.getLocation().distanceSquared(block.getLocation())));
			if (player != null) { // make sure the breaking/opening player is first in the list, even if another player is closer
				players.remove(player);
				players.add(0, player);
			}
			boolean hasMobs = !EntityUtils.getNearbyMobs(block.getLocation(), MOB_AND_SPAWNER_SEARCH_RADIUS).isEmpty();
			boolean hasSpawners = EntityUtils.hasTileEntityInRange(block.getLocation(), MOB_AND_SPAWNER_SEARCH_RADIUS, b -> b.getType() == Material.SPAWNER);
			if (hasMobs && players.stream().mapToInt(p -> ScoreboardUtils.getScoreboardValue(p, KILLS_SCORE).orElse(0)).sum() < ServerProperties.getLootingLimiterMobKills()) {
				if (player != null) {
					player.sendActionBar(Component.text("The enemies protecting the chest won't let you open it.", NamedTextColor.RED));
				}
				return false;
			}
			if (hasSpawners && players.stream().mapToInt(p -> ScoreboardUtils.getScoreboardValue(p, SPAWNERS_SCORE).orElse(0)).sum() < ServerProperties.getLootingLimiterSpawners()) {
				if (player != null) {
					player.sendActionBar(Component.text("Nearby spawners should be broken before searching this chest.", NamedTextColor.RED));
				}
				return false;
			}
			if (hasMobs) {
				int remaining = ServerProperties.getLootingLimiterMobKills();
				for (Player p : players) {
					int playerScore = ScoreboardUtils.getScoreboardValue(p, KILLS_SCORE).orElse(0);
					if (playerScore >= remaining) {
						ScoreboardUtils.setScoreboardValue(p, KILLS_SCORE, playerScore - remaining);
						break;
					} else if (playerScore > 0) {
						remaining -= playerScore;
						ScoreboardUtils.setScoreboardValue(p, KILLS_SCORE, 0);
					}
				}
			}
			if (hasSpawners) {
				int remaining = ServerProperties.getLootingLimiterSpawners();
				for (Player p : players) {
					int playerScore = ScoreboardUtils.getScoreboardValue(p, SPAWNERS_SCORE).orElse(0);
					if (playerScore >= remaining) {
						ScoreboardUtils.setScoreboardValue(p, SPAWNERS_SCORE, playerScore - remaining);
						break;
					} else if (playerScore > 0) {
						remaining -= playerScore;
						ScoreboardUtils.setScoreboardValue(p, SPAWNERS_SCORE, 0);
					}
				}
			}
		}
		return true;
	}

}
