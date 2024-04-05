package com.playmonumenta.plugins.cosmetics.skills.warlock;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

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

	public void rendHitParticle(Player player, Location loc) {
		new PartialParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 10, 0.75, 0.5, 0.75, 0.0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SPELL_MOB, loc.clone().add(0, 1, 0), 18, 0.75, 0.5, 0.75, 0.0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 7, 0.75, 0.5, 0.75, 0.0).spawnAsPlayerActive(player);
	}

	public void rendMarkTick(Player player, LivingEntity enemy, int marks) {
		for (int i = 1; i <= marks; i++) {
			Location loc = getMarkLocation(player, enemy, i);
			new PartialParticle(Particle.SPELL_WITCH, loc, 3, 0, 0, 0, 0).spawnAsPlayerActive(player);
			new PartialParticle(Particle.SMOKE_NORMAL, loc, 6, 0.05, 0.05, 0.05, 0.015).spawnAsPlayerActive(player);
		}
	}

	public void rendLoseMark(Player player, LivingEntity enemy, int marks) {
		World world = player.getWorld();
		Location loc = player.getLocation();
		world.playSound(loc, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 0.5f, 0.3f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 0.6f, 1.4f);
		world.playSound(loc, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.PLAYERS, 0.5f, 2.0f);
		world.playSound(loc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 0.3f, 2.0f);

		loc = getMarkLocation(player, enemy, marks);
		new PartialParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 0.2, 0), 1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(loc.getDirection().multiply(0.12)), 1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(loc.getDirection().multiply(-0.12)), 1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SPELL_WITCH, loc, 6, 0, 0, 0, 0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 18, 0.05, 0.05, 0.05, 0.02).spawnAsPlayerActive(player);
	}

	public void rendMarkDied(Player player, LivingEntity enemy, int marks) {
		for (int i = 1; i <= marks; i++) {
			Location loc = getMarkLocation(player, enemy, i);
			new PartialParticle(Particle.SPELL_WITCH, loc, 3, 0, 0, 0, 0).spawnAsPlayerActive(player);
			new PartialParticle(Particle.SMOKE_NORMAL, loc, 6, 0.05, 0.05, 0.05, 0.015).spawnAsPlayerActive(player);
			rendLoseMark(player, enemy, i);
		}
	}

	public void rendHealEffect(Player player, Player healed, LivingEntity enemy) {
		new PartialParticle(Particle.DAMAGE_INDICATOR, healed.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, 0.0).spawnAsPlayerActive(player);
	}

	public void rendAbsorptionEffect(Player player, Player healed, LivingEntity enemy) {
		//Nope!
	}

	public Location getMarkLocation(Player player, LivingEntity enemy, int number) {
		Location eyeLoc = enemy.getEyeLocation();

		Vector front = LocationUtils.getDirectionTo(player.getLocation(), eyeLoc).setY(0).normalize();
		Vector up = new Vector(0, 1, 0);
		Vector right = VectorUtils.crossProd(up, front);

		Location location = eyeLoc.clone();
		switch (number) {
			case 1 -> location.add(up.clone().multiply(1.5));
			case 2 -> location.add(up.clone().multiply(1.15)).add(right.clone().multiply(0.8));
			case 3 -> location.add(up.clone().multiply(1.15)).add(right.clone().multiply(-0.8));
			case 4 -> location.add(up.clone().multiply(0.8)).add(right.clone().multiply(1.3));
			case 5 -> location.add(up.clone().multiply(0.8)).add(right.clone().multiply(-1.3));
			default -> location.add(0, 1, 0);
		}
		location.setDirection(right);

		return location;
	}
}
