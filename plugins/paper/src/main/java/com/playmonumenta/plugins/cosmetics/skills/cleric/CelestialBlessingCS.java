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
import org.bukkit.entity.Player;


public class CelestialBlessingCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.CELESTIAL_BLESSING;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SUGAR;
	}

	public void tickEffect(Player player, Player target, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		Location loc = target.getLocation().add(0, 1, 0);
		new PartialParticle(Particle.SPELL_INSTANT, loc, 2, 0.25, 0.25, 0.25, 0.1).spawnAsPlayerBuff(player);
		new PartialParticle(Particle.SPELL_INSTANT, loc, 2, 0.5, 0.5, 0.5, 0).spawnAsPlayerBuff(player);
		new PartialParticle(Particle.VILLAGER_HAPPY, loc, 2, 0.5, 0.5, 0.5, 0.1).spawnAsPlayerBuff(player);
	}

	public void loseEffect(Player player, Player target) {
		Location loc = target.getLocation();
		World world = target.getWorld();
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1f, 0.65f);
		new PartialParticle(Particle.SPELL_INSTANT, loc.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1).spawnAsPlayerBuff(player);
		new PartialParticle(Particle.SPELL_INSTANT, loc.clone().add(0, 1, 0), 25, 0.5, 0.5, 0.5, 0).spawnAsPlayerBuff(player);
		new PartialParticle(Particle.VILLAGER_HAPPY, loc.clone().add(0, 1, 0), 25, 0.5, 0.5, 0.5, 0.1).spawnAsPlayerBuff(player);
	}

	public void startEffectTargets(Player player) {
		Location loc = player.getLocation();
		World world = player.getWorld();
		world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5f, 1.75f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 0.75f, 1.25f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 0.75f, 1.1f);
	}

	public void startEffectCaster(Player caster, double radius) {

	}
}
