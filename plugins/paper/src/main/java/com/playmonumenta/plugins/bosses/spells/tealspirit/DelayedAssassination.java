package com.playmonumenta.plugins.bosses.spells.tealspirit;

import com.playmonumenta.plugins.bosses.spells.SpellTpBehindPlayer;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class DelayedAssassination extends SpellTpBehindPlayer {
	int GROWTH_TICKS = 30;
	double MAX_RADIUS = 4;
	double DAMAGE = 150;

	public DelayedAssassination(Plugin plugin, LivingEntity launcher, int cooldown) {
		super(plugin, launcher, cooldown, 20, 40, 10, true);
	}

	@Override
	protected void onTeleport(Player player) {
		mLauncher.setAI(false);
		Location location = player.getLocation();
		World world = location.getWorld();
		world.playSound(location, Sound.BLOCK_CAMPFIRE_CRACKLE, 2.0f, 0.3f);
		world.playSound(location, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.5f, 0.7f);
		location.setY(location.getBlockY());
		PPCircle indicator = new PPCircle(Particle.REDSTONE, location, 0).ringMode(true).count(25).delta(0.1, 0.05, 0.1).data(new Particle.DustOptions(Color.fromRGB(214, 58, 166), 1.65f));
		PPCircle indicator2 = new PPCircle(Particle.DRAGON_BREATH, location, 0).ringMode(true).count(15).delta(0.25, 0.1, 0.25).extra(0.03);
		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				double radius;

				if (mTicks < GROWTH_TICKS) {
					radius = mTicks * MAX_RADIUS / GROWTH_TICKS;
				} else {
					for (Player p : PlayerUtils.playersInRange(location, MAX_RADIUS, true)) {
						DamageUtils.damage(mLauncher, p, DamageEvent.DamageType.MAGIC, DAMAGE, null, false, false, "Delayed Assassination");
					}
					world.playSound(location, Sound.BLOCK_SHROOMLIGHT_BREAK, 2.0f, 0.7f);
					radius = MAX_RADIUS;
					this.cancel();
				}

				indicator.radius(radius).spawnAsBoss();
				indicator2.radius(radius).spawnAsBoss();

				mTicks += 5;
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				mLauncher.setAI(true);
			}
		};
		mActiveRunnables.add(runnable);
		runnable.runTaskTimer(com.playmonumenta.plugins.Plugin.getInstance(), 0, 5);
	}

	@Override
	protected void animation(Player target) {
		target.getWorld().playSound(target.getLocation(), Sound.BLOCK_BELL_RESONATE, 2.0f, 1.0f);

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;
				Location particleLoc = mLauncher.getLocation().add(new Location(mLauncher.getWorld(), -0.5f, 0f, 0.5f));
				particleLoc.getWorld().spawnParticle(Particle.PORTAL, particleLoc, 10, 1, 1, 1, 0.03);

				if (mTicks > 40) {
					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(com.playmonumenta.plugins.Plugin.getInstance(), 0, 1);
		mActiveRunnables.add(runnable);
	}
}
