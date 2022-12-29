package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.parameters.ParseResult;
import com.playmonumenta.plugins.bosses.parameters.StringReader;
import dev.jorel.commandapi.Tooltip;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.LivingEntity;

public class ForceCastAction implements Action {

	private final String mAbility;

	public ForceCastAction(String ability) {
		mAbility = ability;
	}


	@Override public void runAction(LivingEntity boss) {
		BossManager manager = BossManager.getInstance();
		if (manager != null) {
			List<BossAbilityGroup> abilityList = manager.getAbilities(boss);
			for (BossAbilityGroup ability : abilityList) {
				if (ability.getIdentityTag().equals(mAbility)) {
					ability.forceCastRandomSpell();
				}
			}
		}
	}

	public static ParseResult<Action> fromReader(StringReader reader) {
		if (!reader.advance("(")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "(", "(...)")));
		}

		String ability = reader.readOneOf(List.of(BossManager.getInstance().listStatelessBosses()));

		if (ability == null) {
			List<Tooltip<String>> suggArgs = new ArrayList<>(BossManager.getInstance().listStatelessBosses().length);
			String soFar = reader.readSoFar();
			for (String valid : BossManager.getInstance().listStatelessBosses()) {
				suggArgs.add(Tooltip.of(soFar + valid, "boss ability"));
			}
			return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
		}

		if (!reader.advance(")")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + ")", "(...)")));
		}

		return ParseResult.of(new ForceCastAction(ability));

	}

}
