package com.playmonumenta.plugins.abilities.rogue.swordsage;

import java.util.ArrayList;
import java.util.List;

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
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;

public class WindWalk extends Ability {

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
	private int mCharges = 2;
	private boolean mWasOnCooldown = false;

	public WindWalk(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Wind Walk");
		mInfo.linkedSpell = Spells.WIND_WALK;
		mInfo.scoreboardId = "WindWalk";
		mInfo.mShorthandName = "WW";
		mInfo.mDescriptions.add("Left-click twice without hitting a mob while sprinting to dash in the target direction, stunning and levitating enemies for 2 seconds. Elites are not levitated. Cooldown: 25 seconds. Charges: 2.");
		mInfo.mDescriptions.add("Now afflicts 30% Vulnerability; enemies are stunned and levitated for 4 seconds.");
		mInfo.cooldown = WIND_WALK_COOLDOWN;
		mInfo.ignoreCooldown = true;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
		mDuration = getAbilityScore() == 1 ? WIND_WALK_1_DURATION : WIND_WALK_2_DURATION;
	}

	@Override
	public void cast(Action action) {
		if (mCharges == 0 || !mPlayer.isSprinting() || mPlayer.getLocation().getPitch() > 50
				|| !InventoryUtils.isSwordItem(mPlayer.getInventory().getItemInMainHand())
				|| !InventoryUtils.isSwordItem(mPlayer.getInventory().getItemInOffHand())
				|| ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)) {
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
		mCharges--;
		MessagingUtils.sendActionBarMessage(mPlayer, "Wind Walk Charges: " + mCharges);

		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 1.75f);
		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1, 1f);
		mWorld.spawnParticle(Particle.SMOKE_NORMAL, mPlayer.getLocation(), 90, 0.25, 0.45, 0.25, 0.1);
		mWorld.spawnParticle(Particle.CLOUD, mPlayer.getLocation(), 20, 0.25, 0.45, 0.25, 0.15);
		Vector direction = mPlayer.getLocation().getDirection();
		Vector yVelocity = new Vector(0, direction.getY() * WIND_WALK_Y_VELOCITY_MULTIPLIER + WIND_WALK_Y_VELOCITY, 0);
		mPlayer.setVelocity(direction.multiply(WIND_WALK_VELOCITY_BONUS).add(yVelocity));
		new BukkitRunnable() {
			List<LivingEntity> mMobsAlreadyHit = new ArrayList<LivingEntity>();
			@Override
			public void run() {
				mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation().add(0, 1, 0), 7, 0.25, 0.45, 0.25, 0);
				for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation().add(mPlayer.getVelocity().normalize()), WIND_WALK_RADIUS)) {
					if (!mMobsAlreadyHit.contains(mob)) {
						if (!EntityUtils.isBoss(mob)) {
							mWorld.playSound(mob.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, 1.25f);
							EntityUtils.applyStun(mPlugin, mDuration, mob);
						}
						if (EntityUtils.isElite(mob)) {
							mWorld.spawnParticle(Particle.SWEEP_ATTACK, mob.getLocation().add(0, 1, 0), 16, 0.5, 0.5, 0.5, 0);
							mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, mob.getLocation().add(0, 1, 0), 20, 0.25, 0.45, 0.25, 0.1);
							mWorld.playSound(mob.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.75f, 0.75f);
						} else if (!EntityUtils.isBoss(mob)) {
							mWorld.spawnParticle(Particle.SWEEP_ATTACK, mob.getLocation().add(0, 1, 0), 16, 0.5, 0.5, 0.5, 0);
							mWorld.spawnParticle(Particle.CLOUD, mob.getLocation().add(0, 1, 0), 20, 0.25, 0.45, 0.25, 0.1);
							mob.setVelocity(mob.getVelocity().setY(0.5));
							PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.LEVITATION, mDuration, 0, true, false));
							if (getAbilityScore() > 1) {
								PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.UNLUCK, mDuration + WIND_WALK_VULNERABILITY_DURATION_INCREASE, WIND_WALK_VULNERABILITY_AMPLIFIER, true, false));
							}
						}
						mMobsAlreadyHit.add(mob);
					}
				}
				if (mPlayer.isOnGround() || mPlayer.getLocation().getBlock().getType() == Material.WATER || mPlayer.getLocation().getBlock().getType() == Material.LAVA) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		// If the skill is somehow on cooldown when charges are full, take it off cooldown
		if (mCharges == WIND_WALK_MAX_CHARGES
				&& mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.linkedSpell)) {
			mPlugin.mTimers.removeCooldown(mPlayer.getUniqueId(), mInfo.linkedSpell);
		}

		// Increment charges if last check was on cooldown, and now is off cooldown.
		if (mCharges < WIND_WALK_MAX_CHARGES && mWasOnCooldown
				&& !mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.linkedSpell)) {
			mCharges++;
			MessagingUtils.sendActionBarMessage(mPlayer, "Wind Walk Charges: " + mCharges);
		}

		// Put on cooldown if charges can still be gained
		if (mCharges < WIND_WALK_MAX_CHARGES
				&& !mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.linkedSpell)) {
			putOnCooldown();
		}

		mWasOnCooldown = mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.linkedSpell);
	}

}
