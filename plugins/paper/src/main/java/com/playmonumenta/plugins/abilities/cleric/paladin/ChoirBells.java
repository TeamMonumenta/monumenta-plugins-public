package com.playmonumenta.plugins.abilities.cleric.paladin;

import org.bukkit.Bukkit;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.abilities.AbilityManager;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class ChoirBells extends Ability {

	private static final int DURATION = 20 * 8;
	private static final double WEAKEN_EFFECT_1 = 0.2;
	private static final double WEAKEN_EFFECT_2 = 0.35;
	private static final double VULNERABILITY_EFFECT_1 = 0.2;
	private static final double VULNERABILITY_EFFECT_2 = 0.35;
	private static final double SLOWNESS_AMPLIFIER_1 = 0.1;
	private static final double SLOWNESS_AMPLIFIER_2 = 0.2;
	private static final int COOLDOWN = 20 * 20;
	private static final int CHOIR_BELLS_RANGE = 10;
	private static final double CHOIR_BELLS_CONICAL_THRESHOLD = 0.33;

	private static final float[] CHOIR_BELLS_PITCHES = {0.6f, 0.8f, 0.6f, 0.8f, 1f};

	private final double mSlownessAmount;
	private final double mWeakenEffect;
	private final double mVulnerabilityEffect;

	private Crusade mCrusade;
	private boolean mCountsHumanoids = false;

	public ChoirBells(Plugin plugin, Player player) {
		super(plugin, player, "Choir Bells");
		mInfo.mLinkedSpell = Spells.CHOIR_BELLS;
		mInfo.mScoreboardId = "ChoirBells";
		mInfo.mShorthandName = "CB";
		mInfo.mDescriptions.add("Pressing the swap key while not sneaking causes the Cleric to become the target of any undead within a ten-block-long cone in front of them. All mobs are afflicted with 10% Slowness for 8 seconds, and undead are afflicted with 20% Weaken and 20% Vulnerability for 8 seconds. Cooldown: 20s.");
		mInfo.mDescriptions.add("Slowness increased to 20% and Weaken and Vulnerability increased to 35%.");
		mInfo.mCooldown = COOLDOWN;
		mInfo.mIgnoreCooldown = true;
		mSlownessAmount = getAbilityScore() == 1 ? SLOWNESS_AMPLIFIER_1 : SLOWNESS_AMPLIFIER_2;
		mWeakenEffect = getAbilityScore() == 1 ? WEAKEN_EFFECT_1 : WEAKEN_EFFECT_2;
		mVulnerabilityEffect = getAbilityScore() == 1 ? VULNERABILITY_EFFECT_1 : VULNERABILITY_EFFECT_2;

		Bukkit.getScheduler().runTask(plugin, () -> {
			if (player != null) {
				mCrusade = AbilityManager.getManager().getPlayerAbility(mPlayer, Crusade.class);
				if (mCrusade != null) {
					mCountsHumanoids = mCrusade.getAbilityScore() == 2;
				}
			}
		});
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		if (!mPlayer.isSneaking()) {
			event.setCancelled(true);
			if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
				return;
			}

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
					EntityUtils.applySlow(mPlugin, DURATION, mSlownessAmount, mob);

					if (EntityUtils.isUndead(mob) || (mCountsHumanoids && EntityUtils.isHumanoid(mob))) {
						EntityUtils.applyWeaken(mPlugin, DURATION, mWeakenEffect, mob);
						EntityUtils.applyVulnerability(mPlugin, DURATION, mVulnerabilityEffect, mob);
						EntityUtils.applyTaunt(mPlugin, (Mob) mob, mPlayer);
					}
				}
			}
			putOnCooldown();
		}
	}
}
