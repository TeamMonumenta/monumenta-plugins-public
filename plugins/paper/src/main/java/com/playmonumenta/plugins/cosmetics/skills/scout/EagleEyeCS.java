package com.playmonumenta.plugins.cosmetics.skills.scout;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class EagleEyeCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.EAGLE_EYE;
	}
	@Override
	public Material getDisplayItem() {
		return Material.ENDER_EYE;
	}

	public void eyeStart(World world, Player player, Location loc) {
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 1.0f, 0.6f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 1.0f, 1.0f);
		world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.0f, 1.0f);
		world.playSound(loc, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 0.4f, 0.1f);
	}

	public void eyeOnTarget(World world, Player player, LivingEntity mob) {
		world.playSound(mob.getLocation(), Sound.ENTITY_PARROT_IMITATE_SHULKER, SoundCategory.PLAYERS, 0.4f, 0.7f);
		new PartialParticle(Particle.FIREWORKS_SPARK, mob.getLocation().add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(player);
	}

	public void eyeFirstStrike(World world, Player player, LivingEntity mob) {
		//Nope!
	}

	public NamedTextColor enhancementGlowColor() {
		return NamedTextColor.YELLOW;
	}
}
