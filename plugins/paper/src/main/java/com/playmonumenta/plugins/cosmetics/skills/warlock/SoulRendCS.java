package com.playmonumenta.plugins.cosmetics.skills.warlock;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SoulRendCS implements CosmeticSkill {

	public static final ImmutableMap<String, SoulRendCS> SKIN_LIST = ImmutableMap.<String, SoulRendCS>builder()
		.put(VampiricDrainCS.NAME, new VampiricDrainCS())
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.SOUL_REND;
	}

	@Override
	public Material getDisplayItem() {
		return Material.POTION;
	}

	@Override
	public String getName() {
		return null;
	}

	public void rendHitSound(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, 0.4f, 1.5f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.4f, 1.15f);
	}

	public void rendHitParticle1(Player mPlayer, Location loc) {
		new PartialParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 10, 0.75, 0.5, 0.75, 0.0).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SPELL_MOB, loc.clone().add(0, 1, 0), 18, 0.75, 0.5, 0.75, 0.0).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 7, 0.75, 0.5, 0.75, 0.0).spawnAsPlayerActive(mPlayer);
	}

	public void rendHitParticle2(Player mPlayer, Location loc, double radius) {
		new PartialParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 75, 3.5, 1.5, 3.5, 0.0).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SPELL_MOB, loc.clone().add(0, 1, 0), 95, 3.5, 1.5, 3.5, 0.0).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 45, 3.5, 1.5, 3.5, 0.0).spawnAsPlayerActive(mPlayer);
	}

	public void rendHealEffect(Player mPlayer, Player healed, LivingEntity enemy) {
		new PartialParticle(Particle.DAMAGE_INDICATOR, healed.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, 0.0).spawnAsPlayerActive(mPlayer);
	}

	public void rendAbsorptionEffect(Player mPlayer, Player healed, LivingEntity enemy) {
		//Nope!
	}
}
