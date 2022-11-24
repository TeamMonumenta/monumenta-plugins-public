package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.bosses.parameters.ParseResult;
import com.playmonumenta.plugins.bosses.parameters.StringReader;
import org.bukkit.entity.LivingEntity;

public interface Action {
	void runAction(LivingEntity boss);

	@FunctionalInterface
	interface ActionBuilder {
		ParseResult<Action> buildAction(StringReader reader);
	}
}
