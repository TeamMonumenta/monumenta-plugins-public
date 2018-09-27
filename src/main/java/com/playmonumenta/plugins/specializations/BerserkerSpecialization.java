package com.playmonumenta.plugins.specializations;

import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import java.util.Random;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;
import org.bukkit.World;

public class BerserkerSpecialization extends BaseSpecialization {

	private static final double METEOR_SLAM_1_DAMAGE = 2.5;
	private static final double METEOR_SLAM_2_DAMAGE = 3;
	private static final double METEOR_SLAM_1_RADIUS = 3.0;
	private static final double METEOR_SLAM_2_RADIUS = 5.0;
	private static final int METEOR_SLAM_1_EFFECT_LVL = 2;
	private static final int METEOR_SLAM_2_EFFECT_LVL = 4;
	private static final int METEOR_SLAM_DURATION = 2 * 20;
	private static final int METEOR_SLAM_1_COOLDOWN = 7 * 20;
	private static final int METEOR_SLAM_2_COOLDOWN = 5 * 20;

	private static final int RECKLESS_SWING_1_DAMAGE = 9;
	private static final int RECKLESS_SWING_2_DAMAGE = 12;
	private static final double RECKLESS_SWING_RADIUS = 2.5;
	private static final int RECKLESS_SWING_1_DAMAGE_TAKEN = 2;
	private static final int RECKLESS_SWING_2_DAMAGE_TAKEN = 1;
	private static final int RECKLESS_SWING_COOLDOWN = 12 * 20;
	private static final float RECKLESS_SWING_KNOCKBACK_SPEED = 0.3f;



	private World mWorld;

	public BerserkerSpecialization(Plugin plugin, Random random, World world) {
		super(plugin, random);
		mWorld = world;
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(Player player, EntityDamageByEntityEvent event) {
		if (player.getFallDistance() >= 1) {
			int fall = Math.round(player.getFallDistance());
			int meteorSlam = ScoreboardUtils.getScoreboardValue(player, "MeteorSlam");
			/*
			 * Meteor Slam: Hitting an enemy with an axe or sword while falling
			 * removes fall damage and does +2.5/3 for block fallen extra damage
			 * to all mobs within 3/5 blocks.
			 */

			if (meteorSlam > 0) {
				ItemStack item = player.getInventory().getItemInMainHand();
				if (InventoryUtils.isAxeItem(item) || InventoryUtils.isSwordItem(item)) {
					player.setFallDistance(0);
					LivingEntity entity = (LivingEntity) event.getEntity();
					Location loc = entity.getLocation();
					double radius = meteorSlam == 1 ? METEOR_SLAM_1_RADIUS : METEOR_SLAM_2_RADIUS;
					double dmgMult = meteorSlam == 1 ? METEOR_SLAM_1_DAMAGE : METEOR_SLAM_2_DAMAGE;

					for (Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
						if (EntityUtils.isHostileMob(e)) {
							LivingEntity le = (LivingEntity) e;
							EntityUtils.damageEntity(mPlugin, le, fall * dmgMult, player);
						}
					}

					loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.3F, 0);
					loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2, 1.25F);
					mWorld.spawnParticle(Particle.FLAME, loc, 175, 0F, 0F, 0F, 0.175F);
					//mWorld.spawnParticle(Particle.SMOKE_LARGE, loc, 50, 0F, 0F, 0F, 0.3F);
					mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 50, 0F, 0F, 0F, 0.3F);
					mWorld.spawnParticle(Particle.LAVA, loc, 100, radius, 0.25f, radius, 0);

				}
			}
		}
		return true;
	}

	@Override
	public void PlayerInteractEvent(Player player, Action action, ItemStack itemInHand, Material blockClicked) {
		if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
			if (player.isSneaking()) {
				ItemStack item = player.getInventory().getItemInMainHand();
				if (InventoryUtils.isAxeItem(item) || InventoryUtils.isSwordItem(item)) {
					int recklessSwing = ScoreboardUtils.getScoreboardValue(player, "RecklessSwing");
					/*
					 * RecklessSwing: Shift left clicking (hit) with an axe or a sword
					 * in hand causes you to wildly swing your weapon in a circle to deal
					 * as much damage as possible. Dealing 9/12 damage in a 2.5 block radius,
					 * knocking enemies hit back. You take 2/1 damage. This skill is affected
					 * by Weapon Mastery and Psychosis. (cooldown 12s)
					 */
					if (recklessSwing > 0) {
						if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.RECKLESS_SWING)) {
							int damage = recklessSwing == 1 ? RECKLESS_SWING_1_DAMAGE : RECKLESS_SWING_2_DAMAGE;
							Location loc = player.getLocation();
							for (Entity e : loc.getWorld().getNearbyEntities(loc, RECKLESS_SWING_RADIUS, RECKLESS_SWING_RADIUS, RECKLESS_SWING_RADIUS)) {
								if (EntityUtils.isHostileMob(e)) {
									LivingEntity le = (LivingEntity) e;
									EntityUtils.damageEntity(mPlugin, le, damage, player);
									MovementUtils.KnockAway(player, le, RECKLESS_SWING_KNOCKBACK_SPEED);
								}
								int selfDamage = recklessSwing == 1 ? RECKLESS_SWING_1_DAMAGE_TAKEN : RECKLESS_SWING_2_DAMAGE_TAKEN;
								player.damage(selfDamage);
							}
							mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.RECKLESS_SWING, RECKLESS_SWING_COOLDOWN);
						}
					}
				}
			}
		}
	}

	@Override
	public boolean PlayerExtendedSneakEvent(Player player) {

		return true;
	}
}
