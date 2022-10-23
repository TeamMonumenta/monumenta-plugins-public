package com.playmonumenta.plugins.delves;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.delves.abilities.Chivalrous;
import com.playmonumenta.plugins.delves.abilities.Entropy;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.listeners.MobListener;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import it.unimi.dsi.util.XoRoShiRo128PlusRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.NotNull;

public class DelvesUtils {

	public static final int MAX_DEPTH_POINTS;
	public static final int MINIMUM_DEPTH_POINTS = 5;
	private static final EnumMap<DelvesModifier, Integer> MODIFIER_RANK_CAPS = new EnumMap<>(DelvesModifier.class);

	static {
		MODIFIER_RANK_CAPS.put(DelvesModifier.ARCANIC, 5);
		MODIFIER_RANK_CAPS.put(DelvesModifier.INFERNAL, 5);
		MODIFIER_RANK_CAPS.put(DelvesModifier.TRANSCENDENT, 5);
		MODIFIER_RANK_CAPS.put(DelvesModifier.SPECTRAL, 5);
		MODIFIER_RANK_CAPS.put(DelvesModifier.DREADFUL, 5);
		MODIFIER_RANK_CAPS.put(DelvesModifier.COLOSSAL, 5);
		MODIFIER_RANK_CAPS.put(DelvesModifier.CHIVALROUS, 5);
		MODIFIER_RANK_CAPS.put(DelvesModifier.BLOODTHIRSTY, 5);
		MODIFIER_RANK_CAPS.put(DelvesModifier.PERNICIOUS, 5);
		MODIFIER_RANK_CAPS.put(DelvesModifier.LEGIONARY, 5);
		MODIFIER_RANK_CAPS.put(DelvesModifier.CARAPACE, 5);
		MODIFIER_RANK_CAPS.put(DelvesModifier.ECHOES, 5);
		MODIFIER_RANK_CAPS.put(DelvesModifier.VENGEANCE, 5);
		MODIFIER_RANK_CAPS.put(DelvesModifier.ENTROPY, 5);
		MODIFIER_RANK_CAPS.put(DelvesModifier.TWISTED, 5);
		MODIFIER_RANK_CAPS.put(DelvesModifier.FRAGILE, 1);
		MODIFIER_RANK_CAPS.put(DelvesModifier.ASSASSINS, 1);
		MODIFIER_RANK_CAPS.put(DelvesModifier.UNYIELDING, 1);
		MODIFIER_RANK_CAPS.put(DelvesModifier.ASTRAL, 1);
		MODIFIER_RANK_CAPS.put(DelvesModifier.CHRONOLOGY, 1);
		MODIFIER_RANK_CAPS.put(DelvesModifier.RIFTBORN, 1);
		MODIFIER_RANK_CAPS.put(DelvesModifier.HAUNTED, 1);

		// Depths endless changes- use dev2 for testing
		if (ServerProperties.getShardName().startsWith("depths")
			|| ServerProperties.getShardName().equals("dev2")
			|| ServerProperties.getShardName().equals("corridors")) {
			// Total of 72 points in depths
			MODIFIER_RANK_CAPS.put(DelvesModifier.ARCANIC, 7);
			MODIFIER_RANK_CAPS.put(DelvesModifier.INFERNAL, 7);
			MODIFIER_RANK_CAPS.put(DelvesModifier.TRANSCENDENT, 5);
			MODIFIER_RANK_CAPS.put(DelvesModifier.SPECTRAL, 7);
			MODIFIER_RANK_CAPS.put(DelvesModifier.DREADFUL, 5);
			MODIFIER_RANK_CAPS.put(DelvesModifier.COLOSSAL, 7);
			MODIFIER_RANK_CAPS.put(DelvesModifier.CHIVALROUS, 5);
			MODIFIER_RANK_CAPS.put(DelvesModifier.BLOODTHIRSTY, 5);
			MODIFIER_RANK_CAPS.put(DelvesModifier.PERNICIOUS, 5);
			MODIFIER_RANK_CAPS.put(DelvesModifier.LEGIONARY, 7);
			MODIFIER_RANK_CAPS.put(DelvesModifier.CARAPACE, 7);
			MODIFIER_RANK_CAPS.put(DelvesModifier.VENGEANCE, 5);
			MODIFIER_RANK_CAPS.put(DelvesModifier.ECHOES, 0);
		}

		int maxDepthPoints = 0;
		for (Map.Entry<DelvesModifier, Integer> entry : MODIFIER_RANK_CAPS.entrySet()) {
			if (!DelvesModifier.rotatingDelveModifiers().contains(entry.getKey())
				|| getWeeklyRotatingModifier().contains(entry.getKey())) {
				maxDepthPoints += entry.getValue() * entry.getKey().getPointsPerLevel();
			}
		}

		MAX_DEPTH_POINTS = maxDepthPoints;
	}

	public static List<DelvesModifier> getWeeklyRotatingModifier() {
		List<List<DelvesModifier>> nWeekRotation = new ArrayList<>();
		List<DelvesModifier> rotatingMods = DelvesModifier.rotatingDelveModifiers();
		for (int i = 0; i < rotatingMods.size() - 1; i++) {
			for (int j = i + 1; j < rotatingMods.size(); j++) {
				nWeekRotation.add(Arrays.asList(rotatingMods.get(i), rotatingMods.get(j)));
			}
		}
		Collections.shuffle(nWeekRotation, new XoRoShiRo128PlusRandom(DateUtils.getWeeklyVersion() / nWeekRotation.size()));
		ArrayList<DelvesModifier> available = new ArrayList<>(nWeekRotation.get((int)(DateUtils.getWeeklyVersion() % nWeekRotation.size())));
		if (!available.contains(DelvesModifier.HAUNTED)) {
			available.add(DelvesModifier.HAUNTED);
		}
		return available;
	}

	public static ItemStack getRankItem(DelvesModifier mod, int rank, int level) {
		if (rank > MODIFIER_RANK_CAPS.get(mod)) {
			return null;
		}
		rank = Math.min(rank, MODIFIER_RANK_CAPS.get(mod));
		ItemStack stack = new ItemStack(level >= rank ? Material.ORANGE_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
		ItemMeta meta = stack.getItemMeta();

		meta.displayName(Component.text("Rank " + rank, (level >= rank ? NamedTextColor.GOLD : NamedTextColor.RED)).decoration(TextDecoration.ITALIC, false));

		List<Component> lore = new ArrayList<>();
		for (String loreString : mod.getRankDescriptions()[rank - 1]) {
			lore.add(Component.text(loreString, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
		}

		meta.lore(lore);
		stack.setItemMeta(meta);
		return stack;
	}

	public static int getMaxPointAssignable(@Nullable DelvesModifier mod, int point) {
		if (mod == null || point < 0) {
			return 0;
		}
		return Math.min(point, MODIFIER_RANK_CAPS.get(mod));
	}

	public static int stampDelveInfo(@Nullable CommandSender sender, @NotNull Player target, @NotNull String dungeonName, @Nullable DelvesModifier modifier) {
		if (modifier == null) {
			return 0;
		}

		Map<String, DelvesManager.DungeonDelveInfo> map = DelvesManager.PLAYER_DELVE_DUNGEON_MOD_MAP.getOrDefault(target.getUniqueId(), new HashMap<>());

		if (!map.containsKey(dungeonName)) {
			if (sender != null && !(sender instanceof ProxiedCommandSender)) {
				sender.sendMessage(Component.text("Player: " + target.getName() + " has no delve info for " + dungeonName, NamedTextColor.GOLD));
			}
			return 0;
		}

		DelvesManager.DungeonDelveInfo info = map.get(dungeonName);
		int points = info.get(modifier);

		if (sender != null && !(sender instanceof ProxiedCommandSender)) {
			sender.sendMessage(Component.text(modifier.name() + ": " + points, NamedTextColor.GOLD));
		}
		return points;
	}

	public static void copyDelvePoint(@Nullable CommandSender sender, @NotNull Player copyTarget, @NotNull Player target, @NotNull String dungeonName) {
		DelvesManager.PLAYER_DELVE_DUNGEON_MOD_MAP.putIfAbsent(copyTarget.getUniqueId(), new HashMap<>());
		DelvesManager.PLAYER_DELVE_DUNGEON_MOD_MAP.putIfAbsent(target.getUniqueId(), new HashMap<>());
		Map<String, DelvesManager.DungeonDelveInfo> copyMap = DelvesManager.PLAYER_DELVE_DUNGEON_MOD_MAP.get(copyTarget.getUniqueId());
		Map<String, DelvesManager.DungeonDelveInfo> targetMap = DelvesManager.PLAYER_DELVE_DUNGEON_MOD_MAP.get(target.getUniqueId());

		copyMap.putIfAbsent(dungeonName, new DelvesManager.DungeonDelveInfo());
		targetMap.put(dungeonName, copyMap.get(dungeonName).cloneDelveInfo());

		if (sender != null && !(sender instanceof ProxiedCommandSender)) {
			sender.sendMessage(Component.text("Copied delve info " + copyTarget.getName() + " -> " + target.getName() + " for shard " + dungeonName, NamedTextColor.GOLD));
		}

		updateDelveScoreBoard(target);
	}

	public static int setDelvePoint(@Nullable CommandSender sender, @NotNull Player target, @NotNull String dungeonName, @Nullable DelvesModifier modifier, int level) {
		if (level < 0 || modifier == null) {
			return 0;
		}

		Map<String, DelvesManager.DungeonDelveInfo> map = DelvesManager.PLAYER_DELVE_DUNGEON_MOD_MAP.getOrDefault(target.getUniqueId(), new HashMap<>());

		map.putIfAbsent(dungeonName, new DelvesManager.DungeonDelveInfo());

		DelvesManager.DungeonDelveInfo info = map.get(dungeonName);
		int oldLevel = info.get(modifier);

		 if (modifier == DelvesModifier.ENTROPY) {
			int pointsToAssign = Entropy.getDepthPointsAssigned(level) - Entropy.getDepthPointsAssigned(oldLevel);
			info.mTotalPoint += pointsToAssign;
			if (pointsToAssign > 0) {
				List<DelvesModifier> mods = DelvesModifier.valuesList();
				mods.remove(DelvesModifier.ENTROPY);

				while (pointsToAssign > 0) {
					if (mods.isEmpty()) {
						break;
					}
					Collections.shuffle(mods);
					DelvesModifier mod = mods.get(0);
					int oldValue = info.get(mod);
					if (oldValue == DelvesUtils.getMaxPointAssignable(mod, oldValue + 1)) {
						mods.remove(mod);
						continue;
					}
					info.put(mod, oldValue + 1);
					pointsToAssign--;
				}
			}
		} else {
			info.mTotalPoint += (level - oldLevel);
		}

		info.mTotalPoint = Math.min(info.mTotalPoint, MAX_DEPTH_POINTS);
		info.put(modifier, level);
		if (sender != null && !(sender instanceof ProxiedCommandSender)) {
			sender.sendMessage(Component.text(modifier.name() + ": " + info.get(modifier), NamedTextColor.GOLD));
		}

		updateDelveScoreBoard(target);
		return level;
	}

	public static void clearDelvePlayerByShard(@Nullable CommandSender sender, @NotNull Player target, @NotNull String dungeonName) {
		DelvesManager.PLAYER_DELVE_DUNGEON_MOD_MAP.putIfAbsent(target.getUniqueId(), new HashMap<>());
		Map<String, DelvesManager.DungeonDelveInfo> map = DelvesManager.PLAYER_DELVE_DUNGEON_MOD_MAP.get(target.getUniqueId());

		map.remove(dungeonName);

		if (sender != null && !(sender instanceof ProxiedCommandSender)) {
			sender.sendMessage(Component.text("Removed delves point of " + target.getName() + " for shard " + dungeonName, NamedTextColor.GOLD));
		}

		updateDelveScoreBoard(target);
	}

	public static int getDelveModLevel(@NotNull Player target, @NotNull String dungeonName, @Nullable DelvesModifier modifier) {
		if (modifier == null) {
			return 0;
		}
		Map<String, DelvesManager.DungeonDelveInfo> map = DelvesManager.PLAYER_DELVE_DUNGEON_MOD_MAP.getOrDefault(target.getUniqueId(), new HashMap<>());

		return map.getOrDefault(dungeonName, new DelvesManager.DungeonDelveInfo()).get(modifier);

	}

	public static int getPlayerTotalDelvePoint(@Nullable CommandSender sender, @NotNull Player target, @NotNull String dungeonName) {

		Map<String, DelvesManager.DungeonDelveInfo> map = DelvesManager.PLAYER_DELVE_DUNGEON_MOD_MAP.getOrDefault(target.getUniqueId(), new HashMap<>());

		DelvesManager.DungeonDelveInfo info = map.getOrDefault(dungeonName, new DelvesManager.DungeonDelveInfo());

		if (sender != null && !(sender instanceof ProxiedCommandSender)) {
			sender.sendMessage(Component.text(target.getName() + " total delves point " + info.mTotalPoint + " in " + dungeonName, NamedTextColor.GOLD));
		}

		return info.mTotalPoint;
	}

	public static int getTotalDelvePointInRange(@Nullable CommandSender sender, @NotNull Location loc) {
		Map<DelvesModifier, Integer> delvesApplied = new HashMap<>();

		List<DelvesModifier> mods = DelvesModifier.valuesList();

		for (Player delvePlayer : DelvesManager.getParty(loc)) {
			for (DelvesModifier mod : mods) {
				delvesApplied.put(mod, Math.max(delvesApplied.getOrDefault(mod, 0), DelvesManager.getRank(delvePlayer, mod)));
			}
		}

		int totalLevel = 0;
		for (Map.Entry<DelvesModifier, Integer> entry : delvesApplied.entrySet()) {
			totalLevel += entry.getValue();
		}

		totalLevel = Math.min(totalLevel, MAX_DEPTH_POINTS);
		if (sender != null && !(sender instanceof ProxiedCommandSender)) {
			sender.sendMessage(Component.text("Total delves point in range " + totalLevel, NamedTextColor.GOLD));
		}

		return totalLevel;
	}

	public static void assignRandomDelvePoints(Player target, String dungeonName, int pointsToAssign) {
		DelvesManager.PLAYER_DELVE_DUNGEON_MOD_MAP.putIfAbsent(target.getUniqueId(), new HashMap<>());
		Map<String, DelvesManager.DungeonDelveInfo> map = DelvesManager.PLAYER_DELVE_DUNGEON_MOD_MAP.get(target.getUniqueId());
		map.putIfAbsent(dungeonName, new DelvesManager.DungeonDelveInfo());
		DelvesManager.DungeonDelveInfo info = map.get(dungeonName);

		if (pointsToAssign > 0) {
			List<DelvesModifier> mods = DelvesModifier.valuesList();
			mods.remove(DelvesModifier.ENTROPY);
			mods.remove(DelvesModifier.TWISTED);
			mods.remove(DelvesModifier.FRAGILE);
			mods.removeAll(DelvesModifier.rotatingDelveModifiers());

			while (pointsToAssign > 0) {
				if (mods.isEmpty()) {
					break;
				}
				Collections.shuffle(mods);
				DelvesModifier mod = mods.get(0);
				int oldValue = info.get(mod);
				if (oldValue == DelvesUtils.getMaxPointAssignable(mod, oldValue + 1)) {
					mods.remove(mod);
					continue;
				}
				info.put(mod, oldValue + 1);
				pointsToAssign--;
			}
		}
		updateDelveScoreBoard(target);
	}

	public static void updateDelveScoreBoard(Player target) {
		Map<String, DelvesManager.DungeonDelveInfo> dungeonMap = DelvesManager.PLAYER_DELVE_DUNGEON_MOD_MAP.get(target.getUniqueId());
		int score = 0;

		if (dungeonMap != null) {
			DelvesManager.DungeonDelveInfo info = dungeonMap.getOrDefault(ServerProperties.getShardName(), new DelvesManager.DungeonDelveInfo());
			info.recalculateTotalPoint();
			score = info.mTotalPoint;
		}

		ScoreboardUtils.setScoreboardValue(target, "DelvePoint", score);
	}

	public static final Map<String, String> SHARD_SCOREBOARD_PREFIX_MAPPINGS = new HashMap<>();

	static {
		SHARD_SCOREBOARD_PREFIX_MAPPINGS.put("white", "D1Delve");
		SHARD_SCOREBOARD_PREFIX_MAPPINGS.put("orange", "D2Delve");
		SHARD_SCOREBOARD_PREFIX_MAPPINGS.put("magenta", "D3Delve");
		SHARD_SCOREBOARD_PREFIX_MAPPINGS.put("lightblue", "D4Delve");
		SHARD_SCOREBOARD_PREFIX_MAPPINGS.put("yellow", "D5Delve");
		SHARD_SCOREBOARD_PREFIX_MAPPINGS.put("willows", "DWDelve");
		SHARD_SCOREBOARD_PREFIX_MAPPINGS.put("reverie", "DMRDelve");
		SHARD_SCOREBOARD_PREFIX_MAPPINGS.put("lime", "D6Delve");
		SHARD_SCOREBOARD_PREFIX_MAPPINGS.put("pink", "D7Delve");
		SHARD_SCOREBOARD_PREFIX_MAPPINGS.put("gray", "D8Delve");
		SHARD_SCOREBOARD_PREFIX_MAPPINGS.put("lightgray", "D9Delve");
		SHARD_SCOREBOARD_PREFIX_MAPPINGS.put("cyan", "D10Delve");
		SHARD_SCOREBOARD_PREFIX_MAPPINGS.put("purple", "D11Delve");
		SHARD_SCOREBOARD_PREFIX_MAPPINGS.put("teal", "DTLDelve");
		SHARD_SCOREBOARD_PREFIX_MAPPINGS.put("forum", "DFFDelve");
		SHARD_SCOREBOARD_PREFIX_MAPPINGS.put("shiftingcity", "DSCDelve");
		SHARD_SCOREBOARD_PREFIX_MAPPINGS.put("dev1", "DTestDelve");
		SHARD_SCOREBOARD_PREFIX_MAPPINGS.put("dev2", "DTestDelve");
		SHARD_SCOREBOARD_PREFIX_MAPPINGS.put("mobs", "DTestDelve");
		SHARD_SCOREBOARD_PREFIX_MAPPINGS.put("depths", "DDDelve");
	}

	public static void duplicateLibraryOfSoulsMob(LivingEntity mob) {
		duplicateLibraryOfSoulsMob(mob, mob.getLocation());
	}

	private static boolean IS_SPAWNING = false;

	public static void duplicateLibraryOfSoulsMob(LivingEntity mob, Location loc) {
		// Only the bottom mob's name is in the LoS
		if (mob.isInsideVehicle()) {
			return;
		}

		String name = mob.getCustomName();

		if (Chivalrous.isChivalrousName(name)) {
			return;
		}

		if (IS_SPAWNING) {
			return;
		}
		IS_SPAWNING = true;
		try {
			if (name != null) {
				StringBuilder soulNameBuilder = new StringBuilder();
				for (int i = 0; i < name.length(); i++) {
					char c = name.charAt(i);
					if (Character.isLetter(c)) {
						soulNameBuilder.append(c);
					}
				}

				LibraryOfSoulsIntegration.summon(loc, soulNameBuilder.toString());
			}
		} finally {
			IS_SPAWNING = false;
		}
	}


	public static int getLootCapDepthPoints(int players) {
		return switch (players) {
			case 1 -> 12;
			case 2 -> 18;
			case 3 -> 22;
			default -> 25;
		};
	}

	public static final String DELVE_MOB_TAG = "delve_mob";

	public static boolean isDelveMob(Entity entity) {
		Set<String> tags = entity.getScoreboardTags();
		return tags.contains(DELVE_MOB_TAG);
	}

	public static boolean isValidTwistedMob(LivingEntity livingEntity) {
		if (livingEntity.hasMetadata(Constants.SPAWNER_COUNT_METAKEY)) {
			int spawnCount = 0;
			// There should only be one value - just use the latest one
			for (MetadataValue value : livingEntity.getMetadata(Constants.SPAWNER_COUNT_METAKEY)) {
				spawnCount = value.asInt();
			}

			return spawnCount <= MobListener.SPAWNER_DROP_THRESHOLD;
		}
		return true;
	}

	protected static List<Player> playerInRangeForDelves(Location loc) {
		// In dungeons, all players in the same world (i.e. the entire dungeon) are in range
		boolean isDungeon = ScoreboardUtils.getScoreboardValue("$IsDungeon", "const").orElse(0) > 0;
		if (isDungeon) {
			return loc.getWorld().getPlayers().stream()
				.filter(PlayerUtils::playerCountsForLootScaling)
				.toList();
		}

		return PlayerUtils.playersInRange(loc, DelvesManager.DELVES_MAX_PARTY_DISTANCE, true);
	}
}
