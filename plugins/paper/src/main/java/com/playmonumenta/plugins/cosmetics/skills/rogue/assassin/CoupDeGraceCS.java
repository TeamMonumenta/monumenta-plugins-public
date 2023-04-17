package com.playmonumenta.plugins.cosmetics.skills.rogue.assassin;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class CoupDeGraceCS implements CosmeticSkill {
	@Override
	public ClassAbility getAbility() {
		return ClassAbility.COUP_DE_GRACE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.WITHER_SKELETON_SKULL;
	}

	public void execution(Player mPlayer, LivingEntity le) {
		World world = le.getWorld();
		world.playSound(le.getLocation(), Sound.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 0.75f, 0.75f);
		world.playSound(le.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 0.5f, 1.5f);
		new PartialParticle(Particle.BLOCK_DUST, le.getLocation().add(0, le.getHeight() / 2, 0), 20, le.getWidth() / 2, le.getHeight() / 3, le.getWidth() / 2, 0.65, Material.REDSTONE_WIRE.createBlockData()).spawnAsPlayerActive(mPlayer);
	}

	public void executionLv2(Player mPlayer, LivingEntity le) {
		new PartialParticle(Particle.SPELL_WITCH, le.getLocation().add(0, le.getHeight() / 2, 0), 10, le.getWidth() / 2, le.getHeight() / 3, le.getWidth() / 2, 0.65).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.BLOCK_DUST, le.getLocation().add(0, le.getHeight() / 2, 0), 20, le.getWidth() / 2, le.getHeight() / 3, le.getWidth() / 2, 0.65, Material.REDSTONE_BLOCK.createBlockData()).spawnAsPlayerActive(mPlayer);
	}
}
