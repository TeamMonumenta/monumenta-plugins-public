package com.playmonumenta.plugins.abilities.warrior.berserker;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.safezone.SafeZoneManager.LocationType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;

/*
 * Meteor Slam: Hitting an enemy with an axe or sword while falling removes
 * fall damage and does +3/4 for every block fallen diminishing returns
 * also deals the extra damage to all mobs within 3/5 blocks.
 * Right clicking the air twice in quick succession
 * grants you 2s of Jump Boost 4/5. (The jump boost has cooldown 7/5s).
 * If you fall more than 3 blocks without hitting an enemy, you still deal
 * damage but also take full fall damage.
 */

public class MeteorSlam extends Ability {
	private static final double FALL_DISTANCE_THRESHOLD = 3;

	private static final String CHECK_ONCE_THIS_TICK_METAKEY = "MeteorSlamTickRightClicked";
	private static final String SLAM_ONCE_THIS_TICK_METAKEY = "MeteorSlamTickSlammed";

	private static final double METEOR_SLAM_1_DAMAGE_LOW = 3;
	private static final double METEOR_SLAM_2_DAMAGE_LOW = 4;
	private static final double METEOR_SLAM_1_DAMAGE_HIGH = 2;
	private static final double METEOR_SLAM_2_DAMAGE_HIGH = 2.5;
	private static final double METEOR_SLAM_1_RADIUS = 3.0;
	private static final double METEOR_SLAM_2_RADIUS = 5.0;
	private static final int METEOR_SLAM_1_EFFECT_LVL = 3;
	private static final int METEOR_SLAM_2_EFFECT_LVL = 4;
	private static final int METEOR_SLAM_DURATION = 2 * 20;
	private static final int METEOR_SLAM_1_COOLDOWN = 7 * 20;
	private static final int METEOR_SLAM_2_COOLDOWN = 5 * 20;

	private double mFallDistance = 0;
	private boolean mCanTrigger = false;
	private final Plugin mPlugin;

	public MeteorSlam(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mPlugin = plugin;

		mInfo.linkedSpell = Spells.METEOR_SLAM;
		mInfo.scoreboardId = "MeteorSlam";
		mInfo.cooldown = getAbilityScore() == 1 ? METEOR_SLAM_1_COOLDOWN : METEOR_SLAM_2_COOLDOWN;
		mInfo.ignoreCooldown = true;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;

		new BukkitRunnable() {
			@Override
			public void run() {
				if (!player.isOnline()
					|| !player.isValid()
					|| AbilityManager.getManager().getPlayerAbility(player, MeteorSlam.class) == null) {
					this.cancel();
					return;
				}

				mCanTrigger = mFallDistance > FALL_DISTANCE_THRESHOLD;
				if (mCanTrigger && player.isOnGround()) {
					// This value is only checked for in the LivingEntityDamagedByPlayerEvent so we don't slam twice
					MetadataUtils.checkOnceThisTick(plugin, player, SLAM_ONCE_THIS_TICK_METAKEY);
					doSlamAttack(null, getSlamDamage());
					mCanTrigger = false;
					mFallDistance = 0;
				} else {
					mFallDistance = player.getFallDistance();
				}
			}
		}.runTaskTimer(plugin, 0, 1);
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK && mPlayer.getFallDistance() > 1.5
		    && MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, SLAM_ONCE_THIS_TICK_METAKEY)) {
			double damage = getSlamDamage();
			event.setDamage(event.getDamage() + damage);
			LivingEntity damagee = (LivingEntity) event.getEntity();
			doSlamAttack(damagee, damage);
			mPlayer.setFallDistance(0);
			mFallDistance = 0;
			mCanTrigger = false;
		}

		return true;
	}

	private int mRightClicks = 0;

	@Override
	public void cast() {
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack inOffHand = mPlayer.getInventory().getItemInOffHand();
		LocationType locType = mPlugin.mSafeZoneManager.getLocationType(mPlayer.getLocation());
		if (locType == LocationType.Capital || locType == LocationType.SafeZone ||
		    InventoryUtils.isBowItem(inMainHand) || InventoryUtils.isBowItem(inOffHand) ||
		    InventoryUtils.isPotionItem(inMainHand) || inMainHand.getType().isBlock() ||
		    inMainHand.getType().isEdible()) {
			return;
		}
		if (!mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), Spells.METEOR_SLAM)) { //cooldown check because of the ignore cooldown flag
			if (MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, CHECK_ONCE_THIS_TICK_METAKEY)) {
				mRightClicks++;
				new BukkitRunnable() {
					@Override
					public void run() {
						if (mRightClicks > 0) {
							mRightClicks--;
						}
						this.cancel();
					}
				}.runTaskLater(mPlugin, 5);
			}
			if (mRightClicks < 2) {
				return;
			}
			mRightClicks = 0;
			int meteorSlam = getAbilityScore();
			int effectLevel = meteorSlam == 1 ? METEOR_SLAM_1_EFFECT_LVL : METEOR_SLAM_2_EFFECT_LVL;
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP, METEOR_SLAM_DURATION, effectLevel, true, false));
			putOnCooldown();
			mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1, 1);
			mWorld.spawnParticle(Particle.LAVA, mPlayer.getLocation(), 15, 1, 0f, 1, 0);

			//Wanted to try something new: Particles that have no y velocity and only x and z.
			//Flame
			for (int i = 0; i < 120; i++) {
				double x = ThreadLocalRandom.current().nextDouble(-3, 3);
				double z = ThreadLocalRandom.current().nextDouble(-3, 3);
				Location to = mPlayer.getLocation().add(x, 0.15, z);
				Vector dir = LocationUtils.getDirectionTo(to, mPlayer.getLocation().add(0, 0.15, 0));
				mWorld.spawnParticle(Particle.FLAME, mPlayer.getLocation().add(0, 0.15, 0), 0, (float) dir.getX(), 0f, (float) dir.getZ(), ThreadLocalRandom.current().nextDouble(0.1, 0.4));
			}
		}
	}

	public void doSlamAttack(LivingEntity damagee, double damage) {
		double radius = getAbilityScore() == 1 ? METEOR_SLAM_1_RADIUS : METEOR_SLAM_2_RADIUS;
		Location loc;
		if (damagee == null) {
			loc = mPlayer.getLocation();
		} else {
			loc = damagee.getLocation();
		}
		World world = mPlayer.getWorld();

		for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, radius)) {
			if (mob != damagee) {
				EntityUtils.damageEntity(Plugin.getInstance(), mob, damage, mPlayer);
			}
		}

		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.3F, 0);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2, 1.25F);
		world.spawnParticle(Particle.FLAME, loc, 175, 0F, 0F, 0F, 0.175F);
		world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 50, 0F, 0F, 0F, 0.3F);
		world.spawnParticle(Particle.LAVA, loc, 100, radius, 0.25f, radius, 0);
	}

	public double getSlamDamage() {
		int meteorSlam = getAbilityScore();
		double dmgMultLow = meteorSlam == 1 ? METEOR_SLAM_1_DAMAGE_LOW : METEOR_SLAM_2_DAMAGE_LOW;
		double dmgMultHigh = meteorSlam == 1 ? METEOR_SLAM_1_DAMAGE_HIGH : METEOR_SLAM_2_DAMAGE_HIGH;
		return Math.min(8, mFallDistance) * dmgMultLow + Math.max(0, (mFallDistance - 8)) * dmgMultHigh;
	}

}
