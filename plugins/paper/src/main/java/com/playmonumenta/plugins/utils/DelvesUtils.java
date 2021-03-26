package com.playmonumenta.plugins.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.loot.LootTable;

import com.playmonumenta.plugins.abilities.delves.Arcanic;
import com.playmonumenta.plugins.abilities.delves.Bloodthirsty;
import com.playmonumenta.plugins.abilities.delves.Carapace;
import com.playmonumenta.plugins.abilities.delves.Chivalrous;
import com.playmonumenta.plugins.abilities.delves.Colossal;
import com.playmonumenta.plugins.abilities.delves.DelveModifier;
import com.playmonumenta.plugins.abilities.delves.Dreadful;
import com.playmonumenta.plugins.abilities.delves.Entropy;
import com.playmonumenta.plugins.abilities.delves.Infernal;
import com.playmonumenta.plugins.abilities.delves.Legionary;
import com.playmonumenta.plugins.abilities.delves.Pernicious;
import com.playmonumenta.plugins.abilities.delves.Relentless;
import com.playmonumenta.plugins.abilities.delves.Spectral;
import com.playmonumenta.plugins.abilities.delves.StatMultiplier;
import com.playmonumenta.plugins.abilities.delves.Transcendent;
import com.playmonumenta.plugins.abilities.delves.Twisted;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.listeners.DelvesListener;
import com.playmonumenta.plugins.server.properties.ServerProperties;

public class DelvesUtils {

	public enum Modifier {

		RELENTLESS(Relentless.class, 1, Material.DIAMOND_PICKAXE, "" + ChatColor.AQUA + ChatColor.BOLD + "Relentless", Relentless.DESCRIPTION, Relentless.RANK_DESCRIPTIONS),
		ARCANIC(Arcanic.class, 2, Material.NETHER_STAR, "" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "Arcanic", Arcanic.DESCRIPTION, Arcanic.RANK_DESCRIPTIONS),
		INFERNAL(Infernal.class, 3, Material.LAVA_BUCKET, "" + ChatColor.GOLD + ChatColor.BOLD + "Infernal", Infernal.DESCRIPTION, Infernal.RANK_DESCRIPTIONS),
		TRANSCENDENT(Transcendent.class, 4, Material.ENDER_EYE, "" + ChatColor.GREEN + ChatColor.BOLD + "Transcendent", Transcendent.DESCRIPTION, Transcendent.RANK_DESCRIPTIONS),
		SPECTRAL(Spectral.class, 5, Material.PHANTOM_MEMBRANE, "" + ChatColor.WHITE + ChatColor.BOLD + "Spectral", Spectral.DESCRIPTION, Spectral.RANK_DESCRIPTIONS),
		DREADFUL(Dreadful.class, 6, Material.BONE, "" + ChatColor.DARK_GRAY + ChatColor.BOLD + "Dreadful", Dreadful.DESCRIPTION, Dreadful.RANK_DESCRIPTIONS),
		COLOSSAL(Colossal.class, 7, Material.IRON_BARS, "" + ChatColor.DARK_BLUE + ChatColor.BOLD + "Colossal", Colossal.DESCRIPTION, Colossal.RANK_DESCRIPTIONS),
		CHIVALROUS(Chivalrous.class, 10, Material.MAGMA_CREAM, "" + ChatColor.DARK_GREEN + ChatColor.BOLD + "Chivalrous", Chivalrous.DESCRIPTION, Chivalrous.RANK_DESCRIPTIONS),
		BLOODTHIRSTY(Bloodthirsty.class, 11, Material.ROTTEN_FLESH, "" + ChatColor.RED + ChatColor.BOLD + "Bloodthirsty", Bloodthirsty.DESCRIPTION, Bloodthirsty.RANK_DESCRIPTIONS),
		PERNICIOUS(Pernicious.class, 12, Material.MUSIC_DISC_11, "" + ChatColor.DARK_AQUA + ChatColor.BOLD + "Pernicious", Pernicious.DESCRIPTION, Pernicious.RANK_DESCRIPTIONS),
		LEGIONARY(Legionary.class, 13, Material.IRON_SWORD, "" + ChatColor.YELLOW + ChatColor.BOLD + "Legionary", Legionary.DESCRIPTION, Legionary.RANK_DESCRIPTIONS),
		CARAPACE(Carapace.class, 14, Material.NETHERITE_HELMET, "" + ChatColor.DARK_PURPLE + ChatColor.BOLD + "Carapace", Carapace.DESCRIPTION, Carapace.RANK_DESCRIPTIONS),
		ENTROPY(Entropy.class, 15, Material.STRUCTURE_VOID, "" + ChatColor.BLUE + ChatColor.BOLD + "Entropy", Entropy.DESCRIPTION, Entropy.RANK_DESCRIPTIONS),
		TWISTED(Twisted.class, 16, Material.TIPPED_ARROW, "" + ChatColor.DARK_RED + ChatColor.BOLD + "Twisted", Twisted.DESCRIPTION, Twisted.RANK_DESCRIPTIONS);

		private static final Modifier[] MODIFIER_COLUMN_MAPPINGS = new Modifier[18];

		static {
			for (Modifier modifier : Modifier.values()) {
				MODIFIER_COLUMN_MAPPINGS[modifier.getColumn()] = modifier;
			}
		}

		private Class<? extends DelveModifier> mClass;
		private int mColumn;
		private Material mIcon;
		private String mName;
		private String mDescription;
		private String[][] mRankDescriptions;

		Modifier(Class<? extends DelveModifier> cls, int column, Material icon, String name, String description, String[][] rankDescriptions) {
			mClass = cls;
			mColumn = column;
			mIcon = icon;
			mName = name;
			mDescription = description;
			mRankDescriptions = rankDescriptions;
		}

		public Class<? extends DelveModifier> getAbilityClass() {
			return mClass;
		}

		public int getColumn() {
			return mColumn;
		}

		public Material getIcon() {
			return mIcon;
		}

		public String getName() {
			return mName;
		}

		public String getDescription() {
			return mDescription;
		}

		public String[][] getRankDescriptions() {
			return mRankDescriptions;
		}

		public static Modifier getModifier(int column) {
			return MODIFIER_COLUMN_MAPPINGS[column];
		}
	}

	public static class DelveInfo {

		private static final int MAX_DEPTH_POINTS;

		private static final int TWISTED_DEPTH_POINTS = 5;

		private static final EnumMap<Modifier, Integer> MODIFIER_RANK_CAPS = new EnumMap<>(Modifier.class);

		static {
			MODIFIER_RANK_CAPS.put(Modifier.RELENTLESS, 5);
			MODIFIER_RANK_CAPS.put(Modifier.ARCANIC, 5);
			MODIFIER_RANK_CAPS.put(Modifier.INFERNAL, 5);
			MODIFIER_RANK_CAPS.put(Modifier.TRANSCENDENT, 3);
			MODIFIER_RANK_CAPS.put(Modifier.SPECTRAL, 3);
			MODIFIER_RANK_CAPS.put(Modifier.DREADFUL, 3);
			MODIFIER_RANK_CAPS.put(Modifier.COLOSSAL, 3);
			MODIFIER_RANK_CAPS.put(Modifier.CHIVALROUS, 3);
			MODIFIER_RANK_CAPS.put(Modifier.BLOODTHIRSTY, 3);
			MODIFIER_RANK_CAPS.put(Modifier.PERNICIOUS, 3);
			MODIFIER_RANK_CAPS.put(Modifier.LEGIONARY, 5);
			MODIFIER_RANK_CAPS.put(Modifier.CARAPACE, 5);
			MODIFIER_RANK_CAPS.put(Modifier.ENTROPY, 5);
			MODIFIER_RANK_CAPS.put(Modifier.TWISTED, 1);

			int maxDepthPoints = 0;
			for (Map.Entry<Modifier, Integer> entry : MODIFIER_RANK_CAPS.entrySet()) {
				if (entry.getKey() == Modifier.TWISTED) {
					maxDepthPoints += TWISTED_DEPTH_POINTS;
				} else {
					maxDepthPoints += entry.getValue();
				}
			}

			MAX_DEPTH_POINTS = maxDepthPoints;
		}

		private final Player mPlayer;
		private final String mDungeon;

		private boolean mIsEditable = false;

		private long mDelveScore;
		private int mDepthPointsRegular;
		private int mDepthPointsEntropyUnassigned;
		private int mDepthPointsTwisted;

		private final EnumMap<Modifier, Integer> mModifierRanks = new EnumMap<>(Modifier.class);

		private DelveInfo(Player player) {
			this(player, ServerProperties.getShardName());
		}

		private DelveInfo(Player player, String dungeon) {
			this(player, dungeon, getDelveScore(player, dungeon));
		}

		private DelveInfo(Player player, String dungeon, long delveScore) {
			mPlayer = player;
			mDungeon = dungeon;
			mDelveScore = delveScore;

			if (!storeDelveInfo()) {
				player.sendMessage("You currently have an invalid Delves score. Please contact a moderator, and do NOT start/continue a Delve.");
			}
		}

		/*
		 * Return false if the score is invalid - need to check because scores can be altered externally
		 *
		 * The correct structure is (x = unusable, a = all modifiers active flag, u = unused, d = depth points, m = modifier):
		 * 00 0 0000000000000 000000 000 000 000 000 000 000 000 000 000 000 000 000 000 000
		 * x  a u             d      m   m   m   m   m   m   m   m   m   m   m   m   m   m
		 */
		private boolean storeDelveInfo() {
			if (mDelveScore == 0) {
				mIsEditable = true;

				for (Modifier modifier : Modifier.values()) {
					mModifierRanks.put(modifier, 0);
				}

				computeDepthPoints(true);

				return true;
			}

			long delveScore = mDelveScore;

			for (Map.Entry<Modifier, Integer> entry : MODIFIER_RANK_CAPS.entrySet()) {
				Modifier modifier = entry.getKey();
				int cap = entry.getValue();

				int rank = (int)(delveScore & 0x7);	// Bit mask the last 3 bits
				if (rank > cap) {
					return false;
				}

				mModifierRanks.put(modifier, rank);
				delveScore >>= 3;
			}

			// Zero scores have no entropy, and non-zero scores should be finalized
			computeDepthPoints(true);

			delveScore &= 0xFF;	// Get rid of the flag for the all modifiers active advancement

			if (delveScore != getDepthPoints() || delveScore > MAX_DEPTH_POINTS) {
				return false;
			}

			return true;
		}

		private void computeDepthPoints(boolean entropyAssigned) {
			mDepthPointsRegular = 0;

			for (Modifier modifier : Modifier.values()) {
				if (modifier == Modifier.TWISTED) {
					mDepthPointsTwisted = mModifierRanks.get(modifier) * TWISTED_DEPTH_POINTS;
				} else {
					mDepthPointsRegular += mModifierRanks.get(modifier);

					if (modifier == Modifier.ENTROPY) {
						if (entropyAssigned) {
							mDepthPointsEntropyUnassigned = 0;
						} else {
							mDepthPointsEntropyUnassigned = Entropy.getDepthPointsAssigned(mModifierRanks.get(modifier));
						}
					}
				}
			}
		}

		public static int getMaxDepthPoints() {
			return MAX_DEPTH_POINTS;
		}

		public static int getRankCap(Modifier modifier) {
			return MODIFIER_RANK_CAPS.get(modifier);
		}

		public boolean isEditable() {
			return mIsEditable;
		}

		public void storeDelveScore() {
			assignEntropyDepthPoints();

			mDelveScore = getDepthPoints();

			List<Integer> reversedRanks = new ArrayList<>(mModifierRanks.values());
			Collections.reverse(reversedRanks);

			for (int rank : reversedRanks) {
				mDelveScore <<= 3;
				mDelveScore += rank;
			}

			if (getDepthPoints() == getMaxDepthPoints()) {
				mDelveScore |= 0x2000000000000000L;
			}

			setDelveScore(mPlayer, mDungeon, mDelveScore);

			DELVE_INFO_MAPPINGS.put(mPlayer.getUniqueId(), this);
		}

		private void assignEntropyDepthPoints() {
			int pointsToAssign = mDepthPointsEntropyUnassigned;
			mDepthPointsEntropyUnassigned = 0;

			List<Modifier> modifiers = new ArrayList<>(Arrays.asList(Modifier.values()));
			modifiers.remove(Modifier.ENTROPY);
			modifiers.remove(Modifier.TWISTED);

			while (pointsToAssign > 0 && modifiers.size() > 0) {
				int index = FastUtils.RANDOM.nextInt(modifiers.size());
				Modifier modifier = modifiers.get(index);

				int rank = getRank(modifier);
				if (rank < getRankCap(modifier)) {
					setRank(modifier, rank + 1);
					pointsToAssign--;
				} else {
					modifiers.remove(index);
				}
			}
		}

		public void setRank(Modifier modifier, int rank) {
			int oldRank = mModifierRanks.get(modifier);
			int difference = rank - oldRank;

			mModifierRanks.put(modifier, rank);

			if (modifier == Modifier.TWISTED) {
				mDepthPointsTwisted += difference * TWISTED_DEPTH_POINTS;
			} else {
				mDepthPointsRegular += difference;

				if (modifier == Modifier.ENTROPY) {
					mDepthPointsEntropyUnassigned += Entropy.getDepthPointsAssigned(rank) - Entropy.getDepthPointsAssigned(oldRank);
				}
			}
		}

		public int getRank(Modifier modifier) {
			return mModifierRanks.get(modifier);
		}

		public int getDepthPoints() {
			return Math.min(MAX_DEPTH_POINTS - TWISTED_DEPTH_POINTS - getRankCap(Modifier.ENTROPY) + getRank(Modifier.ENTROPY),
					mDepthPointsRegular + mDepthPointsEntropyUnassigned) + mDepthPointsTwisted;
		}

		public Collection<Class<? extends DelveModifier>> getActiveModifiers() {
			Collection<Class<? extends DelveModifier>> activeModifiers = new ArrayList<>();

			if (StatMultiplier.canUseStatic(mPlayer)) {
				activeModifiers.add(StatMultiplier.class);
			}

			for (Modifier modifier : mModifierRanks.keySet()) {
				if (DelveModifier.canUse(mPlayer, modifier)) {
					activeModifiers.add(modifier.getAbilityClass());
				}
			}

			return activeModifiers;
		}
	}

	private static final Map<UUID, DelveInfo> DELVE_INFO_MAPPINGS = new HashMap<>();

	private static final Map<String, String> SHARD_SCOREBOARD_PREFIX_MAPPINGS = new HashMap<>();

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
	}

	public static long getDelveScore(Player player, String dungeon) {
		// Get around the reference abilities with null player in the AbilityManager
		if (player == null) {
			return 02211111111111111L;
		}

		String prefix = SHARD_SCOREBOARD_PREFIX_MAPPINGS.get(dungeon);
		if (prefix == null) {
			return 0;
		}

		long msb = ScoreboardUtils.getScoreboardValue(player, prefix + 1);
		long lsb = ScoreboardUtils.getScoreboardValue(player, prefix + 2);
		return (msb << 31) + lsb;	// Shift MSB by only 31 bits because treating ints as 31 bit unsigned ints
	}

	public static void setDelveScore(Player player, String dungeon, long score) {
		String prefix = SHARD_SCOREBOARD_PREFIX_MAPPINGS.get(dungeon);
		long msb = score >> 31;
		long lsb = score & 0x7FFFFFFFL;	// Bit mask the last 31 bits

		ScoreboardUtils.setScoreboardValue(player, prefix + 1, (int) msb);
		ScoreboardUtils.setScoreboardValue(player, prefix + 2, (int) lsb);
	}

	public static DelveInfo getDelveInfo(Player player) {
		DelveInfo info = DELVE_INFO_MAPPINGS.get(player.getUniqueId());
		if (info == null) {
			info = new DelveInfo(player);
			DELVE_INFO_MAPPINGS.put(player.getUniqueId(), info);
		}

		return info;
	}

	public static DelveInfo removeDelveInfo(Player player) {
		return DELVE_INFO_MAPPINGS.remove(player.getUniqueId());
	}

	/*
	 * This should only be called when the player is not on the the dungeon
	 * shard itself (e.g. modifier selection). As such, it doesn't make
	 * sense to save the DelveInfo to a map because it won't be repeatedly
	 * accessed later on.
	 */
	public static DelveInfo getDelveInfo(Player player, String dungeon) {
		return new DelveInfo(player, dungeon);
	}

	public static void duplicateLibraryOfSoulsMob(LivingEntity mob) {
		duplicateLibraryOfSoulsMob(mob, mob.getLocation());
	}

	public static void duplicateLibraryOfSoulsMob(LivingEntity mob, Location loc) {
		// Only the bottom mob's name is in the LoS
		if (mob.isInsideVehicle()) {
			return;
		}

		String name = mob.getCustomName();

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
	}



	private static final int MINIMUM_DEPTH_POINTS = 5;

	public static int getLootCapDepthPoints(int players) {
		switch (players) {
			case 1:
				return 12;
			case 2:
				return 18;
			case 3:
				return 22;
			default:
				return 25;
		}
	}

	private static class DelveLootTableGroup {

		private final String mBaseTable;
		private final String mDelveMaterialTable;
		private final String mCosmeticMaterialTable;

		private final String[] mRegularTables;

		public DelveLootTableGroup(String baseTable, String delveMaterialTable, String cosmeticMaterialTable, String... regularTables) {
			mBaseTable = baseTable;
			mDelveMaterialTable = delveMaterialTable;
			mCosmeticMaterialTable = cosmeticMaterialTable;

			mRegularTables = regularTables;
		}

		public String getDelveLootTable(int depthPoints, int playerCount) {
			if (depthPoints == 0) {
				return null;
			}

			double random = FastUtils.RANDOM.nextDouble();

			if (random < getCosmeticMaterialTableChance(depthPoints)) {
				return mCosmeticMaterialTable;
			}

			if (random < getDelveMaterialTableChance(depthPoints, playerCount)) {
				return mDelveMaterialTable;
			}

			return mBaseTable;
		}

		private static double getDelveMaterialTableChance(int depthPoints, int players) {
			int edp = Math.min(getLootCapDepthPoints(players), depthPoints) - MINIMUM_DEPTH_POINTS;
			return edp < 0 ? 0 : (0.3 + 0.05*edp - 0.00075*edp*edp);
		}

		private static double getCosmeticMaterialTableChance(int depthPoints) {
			return Math.max(0, (double)(depthPoints - getLootCapDepthPoints(9001)) / (DelveInfo.getMaxDepthPoints() - getLootCapDepthPoints(9001)));
		}

		public void mapDelveLootTables(Map<String, DelveLootTableGroup> map) {
			for (String regularTable : mRegularTables) {
				map.put(regularTable, this);
			}
		}
	}

	public static final Map<String, DelveLootTableGroup> DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS = new HashMap<>();

	static {
		new DelveLootTableGroup("r1/delves/white/base_chest", "r1/delves/white/dmat_chest", "r1/delves/white/cmat_chest", "r1/dungeons/1/level_2_chest", "r1/dungeons/1/level_3_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r1/delves/white/base_final", "r1/delves/white/dmat_final", "r1/delves/white/cmat_final", "r1/dungeons/1/final_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r1/delves/orange/base_chest", "r1/delves/orange/dmat_chest", "r1/delves/orange/cmat_chest", "r1/dungeons/2/level_2_chest", "r1/dungeons/2/level_3_chest", "r1/dungeons/2/level_4_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r1/delves/orange/base_final", "r1/delves/orange/dmat_final", "r1/delves/orange/cmat_final", "r1/dungeons/2/final_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r1/delves/magenta/base_chest", "r1/delves/magenta/dmat_chest", "r1/delves/magenta/cmat_chest", "r1/dungeons/3/level_3_chest", "r1/dungeons/3/level_4_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r1/delves/magenta/base_final", "r1/delves/magenta/dmat_final", "r1/delves/magenta/cmat_final", "r1/dungeons/3/final_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r1/delves/lightblue/base_academy", "r1/delves/lightblue/dmat_academy", "r1/delves/lightblue/cmat_academy", "r1/dungeons/4/level_3_chest_u", "r1/dungeons/4/level_4_chest_u").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r1/delves/lightblue/base_bastion", "r1/delves/lightblue/dmat_bastion", "r1/delves/lightblue/cmat_bastion", "r1/dungeons/4/level_3_chest_a", "r1/dungeons/4/level_4_chest_a").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r1/delves/lightblue/base_final", "r1/delves/lightblue/dmat_final", "r1/delves/lightblue/cmat_final", "r1/dungeons/4/final_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r1/delves/yellow/base_seasons", "r1/delves/yellow/dmat_seasons", "r1/delves/yellow/cmat_seasons", "r1/dungeons/5/overgrown-man-1", "r1/dungeons/5/poison-man-1", "r1/dungeons/5/poison-man-2", "r1/dungeons/5/fish-1", "r1/dungeons/5/fish-2", "r1/dungeons/5/dragon-1", "r1/dungeons/5/dragon-2", "r1/dungeons/5/polar-1").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r1/delves/yellow/base_ender", "r1/delves/yellow/dmat_ender", "r1/delves/yellow/cmat_ender", "r1/dungeons/5/ender-1").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r1/delves/yellow/base_final", "r1/delves/yellow/dmat_final", "r1/delves/yellow/cmat_final", "r1/dungeons/5/overgrown-man-2").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r1/delves/willows/base_chest", "r1/delves/willows/dmat_chest", "r1/delves/willows/cmat_chest", "r1/dungeons/bonus/level_4_chest", "r1/dungeons/bonus/level_5_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r1/delves/willows/base_final", "r1/delves/willows/dmat_final", "r1/delves/willows/cmat_final", "r1/dungeons/bonus/final_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r1/delves/reverie/base_chest", "r1/delves/reverie/dmat_chest", "r1/delves/reverie/cmat_chest", "r1/dungeons/reverie/chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r2/delves/lime/base_chest", "r2/delves/lime/dmat_chest", "r2/delves/lime/cmat_chest", "r2/dungeons/lime/swamplevel_2_chest", "r2/dungeons/lime/citylevel_2_chest", "r2/dungeons/lime/citylevel_3_chest", "r2/dungeons/lime/librarylevel_2_chest", "r2/dungeons/lime/librarylevel_3_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/lime/base_final", "r2/delves/lime/dmat_final", "r2/delves/lime/cmat_final", "r2/dungeons/lime/final_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r2/delves/pink/base_temple", "r2/delves/pink/dmat_temple", "r2/delves/pink/cmat_temple", "r2/dungeons/pink/spring_chest", "r2/dungeons/pink/summer_chest", "r2/dungeons/pink/autumn_chest", "r2/dungeons/pink/winter_chest", "r2/dungeons/pink/temple_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/pink/base_dissonant", "r2/delves/pink/dmat_dissonant", "r2/delves/pink/cmat_dissonant", "r2/dungeons/pink/dissonant_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/pink/base_final", "r2/delves/pink/dmat_final", "r2/delves/pink/cmat_final", "r2/dungeons/pink/final_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r2/delves/gray/base_chest", "r2/delves/gray/dmat_chest", "r2/delves/gray/cmat_chest", "r2/dungeons/gray/chest_outside", "r2/dungeons/gray/chest_library", "r2/dungeons/gray/chest_labs").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/gray/base_final", "r2/delves/gray/dmat_final", "r2/delves/gray/cmat_final", "r2/dungeons/gray/chest_final").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r2/delves/lightgray/base_chest", "r2/delves/lightgray/dmat_chest", "r2/delves/lightgray/cmat_chest", "r2/dungeons/lightgray/level_3_chestoutside", "r2/dungeons/lightgray/level_4_chestoutside", "r2/dungeons/lightgray/level_3_chestpalace", "r2/dungeons/lightgray/level_4_chestpalace").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/lightgray/base_final", "r2/delves/lightgray/dmat_final", "r2/delves/lightgray/cmat_final", "r2/dungeons/lightgray/chest_final").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r2/delves/cyan/base_mine", "r2/delves/cyan/dmat_mine", "r2/delves/cyan/cmat_mine", "r2/dungeons/cyan/level_3_chestmine", "r2/dungeons/cyan/level_4_chestmine").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/cyan/base_temple", "r2/delves/cyan/dmat_temple", "r2/delves/cyan/cmat_temple", "r2/dungeons/cyan/level_3_chesttemple", "r2/dungeons/cyan/level_4_chesttemple").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/cyan/base_final", "r2/delves/cyan/dmat_final", "r2/delves/cyan/cmat_final", "r2/dungeons/cyan/final_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r2/delves/purple/base_pirate", "r2/delves/purple/dmat_pirate", "r2/delves/purple/cmat_pirate", "r2/dungeons/purple/chestpirate").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/purple/base_temple", "r2/delves/purple/dmat_temple", "r2/delves/purple/cmat_temple", "r2/dungeons/purple/chesttemple").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/purple/base_final", "r2/delves/purple/dmat_final", "r2/delves/purple/cmat_final", "r2/dungeons/purple/final_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r2/delves/teal/base_chest", "r2/delves/teal/dmat_chest", "r2/delves/teal/cmat_chest", "r2/dungeons/teal/ruined", "r2/dungeons/teal/eroded", "r2/dungeons/teal/pristine", "r2/dungeons/delves/teal/colosseum").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/teal/base_escape", "r2/delves/teal/dmat_escape", "r2/delves/teal/cmat_escape", "r2/dungeons/teal/escape").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/teal/base_final", "r2/delves/teal/dmat_final", "r2/delves/teal/cmat_final", "r2/dungeons/teal/final").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r2/delves/forum/base_forum", "r2/delves/forum/dmat_forum", "r2/delves/forum/cmat_forum", "r2/dungeons/forum/forum").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/forum/base_quarters", "r2/delves/forum/dmat_quarters", "r2/delves/forum/cmat_quarters", "r2/dungeons/forum/quarters").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/forum/base_conservatory", "r2/delves/forum/dmat_conservatory", "r2/delves/forum/cmat_conservatory", "r2/dungeons/forum/conservatory").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/forum/base_conscriptorium", "r2/delves/forum/dmat_conscriptorium", "r2/delves/forum/cmat_conscriptorium", "r2/dungeons/forum/conscriptorium").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/forum/base_final", "r2/delves/forum/dmat_final", "r2/delves/forum/cmat_final", "r2/dungeons/forum/final").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r2/delves/shiftingcity/base_chest", "r2/delves/shiftingcity/dmat_chest", "r2/delves/shiftingcity/cmat_chest", "r2/dungeons/fred/normal_city", "r2/dungeons/fred/objective_city", "r2/dungeons/fred/normal_lush", "r2/dungeons/fred/objective_lush", "r2/dungeons/fred/normal_water", "r2/dungeons/fred/objective_water", "r2/dungeons/fred/challenge").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/shiftingcity/base_final", "r2/delves/shiftingcity/dmat_final", "r2/delves/shiftingcity/cmat_final", "r2/dungeons/fred/final_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
	}

	public static void setDelveLootTable(Player player, Block block) {
		BlockState blockState = block.getState();
		if (blockState instanceof Chest) {
			setDelveLootTable(DelvesUtils.getDelveInfo(player).getDepthPoints(),
					PlayerUtils.playersInRange(player.getLocation(), ChestUtils.CHEST_LUCK_RADIUS).size(),
					(Chest) blockState);
		}
	}

	public static void setDelveLootTable(int depthPoints, int playerCount, Chest chest) {
		LootTable lootTable = chest.getLootTable();
		if (lootTable != null) {
			String path = lootTable.getKey().getKey();

			DelveLootTableGroup group = DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS.get(path);
			if (group != null) {
				String newPath = group.getDelveLootTable(depthPoints, playerCount);
				if (newPath != null) {
					chest.setLootTable(Bukkit.getLootTable(new NamespacedKey("epic", newPath)));
					chest.update();
				}
			}
		}
	}



	public static class DelveModifierSelectionGUI {

		private static final Map<String, String> DUNGEON_FUNCTION_MAPPINGS = new HashMap<>();

		static {
			DUNGEON_FUNCTION_MAPPINGS.put("white", "function monumenta:lobbies/d1/new");
			DUNGEON_FUNCTION_MAPPINGS.put("orange", "function monumenta:lobbies/d2/new");
			DUNGEON_FUNCTION_MAPPINGS.put("magenta", "function monumenta:lobbies/d3/new");
			DUNGEON_FUNCTION_MAPPINGS.put("lightblue", "function monumenta:lobbies/d4/new");
			DUNGEON_FUNCTION_MAPPINGS.put("yellow", "function monumenta:lobbies/d5/new");
			DUNGEON_FUNCTION_MAPPINGS.put("willows", "function monumenta:lobbies/db1/new");
			DUNGEON_FUNCTION_MAPPINGS.put("reverie", "function monumenta:lobbies/dc/new");
			DUNGEON_FUNCTION_MAPPINGS.put("lime", "function monumenta:lobbies/d6/new");
			DUNGEON_FUNCTION_MAPPINGS.put("pink", "function monumenta:lobbies/d7/new");
			DUNGEON_FUNCTION_MAPPINGS.put("gray", "function monumenta:lobbies/d8/new");
			DUNGEON_FUNCTION_MAPPINGS.put("lightgray", "function monumenta:lobbies/d9/new");
			DUNGEON_FUNCTION_MAPPINGS.put("cyan", "function monumenta:lobbies/d10/new");
			DUNGEON_FUNCTION_MAPPINGS.put("purple", "function monumenta:lobbies/d11/new");
			DUNGEON_FUNCTION_MAPPINGS.put("teal", "function monumenta:lobbies/dtl/new");
			DUNGEON_FUNCTION_MAPPINGS.put("forum", "function monumenta:lobbies/dff/new");
			DUNGEON_FUNCTION_MAPPINGS.put("shiftingcity", "function monumenta:lobbies/drl2/new");
		}

		private static final int ROWS = 6;
		private static final int COLUMNS = 9;

		private static final int SUMMARY_INDEX = 0;
		private static final int BEGIN_DELVE_INDEX = COLUMNS - 1;
		private static final int PREVIOUS_PAGE_INDEX = (ROWS - 1) * COLUMNS;
		private static final int NEXT_PAGE_INDEX = ROWS * COLUMNS - 1;
		private static final int RESET_MODIFIERS_INDEX = (ROWS - 1) * COLUMNS;
		private static final int SELECT_ALL_MODIFIERS_INDEX = ROWS * COLUMNS - 1;

		private final Player mPlayer;
		private final String mDungeon;
		private final DelveInfo mDelveInfo;
		private final Inventory mInventory1 = Bukkit.createInventory(null, ROWS * COLUMNS, "Delve Modifier Selection");
		private final Inventory mInventory2 = Bukkit.createInventory(null, ROWS * COLUMNS, "Delve Modifier Selection");

		private boolean mCanBeginDelve = false;

		public DelveModifierSelectionGUI(Player player, String dungeon) {
			mPlayer = player;
			mDungeon = dungeon;
			mDelveInfo = getDelveInfo(player, dungeon);

			generateGUI();
		}

		private void generateGUI() {
			setColumn(mInventory1, getInventory1LeftColumn(), SUMMARY_INDEX);
			setColumn(mInventory1, getInventory1RightColumn(), BEGIN_DELVE_INDEX);

			setColumn(mInventory2, getInventory2LeftColumn(), SUMMARY_INDEX);
			setColumn(mInventory2, getInventory2RightColumn(), BEGIN_DELVE_INDEX);

			for (Modifier modifier : Modifier.values()) {
				int column = modifier.getColumn();

				if (column < COLUMNS) {
					setColumn(mInventory1, getModifierItems(modifier), column);
				} else {
					setColumn(mInventory2, getModifierItems(modifier), column % COLUMNS);
				}
			}
		}

		private static void setColumn(Inventory inventory, List<ItemStack> items, int column) {
			int index = column;

			for (ItemStack item : items) {
				inventory.setItem(index, item);
				index += COLUMNS;
			}
		}

		public boolean contains(Inventory inventory) {
			return mInventory1.equals(inventory) || mInventory2.equals(inventory);
		}

		public void openGUI() {
			mPlayer.openInventory(mInventory1);
		}

		public void registerClick(InventoryClickEvent event) {
			// Make sure player wasn't clicking their own inventory
			Inventory inventory = event.getClickedInventory();
			if (!mInventory1.equals(inventory) && !mInventory2.equals(inventory)) {
				return;
			}

			ItemStack clickedItem = event.getCurrentItem();

			if (clickedItem == null || clickedItem.getType() == Material.BLACK_STAINED_GLASS_PANE) {
				return;
			}

			if (mInventory1.getItem(NEXT_PAGE_INDEX).equals(clickedItem)) {
				nextPage();
			} else if (mInventory2.getItem(PREVIOUS_PAGE_INDEX).equals(clickedItem)) {
				previousPage();
			} else if (mDelveInfo.isEditable()) {
				if (mInventory1.getItem(BEGIN_DELVE_INDEX).equals(clickedItem) || mInventory2.getItem(BEGIN_DELVE_INDEX).equals(clickedItem)) {
					beginDelve();
				} else if (mInventory1.getItem(RESET_MODIFIERS_INDEX).equals(clickedItem)) {
					resetModifiers();
				} else if (mInventory2.getItem(SELECT_ALL_MODIFIERS_INDEX).equals(clickedItem)) {
					selectAllModifiers();
				} else {
					updateModifier(event.getInventory(), event.getSlot());
				}
			}
		}

		private void beginDelve() {
			if (mCanBeginDelve) {
				mDelveInfo.storeDelveScore();
				DelvesListener.closeGUI(mPlayer);

				Bukkit.getConsoleSender().getServer().dispatchCommand(Bukkit.getConsoleSender(),
						"execute as " + mPlayer.getName() + " at @s run " + DUNGEON_FUNCTION_MAPPINGS.get(mDungeon));
			}
		}

		private void nextPage() {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);

			mPlayer.openInventory(mInventory2);

			mInventory2.setItem(SUMMARY_INDEX, getSummary());
			mInventory2.setItem(BEGIN_DELVE_INDEX, getBeginDelve());
		}

		private void previousPage() {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);

			mPlayer.openInventory(mInventory1);

			mInventory1.setItem(SUMMARY_INDEX, getSummary());
			mInventory1.setItem(BEGIN_DELVE_INDEX, getBeginDelve());
		}

		private void resetModifiers() {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1f, 0.5f);

			for (Modifier modifier : Modifier.values()) {
				mDelveInfo.setRank(modifier, 0);
			}

			generateGUI();
		}

		private void selectAllModifiers() {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.4f, 0.5f);

			for (Modifier modifier : Modifier.values()) {
				mDelveInfo.setRank(modifier, DelveInfo.getRankCap(modifier));
			}

			generateGUI();
		}

		private void updateModifier(Inventory inventory, int index) {
			Modifier modifier = getModifier(inventory, index);

			if (modifier == null) {
				return;
			}

			int oldRank = mDelveInfo.getRank(modifier);
			int newRank = ROWS - 1 - index / COLUMNS;

			for (int i = 0; i <= newRank; i++) {
				mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_NETHER_BRICKS_BREAK, 0.5f + 0.15f * i, 0.4f + 0.1f * i);
			}

			if (modifier == Modifier.TWISTED && newRank == 1 && oldRank == 0) {
				mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.2f, 0.2f);
			}

			if (newRank > oldRank) {
				int current = index;
				for (int i = 0; i < newRank - oldRank; i++) {
					ItemStack item = inventory.getItem(current);
					item.setType(Material.ORANGE_STAINED_GLASS_PANE);

					ItemMeta meta = item.getItemMeta();
					String name = meta.getDisplayName();
					meta.setDisplayName(ChatColor.GOLD + "Rank " + name.charAt(name.length() - 1));
					item.setItemMeta(meta);
					item = ItemUtils.setPlainName(item);

					current += COLUMNS;
				}
			} else if (newRank < oldRank) {
				int current = index;
				for (int i = 0; i < oldRank - newRank; i++) {
					current -= COLUMNS;

					ItemStack item = inventory.getItem(current);
					item.setType(Material.RED_STAINED_GLASS_PANE);

					ItemMeta meta = item.getItemMeta();
					String name = meta.getDisplayName();
					meta.setDisplayName(ChatColor.GRAY + "Rank " + name.charAt(name.length() - 1));
					item.setItemMeta(meta);
					item = ItemUtils.setPlainName(item);
				}
			}

			mDelveInfo.setRank(modifier, newRank);

			inventory.setItem(SUMMARY_INDEX, getSummary());
			inventory.setItem(BEGIN_DELVE_INDEX, getBeginDelve());
		}

		private ItemStack getSummary() {
			int depthPoints = mDelveInfo.getDepthPoints();

			ItemStack item = new ItemStack(Material.SOUL_LANTERN, Math.max(1, depthPoints));

			ItemMeta meta = item.getItemMeta();
			meta.setDisplayName("" + ChatColor.GOLD + ChatColor.BOLD + "Delve Summary");
			meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

			List<String> lore = new ArrayList<>();

			lore.add(ChatColor.WHITE + "" + mDelveInfo.getDepthPoints() + " Depth Points Assigned");

			lore.add(ChatColor.RESET + "");

			lore.add(ChatColor.WHITE + "Stat Multipliers from Depth Points:");
			double damageMultiplier = StatMultiplier.getDamageMultiplier(depthPoints);
			double healthMultiplier = StatMultiplier.getHealthMultiplier(depthPoints);
			double speedMultiplier = StatMultiplier.getSpeedMultiplier(depthPoints);
			if (damageMultiplier >= 1.75) {
				lore.add(String.format(ChatColor.DARK_RED + "- Damage Multiplier: x%.2f", damageMultiplier));
				lore.add(String.format(ChatColor.DARK_RED + "- Health Multiplier: x%.2f", healthMultiplier));
				lore.add(String.format(ChatColor.DARK_RED + "- Speed Multiplier: x%.2f", speedMultiplier));
			} else if (damageMultiplier >= 1.45) {
				lore.add(String.format(ChatColor.RED + "- Damage Multiplier: x%.2f", damageMultiplier));
				lore.add(String.format(ChatColor.RED + "- Health Multiplier: x%.2f", healthMultiplier));
				lore.add(String.format(ChatColor.RED + "- Speed Multiplier: x%.2f", speedMultiplier));
			} else {
				lore.add(String.format(ChatColor.GRAY + "- Damage Multiplier: x%.2f", damageMultiplier));
				lore.add(String.format(ChatColor.GRAY + "- Health Multiplier: x%.2f", healthMultiplier));
				lore.add(String.format(ChatColor.GRAY + "- Speed Multiplier: x%.2f", speedMultiplier));
			}

			lore.add(ChatColor.RESET + "");

			double dungeonMultiplier = StatMultiplier.getStatCompensation(mDungeon);
			lore.add(ChatColor.WHITE + "Stat Multipliers from Base Dungeon:");
			lore.add(String.format(ChatColor.GRAY + "- Damage Multiplier: x%.2f", dungeonMultiplier));
			lore.add(String.format(ChatColor.GRAY + "- Health Multiplier: x%.2f", dungeonMultiplier));

			lore.add(ChatColor.RESET + "");

			lore.add(ChatColor.WHITE + "Delve Material Multipliers (Not Counting Loot Scaling):");
			double baseAmount = DelveLootTableGroup.getDelveMaterialTableChance(MINIMUM_DEPTH_POINTS, 9001);
			double delveMaterialMultiplierSolo = DelveLootTableGroup.getDelveMaterialTableChance(depthPoints, 1) / baseAmount;
			double delveMaterialMultiplierDuo = DelveLootTableGroup.getDelveMaterialTableChance(depthPoints, 2) / baseAmount;
			double delveMaterialMultiplierTrio = DelveLootTableGroup.getDelveMaterialTableChance(depthPoints, 3) / baseAmount;
			double delveMaterialMultiplier = DelveLootTableGroup.getDelveMaterialTableChance(depthPoints, 9001) / baseAmount;

			if (delveMaterialMultiplier > 0) {
				if (delveMaterialMultiplierSolo == DelveLootTableGroup.getDelveMaterialTableChance(9001, 1) / baseAmount) {
					lore.add(String.format(ChatColor.YELLOW + "- 1 Player: x%.2f (Capped)", delveMaterialMultiplierSolo));
				} else {
					lore.add(String.format(ChatColor.GRAY + "- 1 Player: x%.2f", delveMaterialMultiplierSolo));
				}

				if (delveMaterialMultiplierDuo == DelveLootTableGroup.getDelveMaterialTableChance(9001, 2) / baseAmount) {
					lore.add(String.format(ChatColor.YELLOW + "- 2 Players: x%.2f (Capped)", delveMaterialMultiplierDuo));
				} else {
					lore.add(String.format(ChatColor.GRAY + "- 2 Players: x%.2f", delveMaterialMultiplierDuo));
				}

				if (delveMaterialMultiplierTrio == DelveLootTableGroup.getDelveMaterialTableChance(9001, 3) / baseAmount) {
					lore.add(String.format(ChatColor.YELLOW + "- 3 Players: x%.2f (Capped)", delveMaterialMultiplierTrio));
				} else {
					lore.add(String.format(ChatColor.GRAY + "- 3 Players: x%.2f", delveMaterialMultiplierTrio));
				}

				if (delveMaterialMultiplier == DelveLootTableGroup.getDelveMaterialTableChance(9001, 9001) / baseAmount) {
					lore.add(String.format(ChatColor.YELLOW + "- 4+ Players: x%.2f (Capped)", delveMaterialMultiplier));
				} else {
					lore.add(String.format(ChatColor.GRAY + "- 4+ Players: x%.2f", delveMaterialMultiplier));
				}
			} else {
				lore.add(String.format(ChatColor.DARK_GRAY + "  - 1 Player: x%.2f", delveMaterialMultiplierSolo));
				lore.add(String.format(ChatColor.DARK_GRAY + "  - 2 Players: x%.2f", delveMaterialMultiplierDuo));
				lore.add(String.format(ChatColor.DARK_GRAY + "  - 3 Players: x%.2f", delveMaterialMultiplierTrio));
				lore.add(String.format(ChatColor.DARK_GRAY + "  - 4+ Players: x%.2f", delveMaterialMultiplier));
			}

			lore.add(ChatColor.RESET + "");

			if (depthPoints == DelveInfo.getMaxDepthPoints()) {
				lore.add(ChatColor.GOLD + "- All Modifiers Advancement Granted upon Completion");
			} else {
				lore.add(ChatColor.DARK_GRAY + "- All Modifiers Advancement Granted upon Completion");
			}

			meta.setLore(lore);

			item.setItemMeta(meta);
			item = ItemUtils.setPlainTag(item);

			return item;
		}

		private ItemStack getBeginDelve() {
			ItemStack item = new ItemStack(Material.OBSERVER, 1);

			ItemMeta meta = item.getItemMeta();
			meta.setDisplayName("" + ChatColor.GOLD + ChatColor.BOLD + "Begin Delve");

			List<String> lore = new ArrayList<>();

			int depthPoints = mDelveInfo.getDepthPoints();

			if (depthPoints < MINIMUM_DEPTH_POINTS) {
				lore.add(ChatColor.RED + "- Requires " + MINIMUM_DEPTH_POINTS + " Depth Points to begin");
				mCanBeginDelve = false;
			} else {
				int rank = mDelveInfo.getRank(Modifier.ENTROPY);
				int required = rank == 0 ? 0 : Entropy.getTotalDepthPointsRequired(rank);
				if (depthPoints < required) {
					lore.add(ChatColor.RED + "- Requires " + required + " Depth Points to begin with Entropy");
					mCanBeginDelve = false;
				} else {
					mCanBeginDelve = true;
				}
			}

			meta.setLore(lore);

			item.setItemMeta(meta);
			item = ItemUtils.setPlainTag(item);

			return item;
		}

		private ItemStack getResetModifiers() {
			ItemStack item = new ItemStack(Material.BARRIER, 1);

			ItemMeta meta = item.getItemMeta();
			meta.setDisplayName("" + ChatColor.WHITE + ChatColor.BOLD + "Reset Modifiers");
			item.setItemMeta(meta);
			item = ItemUtils.setPlainName(item);

			return item;
		}

		private ItemStack getSelectAllModifiers() {
			ItemStack item = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1);

			ItemMeta meta = item.getItemMeta();
			meta.setDisplayName("" + ChatColor.WHITE + ChatColor.BOLD + "Select All Modifiers");
			item.setItemMeta(meta);
			item = ItemUtils.setPlainName(item);

			return item;
		}

		private ItemStack getNextPage() {
			ItemStack item = new ItemStack(Material.ARROW, 1);

			ItemMeta meta = item.getItemMeta();
			meta.setDisplayName("" + ChatColor.WHITE + ChatColor.BOLD + "Next Page");
			item.setItemMeta(meta);
			item = ItemUtils.setPlainName(item);

			return item;
		}

		private ItemStack getPreviousPage() {
			ItemStack item = new ItemStack(Material.ARROW, 1);

			ItemMeta meta = item.getItemMeta();
			meta.setDisplayName("" + ChatColor.WHITE + ChatColor.BOLD + "Previous Page");
			item.setItemMeta(meta);
			item = ItemUtils.setPlainName(item);

			return item;
		}

		private List<ItemStack> getModifierItems(Modifier modifier) {
			List<ItemStack> modifierItems = new ArrayList<>();
			String[][] rankDescriptions = modifier.getRankDescriptions();
			int row = ROWS - 1;

			for (; row > rankDescriptions.length; row--) {
				modifierItems.add(new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1));
			}

			for (; row > mDelveInfo.getRank(modifier); row--) {
				ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE, 1);
				ItemMeta meta = item.getItemMeta();

				meta.setDisplayName(ChatColor.GRAY + "Rank " + row);

				List<String> lore = new ArrayList<>();
				for (int i = 0; i < rankDescriptions[row - 1].length; i++) {
					lore.add(ChatColor.WHITE + rankDescriptions[row - 1][i]);
				}

				meta.setLore(lore);

				item.setItemMeta(meta);
				item = ItemUtils.setPlainTag(item);

				modifierItems.add(item);
			}

			for (; row > 0; row--) {
				ItemStack item = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE, 1);
				ItemMeta meta = item.getItemMeta();

				meta.setDisplayName(ChatColor.GOLD + "Rank " + row);

				List<String> lore = new ArrayList<>();
				for (int i = 0; i < rankDescriptions[row - 1].length; i++) {
					lore.add(ChatColor.WHITE + rankDescriptions[row - 1][i]);
				}

				meta.setLore(lore);

				item.setItemMeta(meta);
				item = ItemUtils.setPlainTag(item);

				modifierItems.add(item);
			}

			ItemStack item = new ItemStack(modifier.getIcon());

			ItemMeta meta = item.getItemMeta();
			meta.setDisplayName(modifier.getName());
			meta.setLore(Arrays.asList(ChatColor.WHITE + modifier.getDescription()));

			if (modifier == Modifier.PERNICIOUS) {
				meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
			} else if (modifier == Modifier.RELENTLESS || modifier == Modifier.LEGIONARY || modifier == Modifier.CARAPACE) {
				meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			} else if (modifier == Modifier.TWISTED) {
				((PotionMeta) meta).setColor(Color.fromRGB(6684672));
				meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
			}

			item.setItemMeta(meta);
			item = ItemUtils.setPlainTag(item);

			modifierItems.add(item);

			return modifierItems;
		}

		private List<ItemStack> getInventory1LeftColumn() {
			List<ItemStack> items = new ArrayList<>();

			items.add(getSummary());

			for (int i = 1; i < ROWS - 1; i++) {
				items.add(new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1));
			}

			items.add(getResetModifiers());

			return items;
		}

		private List<ItemStack> getInventory1RightColumn() {
			List<ItemStack> items = new ArrayList<>();

			items.add(getBeginDelve());

			for (int i = 1; i < ROWS - 1; i++) {
				items.add(new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1));
			}

			items.add(getNextPage());

			return items;
		}

		private List<ItemStack> getInventory2LeftColumn() {
			List<ItemStack> items = new ArrayList<>();

			items.add(getSummary());

			for (int i = 1; i < ROWS - 1; i++) {
				items.add(new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1));
			}

			items.add(getPreviousPage());

			return items;
		}

		private List<ItemStack> getInventory2RightColumn() {
			List<ItemStack> items = new ArrayList<>();

			items.add(getBeginDelve());

			for (int i = 1; i < ROWS - 1; i++) {
				items.add(new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1));
			}

			items.add(getSelectAllModifiers());

			return items;
		}

		private Modifier getModifier(Inventory inventory, int index) {
			int column = index % COLUMNS;
			if (mInventory2.equals(inventory)) {
				column += COLUMNS;
			}

			return Modifier.getModifier(column);
		}
	}



	private static final String DELVE_MOB_TAG = "delve_mob";

	public static boolean isDelveMob(Entity entity) {
		Set<String> tags = entity.getScoreboardTags();
		return tags != null && tags.contains(DELVE_MOB_TAG);
	}

}
