package com.playmonumenta.plugins.bosses.spells.spells_kaul;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellEarthsWrath extends Spell {
	private Plugin mPlugin;
	private LivingEntity mBoss;
	private double mY;

	public SpellEarthsWrath(Plugin plugin, LivingEntity boss, double y) {
		mPlugin = plugin;
		mBoss = boss;
		mY = y;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		new BukkitRunnable() {
			int t = 0;
			@Override
			public void run() {
				t++;
				if (t % 2 == 0) {
					world.playSound(mBoss.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 2, 1);
				}
				world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation(), 2, 0.25, 0.1, 0.25, 0.25);
				if (t >= 20 * 2.15) {
					this.cancel();
					world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, 2, 1);
					Location loc = mBoss.getLocation().add(0, 0.25, 0);
					loc.setY(mY);
					for (int i = 0; i < 48; i++) {
						int j = i;
						new BukkitRunnable() {
							BoundingBox box = BoundingBox.of(loc, 0.75, 0.4, 0.75);
							double radian1 = Math.toRadians((7.5 * j));
							Location point = loc.clone().add(Math.cos(radian1) * 0.5, 0, Math.sin(radian1) * 0.5);
							Vector dir = LocationUtils.getDirectionTo(point, loc);
							int t = 0;
							@Override
							public void run() {
								t++;
								box.shift(dir.clone().multiply(0.45));
								Location bLoc = box.getCenter().toLocation(world);
								world.spawnParticle(Particle.DAMAGE_INDICATOR, bLoc, 1, 0.25, 0.25, 0.25, 0);
								world.spawnParticle(Particle.CLOUD, bLoc, 1, 0, 0, 0, 0);
								for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), 40)) {
									if (player.getBoundingBox().overlaps(box)) {
										BossUtils.bossDamage(mBoss, player, 24);
										MovementUtils.knockAway(mBoss.getLocation(), player, -0.5f, 0.65f);
										player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 10, 2));
										player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20 * 10, -4));
									}
								}
								if (t >= 20 * 3) {
									this.cancel();
								}
							}

						}.runTaskTimer(mPlugin, 0, 1);
					}
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}



	@Override
	public int duration() {
		return 20 * 12;
	}

}
