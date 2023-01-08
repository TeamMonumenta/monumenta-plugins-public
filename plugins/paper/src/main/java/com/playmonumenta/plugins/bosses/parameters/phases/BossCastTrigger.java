package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.parameters.ParseResult;
import com.playmonumenta.plugins.bosses.parameters.StringReader;
import dev.jorel.commandapi.Tooltip;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.LivingEntity;

public class BossCastTrigger extends Trigger {
	//TODO - when we are going to refactor the old boss ability code this trigger need to handle custom names for abilities instead of tags

	private final String mBossAbilityTag;

	private BossCastTrigger(String tag) {
		mBossAbilityTag = tag;
	}

	@Override public boolean onBossCastAbility(LivingEntity boss, SpellCastEvent event) {
		return event.getBossAbilityGroup().getIdentityTag().equals(mBossAbilityTag);
	}

	@Override public boolean test(LivingEntity boss) {
		return false;
	}

	@Override public void reset(LivingEntity boss) {
		//no reset needed
	}


	public static ParseResult<Trigger> fromReader(StringReader reader) {
		if (!reader.advance("(")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "(", "(...)")));
		}

		String bossTag = reader.readOneOf(List.of(BossManager.getInstance().listStatelessBosses()));

		if (bossTag == null) {
			List<Tooltip<String>> suggArgs = new ArrayList<>(BossManager.getInstance().listStatelessBosses().length);
			String soFar = reader.readSoFar();
			for (String name : BossManager.getInstance().listStatelessBosses()) {
				suggArgs.add(Tooltip.ofString(soFar + name, "boss tag"));
			}
			return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
		}

		if (!reader.advance(")")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ")", "(...)")));
		}

		return ParseResult.of(new BossCastTrigger(bossTag));
	}

}
