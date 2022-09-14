package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.*;
import org.bukkit.entity.Player;

public class HallowedLanceCS extends ManaLanceCS {
	// Test only. What happens if two skins for one spell?

	public static final String NAME = "Hallowed Lance";

	private static final Particle.DustOptions HALLOWED_LANCE_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 80), 1.0f);

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName());
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.MANA_LANCE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.GLOWSTONE_DUST;
	}

	@Override
	public void lanceHit(Player mPlayer, Location bLoc, World world) {
		new PartialParticle(Particle.CLOUD, bLoc, 20, 0, 0, 0, 0.1).spawnAsPlayerActive(mPlayer);
		world.playSound(bLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.65f);
	}

	@Override
	public void lanceParticle(Player mPlayer, Location loc, Location endLoc) {
		new PPLine(Particle.END_ROD, loc, endLoc).shiftStart(0.75).countPerMeter(2).minParticlesPerMeter(0).delta(0.1).extra(0.03).spawnAsPlayerActive(mPlayer);
		new PPLine(Particle.REDSTONE, loc, endLoc).shiftStart(0.75).countPerMeter(18).delta(0.35).data(HALLOWED_LANCE_COLOR).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void lanceSound(World world, Player mPlayer) {
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1.0f, 1.75f);
	}
}
