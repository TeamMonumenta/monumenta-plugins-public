package com.playmonumenta.plugins.cosmetics.skills.warlock.reaper;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.skills.PrestigeCS;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import com.playmonumenta.scriptedquests.utils.MetadataUtils;
import java.util.List;
import org.bukkit.Color;
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

public class PrestigiousBondsCS extends VoodooBondsCS implements PrestigeCS {

	public static final String NAME = "Prestigious Bonds";

	private static final Particle.DustOptions GOLD_COLOR = new Particle.DustOptions(Color.fromRGB(224, 208, 80), 1.25f);
	private static final Particle.DustOptions GOLD_COLOR_SMALL = new Particle.DustOptions(Color.fromRGB(224, 208, 80), 0.8f);
	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 247, 207), 1.0f);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Chains of radiance weave",
			"a beautiful prison."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.GLOW_BERRIES;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public boolean isUnlocked(Player player) {
		return player != null;
	}

	@Override
	public String[] getLockDesc() {
		return List.of("LOCKED").toArray(new String[0]);
	}

	@Override
	public int getPrice() {
		return 1;
	}

	@Override
	public void launchPin(Player player, Location startLoc, Location endLoc) {
		World world = player.getWorld();

		world.playSound(startLoc, Sound.ENTITY_ENDER_PEARL_THROW, SoundCategory.PLAYERS, 0.8f, 2.0f);
		world.playSound(startLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.5f, 1.3f);
		world.playSound(startLoc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1.0f, 1.7f);
		world.playSound(startLoc, Sound.ENTITY_GUARDIAN_HURT, SoundCategory.PLAYERS, 1.0f, 2.0f);

		new PPLine(Particle.REDSTONE, startLoc, endLoc).data(LIGHT_COLOR).countPerMeter(4).delta(0.07).spawnAsPlayerActive(player);
		new PPLine(Particle.SPELL_INSTANT, startLoc, endLoc)
			.directionalMode(true).delta(0, -1, 0).extra(1).countPerMeter(3).spawnAsPlayerActive(player);

		if (startLoc.distance(endLoc) > 1) {
			Vector direction = LocationUtils.getDirectionTo(endLoc, startLoc);
			Location particleLoc = startLoc.clone();
			double length = 2;
			for (int i = 0; i < 10; i++) {
				Vector offset = VectorUtils.rotateTargetDirection(direction, 90, 0).multiply(0.5);
				Location loc1 = particleLoc.clone();
				Location loc2 = particleLoc.clone().add(direction.clone().multiply(length));
				Location loc3 = particleLoc.clone().add(direction.clone().multiply(length / 2)).add(offset);
				Location loc4 = particleLoc.clone().add(direction.clone().multiply(length / 2)).subtract(offset);

				new PPLine(Particle.REDSTONE, loc1, loc3).data(GOLD_COLOR_SMALL)
					.countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, loc1, loc4).data(GOLD_COLOR_SMALL)
					.countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, loc3, loc2).data(GOLD_COLOR_SMALL)
					.countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, loc4, loc2).data(GOLD_COLOR_SMALL)
					.countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(player);

				particleLoc.add(direction.clone().multiply(length));
				if (particleLoc.distance(endLoc) < 2) {
					break;
				}
			}
		}

		startLoc.subtract(0, 2, 0);
		new BukkitRunnable() {
			double mD = 30;
			@Override
			public void run() {
				Vector vec;
				for (double degree = mD; degree < mD + 40; degree += 8) {
					double radian1 = Math.toRadians(degree);
					double cos = FastUtils.cos(radian1);
					double sin = FastUtils.sin(radian1);
					for (double r = 1; r < 5; r += 0.5) {
						vec = new Vector(cos * r, 1, sin * r);
						vec = VectorUtils.rotateXAxis(vec, startLoc.getPitch());
						vec = VectorUtils.rotateYAxis(vec, startLoc.getYaw());

						Location l = startLoc.clone().add(vec);
						new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, GOLD_COLOR_SMALL).spawnAsPlayerActive(player);
						new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, LIGHT_COLOR).spawnAsPlayerActive(player);
					}
				}
				mD += 40;
				if (mD >= 150) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void hitMob(Player player, LivingEntity mob) {
		World world = player.getWorld();
		Location loc = LocationUtils.getEntityCenter(mob);

		world.playSound(loc, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 1.5f, 1.0f);
		world.playSound(loc, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 1.5f, 1.5f);
		world.playSound(loc, Sound.ENTITY_BEE_STING, SoundCategory.PLAYERS, 0.4f, 0.5f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 0.4f, 1.5f);

		new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 8, 0.25, 0.5, 0.25, 0.05).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SPELL_INSTANT, mob.getEyeLocation(), 15, 0.2, 0.2, 0.2, 0).spawnAsPlayerActive(player);
	}

	@Override
	public void hitPlayer(Player mPlayer, Player p) {
		p.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1.2f, 0.65f);
		p.playSound(p.getLocation(), Sound.ENTITY_GUARDIAN_HURT, SoundCategory.PLAYERS, 2f, 0.6f);
		new PartialParticle(Particle.SPELL_INSTANT, p.getLocation(), 40, 0.3, 0, 0.3, 0.02).spawnAsPlayerActive(mPlayer);
		new PPLine(Particle.REDSTONE, mPlayer.getLocation().clone().add(0, 0.8, 0), p.getLocation().clone().add(0, 0.8, 0))
			.data(GOLD_COLOR).countPerMeter(2.5).delta(0.25).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void curseTick(Player player, Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (entity instanceof LivingEntity mob) {
			Location loc = mob.getEyeLocation();
			new PartialParticle(Particle.SPELL_INSTANT, loc, 6, 0.2, 0.2, 0.2, 0).spawnAsPlayerActive(player);
			new PartialParticle(Particle.REDSTONE, loc, 2, 0.2, 0.2, 0.2, 0).data(LIGHT_COLOR).spawnAsPlayerActive(player);
			new PartialParticle(Particle.REDSTONE, loc, 2, 0.2, 0.2, 0.2, 0).data(GOLD_COLOR).spawnAsPlayerActive(player);
		}
	}

	@Override
	public void curseSpread(Player player, LivingEntity toMob, LivingEntity sourceMob) {
		World world = sourceMob.getWorld();
		if (MetadataUtils.checkOnceThisTick(Plugin.getInstance(), player, "PrestigiousBondsCurseSound")) {
			world.playSound(sourceMob.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, 0.35f, 0.95f);
		}
		Location mLoc = toMob.getLocation();
		Location eLoc = sourceMob.getLocation();
		new PartialParticle(Particle.REDSTONE, mLoc, 30, 0.4, 0.7, 0.4, 0, LIGHT_COLOR).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, mLoc, 40, 0.5, 0.5, 0.5, 0, GOLD_COLOR).spawnAsPlayerActive(player);
		Vector mFront = mLoc.toVector().subtract(eLoc.toVector());
		ParticleUtils.drawCurve(eLoc.clone().add(0, 0.75, 0), 1, 36, mFront,
			t -> 0.5 + 0.5 * FastUtils.sinDeg(t * 10),
				t -> 0, t -> 0.125 * FastUtils.cosDeg(t * 10),
				(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0, GOLD_COLOR).spawnAsPlayerActive(player)
		);
	}
}
