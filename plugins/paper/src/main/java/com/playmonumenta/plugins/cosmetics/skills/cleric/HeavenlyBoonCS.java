package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.utils.PotionUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;

public class HeavenlyBoonCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.HEAVENLY_BOON;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SPLASH_POTION;
	}

	public void splashEffectRegeneration(Player player) {
		PotionUtils.splashPotionParticlesAndSound(player, Color.fromRGB(255, 102, 204));
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_COW_BELL, SoundCategory.PLAYERS, 0.65f, 2f);
	}

	public void splashEffectSpeed(Player player) {
		PotionUtils.splashPotionParticlesAndSound(player, Color.fromRGB(102, 204, 255));
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_COW_BELL, SoundCategory.PLAYERS, 0.65f, 2f);
	}

	public void splashEffectStrength(Player player) {
		PotionUtils.splashPotionParticlesAndSound(player, Color.fromRGB(153, 0, 51));
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_COW_BELL, SoundCategory.PLAYERS, 0.65f, 2f);
	}

	public void splashEffectResistance(Player player) {
		PotionUtils.splashPotionParticlesAndSound(player, Color.fromRGB(102, 153, 153));
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_COW_BELL, SoundCategory.PLAYERS, 0.65f, 2f);
	}

	public void splashEffectAbsorption(Player player) {
		PotionUtils.splashPotionParticlesAndSound(player, Color.fromRGB(255, 214, 0));
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_COW_BELL, SoundCategory.PLAYERS, 0.65f, 2f);
	}
}
