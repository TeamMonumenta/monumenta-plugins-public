package com.playmonumenta.plugins.cosmetics.skills.mage;

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

public class PrismaticShieldCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.PRISMATIC_SHIELD;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SHIELD;
	}

	public void prismaEffect(World world, Player player, Location loc, double radius) {
		world.playSound(loc, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.5f, 1.8f);
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 0.5f, 2.0f);
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 0.8f, 1.4f);
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 0.8f, 1.0f);
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 2.0f, 1.6f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.4f, 1.4f);
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.5f, 0.7f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.0f, 1.4f);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc.clone().add(0, 1.15, 0), 150, 0.2, 0.35, 0.2, 0.5).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SPELL_INSTANT, loc.clone().add(0, 1.15, 0), 100, 0.2, 0.35, 0.2, 1).spawnAsPlayerActive(player);
	}

	public void prismaOnStun(LivingEntity mob, int stunTime, Player player) {
		//Nope!
	}

	public void prismaOnHeal(Player player) {
		//Nope!
	}
}
