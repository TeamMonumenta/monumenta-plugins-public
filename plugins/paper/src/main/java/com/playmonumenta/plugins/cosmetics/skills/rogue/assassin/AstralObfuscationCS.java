package com.playmonumenta.plugins.cosmetics.skills.rogue.assassin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.List;
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

public class AstralObfuscationCS extends CloakAndDaggerCS {
	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"The stars conceal and protect,",
			"anticipating the ideal time to strike."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.PURPLE_GLAZED_TERRACOTTA;
	}

	@Override
	public @Nullable String getName() {
		return "Astral Obfuscation";
	}

	@Override
	public void applyStealthCosmetic(Player player) {
		// Intentionally empty, all effects placed in castEffects() since its functionally equivalent for this skill.
	}

	@Override
	public void castEffects(Player player) {
		World world = player.getWorld();
		Location loc = player.getLocation();

		new PartialParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 15, 0.25, 0.5, 0.25, 0.1f).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT_MAGIC, loc.clone().add(0, 1, 0), 25, 0.3, 0.5, 0.3, 0.5f).spawnAsPlayerActive(player);
		new PPCircle(Particle.END_ROD, loc.clone().add(0, 0.2, 0), 2).count(10).spawnAsPlayerActive(player);

		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 0.8f, 1.4f);
		new BukkitRunnable() {
			@Override
			public void run() {
				world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 0.8f, 2f);
			}
		}.runTaskLater(Plugin.getInstance(), 6);
	}

	@Override
	public void removeStealthCosmetic(Player player) {
		World world = player.getWorld();
		Location loc = player.getLocation();

		new PartialParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 15, 0.25, 0.5, 0.25, 0.1f).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT_MAGIC, loc.clone().add(0, 1, 0), 25, 0.3, 0.5, 0.3, 0.5f).spawnAsPlayerActive(player);
		new PPCircle(Particle.END_ROD, loc.clone().add(0, 0.2, 0), 2).count(10).spawnAsPlayerActive(player);

		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 1f, 2f);
		new BukkitRunnable() {
			@Override
			public void run() {
				world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 1f, 0.8f);
			}
		}.runTaskLater(Plugin.getInstance(), 4);
	}

	@Override
	public void activationEffects(Player player, LivingEntity enemy) {
		Location enemyLocation = enemy.getLocation().clone();
		double widthMultiplier = enemy.getWidth() + 0.7;
		double heightMultiplier = enemy.getEyeHeight() + 0.6 * widthMultiplier;

		new PPCircle(Particle.CRIT_MAGIC, enemyLocation.clone().add(0, 0.2, 0), widthMultiplier).delta(0.03).count(40).spawnAsPlayerActive(player);

		double randomHaloAngle = FastUtils.randomDoubleInRange(0, 2 * Math.PI / 5);
		for (int i = 0; i < 5; i++) {
			drawStar(enemy.getEyeLocation().add(new Vector(FastUtils.cos(randomHaloAngle + 2 * i * Math.PI / 5), 0.6, FastUtils.sin(randomHaloAngle + 2 * i * Math.PI / 5)).multiply(widthMultiplier)), player);
		}

		new BukkitRunnable() {
			double mAngle = Math.PI / 4;

			@Override
			public void run() {
				new PPCircle(Particle.CRIT_MAGIC, enemyLocation.clone().add(0, 0.2, 0), 4 * (Math.PI - mAngle) * widthMultiplier / (3 * Math.PI)).delta(0.03).count(20).spawnAsPlayerActive(player);
				for (int i = 0; i < 5; i++) {
					for (int j = 0; j < 4; j++) {
						Vector rotatedPoint = new Vector(0.5858 * widthMultiplier * (1 + FastUtils.cos(mAngle)), 1.4142 * heightMultiplier * FastUtils.sin(mAngle), 0).rotateAroundY(randomHaloAngle + 2 * i * Math.PI / 5);
						drawCosmic(player, enemyLocation.clone().add(rotatedPoint));
						mAngle += Math.PI / 64;
					}
					mAngle -= Math.PI / 16;
				}
				mAngle += Math.PI / 16;
				if (mAngle >= Math.PI) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

		Location loc = enemy.getLocation();
		World world = player.getWorld();
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.PLAYERS, 1.25f, 2f);
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1.25f, 2f);
	}

	private void drawStar(Location location, Player player) {
		new PartialParticle(Particle.END_ROD, location, 1, 0, 0, 0, 0)
			.spawnAsPlayerActive(player);
	}

	private void drawCosmic(Player player, Location loc) {
		if (FastUtils.randomIntInRange(0, 3) == 0) {
			new PartialParticle(Particle.CRIT_MAGIC, loc, 3, 0.1, 0.1, 0.1, 0).spawnAsPlayerActive(player);
		} else {
			new PartialParticle(Particle.REDSTONE, loc, 5, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(rollCosmicColor(), 0.6f)).spawnAsPlayerActive(player);
		}
	}

	private Color rollCosmicColor() {
		return Color.fromRGB(80 + FastUtils.randomIntInRange(0, 160), 80, 200);
	}
}
