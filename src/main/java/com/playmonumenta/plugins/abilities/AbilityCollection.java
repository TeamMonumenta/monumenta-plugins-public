package com.playmonumenta.plugins.abilities;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class AbilityCollection {

	private List<Ability> mAbilities;
	private Player mPlayer;

	public AbilityCollection(Player player) {
		mAbilities = new ArrayList<Ability>();
		mPlayer = player;
	}

	public List<Ability> getAbilities() {
		return mAbilities;
	}

	/**
	 * Removes the ability that is specified in the parameters.
	 * You can use the getAbility() methods in order to get the
	 * ability that needs to be removed.
	 * @param abil The ability that will be removed, if it exists
	 */
	public void removeAbility(Ability abil) {
		if (mAbilities.contains(abil)) {
			abil.player = null;
			mAbilities.remove(abil);
		}
	}

	public void addAbility(Ability abil) {
		abil.player = mPlayer;
		mAbilities.add(abil);
	}

	public Ability getAbility(String scoreboardId) {
		for (Ability abil : mAbilities) {
			if (abil.getInfo() != null) {
				AbilityInfo info = abil.getInfo();
				if (info.scoreboardId != null) {
					if (scoreboardId.equals(info.scoreboardId)) {
						return abil;
					}
				}
			}
		}
		return null;
	}

	public Ability getAbility(Spells spell) {
		for (Ability abil : mAbilities) {
			if (abil.getInfo() != null) {
				AbilityInfo info = abil.getInfo();
				if (info.linkedSpell != null) {
					if (spell.equals(info.linkedSpell)) {
						return abil;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Refreshes the player's ability collection.
	 */
	public void refreshAbilities() {
		mAbilities.clear();
		for (Ability abil : Ability.getAbilities()) {
			if (abil.getInfo() != null) {
				AbilityInfo info = abil.getInfo();
				if (abil.canUse(mPlayer)) {
					if (info.scoreboardId != null) {
						int score = ScoreboardUtils.getScoreboardValue(mPlayer, info.scoreboardId);
						if (score > 0) {
							addAbility(abil.getInstance());
						}
					} else {
						addAbility(abil.getInstance());
					}
				}
			}
		}
	}
}
