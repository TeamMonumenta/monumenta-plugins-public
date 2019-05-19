package com.playmonumenta.plugins.abilities.mage.arcanist;

import java.util.Random;
import java.util.Set;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
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
 * This effect does not work with Spellshock damage.
 */

public class Overload extends Ability {

	public Overload(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "Overload";
	}

	private static final Particle.DustOptions OVERLOAD_COLOR = new Particle.DustOptions(Color.fromRGB(220, 147, 249), 1.0f);

	@Override
	public void PeriodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (fourHertz) {
			Set<Spells> cds = mPlugin.mTimers.getCooldowns(mPlayer.getUniqueId());
			if (cds != null && cds.size() > 0) {
				Location loc = mPlayer.getLocation().add(0, 1, 0);
				mWorld.spawnParticle(Particle.SPELL_WITCH, loc, 2, 0.4, 0.4, 0.4, 1);
				mWorld.spawnParticle(Particle.REDSTONE, loc, 3, 0.4, 0.4, 0.4, OVERLOAD_COLOR);
			}
		}
	}

	@Override
	public void PlayerDealtCustomDamageEvent(CustomDamageEvent event) {
		Set<Spells> cds = mPlugin.mTimers.getCooldowns(mPlayer.getUniqueId());
		if (cds != null) {
			if (cds.size() > 0) {
				int mult = cds.size();
				double dmg = getAbilityScore() == 1 ? 1.5 : 3;
				if (event.getDamaged() instanceof Player) {
					dmg *= 0.5;
				}
				event.setDamage(event.getDamage() + (dmg * mult));
			}
		}
	}

}
