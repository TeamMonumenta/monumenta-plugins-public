package com.playmonumenta.plugins.server.properties;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.scriptedquests.utils.QuestUtils;

public class ServerProperties {

	private static final ServerProperties INSTANCE = new ServerProperties();

	private boolean mDailyResetEnabled = false;
	private boolean mJoinMessagesEnabled = false;
	private boolean mIsTownWorld = false;
	// Height of plots in Sierhaven so that players under plots stay in adventure
	private int mPlotSurvivalMinHeight = 256;

	private boolean mIsSleepingEnabled = true;
	private boolean mKeepLowTierInventory = false;
	private boolean mClassSpecializationsEnabled = false;
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

	public ServerProperties() {
	}

	public static boolean getDailyResetEnabled() {
		return INSTANCE.mDailyResetEnabled;
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

	public static void load(Plugin plugin, @Nullable CommandSender sender) {
		INSTANCE.loadInternal(plugin, sender);
	}

	private void loadInternal(Plugin plugin, @Nullable CommandSender sender) {
		QuestUtils.loadScriptedQuests(plugin, ".", sender, (object) -> {
			mDailyResetEnabled = getPropertyValueBool(plugin, object, "dailyResetEnabled", mDailyResetEnabled);
			mJoinMessagesEnabled = getPropertyValueBool(plugin, object, "joinMessagesEnabled", mJoinMessagesEnabled);
			mIsTownWorld = getPropertyValueBool(plugin, object, "isTownWorld", mIsTownWorld);
			mPlotSurvivalMinHeight = getPropertyValueInt(plugin, object, "plotSurvivalMinHeight", mPlotSurvivalMinHeight);

			mIsSleepingEnabled = getPropertyValueBool(plugin, object, "isSleepingEnabled", mIsSleepingEnabled);
			mKeepLowTierInventory = getPropertyValueBool(plugin, object, "keepLowTierInventory", mKeepLowTierInventory);
			mClassSpecializationsEnabled = getPropertyValueBool(plugin, object, "classSpecializationsEnabled", mClassSpecializationsEnabled);
			mAuditMessagesEnabled = getPropertyValueBool(plugin, object, "auditMessagesEnabled", mAuditMessagesEnabled);
			mRepairExplosions = getPropertyValueBool(plugin, object, "repairExplosions", mRepairExplosions);
			mPreventDungeonItemTransfer = getPropertyValueBool(plugin, object, "preventDungeonItemTransfer", mPreventDungeonItemTransfer);
			mReplaceSpawnerEntities = getPropertyValueBool(plugin, object, "replaceSpawnerEntities", mReplaceSpawnerEntities);
			mInfusionsEnabled = getPropertyValueBool(plugin, object, "infusionsEnabled", mInfusionsEnabled);
			mHTTPStatusPort = getPropertyValueInt(plugin, object, "httpStatusPort", mHTTPStatusPort);

			mShardName = getPropertyValueString(plugin, object, "shardName", mShardName);

			getPropertyValueMaterialList(plugin, object, "unbreakableBlocks", sender, mUnbreakableBlocks);
			getPropertyValueMaterialList(plugin, object, "alwaysPickupMaterials", sender, mAlwaysPickupMats);
			getPropertyValueMaterialList(plugin, object, "namedPickupMaterials", sender, mNamedPickupMats);

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

		out.add("dailyResetEnabled = " + mDailyResetEnabled);
		out.add("joinMessagesEnabled = " + mJoinMessagesEnabled);
		out.add("isTownWorld = " + mIsTownWorld);
		out.add("plotSurvivalMinHeight = " + mPlotSurvivalMinHeight);

		out.add("isSleepingEnabled = " + mIsSleepingEnabled);
		out.add("keepLowTierInventory = " + mKeepLowTierInventory);
		out.add("classSpecializationsEnabled = " + mClassSpecializationsEnabled);
		out.add("auditMessagesEnabled = " + mAuditMessagesEnabled);
		out.add("repairExplosions = " + mRepairExplosions);
		out.add("preventDungeonItemTransfer = " + mPreventDungeonItemTransfer);
		out.add("replaceSpawnerEntities = " + mReplaceSpawnerEntities);
		out.add("infusionsEnabled = " + mInfusionsEnabled);
		out.add("httpStatusPort = " + mHTTPStatusPort);

		out.add("shardName = " + mShardName);

		out.add("unbreakableBlocks = [" + String.join("  ", mUnbreakableBlocks.stream().map((x) -> x.toString()).collect(Collectors.toList())) + "]");
		out.add("alwaysPickupMaterials = [" + String.join("  ", mAlwaysPickupMats.stream().map((x) -> x.toString()).collect(Collectors.toList())) + "]");
		out.add("namedPickupMaterials = [" + String.join("  ", mNamedPickupMats.stream().map((x) -> x.toString()).collect(Collectors.toList())) + "]");

		return out;
	}

	private boolean getPropertyValueBool(Plugin plugin, JsonObject object, String properyName, boolean defaultVal) {
		boolean value = defaultVal;

		JsonElement element = object.get(properyName);
		if (element != null) {
			value = element.getAsBoolean();
		}

		return value;
	}

	private int getPropertyValueInt(Plugin plugin, JsonObject object, String properyName, int defaultVal) {
		int value = defaultVal;

		JsonElement element = object.get(properyName);
		if (element != null) {
			value = element.getAsInt();
		}

		return value;
	}

	private String getPropertyValueString(Plugin plugin, JsonObject object, String properyName, String defaultVal) {
		String value = defaultVal;

		JsonElement element = object.get(properyName);
		if (element != null) {
			value = element.getAsString();
		}

		return value;
	}

	private void getPropertyValueMaterialList(Plugin plugin, JsonObject object, String propertyName, @Nullable CommandSender sender, Set<Material> set) {
		JsonElement element = object.get(propertyName);
		if (element != null) {
			set.clear();

			for (JsonElement iter : element.getAsJsonArray()) {
				try {
					String blockName = iter.getAsString();
					Material mat = Material.getMaterial(blockName);
					if (mat != null) {
						set.add(mat);
					}
				} catch (Exception e) {
					plugin.getLogger().severe("Invalid unbreakableBlocks element at: '" + iter.toString() + "'");
					e.printStackTrace();

					if (sender != null) {
						sender.sendMessage(ChatColor.RED + "Invalid unbreakableBlocks element at: '" + iter.toString() + "'");
						MessagingUtils.sendStackTrace(sender, e);
					}
				}
			}
		}
	}
}
