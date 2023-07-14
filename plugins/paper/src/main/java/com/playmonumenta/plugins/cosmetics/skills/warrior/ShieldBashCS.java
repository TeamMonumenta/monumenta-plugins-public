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

public class ShieldBashCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.SHIELD_BASH;
	}

	@Override
	public Material getDisplayItem() {
		return Material.IRON_DOOR;
	}

	public void onCast(Player player, World world, Location playerLoc, Location mobLoc) {
		new PartialParticle(Particle.CRIT, mobLoc, 50, 0, 0.25, 0, 0.25).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT_MAGIC, mobLoc, 50, 0, 0.25, 0, 0.25).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CLOUD, mobLoc, 5, 0.15, 0.5, 0.15, 0).spawnAsPlayerActive(player);
		world.playSound(playerLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0f, 0.1f);
		world.playSound(playerLoc, Sound.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0f, 0.1f);
		world.playSound(playerLoc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.0f, 0.1f);
		world.playSound(playerLoc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 0.5f, 0.1f);
		world.playSound(playerLoc, Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 0.5f, 0.1f);
		world.playSound(playerLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 0.3f, 0.1f);
	}

	public void onParry(World world, Location loc) {
		world.playSound(loc, Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.0f, 2.0f);
	}
}
