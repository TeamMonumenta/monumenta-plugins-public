package com.playmonumenta.plugins.cosmetics.skills.warrior;

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

public class BruteForceCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.BRUTE_FORCE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.STONE_AXE;
	}

	public void bruteOnDamage(Player player, World world, Location loc, double radius, int combo) {
		world.playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.PLAYERS, 0.2f, 1.6f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0f, 0.5f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.0f, 0.5f);
		world.playSound(loc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 0.2f, 0.5f);
		new PartialParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 10, 0, 0, 0, 0.135).spawnAsPlayerActive(player);
	}
}
