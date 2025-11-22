package com.playmonumenta.plugins.cosmetics.skills.mage.arcanist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class ScissionCS extends CosmicMoonbladeCS {
	public static final String NAME = "Scission";
	private int mCombo = 0;
	private static final BlockData REDSTONE_BLOCK = Material.REDSTONE_BLOCK.createBlockData();

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Within your heart is a boundless hunger.",
			"Dismantle all meager vestiges of harmony.",
			"Cleave your way through reality itself.");
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	private static final Color BLACK = Color.fromRGB(0, 0, 0);
	private static final Color DARK_RED = Color.fromRGB(48, 2, 22);
	private static final Color LIGHTER_RED = Color.fromRGB(102, 6, 32);
	private static final Color ROSE = Color.fromRGB(204, 10, 49);

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.COSMIC_MOONBLADE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.NETHERITE_SWORD;
	}

	@Override
	public void moonbladeSwingEffect(World world, Player player, Location origin, double range, int swings, int maxSwing) {

		switch (mCombo) {
			case 0 -> {
				ParticleUtils.drawCleaveArc(origin.clone().add(0, 1, 0), 1.5, 210, -20, 190, 3, 0, 0, 0.2, 40,
					(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
						new Particle.DustOptions(
							ParticleUtils.getTransition(BLACK, LIGHTER_RED, ring / 3D), 1f)
					).spawnAsPlayerActive(player), 4);

				if (range * 0.4 >= 2.6) {
					Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> ParticleUtils.drawCleaveArc(origin.clone().add(0, 1, 0), range * 0.4, 210, 45, 135, (int) (0.5 * range), 0, 0, 0.2, 90,
						(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
							new Particle.DustOptions(
								ParticleUtils.getTransition(BLACK, DARK_RED, (double) ring / (int) (0.5 * range)), 0.6f)
						).spawnAsPlayerActive(player), 3), 2);
				}

				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> ParticleUtils.drawCleaveArc(origin.clone().add(0, 1, 0), range * 0.5, 210, 45, 135, (int) (0.5 * range), 0, 0, 0.2, 90,
					(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
						new Particle.DustOptions(
							ParticleUtils.getTransition(BLACK, LIGHTER_RED, (double) ring / (int) (0.5 * range)), 0.7f)
					).spawnAsPlayerActive(player), 3), 2);

				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> ParticleUtils.drawCleaveArc(origin.clone().add(0, 1, 0), range * 0.65, 210, 45, 135, (int) (0.5 * range), 0, 0, 0.2, 90,
					(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
						new Particle.DustOptions(
							ParticleUtils.getTransition(DARK_RED, LIGHTER_RED, (double) ring / (int) (0.5 * range)), 0.9f)
					).spawnAsPlayerActive(player), 3), 4);

				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> ParticleUtils.drawCleaveArc(origin.clone().add(0, 1, 0), range * 0.8, 210, 45, 135, (int) range, 0, 0, 0.2, 90,
					(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
						new Particle.DustOptions(
							ParticleUtils.getTransition(DARK_RED, ROSE, (double) ring / (int) range), 1f + (ring * 0.1f))
					).spawnAsPlayerActive(player), 3), 4);
				if (swings >= maxSwing) {
					mCombo = 0;
				} else {
					mCombo = 1;
				}
			}

			case 1 -> {
				ParticleUtils.drawCleaveArc(origin.clone().add(0, 1, 0), 1.5, -20, -10, 225, 3, 0, 0, 0.2, 40,
					(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
						new Particle.DustOptions(
							ParticleUtils.getTransition(BLACK, LIGHTER_RED, ring / 3D), 1f)
					).spawnAsPlayerActive(player), 4);

				if (range * 0.4 >= 2.6) {
					Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> ParticleUtils.drawCleaveArc(origin.clone().add(0, 1, 0), range * 0.4, -20, 45, 135, (int) (0.5 * range), 0, 0, 0.15, 90,
						(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
							new Particle.DustOptions(
								ParticleUtils.getTransition(BLACK, DARK_RED, (double) ring / (int) (0.5 * range)), 0.6f)
						).spawnAsPlayerActive(player), 3), 2);
				}

				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> ParticleUtils.drawCleaveArc(origin.clone().add(0, 1, 0), range * 0.5, -20, 45, 135, (int) (0.5 * range), 0, 0, 0.2, 90,
					(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
						new Particle.DustOptions(
							ParticleUtils.getTransition(BLACK, LIGHTER_RED, (double) ring / (int) (0.5 * range)), 0.7f)
					).spawnAsPlayerActive(player), 3), 2);

				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> ParticleUtils.drawCleaveArc(origin.clone().add(0, 1, 0), range * 0.65, -20, 45, 135, (int) (0.5 * range), 0, 0, 0.2, 90,
					(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
						new Particle.DustOptions(
							ParticleUtils.getTransition(DARK_RED, LIGHTER_RED, (double) ring / (int) (0.5 * range)), 0.9f)
					).spawnAsPlayerActive(player), 3), 4);

				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> ParticleUtils.drawCleaveArc(origin.clone().add(0, 1, 0), range * 0.8, -20, 45, 135, (int) range, 0, 0, 0.2, 90,
					(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
						new Particle.DustOptions(
							ParticleUtils.getTransition(DARK_RED, ROSE, (double) ring / (int) range), 1f + (ring * 0.1f))
					).spawnAsPlayerActive(player), 3), 4);

				if (swings >= maxSwing) {
					mCombo = 0;
				} else {
					mCombo = 2;
				}
			}

			default -> {
				ParticleUtils.drawCleaveArc(origin.clone().add(0, 1, 0), 1.5, 10, -80, 220, 3, 0, 0, 0.2, 60,
					(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
						new Particle.DustOptions(
							ParticleUtils.getTransition(BLACK, LIGHTER_RED, ring / 3D), 1f)
					).spawnAsPlayerActive(player), 4);

				if (range * 0.4 >= 2.6) {
					Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> ParticleUtils.drawCleaveArc(origin.clone().add(0, 1, 0), range * 0.4, 10, 45, 135, (int) (0.5 * range), 0, 0, 0.15, 90,
						(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
							new Particle.DustOptions(
								ParticleUtils.getTransition(BLACK, DARK_RED, (double) ring / (int) (0.5 * range)), 0.6f)
						).spawnAsPlayerActive(player), 3), 2);
				}

				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> ParticleUtils.drawCleaveArc(origin.clone().add(0, 1, 0), range * 0.5, 10, 45, 135, (int) (0.5 * range), 0, 0, 0.2, 90,
					(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
						new Particle.DustOptions(
							ParticleUtils.getTransition(BLACK, LIGHTER_RED, (double) ring / (int) (0.5 * range)), 0.7f)
					).spawnAsPlayerActive(player), 3), 2);

				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> ParticleUtils.drawCleaveArc(origin.clone().add(0, 1, 0), range * 0.65, 10, 45, 135, (int) (0.5 * range), 0, 0, 0.2, 90,
					(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
						new Particle.DustOptions(
							ParticleUtils.getTransition(DARK_RED, LIGHTER_RED, (double) ring / (int) (0.5 * range)), 0.9f)
					).spawnAsPlayerActive(player), 3), 4);

				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> ParticleUtils.drawCleaveArc(origin.clone().add(0, 1, 0), range * 0.8, 10, 45, 135, (int) range, 0, 0, 0.2, 90,
					(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
						new Particle.DustOptions(
							ParticleUtils.getTransition(DARK_RED, ROSE, (double) ring / (int) range), 1f + (ring * 0.1f))
					).spawnAsPlayerActive(player), 3), 4);

				mCombo = 0;
			}
		}

		world.playSound(origin, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1f, 0.5f);
		world.playSound(origin, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.0f, 0.75f);

		if (swings >= maxSwing) {
			new PartialParticle(Particle.BLOCK_CRACK, LocationUtils.getHalfHeightLocation(player), 75, 0.25, 0.25, 0.25, 1, REDSTONE_BLOCK).spawnAsPlayerActive(player);
			ParticleUtils.drawParticleCircleExplosion(player, origin.clone().add(0, 0.2, 0), 0, 1, -player.getLocation().getYaw(), -player.getLocation().getPitch(),
				50, 0.65f, true, 0, 0, Particle.SQUID_INK);
			ParticleUtils.drawParticleCircleExplosion(player, origin.clone().add(0, 0.2, 0), 0, 1, -player.getLocation().getYaw(), -player.getLocation().getPitch(),
				30, 0.5f, true, 0, 0, Particle.SQUID_INK);

			world.playSound(origin, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 0.9f, 1.1f);
			world.playSound(origin, Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 0.85f, 0.8f);
			world.playSound(origin, Sound.ENTITY_SQUID_SQUIRT, SoundCategory.PLAYERS, 0.85f, 1f);
		} else {
			world.playSound(origin, Sound.ENTITY_SQUID_SQUIRT, SoundCategory.PLAYERS, 0.85f, 1.5f);
			world.playSound(origin, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 1.0f, 1.5f);
			world.playSound(origin, Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 0.85f, 1f);
		}

	}
}
