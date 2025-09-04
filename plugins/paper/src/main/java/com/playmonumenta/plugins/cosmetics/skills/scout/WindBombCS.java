package com.playmonumenta.plugins.cosmetics.skills.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;

public class WindBombCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.WIND_BOMB;
	}

	@Override
	public Material getDisplayItem() {
		return Material.TNT;
	}

	public void onThrow(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_DROWNED_SHOOT, SoundCategory.PLAYERS, 0.3f, 0.1f);
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 2.0f, 0.8f);
	}

	public void onLand(Player player, World world, Location loc, double radius) {
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 2.0f, 1.4f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.4f, 1.1f);
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 2.0f, 0.8f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.7f, 0.1f);
		new PPCircle(Particle.CLOUD, loc, radius)
			.extra(0.125)
			.count(150)
			.spawnAsPlayerActive(player);
	}

	public void onVortexSpawn(Player player, World world, Location loc, double enhancePullDuration) {
		new PartialParticle(Particle.CLOUD, loc, 35, 4, 4, 4, 0.125).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 25, 2, 2, 2, 0.125).spawnAsPlayerActive(player);
		world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.8f, 1f);
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				world.playSound(loc, Sound.ENTITY_BREEZE_IDLE_GROUND, SoundCategory.PLAYERS, 1.6f, 0.5f);
				mTicks += 10;
				if (mTicks >= enhancePullDuration) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 10);
	}

	public void onVortexTick(Player player, Location loc, double radius, int tick) {
		new PartialParticle(Particle.CLOUD, loc, 3, 1, 2, 1).spawnAsPlayerActive(player);

		if (tick % 10 == 0) {
			new BukkitRunnable() {
				int mTicks = 1;

				@Override
				public void run() {
					double multiplier = Math.pow((4 - mTicks) / 3.0, 1.6);

					new PPCircle(Particle.FIREWORKS_SPARK, loc.clone().subtract(new Vector(0, mTicks / 2.0, 0)), radius * multiplier)
						.count(30)
						.directionalMode(true).rotateDelta(true)
						.delta(-multiplier, 0, 0.15)
						.extra(0.75)
						.extraVariance(0.1)
						.spawnAsPlayerActive(player);
					mTicks++;
					if (mTicks > 3) {
						this.cancel();
					}
				}
			}.runTaskTimer(Plugin.getInstance(), 0, 1);
		}
	}

	public String getProjectileName() {
		return "Wind Bomb Projectile";
	}

	public @Nullable Particle getProjectileParticle() {
		return Particle.CLOUD;
	}
}
