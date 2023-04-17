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

	public void activate(Player mPlayer, World world, Location loc) {
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 80, 0, 0, 0, 0.25).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 125, 0, 0, 0, 0.3).spawnAsPlayerActive(mPlayer);

		world.playSound(loc, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.75f, 1.5f);
		world.playSound(loc, Sound.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1f, 0f);
	}
}
