package com.playmonumenta.plugins.cosmetics.skills.warrior;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class WeaponMasteryCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.WEAPON_MASTERY;
	}

	@Override
	public Material getDisplayItem() {
		return Material.STONE_SWORD;
	}

	public void weaponMasteryAxeHit(Player player) {

	}

	public void weaponMasterySwordHit(Player player) {

	}

	public void weaponMasteryBleedApply(Player player) {

	}
}
