package com.playmonumenta.plugins.abilities.scout.hunter;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.PotionUtils;

public class PinningShot extends Ability {

	private static final double PINNING_SHOT_1_DAMAGE_MULTIPLIER = 1.2;
	private static final double PINNING_SHOT_2_DAMAGE_MULTIPLIER = 1.6;
	private static final int PINNING_SHOT_1_DURATION = 20 * 4;
	private static final int PINNING_SHOT_2_DURATION = 20 * 6;

	private final double mDamageMultiplier;
	private final int mDuration;

	private final Map<LivingEntity, Boolean> mPinnedMobs = new HashMap<LivingEntity, Boolean>();

	public PinningShot(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Pinning Shot");
		mInfo.mScoreboardId = "PinningShot";
		mInfo.mShorthandName = "PSh";
		mInfo.mDescriptions.add("The first time you shoot a non-boss enemy, pin it for 4s. Pinned enemies are afflicted with Slowness VII. Shooting a pinned enemy deals 20% more damage and removes the pin.");
		mInfo.mDescriptions.add("Pins last for 6s instead, and bonus damage when shooting pinned enemies is increased to 60%.");
		mInfo.mIgnoreTriggerCap = true;

		mDamageMultiplier = getAbilityScore() == 1 ? PINNING_SHOT_1_DAMAGE_MULTIPLIER : PINNING_SHOT_2_DAMAGE_MULTIPLIER;
		mDuration = getAbilityScore() == 1 ? PINNING_SHOT_1_DURATION : PINNING_SHOT_2_DURATION;
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity damagee, EntityDamageByEntityEvent event) {
		if (mPinnedMobs.containsKey(damagee)) {
			// If currently pinned
			if (mPinnedMobs.get(damagee)) {
				Location loc = damagee.getEyeLocation();
				mWorld.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 0.5f);
				mWorld.spawnParticle(Particle.FIREWORKS_SPARK, loc, 20, 0, 0, 0, 0.2);
				mWorld.spawnParticle(Particle.SNOWBALL, loc, 30, 0, 0, 0, 0.25);
				damagee.removePotionEffect(PotionEffectType.SLOW);
				event.setDamage(event.getDamage() * mDamageMultiplier);
				mPinnedMobs.put(damagee, false);
			}
		} else {
			Location loc = damagee.getLocation();
			mWorld.playSound(loc, Sound.BLOCK_SLIME_BLOCK_PLACE, 1, 0.5f);
			mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 8, 0, 0, 0, 0.2);
			PotionUtils.applyPotion(mPlayer, damagee, new PotionEffect(PotionEffectType.SLOW, mDuration, 6));
			mPinnedMobs.put(damagee, true);
		}

		return true;
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond) {
			for (LivingEntity mob : mPinnedMobs.keySet()) {
				if (!mob.isValid() || mob.isDead()) {
					mPinnedMobs.remove(mob);
				}
			}
		}
	}

}
