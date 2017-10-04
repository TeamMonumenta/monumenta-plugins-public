package pe.project.server.properties;

import java.io.File;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;

import pe.project.Main;
import pe.project.utils.FileUtils;

public class ServerProperties {
	private final static String FILE_NAME = "Properties.json";

	private boolean mDailyResetEnabled = false;
	private boolean mJoinMessagesEnabled = false;

	public boolean getDailyResetEnabled() {
		return mDailyResetEnabled;
	}

	public boolean getJoinMessagesEnabled() {
		return mJoinMessagesEnabled;
	}

	public void load(Main plugin) {
		final String fileLocation = plugin.getDataFolder() + File.separator + FILE_NAME;

		try {
			String content = FileUtils.readFile(fileLocation);
			if (content != null && content != "") {
				_loadFromString(plugin, content);
			}
		} catch (Exception e) {
			plugin.getLogger().severe("Caught exception: " + e);
			e.printStackTrace();
		}
	}

	private void _loadFromString(Main main, String content) throws Exception {
		if (content != null && content != "") {
			try {
				Gson gson = new Gson();

				//	Load the file, if it exist than let's start parsing it.
				JsonObject object = gson.fromJson(content, JsonObject.class);
				if (object != null) {
					JsonElement dailyResetEnabled = object.get("dailyResetEnabled");
					if (dailyResetEnabled != null) {
						mDailyResetEnabled = dailyResetEnabled.getAsBoolean();
					}

					JsonElement joinMessagesEnabled = object.get("joinMessagesEnabled");
					if (joinMessagesEnabled != null) {
						mJoinMessagesEnabled = joinMessagesEnabled.getAsBoolean();
					}
				}
			} catch (Exception e) {
				main.getLogger().severe("Caught exception: " + e);
				e.printStackTrace();
			}
		}
	}
}
