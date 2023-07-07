package com.playmonumenta.plugins.cosmetics.skills.scout;

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

public class SwiftCutsCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.SWIFT_CUTS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.STONE_SWORD;
	}

	public void onHit(Player player, Location enemyLoc, World world) {
		world.playSound(enemyLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.8f, 1.7f);
		world.playSound(enemyLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.1f, 1.2f);
		world.playSound(enemyLoc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.0f, 1.4f);
		world.playSound(enemyLoc, Sound.ENTITY_DROWNED_SHOOT, SoundCategory.PLAYERS, 0.5f, 1.4f);
		world.playSound(enemyLoc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 0.6f, 2.0f);
		world.playSound(enemyLoc, Sound.ITEM_CROSSBOW_SHOOT, SoundCategory.PLAYERS, 0.8f, 2.0f);
		new PartialParticle(Particle.SWEEP_ATTACK, enemyLoc, 2, 0.25, 0.35, 0.25, 0.001).spawnAsPlayerActive(player);
	}
}
