package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.bosses.parameters.ParseResult;
import com.playmonumenta.plugins.bosses.parameters.StringReader;
import com.playmonumenta.plugins.utils.PlayerUtils;
import dev.jorel.commandapi.Tooltip;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class NearbyPlayersTrigger extends Trigger {

	private final double mRadius;
	private final CompareOperation mOperation;
	private final int mNumberOfPlayers;
	private final boolean mLineOfSight;

	private NearbyPlayersTrigger(double radius, CompareOperation opp, int numPlayers) {
		this(radius, opp, numPlayers, false);
	}

	private NearbyPlayersTrigger(double radius, CompareOperation opp, int numPlayers, boolean lineOfSight) {
		mRadius = radius;
		mOperation = opp;
		mNumberOfPlayers = numPlayers;
		mLineOfSight = lineOfSight;
	}


	@Override public boolean test(LivingEntity boss) {
		return testPlayersInRange(boss.getLocation());
	}

	@Override public boolean tick(LivingEntity boss, int ticks) {
		return testPlayersInRangeWithNegation(boss.getLocation());
	}

	@Override public void reset(LivingEntity boss) {

	}

	private boolean testPlayersInRangeWithNegation(Location loc) {
		boolean result = testPlayersInRange(loc);
		return (result && !isNegated()) || (!result && isNegated());
	}

	private boolean testPlayersInRange(Location loc) {
		List<Player> players = PlayerUtils.playersInRange(loc, mRadius, true);
		if (mLineOfSight) {
			players.removeIf(player -> !player.hasLineOfSight(loc));
		}

		if (players.size() == mNumberOfPlayers && (mOperation == CompareOperation.EQUAL || mOperation == CompareOperation.LEQ || mOperation == CompareOperation.GEQ)) {
			return true;
		} else if (players.size() > mNumberOfPlayers && (mOperation == CompareOperation.GREATER || mOperation == CompareOperation.GEQ)) {
			return true;
		} else if (players.size() < mNumberOfPlayers && (mOperation == CompareOperation.LOWER || mOperation == CompareOperation.LEQ)) {
			return true;
		}
		return false;
	}

	enum CompareOperation {
		EQUAL, GREATER, LOWER, LEQ, GEQ;
	}

	public static ParseResult<Trigger> fromReader(StringReader reader) {
		if (!reader.advance("(")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "(", "(..)")));
		}
		Long playerCount = reader.readLong();
		if (playerCount == null || playerCount < 0) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "5", "player count must be positive")));
		}
		if (!reader.advance(",")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ",", "(..)")));
		}

		CompareOperation opp = reader.readEnum(CompareOperation.values());
		if (opp == null) {
			List<Tooltip<String>> suggArgs = new ArrayList<>(CompareOperation.values().length);
			String soFar = reader.readSoFar();
			for (CompareOperation valid : CompareOperation.values()) {
				suggArgs.add(Tooltip.ofString(soFar + valid.name(), "CompareOperation"));
			}
			return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
		}
		if (!reader.advance(",")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ",", "(..)")));
		}

		Double range = reader.readDouble();
		if (range == null || range < 0) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "30", "range must be positive")));
		}

		Boolean requireLineOfSight = null;
		if (reader.peek().equals(")")) {
			if (!reader.advance(")")) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ")", "this object requires brackets")));
			}
		} else if (reader.peek().equals(",")) {
			if (!reader.advance(",")) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ",", "(..)")));
			}

			requireLineOfSight = reader.readBoolean();
			if (requireLineOfSight == null) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "true", "only count players in line of sight"),
					Tooltip.ofString(reader.readSoFar() + "false", "count all players in range regardless of line of sight")));
			}

			if (!reader.advance(")")) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ")", "(..)")));
			}
		}

		if (requireLineOfSight != null) {
			return ParseResult.of(new NearbyPlayersTrigger(range, opp, playerCount.intValue(), requireLineOfSight));
		}

		return ParseResult.of(new NearbyPlayersTrigger(range, opp, playerCount.intValue()));

	}
}
