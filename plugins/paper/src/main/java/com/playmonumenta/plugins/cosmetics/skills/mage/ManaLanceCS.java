package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.*;
import org.bukkit.entity.Player;

public class ManaLanceCS implements CosmeticSkill {

	public static final ImmutableMap<String, ManaLanceCS> SKIN_LIST = ImmutableMap.<String, ManaLanceCS>builder()
		.put(DarkLanceCS.NAME, new DarkLanceCS())
		.put(HallowedLanceCS.NAME, new HallowedLanceCS())
		.put(TwistedLanceCS.NAME, new TwistedLanceCS())
		.build();

	private static final Particle.DustOptions MANA_LANCE_COLOR = new Particle.DustOptions(Color.fromRGB(91, 187, 255), 1.0f);

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.MANA_LANCE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.TRIDENT;
	}

	public void lanceHitBlock(Player mPlayer, Location bLoc, World world) {
		new PartialParticle(Particle.CLOUD, bLoc, 30, 0, 0, 0, 0.125).spawnAsPlayerActive(mPlayer);
		world.playSound(bLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.65f);
	}

	public void lanceParticle(Player mPlayer, Location loc, Location endLoc, int iterations, double range) {
		new PPLine(Particle.EXPLOSION_NORMAL, loc, endLoc).shiftStart(0.75).countPerMeter(2).minParticlesPerMeter(0).delta(0.05).extra(0.025).spawnAsPlayerActive(mPlayer);
		new PPLine(Particle.REDSTONE, loc, endLoc).shiftStart(0.75).countPerMeter(18).delta(0.35).data(MANA_LANCE_COLOR).spawnAsPlayerActive(mPlayer);
	}

	public void lanceSound(World world, Player mPlayer) {
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1, 1.75f);
	}

	public void lanceHit(Location loc, Player mPlayer) {

	}
}
