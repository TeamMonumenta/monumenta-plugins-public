package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.PhasesManagerBoss;
import com.playmonumenta.plugins.bosses.parameters.ParseResult;
import com.playmonumenta.plugins.bosses.parameters.StringReader;
import dev.jorel.commandapi.Tooltip;
import org.bukkit.entity.LivingEntity;

public class FlagSetAction implements Action {

	private final String mKeyTrigger;
	private final boolean mState;

	private FlagSetAction(String key, boolean state) {
		mKeyTrigger = key;
		mState = state;
	}

	@Override public void runAction(LivingEntity boss) {
		PhasesManagerBoss phaseManagerBoss = BossManager.getInstance().getBoss(boss, PhasesManagerBoss.class);
		if (phaseManagerBoss != null) {
			phaseManagerBoss.onFlagTrigger(mKeyTrigger, mState);
		}
	}

	public static ParseResult<Action> fromReader(StringReader reader) {
		if (!reader.advance("(")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "(", "(...)")));
		}

		String key = reader.readString();
		if (key == null || key.isEmpty()) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "KeyTriggerName", "name of the flag to set"),
				Tooltip.ofString(reader.readSoFar() + "\"KeyTriggerNameWithInvertedCommas\"", "name of the flag to set")));
		}

		if (!reader.advance(",")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ",", ",")));
		}

		Boolean setState = reader.readBoolean();
		if (setState == null) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "true", "the value to set the flag to"),
				Tooltip.ofString(reader.readSoFar() + "false", "the value to set the flag to")));
		}

		if (!reader.advance(")")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ")", "(...)")));
		}

		return ParseResult.of(new FlagSetAction(key, setState));

	}

}
