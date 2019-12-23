package com.playmonumenta.plugins.bosses.spells.spells_cluckingop;

import java.util.List;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellEruption extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;

	public SpellEruption(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), 30);
		new BukkitRunnable() {
			int t = 0;
			@Override
			public void run() {
				t++;
				world.spawnParticle(Particle.LAVA, mBoss.getLocation(), 20, 0.15, 0, 0.15, 0.175);
				world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.5f, 1f);
				for (Player player : players) {
					world.spawnParticle(Particle.LAVA, player.getLocation(), 10, 0.15, 0, 0.15, 0.175);
				}

				if (t >= 3) {
					this.cancel();
					for (Player player : players) {
						player.setVelocity(new Vector(0, 2, 0));
						player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 60, 10));
						world.spawnParticle(Particle.FLAME, player.getLocation(), 150, 0, 0, 0, 0.175);
						world.spawnParticle(Particle.SMOKE_LARGE, player.getLocation(), 75, 0, 0, 0, 0.25);
						world.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
						player.damage(1, mBoss);
						new BukkitRunnable() {

							@Override
							public void run() {
								player.damage(1, mBoss);
								world.spawnParticle(Particle.FLAME, player.getLocation(), 150, 0, 0, 0, 0.175);
								world.spawnParticle(Particle.SMOKE_LARGE, player.getLocation(), 75, 0, 0, 0, 0.25);
								world.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
								player.setVelocity(new Vector(0, -2, 0));
								new BukkitRunnable() {

									@Override
									public void run() {
										world.spawnParticle(Particle.SMOKE_LARGE, player.getLocation(), 1, 0, 0, 0, 0.05);
										if (player.isOnGround() || player.isDead() || !player.isValid() || !player.isOnline()) {
											this.cancel();
											new BukkitRunnable() {

												@Override
												public void run() {
													player.damage(1, mBoss);
													world.spawnParticle(Particle.FLAME, player.getLocation(), 150, 0, 0, 0, 0.175);
													world.spawnParticle(Particle.SMOKE_LARGE, player.getLocation(), 75, 0, 0, 0, 0.25);
													world.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
													world.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_DEATH, 1, 0);
												}

											}.runTaskLater(mPlugin, 1);
										}
									}

								}.runTaskTimer(mPlugin, 0, 1);
							}

						}.runTaskLater(mPlugin, 15);
					}
				}
			}

		}.runTaskTimer(mPlugin, 0, 15);
	}

	@Override
	public int duration() {
		return 20 * 5;
	}
}
