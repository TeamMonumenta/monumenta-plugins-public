package com.playmonumenta.plugins.cosmetics.skills.warrior;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class BrambleShellCS extends CounterStrikeCS {
	//Earthy counter strike. Depth set: earth

	public static final String NAME = "Bramble Shell";

	private static final Particle.DustOptions EARTH_COLOR = new Particle.DustOptions(Color.fromRGB(51, 102, 0), 1.0f);

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName());
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.COUNTER_STRIKE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SWEET_BERRIES;
	}

	@Override
	public void counterOnHurt(Player mPlayer, Location loc, LivingEntity source) {
		new PartialParticle(Particle.SWEEP_ATTACK, loc, 4, 0.75, 0.5, 0.75, 0.001).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.BLOCK_CRACK, loc, 12, 0.75, 0.5, 0.75, 0.15, Bukkit.createBlockData(Material.SWEET_BERRY_BUSH)).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.BLOCK_CRACK, loc, 4, 0.75, 0.5, 0.75, 0.1, Bukkit.createBlockData(Material.RAW_IRON_BLOCK)).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc, 8, 0.75, 0.5, 0.75, 0.1, EARTH_COLOR).spawnAsPlayerActive(mPlayer);
		mPlayer.playSound(loc, Sound.BLOCK_SWEET_BERRY_BUSH_BREAK, 1.25f, 0.75f);
	}
}
