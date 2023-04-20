package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public class EmpoweringOdorCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.EMPOWERING_ODOR;
	}

	@Override
	public Material getDisplayItem() {
		return Material.DRAGON_BREATH;
	}

	public void applyEffects(Player caster, Player target, int duration) {
		target.playSound(target.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1, 2);
		new PartialParticle(Particle.END_ROD, target.getLocation(), 15, 0.4, 0.6, 0.4, 0).spawnAsPlayerActive(caster);
	}

}
