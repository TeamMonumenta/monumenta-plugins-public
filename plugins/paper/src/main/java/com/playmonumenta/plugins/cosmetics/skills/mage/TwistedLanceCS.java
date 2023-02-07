package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.apache.commons.math3.util.FastMath;
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

public class TwistedLanceCS extends ManaLanceCS {
	//Delve theme

	public static final String NAME = "Twisted Lance";

	private static final Color TWISTED_COLOR = Color.fromRGB(127, 0, 0);
	private static final Color DARK_COLOR = Color.fromRGB(54, 114, 156);

	@Override
	public @Nullable Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"Something about this lance is",
			"wrong... Twisted...");
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.MANA_LANCE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.MUSIC_DISC_11;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public void lanceHitBlock(Player player, Location bLoc, World world) {
		new PartialParticle(Particle.SMOKE_LARGE, bLoc, 25, 0, 0, 0, 0.1)
			.minimumMultiplier(false).spawnAsPlayerActive(player);
		world.playSound(bLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1, 0.85f);
		world.playSound(bLoc, Sound.ITEM_TRIDENT_HIT_GROUND, SoundCategory.PLAYERS, 1, 0.75f);
	}

	@Override
	public void lanceParticle(Player player, Location startLoc, Location endLoc) {

		Location l = startLoc.clone();
		Vector dir = startLoc.getDirection().multiply(1.0 / 3);
		double rotation = 0;
		double radius = 0.75;
		double distance = startLoc.distance(endLoc);
		for (int i = 0; i < distance * 3; i++) {
			l.add(dir);
			new PartialParticle(Particle.SMOKE_NORMAL, l, 3, 0.175, 0.2, 0.2, 0.05).spawnAsPlayerActive(player);
			rotation += 6;
			radius -= 0.75D / (distance * 3);
			for (int j = 0; j < 3; j++) {
				double radian = FastMath.toRadians(rotation + (j * 120));
				Vector vec = new Vector(FastUtils.cos(radian) * radius, 0,
					FastUtils.sin(radian) * radius);
				vec = VectorUtils.rotateXAxis(vec, l.getPitch() + 90);
				vec = VectorUtils.rotateYAxis(vec, l.getYaw());
				Location helixLoc = l.clone().add(vec);
				new PartialParticle(Particle.DUST_COLOR_TRANSITION, helixLoc, 3, 0.05, 0.05, 0.05, 0.25,
					new Particle.DustTransition(DARK_COLOR, TWISTED_COLOR, 1f))
					.minimumMultiplier(false).spawnAsPlayerActive(player);
			}
		}
	}

	@Override
	public void lanceSound(World world, Player player) {
		world.playSound(player.getLocation(), Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 1.3f, 0.9f);
		world.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1.3f, 1.75f);
		world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 1.3f, 0.7f);
		world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.3f, 0.7f);
	}

	@Override
	public void lanceHit(Location loc, Player player) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.3f, 0.7f);
		world.playSound(loc, Sound.ENTITY_PLAYER_BIG_FALL, SoundCategory.PLAYERS, 1.3f, 0);
		world.playSound(loc, Sound.ENTITY_BEE_STING, SoundCategory.PLAYERS, 1.3f, 0);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 30, 0, 0, 0, 0.15)
			.minimumMultiplier(false).spawnAsPlayerActive(player);

	}
}
