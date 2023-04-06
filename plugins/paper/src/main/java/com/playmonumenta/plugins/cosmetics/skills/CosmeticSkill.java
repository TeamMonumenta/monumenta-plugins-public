package com.playmonumenta.plugins.cosmetics.skills;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import java.util.List;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public interface CosmeticSkill {

	default @Nullable Cosmetic getCosmetic() {
		String name = getName();
		List<String> description = getDescription();
		if (name == null) {
			return null;
		}
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, name, false, this.getAbility(), description == null ? null : description.toArray(String[]::new));
	}

	ClassAbility getAbility();

	Material getDisplayItem();

	default @Nullable String getName() {
		return null;
	}

	default @Nullable List<String> getDescription() {
		return null;
	}

}
