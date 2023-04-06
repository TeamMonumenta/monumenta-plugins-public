package com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class EsotericEnhancementsCS implements CosmeticSkill {

	private static final String ABERRATION_LOS = "Alchemicalaberration";

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.ESOTERIC_ENHANCEMENTS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.CREEPER_HEAD;
	}

	public String getLos() {
		return ABERRATION_LOS;
	}

	public void esotericSummonEffect(World world, Player mPlayer, Location mLoc) {
		//Nope!
	}
}
