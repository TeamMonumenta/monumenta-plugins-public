package com.playmonumenta.plugins.cosmetics.skills.shaman.soothsayer;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;

public class SanctuaryCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.SANCTUARY;
	}

	@Override
	public Material getDisplayItem() {
		return Material.AMETHYST_SHARD;
	}

	public void sanctuaryLevelOne(LivingEntity target) {

	}

	public void sanctuaryLevelTwo(LivingEntity target) {

	}
}
