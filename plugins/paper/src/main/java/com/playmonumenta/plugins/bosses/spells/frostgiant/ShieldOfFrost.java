package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/*
 Shield of Frost - The frost giant gains a shield that absorbs the next
 100 + (4 * (amount of players * 2)) damage and applies
 slowness 3 and deals knockback to the attacker for 5 seconds. If the shield
 expires naturally it explodes dealing 28 damage in an 18 block radius.
 Expires after 15 seconds.

 1 level of Absorption = 4 health
 */
public class ShieldOfFrost extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;

	public ShieldOfFrost(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), 50, true);
		world.playSound(mBoss.getLocation(), Sound.BLOCK_GLASS_BREAK, 3, 0.5f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 3, 1.5f);
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 15, 37 + (players.size() * 3)));
		new BukkitRunnable() {
			int mTicks = 0;
			boolean mShatter = false;

			@Override
			public void run() {
				mTicks++;
				world.spawnParticle(Particle.SPIT, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 6, mBoss.getWidth() / 2, mBoss.getHeight() / 2, mBoss.getWidth() / 2, 0);
				world.spawnParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 12, mBoss.getWidth() / 2, mBoss.getHeight() / 2, mBoss.getWidth() / 2, 0);
				if (mBoss.getAbsorptionAmount() <= 0) {
					this.cancel();
					mBoss.removePotionEffect(PotionEffectType.ABSORPTION);
					world.playSound(mBoss.getLocation(), Sound.BLOCK_GLASS_BREAK, 3, 0.5f);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 3, 0.5f);
					world.spawnParticle(Particle.SPIT, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 150, mBoss.getWidth() / 2, mBoss.getHeight() / 2, mBoss.getWidth() / 2, 0.5);
					world.spawnParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 250, mBoss.getWidth() / 2, mBoss.getHeight() / 2, mBoss.getWidth() / 2, 0.65);
				}
				if (mTicks >= 9 * 20 && mTicks % 2 == 0) {
					world.playSound(mBoss.getLocation(), Sound.BLOCK_GLASS_BREAK, 3, 1.5f);
					world.spawnParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation(), 2, 1, 1, 1, 0.5);
					if (!mShatter) {
						mShatter = true;
						PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), 50, "tellraw @s [\"\",{\"text\":\"The frost shield's energy is becoming unstable!\",\"color\":\"aqua\"}]");
					}
				}
				if (mTicks >= 20 * 12 && mBoss.getAbsorptionAmount() > 0) {
					this.cancel();
					world.spawnParticle(Particle.SPIT, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 100, mBoss.getWidth() / 2, mBoss.getHeight() / 2, mBoss.getWidth() / 2, 0.5);
					world.spawnParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 150, mBoss.getWidth() / 2, mBoss.getHeight() / 2, mBoss.getWidth() / 2, 0.65);
					new BukkitRunnable() {
						Location mLoc = mBoss.getLocation();
						double mRadius = 0;

						@Override
						public void run() {
							for (double i = 0; i < 360; i += 15) {
								double radian1 = Math.toRadians(i);
								double cos = FastUtils.cos(radian1);
								double sin = FastUtils.sin(radian1);

								for (int j = 0; j < 2; j++) {
									mRadius += 1.5;
									mLoc.add(cos * mRadius, 0, sin * mRadius);
									world.spawnParticle(Particle.FIREWORKS_SPARK, mLoc, 2, 0, 0, 0, 0.3);
									world.spawnParticle(Particle.CLOUD, mLoc, 1, 0, 0, 0, 0.25);
									mLoc.subtract(cos * mRadius, 0, sin * mRadius);
								}
							}
							if (mRadius >= 20) {
								this.cancel();
							}
						}

					}.runTaskTimer(mPlugin, 0, 1);
					world.playSound(mBoss.getLocation(), Sound.BLOCK_GLASS_BREAK, 3, 0.5f);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 3, 0.5f);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 3, 0.5f);
					mBoss.removePotionEffect(PotionEffectType.ABSORPTION);
					for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), 18, true)) {
						player.damage(24, mBoss);
						player.removePotionEffect(PotionEffectType.SLOW);
						player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 15, 2));
					}
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public void onHurtByEntity(DamageEvent event, Entity damager) {
		if (mBoss.getAbsorptionAmount() > 0 && damager instanceof Player player) {
			World world = mBoss.getWorld();
			player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 5, 2));
			world.spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation().add(0, 1, 0), 12, 0.4, 0.4, 0.4, 0.15);
			world.spawnParticle(Particle.SPIT, player.getLocation().add(0, 1, 0), 3, 0.4, 0.4, 0.4, 0.2);
			world.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 0.5f);
		}
	}

	@Override
	public int cooldownTicks() {
		return 20 * 20;
	}
}
