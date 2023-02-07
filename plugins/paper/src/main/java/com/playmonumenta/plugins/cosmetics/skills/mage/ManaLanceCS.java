package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
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

public class ManaLanceCS implements CosmeticSkill {

	public static final ImmutableMap<String, ManaLanceCS> SKIN_LIST = ImmutableMap.<String, ManaLanceCS>builder()
		.put(DarkLanceCS.NAME, new DarkLanceCS())
		.put(HallowedLanceCS.NAME, new HallowedLanceCS())
		.put(TwistedLanceCS.NAME, new TwistedLanceCS())
		.build();

	private static final Particle.DustOptions MANA_LANCE_COLOR = new Particle.DustOptions(Color.fromRGB(91, 187, 255), 1.0f);

	@Override
	public @Nullable Cosmetic getCosmetic() {
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

	@Override
	public @Nullable String getName() {
		return null;
	}

	public void lanceHitBlock(Player mPlayer, Location bLoc, World world) {
		world.playSound(bLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1, 1.65f);
		new PartialParticle(Particle.CLOUD, bLoc, 30, 0, 0, 0, 0.125).spawnAsPlayerActive(mPlayer);
	}

	public void lanceParticle(Player mPlayer, Location startLoc, Location endLoc) {
		new PPLine(Particle.EXPLOSION_NORMAL, startLoc, endLoc).shiftStart(0.75).countPerMeter(2).minParticlesPerMeter(0).delta(0.05).extra(0.025).spawnAsPlayerActive(mPlayer);
		new PPLine(Particle.REDSTONE, startLoc, endLoc).shiftStart(0.75).countPerMeter(18).delta(0.35).data(MANA_LANCE_COLOR).spawnAsPlayerActive(mPlayer);
	}

	public void lanceSound(World world, Player mPlayer) {
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 1, 1.75f);
	}

	public void lanceHit(Location loc, Player mPlayer) {

	}
}
