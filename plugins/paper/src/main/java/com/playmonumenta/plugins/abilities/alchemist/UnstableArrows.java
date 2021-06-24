package com.playmonumenta.plugins.abilities.alchemist;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.EnumSet;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.enchantments.abilities.BaseAbilityEnchantment;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

public class UnstableArrows extends Ability {
	public static class UnstableArrowsCooldownEnchantment extends BaseAbilityEnchantment {
		public UnstableArrowsCooldownEnchantment() {
			super("Unstable Arrows Cooldown", EnumSet.of(ItemSlot.OFFHAND));
		}
	}

	public static class UnstableArrowsFuseEnchantment extends BaseAbilityEnchantment {
		public UnstableArrowsFuseEnchantment() {
			super("Unstable Arrows Fuse", EnumSet.of(ItemSlot.OFFHAND));
		}
	}

	private static final int UNSTABLE_ARROWS_1_COOLDOWN = 20 * 20;
	private static final int UNSTABLE_ARROWS_2_COOLDOWN = 16 * 20;
	private static final int UNSTABLE_ARROWS_DURATION = 3 * 20;
	private static final int UNSTABLE_ARROWS_PARTICLE_PERIOD = 3;
	private static final float UNSTABLE_ARROWS_KNOCKBACK_SPEED = 2.5f;
	private static final double UNSTABLE_ARROWS_1_DAMAGE = 12.0;
	private static final double UNSTABLE_ARROWS_2_DAMAGE = 20.0;
	private static final int UNSTABLE_ARROWS_RADIUS = 4;

	private AbstractArrow mUnstableArrow = null;

	public UnstableArrows(Plugin plugin, Player player) {
		super(plugin, player, "Unstable Arrows");
		mInfo.mLinkedSpell = ClassAbility.UNSTABLE_ARROWS;
		mInfo.mScoreboardId = "BombArrow";
		mInfo.mShorthandName = "UA";
		mInfo.mDescriptions.add(" When you crouch and fire an arrow it will begin to hiss upon landing. 3s later it explodes, dealing 12 damage to mobs within a four block radius and spawning an Alchemist Potion at the location. Cooldown: 20 seconds. You can toggle whether the explosion will apply knockback to you or not in the P.E.B.");
		mInfo.mDescriptions.add("The damage is increased to 20 and the cooldown is reduced to 16s.");
		if (player != null && ScoreboardUtils.getScoreboardValue(player, "RocketJumper") == 9001) {
			mInfo.mCooldown = 0;
		} else {
			mInfo.mCooldown = (int) UnstableArrowsCooldownEnchantment.getCooldown(player, getAbilityScore() == 1 ? UNSTABLE_ARROWS_1_COOLDOWN : UNSTABLE_ARROWS_2_COOLDOWN, UnstableArrowsCooldownEnchantment.class);
		}
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		if (proj instanceof Arrow || proj instanceof SpectralArrow) {
			AbstractArrow arrow = (AbstractArrow) proj;
			if (mUnstableArrow != null && arrow == mUnstableArrow) {
				arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
				putOnCooldown();
				mUnstableArrow = null;

				new BukkitRunnable() {
					Location mLoc = EntityUtils.getProjectileHitLocation(event);
					int mTicks = 0;
					@Override
					public void run() {
						World world = mPlayer.getWorld();
						world.spawnParticle(Particle.FLAME, mLoc, 3, 0.3, 0.3, 0.3, 0.05);
						world.spawnParticle(Particle.SMOKE_NORMAL, mLoc, 7, 0.5, 0.5, 0.5, 0.075);
						world.playSound(mLoc, Sound.BLOCK_LAVA_EXTINGUISH, 0.3f,
						                 ((UNSTABLE_ARROWS_DURATION / 3.0f) + mTicks) / (1.5f * UNSTABLE_ARROWS_DURATION));
						if (mTicks % 18 == 0) {
							world.playSound(mLoc, Sound.BLOCK_LAVA_EXTINGUISH, 1.6f, 1f + mTicks / 36f);
							world.spawnParticle(Particle.LAVA, mLoc, 80, UNSTABLE_ARROWS_RADIUS, 0, UNSTABLE_ARROWS_RADIUS, 0);
						}
						if (mTicks >= UNSTABLE_ARROWS_DURATION + UnstableArrowsFuseEnchantment.getLevel(mPlayer, UnstableArrowsFuseEnchantment.class)) {
							arrow.remove();
							world.playSound(mLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0f);
							world.playSound(mLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.25f);

							world.spawnParticle(Particle.FLAME, mLoc, 115, 0.02, 0.02, 0.02, 0.2);
							world.spawnParticle(Particle.SMOKE_LARGE, mLoc, 40, 0.02, 0.02, 0.02, 0.35);
							world.spawnParticle(Particle.EXPLOSION_NORMAL, mLoc, 40, 0.02, 0.02, 0.02, 0.35);

							double baseDamage = (getAbilityScore() == 1) ? UNSTABLE_ARROWS_1_DAMAGE : UNSTABLE_ARROWS_2_DAMAGE;
							AlchemistPotions ap = AbilityManager.getManager().getPlayerAbility(mPlayer, AlchemistPotions.class);
							double potDamage = ap.getDamage();

							for (LivingEntity mob : EntityUtils.getNearbyMobs(mLoc, UNSTABLE_ARROWS_RADIUS, mPlayer)) {
								EntityUtils.damageEntity(mPlugin, mob, baseDamage + potDamage, mPlayer, MagicType.ALCHEMY, true, mInfo.mLinkedSpell);
								if (ap != null) {
									ap.apply(mob);
								}

								MovementUtils.knockAwayRealistic(mLoc, mob, UNSTABLE_ARROWS_KNOCKBACK_SPEED, 0.5f);
							}

							// Custom knockback function because this is unreliable as is with weird arrow location calculations
							if (ScoreboardUtils.getScoreboardValue(mPlayer, "RocketJumper") == 1
								&& mPlayer.getLocation().distance(mLoc) < UNSTABLE_ARROWS_RADIUS / 2.0) {
								if (!ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)) {
									Vector dir = mPlayer.getLocation().subtract(mLoc.toVector()).toVector();
									dir.setY(dir.getY() / 1.5).normalize().multiply(2);
									dir.setY(dir.getY() + 0.5);
									mPlayer.setVelocity(dir);
								}
							} else if (ScoreboardUtils.getScoreboardValue(mPlayer, "RocketJumper") == 9001
								&& mPlayer.getLocation().distance(mLoc) < UNSTABLE_ARROWS_RADIUS) {
								MovementUtils.knockAwayRealistic(mLoc, mPlayer, UNSTABLE_ARROWS_KNOCKBACK_SPEED * 4, 6);
							}
							this.cancel();
						}
						mTicks += UNSTABLE_ARROWS_PARTICLE_PERIOD;
					}
				}.runTaskTimer(mPlugin, 0, UNSTABLE_ARROWS_PARTICLE_PERIOD);
			}
		}
	}

	@Override
	public boolean playerShotArrowEvent(AbstractArrow arrow) {
		// Can't use runCheck for this because player doesn't need to be sneaking when arrow lands
		if (mPlayer.isSneaking()) {
			mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 5.0f, 0.25f);
			mUnstableArrow = arrow;
			mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.FLAME);
		}

		return true;
	}
}
