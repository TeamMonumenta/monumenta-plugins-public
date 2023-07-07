package com.playmonumenta.plugins.cosmetics.skills.scout.hunter;

import com.playmonumenta.plugins.classes.ClassAbility;
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

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.PINNING_SHOT;
	}

	@Override
	public Material getDisplayItem() {
		return Material.CROSSBOW;
	}

	public void pinEffect1(World world, Player player, LivingEntity enemy) {
		Location eLoc = enemy.getLocation();
		world.playSound(eLoc, Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.PLAYERS, 2.0f, 0.5f);
		world.playSound(eLoc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 1.0f, 1.2f);
		world.playSound(eLoc, Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 1.0f, 1.2f);
		new PartialParticle(Particle.EXPLOSION_NORMAL, eLoc, 8, 0, 0, 0, 0.2).spawnAsPlayerActive(player);
	}

	public void pinEffect2(World world, Player player, LivingEntity enemy) {
		Location eLoc = enemy.getEyeLocation();
		world.playSound(eLoc, Sound.ITEM_TRIDENT_HIT_GROUND, SoundCategory.PLAYERS, 2.0f, 1.6f);
		world.playSound(eLoc, Sound.ENTITY_ARROW_HIT, SoundCategory.PLAYERS, 1.0f, 1.0f);
		world.playSound(eLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0f, 2.0f);
		world.playSound(eLoc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.9f, 1.0f);
		world.playSound(eLoc, Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 0.8f, 0.1f);
		world.playSound(eLoc, Sound.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 2.0f, 1.2f);
		new PartialParticle(Particle.FIREWORKS_SPARK, eLoc, 20, 0, 0, 0, 0.2).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SNOWBALL, eLoc, 30, 0, 0, 0, 0.25).spawnAsPlayerActive(player);
	}
}
