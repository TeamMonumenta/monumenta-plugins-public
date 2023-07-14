package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.bosses.parameters.ParseResult;
import com.playmonumenta.plugins.bosses.parameters.StringReader;
import dev.jorel.commandapi.Tooltip;
import org.bukkit.entity.LivingEntity;

public class FlagTrigger extends Trigger {

	private final String mKeyString;
	private boolean mState;

	private FlagTrigger(String key, boolean startingState) {
		mKeyString = key;
		mState = startingState;
	}

	@Override public boolean test(LivingEntity boss) {
		return mState;
	}

	@Override public void reset(LivingEntity boss) {

	}

	@Override public boolean tick(LivingEntity boss, int ticks) {
		return mState;
	}

	@Override public boolean flag(LivingEntity boss, String key, boolean state) {
		if (key.equals(mKeyString)) {
			mState = state;
			return true;
		}
		return false;
	}

	public static ParseResult<Trigger> fromReader(StringReader reader) {
		if (!reader.advance("(")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "(", "(...)")));
		}

		String key = reader.readString();
		if (key == null || key.isEmpty()) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "KeyTriggerName", "name of this flag"),
				Tooltip.ofString(reader.readSoFar() + "\"KeyTriggerNameWithInvertedCommas\"", "name of this flag")));
		}

		if (!reader.advance(",")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ",", ",")));
		}

		Boolean startingState = reader.readBoolean();
		if (startingState == null) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "true", "starting value of the flag"),
				Tooltip.ofString(reader.readSoFar() + "false", "starting value of the flag")));
		}


		if (!reader.advance(")")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ")", "(...)")));
		}

		return ParseResult.of(new FlagTrigger(key, startingState));
	}

}
