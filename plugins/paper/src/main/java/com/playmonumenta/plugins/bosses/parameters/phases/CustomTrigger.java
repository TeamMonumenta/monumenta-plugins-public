package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.bosses.parameters.ParseResult;
import com.playmonumenta.plugins.bosses.parameters.StringReader;
import dev.jorel.commandapi.Tooltip;
import org.bukkit.entity.LivingEntity;

public class CustomTrigger extends Trigger {

	private final String mKeyString;
	private final boolean mOneTime;
	private boolean mHasTriggedOnce = false;

	private CustomTrigger(String key, boolean oneTime) {
		mKeyString = key;
		mOneTime = oneTime;
	}

	@Override public boolean test(LivingEntity boss) {
		return mHasTriggedOnce;
	}

	@Override public void reset(LivingEntity boss) {
		mHasTriggedOnce = false;
	}

	@Override public boolean custom(LivingEntity boss, String key) {
		if (key.equals(mKeyString)) {
			if (!mOneTime) {
				mHasTriggedOnce = true;
			}
			return true;
		}
		return false;
	}

	public static ParseResult<Trigger> fromReader(StringReader reader) {
		if (!reader.advance("(")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "(", "(...)")));
		}

		String key = reader.readString();
		if (key == null || key.isEmpty()) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "KeyTriggerName", "name of this custom trigger"),
												Tooltip.of(reader.readSoFar() + "\"KeyTriggerNameWithInvertedCommas\"", "name of this custom trigger")));
		}

		if (!reader.advance(",")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + ",", ",")));
		}

		Boolean onlyOnce = reader.readBoolean();
		if (onlyOnce == null) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "true", "if this trigger should return true when running test() after custom() return true"),
				Tooltip.of(reader.readSoFar() + "false", "if this trigger should return false when running test() after custom() return true")));
		}


		if (!reader.advance(")")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + ")", "(...)")));
		}

		return ParseResult.of(new CustomTrigger(key, onlyOnce));
	}

}
