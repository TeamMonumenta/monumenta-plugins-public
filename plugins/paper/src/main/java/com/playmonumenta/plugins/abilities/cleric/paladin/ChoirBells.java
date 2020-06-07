package com.playmonumenta.plugins.abilities.cleric.paladin;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

/*
* Left-Clicking while shifted while airborne causes the Paladin
* to become the target of any undead within a ten-block-long cone
* in front of them. Affected undead gain slowness 2 for 20s.
* At level 2 affected undead also gain 30% vulnerability for 20s.
* Cooldown: 30/20s.
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
	private static final float[] CHOIR_BELLS_PITCHES = {0.6f, 0.8f, 0.6f, 0.8f, 1f};

	public ChoirBells(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Choir Bells");
		mInfo.mLinkedSpell = Spells.CHOIR_BELLS;
		mInfo.mScoreboardId = "ChoirBells";
		mInfo.mShorthandName = "CB";
		mInfo.mDescriptions.add("Left-clicking while shifted while airborne causes the Cleric to become the target of any undead within a ten-block-long cone in front of them. Affected Undead gain Slowness 2 for 20s. Cooldown: 30s.");
		mInfo.mDescriptions.add("Affected Undead also gain 30% Vulnerability for 20s. Cooldown is reduced to 20s.");
		mInfo.mCooldown = getAbilityScore() == 1 ? CHOIR_BELLS_1_COOLDOWN : CHOIR_BELLS_2_COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
	}

	@Override
	public boolean runCheck() {
		Material blockType = mPlayer.getLocation().getBlock().getType();
		return (mPlayer.isSneaking() && !mPlayer.isOnGround() && blockType != Material.LADDER && blockType != Material.VINE);
	}

	@Override
	public void cast(Action action) {
		ParticleUtils.explodingConeEffect(mPlugin, mPlayer, 10, Particle.VILLAGER_HAPPY, 0.5f, Particle.SPELL_INSTANT, 0.5f, 0.33);

		for (int i = 0; i < CHOIR_BELLS_PITCHES.length; i++) {
			float pitch = CHOIR_BELLS_PITCHES[i];
			new BukkitRunnable() {
				@Override
				public void run() {
					mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, pitch);
				}
			}.runTaskLater(mPlugin, i);
		}

		Vector playerDirection = mPlayer.getEyeLocation().getDirection().setY(0).normalize();
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), CHOIR_BELLS_RANGE)) {
			if (EntityUtils.isUndead(mob)) {
				Vector toMobDirection = mob.getLocation().toVector().subtract(mPlayer.getLocation().toVector()).setY(0).normalize();
				if (playerDirection.dot(toMobDirection) > CHOIR_BELLS_CONICAL_THRESHOLD) {
					((Mob)mob).setTarget(mPlayer);
					PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW, CHOIR_BELLS_SLOWNESS_DURATION, CHOIR_BELLS_SLOWNESS_LEVEL, true, false));
					if (getAbilityScore() == 2) {
						PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.UNLUCK, CHOIR_BELLS_VULNERABILITY_DURATION, CHOIR_BELLS_VULNERABILITY_LEVEL, true, false));
					}
				}
			}
		}

		putOnCooldown();
	}
}
