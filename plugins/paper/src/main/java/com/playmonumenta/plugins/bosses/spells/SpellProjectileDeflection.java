package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;

import com.playmonumenta.plugins.utils.LocationUtils;

public class SpellProjectileDeflection extends Spell {
	private LivingEntity mBoss;

	public SpellProjectileDeflection(LivingEntity boss) {
		mBoss = boss;
	}

	@Override
	public void bossHitByProjectile(ProjectileHitEvent event) {
		Projectile proj = event.getEntity();
		if (proj.getShooter() instanceof Player) {
			Player player = (Player) proj.getShooter();
			if (!(proj instanceof Trident)) {
				Projectile deflected = (Projectile) mBoss.getWorld().spawnEntity(proj.getLocation().subtract(proj.getVelocity().normalize()), proj.getType());
				deflected.setShooter(mBoss);
				if (deflected instanceof Arrow && proj instanceof Arrow) {
					Arrow arrow = (Arrow) deflected;
					Arrow projec = (Arrow) proj;
					((Arrow) deflected).setCritical(((Arrow) proj).isCritical());
					if (projec.getBasePotionData() != null) {
						arrow.setBasePotionData((projec).getBasePotionData());
						for (PotionEffect effect : (projec).getCustomEffects()) {
							arrow.addCustomEffect(effect, true);
						}
					}

				}
				deflected.setVelocity(LocationUtils.getDirectionTo(player.getLocation().add(0, 1.25, 0), deflected.getLocation()).multiply(proj.getVelocity().length()));
				proj.remove();
			}
			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 2);
			mBoss.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, proj.getLocation(), 10, 0, 0, 0, 0.1);
		}
	}

	@Override
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		if (event.getDamager() instanceof Projectile) {
			Projectile proj = (Projectile) event.getDamager();
			if (proj.getShooter() instanceof Player) {
				event.setCancelled(true);
			}
		}
	}

	@Override
	public void run() {
	}

	@Override
	public int duration() {
		return 0;
	}

}
