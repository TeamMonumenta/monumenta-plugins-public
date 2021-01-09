package com.playmonumenta.plugins.abilities.cleric.paladin;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
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

public class ChoirBells extends Ability {

	private static final int DURATION = 20 * 8;
	private static final double WEAKEN_EFFECT_1 = 0.2;
	private static final double WEAKEN_EFFECT_2 = 0.35;
	private static final double VULNERABILITY_EFFECT_1 = 0.2;
	private static final double VULNERABILITY_EFFECT_2 = 0.35;
	private static final int SLOWNESS_AMPLIFIER_1 = 0;
	private static final int SLOWNESS_AMPLIFIER_2 = 1;
	private static final int COOLDOWN = 20 * 20;
	private static final int CHOIR_BELLS_RANGE = 10;
	private static final double CHOIR_BELLS_CONICAL_THRESHOLD = 0.33;

	private static final float[] CHOIR_BELLS_PITCHES = {0.6f, 0.8f, 0.6f, 0.8f, 1f};

	private final int mSlownessAmplifier;
	private final double mWeakenEffect;
	private final double mVulnerabilityEffect;

	public ChoirBells(Plugin plugin, Player player) {
		super(plugin, player, "Choir Bells");
		mInfo.mLinkedSpell = Spells.CHOIR_BELLS;
		mInfo.mScoreboardId = "ChoirBells";
		mInfo.mShorthandName = "CB";
		mInfo.mDescriptions.add("Left-clicking while shifted while airborne causes the Cleric to become the target of any undead within a ten-block-long cone in front of them. All mobs are afflicted with Slowness I for 8 seconds, and undead are afflicted with 20% Weaken and 20% Vulnerability for 8 seconds. Cooldown: 20s.");
		mInfo.mDescriptions.add("Slowness is increased to II and Weaken and Vulnerability increased to 35%.");
		mInfo.mCooldown = COOLDOWN;
		mSlownessAmplifier = getAbilityScore() == 1 ? SLOWNESS_AMPLIFIER_1 : SLOWNESS_AMPLIFIER_2;
		mWeakenEffect = getAbilityScore() == 1 ? WEAKEN_EFFECT_1 : WEAKEN_EFFECT_2;
		mVulnerabilityEffect = getAbilityScore() == 1 ? VULNERABILITY_EFFECT_1 : VULNERABILITY_EFFECT_2;
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
					mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, pitch);
				}
			}.runTaskLater(mPlugin, i);
		}

		Vector playerDirection = mPlayer.getEyeLocation().getDirection().setY(0).normalize();
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), CHOIR_BELLS_RANGE)) {
			Vector toMobDirection = mob.getLocation().subtract(mPlayer.getLocation()).toVector().setY(0).normalize();
			if (playerDirection.dot(toMobDirection) > CHOIR_BELLS_CONICAL_THRESHOLD) {
				PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW, DURATION, mSlownessAmplifier, true, false));

				if (EntityUtils.isUndead(mob)) {
					EntityUtils.applyWeaken(mPlugin, DURATION, mWeakenEffect, mob);
					EntityUtils.applyVulnerability(mPlugin, DURATION, mVulnerabilityEffect, mob);
					EntityUtils.applyTaunt(mPlugin, (Mob) mob, mPlayer);
				}
			}
		}

		putOnCooldown();
	}
}
