package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.AbstractMap;
import java.util.List;
import org.apache.commons.math3.util.FastMath;
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

public class GruesomeEchoesCS extends GruesomeAlchemyCS {
	//Twisted theme

	public static final String NAME = "Gruesome Echoes";

	private static final Color TWISTED_COLOR = Color.fromRGB(127, 0, 0);
	private static final Color ECHO_COLOR = Color.fromRGB(39, 89, 97);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Infuses your alchemy with",
			"ghastly and twisted echoes.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.DRAGON_BREATH;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public Color getBrutalColor() {
		return TWISTED_COLOR;
	}

	@Override
	public Color getGruesomeColor() {
		return ECHO_COLOR;
	}

	@Override
	public void effectsOnSwap(Player mPlayer, boolean isGruesomeBeforeSwap) {
		if (!isGruesomeBeforeSwap) { // brutal -> gruesome, dark red
			spawnRing(mPlayer.getLocation(), mPlayer, ECHO_COLOR);
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 1, 1.25f);
		} else { // gruesome -> brutal, darker blue
			spawnRing(mPlayer.getLocation(), mPlayer, TWISTED_COLOR);
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 1, 0.75f);
		}
		new PartialParticle(Particle.SOUL, mPlayer.getLocation().clone().add(0, 1, 0), 10, 0.4, 0.4, 0.4, 0.02)
			.spawnAsPlayerActive(mPlayer);
		mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, SoundCategory.PLAYERS, 0.9f, 0.7f);
		mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, SoundCategory.PLAYERS, 2f, 0.5f);
	}

	@Override
	public void effectsOnSplash(Player mPlayer, Location loc, boolean isGruesome, double radius, boolean isSpecialPot) {
		loc = loc.clone().add(0, 0.1, 0);
		World world = loc.getWorld();
		world.playSound(loc, Sound.ENTITY_HUSK_STEP, SoundCategory.PLAYERS, 1f, 0.5f);
		world.playSound(loc, Sound.ENTITY_HUSK_STEP, SoundCategory.PLAYERS, 1f, 0.5f);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 35, 0, 0, 0, 0.125)
			.spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SOUL, loc, 15, 0, 0, 0, 0.075)
			.spawnAsPlayerActive(mPlayer);

		Vector colorValues = isGruesome ? new Vector(ECHO_COLOR.getRed(), ECHO_COLOR.getGreen(), ECHO_COLOR.getBlue()).normalize() :
			new Vector(TWISTED_COLOR.getRed(), TWISTED_COLOR.getGreen(), TWISTED_COLOR.getBlue()).normalize();
		ParticleUtils.explodingRingEffect(Plugin.getInstance(), loc, radius, 1, 3,
			List.of(
				new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(1.0, (Location location) -> {
					new PartialParticle(Particle.SPELL_MOB_AMBIENT, location, 1, colorValues.getX(), colorValues.getY(), colorValues.getZ(), 1).directionalMode(true).spawnAsPlayerActive(mPlayer);
				})
			)
		);
	}

	@Override
	public Color splashColor(boolean isGruesome) {
		if (isGruesome) {
			return ECHO_COLOR;
		} else {
			return TWISTED_COLOR;
		}
	}

	private void spawnRing(Location loc, Player mPlayer, Color color) {
		Location l = loc.clone().add(0, 0.1, 0);
		new BukkitRunnable() {

			double mRadius = 2.75;

			@Override
			public void run() {

				for (int i = 0; i < 3; i++) {
					mRadius -= 0.25;
					for (int degree = 0; degree < 360; degree += 9) {
						double radian = FastMath.toRadians(degree);
						Vector vec = new Vector(FastUtils.cos(radian) * mRadius, 0,
							FastUtils.sin(radian) * mRadius);
						Location loc = l.clone().add(vec);
						new PartialParticle(Particle.REDSTONE, loc, 1, 0, 0, 0, 0,
							new Particle.DustOptions(color, 0.75f))
							.spawnAsPlayerActive(mPlayer);
					}
					if (mRadius <= 0) {
						this.cancel();
						return;
					}
				}


			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void brutalDotTickEffects(LivingEntity target) {
		Location halfHeightLoc = LocationUtils.getHalfHeightLocation(target);
		new PartialParticle(Particle.BLOCK_CRACK, halfHeightLoc, 24)
			.delta(target.getBoundingBox().getWidthX() / 2, target.getBoundingBox().getHeight() / 2, target.getBoundingBox().getWidthZ() / 2)
			.data(Material.REDSTONE_BLOCK.createBlockData())
			.spawnAsEnemy();
		new PartialParticle(Particle.REDSTONE, halfHeightLoc, 36)
			.delta(target.getBoundingBox().getWidthX() / 2, target.getBoundingBox().getHeight() / 2, target.getBoundingBox().getWidthZ() / 2)
			.data(new Particle.DustOptions(TWISTED_COLOR, 1f))
			.spawnAsEnemy();
		new PartialParticle(Particle.SOUL, halfHeightLoc, 12)
			.delta(target.getBoundingBox().getWidthX() / 2, target.getBoundingBox().getHeight() / 2, target.getBoundingBox().getWidthZ() / 2)
			.spawnAsEnemy();
	}

	@Override
	public void brutalPeriodicEffects(LivingEntity target, int stacks, int maxStacks, int level) {
		if (stacks > maxStacks) {
			// Explosion will happen
			return;
		}

		Location currentLoc = LocationUtils.getHeightLocation(target, 1).add(0, 0.5, 0);
		int stacksLeft = stacks;
		while (stacksLeft > 0) {
			float size = 1f;
			int cost = 1;
			if (stacksLeft >= 5) {
				cost = 5;
				size = 1.75f;
			}
			new PartialParticle(Particle.REDSTONE, currentLoc)
				.count(5)
				.data(new Particle.DustOptions(TWISTED_COLOR, size))
				.spawnAsEnemy();
			stacksLeft -= cost;
			if (cost == 5 && stacksLeft >= 5) {
				// Prevent the next "big blob" from being too close
				currentLoc.add(0, 0.25, 0);
			}
			currentLoc.add(0, 0.5, 0);
		}
	}

	@Override
	public void brutalDotExplosionEffects(LivingEntity target) {
		target.getWorld().playSound(target.getLocation(), Sound.ENTITY_SQUID_SQUIRT, SoundCategory.HOSTILE, 1f, 0.7f);
		target.getWorld().playSound(target.getLocation(), Sound.ENTITY_STRAY_STEP, SoundCategory.HOSTILE, 1.25f, 0.5f);
		target.getWorld().playSound(target.getLocation(), Sound.ENTITY_HUSK_STEP, SoundCategory.HOSTILE, 1.25f, 0.5f);
		Location halfHeightLoc = LocationUtils.getHalfHeightLocation(target);
		new PartialParticle(Particle.BLOCK_CRACK, halfHeightLoc, 80)
			.delta(target.getBoundingBox().getWidthX() / 2, target.getBoundingBox().getHeight() / 2, target.getBoundingBox().getWidthZ() / 2)
			.data(Material.REDSTONE_BLOCK.createBlockData())
			.spawnAsEnemy();
		new PartialParticle(Particle.REDSTONE, halfHeightLoc, 120)
			.delta(target.getBoundingBox().getWidthX() / 2, target.getBoundingBox().getHeight() / 2, target.getBoundingBox().getWidthZ() / 2)
			.data(new Particle.DustOptions(TWISTED_COLOR, 1f))
			.spawnAsEnemy();
		new PartialParticle(Particle.SOUL, halfHeightLoc, 40)
			.delta(target.getBoundingBox().getWidthX() / 2, target.getBoundingBox().getHeight() / 2, target.getBoundingBox().getWidthZ() / 2)
			.spawnAsEnemy();
	}
}
