package com.playmonumenta.plugins.cosmetics.skills.scout.hunter;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class PinningShotCS implements CosmeticSkill {

	public static final ImmutableMap<String, PinningShotCS> SKIN_LIST = ImmutableMap.<String, PinningShotCS>builder()
		.put(PrestigiousPinningShotCS.NAME, new PrestigiousPinningShotCS())
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.PINNING_SHOT;
	}

	@Override
	public Material getDisplayItem() {
		return Material.CROSSBOW;
	}

	@Override
	public String getName() {
		return null;
	}

	public void pinEffect1(World world, Player mPlayer, LivingEntity enemy) {
		Location eLoc = enemy.getLocation();
		world.playSound(eLoc, Sound.BLOCK_SLIME_BLOCK_PLACE, SoundCategory.PLAYERS, 1, 0.5f);
		new PartialParticle(Particle.EXPLOSION_NORMAL, eLoc, 8, 0, 0, 0, 0.2).spawnAsPlayerActive(mPlayer);
	}

	public void pinEffect2(World world, Player mPlayer, LivingEntity enemy) {
		Location eLoc = enemy.getEyeLocation();
		world.playSound(eLoc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1, 0.5f);
		new PartialParticle(Particle.FIREWORKS_SPARK, eLoc, 20, 0, 0, 0, 0.2).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SNOWBALL, eLoc, 30, 0, 0, 0, 0.25).spawnAsPlayerActive(mPlayer);
	}
}
