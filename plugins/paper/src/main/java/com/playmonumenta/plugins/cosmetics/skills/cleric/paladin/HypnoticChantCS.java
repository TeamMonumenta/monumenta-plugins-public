package com.playmonumenta.plugins.cosmetics.skills.cleric.paladin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class HypnoticChantCS extends ChoirBellsCS {

	public static final String NAME = "Hypnotic Chant";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"A frail mind succumbs to manipulation.",
			"Let them come to you.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.NOTE_BLOCK;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	Particle.DustOptions GREEN = new Particle.DustOptions(Color.fromRGB(0, 210, 170), 0.9f);

	@Override
	public void bellsCastEffect(Player player, double range) {
		Location l = player.getLocation().subtract(0, LocationUtils.distanceToGround(player.getLocation(), 0, PlayerUtils.getJumpHeight(player)), 0);
		World world = player.getWorld();
		world.playSound(l, Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.8f, 0.8f);
		world.playSound(l, Sound.ENTITY_EVOKER_PREPARE_ATTACK, SoundCategory.PLAYERS, 0.8f, 2.0f);
		new PPCircle(Particle.ENCHANTMENT_TABLE, l.clone().add(0, 2, 0), range).delta(0, 1, 0).extra(0.6).ringMode(true).countPerMeter(5).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SONIC_BOOM, l.clone().add(0, 4, 0), 1, 0, 0, 0, 0.0).spawnAsPlayerActive(player);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () ->
			world.playSound(l, Sound.ENTITY_WANDERING_TRADER_REAPPEARED, SoundCategory.PLAYERS, 2.0f, 0.7f), 3);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () ->
			world.playSound(l, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 1.4f, 1.8f), 8);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			// Hieroglyph for "Control"
			Vector front = l.getDirection().clone().setY(0).normalize().multiply(range);
			Vector left = VectorUtils.rotateTargetDirection(front, -90, 0);
			Vector right = VectorUtils.rotateTargetDirection(front, 90, 0);
			double[] d = VectorUtils.vectorToRotation(front);
			Location loc1 = l.clone().add(left);
			Location loc2 = l.clone().add(right);
			for (int i = 0; i < 2; i++) {
				double delta = 0.2 * i;
				final Particle.DustOptions GREEN = new Particle.DustOptions(Color.fromRGB(0, 210 - 60 * i, 170 - 50 * i), 1.2f - i * 0.2f);
				new PPLine(Particle.REDSTONE, loc1, loc2).data(GREEN).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, l, l.clone().subtract(front)).data(GREEN).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, loc1.clone().subtract(front.clone().multiply(0.5)), loc2.clone().subtract(front.clone().multiply(0.5))).scaleLength(0.5).shift(range / 2).data(GREEN).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
				new PPCircle(Particle.REDSTONE, l, range / 1.5).data(GREEN).countPerMeter(10).delta(delta, 0, delta).arcDegree(d[0], d[0] + 180).spawnAsPlayerActive(player);
				new PPCircle(Particle.REDSTONE, l, range / 3).data(GREEN).countPerMeter(10).delta(delta, 0, delta).arcDegree(d[0] - 180, d[0]).spawnAsPlayerActive(player);
			}
			new PPCircle(Particle.ENCHANTMENT_TABLE, l, range).countPerMeter(12).extraRange(0.1, 0.2).innerRadiusFactor(1)
				.directionalMode(true).delta(2, 0.2, -8).rotateDelta(true).spawnAsPlayerActive(player);
			new PartialParticle(Particle.FLASH, l.clone().add(0, 4, 0), 1, 0, 0, 0, 0.1).spawnAsPlayerActive(player);
		}, 10);
	}

	@Override
	public void bellsApplyEffect(Player player, LivingEntity mob) {
		Location mobLoc = mob.getEyeLocation();
		Location playerLoc = player.getLocation().add(0, 4 - LocationUtils.distanceToGround(player.getLocation(), 0, PlayerUtils.getJumpHeight(player)), 0);
		Location middleLoc = mobLoc.clone().add(playerLoc).multiply(0.5);
		Vector dir = LocationUtils.getDirectionTo(playerLoc, mobLoc);
		double distance = mobLoc.distance(playerLoc);
		mob.getWorld().playSound(mobLoc, Sound.ENTITY_ALLAY_ITEM_TAKEN, SoundCategory.PLAYERS, 1.3f, 0.5f);
			ParticleUtils.drawParticleLineSlash(middleLoc, dir, 0, 0.5 * distance, 0.1, 6,
			(Location lineLoc, double middleProgress, double endProgress, boolean middle) ->
				new PartialParticle(Particle.REDSTONE, lineLoc, 1, 0.05, 0.05, 0.05, 0.25).data(GREEN).spawnAsPlayerActive(player));
	}
}
