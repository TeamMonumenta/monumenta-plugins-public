package com.playmonumenta.plugins.cosmetics.skills;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import org.bukkit.Material;

public interface CosmeticSkill {

	Cosmetic getCosmetic();

	ClassAbility getAbilityName();

	Material getDisplayItem();

}
