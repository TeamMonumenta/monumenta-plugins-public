package com.playmonumenta.plugins.cosmetics.skills.warlock;

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

public class SoulRendCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.SOUL_REND;
	}

	@Override
	public Material getDisplayItem() {
		return Material.POTION;
	}

	public void rendHitSound(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.PLAYERS, 0.4f, 1.5f);
		world.playSound(loc, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 0.7f, 0.3f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 0.8f, 1.4f);
		world.playSound(loc, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.PLAYERS, 0.7f, 2.0f);
		world.playSound(loc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 0.6f, 2.0f);
	}

	public void rendHitParticle1(Player player, Location loc) {
		new PartialParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 10, 0.75, 0.5, 0.75, 0.0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SPELL_MOB, loc.clone().add(0, 1, 0), 18, 0.75, 0.5, 0.75, 0.0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 7, 0.75, 0.5, 0.75, 0.0).spawnAsPlayerActive(player);
	}

	public void rendHitParticle2(Player player, Location loc, double radius) {
		new PartialParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 75, 3.5, 1.5, 3.5, 0.0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SPELL_MOB, loc.clone().add(0, 1, 0), 95, 3.5, 1.5, 3.5, 0.0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 45, 3.5, 1.5, 3.5, 0.0).spawnAsPlayerActive(player);
	}

	public void rendHealEffect(Player player, Player healed, LivingEntity enemy) {
		new PartialParticle(Particle.DAMAGE_INDICATOR, healed.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, 0.0).spawnAsPlayerActive(player);
	}

	public void rendAbsorptionEffect(Player player, Player healed, LivingEntity enemy) {
		//Nope!
	}
}
