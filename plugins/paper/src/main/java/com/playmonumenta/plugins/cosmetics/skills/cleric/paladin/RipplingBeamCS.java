package com.playmonumenta.plugins.cosmetics.skills.cleric.paladin;

import com.playmonumenta.plugins.particle.PPCircle;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class RipplingBeamCS extends HolyJavelinCS {

	public static final String NAME = "Rippling Beam";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"An echoing spear with a crystalline sound,",
			"conjured from but the wave of your hand."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.DIAMOND;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public void javelinHitBlock(Player player, Location loc, World world) {

	}

	@Override
	public void javelinParticle(Player player, Location startLoc, Location endLoc, double size) {
		Vector dir = startLoc.getDirection().normalize();
		Location pLoc = player.getEyeLocation();
		pLoc.setPitch(pLoc.getPitch() + 90);
		Vector pVec = new Vector(pLoc.getDirection().getX(), pLoc.getDirection().getY(), pLoc.getDirection().getZ());
		pVec = pVec.normalize();
		double d = 1;

		Location currLoc = startLoc.clone();
		for (int i = 0; i < startLoc.distance(endLoc); i++) {
			currLoc.add(dir);
			d++;
			new PPCircle(Particle.SCRAPE, currLoc, (2 * size) / d).countPerMeter(5).extra(16 / d)
				.delta(pVec.getX(), pVec.getY(), pVec.getZ()).directionalMode(true).rotateDelta(true)
				.axes(pVec, pVec.clone().crossProduct(startLoc.getDirection())).ringMode(true).spawnAsPlayerActive(player);
			new PPCircle(Particle.SOUL_FIRE_FLAME, currLoc, (2 * size + 1) / d).countPerMeter(2.5).extra(0.4 / d)
				.delta(pVec.getX(), pVec.getY(), pVec.getZ()).directionalMode(true).rotateDelta(true)
				.axes(pVec, pVec.clone().crossProduct(startLoc.getDirection())).ringMode(true).spawnAsPlayerActive(player);
		}
	}

	@Override
	public void javelinSound(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_VEX_AMBIENT, SoundCategory.PLAYERS, 1.9f, 1.5f);
		world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.9f, 0.7f);
		world.playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.PLAYERS, 1.9f, 0.8f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_2, SoundCategory.PLAYERS, 1.1f, 1.4f);
		world.playSound(loc, Sound.ENTITY_ALLAY_ITEM_TAKEN, SoundCategory.PLAYERS, 2.0f, 1.0f);
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.25f, 1.0f);
	}
}
