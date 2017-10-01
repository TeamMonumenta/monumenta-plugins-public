package pe.project.server.properties;

import java.io.File;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import pe.project.Main;
import pe.project.utils.FileUtils;

public class ServerProperties {
	private final static String FILE_NAME = "Properties.json";
	//private boolean mRandomVariable;	//	SAMPLE VARIABLE
	
	/*public boolean getRandomVariable() {	//	SAMPLE GETTER
		return mRandomVariable;
	}*/
	
	public void load(Main plugin) {
		final String fileLocation = plugin.getDataFolder() + File.separator + FILE_NAME;

		try {
			String content = FileUtils.getCreateFile(fileLocation);
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
					/*//	Load random variable.	//	SAMPLE VARIABLE PARSING
					JsonElement randomVariable = object.get("random_variable");
					if (randomVariable != null) {
						mRandomVariable = randomVariable.getAsBoolean();
					}*/
				}
			} catch (Exception e) {
				main.getLogger().severe("Caught exception: " + e);
				e.printStackTrace();
			}
		}
	}
}
