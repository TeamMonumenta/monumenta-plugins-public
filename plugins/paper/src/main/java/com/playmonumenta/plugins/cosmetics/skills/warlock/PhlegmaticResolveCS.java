package com.playmonumenta.plugins.cosmetics.skills.warlock;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class PhlegmaticResolveCS implements CosmeticSkill {
	@Override
	public ClassAbility getAbility() {
		return ClassAbility.PHLEGMATIC_RESOLVE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SHIELD;
	}

	public void periodicTrigger(Player player, Player receiver, int cooldowns) {

	}

	public void enhanceDamageTick(Player player, double radius, double[] damageTicks) {

	}

	public void enhanceDamageMob(Player player, LivingEntity mob) {
		new PartialParticle(Particle.WAX_OFF, mob.getLocation(), 6, 0.5f, 0.5f, 0.5f).spawnAsPlayerActive(player);
	}

	public void enhanceHurtSound(Player player) {
		player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 1, 1);
	}
}
