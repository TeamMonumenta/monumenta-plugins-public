package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.PhasesManagerBoss;
import com.playmonumenta.plugins.bosses.parameters.ParseResult;
import com.playmonumenta.plugins.bosses.parameters.StringReader;
import dev.jorel.commandapi.Tooltip;
import org.bukkit.entity.LivingEntity;

public class CustomTriggerAction implements Action {

	private final String mKeyTrigger;

	private CustomTriggerAction(String key) {
		mKeyTrigger = key;
	}

	@Override public void runAction(LivingEntity boss) {
		PhasesManagerBoss phaseManagerBoss = BossManager.getInstance().getBoss(boss, PhasesManagerBoss.class);
		if (phaseManagerBoss != null) {
			phaseManagerBoss.onCustomTrigger(mKeyTrigger);
		}
	}

	public static ParseResult<Action> fromReader(StringReader reader) {
		if (!reader.advance("(")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "(", "(...)")));
		}

		String key = reader.readString();
		if (key == null || key.isEmpty()) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "KeyTriggerName", "name of this custom trigger"),
				Tooltip.ofString(reader.readSoFar() + "\"KeyTriggerNameWithInvertedCommas\"", "name of this custom trigger")));
		}

		if (!reader.advance(")")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ")", "(...)")));
		}

		return ParseResult.of(new CustomTriggerAction(key));

	}

}
