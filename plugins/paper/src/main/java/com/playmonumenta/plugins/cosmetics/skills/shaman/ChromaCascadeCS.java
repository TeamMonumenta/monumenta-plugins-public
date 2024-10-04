package com.playmonumenta.plugins.cosmetics.skills.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.skills.DepthsCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
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
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ChromaCascadeCS extends EarthenTremorCS implements DepthsCS {

	public static final String NAME = "Chroma Cascade";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Let the colors run over the ground and shroud",
			"your enemies in blinding, prismatic light."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.ORANGE_GLAZED_TERRACOTTA;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public String getToken() {
		return EXTRA_GEODES;
	}

	public static final Particle.DustOptions RED = new Particle.DustOptions(Color.fromRGB(244, 130, 126), 1.5f);
	public static final Particle.DustOptions ORANGE = new Particle.DustOptions(Color.fromRGB(249, 203, 121), 1.5f);
	public static final Particle.DustOptions YELLOW = new Particle.DustOptions(Color.fromRGB(223, 252, 180), 1.5f);
	public static final Particle.DustOptions GREEN = new Particle.DustOptions(Color.fromRGB(186, 240, 190), 1.5f);
	public static final Particle.DustOptions BLUE = new Particle.DustOptions(Color.fromRGB(148, 206, 248), 1.5f);
	public static final Particle.DustOptions PURPLE = new Particle.DustOptions(Color.fromRGB(224, 124, 233), 1.5f);
	public static final Particle.DustOptions PRISMATIC = new Particle.DustOptions(Color.fromRGB(245, 200, 245), 1.25f);

	@Override
	public void earthenTremorEffect(Player player, double radius) {
		World world = player.getWorld();
		Location loc = player.getLocation().subtract(0, LocationUtils.distanceToGround(player.getLocation(), -64, PlayerUtils.getJumpHeight(player)) - 0.1, 0);
		world.playSound(loc, Sound.ENTITY_SQUID_SQUIRT, SoundCategory.PLAYERS, 2.0f, 1.6f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.4f, 0.6f);
		world.playSound(loc, Sound.BLOCK_CONDUIT_ATTACK_TARGET, SoundCategory.PLAYERS, 1.6f, 1.8f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 1.6f, 1.5f);
		new PPSpiral(Particle.REDSTONE, loc, radius).ticks(5).distancePerParticle(0.097).data(RED).delta(0.03).spawnAsPlayerActive(player);
		new PPSpiral(Particle.REDSTONE, loc, radius).ticks(6).distancePerParticle(0.105).data(ORANGE).delta(0.03).spawnAsPlayerActive(player);
		new PPSpiral(Particle.REDSTONE, loc, radius).ticks(6).distancePerParticle(0.115).data(YELLOW).delta(0.03).spawnAsPlayerActive(player);
		new PPSpiral(Particle.REDSTONE, loc, radius).ticks(7).distancePerParticle(0.125).data(GREEN).delta(0.03).spawnAsPlayerActive(player);
		new PPSpiral(Particle.REDSTONE, loc, radius).ticks(7).distancePerParticle(0.14).data(BLUE).delta(0.03).spawnAsPlayerActive(player);
		new PPSpiral(Particle.REDSTONE, loc, radius).ticks(7).distancePerParticle(0.155).data(PURPLE).delta(0.03).spawnAsPlayerActive(player);

		new PartialParticle(Particle.END_ROD, loc, 22).delta(1).extra(0.08).spawnAsPlayerActive(player);
		new PartialParticle(Particle.WAX_OFF, loc, 22).delta(1).extra(8).spawnAsPlayerActive(player);
	}

	@Override
	public void earthenTremorEnhancement(Player player, Location shockwaveLoc, double enhancementRadius, int distance, double maxDistance) {
		Location loc = shockwaveLoc.clone().subtract(0, LocationUtils.distanceToGround(player.getLocation(), -64, PlayerUtils.getJumpHeight(player)) - 0.1, 0);
		Vector dir = LocationUtils.getDirectionTo(shockwaveLoc, player.getLocation());
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			new PPCircle(Particle.REDSTONE, loc, enhancementRadius).data(PRISMATIC).countPerMeter(2).spawnAsPlayerActive(player);
			new PPCircle(Particle.WAX_OFF, loc, 0.5).countPerMeter(2).delta(dir.getX(), 0, dir.getZ()).extra(1).directionalMode(true).spawnAsPlayerActive(player);
		}, (long) (5 / maxDistance * distance));

	}
}
