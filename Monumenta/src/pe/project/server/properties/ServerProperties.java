package pe.project.server.properties;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.project.Main;
import pe.project.utils.FileUtils;

public class ServerProperties {
	private final static String FILE_NAME = "Properties.json";

	private boolean mDailyResetEnabled = false;
	private boolean mJoinMessagesEnabled = false;
	private boolean mTransferDataEnabled = true;
	private boolean mIsTownWorld = false;
	private boolean mBroadcastCommandEnabled = true;
	// Height of plots in Sierhaven so that players under plots stay in adventure
	private int mPlotSurvivalMinHeight = 256;
	public Set<String> mAllowedTransferTargets = new HashSet<>();
	private boolean mQuestCompassEnabled = true;

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

	public boolean getQuestCompassEnabled() {
		return mQuestCompassEnabled;
	}

	public void load(Main main) {
		final String fileLocation = main.getDataFolder() + File.separator + FILE_NAME;

		try {
			String content = FileUtils.readFile(fileLocation);
			if (content != null && content != "") {
				_loadFromString(main, content);
			}
		} catch (FileNotFoundException e) {
			main.getLogger().info("Properties.json file does not exist - using default values" + e);
		} catch (Exception e) {
			main.getLogger().severe("Caught exception: " + e);
			e.printStackTrace();
		}
	}

	private void _loadFromString(Main main, String content) throws Exception {
		if (content != null && content != "") {
			try {
				Gson gson = new Gson();

				//	Load the file - if it exists, then let's start parsing it.
				JsonObject object = gson.fromJson(content, JsonObject.class);
				if (object != null) {
					mDailyResetEnabled			= _getPropertyValueBool(main, object, "dailyResetEnabled", mDailyResetEnabled);
					mJoinMessagesEnabled		= _getPropertyValueBool(main, object, "joinMessagesEnabled", mJoinMessagesEnabled);
					mTransferDataEnabled		= _getPropertyValueBool(main, object, "transferDataEnabled", mTransferDataEnabled);
					mIsTownWorld				= _getPropertyValueBool(main, object, "isTownWorld", mIsTownWorld);
					mBroadcastCommandEnabled	= _getPropertyValueBool(main, object, "broadcastCommandEnabled", mBroadcastCommandEnabled);
					mPlotSurvivalMinHeight		= _getPropertyValueInt(main, object, "plotSurvivalMinHeight", mPlotSurvivalMinHeight);
					mAllowedTransferTargets		= _getPropertyValueStringSet(main, object, "allowedTransferTargets");
					mQuestCompassEnabled		= _getPropertyValueBool(main, object, "questCompassEnabled", mQuestCompassEnabled);
				}
			} catch (Exception e) {
				main.getLogger().severe("Caught exception: " + e);
				e.printStackTrace();
			}
		}
	}

	private boolean _getPropertyValueBool(Main plugin, JsonObject object, String properyName, boolean defaultVal) {
		boolean value = defaultVal;

		JsonElement element = object.get(properyName);
		if (element != null) {
			value = element.getAsBoolean();
		}

		plugin.getLogger().info("Properties: " + properyName + " = " + value);

		return value;
	}

	private int _getPropertyValueInt(Main plugin, JsonObject object, String properyName, int defaultVal) {
		int value = defaultVal;

		JsonElement element = object.get(properyName);
		if (element != null) {
			value = element.getAsInt();
		}

		plugin.getLogger().info("Properties: " + properyName + " = " + value);

		return value;
	}

	private Set<String> _getPropertyValueStringSet(Main plugin, JsonObject object, String properyName) {
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
}
