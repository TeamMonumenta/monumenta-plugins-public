package com.playmonumenta.plugins.bosses.spells.lich.undeadplayers;

import java.util.List;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;

/*
 * Casts Volley every 16 seconds in a cone in front of it dealing 32 damage to enemies hit.
 */

public class SpellVolley extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;

	public SpellVolley(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
	}

	@Override
	public void run() {
		World w = mBoss.getWorld();
		w.playSound(mBoss.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.HOSTILE, 3, 1);
		w.playSound(mBoss.getLocation(), Sound.ITEM_CROSSBOW_LOADING_START, SoundCategory.HOSTILE, 3, 1);
		w.playSound(mBoss.getLocation(), Sound.ITEM_CROSSBOW_LOADING_START, SoundCategory.HOSTILE, 3, 1);
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 0, false));
		BukkitRunnable runA = new BukkitRunnable() {

			@Override
			public void run() {
				w.playSound(mBoss.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.HOSTILE, 3, 1);
				w.playSound(mBoss.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_SHOOT, SoundCategory.HOSTILE, 3, 1);
				List<Projectile> projectiles;
				projectiles = EntityUtils.spawnArrowVolley(com.playmonumenta.plugins.Plugin.getInstance(), mBoss, 10, 2, 5.0, Arrow.class);
				for (Projectile proj : projectiles) {
					AbstractArrow projArrow = (AbstractArrow) proj;
					projArrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);
					projArrow.setDamage(32);

					BukkitRunnable runB = new BukkitRunnable() {

						@Override
						public void run() {
							// spawn particle
							new PartialParticle(Particle.FIREWORKS_SPARK, projArrow.getLocation(), 1, 0.1, 0.1, 0.1, 0).spawnAsEnemy();

							if (projArrow.isInBlock() || !projArrow.isValid()) {
								this.cancel();
							}
						}

					};
					runB.runTaskTimer(mPlugin, 0, 1);
					mActiveRunnables.add(runB);
				}
			}

		};
		runA.runTaskLater(mPlugin, 20);
		mActiveRunnables.add(runA);
	}

	@Override
	public int cooldownTicks() {
		// TODO Auto-generated method stub
		return 20 * 16;
	}

}
