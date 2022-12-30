package com.playmonumenta.plugins.cosmetics.skills;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public interface CosmeticSkill {

	@Nullable Cosmetic getCosmetic();

	ClassAbility getAbilityName();

	Material getDisplayItem();

	@Nullable String getName();
}
