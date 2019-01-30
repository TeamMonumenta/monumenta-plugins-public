package com.playmonumenta.plugins.abilities.mage.arcanist;

import java.util.Random;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.CustomDamageEvent;

/*
 * Overload: Your spells deal an additional 1.5
 * damage for each other spell already on cooldown.
 * At Level 2, the extra damage is increased to 3.
 */

public class Overload extends Ability {

	public Overload(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "Overload";
	}

	@Override
	public void PlayerDealtCustomDamageEvent(CustomDamageEvent event) {
		Set<Spells> cds = mPlugin.mTimers.getCooldowns(mPlayer.getUniqueId());
		if (cds != null) {
			if (cds.size() > 0) {
				int mult = cds.size();
				double dmg = getAbilityScore() == 1 ? 1.5 : 3;
				event.setDamage(event.getDamage() + (dmg * mult));
			}
		}
	}

}
