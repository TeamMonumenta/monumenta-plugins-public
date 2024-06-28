package com.playmonumenta.plugins.cosmetics.skills.warlock;

import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class UntamedResilienceCS extends PhlegmaticResolveCS {
	private static final Particle.DustOptions NEON_SMALL = new Particle.DustOptions(Color.fromRGB(215, 255, 0), 0.75f);
	private static final Particle.DustOptions LIME = new Particle.DustOptions(Color.fromRGB(155, 210, 0), 1.0f);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Trailing your every step is the",
			"protection of something much greater."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.EMERALD;
	}

	@Override
	public @Nullable String getName() {
		return "Untamed Resilience";
	}

	@Override
	public void periodicTrigger(Player player, Player receiver, int cooldowns) {
		Location loc = LocationUtils.fallToGround(receiver.getLocation(), receiver.getLocation().getY() - 1);
		for (int i = 0; i < cooldowns; i++) {
			Vector direction = VectorUtils.rotateYAxis(new Vector(1, 0, 0), i * 120 + receiver.getLocation().getYaw() + 90);
			Location pLoc = loc.clone().add(direction.clone().multiply(0.25));
			drawDiamond(player, pLoc, direction);
		}
	}

	@Override
	public void enhanceDamageTick(Player player, double radius, double[] damageTicks) {
		Location loc = LocationUtils.fallToGround(player.getLocation(), player.getLocation().getY() - 1);

		player.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);

		for (int degree = 0; degree < 360; degree += 30) {
			Location spikeLoc = loc.clone().add(VectorUtils.rotateYAxis(new Vector(radius, 0, 0), degree));

			new PPLine(Particle.REDSTONE, spikeLoc, VectorUtils.rotateYAxis(LocationUtils.getDirectionTo(loc, spikeLoc), 12), radius / FastUtils.cosDeg(12) * 0.66).data(LIME)
				.countPerMeter(8).groupingDistance(0).spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE, spikeLoc, VectorUtils.rotateYAxis(LocationUtils.getDirectionTo(loc, spikeLoc), -12), radius / FastUtils.cosDeg(12) * 0.66).data(LIME)
				.countPerMeter(8).groupingDistance(0).spawnAsPlayerActive(player);
		}
	}

	@Override
	public void enhanceDamageMob(Player player, LivingEntity mob) {
		mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, SoundCategory.PLAYERS, 0.7f, 2.0f);
		new PartialParticle(Particle.BLOCK_CRACK, LocationUtils.getHalfHeightLocation(mob), 12, 0.5f, 0.5f, 0.5f)
			.data(Material.CACTUS.createBlockData()).spawnAsPlayerActive(player);
	}

	@Override
	public void enhanceHurtSound(Player player) {
		player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 1.0f, 1.5f);
	}

	private void drawDiamond(Player player, Location loc, Vector direction) {
		Location origin = loc.clone();
		Location loc1 = loc.clone().add(VectorUtils.rotateYAxis(direction, 25).multiply(0.5));
		Location loc2 = loc.clone().add(VectorUtils.rotateYAxis(direction, -(double) 25).multiply(0.5));
		Location loc3 = loc.clone().add(direction.clone().multiply(2 * 0.5 * FastUtils.cosDeg(25)));

		new PPLine(Particle.REDSTONE, origin, loc1).data(NEON_SMALL)
			.countPerMeter(7).groupingDistance(0).spawnAsPlayerActive(player);
		new PPLine(Particle.REDSTONE, origin, loc2).data(NEON_SMALL)
			.countPerMeter(7).groupingDistance(0).spawnAsPlayerActive(player);
		new PPLine(Particle.REDSTONE, loc1, loc3).data(NEON_SMALL)
			.countPerMeter(7).groupingDistance(0).spawnAsPlayerActive(player);
		new PPLine(Particle.REDSTONE, loc2, loc3).data(NEON_SMALL)
			.countPerMeter(7).groupingDistance(0).spawnAsPlayerActive(player);
	}
}
