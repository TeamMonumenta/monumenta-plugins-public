package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.adapters.VersionAdapter;
import java.util.logging.Logger;

public class NmsUtils {
	private static VersionAdapter mVersionAdapter = null;

	public static VersionAdapter getVersionAdapter() {
		return mVersionAdapter;
	}

	public static void loadVersionAdapter(Class<?> serverClass, Logger logger) {
		/* From https://github.com/mbax/AbstractionExamplePlugin */

		String packageName = serverClass.getPackage().getName();
		String version = packageName.substring(packageName.lastIndexOf('.') + 1);

		try {
			Class<?> clazz = Class.forName("com.playmonumenta.plugins.adapters.VersionAdapter_" + version);
			// Check if we have a valid adapter class at that location.
			if (VersionAdapter.class.isAssignableFrom(clazz)) {
				mVersionAdapter = (VersionAdapter) clazz.getConstructor().newInstance();
				logger.info("Loaded NMS adapter for " + version);
			} else {
				logger.severe("Somehow VersionAdapter is not assignable from " + clazz + ". NMS utilities will fail and throw NullPointerException's");
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.severe("Server version " + version + " is not supported!");
			logger.severe("Everything that relies on version-specific 'NMS' logic will behave incorrectly");
			try {
				Class<?> clazz = Class.forName("com.playmonumenta.plugins.adapters.VersionAdapter_unsupported");
				// Check if we have a valid adapter class at that location.
				if (VersionAdapter.class.isAssignableFrom(clazz)) {
					mVersionAdapter = (VersionAdapter) clazz.getConstructor().newInstance();
				}
				logger.severe("Loaded 'unsupported' version adapter, which should at least reduce the number of null pointer exceptions");
			} catch (Exception ex) {
				ex.printStackTrace();
				logger.severe("Also failed to load generic unsupported adapter. There will be many null pointer exceptions.");
			}
		}
	}
}
