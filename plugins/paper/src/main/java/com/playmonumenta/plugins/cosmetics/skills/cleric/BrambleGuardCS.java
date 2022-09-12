package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;

@Deprecated
public class BrambleGuardCS implements CosmeticSkill {
	//Earthy sanctified armor. Depth set: earth
	//Deprecated before 16th Sept

	public static final String NAME = "Bramble Guard";

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName());
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.SANCTIFIED_ARMOR;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SWEET_BERRIES;
	}

	public static void sanctHurtEffect(World world, Location loc, LivingEntity source) {
		world.spawnParticle(Particle.BLOCK_CRACK, loc.add(0, source.getHeight() / 2, 0), 30, 0.5, 0.5, 0.5, 0.125, Bukkit.createBlockData(Material.SWEET_BERRY_BUSH));
		world.playSound(loc, Sound.BLOCK_SWEET_BERRY_BUSH_BREAK, 1.05f, 0.75f);
	}
}
