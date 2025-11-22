package com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class EsotericEnhancementsCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.ESOTERIC_ENHANCEMENTS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.CREEPER_HEAD;
	}

	public String getAberrationName() {
		return "Alchemical Aberration";
	}

	public void esotericSummonEffect(Player player, Creeper aberration) {
		//Nope!
	}

	public void periodicEffects(Player player, Creeper aberration, int ticks) {
		if (ticks % 2 == 0) {
			new PartialParticle(Particle.REDSTONE, LocationUtils.getEntityCenter(aberration), 2)
				.delta(0.5)
				.data(new Particle.DustOptions(Color.PURPLE, 2))
				.spawnAsPlayerActive(player);
			new PartialParticle(Particle.REDSTONE, LocationUtils.getEntityCenter(aberration), 1)
				.data(new Particle.DustOptions(Color.BLUE, 2))
				.spawnAsPlayerActive(player);
		}
	}

	public void explosionEffects(Player player, LivingEntity aberration, double radius) {
	}

	public void expireEffects(Player player, LivingEntity aberration) {

	}

}
