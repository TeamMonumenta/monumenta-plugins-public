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

	private boolean mDailyResetEnabled = false;
	private boolean mJoinMessagesEnabled = false;
	private boolean mTransferDataEnabled = true;
	private boolean mIsTownWorld = false;
	private boolean mBroadcastCommandEnabled = true;
	// Height of plots in Sierhaven so that players under plots stay in adventure
	private int mPlotSurvivalMinHeight = 256;
	private int mSocketPort = 9576;

	private boolean mIsSleepingEnabled = true;
	private boolean mKeepLowTierInventory = false;
	private boolean mClassSpecializationsEnabled = false;
	private boolean mAuditMessagesEnabled = true;

	private String mShardName = "default_settings";
	private String mSocketHost = "bungee";

	public Set<String> mAllowedTransferTargets = new HashSet<>();
	public Set<String> mForbiddenItemLore = new HashSet<>();

	private EnumSet<Material> mUnbreakableBlocks = EnumSet.noneOf(Material.class);
	private EnumSet<Material> mAlwaysPickupMats = EnumSet.noneOf(Material.class);
	private EnumSet<Material> mNamedPickupMats = EnumSet.noneOf(Material.class);

	public boolean getDailyResetEnabled() {
		return mDailyResetEnabled;
	}

	public boolean getJoinMessagesEnabled() {
		return mJoinMessagesEnabled;
	}

	public boolean getTransferDataEnabled() {
		return mTransferDataEnabled;
	}

	public boolean getIsTownWorld() {
		return mIsTownWorld;
	}

	public boolean getBroadcastCommandEnabled() {
		return mBroadcastCommandEnabled;
	}

	public int getPlotSurvivalMinHeight() {
		return mPlotSurvivalMinHeight;
	}

	public int getSocketPort() {
		return mSocketPort;
	}

	public boolean getIsSleepingEnabled() {
		return mIsSleepingEnabled;
	}

	public boolean getKeepLowTierInventory() {
		return mKeepLowTierInventory;
	}

	public boolean getClassSpecializationsEnabled() {
		return mClassSpecializationsEnabled;
	}

	public boolean getAuditMessagesEnabled() {
		return mAuditMessagesEnabled;
	}

	public String getShardName() {
		return mShardName;
	}

	public String getSocketHost() {
		return mSocketHost;
	}

	public Set<Material> getUnbreakableBlocks() {
		return mUnbreakableBlocks;
	}

	public Set<Material> getAlwaysPickupMats() {
		return mAlwaysPickupMats;
	}

	public Set<Material> getNamedPickupMats() {
		return mNamedPickupMats;
	}

	public void load(Plugin plugin, CommandSender sender) {
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
					mTransferDataEnabled         = getPropertyValueBool(plugin, object, "transferDataEnabled", mTransferDataEnabled);
					mIsTownWorld                 = getPropertyValueBool(plugin, object, "isTownWorld", mIsTownWorld);
					mBroadcastCommandEnabled     = getPropertyValueBool(plugin, object, "broadcastCommandEnabled", mBroadcastCommandEnabled);
					mPlotSurvivalMinHeight       = getPropertyValueInt(plugin, object, "plotSurvivalMinHeight", mPlotSurvivalMinHeight);
					mSocketPort                  = getPropertyValueInt(plugin, object, "socketPort", mSocketPort);

					mIsSleepingEnabled           = getPropertyValueBool(plugin, object, "isSleepingEnabled", mIsSleepingEnabled);
					mKeepLowTierInventory        = getPropertyValueBool(plugin, object, "keepLowTierInventory", mKeepLowTierInventory);
					mClassSpecializationsEnabled = getPropertyValueBool(plugin, object, "classSpecializationsEnabled", mClassSpecializationsEnabled);
					mAuditMessagesEnabled        = getPropertyValueBool(plugin, object, "auditMessagesEnabled", mAuditMessagesEnabled);

					mShardName                   = getPropertyValueString(plugin, object, "shardName", mShardName);
					mSocketHost                  = getPropertyValueString(plugin, object, "socketHost", mSocketHost);

					mAllowedTransferTargets      = getPropertyValueStringSet(plugin, object, "allowedTransferTargets");
					mForbiddenItemLore           = getPropertyValueStringSet(plugin, object, "forbiddenItemLore");

					mUnbreakableBlocks           = getPropertyValueMaterialList(plugin, object, "unbreakableBlocks", sender);
					mAlwaysPickupMats            = getPropertyValueMaterialList(plugin, object, "alwaysPickupMaterials", sender);
					mNamedPickupMats             = getPropertyValueMaterialList(plugin, object, "namedPickupMaterials", sender);

					plugin.mSafeZoneManager.reload(object.get("locationBounds"), sender);

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
