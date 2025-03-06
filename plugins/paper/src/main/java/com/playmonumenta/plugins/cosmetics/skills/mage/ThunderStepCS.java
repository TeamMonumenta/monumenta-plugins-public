package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ThunderStepCS implements CosmeticSkill {
	public static final Particle.DustOptions DUST_GRAY_LARGE = new Particle.DustOptions(Color.fromRGB(111, 111, 111), 1.25f);
	private @Nullable BukkitRunnable mRunnable = null;

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.THUNDER_STEP;
	}

	@Override
	public Material getDisplayItem() {
		return Material.HORN_CORAL;
	}

	public static final Particle.DustOptions COLOR_YELLOW = new Particle.DustOptions(Color.YELLOW, 0.75f);
	public static final Particle.DustOptions COLOR_AQUA = new Particle.DustOptions(Color.AQUA, 0.75f);

	public void castEffect(Player player, double ratio) {
		Location location = player.getLocation();
		World world = location.getWorld();
		world.playSound(location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1f, 1.5f);
		new PartialParticle(Particle.REDSTONE, location, (int) (100 * ratio * ratio), 2.5 * ratio, 2.5 * ratio, 2.5 * ratio, 3, COLOR_YELLOW).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, location, (int) (100 * ratio * ratio), 2.5 * ratio, 2.5 * ratio, 2.5 * ratio, 3, COLOR_AQUA).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FLASH, location.clone().add(location.getDirection()), 1, 0, 0, 0, 10)
			.minimumCount(1).spawnAsPlayerActive(player);
	}

	public void trailEffect(Player player, Location startLoc, Location endLoc) {
		new PPLine(Particle.END_ROD, startLoc.add(0, 1, 0), endLoc.add(0, 1, 0)).countPerMeter(2).minParticlesPerMeter(0).delta(0).extra(0).spawnAsPlayerActive(player);
	}

	public void lingeringEffect(Plugin plugin, Player player, Location startLoc, int duration) {
		if (mRunnable == null || mRunnable.isCancelled()) {
			mRunnable = new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					new PartialParticle(
						Particle.REDSTONE, startLoc.clone(), 5, 0.4, 0.2, 0.4, 0, DUST_GRAY_LARGE).spawnAsPlayerActive(player);
					if (mTicks % 3 == 0) {
						for (int i = 0; i < 3; i++) {
							sparkParticle(player, startLoc.clone(), VectorUtils.randomUnitVector().multiply(FastUtils.randomDoubleInRange(1, 2)), 0.5f);
						}
					}
					new PartialParticle(Particle.REDSTONE, player.getLocation().add(0, 1, 0), 2, 0.5, 0.5, 0.5, 0.1, COLOR_YELLOW).spawnAsPlayerActive(player);

					if (mTicks >= duration) {
						this.cancel();
						mTicks = 0;
						mRunnable = null;
					}
					mTicks++;
				}
			};
			mRunnable.runTaskTimer(plugin, 0, 1);
		}
	}

	public void onDamage(Player player, LivingEntity enemy, int mobParticles) {
		Location enemyParticleLocation = enemy.getLocation().add(0, enemy.getHeight() / 2, 0);
		new PartialParticle(Particle.CLOUD, enemyParticleLocation, mobParticles, 0.5, 0.5, 0.5, 0.5).spawnAsPlayerActive(player);
		new PartialParticle(Particle.END_ROD, enemyParticleLocation, mobParticles, 0.5, 0.5, 0.5, 0.5).spawnAsPlayerActive(player);
	}

	private void sparkParticle(Player player, Location loc, Vector dir, float size) {
		Location location = loc.clone();
		Vector direction = dir.clone();

		for (int i = 0; i < 3; i++) {
			Location oldLocation = location.clone();
			location.add(direction.multiply(0.3)).add(FastUtils.randomDoubleInRange(-0.5, 0.5), FastUtils.randomDoubleInRange(-0.5, 0.5), FastUtils.randomDoubleInRange(-0.5, 0.5));

			new PPLine(Particle.DUST_COLOR_TRANSITION, oldLocation, location).data(new Particle.DustTransition(Color.YELLOW, Color.YELLOW, size))
				.countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
		}
	}

	public void playerTeleportedBack() {
		if (mRunnable != null) {
			mRunnable.cancel();
			mRunnable = null;
		}
	}
}
