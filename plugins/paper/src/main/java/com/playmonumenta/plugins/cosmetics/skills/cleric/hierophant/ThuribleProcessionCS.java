package com.playmonumenta.plugins.cosmetics.skills.cleric.hierophant;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class ThuribleProcessionCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.THURIBLE_PROCESSION;
	}

	@Override
	public Material getDisplayItem() {
		return Material.GLOWSTONE_DUST;
	}

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(255, 195, 0), 1.0f);

	public void endBuildupEffect(Player player) {
		World world = player.getWorld();
		world.playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1, 1);
		new PartialParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 60, 0, 0, 0, 0.35).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SPELL_INSTANT, player.getLocation(), 60, 0.4, 0.4, 0.4, 1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SPELL_INSTANT, player.getLocation(), 200, 5, 3, 5, 1).spawnAsPlayerActive(player);
	}

	public void firstBuffs(Player player) {
		new PartialParticle(Particle.REDSTONE, player.getLocation(), 5, 0.4, 0.4, 0.4, COLOR).spawnAsPlayerPassive(player);
	}

	public void secondBuffs(Player player) {
		new PartialParticle(Particle.REDSTONE, player.getLocation(), 5, 0.4, 0.4, 0.4, COLOR).spawnAsPlayerPassive(player);
	}

	public void thirdBuffs(Player player) {
		new PartialParticle(Particle.REDSTONE, player.getLocation(), 5, 0.4, 0.4, 0.4, COLOR).spawnAsPlayerPassive(player);
	}

	public void fourthBuffs(Player player) {
		new PartialParticle(Particle.REDSTONE, player.getLocation(), 5, 0.4, 0.4, 0.4, COLOR).spawnAsPlayerPassive(player);
	}
}
