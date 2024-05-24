package com.playmonumenta.plugins.cosmetics.skills.warrior;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.Material;
import org.bukkit.entity.Player;


public class FrenzyCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.FRENZY;
	}

	@Override
	public Material getDisplayItem() {
		return Material.STONE_AXE;
	}

	public void frenzyLevelOne(Player player) {

	}

	public void frenzyLevelTwo(Player player) {

	}

	public void frenzyEnhancement(Player player) {

	}
}
