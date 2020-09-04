package com.playmonumenta.plugins.abilities.warrior.berserker;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
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
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

public class MeteorSlam extends Ability {
	private static final double FALL_DISTANCE_THRESHOLD = 3;

	private static final String CHECK_ONCE_THIS_TICK_METAKEY = "MeteorSlamTickRightClicked";
	private static final String SLAM_ONCE_THIS_TICK_METAKEY = "MeteorSlamTickSlammed";

	private static final double DAMAGE_LOW_1 = 3;
	private static final double DAMAGE_LOW_2 = 4;
	private static final double DAMAGE_HIGH_1 = 2;
	private static final double DAMAGE_HIGH_2 = 2.5;
	private static final double RADIUS_1 = 2.0;
	private static final double RADIUS_2 = 2.5;
	private static final int JUMP_BOOST_AMPLIFIER_1 = 3;
	private static final int JUMP_BOOST_AMPLIFIER_2 = 4;
	private static final int DURATION = 2 * 20;
	private static final int COOLDOWN_1 = 8 * 20;
	private static final int COOLDOWN_2 = 6 * 20;

	private final double mDamageLow;
	private final double mDamageHigh;
	private final double mRadius;
	private final int mJumpBoostAmplifier;

	private double mFallDistance = 0;
	private boolean mCanTrigger = false;
	private final BukkitRunnable mRunnable;

	public MeteorSlam(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Meteor Slam");
		mInfo.mLinkedSpell = Spells.METEOR_SLAM;
		mInfo.mScoreboardId = "MeteorSlam";
		mInfo.mShorthandName = "MS";
		mInfo.mDescriptions.add("Hitting an enemy with an axe or sword while falling removes fall damage and does 3 extra damage for each block fallen (up to eight blocks) and 2 extra damage for each block fallen after that to all mobs within 2 blocks. If you fall more than 3 blocks but do not hit a mob, this effect still activates, but you do not negate fall damage. In addition right-clicking twice quickly grants you 2s of Jump Boost 4. Jump Boost Cooldown: 8s.");
		mInfo.mDescriptions.add("Damage increases to 4 per block for the first eight blocks and 2.5 for each block after that to all enemies within 2.5 blocks. Right-clicking twice quickly now gives 2s of Jump Boost 5. The cooldown of the Jump Boost application is reduced to 6 seconds.");
		mInfo.mCooldown = getAbilityScore() == 1 ? COOLDOWN_1 : COOLDOWN_2;
		mInfo.mIgnoreCooldown = true;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mDamageLow = getAbilityScore() == 1 ? DAMAGE_LOW_1 : DAMAGE_LOW_2;
		mDamageHigh = getAbilityScore() == 1 ? DAMAGE_HIGH_1 : DAMAGE_HIGH_2;
		mRadius = getAbilityScore() == 1 ? RADIUS_1 : RADIUS_2;
		mJumpBoostAmplifier = getAbilityScore() == 1 ? JUMP_BOOST_AMPLIFIER_1 : JUMP_BOOST_AMPLIFIER_2;

		mRunnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (player == null ||
					!player.isOnline()
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
		};

		mRunnable.runTaskTimer(plugin, 0, 1);
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
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
	public void cast(Action action) {
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack inOffHand = mPlayer.getInventory().getItemInOffHand();
		if (ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES) ||
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

			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP, DURATION, mJumpBoostAmplifier, true, false));
			putOnCooldown();
			mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1, 1);
			mWorld.spawnParticle(Particle.LAVA, mPlayer.getLocation(), 15, 1, 0f, 1, 0);

			// Flame particles that have no y velocity and only x and z.
			for (int i = 0; i < 60; i++) {
				double x = FastUtils.randomDoubleInRange(-3, 3);
				double z = FastUtils.randomDoubleInRange(-3, 3);
				Location to = mPlayer.getLocation().add(x, 0.15, z);
				Vector dir = LocationUtils.getDirectionTo(to, mPlayer.getLocation().add(0, 0.15, 0));
				mWorld.spawnParticle(Particle.FLAME, mPlayer.getLocation().add(0, 0.15, 0), 0, (float) dir.getX(), 0f, (float) dir.getZ(), FastUtils.randomDoubleInRange(0.1, 0.3));
			}
		}
	}

	public void doSlamAttack(LivingEntity damagee, double damage) {
		Location loc;
		if (damagee == null) {
			loc = mPlayer.getLocation().add(0, 0.15, 0);
		} else {
			loc = damagee.getLocation().add(0, 0.15, 0);
		}

		for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, mRadius)) {
			if (mob != damagee) {
				EntityUtils.damageEntity(Plugin.getInstance(), mob, damage, mPlayer, MagicType.PHYSICAL, true, mInfo.mLinkedSpell);
			}
		}

		mWorld.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.3F, 0);
		mWorld.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2, 1.25F);
		mWorld.spawnParticle(Particle.FLAME, loc, 60, 0F, 0F, 0F, 0.2F);
		mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 20, 0F, 0F, 0F, 0.3F);
		mWorld.spawnParticle(Particle.LAVA, loc, 3 * (int)(mRadius * mRadius), mRadius, 0.25f, mRadius, 0);
	}

	public double getSlamDamage() {
		return Math.min(8, mFallDistance) * mDamageLow + Math.max(0, (mFallDistance - 8)) * mDamageHigh;
	}

	@Override
	public void invalidate() {
		if (mRunnable != null && mRunnable.isCancelled()) {
			mRunnable.cancel();
		}
	}
}
