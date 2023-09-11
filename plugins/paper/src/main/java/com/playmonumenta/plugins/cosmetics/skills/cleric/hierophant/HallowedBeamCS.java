package com.playmonumenta.plugins.cosmetics.skills.cleric.hierophant;

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
import org.bukkit.util.Vector;

public class HallowedBeamCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.HALLOWED_BEAM;
	}

	@Override
	public Material getDisplayItem() {
		return Material.BOW;
	}

	public void beamHealEffect(World world, Player player, LivingEntity pe, Vector dir, double range) {
		Location loc = player.getEyeLocation();
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1.2f, 2.0f);
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 0.5f, 1.0f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 0.5f, 2.0f);

		for (int i = 0; i < range; i++) {
			loc.add(dir);
			new PartialParticle(Particle.VILLAGER_HAPPY, loc, 5, 0.25, 0.25, 0.25, 0).spawnAsPlayerActive(player);
			new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0.05f, 0.05f, 0.05f, 0.025f).spawnAsPlayerActive(player);
			if (loc.distance(pe.getEyeLocation()) < 1.25) {
				break;
			}
		}

		// Effect on target
		Location targetLoc = pe.getLocation();
		new PartialParticle(Particle.SPELL_INSTANT, targetLoc, 500, 2.5, 0.15f, 2.5, 1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.VILLAGER_HAPPY, targetLoc, 150, 2.55, 0.15f, 2.5, 1).spawnAsPlayerActive(player);
		world.playSound(targetLoc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 0.3f, 1.2f);
		world.playSound(targetLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.1f, 1.5f);
		world.playSound(targetLoc, Sound.ENTITY_ALLAY_HURT, SoundCategory.PLAYERS, 0.5f, 0.4f);
	}

	public void beamHarm(World world, Player player, LivingEntity e, Vector dir, double radius) {
		Location loc = player.getEyeLocation();
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1.4f, 1.3f);
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 0.5f, 0.6f);
		world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_DEATH, SoundCategory.PLAYERS, 0.3f, 1.5f);
		world.playSound(loc, Sound.ENTITY_WARDEN_DEATH, SoundCategory.PLAYERS, 0.1f, 2.0f);
		for (int i = 0; i < radius; i++) {
			loc.add(dir);
			new PartialParticle(Particle.VILLAGER_HAPPY, loc, 5, 0.25, 0.25, 0.25, 0).spawnAsPlayerActive(player);
			new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0.05f, 0.05f, 0.05f, 0.025f).spawnAsPlayerActive(player);
			if (loc.distance(e.getEyeLocation()) < 1.25) {
				break;
			}
		}

		Location targetLoc = e.getLocation();
		world.playSound(targetLoc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 0.3f, 1.2f);
		world.playSound(targetLoc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, 0.7f, 1.2f);
		world.playSound(targetLoc, Sound.ENTITY_ALLAY_HURT, SoundCategory.PLAYERS, 0.5f, 0.4f);
		world.playSound(targetLoc, Sound.ENTITY_ALLAY_HURT, SoundCategory.PLAYERS, 0.5f, 0.1f);
		world.playSound(targetLoc, Sound.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.1f, 1.1f);
		world.playSound(targetLoc, Sound.ITEM_TRIDENT_HIT_GROUND, SoundCategory.PLAYERS, 1.4f, 2.0f);
	}

	public void beamHarmCrusade(Player player, Location eLoc) {
		new PartialParticle(Particle.SPIT, eLoc, 40, 0, 0, 0, 0.25f).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FIREWORKS_SPARK, eLoc, 75, 0, 0, 0, 0.3f).spawnAsPlayerActive(player);
	}

	public void beamHarmOther(Player player, Location eLoc) {
		new PartialParticle(Particle.SPIT, eLoc, 40, 0, 0, 0, 0.25f).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT_MAGIC, eLoc, 30, 1, 1, 1, 0.25).spawnAsPlayerActive(player);
	}
}
