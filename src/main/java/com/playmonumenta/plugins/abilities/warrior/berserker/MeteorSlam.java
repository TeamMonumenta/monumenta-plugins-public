package com.playmonumenta.plugins.abilities.warrior.berserker;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

/*
 * Meteor Slam: Hitting an enemy with an axe or sword while falling removes
 * fall damage and does +2.5/3 for block fallen extra damage to all mobs
 * within 3/5 blocks. Holding shift and looking directly down for 2s
 * grants you 2s of Jump Boost 3/5. (The jump boost has cooldown 7/5s)
 */

public class MeteorSlam extends Ability {

	private static final double METEOR_SLAM_1_DAMAGE = 2.5;
	private static final double METEOR_SLAM_2_DAMAGE = 3;
	private static final double METEOR_SLAM_1_RADIUS = 3.0;
	private static final double METEOR_SLAM_2_RADIUS = 5.0;
	private static final double METEOR_SLAM_ANGLE = 70;
	private static final int METEOR_SLAM_1_EFFECT_LVL = 2;
	private static final int METEOR_SLAM_2_EFFECT_LVL = 4;
	private static final int METEOR_SLAM_DURATION = 2 * 20; //ticks
	private static final int METEOR_SLAM_1_COOLDOWN = 7 * 20;
	private static final int METEOR_SLAM_2_COOLDOWN = 5 * 20;

	public MeteorSlam(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.METEOR_SLAM;
		mInfo.scoreboardId = "MeteorSlam";
		// NOTE: getAbilityScore() can only be used after the scoreboardId is set!
		mInfo.cooldown = getAbilityScore() == 1 ? METEOR_SLAM_1_COOLDOWN : METEOR_SLAM_2_COOLDOWN;
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		LivingEntity damagee = (LivingEntity) event.getEntity();

		if (mPlayer.getFallDistance() >= 1) {
			int fall = Math.round(mPlayer.getFallDistance());
			int meteorSlam = getAbilityScore();
			ItemStack item = mPlayer.getInventory().getItemInMainHand();
			if (InventoryUtils.isAxeItem(item) || InventoryUtils.isSwordItem(item)) {
				mPlayer.setFallDistance(0);
				Location loc = damagee.getLocation();
				double radius = meteorSlam == 1 ? METEOR_SLAM_1_RADIUS : METEOR_SLAM_2_RADIUS;
				double dmgMult = meteorSlam == 1 ? METEOR_SLAM_1_DAMAGE : METEOR_SLAM_2_DAMAGE;
				double meteorSlamDamage = fall * dmgMult;
				event.setDamage(event.getDamage() + meteorSlamDamage);
				for (Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
					if (EntityUtils.isHostileMob(e) && e != damagee) {
						LivingEntity le = (LivingEntity) e;
						EntityUtils.damageEntity(mPlugin, le, meteorSlamDamage, mPlayer);
					}
				}

				loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.3F, 0);
				loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2, 1.25F);
				mWorld.spawnParticle(Particle.FLAME, loc, 175, 0F, 0F, 0F, 0.175F);
				mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 50, 0F, 0F, 0F, 0.3F);
				mWorld.spawnParticle(Particle.LAVA, loc, 100, radius, 0.25f, radius, 0);
			}
		}
		return true;
	}

	@Override
	public void PlayerExtendedSneakEvent() {
		int meteorSlam = getAbilityScore();
		if (mPlayer.getLocation().getPitch() > METEOR_SLAM_ANGLE) {
			if (!mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), Spells.METEOR_SLAM)) {
				int effectLevel = meteorSlam == 1 ? METEOR_SLAM_1_EFFECT_LVL : METEOR_SLAM_2_EFFECT_LVL;
				int cooldown = meteorSlam == 1 ? METEOR_SLAM_1_COOLDOWN : METEOR_SLAM_2_COOLDOWN;
				mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP, METEOR_SLAM_DURATION, effectLevel, true, false));
				mPlugin.mTimers.AddCooldown(mPlayer.getUniqueId(), Spells.METEOR_SLAM, cooldown);
			}
		}
	}
}
