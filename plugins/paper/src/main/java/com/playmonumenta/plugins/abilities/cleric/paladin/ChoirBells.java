package com.playmonumenta.plugins.abilities.cleric.paladin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;


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
	private static final double CHOIR_BELLS_CONICAL_THRESHOLD = 1d / 3;
	private static final int DAMAGE = 4;

	private static final float[] CHOIR_BELLS_PITCHES = {0.6f, 0.8f, 0.6f, 0.8f, 1f};

	private final double mSlownessAmount;
	private final double mWeakenEffect;
	private final double mVulnerabilityEffect;

	private @Nullable Crusade mCrusade;

	public ChoirBells(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Choir Bells");
		mInfo.mLinkedSpell = ClassAbility.CHOIR_BELLS;
		mInfo.mScoreboardId = "ChoirBells";
		mInfo.mShorthandName = "CB";
		mInfo.mDescriptions.add("While not sneaking, pressing the swap key afflicts all enemies in front of you within a 10-block cube around you with 10% slowness for 8s. Undead enemies also switch targets over to you, are dealt " + DAMAGE + " magic damage, and are afflicted with 20% vulnerability and 20% weakness for 8s. Cooldown: 20s.");
		mInfo.mDescriptions.add("Slowness is increased from 10% to 20%. Vulnerability and weakness are increased from 20% to 35%.");
		mInfo.mCooldown = COOLDOWN;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.BELL, 1);
		mSlownessAmount = getAbilityScore() == 1 ? SLOWNESS_AMPLIFIER_1 : SLOWNESS_AMPLIFIER_2;
		mWeakenEffect = getAbilityScore() == 1 ? WEAKEN_EFFECT_1 : WEAKEN_EFFECT_2;
		mVulnerabilityEffect = getAbilityScore() == 1 ? VULNERABILITY_EFFECT_1 : VULNERABILITY_EFFECT_2;

		if (player != null) {
			Bukkit.getScheduler().runTask(plugin, () -> {
				mCrusade = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, Crusade.class);
			});
		}
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		event.setCancelled(true);

		if (
			!isTimerActive()
			&& !mPlayer.isSneaking()
		) {
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

					if (Crusade.enemyTriggersAbilities(mob, mCrusade)) {
						// Infusion
						EntityUtils.applyTaunt(mPlugin, mob, mPlayer);
						DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, DAMAGE, mInfo.mLinkedSpell, true, true);
						EntityUtils.applyVulnerability(mPlugin, DURATION, mVulnerabilityEffect, mob);
						EntityUtils.applyWeaken(mPlugin, DURATION, mWeakenEffect, mob);
					}
				}
			}
			putOnCooldown();
		}
	}
}
