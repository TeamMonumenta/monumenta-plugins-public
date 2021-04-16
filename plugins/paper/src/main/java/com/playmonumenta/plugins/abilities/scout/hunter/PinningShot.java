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
import com.playmonumenta.plugins.utils.EntityUtils;

public class PinningShot extends Ability {

	private static final double PINNING_SHOT_1_DAMAGE_MULTIPLIER = 1.2;
	private static final double PINNING_SHOT_2_DAMAGE_MULTIPLIER = 1.6;
	private static final int PINNING_SHOT_1_DURATION = 20 * 4;
	private static final int PINNING_SHOT_2_DURATION = 20 * 6;
	private static final double PINNING_SLOW = 0.7;

	private final double mDamageMultiplier;
	private final int mDuration;

	private final Map<LivingEntity, Boolean> mPinnedMobs = new HashMap<LivingEntity, Boolean>();

	public PinningShot(Plugin plugin, Player player) {
		super(plugin, player, "Pinning Shot");
		mInfo.mScoreboardId = "PinningShot";
		mInfo.mShorthandName = "PSh";
		mInfo.mDescriptions.add("The first time you shoot a non-boss enemy, pin it for 4s. Pinned enemies are afflicted with 70% Slowness. Shooting a pinned enemy deals 20% more damage and removes the pin.");
		mInfo.mDescriptions.add("Pins last for 6s instead, and bonus damage when shooting pinned enemies is increased to 60%.");
		mInfo.mIgnoreTriggerCap = true;

		mDamageMultiplier = getAbilityScore() == 1 ? PINNING_SHOT_1_DAMAGE_MULTIPLIER : PINNING_SHOT_2_DAMAGE_MULTIPLIER;
		mDuration = getAbilityScore() == 1 ? PINNING_SHOT_1_DURATION : PINNING_SHOT_2_DURATION;
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity damagee, EntityDamageByEntityEvent event) {
		World world = mPlayer.getWorld();
		if (mPinnedMobs.containsKey(damagee)) {
			// If currently pinned
			if (mPinnedMobs.get(damagee)) {
				Location loc = damagee.getEyeLocation();
				world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 0.5f);
				world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 20, 0, 0, 0, 0.2);
				world.spawnParticle(Particle.SNOWBALL, loc, 30, 0, 0, 0, 0.25);
				damagee.removePotionEffect(PotionEffectType.SLOW);
				event.setDamage(event.getDamage() * mDamageMultiplier);
				mPinnedMobs.put(damagee, false);
			}
		} else {
			Location loc = damagee.getLocation();
			world.playSound(loc, Sound.BLOCK_SLIME_BLOCK_PLACE, 1, 0.5f);
			world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 8, 0, 0, 0, 0.2);
			EntityUtils.applySlow(mPlugin, mDuration, PINNING_SLOW, damagee);
			mPinnedMobs.put(damagee, true);
		}

		return true;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond) {
			mPinnedMobs.entrySet().removeIf((entry) -> !entry.getKey().isValid() || entry.getKey().isDead());
		}
	}

}
