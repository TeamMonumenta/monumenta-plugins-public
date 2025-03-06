package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class DarkSparkCS extends ArcaneStrikeCS {
	public static final String NAME = "Dark Spark";
	public static final Color TIP_COLOR = Color.fromRGB(166, 0, 64);
	public static final Color BASE_COLOR = Color.fromRGB(0, 0, 0);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"When dissonant energy is applied mere moments",
			"after a physical blow, the spark ripples across",
			"realms with a violent dark fury."
		);
	}

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.ARCANE_STRIKE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.NETHER_STAR;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public void onStrike(Plugin plugin, Player player, World world, Location enemyLoc, Location playerLoc, double radius) {
		new PartialParticle(Particle.FLASH, enemyLoc, 1, 0, 0, 0, 0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.ELECTRIC_SPARK, enemyLoc, 75, 0, 0, 0, 1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FALLING_DUST, enemyLoc, 75).delta(radius / 2).data(Material.RED_CONCRETE.createBlockData()).spawnAsPlayerActive(player);

		world.playSound(enemyLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.0f, 1.0f);
		world.playSound(enemyLoc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.0f, 0.5f);
		world.playSound(enemyLoc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 1.0f, 1.7f);
		world.playSound(enemyLoc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.2f, 1.0f);
		world.playSound(enemyLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1.0f, 1.0f);
		world.playSound(enemyLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 1.0f, 1f);
		world.playSound(enemyLoc, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1f, 1.5f);
		world.playSound(enemyLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0f, 2f);

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (mT % 2 == 0) {
					for (int i = 0; i < 5; i++) {
						Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> sparkParticle(player, enemyLoc, enemyLoc, VectorUtils.randomUnitVector().multiply(FastUtils.randomDoubleInRange(1, 2)), 0.8f, Math.max(0, radius - (0.5 * mT / 2)), 0), FastUtils.randomIntInRange(0, 2));
					}
				}

				if (mT >= 4) {
					this.cancel();
				}
				mT++;
			}

		}.runTaskTimer(plugin, 0, 1);
	}

	private void sparkParticle(Player player, Location loc, Location origin, Vector dir, float size, double radius, int iteration) {
		if (loc.distance(origin) > radius || iteration > 2) {
			return;
		}

		Location location = loc.clone();
		Vector direction = dir.clone();

		for (int i = 0; i < 2; i++) {
			Location oldLocation = location.clone();
			location.add(direction.multiply(0.7)).add(FastUtils.randomDoubleInRange(-0.5, 0.5), FastUtils.randomDoubleInRange(-0.5, 0.5), FastUtils.randomDoubleInRange(-0.5, 0.5));

			new PPLine(Particle.DUST_COLOR_TRANSITION, oldLocation, location).data(new Particle.DustTransition(BASE_COLOR, TIP_COLOR, size))
				.countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
		}
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			sparkParticle(player, location, origin, dir, size, radius, iteration + 1);
			sparkParticle(player, location, origin, dir, size, radius, iteration + 1);
		}, 1);
	}
}
