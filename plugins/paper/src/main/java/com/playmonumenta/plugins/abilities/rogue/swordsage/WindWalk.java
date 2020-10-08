package com.playmonumenta.plugins.abilities.rogue.swordsage;

import java.util.Iterator;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

public class WindWalk extends MultipleChargeAbility {

	private static final int WIND_WALK_COOLDOWN = 20 * 25;
	private static final int WIND_WALK_MAX_CHARGES = 2;
	private static final int WIND_WALK_1_DURATION = 20 * 2;
	private static final int WIND_WALK_2_DURATION = 20 * 4;
	private static final int WIND_WALK_VULNERABILITY_DURATION_INCREASE = 20 * 3;
	private static final int WIND_WALK_VULNERABILITY_AMPLIFIER = 5;
	private static final int WIND_WALK_RADIUS = 3;
	private static final double WIND_WALK_Y_VELOCITY = 0.2;
	private static final double WIND_WALK_Y_VELOCITY_MULTIPLIER = 0.2;
	private static final double WIND_WALK_VELOCITY_BONUS = 1.5;

	private final int mDuration;

	private int mLeftClicks = 0;

	public WindWalk(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Wind Walk", WIND_WALK_MAX_CHARGES, WIND_WALK_MAX_CHARGES);
		mInfo.mLinkedSpell = Spells.WIND_WALK;
		mInfo.mScoreboardId = "WindWalk";
		mInfo.mShorthandName = "WW";
		mInfo.mDescriptions.add("Left-click twice without hitting a mob while sprinting to dash in the target direction, stunning and levitating enemies for 2 seconds. Elites are not levitated. Cooldown: 25s. Charges: 2.");
		mInfo.mDescriptions.add("Now afflicts 30% Vulnerability; enemies are stunned and levitated for 4 seconds.");
		mInfo.mCooldown = WIND_WALK_COOLDOWN;
		mInfo.mIgnoreCooldown = true;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mDuration = getAbilityScore() == 1 ? WIND_WALK_1_DURATION : WIND_WALK_2_DURATION;
	}

	@Override
	public void cast(Action action) {
		if (!mPlayer.isSprinting() || ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)
				|| !InventoryUtils.isSwordItem(mPlayer.getInventory().getItemInMainHand())
				|| !InventoryUtils.isSwordItem(mPlayer.getInventory().getItemInOffHand())) {
			return;
		}

		Location loc = mPlayer.getLocation();
		if (loc.getPitch() > 50) {
			return;
		}

		mLeftClicks++;
		new BukkitRunnable() {
			@Override
			public void run() {
				if (mLeftClicks > 0) {
					mLeftClicks--;
				}
				this.cancel();
			}
		}.runTaskLater(mPlugin, 5);
		if (mLeftClicks < 2) {
			return;
		}
		mLeftClicks = 0;

		if (!consumeCharge()) {
			return;
		}

		mPlayer.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 1.75f);
		mPlayer.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1, 1f);
		mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, 90, 0.25, 0.45, 0.25, 0.1);
		mWorld.spawnParticle(Particle.CLOUD, loc, 20, 0.25, 0.45, 0.25, 0.15);
		Vector direction = loc.getDirection();
		Vector yVelocity = new Vector(0, direction.getY() * WIND_WALK_Y_VELOCITY_MULTIPLIER + WIND_WALK_Y_VELOCITY, 0);
		mPlayer.setVelocity(direction.multiply(WIND_WALK_VELOCITY_BONUS).add(yVelocity));

		new BukkitRunnable() {
			final List<LivingEntity> mMobsNotHit = EntityUtils.getNearbyMobs(mPlayer.getLocation(), 32);
			@Override
			public void run() {
				if (mPlayer.isOnGround() || mPlayer.isDead() || !mPlayer.isOnline()) {
					this.cancel();
					return;
				}

				Material block = mPlayer.getLocation().getBlock().getType();
				if (block == Material.WATER || block == Material.LAVA || block == Material.LADDER) {
					this.cancel();
					return;
				}

				mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation().add(0, 1, 0), 7, 0.25, 0.45, 0.25, 0);

				Iterator<LivingEntity> iter = mMobsNotHit.iterator();
				while (iter.hasNext()) {
					LivingEntity mob = iter.next();

					if (mob.getLocation().distance(mPlayer.getLocation()) < WIND_WALK_RADIUS) {
						if (!EntityUtils.isBoss(mob)) {
							mWorld.spawnParticle(Particle.SWEEP_ATTACK, mob.getLocation().add(0, 1, 0), 16, 0.5, 0.5, 0.5, 0);
							mWorld.playSound(mob.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, 1.25f);

							EntityUtils.applyStun(mPlugin, mDuration, mob);
							if (getAbilityScore() > 1) {
								PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.UNLUCK, mDuration + WIND_WALK_VULNERABILITY_DURATION_INCREASE, WIND_WALK_VULNERABILITY_AMPLIFIER, true, false));
							}

							if (EntityUtils.isElite(mob)) {
								mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, mob.getLocation().add(0, 1, 0), 20, 0.25, 0.45, 0.25, 0.1);
								mWorld.playSound(mob.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.75f, 0.75f);
							} else {
								mWorld.spawnParticle(Particle.CLOUD, mob.getLocation().add(0, 1, 0), 20, 0.25, 0.45, 0.25, 0.1);

								mob.setVelocity(mob.getVelocity().setY(0.5));
								PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.LEVITATION, mDuration, 0, true, false));
							}
						}

						iter.remove();
					}
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

}
