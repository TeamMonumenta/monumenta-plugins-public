package com.playmonumenta.plugins.cosmetics.skills.shaman.soothsayer;

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

public class ChainHealingWaveCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.CHAIN_HEALING_WAVE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.LIME_CANDLE;
	}

	public void chainHeal(Player player, LivingEntity target) {
		player.getWorld().playSound(target.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 2.0f, 1.6f);
		player.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.1f, 1.0f);
	}

	public void chainBeam(List<LivingEntity> mHitTargets, int i, LivingEntity target, Player player) {
		new PPLine(Particle.VILLAGER_HAPPY, mHitTargets.get(i).getEyeLocation().add(0, -0.5, 0), target.getEyeLocation().add(0, -0.5, 0)).countPerMeter(8).spawnAsPlayerActive(player);
	}
}
