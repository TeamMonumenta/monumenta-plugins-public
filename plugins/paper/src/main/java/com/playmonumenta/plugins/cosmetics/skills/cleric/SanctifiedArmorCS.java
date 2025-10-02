package com.playmonumenta.plugins.cosmetics.skills.cleric;

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

public class SanctifiedArmorCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.SANCTIFIED_ARMOR;
	}

	@Override
	public Material getDisplayItem() {
		return Material.IRON_CHESTPLATE;
	}

	public void sanctOnTrigger(World world, Player player, Location loc) {
		world.playSound(loc, Sound.ITEM_ARMOR_EQUIP_NETHERITE, SoundCategory.PLAYERS, 1.3f, 0.8f);
		world.playSound(loc, Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.1f, 1.15f);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc.add(0, player.getHeight() / 2, 0), 15, 0.35, 0.35, 0.35, 0.125).spawnAsPlayerPassive(player);
	}

	public void sanctApply1(World world, Player player, Location loc, LivingEntity source) {
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1.1f, 1.6f);
		world.playSound(loc, Sound.ENTITY_GUARDIAN_HURT, SoundCategory.PLAYERS, 1.2f, 1.4f);
		world.playSound(loc, Sound.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.5f, 1.4f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 0.9f, 1.4f);
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.2f, 0.1f);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc.add(0, source.getHeight() / 2, 0), 7, 0.35, 0.35, 0.35, 0.125).spawnAsPlayerPassive(player);
	}

	public void sanctApply2(World world, Player player, Location loc, LivingEntity source) {
		sanctApply1(world, player, loc, source);
	}

	public void sanctOnHeal(Player player, Location loc, LivingEntity enemy) {
		player.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.65f, 1.25f);
	}
}
