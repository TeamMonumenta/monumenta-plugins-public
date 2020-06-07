package com.playmonumenta.plugins.abilities.mage.arcanist;

import java.util.Set;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.events.CustomDamageEvent;

/*
 * Overload: Your spells deal an additional 1
 * damage for each other spell already on cooldown.
 * At Level 2, the extra damage is increased to 2.
 * This effect does not work with Spellshock damage.
 */

public class Overload extends Ability {

	private static final Particle.DustOptions OVERLOAD_COLOR = new Particle.DustOptions(Color.fromRGB(220, 147, 249), 1.0f);
	private static final int OVERLOAD_1_DAMAGE = 1;
	private static final int OVERLOAD_2_DAMAGE = 2;

	private final int mDamage;

	public Overload(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Overload");
		mInfo.mScoreboardId = "Overload";
		mInfo.mShorthandName = "Ov";
		mInfo.mDescriptions.add("Spells other than Spellshock and Channeling deal an additional 1 damage for each spell on cooldown.");
		mInfo.mDescriptions.add("The damage per spell on cooldown is increased to 2.");
		mDamage = getAbilityScore() == 1 ? OVERLOAD_1_DAMAGE : OVERLOAD_2_DAMAGE;
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (fourHertz) {
			Set<Spells> cds = mPlugin.mTimers.getCooldowns(mPlayer.getUniqueId());
			if (cds != null) {
				Location loc = mPlayer.getLocation().add(0, 1, 0);
				mWorld.spawnParticle(Particle.SPELL_WITCH, loc, 2 * cds.size(), 0.4, 0.4, 0.4, 1);
				mWorld.spawnParticle(Particle.REDSTONE, loc, 3 * cds.size(), 0.4, 0.4, 0.4, OVERLOAD_COLOR);
			}
		}
	}

	@Override
	public void playerDealtCustomDamageEvent(CustomDamageEvent event) {
		Set<Spells> cds = mPlugin.mTimers.getCooldowns(mPlayer.getUniqueId());
		if (cds != null) {
			event.setDamage(event.getDamage() + mDamage * cds.size());
		}
	}

}
