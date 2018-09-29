package com.playmonumenta.plugins.abilities;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.player.data.PlayerInfo;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class AbilityCollection {

	private List<Ability> abilities;
	private Player player;

	public AbilityCollection(Player player) {
		abilities = new ArrayList<Ability>();
		this.player = player;
	}

	public List<Ability> getAbilities() { return abilities; }

	/**
	 * Removes the ability that is specified in the parameters.
	 * You can use the getAbility() methods in order to get the
	 * ability that needs to be removed.
	 * @param abil The ability that will be removed, if it exists
	 */
	public void removeAbility(Ability abil) {
		if (abilities.contains(abil))
			abilities.remove(abil);
	}

	public void addAbility(Ability abil) { abilities.add(abil); }

	public Ability getAbility(String scoreboardId) {
		for (Ability abil : abilities) {
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
		for (Ability abil : abilities) {
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
	 * @param pInfo The PlayerInfo object of a player
	 */
	public void refreshAbilities(PlayerInfo pInfo) {
		abilities.clear();
		for (Ability abil : Ability.getAbilities()) {
			if (abil.getInfo() != null) {
				AbilityInfo info = abil.getInfo();
				if (abil.canUse(player, pInfo)) {
					if (info.scoreboardId != null) {
						int score = ScoreboardUtils.getScoreboardValue(player, info.scoreboardId);
						if (score > 0)
							addAbility(abil);
					}
				}
			}
		}
	}

}
