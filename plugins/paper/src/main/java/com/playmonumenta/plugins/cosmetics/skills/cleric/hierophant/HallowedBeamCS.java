package com.playmonumenta.plugins.cosmetics.skills.cleric.hierophant;

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
import org.bukkit.util.Vector;

public class HallowedBeamCS implements CosmeticSkill {

	public static final ImmutableMap<String, HallowedBeamCS> SKIN_LIST = ImmutableMap.<String, HallowedBeamCS>builder()
		.put(PrestigiousBeamCS.NAME, new PrestigiousBeamCS())
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.HALLOWED_BEAM;
	}

	@Override
	public Material getDisplayItem() {
		return Material.BOW;
	}

	@Override
	public String getName() {
		return null;
	}

	public void beamHealEffect(World world, Player mPlayer, LivingEntity pe, Vector dir, double range) {
		Location loc = mPlayer.getEyeLocation();
		world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.PLAYERS, 1, 0.85f);
		world.playSound(loc, Sound.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1, 0.9f);
		for (int i = 0; i < range; i++) {
			loc.add(dir);
			new PartialParticle(Particle.VILLAGER_HAPPY, loc, 5, 0.25, 0.25, 0.25, 0).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0.05f, 0.05f, 0.05f, 0.025f).spawnAsPlayerActive(mPlayer);
			if (loc.distance(pe.getEyeLocation()) < 1.25) {
				loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1, 1.35f);
				loc.getWorld().playSound(loc, Sound.ENTITY_ARROW_HIT, SoundCategory.PLAYERS, 1, 0.9f);
				break;
			}
		}

		// Effect on target
		new PartialParticle(Particle.SPELL_INSTANT, pe.getLocation(), 500, 2.5, 0.15f, 2.5, 1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.VILLAGER_HAPPY, pe.getLocation(), 150, 2.55, 0.15f, 2.5, 1).spawnAsPlayerActive(mPlayer);
		world.playSound(mPlayer.getEyeLocation(), Sound.ITEM_HONEY_BOTTLE_DRINK, SoundCategory.PLAYERS, 2, 1.5f);
	}

	public void beamHarm(World world, Player mPlayer, LivingEntity e, Vector dir, double radius) {
		Location loc = mPlayer.getEyeLocation();
		world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.PLAYERS, 1, 0.85f);
		world.playSound(loc, Sound.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1, 0.9f);
		for (int i = 0; i < radius; i++) {
			loc.add(dir);
			new PartialParticle(Particle.VILLAGER_HAPPY, loc, 5, 0.25, 0.25, 0.25, 0).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0.05f, 0.05f, 0.05f, 0.025f).spawnAsPlayerActive(mPlayer);
			if (loc.distance(e.getEyeLocation()) < 1.25) {
				loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1, 1.35f);
				loc.getWorld().playSound(loc, Sound.ENTITY_ARROW_HIT, SoundCategory.PLAYERS, 1, 0.9f);
				break;
			}
		}
	}

	public void beamHarmCrusade(Player mPlayer, Location eLoc) {
		new PartialParticle(Particle.SPIT, eLoc, 40, 0, 0, 0, 0.25f).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FIREWORKS_SPARK, eLoc, 75, 0, 0, 0, 0.3f).spawnAsPlayerActive(mPlayer);
	}

	public void beamHarmOther(Player mPlayer, Location eLoc) {
		new PartialParticle(Particle.SPIT, eLoc, 40, 0, 0, 0, 0.25f).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIT_MAGIC, eLoc, 30, 1, 1, 1, 0.25).spawnAsPlayerActive(mPlayer);
	}
}
