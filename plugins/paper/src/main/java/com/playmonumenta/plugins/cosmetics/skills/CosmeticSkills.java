package com.playmonumenta.plugins.cosmetics.skills;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.*;
import com.playmonumenta.plugins.cosmetics.skills.cleric.*;
import com.playmonumenta.plugins.cosmetics.skills.mage.*;
import com.playmonumenta.plugins.cosmetics.skills.rogue.*;
import com.playmonumenta.plugins.cosmetics.skills.scout.*;
import com.playmonumenta.plugins.cosmetics.skills.scout.hunter.*;
import com.playmonumenta.plugins.cosmetics.skills.warlock.*;
import com.playmonumenta.plugins.cosmetics.skills.warrior.*;
import com.playmonumenta.scriptedquests.internal.com.google.common.collect.ImmutableList;
import org.bukkit.Material;

public class CosmeticSkills {

	private static final ImmutableMap<String, CosmeticSkill> COSMETIC_SKILLS =
		ImmutableMap.<String, CosmeticSkill>builder()
			//Alchemist
			.put(SunriseBrewCS.NAME, new SunriseBrewCS())
			.put(GruesomeEchoesCS.NAME, new GruesomeEchoesCS())
			//Cleric
			.put(DarkPunishmentCS.NAME, new DarkPunishmentCS())
			.put(TouchOfEntropyCS.NAME, new TouchOfEntropyCS())
			//Mage
			.put(VolcanicBurstCS.NAME, new VolcanicBurstCS())
			.put(TwistedLanceCS.NAME, new TwistedLanceCS())
			//Rogue
			.put(WindStepCS.NAME, new WindStepCS())
			.put(TranscCombosCS.NAME, new TranscCombosCS())
			//Scout
			.put(FireworkStrikeCS.NAME, new FireworkStrikeCS())
			.put(TwistedCompanionCS.NAME, new TwistedCompanionCS())
			//Warlock
			.put(AvalanchexCS.NAME, new AvalanchexCS())
			.put(InfernalFlamesCS.NAME, new InfernalFlamesCS())
			//Warrior
			.put(BrambleShellCS.NAME, new BrambleShellCS())
			.put(ColossalBruteCS.NAME, new ColossalBruteCS())
			//Deprecated or test only
			//.put(BrambleGuardCS.NAME, new BrambleGuardCS())
			.put(DarkLanceCS.NAME, new DarkLanceCS())
			.put(HallowedLanceCS.NAME, new HallowedLanceCS())
			//.put(BrutalShadowCS.NAME, new BrutalShadowCS())
			.build();

	/* Notes for implementing more Cosmetic Skills
	 * 0. If the ability is not skinned, add a default CS class!
	 * 0.a A default CS should have an ImmutableList to store all skins of this ability.
	 * 0.b A default CS should store all effects to be skinned.
	 * 0.c Add skin checker and mCosmetic field in the ability
	 * 1. Create new class in correct package, extending the base CS.
	 * 2. Override effect methods for new skin.
	 * 3. Override getCosmetic() and getDisplayItem() for correct cosmetic display.
	 * 4. Put a new entry here.
	 */

	public static Cosmetic getCosmeticByName(String name) {
		return COSMETIC_SKILLS.get(name).getCosmetic();
	}

	public static CosmeticSkill getCosmeticSkill(String name) {
		return COSMETIC_SKILLS.get(name);
	}

	public static Material getDisplayItem(String cosmeticSkillName) {
		CosmeticSkill cosmeticSkill = COSMETIC_SKILLS.get(cosmeticSkillName);
		if (cosmeticSkill != null) {
			return cosmeticSkill.getDisplayItem();
		} else {
			return Material.BLAZE_POWDER;
		}
	}

	public static String[] getNames() {
		return COSMETIC_SKILLS.keySet().toArray(new String[0]);
	}

	public static ImmutableList<String> getDepthNames() {
		return ImmutableList.<String>builder()
			.add(SunriseBrewCS.NAME)
			.add(DarkPunishmentCS.NAME)
			.add(VolcanicBurstCS.NAME)
			.add(WindStepCS.NAME)
			.add(FireworkStrikeCS.NAME)
			.add(AvalanchexCS.NAME)
			.add(BrambleShellCS.NAME)
			.build();
	}

	public static ImmutableList<String> getDelveNames() {
		return ImmutableList.<String>builder()
			.add(GruesomeEchoesCS.NAME)
			.add(TouchOfEntropyCS.NAME)
			.add(TwistedLanceCS.NAME)
			.add(TranscCombosCS.NAME)
			.add(TwistedCompanionCS.NAME)
			.add(InfernalFlamesCS.NAME)
			.add(ColossalBruteCS.NAME)
			.build();
	}
}
