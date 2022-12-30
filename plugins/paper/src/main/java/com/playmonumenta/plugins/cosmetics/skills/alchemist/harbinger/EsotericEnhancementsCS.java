package com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class EsotericEnhancementsCS implements CosmeticSkill {

	public static final ImmutableMap<String, EsotericEnhancementsCS> SKIN_LIST = ImmutableMap.<String, EsotericEnhancementsCS>builder()
		.put(PrestigiousEsotericCS.NAME, new PrestigiousEsotericCS())
		.build();

	private static final String ABERRATION_LOS = "Alchemicalaberration";

	@Override
	public @Nullable Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.ESOTERIC_ENHANCEMENTS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.CREEPER_HEAD;
	}

	@Override
	public @Nullable String getName() {
		return null;
	}

	public String getLos() {
		return ABERRATION_LOS;
	}

	public void esotericSummonEffect(World world, Player mPlayer, Location mLoc) {
		//Nope!
	}
}
