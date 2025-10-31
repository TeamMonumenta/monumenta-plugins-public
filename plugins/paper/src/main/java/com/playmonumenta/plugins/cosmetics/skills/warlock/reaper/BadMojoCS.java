package com.playmonumenta.plugins.cosmetics.skills.warlock.reaper;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
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

public class BadMojoCS extends DarkPactCS {
	private static final Particle.DustOptions MAGENTA = new Particle.DustOptions(Color.fromRGB(255, 0, 120), 1f);
	private static final Particle.DustOptions RED = new Particle.DustOptions(Color.fromRGB(255, 0, 40), 1f);
	private int mKills = 0;

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"It will be a most unfortunate",
			"fight for those who face you."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.CARVED_PUMPKIN;
	}

	@Override
	public @Nullable String getName() {
		return "Bad Mojo";
	}

	@Override
	public void onCast(Player player, World world, Location loc) {
		reset();

		world.playSound(loc, Sound.ENTITY_SKELETON_HORSE_DEATH, SoundCategory.PLAYERS, 0.35f, 0.85f);
		world.playSound(loc, Sound.ENTITY_STRAY_DEATH, SoundCategory.PLAYERS, 0.7f, 0.55f);
		world.playSound(loc, Sound.ENTITY_STRAY_DEATH, SoundCategory.PLAYERS, 0.7f, 0.5f);
		world.playSound(loc, Sound.ENTITY_WITHER_SKELETON_DEATH, SoundCategory.PLAYERS, 0.8f, 0.9f);
		world.playSound(loc, Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.PLAYERS, 1f, 0.5f);
		world.playSound(loc, Sound.ENTITY_ALLAY_DEATH, SoundCategory.PLAYERS, 0.6f, 0.65f);

		new PartialParticle(Particle.SPELL_WITCH, loc, 20, 0.2, 0.1, 0.2, 1).spawnAsPlayerActive(player);
		drawPattern(player, 13, true);

		ArrayList<Location> hexLocations = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			Location hexLocation = loc.clone().add(VectorUtils.rotateYAxis(new Vector(2, 0, 0), 60 * i + player.getEyeLocation().getYaw()));
			hexLocations.add(hexLocation.add(0, 0.1, 0));
		}
		new BukkitRunnable() {
			int mTicks = 0;
			final List<Location> mLocations = hexLocations;

			@Override
			public void run() {
				for (Location loc : mLocations) {
					new PartialParticle(Particle.SPELL_MOB, loc, 1, 0, 0, 0, 0).directionalMode(true).spawnAsPlayerActive(player);
					new PartialParticle(Particle.SPELL_WITCH, loc, 1, 0, 0).spawnAsPlayerActive(player);
				}

				mTicks++;
				if (mTicks > 30) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void tick(Player player, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (oneHertz && mKills > 0) {
			drawPattern(player, mKills, false);
		}

		if (FastUtils.RANDOM.nextDouble() < (mKills + 6) / 12.0) {
			new PartialParticle(Particle.SPELL_WITCH, player.getLocation(), mKills < 6 ? 3 : 7, 1, 0, 1, 0).spawnAsPlayerBuff(player);

			double r = FastUtils.randomDoubleInRange(1, 3);
			double theta = FastUtils.randomDoubleInRange(0, 360);
			Location pLoc = player.getLocation().add(r * FastUtils.cosDeg(theta), 0, r * FastUtils.sinDeg(theta));
			pLoc = LocationUtils.fallToGround(pLoc, pLoc.getY() - 1).add(0, 0.25, 0);

			Location finalLocation = pLoc.clone();
			new BukkitRunnable() {
				int mTicks = 0;
				final Location mLoc = finalLocation;

				@Override
				public void run() {
					if (mTicks == 0) {
						new PartialParticle(Particle.SOUL, mLoc, 1, 0, 0, 0, 0).spawnAsPlayerBuff(player);
					}
					new PartialParticle(Particle.SPELL_MOB_AMBIENT, mLoc, 1, 0, 0, 0, 1).directionalMode(true).spawnAsPlayerBuff(player);

					mTicks++;
					if (mTicks > (mKills < 6 ? 12 : 40)) {
						this.cancel();
					}
				}
			}.runTaskTimer(Plugin.getInstance(), 0, 1);
		}

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				// spawn soul faces around the player's head
				Location origin = player.getEyeLocation();

				Vector front = origin.getDirection().setY(0).normalize();
				Vector up = new Vector(0, 1, 0);
				Vector right = VectorUtils.crossProd(up, front);

				Location loc1 = origin.clone().add(up.clone().multiply(0.7)).add(right.clone().multiply(0.45));
				Location loc2 = origin.clone().add(up.clone().multiply(0.7)).add(right.clone().multiply(-0.45));
				Location loc3 = origin.clone().add(up.clone().multiply(0.2)).add(right.clone().multiply(0.75));
				Location loc4 = origin.clone().add(up.clone().multiply(0.2)).add(right.clone().multiply(-0.75));
				List<Location> playerLocs = List.of(loc1, loc2, loc3, loc4);

				for (Location loc : playerLocs) {
					new PartialParticle(Particle.SOUL, loc, 1, 1, 9999999).directionalMode(true).spawnAsPlayerBuff(player);
				}

				mTicks++;
				if (mTicks > 4) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void onKill(Player player, LivingEntity mob) {
		mKills++;
		drawTriangle(player, LocationUtils.getHalfHeightLocation(mob), mob.getWidth());

		World world = player.getWorld();
		float pitch = (float) Math.min(0.5 + mKills / 10.0, 1.2);
		world.playSound(player.getLocation(), Sound.ENTITY_STRAY_STEP, SoundCategory.PLAYERS, 1f, pitch);
		world.playSound(player.getLocation(), Sound.BLOCK_CHAIN_HIT, SoundCategory.PLAYERS, 1f, pitch);
	}

	@Override
	public void loseEffect(Player player) {
		drawPattern(player, mKills, true);

		World world = player.getWorld();
		world.playSound(player.getLocation(), Sound.ENTITY_WITHER_SKELETON_DEATH, SoundCategory.PLAYERS, 0.3f, 0.5f);
		world.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_DEATH, SoundCategory.PLAYERS, 1f, 1f);
		world.playSound(player.getLocation(), Sound.ENTITY_SQUID_DEATH, SoundCategory.PLAYERS, 1f, 0.85f);

		reset();
	}

	public void reset() {
		mKills = 0;
	}

	public void drawPattern(Player player, int kills, boolean instant) {
		if (kills <= 0) {
			return;
		}

		if (instant) {
			Location loc = LocationUtils.fallToGround(player.getLocation(), player.getLocation().getY() - 1);
			Vector direction = player.getEyeLocation().getDirection().setY(0).normalize();
			for (int i = 0; i < Math.min(13, kills); i++) {
				drawPatternSegment(player, i, loc, direction);
			}
		} else {
			new BukkitRunnable() {
				int mTicks = 0;
				final Vector mDirection = player.getEyeLocation().getDirection().setY(0).normalize();
				final Location mLoc = LocationUtils.fallToGround(player.getLocation(), player.getLocation().getY() - 1);

				@Override
				public void run() {
					drawPatternSegment(player, mTicks, mLoc, mDirection);

					mTicks++;
					if (mTicks > Math.min(12, kills - 1)) {
						this.cancel();
					}
				}
			}.runTaskTimer(Plugin.getInstance(), 0, 1);
		}
	}

	public void drawPatternSegment(Player player, int num, Location loc, Vector direction) {

		Color color = ParticleUtils.getTransition(RED.getColor(), MAGENTA.getColor(), Math.max(0, Math.min(1, Math.abs(num - 6) / 6.0)));

		new PPParametric(Particle.REDSTONE, loc, (param, builder) -> {
			double x = 2.5 * FastUtils.cos(Math.PI * (param - 0.5)) * FastUtils.cos(Math.PI / 3 * (param - 0.5));
			double y = 0.1;
			double z = 2 * FastUtils.cos(Math.PI * (param - 0.5)) * FastUtils.sin(Math.PI / 3 * (param - 0.5));
			Vector vec = new Vector(x, y, z);

			double[] rotation = VectorUtils.vectorToRotation(direction);
			double offset = 90 + 30 * num;
			vec = VectorUtils.rotateYAxis(vec, rotation[0] + offset);

			builder.location(loc.clone().add(vec));
		}).data(new Particle.DustOptions(color, 1f)).count(35).spawnAsPlayerActive(player);
	}

	private void drawTriangle(Player player, Location loc, double length) {
		double angle = FastUtils.randomDoubleInRange(0, 360);
		Location loc1 = loc.clone().add(VectorUtils.rotateYAxis(new Vector(length, 0, 0), angle));
		Location loc2 = loc.clone().add(VectorUtils.rotateYAxis(new Vector(length, 0, 0), angle + 120));
		Location loc3 = loc.clone().add(VectorUtils.rotateYAxis(new Vector(length, 0, 0), angle + 240));

		new PPLine(Particle.REDSTONE, loc1, loc2).data(MAGENTA).countPerMeter(10).spawnAsPlayerActive(player);
		new PPLine(Particle.REDSTONE, loc2, loc3).data(MAGENTA).countPerMeter(10).spawnAsPlayerActive(player);
		new PPLine(Particle.REDSTONE, loc3, loc1).data(MAGENTA).countPerMeter(10).spawnAsPlayerActive(player);
	}
}
