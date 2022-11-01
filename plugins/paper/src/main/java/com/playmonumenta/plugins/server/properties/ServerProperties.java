package com.playmonumenta.plugins.server.properties;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;

public class ServerProperties {

	private static final ServerProperties INSTANCE = new ServerProperties();

	private boolean mJoinMessagesEnabled = false;
	private boolean mIsTownWorld = false;
	// Height of plots in Sierhaven so that players under plots stay in adventure
	private int mPlotSurvivalMinHeight = 256;

	private boolean mIsSleepingEnabled = true;
	private boolean mKeepLowTierInventory = false;
	private boolean mClassSpecializationsEnabled = false;
	private boolean mAbilityEnhancementsEnabled = false;
	private boolean mAuditMessagesEnabled = true;
	private boolean mRepairExplosions = false;
	private boolean mPreventDungeonItemTransfer = true;
	private boolean mReplaceSpawnerEntities = true;
	private boolean mInfusionsEnabled = true;
	private int mHTTPStatusPort = 8000;

	private String mShardName = "default_settings";

	private final EnumSet<Material> mUnbreakableBlocks = EnumSet.noneOf(Material.class);
	private final EnumSet<Material> mAlwaysPickupMats = EnumSet.noneOf(Material.class);
	private final EnumSet<Material> mNamedPickupMats = EnumSet.noneOf(Material.class);

	private final List<NamespacedKey> mEggifySpawnEggs = new ArrayList<>();
	private int mLootingLimiterMobKills = 0;
	private int mLootingLimiterSpawners = 0;

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

	public static boolean getIsSleepingEnabled() {
		return INSTANCE.mIsSleepingEnabled;
	}

	public static boolean getKeepLowTierInventory() {
		return INSTANCE.mKeepLowTierInventory;
	}

	public static boolean getClassSpecializationsEnabled() {
		return INSTANCE.mClassSpecializationsEnabled;
	}

	public static boolean getAbilityEnhancementsEnabled() {
		return INSTANCE.mAbilityEnhancementsEnabled;
	}

	public static boolean getAuditMessagesEnabled() {
		return INSTANCE.mAuditMessagesEnabled;
	}

	public static boolean getRepairExplosions() {
		return INSTANCE.mRepairExplosions;
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

	public static int getHTTPStatusPort() {
		return INSTANCE.mHTTPStatusPort;
	}

	public static String getShardName() {
		return INSTANCE.mShardName;
	}

	public static Set<Material> getUnbreakableBlocks() {
		return INSTANCE.mUnbreakableBlocks;
	}

	public static Set<Material> getAlwaysPickupMats() {
		return INSTANCE.mAlwaysPickupMats;
	}

	public static Set<Material> getNamedPickupMats() {
		return INSTANCE.mNamedPickupMats;
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

	public static void load(Plugin plugin, @Nullable CommandSender sender) {
		INSTANCE.loadInternal(plugin, sender);
	}

	private void loadInternal(Plugin plugin, @Nullable CommandSender sender) {
		QuestUtils.loadScriptedQuests(plugin, ".", sender, (object) -> {
			mJoinMessagesEnabled = getPropertyValueBool(object, "joinMessagesEnabled", mJoinMessagesEnabled);
			mIsTownWorld = getPropertyValueBool(object, "isTownWorld", mIsTownWorld);
			mPlotSurvivalMinHeight = getPropertyValueInt(object, "plotSurvivalMinHeight", mPlotSurvivalMinHeight);

			mIsSleepingEnabled = getPropertyValueBool(object, "isSleepingEnabled", mIsSleepingEnabled);
			mKeepLowTierInventory = getPropertyValueBool(object, "keepLowTierInventory", mKeepLowTierInventory);
			mClassSpecializationsEnabled = getPropertyValueBool(object, "classSpecializationsEnabled", mClassSpecializationsEnabled);
			mAbilityEnhancementsEnabled = getPropertyValueBool(object, "abilityEnhancementsEnabled", mAbilityEnhancementsEnabled);
			mAuditMessagesEnabled = getPropertyValueBool(object, "auditMessagesEnabled", mAuditMessagesEnabled);
			mRepairExplosions = getPropertyValueBool(object, "repairExplosions", mRepairExplosions);
			mPreventDungeonItemTransfer = getPropertyValueBool(object, "preventDungeonItemTransfer", mPreventDungeonItemTransfer);
			mReplaceSpawnerEntities = getPropertyValueBool(object, "replaceSpawnerEntities", mReplaceSpawnerEntities);
			mInfusionsEnabled = getPropertyValueBool(object, "infusionsEnabled", mInfusionsEnabled);
			mHTTPStatusPort = getPropertyValueInt(object, "httpStatusPort", mHTTPStatusPort);

			mShardName = getPropertyValueString(object, "shardName", mShardName);

			getPropertyValueMaterialList(plugin, object, "unbreakableBlocks", sender, mUnbreakableBlocks);
			getPropertyValueMaterialList(plugin, object, "alwaysPickupMaterials", sender, mAlwaysPickupMats);
			getPropertyValueMaterialList(plugin, object, "namedPickupMaterials", sender, mNamedPickupMats);

			getPropertyValueCollection(plugin, object, "eggifySpawnEggs", sender, NamespacedKeyUtils::fromString, mEggifySpawnEggs);

			mLootingLimiterMobKills = getPropertyValueInt(object, "lootingLimiterMobKills", mLootingLimiterMobKills);
			mLootingLimiterSpawners = getPropertyValueInt(object, "lootingLimiterSpawners", mLootingLimiterSpawners);

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

		out.add("isSleepingEnabled = " + mIsSleepingEnabled);
		out.add("keepLowTierInventory = " + mKeepLowTierInventory);
		out.add("classSpecializationsEnabled = " + mClassSpecializationsEnabled);
		out.add("abilityEnhancementsEnabled = " + mAbilityEnhancementsEnabled);
		out.add("auditMessagesEnabled = " + mAuditMessagesEnabled);
		out.add("repairExplosions = " + mRepairExplosions);
		out.add("preventDungeonItemTransfer = " + mPreventDungeonItemTransfer);
		out.add("replaceSpawnerEntities = " + mReplaceSpawnerEntities);
		out.add("infusionsEnabled = " + mInfusionsEnabled);
		out.add("httpStatusPort = " + mHTTPStatusPort);

		out.add("shardName = " + mShardName);

		out.add("unbreakableBlocks = [" + mUnbreakableBlocks.stream().map(Enum::toString).collect(Collectors.joining("  ")) + "]");
		out.add("alwaysPickupMaterials = [" + mAlwaysPickupMats.stream().map(Enum::toString).collect(Collectors.joining("  ")) + "]");
		out.add("namedPickupMaterials = [" + mNamedPickupMats.stream().map(Enum::toString).collect(Collectors.joining("  ")) + "]");

		out.add("eggifySpawnEggs = [" + mEggifySpawnEggs.stream().map(NamespacedKey::toString).collect(Collectors.joining("  ")) + "]");

		out.add("lootingLimiterMobKills = " + mLootingLimiterMobKills);
		out.add("lootingLimiterSpawners = " + mLootingLimiterSpawners);

		return out;
	}

	private boolean getPropertyValueBool(JsonObject object, String properyName, boolean defaultVal) {
		boolean value = defaultVal;

		JsonElement element = object.get(properyName);
		if (element != null) {
			value = element.getAsBoolean();
		}

		return value;
	}

	private int getPropertyValueInt(JsonObject object, String properyName, int defaultVal) {
		int value = defaultVal;

		JsonElement element = object.get(properyName);
		if (element != null) {
			value = element.getAsInt();
		}

		return value;
	}

	private String getPropertyValueString(JsonObject object, String propertyName, String defaultVal) {
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
						sender.sendMessage(ChatColor.RED + "Invalid " + propertyName + " element: '" + iter + "'");
						MessagingUtils.sendStackTrace(sender, e);
					}
				}
			}
		}
	}

	public static ItemStatUtils.Region getRegion() {
		return ServerProperties.getAbilityEnhancementsEnabled() ? ItemStatUtils.Region.RING : ServerProperties.getClassSpecializationsEnabled() ? ItemStatUtils.Region.ISLES : ItemStatUtils.Region.VALLEY;
	}

}
