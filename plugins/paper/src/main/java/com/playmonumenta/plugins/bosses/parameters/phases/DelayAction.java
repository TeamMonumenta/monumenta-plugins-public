package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.ParseResult;
import com.playmonumenta.plugins.bosses.parameters.StringReader;
import dev.jorel.commandapi.Tooltip;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

public class DelayAction implements Action {

	private final Action mAction;
	private final int mDelay;

	private DelayAction(int delay, Action action) {
		mAction = action;
		mDelay = delay;
	}


	@Override public void runAction(LivingEntity boss) {
		Bukkit.getScheduler().runTaskLater(
			Plugin.getInstance(),
			() -> {
				mAction.runAction(boss);
			},
			mDelay);
	}


	public static ParseResult<Action> fromReader(StringReader reader) {
		if (!reader.advance("(")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "(", "(..)")));
		}

		Long delay = reader.readLong();
		if (delay == null || delay < 0) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "20", "ticks of delay")));
		}

		if (!reader.advance(",")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + ",", ".-.")));
		}

		String name = reader.readOneOf(Phase.ACTION_BUILDER_MAP.keySet().stream().filter(str -> !str.equals("DELAY_ACTION")).toList());
		if (name == null) {
			List<Tooltip<String>> suggArgs = new ArrayList<>(Phase.ACTION_BUILDER_MAP.keySet().size());
			String soFar = reader.readSoFar();
			for (String valid : Phase.ACTION_BUILDER_MAP.keySet()) {
				suggArgs.add(Tooltip.of(soFar + valid, "action builder"));
			}
			return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
		}

		ParseResult<Action> actionParseResult = Objects.requireNonNull(Phase.ACTION_BUILDER_MAP.get(name)).buildAction(reader);

		if (actionParseResult.getResult() == null) {
			return actionParseResult;
		}

		Action action = actionParseResult.getResult();


		if (!reader.advance(")")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + ")", "(..)")));
		}

		return ParseResult.of(new DelayAction(delay.intValue(), action));
	}

}
