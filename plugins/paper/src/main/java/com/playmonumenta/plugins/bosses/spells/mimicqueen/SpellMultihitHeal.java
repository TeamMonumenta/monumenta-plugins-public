package com.playmonumenta.plugins.bosses.spells.mimicqueen;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Color;
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

public class SpellMultihitHeal extends Spell {

	private static final int HEALTH_HEALED = 30;

	private static final Particle.DustOptions RED_COLOR = new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.0f);

	private Plugin mPlugin;
	private LivingEntity mBoss;

	public SpellMultihitHeal(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 3, 0.25f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 3, 0.65f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 3, 0.5f);

		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 4, 2));

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;
				if (mTicks >= 30) {
					this.cancel();

					new BukkitRunnable() {
						int mCount = 0;

						@Override
						public void run() {
							mCount++;
							Location loc = mBoss.getLocation();

							new PartialParticle(Particle.CLOUD, mBoss.getLocation(), 5, 1, 1, 1, 0.25).spawnAsEntityActive(mBoss);
							world.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 3, 0.75f);
							for (int deg = 0; deg < 360; deg += 45) {
								new PartialParticle(Particle.EXPLOSION_NORMAL, loc.clone().add(3 * FastUtils.cos(deg), 0, 3 * FastUtils.sin(deg)), 4, 1, 1.5, 1, 0).spawnAsEntityActive(mBoss);
								new PartialParticle(Particle.SWEEP_ATTACK, loc.clone().add(3 * FastUtils.cos(deg), 0, 3 * FastUtils.sin(deg)), 3, 1, 1.5, 1, 0).spawnAsEntityActive(mBoss);
							}

							for (Player player : PlayerUtils.playersInRange(loc, 4, true)) {
								player.damage(15, mBoss);

								double originalHealth = mBoss.getHealth();
								if (originalHealth <= 0) {
									break;
								}

								//Heal Mimic Queen by 15 health on hit
								double hp = originalHealth + HEALTH_HEALED;
								double max = EntityUtils.getMaxHealth(mBoss);
								if (hp >= max) {
									mBoss.setHealth(max);
								} else {
									mBoss.setHealth(hp);
								}
								world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.HOSTILE, 1, 1.25f);
								world.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.HOSTILE, 1, 2f);
								new PartialParticle(Particle.REDSTONE, loc.clone().add(0, 1, 0), 5, 0.15, 0.15, 0.15, RED_COLOR).spawnAsEntityActive(mBoss);
							}

							if (mCount > 5) {
								this.cancel();
							}
						}

					}.runTaskTimer(mPlugin, 0, 5);
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 4;
	}

}
