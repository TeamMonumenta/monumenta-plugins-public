package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class DarkLanceCS extends ManaLanceCS {
	// Literally dark mana lance.
	// The first cosmetic skill! Maybe save it for gallery set?

	public static final String NAME = "Dark Lance";

	private static final Particle.DustOptions DARK_LANCE_COLOR = new Particle.DustOptions(Color.fromRGB(102, 0, 118), 1.0f);

	@Override
	public @Nullable Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName());
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.MANA_LANCE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.WITHER_ROSE;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public void lanceHitBlock(Player player, Location bLoc, World world) {
		new PartialParticle(Particle.SOUL_FIRE_FLAME, bLoc, 40, 0, 0, 0, 0.1).spawnAsPlayerActive(player);
		world.playSound(bLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1, 1.65f);
	}

	@Override
	public void lanceParticle(Player player, Location startLoc, Location endLoc) {
		new PPLine(Particle.DRAGON_BREATH, startLoc, endLoc).shiftStart(0.75).countPerMeter(2).minParticlesPerMeter(0).delta(0.1).extra(0.03).spawnAsPlayerActive(player);
		new PPLine(Particle.REDSTONE, startLoc, endLoc).shiftStart(0.75).countPerMeter(18).delta(0.35).data(DARK_LANCE_COLOR).spawnAsPlayerActive(player);
	}

	@Override
	public void lanceSound(World world, Player player) {
		world.playSound(player.getLocation(), Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 1.25f, 1.25f);
	}
}
