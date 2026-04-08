package com.playmonumenta.plugins.cosmetics.skills.shaman;

import com.playmonumenta.plugins.cosmetics.skills.DepthsCS;
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
	public void earthenTremorEffect(Player player, Location location, double radius) {
		World world = player.getWorld();
		Location loc = location.subtract(0, LocationUtils.distanceToGround(player.getLocation(), -64, PlayerUtils.getJumpHeight(player)) - 0.1, 0);
		world.playSound(loc, Sound.ENTITY_SQUID_SQUIRT, SoundCategory.PLAYERS, 2.0f, 1.6f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.4f, 0.6f);
		world.playSound(loc, Sound.BLOCK_CONDUIT_ATTACK_TARGET, SoundCategory.PLAYERS, 1.6f, 1.8f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 1.6f, 1.5f);
		new PPSpiral(Particle.REDSTONE, loc, radius).ticks(5).curveAngle(220).count(180).data(RED).delta(0.03).spawnAsPlayerActive(player);
		new PPSpiral(Particle.REDSTONE, loc, radius).ticks(6).curveAngle(208).count(180).data(ORANGE).delta(0.03).spawnAsPlayerActive(player);
		new PPSpiral(Particle.REDSTONE, loc, radius).ticks(6).curveAngle(192).count(180).data(YELLOW).delta(0.03).spawnAsPlayerActive(player);
		new PPSpiral(Particle.REDSTONE, loc, radius).ticks(7).curveAngle(178).count(180).data(GREEN).delta(0.03).spawnAsPlayerActive(player);
		new PPSpiral(Particle.REDSTONE, loc, radius).ticks(7).curveAngle(158).count(180).data(BLUE).delta(0.03).spawnAsPlayerActive(player);
		new PPSpiral(Particle.REDSTONE, loc, radius).ticks(7).curveAngle(140).count(180).data(PURPLE).delta(0.03).spawnAsPlayerActive(player);

		new PartialParticle(Particle.END_ROD, loc, 22).delta(1).extra(0.08).spawnAsPlayerActive(player);
		new PartialParticle(Particle.WAX_OFF, loc, 22).delta(1).extra(8).spawnAsPlayerActive(player);
	}

	@Override
	public void totemLandingEffect(Player player, Location location, double radius) {
		Location loc = location.add(0, 0.2, 0);
		new PartialParticle(Particle.BLOCK_CRACK, loc, 50, radius * 0.75, 0.25, radius * 0.75, 0.1,
			Bukkit.createBlockData(Material.OCHRE_FROGLIGHT)).spawnAsPlayerActive(player);
		new PartialParticle(Particle.BLOCK_CRACK, loc, 50, radius * 0.75, 0.25, radius * 0.75, 0.1,
			Bukkit.createBlockData(Material.VERDANT_FROGLIGHT)).spawnAsPlayerActive(player);
		new PartialParticle(Particle.BLOCK_CRACK, loc, 50, radius * 0.75, 0.25, radius * 0.75, 0.1,
			Bukkit.createBlockData(Material.PEARLESCENT_FROGLIGHT)).spawnAsPlayerActive(player);

		World world = player.getWorld();
		world.playSound(loc, Sound.ENTITY_SQUID_SQUIRT, SoundCategory.PLAYERS, 1.0f, 1.6f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.6f);
		world.playSound(loc, Sound.BLOCK_CONDUIT_ATTACK_TARGET, SoundCategory.PLAYERS, 1.0f, 1.8f);
	}
}
