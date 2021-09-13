package com.playmonumenta.plugins.bosses.spells.lich.undeadplayers;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;

/*
 * Leaves a stationary cloud of doom that deals 20 damage every 0.5 seconds
 * and inflicts slowness 1, mining fatigue 3 and unluck (for warlock undead amphex), all 30 seconds duration
 * Cloud is active for 18 seconds
 */
public class SpellPotionCloud extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;

	public SpellPotionCloud(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
	}

	@Override
	public void run() {
		Location loc = mBoss.getLocation();
		World world = mBoss.getWorld();
		world.playSound(loc, Sound.ENTITY_WITCH_CELEBRATE, SoundCategory.HOSTILE, 1.5f, 0.9f);
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 0, false));
		new PartialParticle(Particle.SPELL_WITCH, mBoss.getEyeLocation(), 10, 0.3, 0.3, 0.3, 0.1).spawnAsEnemy();

		loc.setY(loc.getY() + 0.1);
		BukkitRunnable run = new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (mT == 0) {
					world.playSound(loc, Sound.ENTITY_SPLASH_POTION_BREAK, SoundCategory.HOSTILE, 3, 1);
				}
				mT++;
				new PartialParticle(Particle.DRAGON_BREATH, loc, 15, 1.5, 0.1, 1.5, 0.01).spawnAsEnemy();

				if (mT % 10 == 0 && mT >= 20) {
					for (Player p : Lich.playersInRange(loc, 2, true)) {
						BossUtils.bossDamage(mBoss, p, 20);
						p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 30, 0));
						p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 20 * 30, 2));
						p.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, 20 * 30, 0));
					}
				}

				if (mT > 20 * 18) {
					this.cancel();
				}
			}

		};
		run.runTaskTimer(mPlugin, 20, 1);
		mActiveRunnables.add(run);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 9;
	}

}
