package com.playmonumenta.plugins;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

public class CustomLogger extends Logger {
	private static @Nullable CustomLogger INSTANCE = null;

	private final Logger mLogger;
	private Level mLevel;

	public CustomLogger(Logger logger, Level level) {
		super(logger.getName(), logger.getResourceBundleName());
		INSTANCE = this;
		mLogger = logger;
		mLevel = level;
	}

	public static CustomLogger getInstance() {
		return Objects.requireNonNull(INSTANCE);
	}

	@Override
	public void setLevel(Level level) {
		mLevel = level;
	}

	@Override
	public Level getLevel() {
		return mLevel;
	}

	@Override
	public boolean isLoggable(Level level) {
		return level.intValue() >= mLevel.intValue();
	}

	@Override
	public void log(Level level, String msg, Throwable thrown) {
		if (isLoggable(level)) {
			mLogger.log(level, msg, thrown);
		}
	}

	@Override
	public void log(Level level, String msg) {
		if (isLoggable(level)) {
			mLogger.log(level, msg);
		}
	}

	@Override
	public void log(LogRecord record) {
		if (isLoggable(record.getLevel())) {
			mLogger.log(record);
		}
	}

	@Override
	public void log(Level level, Throwable thrown, Supplier<String> msgSupplier) {
		if (isLoggable(level)) {
			mLogger.log(level, thrown, msgSupplier);
		}
	}

	@Override
	public void log(Level level, Supplier<String> msgSupplier) {
		if (isLoggable(level)) {
			mLogger.log(level, msgSupplier);
		}
	}

	@Override
	public void log(Level level, String msg, Object param1) {
		if (isLoggable(level)) {
			mLogger.log(level, msg, param1);
		}
	}

	@Override
	public void log(Level level, String msg, Object[] params) {
		if (isLoggable(level)) {
			mLogger.log(level, msg, params);
		}
	}

	@Override
	public void finest(Supplier<String> msg) {
		if (isLoggable(Level.FINEST)) {
			mLogger.info(msg);
		}
	}

	@Override
	public void finest(String msg) {
		if (isLoggable(Level.FINEST)) {
			mLogger.info(msg);
		}
	}

	@Override
	public void finer(Supplier<String> msg) {
		if (isLoggable(Level.FINER)) {
			mLogger.info(msg);
		}
	}

	@Override
	public void finer(String msg) {
		if (isLoggable(Level.FINER)) {
			mLogger.info(msg);
		}
	}

	@Override
	public void fine(Supplier<String> msg) {
		if (isLoggable(Level.FINE)) {
			mLogger.info(msg);
		}
	}

	@Override
	public void fine(String msg) {
		if (isLoggable(Level.FINE)) {
			mLogger.info(msg);
		}
	}

	@Override
	public void info(Supplier<String> msg) {
		if (isLoggable(Level.INFO)) {
			mLogger.info(msg);
		}
	}

	@Override
	public void info(String msg) {
		if (isLoggable(Level.INFO)) {
			mLogger.info(msg);
		}
	}

	@Override
	public void warning(Supplier<String> msg) {
		mLogger.warning(msg);
	}

	@Override
	public void warning(String msg) {
		mLogger.warning(msg);
	}

	@Override
	public void severe(Supplier<String> msg) {
		mLogger.severe(msg);
	}

	@Override
	public void severe(String msg) {
		mLogger.severe(msg);
	}
}
