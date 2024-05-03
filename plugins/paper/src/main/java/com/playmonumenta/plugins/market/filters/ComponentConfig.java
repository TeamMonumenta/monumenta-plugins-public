package com.playmonumenta.plugins.market.filters;

import com.playmonumenta.plugins.itemstats.enums.Location;
import com.playmonumenta.plugins.itemstats.enums.Region;
import com.playmonumenta.plugins.market.MarketListingIndex;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"checkstyle:SingleSpaceSeparator", "checkstyle:RegexpSinglelineJava"})
public class ComponentConfig {

	public static final Map<String, ComponentConfigObject> REGION_CONFIG;
	public static final Map<String, ComponentConfigObject> CURRENCY_CONFIG;
	public static final Map<String, ComponentConfigObject> LOCATION_CONFIG;

	@Nullable public static Map<String, ComponentConfigObject> mSelectedMap;

	private static int mOrderIncrement;

	static {
		REGION_CONFIG = new HashMap<>();
		selectMap(REGION_CONFIG);
		put(Region.VALLEY.toString(), "King's Valley", null, ItemUtils.createBanner(Material.CYAN_BANNER, new Pattern(DyeColor.LIGHT_BLUE, PatternType.CROSS), new Pattern(DyeColor.BLUE, PatternType.CIRCLE_MIDDLE), new Pattern(DyeColor.BLACK, PatternType.FLOWER), new Pattern(DyeColor.BLUE, PatternType.TRIANGLES_TOP), new Pattern(DyeColor.BLUE, PatternType.TRIANGLES_BOTTOM)));
		put(Region.ISLES.toString(), "Celsian Isles", "Quest101<13", ItemUtils.createBanner(Material.GREEN_BANNER, new Pattern(DyeColor.LIME, PatternType.GRADIENT_UP), new Pattern(DyeColor.GREEN, PatternType.BORDER), new Pattern(DyeColor.GREEN, PatternType.RHOMBUS_MIDDLE), new Pattern(DyeColor.LIME, PatternType.CIRCLE_MIDDLE)));
		put(Region.RING.toString(), "Architect's Ring", "R3Access<1", ItemUtils.createBanner(Material.WHITE_BANNER, new Pattern(DyeColor.BROWN, PatternType.STRIPE_SMALL), new Pattern(DyeColor.GREEN, PatternType.TRIANGLES_BOTTOM), new Pattern(DyeColor.GREEN, PatternType.TRIANGLES_TOP), new Pattern(DyeColor.LIGHT_GRAY, PatternType.GRADIENT), new Pattern(DyeColor.GREEN, PatternType.STRIPE_MIDDLE), new Pattern(DyeColor.GRAY, PatternType.GRADIENT_UP), new Pattern(DyeColor.BLACK, PatternType.FLOWER), new Pattern(DyeColor.WHITE, PatternType.CIRCLE_MIDDLE)));

		CURRENCY_CONFIG = new HashMap<>();
		selectMap(CURRENCY_CONFIG);
		put("Experience Bottle", null, null, null);
		put("Crystalline Shard", null, "Quest101<13", null);
		put("Archos Ring", null, "R3Access<1", null);

		LOCATION_CONFIG = new HashMap<>();
		selectMap(LOCATION_CONFIG);

		// NO REGION
		put(Location.NONE.toString(), "No locations", null, null);
		put(Location.SOULTHREAD, null);
		put(Location.QUEST, null);
		put(Location.TRANSMOG, null);
		put(Location.MYTHIC, null);
		put(Location.CHALLENGER_SKIN, null);
		put(Location.PASS, null);
		put(Location.SKETCHED, null);

		// King's Valley
		put(Location.OVERWORLD1, null, "epic:r1/fragments/kings_fragment");
		put(Location.CASINO1, "Casino1Rolls<1", "epic:r1/items/currency/chip");
		put(Location.BLITZ, "Blitz<1");
		put(Location.LIGHT, "Arena<1");
		put(Location.ROYAL, null);
		put(Location.LABS, null);
		put(Location.WHITE, null, new ItemStack(Material.WHITE_WOOL));
		put(Location.ORANGE, null, new ItemStack(Material.ORANGE_WOOL));
		put(Location.MAGENTA, null, new ItemStack(Material.MAGENTA_WOOL));
		put(Location.LIGHTBLUE, "LightBlue<1 && Quest32<15", new ItemStack(Material.LIGHT_BLUE_WOOL));
		put(Location.YELLOW, "Yellow<1 && Quest33<11", new ItemStack(Material.YELLOW_WOOL));
		put(Location.WILLOWS, "R1Bonus<1 && Quest04<6");
		put(Location.WILLOWSKIN, "R1Bonus<1");
		put(Location.EPHEMERAL, "!adv:monumenta:dungeons/roguelike/find");
		put(Location.EPHEMERAL_ENHANCEMENTS, "RogFinished<1");
		put(Location.REVERIE, null, "epic:r1/dungeons/reverie/corrupted_malevolence");
		put(Location.SANCTUM, null);
		put(Location.VERDANT, null);
		put(Location.VERDANTSKIN, null);
		put(Location.AZACOR, "AzacorHardWin<1");
		put(Location.KAUL, "KaulWins<1");
		put(Location.DIVINE, "KaulWins<1");
		put(Location.LOWTIDE, "Quest101<13");

		// Celsian Isles
		put(Location.OVERWORLD2, "Quest101<13");
		put(Location.CASINO2, "Casino2Rolls<1");
		put(Location.CARNIVAL, "LifetimeTokens<1");
		put(Location.TREASURE, "TreasureHunt<1");
		put(Location.DOCKS, "Daily2Completed<1");
		put(Location.INTELLECT, "Quest101<13");
		put(Location.DELVES, null);
		put(Location.LIME, "Lime<1 && Quest105<6", new ItemStack(Material.LIME_WOOL));
		put(Location.PINK, "Pink<1 && Quest116<6", new ItemStack(Material.PINK_WOOL));
		put(Location.GRAY, "Gray<1 && Quest108<9", new ItemStack(Material.GRAY_WOOL));
		put(Location.LIGHTGRAY, "LightGray<1 && Quest134<12", new ItemStack(Material.LIGHT_GRAY_WOOL));
		put(Location.CYAN, "Cyan<1 && Quest109<10", new ItemStack(Material.CYAN_WOOL));
		put(Location.PURPLE, "Purple<1 && Quest110<14", new ItemStack(Material.PURPLE_WOOL));
		put(Location.TEAL, null);
		put(Location.SHIFTING, null);
		put(Location.FORUM, null);
		put(Location.DEPTHS, "Depths<1 && DepthsEndless<30 && DepthsEndless6<30");
		put(Location.MIST, "MistClears<1");
		put(Location.HOARD, "!adv:monumenta:challenges/r2/mist/bounty && MistClears<20");
		put(Location.GREEDSKIN, "MistClears<1");
		put(Location.REMORSE, "SealedRemorse<1");
		put(Location.REMORSEFULSKIN, "SealedRemorse<1");
		put(Location.VIGIL, "!adv:monumenta:challenges/r2/sr/vigil");
		put(Location.RUSH, "RushDown<25 && RushDuo<50");
		put(Location.SILVER, "SKT<1 && SKTH<1");
		put(Location.HORSEMAN, "HorsemanWins<1");
		put(Location.FROSTGIANT, "FGWins<1");
		put(Location.TITANICSKIN, "FGWins<1");
		put(Location.LICH, "LichWins<1");
		put(Location.ETERNITYSKIN, "LichWins<1");

		// Architect's Ring
		put(Location.OVERWORLD3, "R3Access<1");
		put(Location.FOREST, null);
		put(Location.KEEP, null);
		put(Location.STARPOINT, null);
		put(Location.FISHING, "FishCombatsCompleted<1 || FishQuestsCompleted<1");
		put(Location.CASINO3, "Casino3Rolls<1");
		put(Location.BLUE, "Blue<1 && Quest206<6", new ItemStack(Material.BLUE_WOOL));
		put(Location.BROWN, "Brown<1 && Quest211<9", new ItemStack(Material.BROWN_WOOL));
		put(Location.SCIENCE, "Portal<1");
		put(Location.BLUESTRIKE, "MasqueradersRuin<1");
		put(Location.SANGUINEHALLS, "GallerySanguineHallsHighSolo<35 && GallerySanguineHallsHighGroup<35");
		put(Location.MARINANOIR, "GalleryMarinaNoirHighSolo<35 && GalleryMarinaNoirHighGroup<35");
		put(Location.ZENITH, "Zenith<1");
		put(Location.GODSPORE, "GodsporeWins<1");
		put(Location.SIRIUS, "SiriusWins<1");

		// Events
		put(Location.VALENTINE, null);
		put(Location.VALENTINESKIN, null);
		put(Location.APRILFOOLS, null);
		put(Location.APRILFOOLSSKIN, null);
		put(Location.EASTER, null);
		put(Location.EASTERSKIN, null);
		put(Location.HALLOWEEN, null);
		put(Location.HALLOWEENSKIN, null);
		put(Location.UGANDA, null);
		put(Location.WINTER, "EventWins<1 && IceSpleef_MVPWins<1 && SnowFight2Wins<1");
		put(Location.HOLIDAYSKIN, "SnowSpiritWins<1");

	}

	private static void selectMap(Map<String, ComponentConfigObject> map) {
		mSelectedMap = map;
		mOrderIncrement = 0;
	}

	private static void put(String key, @Nullable String displayName, @Nullable String conditions, @Nullable ItemStack displayIcon) {
		if (mSelectedMap != null) {
			mSelectedMap.put(key, new ComponentConfigObject(mOrderIncrement++, displayName, conditions, displayIcon));
		}
	}

	private static void put(Location loc, @Nullable String conditions, @Nullable ItemStack displayIcon) {
		if (mSelectedMap != null) {
			mSelectedMap.put(loc.toString(), new ComponentConfigObject(mOrderIncrement++, loc.getDisplayName(), conditions, displayIcon));
		}
	}


	private static void put(Location loc, @Nullable String conditions, @NotNull String displayIconPath) {
		if (mSelectedMap != null) {
			mSelectedMap.put(loc.toString(), new ComponentConfigObject(mOrderIncrement++, loc.getDisplayName(), conditions, InventoryUtils.getItemFromLootTable(new org.bukkit.Location(Bukkit.getWorlds().get(0), 0, 0, 0), NamespacedKeyUtils.fromString(displayIconPath))));
		}
	}

	private static void put(Location loc, @Nullable String conditions) {
		if (mSelectedMap != null) {
			mSelectedMap.put(loc.toString(), new ComponentConfigObject(mOrderIncrement++, loc.getDisplayName(), conditions, null));
		}
	}


	public static class ComponentConfigObject {
		@Nullable String mDisplayName;
		@Nullable ItemStack mDisplayItemStack;
		int mOrder;
		@Nullable String mBlacklistConditions;

		private ComponentConfigObject(int order, @Nullable String displayName, @Nullable String blacklistConditions, @Nullable ItemStack displayItemStack) {
			this.mDisplayName = displayName;
			this.mDisplayItemStack = displayItemStack;
			this.mOrder = order;
			this.mBlacklistConditions = blacklistConditions;
		}

		public @Nullable String getDisplayName() {
			return mDisplayName;
		}

		public @Nullable ItemStack getDisplayItemStack() {
			return mDisplayItemStack;
		}

		public int getOrder() {
			if (mOrder == 0) {
				return 999999;
			}
			return mOrder;
		}

		public @Nullable String getBlacklistConditions() {
			return mBlacklistConditions;
		}
	}

	public static MarketFilter buildForcedBlacklistFilterForPlayer(Player player) {

		ArrayList<FilterComponent> comps = new ArrayList<>();

		for (MarketListingIndex idx : MarketListingIndex.getAllPlayerSelectable()) {
			if (idx.getComponentConfig() == null || idx.getComponentConfig().isEmpty()) {
				continue;
			}

			ArrayList<String> blacklistedValues = new ArrayList<>();

			for (Map.Entry<String, ComponentConfigObject> configEntry : idx.getComponentConfig().entrySet()) {
				String condition = configEntry.getValue().getBlacklistConditions();
				try {
					if (condition != null && conditionMatchesPlayerScores(condition, player)) {
						blacklistedValues.add(configEntry.getKey());
					}
				} catch (Exception e) {
					MMLog.warning("Failed to handle market forced filter condition: " + condition, e);
					if (player.isOp()) {
						player.sendMessage("Something went wrong while calculating market forced filter condition:");
						player.sendMessage("For " + configEntry.getKey() + " : " + condition);
						player.sendMessage("this makes every item matching this value visible to everyone in the market");
						player.sendMessage("and might lead to prog-skipping. only ops can see this message");
					}

				}
			}

			if (!blacklistedValues.isEmpty()) {
				comps.add(new FilterComponent(idx, Comparator.BLACKLIST, blacklistedValues));
			}
		}

		return new MarketFilter("Forced Filter", comps);
	}

	private static boolean conditionMatchesPlayerScores(String condition, Player player) {

		// handle AND and OR operators
		int indexAND = condition.indexOf(" && ");
		int indexOR = condition.indexOf(" || ");
		int index = 999999;
		if (indexAND != -1) {
			index = Math.min(index, indexAND);
		}
		if (indexOR != -1) {
			index = Math.min(index, indexOR);
		}
		if (index < 999999) {
			// we know there's an operator, we can split
			String cond1 = condition.substring(0, index);
			String op = condition.substring(index, index + 4);
			String cond2 = condition.substring(index + 4);
			if (op.equals(" && ")) {
				return conditionMatchesPlayerScores(cond1, player) && conditionMatchesPlayerScores(cond2, player);
			} else if (op.equals(" || ")) {
				return conditionMatchesPlayerScores(cond1, player) || conditionMatchesPlayerScores(cond2, player);
			}
			return false;
		}

		// handle negative
		if (condition.startsWith("!")) {
			return !conditionMatchesPlayerScores(condition.substring(1), player);
		}

		// handle advancement
		if (condition.startsWith("adv:")) {
			condition = condition.substring(4);
			return AdvancementUtils.checkAdvancement(player, condition);
		}

		// handle scoreboard
		String[] split = condition.split("<=|>=|!=|==|<|>");
		if (split.length != 2) {
			return false;
		}
		int scoreboardValue = ScoreboardUtils.getScoreboardValue(player, split[0]).orElse(0);

		int value = Integer.parseInt(split[1]);
		if (condition.contains("!=")) {
			return scoreboardValue != value;
		} else if (condition.contains("<=")) {
			return scoreboardValue <= value;
		} else if (condition.contains(">=")) {
			return scoreboardValue >= value;
		} else if (condition.contains("==")) {
			return scoreboardValue == value;
		} else if (condition.contains(">")) {
			return scoreboardValue > value;
		} else if (condition.contains("<")) {
			return scoreboardValue < value;
		} else {
			return false;
		}

	}

}
