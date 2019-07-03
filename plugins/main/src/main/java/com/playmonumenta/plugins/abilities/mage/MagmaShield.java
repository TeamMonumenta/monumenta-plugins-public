package com.playmonumenta.plugins.abilities.mage;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;

public class MagmaShield extends Ability {

	private static final int MAGMA_SHIELD_COOLDOWN = 12 * 20;
	private static final int MAGMA_SHIELD_RADIUS = 6;
	private static final int MAGMA_SHIELD_FIRE_DURATION = 4 * 20;
	private static final int MAGMA_SHIELD_1_DAMAGE = 7;
	private static final int MAGMA_SHIELD_2_DAMAGE = 14;
	private static final float MAGMA_SHIELD_KNOCKBACK_SPEED = 0.5f;
	private static final double MAGMA_SHIELD_DOT_ANGLE = 0.33;

	public MagmaShield(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.MAGMA_SHIELD;
		mInfo.scoreboardId = "Magma";
		mInfo.cooldown = MAGMA_SHIELD_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public void cast() {
		Vector playerDir = mPlayer.getEyeLocation().getDirection().setY(0).normalize();
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), MAGMA_SHIELD_RADIUS, mPlayer)) {
			Vector toMobVector = mob.getLocation().toVector().subtract(mPlayer.getLocation().toVector()).setY(0).normalize();
			if (playerDir.dot(toMobVector) > MAGMA_SHIELD_DOT_ANGLE) {
				float kb = (mob instanceof Player) ? 0.3f : MAGMA_SHIELD_KNOCKBACK_SPEED;
				MovementUtils.KnockAway(mPlayer, mob, kb);
				EntityUtils.applyFire(mPlugin, MAGMA_SHIELD_FIRE_DURATION, mob);

				int extraDamage = getAbilityScore() == 1 ? MAGMA_SHIELD_1_DAMAGE : MAGMA_SHIELD_2_DAMAGE;
				if (mob instanceof Player && extraDamage > 10) {
					extraDamage = 10;
				}
				EntityUtils.damageEntity(mPlugin, mob, extraDamage, mPlayer, MagicType.FIRE);
			}
		}

		mWorld.spawnParticle(Particle.SMOKE_LARGE, mPlayer.getLocation(), 15, 0.05, 0.05, 0.05, 0.1);
		new BukkitRunnable() {
			Location loc = mPlayer.getLocation();
			double radius = 0;

			@Override
			public void run() {
				if (radius == 0) {
					loc.setDirection(mPlayer.getLocation().getDirection().setY(0).normalize());
				}
				Vector vec;
				radius += 1.25;
				for (double degree = 30; degree <= 150; degree += 10) {
					double radian1 = Math.toRadians(degree);
					vec = new Vector(Math.cos(radian1) * radius, 0.125, Math.sin(radian1) * radius);
					vec = VectorUtils.rotateXAxis(vec, -loc.getPitch());
					vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

					Location l = loc.clone().add(0, 0.1, 0).add(vec);
					mWorld.spawnParticle(Particle.FLAME, l, 2, 0.15, 0.15, 0.15, 0.15);
					mWorld.spawnParticle(Particle.SMOKE_NORMAL, l, 3, 0.15, 0.15, 0.15, 0.1);
				}

				if (radius >= MAGMA_SHIELD_RADIUS + 1) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.75f);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1f, 1.25f);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f);
		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		if (InventoryUtils.isWandItem(mainHand)
		    || InventoryUtils.isWandItem(offHand)) {
			return mPlayer.isSneaking() && mPlayer.getLocation().getPitch() > -50;
		}
		return false;
	}

}
