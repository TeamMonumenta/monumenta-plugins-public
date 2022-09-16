package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.CustomLogger;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MMLog {
	public static void setLevel(Level level) {
		Logger logger = CustomLogger.getInstance();
		if (logger != null) {
			logger.setLevel(level);
		}
	}

	public static boolean isLevelEnabled(Level level) {
		Logger logger = CustomLogger.getInstance();
		if (logger != null) {
			return level.intValue() >= logger.getLevel().intValue();
		}
		return true;
	}

	public static void finest(Supplier<String> msg) {
		Logger logger = CustomLogger.getInstance();
		if (logger != null) {
			logger.finest(msg);
		}
	}

	public static void finest(String msg) {
		Logger logger = CustomLogger.getInstance();
		if (logger != null) {
			logger.finest(msg);
		}
	}

	public static void finer(Supplier<String> msg) {
		Logger logger = CustomLogger.getInstance();
		if (logger != null) {
			logger.finer(msg);
		}
	}

	public static void finer(String msg) {
		Logger logger = CustomLogger.getInstance();
		if (logger != null) {
			logger.finer(msg);
		}
	}

	public static void fine(Supplier<String> msg) {
		Logger logger = CustomLogger.getInstance();
		if (logger != null) {
			logger.fine(msg);
		}
	}

	public static void fine(String msg) {
		Logger logger = CustomLogger.getInstance();
		if (logger != null) {
			logger.fine(msg);
		}
	}

	public static void info(String msg) {
		Logger logger = CustomLogger.getInstance();
		if (logger != null) {
			logger.info(msg);
		}
	}

	public static void info(Supplier<String> msg) {
		Logger logger = CustomLogger.getInstance();
		if (logger != null) {
			logger.info(msg);
		}
	}

	public static void warning(Supplier<String> msg) {
		Logger logger = CustomLogger.getInstance();
		if (logger != null) {
			logger.warning(msg);
		}
	}

	public static void warning(String msg) {
		Logger logger = CustomLogger.getInstance();
		if (logger != null) {
			logger.warning(msg);
		}
	}

	public static void warning(String msg, Throwable throwable) {
		Logger logger = CustomLogger.getInstance();
		if (logger != null) {
			logger.log(Level.WARNING, msg, throwable);
		}
	}

	public static void severe(Supplier<String> msg) {
		Logger logger = CustomLogger.getInstance();
		if (logger != null) {
			logger.severe(msg);
		}
	}

	public static void severe(String msg) {
		Logger logger = CustomLogger.getInstance();
		if (logger != null) {
			logger.severe(msg);
		}
	}

	public static void severe(String msg, Throwable throwable) {
		Logger logger = CustomLogger.getInstance();
		if (logger != null) {
			logger.log(Level.SEVERE, msg, throwable);
		}
	}
}
