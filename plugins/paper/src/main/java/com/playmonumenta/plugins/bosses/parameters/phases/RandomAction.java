package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.bosses.parameters.ParseResult;
import com.playmonumenta.plugins.bosses.parameters.StringReader;
import dev.jorel.commandapi.Tooltip;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.entity.LivingEntity;

public class RandomAction implements Action {

	private final Action mAction1;
	private final Action mAction2;
	private final double mChance;

	private RandomAction(double chance, Action action1, Action action2) {
		mChance = chance;
		mAction1 = action1;
		mAction2 = action2;
	}


	@Override public void runAction(LivingEntity boss) {
		if (Math.random() < mChance) {
			mAction1.runAction(boss);
		} else {
			mAction2.runAction(boss);
		}
	}


	public static ParseResult<Action> fromReader(StringReader reader) {
		if (!reader.advance("(")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "(", "(..)")));
		}

		Double chance = reader.readDouble();
		if (chance == null || chance < 0) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "0.5", "ticks of delay")));
		}

		if (!reader.advance(",")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ",", ".-.")));
		}

		// action 1
		String name = reader.readOneOf(Phase.ACTION_BUILDER_MAP.keySet().stream().toList());
		if (name == null) {
			List<Tooltip<String>> suggArgs = new ArrayList<>(Phase.ACTION_BUILDER_MAP.keySet().size());
			String soFar = reader.readSoFar();
			for (String valid : Phase.ACTION_BUILDER_MAP.keySet()) {
				suggArgs.add(Tooltip.ofString(soFar + valid, "action builder"));
			}
			return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
		}

		ParseResult<Action> action1ParseResult = Objects.requireNonNull(Phase.ACTION_BUILDER_MAP.get(name)).buildAction(reader);

		if (action1ParseResult.getResult() == null) {
			return action1ParseResult;
		}

		Action action1 = action1ParseResult.getResult();

		if (!reader.advance(",")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ",", ".-.")));
		}

		// action 2
		name = reader.readOneOf(Phase.ACTION_BUILDER_MAP.keySet().stream().toList());
		if (name == null) {
			List<Tooltip<String>> suggArgs = new ArrayList<>(Phase.ACTION_BUILDER_MAP.keySet().size());
			String soFar = reader.readSoFar();
			for (String valid : Phase.ACTION_BUILDER_MAP.keySet()) {
				suggArgs.add(Tooltip.ofString(soFar + valid, "action builder"));
			}
			return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
		}

		ParseResult<Action> action2ParseResult = Objects.requireNonNull(Phase.ACTION_BUILDER_MAP.get(name)).buildAction(reader);

		if (action2ParseResult.getResult() == null) {
			return action2ParseResult;
		}

		Action action2 = action2ParseResult.getResult();

		if (!reader.advance(")")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ")", "(..)")));
		}

		return ParseResult.of(new RandomAction(chance, action1, action2));
	}

}
