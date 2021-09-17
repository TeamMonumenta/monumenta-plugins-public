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

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
 * Leaves a stationary cloud of doom that deals 20 damage every 0.5 seconds
 * and inflicts slowness 1, mining fatigue 3 and unluck (for warlock undead amphex), all 30 seconds duration
 * Cloud is active for 15 seconds, afterwards, explodes and deal 40 damage to all players nearby.
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
				if (mT <= 20 * 14) {
					new PartialParticle(Particle.DRAGON_BREATH, loc, 15, 1.5, 0.1, 1.5, 0.01).spawnAsEnemy();
				}

				if (mT % 10 == 0 && mT >= 20 && mT < 20 * 15) {
					for (Player p : PlayerUtils.playersInRange(loc, 2, true)) {
						BossUtils.bossDamage(mBoss, p, 20, null, "Unstable Concoction");
						p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 30, 0));
						p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 20 * 30, 0));
						AbilityUtils.increaseDamageRecievedPlayer(p, 20 * 30, 0.15, "Lich");
					}
				}

				if (mT % 18 == 0 && mT > 20 * 12 && mT < 20 * 15) {
					world.playSound(loc, Sound.BLOCK_LAVA_EXTINGUISH, SoundCategory.HOSTILE, 1f, 1f);
					new PartialParticle(Particle.LAVA, loc, 20, 3, 0, 3, 0).spawnAsEnemy();
				}

				if (mT >= 20 * 15) {
					this.cancel();
					new PartialParticle(Particle.EXPLOSION_HUGE, loc, 1, 0, 0, 0, 0).spawnAsEnemy();
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2f, 1f);
					for (Player p : PlayerUtils.playersInRange(loc, 3, true)) {
						BossUtils.bossDamage(mBoss, p, 40, loc, "Unstable Concoction");
						p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 10, 1));
						p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 20 * 10, 2));
						AbilityUtils.increaseDamageRecievedPlayer(p, 20 * 10, 0.25, "Lich");
						MovementUtils.knockAway(loc, p, 0.7f);
					}
				}
			}

		};
		run.runTaskTimer(mPlugin, 20, 1);
		mActiveRunnables.add(run);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 18;
	}

}
