package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPLine;
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
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class AceArtilleryCS extends AlchemicalArtilleryCS {

	public static final String NAME = "Ace Artillery";

	public static final Color ACE_BLACK = Color.fromRGB(0x272425);
	public static final Color ACE_GRAY = Color.fromRGB(0xA1A1A1);
	public static final Color ACE_WHITE = Color.WHITE;
	public static final Color ACE_PURPLE = Color.fromRGB(0x703277);

	public static final List<Color> ACE_COLORS = List.of(ACE_BLACK, ACE_GRAY, ACE_WHITE, ACE_PURPLE);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Forever told that it wasn't right",
			"to not feel the same as everyone else.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.PURPLE_GLAZED_TERRACOTTA;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public void onSpawn(World world, Location loc) {
		world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.0f, 0.6f);
		world.playSound(loc, Sound.ENTITY_ZOMBIE_INFECT, SoundCategory.PLAYERS, 0.8f, 0.3f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.7f, 0.8f);
		world.playSound(loc, Sound.ENTITY_PLAYER_BREATH, SoundCategory.PLAYERS, 1.0f, 0.6f);
	}

	@Override
	public void periodicEffects(Player caster, MagmaCube grenade, Item physicsItem, int ticks) {
		new PartialParticle(Particle.REDSTONE, physicsItem.getLocation())
			.data(new Particle.DustOptions(ACE_COLORS.get(ticks % ACE_COLORS.size()), 1.2f))
			.delta(0.15)
			.count(3)
			.spawnAsPlayerActive(caster);
	}

	@Override
	public void explosionEffect(Player caster, Location loc, double radius) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 0.8f, 0.1f);
		world.playSound(loc, Sound.ENTITY_BREEZE_HURT, SoundCategory.PLAYERS, 1.2f, 0.8f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.8f, 0.4f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.4f, 1.2f);

		new PartialParticle(Particle.SQUID_INK, loc)
			.count(36)
			.delta(0.1)
			.extra(radius * 0.1)
			.spawnAsPlayerActive(caster);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				int sides = mTicks + 3;
				double circleRadius = 1.5 + mTicks * 0.5;

				double degStep = 360.0 / sides;
				for (double deg = 0; deg < 360; deg += degStep) {
					new PPLine(Particle.REDSTONE,
						loc.clone().add(circleRadius * FastUtils.cosDeg(deg), 0, circleRadius * FastUtils.sinDeg(deg)),
						loc.clone().add(circleRadius * FastUtils.cosDeg(deg + degStep), 0, circleRadius * FastUtils.sinDeg(deg + degStep)))
						.data(new Particle.DustOptions(ACE_COLORS.get(mTicks % ACE_COLORS.size()), 0.9f + mTicks * 0.1f))
						.delta(0.06, 0.15, 0.06)
						.countPerMeter(6)
						.spawnAsPlayerActive(caster);
				}
				mTicks++;
				if (circleRadius > radius) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

		ParticleUtils.drawFlag(caster, loc.clone().add(0, 3, 0), ACE_COLORS, 2.0f);
	}

	@Override
	public void aftershockEffect(Player caster, Location loc, double radius, List<LivingEntity> hitMobs) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.ENTITY_BREEZE_HURT, SoundCategory.PLAYERS, 1.2f, 0.8f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.2f, 1.6f);

		if (hitMobs.isEmpty()) {
			return;
		}
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				int sides = mTicks + 3;
				double circleRadius = 1.5 + mTicks * 0.5;

				double degStep = 360.0 / sides;
				for (double deg = 0; deg < 360; deg += degStep) {
					new PPLine(Particle.REDSTONE,
						loc.clone().add(circleRadius * FastUtils.cosDeg(deg), 0, circleRadius * FastUtils.sinDeg(deg)),
						loc.clone().add(circleRadius * FastUtils.cosDeg(deg + degStep), 0, circleRadius * FastUtils.sinDeg(deg + degStep)))
						.data(new Particle.DustOptions(ACE_COLORS.get(mTicks % ACE_COLORS.size()), 1f))
						.delta(0.06)
						.countPerMeter(3)
						.spawnAsPlayerActive(caster);
				}
				mTicks++;
				if (circleRadius > radius) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
