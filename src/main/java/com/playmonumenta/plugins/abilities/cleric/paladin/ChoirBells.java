package com.playmonumenta.plugins.abilities.cleric.paladin;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;

/*
* Right-Clicking while sprinting causes the Paladin to become the target of any
* undead within a conical area in front of them. Affected Undead are stricken with
* slowness II (level 1) and 30% Vulnerability (level 2) for 20s. Cooldown 30/20s
*/

public class ChoirBells extends Ability {

	private static final double CHOIR_BELLS_CONICAL_THRESHOLD = 0.33;
	private static final double CHOIR_BELLS_RANGE = 10;
	private static final int CHOIR_BELLS_SLOWNESS_DURATION = 20 * 20;
	private static final int CHOIR_BELLS_SLOWNESS_LEVEL = 1;
	private static final int CHOIR_BELLS_VULNERABILITY_DURATION = 20 * 20;
	private static final int CHOIR_BELLS_VULNERABILITY_LEVEL = 5;
	private static final int CHOIR_BELLS_1_COOLDOWN = 30 * 20;
	private static final int CHOIR_BELLS_2_COOLDOWN = 20 * 20;

	public ChoirBells(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.CHOIR_BELLS;
		mInfo.scoreboardId = "ChoirBells";
		mInfo.cooldown = getAbilityScore() == 1 ? CHOIR_BELLS_1_COOLDOWN : CHOIR_BELLS_2_COOLDOWN;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSneaking() && !mPlayer.isOnGround();
	}

	@Override
	public boolean cast() {
		ParticleUtils.explodingConeEffect(mPlugin, mPlayer, 10, Particle.VILLAGER_HAPPY, 0.5f, Particle.SPELL_INSTANT, 0.5f, 0.33);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 0.4f);
		Vector playerDirection = mPlayer.getEyeLocation().getDirection().setY(0).normalize();
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), CHOIR_BELLS_RANGE)) {
			if (EntityUtils.isUndead(mob)) {
				Vector toMobDirection = mob.getLocation().toVector().subtract(mPlayer.getLocation().toVector()).setY(0).normalize();
				if (playerDirection.dot(toMobDirection) > CHOIR_BELLS_CONICAL_THRESHOLD) {
					((Mob)mob).setTarget(mPlayer);
					mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, CHOIR_BELLS_SLOWNESS_DURATION, CHOIR_BELLS_SLOWNESS_LEVEL, true, false));
					if (getAbilityScore() == 2) {
						mob.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, CHOIR_BELLS_VULNERABILITY_DURATION, CHOIR_BELLS_VULNERABILITY_LEVEL, true, false));
					}
				}
			}
		}

		putOnCooldown();
		return true;
	}
}