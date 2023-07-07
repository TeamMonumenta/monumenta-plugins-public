package com.playmonumenta.plugins.cosmetics.skills.warlock;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class AmplifyingHexCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.AMPLIFYING;
	}

	@Override
	public Material getDisplayItem() {
		return Material.DRAGON_BREATH;
	}

	public void amplifyingParticle(Player player, Location l) {
		new PartialParticle(Particle.DRAGON_BREATH, l, 2, 0.05, 0.05, 0.05, 0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SMOKE_NORMAL, l, 3, 0.05, 0.05, 0.05, 0.1).spawnAsPlayerActive(player);
	}

	public void amplifyingEffects(Player player, World world, Location soundLoc) {
		world.playSound(soundLoc, Sound.ENTITY_BLAZE_DEATH, SoundCategory.PLAYERS, 0.4f, 0.7f);
		world.playSound(soundLoc, Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 0.3f, 0.9f);
		world.playSound(soundLoc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 2.0f, 0.6f);
		world.playSound(soundLoc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 2.0f, 0.6f);
		world.playSound(soundLoc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 1.3f, 1.4f);
		world.playSound(soundLoc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 1.4f, 0.6f);
		world.playSound(soundLoc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.6f, 0.7f);
	}

	public double amplifyingAngle(double angle, double radius) {
		return 0;
	}

	public double amplifyingHeight(double radius, double max) {
		return 0;
	}
}
