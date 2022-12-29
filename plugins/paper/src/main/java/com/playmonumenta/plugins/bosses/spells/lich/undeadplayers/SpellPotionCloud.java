package com.playmonumenta.plugins.bosses.spells.lich.undeadplayers;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
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

/*
 * Leaves a stationary cloud of doom that deals 20 damage every 0.5 seconds
 * and inflicts slowness 1, mining fatigue 3 and unluck (for warlock undead amphex), all 30 seconds duration
 * Cloud is active for 15 seconds, afterwards, explodes and deal 40 damage to all players nearby.
 */
public class SpellPotionCloud extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private PartialParticle mWitch;
	private PartialParticle mBreath;
	private PartialParticle mLava;
	private PartialParticle mExpH;

	public SpellPotionCloud(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
		mWitch = new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation(), 5, 0.3, 0.3, 0.3, 0.1);
		mBreath = new PartialParticle(Particle.DRAGON_BREATH, mBoss.getLocation(), 8, 1.5, 0.1, 1.5, 0.01);
		mLava = new PartialParticle(Particle.LAVA, mBoss.getLocation(), 20, 3, 0, 3, 0);
		mExpH = new PartialParticle(Particle.EXPLOSION_HUGE, mBoss.getLocation(), 1, 0, 0, 0, 0);
	}

	@Override
	public void run() {
		Location loc = mBoss.getLocation();
		World world = mBoss.getWorld();
		world.playSound(loc, Sound.ENTITY_WITCH_CELEBRATE, SoundCategory.HOSTILE, 1.5f, 0.9f);
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 0, false));
		mWitch.location(mBoss.getEyeLocation()).spawnAsEnemy();

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
					mBreath.location(loc).spawnAsEnemy();
				}

				if (mT % 10 == 0 && mT >= 20 && mT < 20 * 15) {
					for (Player p : PlayerUtils.playersInRange(loc, 2, true)) {
						DamageUtils.damage(mBoss, p, DamageType.AILMENT, 2, null, false, true, "Unstable Concoction");
						p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 30, 0));
						p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 20 * 30, 0));
						AbilityUtils.increaseDamageRecievedPlayer(p, 20 * 30, 0.15, "Lich");
					}
				}

				if (mT % 18 == 0 && mT > 20 * 12 && mT < 20 * 15) {
					world.playSound(loc, Sound.BLOCK_LAVA_EXTINGUISH, SoundCategory.HOSTILE, 1f, 1f);
					mLava.location(loc).spawnAsEnemy();
				}

				if (mT >= 20 * 15) {
					this.cancel();
					mExpH.location(loc).spawnAsEnemy();
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2f, 1f);
					for (Player p : PlayerUtils.playersInRange(loc, 3, true)) {
						BossUtils.blockableDamage(mBoss, p, DamageType.BLAST, 35, "Unstable Concoction", loc);
						p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 10, 1));
						p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 20 * 10, 2));
						AbilityUtils.increaseDamageRecievedPlayer(p, 20 * 10, 0.25, "Lich");
						MovementUtils.knockAway(loc, p, 0.7f, false);
					}
				}
			}

		};
		run.runTaskTimer(mPlugin, 20, 1);
		mActiveRunnables.add(run);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 8;
	}

}
