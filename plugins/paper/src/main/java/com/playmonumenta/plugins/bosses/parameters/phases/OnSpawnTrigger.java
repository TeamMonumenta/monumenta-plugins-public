package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.bosses.parameters.ParseResult;
import com.playmonumenta.plugins.bosses.parameters.StringReader;
import org.bukkit.entity.LivingEntity;

public class OnSpawnTrigger extends Trigger {

	@Override public boolean onSpawn(LivingEntity boss) {
		return true;
	}

	@Override public boolean test(LivingEntity boss) {
		return false;
	}

	@Override public void reset(LivingEntity boss) {

	}

	public static ParseResult<Trigger> fromReader(StringReader reader) {
		return ParseResult.of(new OnSpawnTrigger());
	}
}
