package com.playmonumenta.plugins.rush;

import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.WeakHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class RushManager implements Listener {
	private static final double[] MOB_COUNT_MULTIPLIER_PER_PLAYER = {1, 1.33, 1.67, 2};

	protected static final int WAVE_PER_ROUND = 4;
	protected static final int BOSS_ROUND = 20;
	protected static final int BOSS_INCREMENT = 5;
	protected static final int SCALING_ROUND = 30;

	private static final Plugin mPlugin = Plugin.getInstance();
	private static final int MAX_SEARCH_RADIUS = 300;
	private static final int MAX_COMMON_MOBS = 30;
	private static final int MAX_ELITE_MOBS = 8;

	private static final String RUSH_VULN = "RushVulnerability";
	private static final String RUSH_HIGHEST_ROUND_SOLO = "RushHighestRoundSolo";
	private static final String RUSH_HIGHEST_ROUND_MULTIPLAYER = "RushHighestRound";

	private static final Component RUSH_ERROR_NO_LOOT_ROOM = Component.text("Unable to find a loot room! Talk to the Dissonant Converter to claim you loot!", NamedTextColor.RED);
	protected static final Component BREAK_ASK = Component.text("Want to take a break?", NamedTextColor.GRAY);
	protected static final Component BREAK_PASS = Component.text("Request break window has passed!", NamedTextColor.GRAY);

	private static final Vector RUSH_LOOT_ROOM_OFFSET = new Vector(-20, 3, 0);

	protected static final double LAST_WAVE_INCREASE = 1.3;

	protected static final String RUSH_LOOT_ROOM_TAG = "RushDownLootRoom";

	protected static final NamespacedKey RUSH_WAVE_KEY = NamespacedKeyUtils.fromString("monumenta:rush-wave");
	protected static final NamespacedKey RUSH_PLAYER_COUNT_KEY = NamespacedKeyUtils.fromString("monumenta:rush-player-count");
	protected static final NamespacedKey RUSH_IS_MULTIPLAYER = NamespacedKeyUtils.fromString("monumenta:rush-is-multiplayer");
	protected static final NamespacedKey RUSH_PLAYER_COMBAT_LOG = NamespacedKeyUtils.fromString("monumenta:rush-combat-log");
	protected static final String RUSH_FINISHED_SCOREBOARD = "DRDFinished";

	protected static final Map<Player, RushArena> mPlayerArenaMap = new WeakHashMap<>();

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerDeathEvent(PlayerDeathEvent event) {
		attemptEjection(event.getPlayer(), true);
	}

	/*
	RUSH_PLAYER_COMBAT_LOG tracks whether a player combat logs.
	DRDFinished tracks any player leaving the instance, if a player
	has a value of any sorts and have DRDAccess = 0, they can be refunded.
	 */

	@EventHandler(ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		attemptEjection(event.getPlayer(), false);
	}


	@EventHandler(ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		PersistentDataContainer data = player.getPersistentDataContainer();
		int round = data.getOrDefault(RUSH_PLAYER_COMBAT_LOG, PersistentDataType.INTEGER, -1);
		if (round != -1) {
			data.remove(RUSH_PLAYER_COMBAT_LOG);
			displayDeathAndGenerateLoot(player, player.getLocation().add(RUSH_LOOT_ROOM_OFFSET), round);
		}
	}

	// Handle should the loot be generated or not yet

	public static void attemptEjection(Player player, boolean death) {
		RushArena arena = mPlayerArenaMap.remove(player);
		restorePlayer(player);
		saveRoundToPlayer(player);
		if (arena != null && arena.mPlayers.remove(player)) {
			int round = arena.mRound;
			arena.mPlayerCount = RushArenaUtils.updatePlayerCount(arena.mSpawnStand);
			Boolean isMultiplayer = arena.mSpawnStand
				.getPersistentDataContainer()
				.getOrDefault(RUSH_IS_MULTIPLAYER, PersistentDataType.BOOLEAN, true);

			Location lootLoc = teleportPlayerToLootroom(player);
			updatePlayerStats(player, round, isMultiplayer);
			if (lootLoc != null) {
				lootLoc.add(RUSH_LOOT_ROOM_OFFSET);
				if (death) {
					displayDeathAndGenerateLoot(player, lootLoc, round);
				} else {
					player.getPersistentDataContainer().set(RUSH_PLAYER_COMBAT_LOG, PersistentDataType.INTEGER, round);
				}
			}
		}
	}

	private static void saveRoundToPlayer(Player player) {
		if (!ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.LOOTROOM)) {

			Integer round = RushArenaUtils.getStandOrThrow(player, RushArenaUtils.RUSH_SPAWN_TAG)
				.getPersistentDataContainer().getOrDefault(RUSH_WAVE_KEY, PersistentDataType.INTEGER, 1);
			ScoreboardUtils.setScoreboardValue(player, RUSH_FINISHED_SCOREBOARD, round - 1);
		}
	}

	private static void displayDeathAndGenerateLoot(Player player, Location lootLoc, int round) {
		MessagingUtils.sendTitle(player, Component.text("You Died", NamedTextColor.RED), Component.text("You have succumbed on round " + round, NamedTextColor.RED), Title.Times.times(Duration.ofSeconds(0), Duration.ofSeconds(2), Duration.ofSeconds(1)));
		ScoreboardUtils.setScoreboardValue(player, RUSH_FINISHED_SCOREBOARD, 0);
		player.playSound(player, Sound.ENTITY_WITHER_SPAWN, SoundCategory.AMBIENT, 1.0f, 1.0f);
		player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Constants.TICKS_PER_SECOND * 2, 0, false, false, false));

		RushAdvancements.checkWaveConquered(player, round, lootLoc);

		RushReward.generateLoot(lootLoc, round);
	}

	// Updates highest round leaderboard, and omit DRD access

	public static void updatePlayerStats(Player player, int round, boolean isMultiplayer) {
		String rushType = isMultiplayer ? RUSH_HIGHEST_ROUND_MULTIPLAYER : RUSH_HIGHEST_ROUND_SOLO;

		int highestRound = ScoreboardUtils.getScoreboardValue(player, rushType).orElse(0);
		boolean isHighestRound = round > highestRound;

		if (isHighestRound) {
			ScoreboardUtils.setScoreboardValue(player, rushType, round);
			Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "leaderboard update " + player.getName() + " " + rushType);
		}
		if (isHighestRound && round >= 8) {
			MonumentaNetworkRelayIntegration.broadcastCommand("tellmini msg @a[all_worlds=true] <italic><gold>" + player.getName() + "</gold> has succumbed to the Rush of Dissonance with a new personal best! (" +
				(isMultiplayer ? "" : "Solo ")
				+ "Highest Round Reached: " + round + ")");
		} else {
			Bukkit.getServer().sendMessage(Component.empty()
				.append(Component.text(player.getName(), NamedTextColor.GOLD, TextDecoration.ITALIC))
				.append(Component.text(" has succumbed to the Rush of Dissonance! " +
					(isHighestRound ? "with a new personal best! " : "") +
					"(" +
					(isMultiplayer ? "" : "Solo ")
					+ "Round Reached: " + round + ")", NamedTextColor.YELLOW, TextDecoration.ITALIC)));
		}
	}

	// Find an available lootroom via armor stand, and remove the armor stand
	public static @Nullable Location teleportPlayerToLootroom(Player player) {
		try {
			ArmorStand lootRoom = RushArenaUtils.getStandOrThrow(player, RUSH_LOOT_ROOM_TAG);
			Location lootRoomLoc = lootRoom.getLocation();
			lootRoom.remove();
			lootRoomLoc.setYaw(90);
			PlayerUtils.playerTeleport(player, lootRoomLoc);
			return lootRoomLoc;
		} catch (NoSuchElementException e) {
			printDebugMessage("Loot room not found!", player);
			player.sendMessage(RUSH_ERROR_NO_LOOT_ROOM);
			ScoreboardUtils.setScoreboardValue(player, "DRDAccess", 0);
			String pName = player.getName();
			NmsUtils.getVersionAdapter().runConsoleCommandSilently("execute at " + pName + " as " + pName + " run function monumenta:mechanisms/teleporters/shards/isles");
			return null;
		}
	}

	public static void scaleMobHealthMultiplayer(@Nullable Entity entity, int count) {
		int scalingCount = count - 1; // Respect indexing
		if (scalingCount > 0 && scalingCount < 4 && entity instanceof LivingEntity mob) {
			EntityUtils.scaleMaxHealth(mob, MOB_COUNT_MULTIPLIER_PER_PLAYER[scalingCount] - 1, "RushMultiplayerHealthScale");

			mob.getPassengers().stream()
				.filter(e -> e instanceof LivingEntity)
				.forEach(e -> EntityUtils.scaleMaxHealth((LivingEntity) e, MOB_COUNT_MULTIPLIER_PER_PLAYER[scalingCount] - 1, "RushMultiplayerHealthScale"));
		}
	}

	private static final double VULN_SCALING = 0.01;
	private static final double HP_SCALING = 0.01;
	private static final double SPEED_SCALING = 0.0015;

	public static void scaleMobPastRound(@Nullable Entity entity, int round) {
		if (round > SCALING_ROUND && entity instanceof LivingEntity mob) {
			EntityUtils.scaleMaxHealth(mob, HP_SCALING * (round - SCALING_ROUND), "RushScalingHealthScale");
			EntityUtils.addAttribute(mob, Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier("RushScalingSpeedScale", SPEED_SCALING * (round - SCALING_ROUND), AttributeModifier.Operation.MULTIPLY_SCALAR_1));

			for (Entity e : mob.getPassengers()) {
				if (e instanceof LivingEntity le) {
					EntityUtils.scaleMaxHealth(le, HP_SCALING * (round - SCALING_ROUND), "RushScalingHealthScale");
					EntityUtils.addAttribute(le, Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier("RushScalingSpeedScale", SPEED_SCALING * (round - SCALING_ROUND), AttributeModifier.Operation.MULTIPLY_SCALAR_1));
				}
			}

		}
	}

	public static void applyVulnerability(int round, Set<Player> players) {
		if (round > SCALING_ROUND) {
			players.forEach(p -> mPlugin.mEffectManager.addEffect(p, RUSH_VULN, new PercentDamageReceived(Integer.MAX_VALUE, (VULN_SCALING * (round - SCALING_ROUND))).displaysTime(false)));
		}
	}

	public static void restorePlayer(Player player) {
		PotionUtils.clearNegatives(mPlugin, player);
		EntityUtils.setWeakenTicks(mPlugin, player, 0);
		EntityUtils.setSlowTicks(mPlugin, player, 0);

		mPlugin.mEffectManager.clearEffects(player, RUSH_VULN);
		mPlugin.mTimers.removeAllCooldowns(player);

		player.setHealth(EntityUtils.getMaxHealth(player));
		player.setFoodLevel(PlayerUtils.MAX_FOOD_LEVEL);
		player.setSaturation(PlayerUtils.MAX_FOOD_SATURATION_LEVEL);
	}

	public static boolean checkRushOccupation(List<Player> players) {
		return players.get(0).getWorld().getPlayers().stream().anyMatch(mPlayerArenaMap::containsKey);
	}

	public static void clearArenaFromMap(Set<Player> players) {
		players.forEach(mPlayerArenaMap::remove);
	}

	public static void addArenaToMap(List<Player> players, RushArena arena) {
		players.forEach(p -> mPlayerArenaMap.put(p, arena));
	}

	public static double[] calculateMobCount(int round, boolean finalWave) {
		double[] mobCount = new double[4];

		double seasonMobs = seasonMobs(round);
		double eliteMobs = eliteMobs(round);
		double miniboss = 0;

		if (finalWave) {
			seasonMobs *= LAST_WAVE_INCREASE;
			eliteMobs *= LAST_WAVE_INCREASE;

			// Cull the count of mobs to accommodate miniboss spawn
			if (round >= BOSS_ROUND && round % BOSS_INCREMENT == 0) {
				miniboss = 1;
				seasonMobs = seasonMobs(round - BOSS_ROUND) * LAST_WAVE_INCREASE;
				eliteMobs = eliteMobs(round - BOSS_ROUND) * LAST_WAVE_INCREASE;
			} else {
				eliteMobs += 1;
			}
		}

		mobCount[0] = seasonMobs;
		mobCount[1] = eliteMobs;
		mobCount[2] = miniboss;

		return mobCount;
	}

	public static void printDebugMessage(String message, Player player) {
		MMLog.warning("[RoD] " + message + " " + player.getName() + " @ " + player.getWorld().getName());
	}

	public static List<ArmorStand> retrieveArmorStandList(Location loc) {
		return loc.getNearbyEntitiesByType(ArmorStand.class, MAX_SEARCH_RADIUS)
			.stream()
			.toList();
	}

	private static double seasonMobs(int round) {
		return MathUtil.clamp(0.85 * (round - 1) + 5, 0, MAX_COMMON_MOBS);
	}

	private static double eliteMobs(int round) {
		return MathUtil.clamp(0.275 * (round - 1), 0, MAX_ELITE_MOBS);
	}

}
