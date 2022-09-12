package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.*;
import org.bukkit.entity.Player;

public class TwistedLanceCS extends ManaLanceCS {
	//Delve theme

	public static final String NAME = "Twisted Lance";

	private static final Particle.DustOptions TWISTED_COLOR = new Particle.DustOptions(Color.fromRGB(127, 0, 0), 1.0f);
	private static final Particle.DustOptions DARK_COLOR = new Particle.DustOptions(Color.fromRGB(0, 31, 95), 1.0f);

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
		return Material.MUSIC_DISC_11;
	}

	@Override
	public void lanceHit(Player mPlayer, Location bLoc, World world) {
		new PartialParticle(Particle.SMOKE_NORMAL, bLoc, 25, 0, 0, 0, 0.1).spawnAsPlayerActive(mPlayer);
		world.playSound(bLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.25f);
	}

	@Override
	public void lanceParticle(Player mPlayer, Location loc, Location endLoc) {
		new PPLine(Particle.SMOKE_NORMAL, loc, endLoc).shiftStart(0.75).countPerMeter(2).minParticlesPerMeter(0).delta(0.05).extra(0.001).spawnAsPlayerActive(mPlayer);
		new PPLine(Particle.SPELL_MOB, loc, endLoc).shiftStart(0.75).countPerMeter(8).delta(0.05).spawnAsPlayerActive(mPlayer);
		new PPLine(Particle.REDSTONE, loc, endLoc).shiftStart(0.75).countPerMeter(16).delta(0.3).data(TWISTED_COLOR).spawnAsPlayerActive(mPlayer);
		new PPLine(Particle.REDSTONE, loc, endLoc).shiftStart(0.75).countPerMeter(8).delta(0.3).data(DARK_COLOR).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void lanceSound(World world, Player mPlayer) {
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1.3f, 0.9f);
	}
}
