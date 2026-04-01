package com.playmonumenta.plugins.cosmetics.skills.scout.ranger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class GaleShotCS implements CosmeticSkill {
	private static final Color BEAM_COLOR_1 = Color.fromRGB(173, 173, 168);
	private static final Color BEAM_COLOR_2 = Color.fromRGB(235, 235, 228);

	private static final Particle.DustOptions LIGHT_BLUE_COLOR = new Particle.DustOptions(Color.fromRGB(164, 227, 232), 1.0f);
	private static final Particle.DustOptions WHITE = new Particle.DustOptions(Color.fromRGB(222, 238, 239), 1.5f);


	@Override
	public ClassAbility getAbility() {
		return ClassAbility.GALE_SHOT;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SHEARS;
	}

	public void fire(Player player, Projectile proj) {
		final World world = player.getWorld();
		final Location playerLoc = player.getLocation();
		world.playSound(playerLoc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 1f, 1.4f);
		world.playSound(playerLoc, "minecraft:entity.breeze.deflect", SoundCategory.PLAYERS, 1, 0.8f);
		world.playSound(playerLoc, Sound.ENTITY_BREEZE_DEATH, SoundCategory.PLAYERS, 1, 0.8f);
		world.playSound(playerLoc, Sound.ENTITY_BREEZE_IDLE_GROUND, SoundCategory.PLAYERS, 1, 1.2f);
		world.playSound(playerLoc, Sound.ENTITY_ELDER_GUARDIAN_DEATH, SoundCategory.PLAYERS, 1, 1.4f);

		Location loc = player.getEyeLocation();

		new PartialParticle(Particle.SWEEP_ATTACK, loc).minimumCount(1).count(2).delta(0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.END_ROD, loc).count(5).delta(0.3).extra(0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.DAMAGE_INDICATOR, loc).count(5).delta(0.3).extra(0.1).spawnAsPlayerActive(player);

		Location fireLoc = player.getEyeLocation();
		proj.setVisibleByDefault(false);

		new BukkitRunnable() {
			double mFlightDistance = 1;
			Vector mFlightDir = proj.getVelocity().normalize();
			Location mPastLoc = fireLoc.clone().add(mFlightDir.clone()).add(0, 0.3, 0);

			@Override
			public void run() {
				if (!proj.isValid() || proj.getTicksLived() > 200 || (proj instanceof AbstractArrow arrow && arrow.isInBlock())) {
					this.cancel();
				}
				mFlightDir = proj.getVelocity().normalize();

				// Spiral
				double beamRange = mPastLoc.distance(proj.getLocation());

				Vector axis1 = mFlightDir.clone().crossProduct(new Vector(0, 1, 0));
				Vector axis2 = mFlightDir.clone().crossProduct(axis1); // Already normalised

				for (int i = 0; i < 5 * ((int) Math.ceil(beamRange)); i++) {
					double distanceAlongLine = (float) i / (5 * ((int) Math.ceil(beamRange))) * beamRange;
					Location point = mPastLoc.clone().add(mFlightDir.clone().multiply(distanceAlongLine));
					double distanceFromFiring = mFlightDistance + distanceAlongLine;
					double theta = (distanceFromFiring < 7 ?
						(0.0152 * Math.pow(distanceFromFiring, 3)) :
						8.1105 * Math.pow(distanceFromFiring, 0.6) - 20.854) % (Math.PI * 2);
					Vector direction = axis1.clone().multiply(FastUtils.cos(theta))
						.add(axis2.clone().multiply(FastUtils.sin(theta)));
					Location spiralTip = point.add(direction.clone().multiply(0.5f));
					new PartialParticle(Particle.WHITE_SMOKE, spiralTip, 1)
						.delta(direction)
						.directionalMode(true)
						.extra(0.15)
						.spawnAsPlayerActive(player);
				}

				// Main trail
				new PPLine(Particle.DUST_COLOR_TRANSITION, mPastLoc, proj.getLocation())
					.includeStart(false)
					.data(new Particle.DustTransition(BEAM_COLOR_1, BEAM_COLOR_2, 1.8f))
					.countPerMeter(1.6)
					.spawnAsPlayerActive(player);

				mFlightDistance += beamRange;
				mPastLoc = proj.getLocation();
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	public void hit(final Player player, final Location location) {
		new PartialParticle(Particle.GUST, location).minimumCount(1).spawnAsPlayerActive(player);
		location.getWorld().playSound(location, Sound.ENTITY_BREEZE_JUMP, 1f, 2f);
	}

	public void hitBlock(final Player player, final Location location) {
		new PartialParticle(Particle.GUST, location)
			.minimumCount(1)
			.spawnAsPlayerActive(player);
		new PartialParticle(Particle.GUST_DUST, location)
			.count(8)
			.spawnAsPlayerActive(player);
		new PPExplosion(Particle.CLOUD, location)
			.count(25)
			.speed(0.4)
			.extra(Math.min(0.2 + player.getLocation().distance(location) / 16, 1))
			.directionalMode(true)
			.speedVar(0.03)
			.spawnAsPlayerActive(player);
		new PPExplosion(Particle.DUST_PLUME, location)
			.count(35)
			.speed(0.5)
			.extra(Math.min(0.2 + player.getLocation().distance(location) / 16, 1))
			.directionalMode(true)
			.speedVar(0.03)
			.spawnAsPlayerActive(player);
		location.getWorld().playSound(location, Sound.ENTITY_BREEZE_JUMP, 1f, 2f);
	}

	public void imbue(final Location playerLoc) {
		final World world = playerLoc.getWorld();
		world.playSound(playerLoc, Sound.BLOCK_END_PORTAL_FRAME_FILL, 1f, 0.8f);
		world.playSound(playerLoc, Sound.ITEM_TRIDENT_RETURN, 1, 1f);
	}

	public void tick(Player player, Location loc) {
		if (player.getGameMode() == GameMode.SPECTATOR) {
			return;
		}

		Location newLoc = loc.clone().add(0, 0.2, 0);

		new PPCircle(Particle.REDSTONE, newLoc, 1)
			.ringMode(true)
			.countPerMeter(3)
			.data(LIGHT_BLUE_COLOR)
			.spawnAsPlayerPassive(player);


		new PPCircle(Particle.REDSTONE, newLoc, 1)
			.ringMode(true)
			.countPerMeter(2)
			.data(WHITE)
			.spawnAsPlayerPassive(player);
	}
}
