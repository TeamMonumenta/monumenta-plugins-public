package com.playmonumenta.plugins.server.properties;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;

public class ServerProperties {
	private static final String FILE_NAME = "Properties.json";

	/* Only the most recent instance of this is used */
	private static ServerProperties INSTANCE = null;

	private boolean mDailyResetEnabled = false;
	private boolean mJoinMessagesEnabled = false;
	private boolean mIsTownWorld = false;
	private boolean mBroadcastCommandEnabled = true;
	// Height of plots in Sierhaven so that players under plots stay in adventure
	private int mPlotSurvivalMinHeight = 256;

	private boolean mIsSleepingEnabled = true;
	private boolean mKeepLowTierInventory = false;
	private boolean mClassSpecializationsEnabled = false;
	private boolean mAuditMessagesEnabled = true;
	private boolean mRepairExplosions = false;
	private int mHTTPStatusPort = 8000;

	private String mShardName = "default_settings";
	private String mRabbitHost = "rabbitmq";

	private Set<String> mForbiddenItemLore = new HashSet<>();

	private EnumSet<Material> mUnbreakableBlocks = EnumSet.noneOf(Material.class);
	private EnumSet<Material> mAlwaysPickupMats = EnumSet.noneOf(Material.class);
	private EnumSet<Material> mNamedPickupMats = EnumSet.noneOf(Material.class);

	public ServerProperties() {
		INSTANCE = this;
	}

	/*
	 * Ensures that INSTANCE is non null
	 * If it is null, creates a default instance with default values
	 */
	private static void ensureInstance() {
		if (INSTANCE == null) {
			new ServerProperties();
		}
	}

	public static boolean getDailyResetEnabled() {
		ensureInstance();
		return INSTANCE.mDailyResetEnabled;
	}

	public static boolean getJoinMessagesEnabled() {
		ensureInstance();
		return INSTANCE.mJoinMessagesEnabled;
	}

	public static boolean getIsTownWorld() {
		ensureInstance();
		return INSTANCE.mIsTownWorld;
	}

	public static boolean getBroadcastCommandEnabled() {
		ensureInstance();
		return INSTANCE.mBroadcastCommandEnabled;
	}

	public static int getPlotSurvivalMinHeight() {
		ensureInstance();
		return INSTANCE.mPlotSurvivalMinHeight;
	}

	public static boolean getIsSleepingEnabled() {
		ensureInstance();
		return INSTANCE.mIsSleepingEnabled;
	}

	public static boolean getKeepLowTierInventory() {
		ensureInstance();
		return INSTANCE.mKeepLowTierInventory;
	}

	public static boolean getClassSpecializationsEnabled() {
		ensureInstance();
		return INSTANCE.mClassSpecializationsEnabled;
	}

	public static boolean getAuditMessagesEnabled() {
		ensureInstance();
		return INSTANCE.mAuditMessagesEnabled;
	}

	public static boolean getRepairExplosions() {
		ensureInstance();
		return INSTANCE.mRepairExplosions;
	}

	public static int getHTTPStatusPort() {
		ensureInstance();
		return INSTANCE.mHTTPStatusPort;
	}

	public static String getShardName() {
		ensureInstance();
		return INSTANCE.mShardName;
	}

	public static String getRabbitHost() {
		ensureInstance();
		return INSTANCE.mRabbitHost;
	}

	public static Set<String> getForbiddenItemLore() {
		ensureInstance();
		return INSTANCE.mForbiddenItemLore;
	}

	public static Set<Material> getUnbreakableBlocks() {
		ensureInstance();
		return INSTANCE.mUnbreakableBlocks;
	}

	public static Set<Material> getAlwaysPickupMats() {
		ensureInstance();
		return INSTANCE.mAlwaysPickupMats;
	}

	public static Set<Material> getNamedPickupMats() {
		ensureInstance();
		return INSTANCE.mNamedPickupMats;
	}

	public static void load(Plugin plugin, CommandSender sender) {
		ensureInstance();
		INSTANCE.loadInternal(plugin, sender);
	}

	private void loadInternal(Plugin plugin, CommandSender sender) {
		final String fileLocation = plugin.getDataFolder() + File.separator + FILE_NAME;

		try {
			String content = FileUtils.readFile(fileLocation);
			if (content != null && content != "") {
				loadFromString(plugin, content, sender);
			}
		} catch (FileNotFoundException e) {
			plugin.getLogger().info("Properties.json file does not exist - using default values");
		} catch (Exception e) {
			plugin.getLogger().severe("Caught exception: " + e);
			e.printStackTrace();

			if (sender != null) {
				sender.sendMessage(ChatColor.RED + "Properties.json file does not exist - using default values");
				MessagingUtils.sendStackTrace(sender, e);
			}
		}
	}

	private void loadFromString(Plugin plugin, String content, CommandSender sender) throws Exception {
		if (content != null && content != "") {
			try {
				Gson gson = new Gson();

				//  Load the file - if it exists, then let's start parsing it.
				JsonObject object = gson.fromJson(content, JsonObject.class);
				if (object != null) {
					mDailyResetEnabled           = getPropertyValueBool(plugin, object, "dailyResetEnabled", mDailyResetEnabled);
					mJoinMessagesEnabled         = getPropertyValueBool(plugin, object, "joinMessagesEnabled", mJoinMessagesEnabled);
					mIsTownWorld                 = getPropertyValueBool(plugin, object, "isTownWorld", mIsTownWorld);
					mBroadcastCommandEnabled     = getPropertyValueBool(plugin, object, "broadcastCommandEnabled", mBroadcastCommandEnabled);
					mPlotSurvivalMinHeight       = getPropertyValueInt(plugin, object, "plotSurvivalMinHeight", mPlotSurvivalMinHeight);

					mIsSleepingEnabled           = getPropertyValueBool(plugin, object, "isSleepingEnabled", mIsSleepingEnabled);
					mKeepLowTierInventory        = getPropertyValueBool(plugin, object, "keepLowTierInventory", mKeepLowTierInventory);
					mClassSpecializationsEnabled = getPropertyValueBool(plugin, object, "classSpecializationsEnabled", mClassSpecializationsEnabled);
					mAuditMessagesEnabled        = getPropertyValueBool(plugin, object, "auditMessagesEnabled", mAuditMessagesEnabled);
					mRepairExplosions            = getPropertyValueBool(plugin, object, "repairExplosions", mRepairExplosions);
					mHTTPStatusPort              = getPropertyValueInt(plugin, object, "httpStatusPort", mHTTPStatusPort);

					mShardName                   = getPropertyValueString(plugin, object, "shardName", mShardName);
					mRabbitHost                  = getPropertyValueString(plugin, object, "rabbitHost", mRabbitHost);

					mForbiddenItemLore           = getPropertyValueStringSet(plugin, object, "forbiddenItemLore");

					mUnbreakableBlocks           = getPropertyValueMaterialList(plugin, object, "unbreakableBlocks", sender);
					mAlwaysPickupMats            = getPropertyValueMaterialList(plugin, object, "alwaysPickupMaterials", sender);
					mNamedPickupMats             = getPropertyValueMaterialList(plugin, object, "namedPickupMaterials", sender);

					if (sender != null) {
						sender.sendMessage(ChatColor.GOLD + "Successfully reloaded monumenta configuration");
					}
				}
			} catch (Exception e) {
				plugin.getLogger().severe("Caught exception: " + e);
				e.printStackTrace();

				if (sender != null) {
					sender.sendMessage(ChatColor.RED + "Failed to load configuration!");
					MessagingUtils.sendStackTrace(sender, e);
				}
			}
		}
	}

	private boolean getPropertyValueBool(Plugin plugin, JsonObject object, String properyName, boolean defaultVal) {
		boolean value = defaultVal;

		JsonElement element = object.get(properyName);
		if (element != null) {
			value = element.getAsBoolean();
		}

		plugin.getLogger().info("Properties: " + properyName + " = " + value);

		return value;
	}

	private int getPropertyValueInt(Plugin plugin, JsonObject object, String properyName, int defaultVal) {
		int value = defaultVal;

		JsonElement element = object.get(properyName);
		if (element != null) {
			value = element.getAsInt();
		}

		plugin.getLogger().info("Properties: " + properyName + " = " + value);

		return value;
	}

	private String getPropertyValueString(Plugin plugin, JsonObject object, String properyName, String defaultVal) {
		String value = defaultVal;

		JsonElement element = object.get(properyName);
		if (element != null) {
			value = element.getAsString();
		}

		plugin.getLogger().info("Properties: " + properyName + " = " + value);

		return value;
	}

	private Set<String> getPropertyValueStringSet(Plugin plugin, JsonObject object, String properyName) {
		Set<String> value = new HashSet<>();

		JsonElement element = object.get(properyName);
		if (element != null) {
			Iterator<JsonElement> targetIter = element.getAsJsonArray().iterator();
			while (targetIter.hasNext()) {
				value.add(targetIter.next().getAsString());
			}
		}

		if (value.isEmpty()) {
			plugin.getLogger().info("Properties: " + properyName + " = <all>");
		} else {
			plugin.getLogger().info("Properties: " + properyName + " = " + value.toString());
		}

		return value;
	}

	private EnumSet<Material> getPropertyValueMaterialList(Plugin plugin, JsonObject object, String propertyName, CommandSender sender) {
		EnumSet<Material> value = EnumSet.noneOf(Material.class);

		JsonElement element = object.get(propertyName);
		if (element != null) {
			Iterator<JsonElement> targetIter = element.getAsJsonArray().iterator();
			while (targetIter.hasNext()) {
				JsonElement iter = targetIter.next();
				try {
					String blockName = iter.getAsString();
					Material mat = Material.getMaterial(blockName);
					if (mat != null) {
						value.add(mat);
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

		if (value.isEmpty()) {
			plugin.getLogger().info("Properties: " + propertyName + " = []");
		} else {
			plugin.getLogger().info("Properties: " + propertyName + " = " + value.toString());
		}

		return value;
	}
}
