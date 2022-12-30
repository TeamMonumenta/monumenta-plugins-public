package com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class TransmRingCS implements CosmeticSkill {

	public static final ImmutableMap<String, TransmRingCS> SKIN_LIST = ImmutableMap.<String, TransmRingCS>builder()
		.put(RitualRingCS.NAME, new RitualRingCS())
		.build();

	private static final Particle.DustOptions GOLD_COLOR = new Particle.DustOptions(Color.fromRGB(255, 200, 0), 1.2f);

	@Override
	public @Nullable Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.TRANSMUTATION_RING;
	}

	@Override
	public Material getDisplayItem() {
		return Material.GLOWSTONE_DUST;
	}

	@Override
	public @Nullable String getName() {
		return null;
	}

	public PPCircle ringPPCircle(Location mCenter, double mRadius) {
		return new PPCircle(Particle.REDSTONE, mCenter, mRadius)
			.data(GOLD_COLOR)
			.ringMode(true);
	}

	public void ringEffect(Player mPlayer, Location mCenter, PPCircle particles, double mRadius, double ringRaidus, int tick) {
		particles.count((int) Math.floor(120 * mRadius / ringRaidus)).location(mCenter).spawnAsPlayerActive(mPlayer);
		particles.count((int) Math.floor(30 * mRadius / ringRaidus)).location(mCenter.clone().add(0, 0.5, 0)).spawnAsPlayerActive(mPlayer);
		particles.count((int) Math.floor(15 * mRadius / ringRaidus)).location(mCenter.clone().add(0, 1, 0)).spawnAsPlayerActive(mPlayer);
		particles.count((int) Math.floor(7 * mRadius / ringRaidus)).location(mCenter.clone().add(0, 1.75, 0)).spawnAsPlayerActive(mPlayer);
	}

	public void ringSoundStart(World world, Location mCenter) {
		world.playSound(mCenter, Sound.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS, 3f, 0.35f);
	}

	public void ringEffectOnKill(Player mPlayer, Location loc) {
		mPlayer.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 1, 2);
	}
}
