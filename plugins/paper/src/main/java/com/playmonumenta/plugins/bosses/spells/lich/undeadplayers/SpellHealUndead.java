package com.playmonumenta.plugins.bosses.spells.lich.undeadplayers;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;

/*
 * Heals Undead for 80hp within 10 blocks every 12 seconds.
 */

public class SpellHealUndead extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;

	public SpellHealUndead(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 1, 1);
		BukkitRunnable run = new BukkitRunnable() {
			double mRotation = 0;
			double mRadius = 5;
			int mT = 0;
			@Override
			public void run() {
				mRadius -= 0.25;
				mRotation += 3;
				mT++;
				Location loc = mBoss.getLocation();
				if (mT % 2 == 0) {
					world.playSound(mBoss.getLocation(), Sound.ITEM_TRIDENT_RETURN, SoundCategory.HOSTILE, 2, (float) (0.8 + mT * 0.1));
				}
				new PartialParticle(Particle.SPELL_INSTANT, mBoss.getLocation().add(0, 1, 0), 6, 0.4, 0.4, 0.4, 0).spawnAsEnemy();
				for (int i = 0; i < 3; i++) {
					double radian = Math.toRadians(mRotation + (120*i));
					loc.add(Math.cos(radian) * mRadius, 0, Math.sin(radian) * mRadius);
					new PartialParticle(Particle.SPELL_INSTANT, loc, 3, 0.1, 0.1, 0.1, 0.075).spawnAsEnemy();
					loc.subtract(Math.cos(radian) * mRadius, 0, Math.sin(radian) * mRadius);
				}
				if (mRadius <= 0) {
					this.cancel();
					world.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1, 1.25f);
					world.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 3, 2f);
					new PartialParticle(Particle.FIREWORKS_SPARK, loc, 50, 0, 0, 0, 0.25).spawnAsEnemy();
					new PartialParticle(Particle.SPELL_INSTANT, loc, 250, 5, 3, 5, 0.075).spawnAsEnemy();
					for (LivingEntity le : loc.getNearbyLivingEntities(10, 10)) {
						if (EntityUtils.isUndead(le) && !le.isDead() && le != mBoss) {
							double health = le.getHealth() + 80;
							if (health >= le.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()) {
								le.setHealth(le.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
							} else {
								le.setHealth(health);
							}
							new PartialParticle(Particle.SPELL_INSTANT, le.getLocation().add(0, 1, 0), 40, 0.35, 0.4, 0.35, 0.075).spawnAsEnemy();
						}
					}
				}
			}

		};
		run.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(run);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 12;
	}

}
