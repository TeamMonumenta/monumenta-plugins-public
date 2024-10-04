package com.playmonumenta.plugins.cosmetics.skills.shaman.hexbreaker;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.skills.PrestigeCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPFlower;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
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

public class PrestigiousDevastationCS extends DevastationCS implements PrestigeCS {

	public static final String NAME = "Prestigious Devastation";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"A glorious blaze fuels",
			"the striving heart."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.YELLOW_GLAZED_TERRACOTTA;
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

	private static final Particle.DustOptions GOLD_COLOR = new Particle.DustOptions(Color.fromRGB(255, 224, 48), 1.25f);
	private static final Particle.DustOptions BURN_COLOR = new Particle.DustOptions(Color.fromRGB(255, 180, 0), 1.0f);
	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f);

	@Override
	public void devastationCast(Plugin plugin, Location targetLoc, Player player, double radius) {
		World world = targetLoc.getWorld();
		targetLoc.add(0, 0.1, 0);
		world.playSound(targetLoc, Sound.BLOCK_END_GATEWAY_SPAWN, SoundCategory.PLAYERS, 1.25f, 0.9f);
		world.playSound(targetLoc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1.3f, 1.5f);
		world.playSound(targetLoc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.7f, 1.45f);
		world.playSound(targetLoc, Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 0.5f, 1.8f);
		world.playSound(targetLoc, Sound.ENTITY_SQUID_SQUIRT, SoundCategory.PLAYERS, 1.5f, 1.35f);
		world.playSound(targetLoc, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5f, Constants.NotePitches.CS19);
		world.playSound(targetLoc, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5f, Constants.NotePitches.E10);
		world.playSound(targetLoc, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5f, Constants.NotePitches.A3);
		new PartialParticle(Particle.FLASH, targetLoc).spawnAsPlayerActive(player);
		new PartialParticle(Particle.LAVA, targetLoc, 15, 1, 1, 1, 0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.END_ROD, targetLoc, 20, 1, 1, 1, 0.3).spawnAsPlayerActive(player);

		new PPFlower(Particle.REDSTONE, targetLoc, 0.4 * radius).petals(5).count((int) (30 * radius)).sharp(true).delta(0.05).data(LIGHT_COLOR).spawnAsPlayerPassive(player);
		new PPCircle(Particle.REDSTONE, targetLoc, radius).countPerMeter(5).ringMode(true).delta(0.05).data(LIGHT_COLOR).spawnAsPlayerPassive(player);
		new PPCircle(Particle.REDSTONE, targetLoc, 0.4 * radius).countPerMeter(5).ringMode(true).delta(0.05).data(GOLD_COLOR).spawnAsPlayerPassive(player);

		Vector toCorner = LocationUtils.getVectorTo(targetLoc.clone().add(radius, 0, 0), targetLoc).multiply(1.6245);
		for (int i = 0; i < 5; i++) {
			Location cornerLoc = targetLoc.clone().add(VectorUtils.rotateTargetDirection(toCorner.clone(), i * 72, 0));
			new PPCircle(Particle.REDSTONE, cornerLoc, radius).countPerMeter(7).arcDegree(i * 72 - 216, i * 72 - 144).data(BURN_COLOR).delta(0.05).spawnAsPlayerActive(player);
			new PPCircle(Particle.REDSTONE, cornerLoc, radius).countPerMeter(5).arcDegree(i * 72 - 216, i * 72 - 144).data(GOLD_COLOR).delta(0.15, 0.15, 0.15).spawnAsPlayerActive(player);
			new PPCircle(Particle.SMALL_FLAME, cornerLoc, radius).countPerMeter(3).arcDegree(i * 72 - 216, i * 72 - 144).delta(0.15, 0.4, 0.15).extra(0.1).spawnAsPlayerActive(player);
		}
	}
}
