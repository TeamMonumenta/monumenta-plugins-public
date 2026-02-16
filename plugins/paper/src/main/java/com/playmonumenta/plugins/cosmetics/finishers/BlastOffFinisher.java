package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class BlastOffFinisher implements EliteFinisher {
	public static final String NAME = "Blast Off";

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		if (!(killedMob instanceof LivingEntity le)) {
			return;
		}

		new BukkitRunnable() {
			int mTicks = 0;
			final World mWorld = le.getWorld();
			final Location mCenterLoc = LocationUtils.getHalfHeightLocation(killedMob);
			@Nullable LivingEntity mClonedKilledMob;

			@Override
			public void run() {
				if (mTicks == 0) {
					mWorld.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.8f, 2f);
					mWorld.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.8f, 0.5f);
					mWorld.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1f, 0.55f);
					mWorld.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, SoundCategory.PLAYERS, 1f, 0.65f);

					mClonedKilledMob = EliteFinishers.createClonedMob(le, p, NamedTextColor.GOLD, true, true, true);
					mClonedKilledMob.setHealth(0);
					killedMob.remove();

					new PPExplosion(Particle.FLAME, mCenterLoc)
						.extra(0.4)
						.count(50)
						.spawnAsPlayerActive(p);

					new PPExplosion(Particle.CLOUD, mCenterLoc)
						.extra(0.4)
						.count(75)
						.spawnAsPlayerActive(p);

					new PartialParticle(Particle.GUST, mCenterLoc, 15, 1.5, 1.5, 1.5).spawnAsPlayerActive(p);

					Vector vel = LocationUtils.getDirectionTo(mClonedKilledMob.getLocation(), p.getLocation()).setY(3).normalize();
					vel.multiply(5);
					mClonedKilledMob.setVelocity(vel);
				}
				if (mClonedKilledMob != null) {
					new PartialParticle(Particle.CAMPFIRE_COSY_SMOKE, mClonedKilledMob.getLocation(), 5, 0.2, 0.2, 0.2, 0.05).spawnAsPlayerActive(p);

					if (mTicks >= 19) {
						mWorld.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.9f, 2f);
						mWorld.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.9f, 1.5f);
						mWorld.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1f, 1.25f);

						new PPExplosion(Particle.WAX_OFF, mClonedKilledMob.getLocation())
							.extra(10)
							.count(75)
							.spawnAsPlayerActive(p);

						this.cancel();
					}
					mTicks++;
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public Material getDisplayItem() {
		return Material.TNT;
	}
}
