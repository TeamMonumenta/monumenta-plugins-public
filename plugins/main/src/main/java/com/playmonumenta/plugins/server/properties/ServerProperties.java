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
	private final static String FILE_NAME = "Properties.json";

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

	private String mShardName = "default_settings";

	public Set<String> mAllowedTransferTargets = new HashSet<>();
	public Set<String> mForbiddenItemLore = new HashSet<>();

	public EnumSet<Material> mUnbreakableBlocks = EnumSet.noneOf(Material.class);

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

	public String getShardName() {
		return mShardName;
	}

	public void load(Plugin plugin, CommandSender sender) {
		final String fileLocation = plugin.getDataFolder() + File.separator + FILE_NAME;

		try {
			String content = FileUtils.readFile(fileLocation);
			if (content != null && content != "") {
				_loadFromString(plugin, content, sender);
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

	private void _loadFromString(Plugin plugin, String content, CommandSender sender) throws Exception {
		if (content != null && content != "") {
			try {
				Gson gson = new Gson();

				//  Load the file - if it exists, then let's start parsing it.
				JsonObject object = gson.fromJson(content, JsonObject.class);
				if (object != null) {
					mDailyResetEnabled           = _getPropertyValueBool(plugin, object, "dailyResetEnabled", mDailyResetEnabled);
					mJoinMessagesEnabled         = _getPropertyValueBool(plugin, object, "joinMessagesEnabled", mJoinMessagesEnabled);
					mTransferDataEnabled         = _getPropertyValueBool(plugin, object, "transferDataEnabled", mTransferDataEnabled);
					mIsTownWorld                 = _getPropertyValueBool(plugin, object, "isTownWorld", mIsTownWorld);
					mBroadcastCommandEnabled     = _getPropertyValueBool(plugin, object, "broadcastCommandEnabled", mBroadcastCommandEnabled);
					mPlotSurvivalMinHeight       = _getPropertyValueInt(plugin, object, "plotSurvivalMinHeight", mPlotSurvivalMinHeight);
					mSocketPort                  = _getPropertyValueInt(plugin, object, "socketPort", mSocketPort);

					mIsSleepingEnabled           = _getPropertyValueBool(plugin, object, "isSleepingEnabled", mIsSleepingEnabled);
					mKeepLowTierInventory        = _getPropertyValueBool(plugin, object, "keepLowTierInventory", mKeepLowTierInventory);
					mClassSpecializationsEnabled = _getPropertyValueBool(plugin, object, "classSpecializationsEnabled", mClassSpecializationsEnabled);

					mShardName                   = _getPropertyValueString(plugin, object, "shardName", mShardName);

					mAllowedTransferTargets      = _getPropertyValueStringSet(plugin, object, "allowedTransferTargets");
					mForbiddenItemLore           = _getPropertyValueStringSet(plugin, object, "forbiddenItemLore");

					mUnbreakableBlocks           = _getPropertyValueMaterialList(plugin, object, "unbreakableBlocks", sender);

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

	private boolean _getPropertyValueBool(Plugin plugin, JsonObject object, String properyName, boolean defaultVal) {
		boolean value = defaultVal;

		JsonElement element = object.get(properyName);
		if (element != null) {
			value = element.getAsBoolean();
		}

		plugin.getLogger().info("Properties: " + properyName + " = " + value);

		return value;
	}

	private int _getPropertyValueInt(Plugin plugin, JsonObject object, String properyName, int defaultVal) {
		int value = defaultVal;

		JsonElement element = object.get(properyName);
		if (element != null) {
			value = element.getAsInt();
		}

		plugin.getLogger().info("Properties: " + properyName + " = " + value);

		return value;
	}

	private String _getPropertyValueString(Plugin plugin, JsonObject object, String properyName, String defaultVal) {
		String value = defaultVal;

		JsonElement element = object.get(properyName);
		if (element != null) {
			value = element.getAsString();
		}

		plugin.getLogger().info("Properties: " + properyName + " = " + value);

		return value;
	}

	private Set<String> _getPropertyValueStringSet(Plugin plugin, JsonObject object, String properyName) {
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

	private EnumSet<Material> _getPropertyValueMaterialList(Plugin plugin, JsonObject object, String propertyName, CommandSender sender) {
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
