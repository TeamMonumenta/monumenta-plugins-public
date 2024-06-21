package com.playmonumenta.plugins.cosmetics.skills.shaman;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPLine;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ChainLightningCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.CHAIN_LIGHTNING;
	}

	@Override
	public Material getDisplayItem() {
		return Material.BLAZE_ROD;
	}

	public void chainLightningCast(Player player, List<LivingEntity> mHitTargets, LivingEntity target, int i) {
		new PPLine(Particle.END_ROD, mHitTargets.get(i).getEyeLocation().add(0, -0.5, 0), target.getEyeLocation().add(0, -0.5, 0), 0.08).deltaVariance(true).countPerMeter(8).spawnAsPlayerActive(player);
	}

	public void chainLightningSound(Player player) {
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.PLAYERS, 2.0f, 2.0f);
	}
}
