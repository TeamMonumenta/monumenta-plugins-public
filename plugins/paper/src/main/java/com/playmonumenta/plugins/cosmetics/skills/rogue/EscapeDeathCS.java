package com.playmonumenta.plugins.cosmetics.skills.rogue;

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

public class EscapeDeathCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.ESCAPE_DEATH;
	}

	@Override
	public Material getDisplayItem() {
		return Material.DRAGON_BREATH;
	}

	public void activate(Player player, World world, Location loc) {
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 80, 0, 0, 0, 0.25).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 125, 0, 0, 0, 0.3).spawnAsPlayerActive(player);

		world.playSound(loc, Sound.ENTITY_WITHER_HURT, SoundCategory.PLAYERS, 0.4f, 0.8f);
		world.playSound(loc, Sound.ENTITY_WITHER_SKELETON_HURT, SoundCategory.PLAYERS, 0.7f, 0.1f);
		world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.PLAYERS, 1.2f, 1.0f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, 0.8f, 1.0f);
		world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 2.0f, 0.6f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 1.7f, 1.5f);
		world.playSound(loc, Sound.ENTITY_WITHER_DEATH, SoundCategory.PLAYERS, 0.1f, 2.0f);
		world.playSound(loc, Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 0.4f, 2.0f);
	}
}
