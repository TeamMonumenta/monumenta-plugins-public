package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.Material;

public class BrutalAlchemyCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.BRUTAL_ALCHEMY;
	}

	@Override
	public Material getDisplayItem() {
		return Material.REDSTONE;
	}

	// See GruesomeAlchemyCS regarding cosmetics for this skill.

}
