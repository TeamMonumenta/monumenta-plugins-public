package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.parameters.ParseResult;
import com.playmonumenta.plugins.bosses.parameters.StringReader;
import com.playmonumenta.plugins.utils.MMLog;
import dev.jorel.commandapi.Tooltip;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.LivingEntity;

public class AddAbilityAction implements Action {

	private final String mAbility;

	public AddAbilityAction(String ability) {
		mAbility = ability;
	}

	@Override public void runAction(LivingEntity boss) {
		try {
			BossManager.createBoss(null, boss, mAbility);
		} catch (Exception e) {
			MMLog.warning("[BossTriggerAction] AddAbilityAction | exception while creating ability for boss: " + boss.getName() + " ability: " + mAbility + " reason: " + e.getMessage());
		}
	}


	public static ParseResult<Action> fromReader(StringReader reader) {
		if (!reader.advance("(")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "(", "(...)")));
		}

		String ability = reader.readOneOf(List.of(BossManager.getInstance().listStatelessBosses()));

		if (ability == null) {
			List<Tooltip<String>> suggArgs = new ArrayList<>(BossManager.getInstance().listStatelessBosses().length);
			String soFar = reader.readSoFar();
			for (String valid : BossManager.getInstance().listStatelessBosses()) {
				suggArgs.add(Tooltip.ofString(soFar + valid, "boss ability"));
			}
			return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
		}

		if (!reader.advance(")")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ")", "(...)")));
		}

		return ParseResult.of(new AddAbilityAction(ability));

	}
}
