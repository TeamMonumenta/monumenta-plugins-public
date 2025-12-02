package com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger;

import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

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

	public void periodicAppliedLocationEffects(Player player, Location loc, double radius, boolean isGruesome,
											   @Nullable AlchemistPotions alchemistPotions) {
		if (alchemistPotions == null) {
			return;
		}

		Color color = isGruesome
			? alchemistPotions.mCosmetic.getGruesomeColor()
			: alchemistPotions.mCosmetic.getBrutalColor();
		new PPCircle(Particle.REDSTONE, loc.clone().add(0, 0.1, 0), radius)
			.ringMode(false)
			.countPerMeter(1.5)
			.data(new Particle.DustOptions(color, 1f))
			.spawnAsPlayerActive(player);
	}

	public void explosionEffects(Player player, LivingEntity aberration, double radius) {
	}

	public void expireEffects(Player player, LivingEntity aberration) {

	}

}
