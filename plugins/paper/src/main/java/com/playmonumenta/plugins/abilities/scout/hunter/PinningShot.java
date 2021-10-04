package com.playmonumenta.plugins.abilities.scout.hunter;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;



public class PinningShot extends Ability {

	private static final double PINNING_SHOT_1_DAMAGE_MULTIPLIER = 0.1;
	private static final double PINNING_SHOT_2_DAMAGE_MULTIPLIER = 0.2;
	private static final int PINNING_SHOT_DURATION = (int) (20 * 2.5);
	private static final double PINNING_SLOW = 1.0;
	private static final double PINNING_SLOW_BOSS = 0.3;
	private static final double PINNING_WEAKEN_1 = 0.3;
	private static final double PINNING_WEAKEN_2 = 0.6;

	private final double mDamageMultiplier;
	private final double mWeaken;

	private final Map<LivingEntity, Boolean> mPinnedMobs = new HashMap<LivingEntity, Boolean>();

	public PinningShot(Plugin plugin, Player player) {
		super(plugin, player, "Pinning Shot");
		mInfo.mScoreboardId = "PinningShot";
		mInfo.mShorthandName = "PSh";
		mInfo.mDescriptions.add("The first time you shoot a non-boss enemy, pin it for 2.5s. Pinned enemies are afflicted with 100% Slowness and 30% Weaken (Bosses receive 30% Slowness and no Weaken). Shooting a pinned non-boss enemy deals 10% of its max health on top of regular damage and removes the pin. A mob cannot be pinned more than once.");
		mInfo.mDescriptions.add("Weaken increased to 60% and bonus damage increased to 20% max health.");
		mInfo.mIgnoreTriggerCap = true;
		mDisplayItem = new ItemStack(Material.CROSSBOW, 1);

		mDamageMultiplier = getAbilityScore() == 1 ? PINNING_SHOT_1_DAMAGE_MULTIPLIER : PINNING_SHOT_2_DAMAGE_MULTIPLIER;
		mWeaken = getAbilityScore() == 1 ? PINNING_WEAKEN_1 : PINNING_WEAKEN_2;
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
				EntityUtils.setSlowTicks(mPlugin, damagee, 1);
				EntityUtils.setWeakenTicks(mPlugin, damagee, 1);
				AttributeInstance maxHealth = damagee.getAttribute(Attribute.GENERIC_MAX_HEALTH);
				if (!EntityUtils.isBoss(damagee) && maxHealth != null) {
					EntityUtils.damageEntity(mPlugin, damagee, maxHealth.getValue() * mDamageMultiplier, mPlayer, null, true, null, false, false, true);
				}
				mPinnedMobs.put(damagee, false);
			}
		} else if (!mPinnedMobs.containsKey(damagee)) {
			Location loc = damagee.getLocation();
			world.playSound(loc, Sound.BLOCK_SLIME_BLOCK_PLACE, 1, 0.5f);
			world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 8, 0, 0, 0, 0.2);
			if (EntityUtils.isBoss(damagee)) {
				EntityUtils.applySlow(mPlugin, PINNING_SHOT_DURATION, PINNING_SLOW_BOSS, damagee);
			} else {
				EntityUtils.applySlow(mPlugin, PINNING_SHOT_DURATION, PINNING_SLOW, damagee);
				EntityUtils.applyWeaken(mPlugin, PINNING_SHOT_DURATION, mWeaken, damagee);
			}
			mPinnedMobs.put(damagee, true);
			new BukkitRunnable() {
				@Override
				public void run() {
					mPinnedMobs.put(damagee, false);
				}
			}.runTaskLater(mPlugin, PINNING_SHOT_DURATION);
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
