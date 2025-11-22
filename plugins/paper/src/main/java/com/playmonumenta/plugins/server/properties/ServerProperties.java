package com.playmonumenta.plugins.server.properties;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.enums.Region;
import com.playmonumenta.plugins.plots.PlotManager;
import com.playmonumenta.plugins.utils.DungeonCommandMapping;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public class ServerProperties {

	private static final ServerProperties INSTANCE = new ServerProperties();

	private boolean mJoinMessagesEnabled = false;
	private boolean mIsTownWorld = false;
	// Height of plots in Sierhaven so that players under plots stay in adventure
	private int mPlotSurvivalMinHeight = 256;
	private boolean mDisableSpoofWorldNames = false;

	private boolean mIsSleepingEnabled = true;
	private boolean mKeepLowTierInventory = false;
	private boolean mClassSpecializationsEnabled = false;
	private boolean mAbilityEnhancementsEnabled = false;
	private boolean mDisableBossProcessing = false;
	private boolean mAuditMessagesEnabled = true;
	private boolean mRepairExplosions = false;
	private @Nullable Pattern mRepairExplosionsWorldPattern = null;
	private boolean mPreventDungeonItemTransfer = true;
	private boolean mReplaceSpawnerEntities = true;
	private boolean mInfusionsEnabled = true;
	private boolean mMasterworkRefundEnabled = false;
	private boolean mLootBoxEnabled = true;
	private boolean mSharedVaultEnabled = true;
	private boolean mShardOpen = true;
	private int mHTTPStatusPort = 8000;

	private String mShardName = "default_settings";

	private boolean mDisableEntityScoresInDefaultWorld = true;
	private final List<String> mDisableEntityScoresInWorlds = new ArrayList<>();

	private final EnumSet<Material> mUnbreakableBlocks = EnumSet.noneOf(Material.class);
	private final EnumSet<Material> mPreciousBlocks = EnumSet.noneOf(Material.class);
	private final EnumSet<Material> mAlwaysPickupMats = EnumSet.noneOf(Material.class);
	private final EnumSet<Material> mNamedPickupMats = EnumSet.noneOf(Material.class);

	private final Set<String> mFormattingFreeBlockNames = new TreeSet<>();
	private final List<NamespacedKey> mDroppedItemReplacements = new ArrayList<>();
	private final List<NamespacedKey> mEggifySpawnEggs = new ArrayList<>();
	private int mLootingLimiterMobKills = 0;
	private int mLootingLimiterSpawners = 0;
	private boolean mLootingLimiterIgnoreBreakingChests = false;
	private boolean mDepthsEnabled = false;
	private boolean mTrickyCreepersEnabled = true;
	private @Nullable String mGameplayDataExportPath = null;

	private JsonObject mDebugParameters = new JsonObject();

	private final Map<String, Integer> mShardCounts = new HashMap<>();

	public ServerProperties() {
	}

	public static boolean getJoinMessagesEnabled() {
		return INSTANCE.mJoinMessagesEnabled;
	}

	public static boolean getIsTownWorld() {
		return INSTANCE.mIsTownWorld;
	}

	public static int getPlotSurvivalMinHeight() {
		return INSTANCE.mPlotSurvivalMinHeight;
	}

	public static boolean disableSpoofWorldNames() {
		return INSTANCE.mDisableSpoofWorldNames;
	}

	public static boolean getIsSleepingEnabled() {
		return INSTANCE.mIsSleepingEnabled;
	}

	public static boolean getKeepLowTierInventory() {
		return INSTANCE.mKeepLowTierInventory;
	}

	// Returns null if the default region should be used
	private static @Nullable Integer getEffectiveRegion(@Nullable Player player) {
		if (player == null) {
			return null;
		}

		String shard = getShardName();
		if (shard.startsWith("dev") || shard.equals("mobs") || shard.contains("plots")) {
			// Default to highest region unlocked
			int plotRegion = PlotManager.getPlotRegion(player);
			if (PlayerUtils.hasUnlockedRing(player) || shard.startsWith("dev") || shard.equals("mobs")) {
				return plotRegion;
			} else if (PlayerUtils.hasUnlockedIsles(player)) {
				return plotRegion == 0 ? 2 : Math.max(plotRegion, 2);
			} else {
				return 1;
			}
		}

		DungeonCommandMapping mapping = DungeonCommandMapping.getByShard(shard);
		if (mapping == null) {
			return null;
		}
		String typeName = mapping.getTypeName();
		if (typeName == null) {
			return null;
		}
		// This assumes that the dungeon mapping is only used for R3 Exalted dungeons
		return ScoreboardUtils.getScoreboardValue(player.getName(), typeName).orElse(0) == 1 ? 3 : null;
	}

	public static boolean getClassSpecializationsEnabled(@Nullable Player player) {
		Integer effectiveRegion = getEffectiveRegion(player);
		if (effectiveRegion == null) {
			return INSTANCE.mClassSpecializationsEnabled;
		}
		return effectiveRegion == 0 || effectiveRegion >= 2;
	}

	public static boolean getAbilityEnhancementsEnabled(@Nullable Player player) {
		Integer effectiveRegion = getEffectiveRegion(player);
		if (effectiveRegion == null) {
			return INSTANCE.mAbilityEnhancementsEnabled;
		}
		return effectiveRegion == 0 || effectiveRegion >= 3;
	}

	public static boolean isBossProcessingDisabled() {
		return INSTANCE.mDisableBossProcessing;
	}

	public static String getGameplayDataExportPath() {
		if (INSTANCE.mGameplayDataExportPath == null) {
			return Plugin.getInstance().getDataFolder() + File.separator;
		}
		return INSTANCE.mGameplayDataExportPath;
	}

	public static boolean getAuditMessagesEnabled() {
		return INSTANCE.mAuditMessagesEnabled;
	}

	public static boolean getRepairExplosions() {
		return INSTANCE.mRepairExplosions;
	}

	public static @Nullable Pattern getRepairExplosionsWorldPattern() {
		return INSTANCE.mRepairExplosionsWorldPattern;
	}

	public static boolean getPreventDungeonItemTransfer() {
		return INSTANCE.mPreventDungeonItemTransfer;
	}

	public static boolean getReplaceSpawnerEntities() {
		return INSTANCE.mReplaceSpawnerEntities;
	}

	public static boolean getInfusionsEnabled() {
		return INSTANCE.mInfusionsEnabled;
	}

	public static boolean getLootBoxEnabled() {
		return INSTANCE.mLootBoxEnabled;
	}

	public static boolean getSharedVaultEnabled() {
		return INSTANCE.mSharedVaultEnabled;
	}

	public static boolean getShardOpen() {
		return INSTANCE.mShardOpen;
	}

	public static int getHTTPStatusPort() {
		return INSTANCE.mHTTPStatusPort;
	}

	public static String getShardName() {
		return INSTANCE.mShardName;
	}

	public static boolean isBuildShard() {
		return getShardName().equals("build");
	}

	public static boolean getEntityScoresDisabled(World world) {
		if (world == null) {
			return true;
		}

		if (INSTANCE.mDisableEntityScoresInDefaultWorld) {
			// TODO Should statically read server.properties instead, or add NMS code
			if (Bukkit.getServer().getWorlds().get(0) == world) {
				return true;
			}
		}

		String worldName = world.getName();
		return INSTANCE.mDisableEntityScoresInWorlds.contains(worldName);
	}

	public static Set<Material> getUnbreakableBlocks() {
		return INSTANCE.mUnbreakableBlocks;
	}

	public static Set<Material> getPreciousBlocks() {
		return INSTANCE.mPreciousBlocks;
	}

	public static Set<Material> getAlwaysPickupMats() {
		return INSTANCE.mAlwaysPickupMats;
	}

	public static Set<Material> getNamedPickupMats() {
		return INSTANCE.mNamedPickupMats;
	}

	public static Set<String> getFormatingFreeBlockNames() {
		return INSTANCE.mFormattingFreeBlockNames;
	}

	public static List<NamespacedKey> getDroppedItemReplacements() {
		return INSTANCE.mDroppedItemReplacements;
	}

	public static List<NamespacedKey> getEggifySpawnEggs() {
		return INSTANCE.mEggifySpawnEggs;
	}

	public static int getLootingLimiterMobKills() {
		return INSTANCE.mLootingLimiterMobKills;
	}

	public static int getLootingLimiterSpawners() {
		return INSTANCE.mLootingLimiterSpawners;
	}

	public static boolean getLootingLimiterIgnoreBreakingChests() {
		return INSTANCE.mLootingLimiterIgnoreBreakingChests;
	}

	public static boolean getDepthsEnabled() {
		return INSTANCE.mDepthsEnabled;
	}

	public static boolean getTrickyCreepersEnabled() {
		return INSTANCE.mTrickyCreepersEnabled;
	}

	public static boolean getMasterworkRefundEnabled() {
		return INSTANCE.mMasterworkRefundEnabled;
	}

	public static boolean getDebugParameter(String parameterName, boolean defaultValue) {
		if (
			INSTANCE.mDebugParameters.get(parameterName) instanceof JsonPrimitive parameterPrimitive &&
				parameterPrimitive.isBoolean()
		) {
			return parameterPrimitive.getAsBoolean();
		}
		return defaultValue;
	}

	public static byte getDebugParameter(String parameterName, byte defaultValue) {
		if (
			INSTANCE.mDebugParameters.get(parameterName) instanceof JsonPrimitive parameterPrimitive &&
				parameterPrimitive.isNumber()
		) {
			return parameterPrimitive.getAsByte();
		}
		return defaultValue;
	}

	public static short getDebugParameter(String parameterName, short defaultValue) {
		if (
			INSTANCE.mDebugParameters.get(parameterName) instanceof JsonPrimitive parameterPrimitive &&
				parameterPrimitive.isNumber()
		) {
			return parameterPrimitive.getAsShort();
		}
		return defaultValue;
	}

	public static int getDebugParameter(String parameterName, int defaultValue) {
		if (
			INSTANCE.mDebugParameters.get(parameterName) instanceof JsonPrimitive parameterPrimitive &&
				parameterPrimitive.isNumber()
		) {
			return parameterPrimitive.getAsInt();
		}
		return defaultValue;
	}

	public static long getDebugParameter(String parameterName, long defaultValue) {
		if (
			INSTANCE.mDebugParameters.get(parameterName) instanceof JsonPrimitive parameterPrimitive &&
				parameterPrimitive.isNumber()
		) {
			return parameterPrimitive.getAsLong();
		}
		return defaultValue;
	}

	public static float getDebugParameter(String parameterName, float defaultValue) {
		if (
			INSTANCE.mDebugParameters.get(parameterName) instanceof JsonPrimitive parameterPrimitive &&
				parameterPrimitive.isNumber()
		) {
			return parameterPrimitive.getAsFloat();
		}
		return defaultValue;
	}

	public static double getDebugParameter(String parameterName, double defaultValue) {
		if (
			INSTANCE.mDebugParameters.get(parameterName) instanceof JsonPrimitive parameterPrimitive &&
				parameterPrimitive.isNumber()
		) {
			return parameterPrimitive.getAsDouble();
		}
		return defaultValue;
	}

	public static String getDebugParameter(String parameterName, String defaultValue) {
		if (
			INSTANCE.mDebugParameters.get(parameterName) instanceof JsonPrimitive parameterPrimitive &&
				parameterPrimitive.isString()
		) {
			return parameterPrimitive.getAsString();
		}
		return defaultValue;
	}

	public static int getShardCount(String shard) {
		return INSTANCE.mShardCounts.getOrDefault(shard, 1);
	}

	public static void load(Plugin plugin, @Nullable CommandSender sender) {
		INSTANCE.loadInternal(plugin, sender);

	}

	private void loadInternal(Plugin plugin, @Nullable CommandSender sender) {
		QuestUtils.loadScriptedQuests(plugin, "properties", sender, (object) -> {
			mJoinMessagesEnabled = getPropertyValueBool(object, "joinMessagesEnabled", mJoinMessagesEnabled);
			mIsTownWorld = getPropertyValueBool(object, "isTownWorld", mIsTownWorld);
			mPlotSurvivalMinHeight = getPropertyValueInt(object, "plotSurvivalMinHeight", mPlotSurvivalMinHeight);
			mDisableSpoofWorldNames = getPropertyValueBool(object, "disableSpoofWorldNames", mDisableSpoofWorldNames);

			mIsSleepingEnabled = getPropertyValueBool(object, "isSleepingEnabled", mIsSleepingEnabled);
			mKeepLowTierInventory = getPropertyValueBool(object, "keepLowTierInventory", mKeepLowTierInventory);
			mClassSpecializationsEnabled = getPropertyValueBool(object, "classSpecializationsEnabled", mClassSpecializationsEnabled);
			mAbilityEnhancementsEnabled = getPropertyValueBool(object, "abilityEnhancementsEnabled", mAbilityEnhancementsEnabled);
			mDisableBossProcessing = getPropertyValueBool(object, "disableBossProcessing", mDisableBossProcessing);
			mAuditMessagesEnabled = getPropertyValueBool(object, "auditMessagesEnabled", mAuditMessagesEnabled);
			mRepairExplosions = getPropertyValueBool(object, "repairExplosions", mRepairExplosions);
			String repairExplosionsWorldPattern = getPropertyValueString(object, "repairExplosionsWorldPattern", null);
			if (repairExplosionsWorldPattern != null) {
				try {
					mRepairExplosionsWorldPattern = Pattern.compile(repairExplosionsWorldPattern);
				} catch (PatternSyntaxException e) {
					String error = "Error in repairExplosionsWorldPattern: " + e.getMessage();
					plugin.getLogger().warning(error);
					if (sender != null) {
						sender.sendMessage(Component.text(error, NamedTextColor.RED));
					}
				}
			}
			mPreventDungeonItemTransfer = getPropertyValueBool(object, "preventDungeonItemTransfer", mPreventDungeonItemTransfer);
			mReplaceSpawnerEntities = getPropertyValueBool(object, "replaceSpawnerEntities", mReplaceSpawnerEntities);
			mInfusionsEnabled = getPropertyValueBool(object, "infusionsEnabled", mInfusionsEnabled);
			mMasterworkRefundEnabled = getPropertyValueBool(object, "masterworkRefundEnabled", mMasterworkRefundEnabled);
			mLootBoxEnabled = getPropertyValueBool(object, "lootBoxEnabled", mLootBoxEnabled);
			mSharedVaultEnabled = getPropertyValueBool(object, "sharedVaultEnabled", mSharedVaultEnabled);
			mShardOpen = getPropertyValueBool(object, "shardOpen", mShardOpen);
			mHTTPStatusPort = getPropertyValueInt(object, "httpStatusPort", mHTTPStatusPort);

			mDisableEntityScoresInDefaultWorld = getPropertyValueBool(object,
				"disableEntityScoresInDefaultWorld",
				mDisableEntityScoresInDefaultWorld);
			mDisableEntityScoresInWorlds.clear();
			JsonElement disableEntityScoresInWorldsElement = object.get("disableEntityScoresInWorlds");
			if (disableEntityScoresInWorldsElement instanceof JsonArray disableEntityScoresInWorldsArray) {
				for (JsonElement disabledWorldElement : disableEntityScoresInWorldsArray) {
					if (disabledWorldElement instanceof JsonPrimitive disabledWorld
						&& disabledWorld.isString()) {
						mDisableEntityScoresInWorlds.add(disabledWorld.getAsString());
					}
				}
			}

			mShardName = getPropertyValueString(object, "shardName", mShardName);
			mGameplayDataExportPath = getPropertyValueString(object, "gameplayDataExportPath", mGameplayDataExportPath);

			getPropertyValueMaterialList(plugin, object, "unbreakableBlocks", sender, mUnbreakableBlocks);
			getPropertyValueMaterialList(plugin, object, "alwaysPickupMaterials", sender, mAlwaysPickupMats);
			getPropertyValueMaterialList(plugin, object, "namedPickupMaterials", sender, mNamedPickupMats);

			// Set default value
			mPreciousBlocks.clear();
			mPreciousBlocks.addAll(EnumSet.of(
				Material.IRON_BLOCK,
				Material.RAW_IRON_BLOCK,
				Material.GOLD_BLOCK,
				Material.RAW_GOLD_BLOCK,
				Material.DIAMOND_BLOCK
			));
			// Replace default value if possible
			getPropertyValueMaterialList(plugin, object, "preciousBlocks", sender, mPreciousBlocks);

			getPropertyValueCollection(plugin, object, "formattingFreeBlockNames", sender, String::toString, mFormattingFreeBlockNames);
			getPropertyValueCollection(plugin, object, "droppedItemReplacements", sender, NamespacedKeyUtils::fromString, mDroppedItemReplacements);
			getPropertyValueCollection(plugin, object, "eggifySpawnEggs", sender, NamespacedKeyUtils::fromString, mEggifySpawnEggs);

			mLootingLimiterMobKills = getPropertyValueInt(object, "lootingLimiterMobKills", mLootingLimiterMobKills);
			mLootingLimiterSpawners = getPropertyValueInt(object, "lootingLimiterSpawners", mLootingLimiterSpawners);
			mLootingLimiterIgnoreBreakingChests = getPropertyValueBool(object, "lootingLimiterIgnoreBreakingChests", mLootingLimiterIgnoreBreakingChests);

			mDepthsEnabled = getPropertyValueBool(object, "depthsEnabled", mDepthsEnabled);
			mTrickyCreepersEnabled = getPropertyValueBool(object, "trickyCreepersEnabled", mTrickyCreepersEnabled);

			if (!(object.get("debugParameters") instanceof JsonObject debugParameters)) {
				mDebugParameters = new JsonObject();
			} else {
				mDebugParameters = debugParameters;
			}

			JsonElement shardCounts = object.get("shardCounts");
			if (shardCounts != null) {
				mShardCounts.clear();
				for (Map.Entry<String, JsonElement> shardCount : shardCounts.getAsJsonObject().entrySet()) {
					mShardCounts.put(shardCount.getKey(), shardCount.getValue().getAsJsonPrimitive().getAsInt());
				}
			}

			return null;
		});

		plugin.getLogger().info("Properties:");
		if (sender != null) {
			sender.sendMessage("Properties:");
		}
		for (String str : toDisplay()) {
			plugin.getLogger().info("  " + str);
			if (sender != null) {
				sender.sendMessage("  " + str);
			}
		}
	}

	private List<String> toDisplay() {
		List<String> out = new ArrayList<>();

		out.add("joinMessagesEnabled = " + mJoinMessagesEnabled);
		out.add("isTownWorld = " + mIsTownWorld);
		out.add("plotSurvivalMinHeight = " + mPlotSurvivalMinHeight);
		out.add("disableSpoofWorldNames = " + mDisableSpoofWorldNames);

		out.add("isSleepingEnabled = " + mIsSleepingEnabled);
		out.add("keepLowTierInventory = " + mKeepLowTierInventory);
		out.add("classSpecializationsEnabled = " + mClassSpecializationsEnabled);
		out.add("abilityEnhancementsEnabled = " + mAbilityEnhancementsEnabled);
		out.add("disableBossProcessing = " + mDisableBossProcessing);
		out.add("auditMessagesEnabled = " + mAuditMessagesEnabled);
		out.add("repairExplosions = " + mRepairExplosions);
		out.add("repairExplosionsWorldPattern = " + (mRepairExplosionsWorldPattern == null ? null : mRepairExplosionsWorldPattern.pattern()));
		out.add("preventDungeonItemTransfer = " + mPreventDungeonItemTransfer);
		out.add("replaceSpawnerEntities = " + mReplaceSpawnerEntities);
		out.add("infusionsEnabled = " + mInfusionsEnabled);
		out.add("masterworkRefundEnabled = " + mMasterworkRefundEnabled);
		out.add("lootBoxEnabled = " + mLootBoxEnabled);
		out.add("sharedVaultEnabled = " + mSharedVaultEnabled);
		out.add("shardOpen = " + mShardOpen);
		out.add("httpStatusPort = " + mHTTPStatusPort);

		out.add("shardName = " + mShardName);
		out.add("gameplayDataExportPath = " + mGameplayDataExportPath);

		out.add("disableEntityScoresInDefaultWorld = " + mDisableEntityScoresInDefaultWorld);
		out.add("disableEntityScoresInWorlds = [" + String.join(" ", mDisableEntityScoresInWorlds) + "]");

		out.add("unbreakableBlocks = [" + mUnbreakableBlocks.stream().map(Enum::toString).collect(Collectors.joining("  ")) + "]");
		out.add("preciousBlocks = [" + mPreciousBlocks.stream().map(Enum::toString).collect(Collectors.joining("  ")) + "]");
		out.add("alwaysPickupMaterials = [" + mAlwaysPickupMats.stream().map(Enum::toString).collect(Collectors.joining("  ")) + "]");
		out.add("namedPickupMaterials = [" + mNamedPickupMats.stream().map(Enum::toString).collect(Collectors.joining("  ")) + "]");

		out.add("formattingFreeBlockNames = <set of " + mFormattingFreeBlockNames.size() + " block names>");
		out.add("droppedItemReplacements = <set of " + mDroppedItemReplacements.size() + " loot tables>");
		out.add("eggifySpawnEggs = <set of " + mEggifySpawnEggs.size() + " loot tables>");

		out.add("lootingLimiterMobKills = " + mLootingLimiterMobKills);
		out.add("lootingLimiterSpawners = " + mLootingLimiterSpawners);
		out.add("lootingLimiterIgnoreBreakingChests = " + mLootingLimiterIgnoreBreakingChests);

		out.add("depthsEnabled = " + mDepthsEnabled + " (NB: changing this requires a restart)");

		out.add("trickyCreepersEnabled = " + mTrickyCreepersEnabled);

		out.add("debugParameters = " + mDebugParameters.toString());

		out.add("shardCounts = " + mShardCounts);

		return out;
	}

	private boolean getPropertyValueBool(JsonObject object, String propertyName, boolean defaultVal) {
		boolean value = defaultVal;

		JsonElement element = object.get(propertyName);
		if (element != null) {
			value = element.getAsBoolean();
		}

		return value;
	}

	private int getPropertyValueInt(JsonObject object, String propertyName, int defaultVal) {
		int value = defaultVal;

		JsonElement element = object.get(propertyName);
		if (element != null) {
			value = element.getAsInt();
		}

		return value;
	}

	@Contract("_, _, !null -> !null")
	private @Nullable String getPropertyValueString(JsonObject object, String propertyName, @Nullable String defaultVal) {
		String value = defaultVal;

		JsonElement element = object.get(propertyName);
		if (element != null) {
			value = element.getAsString();
		}

		return value;
	}

	private void getPropertyValueMaterialList(Plugin plugin, JsonObject object, String propertyName, @Nullable CommandSender sender, Set<Material> set) {
		getPropertyValueCollection(plugin, object, propertyName, sender, Material::getMaterial, set);
	}

	private <T> void getPropertyValueCollection(Plugin plugin, JsonObject object, String propertyName, @Nullable CommandSender sender,
	                                            Function<String, T> parser, Collection<T> collection) {
		JsonElement element = object.get(propertyName);
		if (element != null) {
			collection.clear();

			for (JsonElement iter : element.getAsJsonArray()) {
				try {
					T value = parser.apply(iter.getAsString());
					if (value != null) {
						collection.add(value);
					}
				} catch (Exception e) {
					plugin.getLogger().severe("Invalid " + propertyName + " element: '" + iter + "'");
					e.printStackTrace();

					if (sender != null) {
						sender.sendMessage(Component.text("Invalid " + propertyName + " element: '" + iter + "'", NamedTextColor.RED));
						MessagingUtils.sendStackTrace(sender, e);
					}
				}
			}
		}
	}

	public static Region getRegion(Player player) {
		return getAbilityEnhancementsEnabled(player) ? Region.RING : getClassSpecializationsEnabled(player) ? Region.ISLES : Region.VALLEY;
	}
}
