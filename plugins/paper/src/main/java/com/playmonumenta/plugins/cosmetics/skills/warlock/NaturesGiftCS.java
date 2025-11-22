package com.playmonumenta.plugins.cosmetics.skills.warlock;

import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
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
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class NaturesGiftCS extends SoulRendCS {

	private static final Particle.DustOptions YELLOW = new Particle.DustOptions(Color.fromRGB(255, 255, 0), 1f);
	private static final Particle.DustOptions MAGENTA = new Particle.DustOptions(Color.fromRGB(255, 0, 125), 1f);
	private static final Particle.DustOptions MAGENTA_SMALL = new Particle.DustOptions(Color.fromRGB(255, 0, 125), 0.6f);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Inside your enemy's wounds is a",
			"blossom waiting to be freed."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.FIRE_CORAL;
	}

	@Override
	public @Nullable String getName() {
		return "Nature's Gift";
	}

	@Override
	public void rendHitSound(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 0.5f, 0.9f);
		world.playSound(loc, Sound.ITEM_BOTTLE_FILL, SoundCategory.PLAYERS, 0.8f, 0.5f);
		world.playSound(loc, Sound.BLOCK_COMPOSTER_READY, SoundCategory.PLAYERS, 1f, 0.8f);
		world.playSound(loc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 0.4f, 1.3f);
		world.playSound(loc, Sound.ENTITY_PLAYER_SWIM, SoundCategory.PLAYERS, 0.6f, 1.6f);
		world.playSound(loc, Sound.ENTITY_TURTLE_DEATH, SoundCategory.PLAYERS, 1f, 0.7f);
		world.playSound(loc, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.PLAYERS, 1f, 1.7f);
		world.playSound(loc, Sound.ENTITY_GLOW_SQUID_DEATH, SoundCategory.PLAYERS, 0.7f, 1.7f);
	}

	@Override
	public void rendHitParticle(Player player, Location loc) {
		new PPParametric(Particle.REDSTONE, loc, (param, builder) -> {
			double x = 2 * FastUtils.cos(14 * Math.PI * param) * FastUtils.cos(4 * Math.PI * param);
			double z = 2 * FastUtils.cos(14 * Math.PI * param) * FastUtils.sin(4 * Math.PI * param);
			Vector vec = new Vector(x, 0.15, z);
			vec.setY(vec.lengthSquared() / 8);
			builder.location(loc.clone().add(vec));

			Color color = ParticleUtils.getTransition(YELLOW.getColor(), MAGENTA.getColor(), vec.lengthSquared() / 4);
			builder.data(new Particle.DustOptions(color, 0.8f));
		}).count(600).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SPELL_INSTANT, loc.clone().add(0, 0.2, 0), 12, 0.15, 0.15, 0.15, 1).spawnAsPlayerActive(player);
	}

	@Override
	public void rendMarkTick(Player player, LivingEntity enemy, int marks) {
		for (int i = 1; i <= marks; i++) {
			Location loc = getMarkLocation(player, enemy, i);

			drawRuneMark(player, loc, i, true);
		}
	}

	@Override
	public void rendLoseMark(Player player, LivingEntity enemy, int marks, boolean doSound) {
		World world = player.getWorld();
		Location loc = player.getLocation();
		if (doSound) {
			world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 0.3f, 0.9f);
			world.playSound(loc, Sound.ITEM_BOTTLE_FILL, SoundCategory.PLAYERS, 0.5f, 0.5f);
			world.playSound(loc, Sound.BLOCK_COMPOSTER_READY, SoundCategory.PLAYERS, 0.6f, 0.8f);
			world.playSound(loc, Sound.ENTITY_TURTLE_DEATH, SoundCategory.PLAYERS, 0.6f, 0.7f);
			world.playSound(loc, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.PLAYERS, 0.6f, 1.7f);
			world.playSound(loc, Sound.ENTITY_GLOW_SQUID_DEATH, SoundCategory.PLAYERS, 0.5f, 1.7f);
		}

		Location markLoc = getMarkLocation(player, enemy, marks);
		drawRuneMark(player, markLoc, marks, false);
		new PartialParticle(Particle.SPELL_INSTANT, markLoc, 3, 0.05, 0.05, 0.05, 0).spawnAsPlayerActive(player);
	}

	@Override
	public void rendMarkDied(Player player, LivingEntity enemy, int marks) {
		for (int i = 1; i <= marks; i++) {
			Location loc = getMarkLocation(player, enemy, i);
			new PartialParticle(Particle.SPELL_INSTANT, loc, 2, 0.05, 0, 0.05, 0).spawnAsPlayerActive(player);
			rendLoseMark(player, enemy, i, false);
		}
	}

	@Override
	public void rendHealEffect(Player player, Player healed, LivingEntity enemy) {
		Location targetLoc = healed.getLocation();
		targetLoc.setY(enemy.getLocation().getY());

		new PPLine(Particle.REDSTONE, enemy.getLocation(), targetLoc)
			.data(MAGENTA).countPerMeter(18).delta(0.15).spawnAsPlayerActive(player);
	}

	@Override
	public void rendAbsorptionEffect(Player player, Player healed, LivingEntity enemy) {
		Location targetLoc = healed.getLocation();
		targetLoc.setY(enemy.getLocation().getY());

		new PPLine(Particle.REDSTONE, enemy.getLocation(), targetLoc)
			.data(YELLOW).countPerMeter(12).spawnAsPlayerActive(player);
	}

	private void drawRuneMark(Player player, Location loc, int markNum, boolean isPassive) {
		Vector up = new Vector(0, 1, 0);
		Vector right = loc.getDirection();

		switch (markNum) {
			case 1 -> {
				Location triangle1 = loc.clone().add(up.clone().multiply(-0.2));
				Location triangle2 = loc.clone().add(up.clone().multiply(0.3)).add(right.clone().multiply(0.25));
				Location triangle3 = loc.clone().add(up.clone().multiply(0.3)).add(right.clone().multiply(-0.25));

				new PPLine(Particle.REDSTONE, triangle3, triangle2)
					.data(isPassive ? MAGENTA_SMALL : YELLOW).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, triangle2, triangle1)
					.data(isPassive ? MAGENTA_SMALL : YELLOW).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, triangle1, triangle3)
					.data(isPassive ? MAGENTA_SMALL : YELLOW).scaleLength(0.6).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
			}
			case 2 -> {
				new PPCircle(Particle.REDSTONE, loc, 0.25)
					.data(isPassive ? MAGENTA_SMALL : YELLOW).countPerMeter(12).axes(up, right).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, loc.clone().add(right.clone().multiply(0.5)), loc.clone().subtract(right.clone().multiply(0.5)))
					.data(isPassive ? MAGENTA_SMALL : YELLOW).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
			}
			case 3 -> {
				Location line1 = loc.clone().add(up.clone().multiply(0.3)).add(right.clone().multiply(0.3));
				Location line2 = loc.clone().add(up.clone().multiply(0.3)).add(right.clone().multiply(-0.3));

				new PPLine(Particle.REDSTONE, loc, line1)
					.data(isPassive ? MAGENTA_SMALL : YELLOW).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, loc, line2)
					.data(isPassive ? MAGENTA_SMALL : YELLOW).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, loc.clone().add(0, -0.3, 0), loc.clone().add(0, 0.2, 0))
					.data(isPassive ? MAGENTA_SMALL : YELLOW).countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
			}
			default -> {
			}
		}
	}
}
