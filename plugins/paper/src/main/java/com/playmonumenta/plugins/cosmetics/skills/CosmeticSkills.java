package com.playmonumenta.plugins.cosmetics.skills;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.GruesomeEchoesCS;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.SunriseBrewCS;
import com.playmonumenta.plugins.cosmetics.skills.cleric.DarkPunishmentCS;
import com.playmonumenta.plugins.cosmetics.skills.cleric.TouchOfEntropyCS;
import com.playmonumenta.plugins.cosmetics.skills.mage.DarkLanceCS;
import com.playmonumenta.plugins.cosmetics.skills.mage.HallowedLanceCS;
import com.playmonumenta.plugins.cosmetics.skills.mage.TwistedLanceCS;
import com.playmonumenta.plugins.cosmetics.skills.mage.VolcanicBurstCS;
import com.playmonumenta.plugins.cosmetics.skills.rogue.TranscCombosCS;
import com.playmonumenta.plugins.cosmetics.skills.rogue.WindStepCS;
import com.playmonumenta.plugins.cosmetics.skills.scout.TwistedCompanionCS;
import com.playmonumenta.plugins.cosmetics.skills.scout.hunter.FireworkStrikeCS;
import com.playmonumenta.plugins.cosmetics.skills.warlock.AvalanchexCS;
import com.playmonumenta.plugins.cosmetics.skills.warlock.InfernalFlamesCS;
import com.playmonumenta.plugins.cosmetics.skills.warrior.BrambleShellCS;
import com.playmonumenta.plugins.cosmetics.skills.warrior.ColossalBruteCS;
import com.playmonumenta.scriptedquests.internal.com.google.common.collect.ImmutableList;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

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
		CosmeticSkill cs = COSMETIC_SKILLS.getOrDefault(name, null);
		return cs != null ? cs.getCosmetic() : null;
	}

	public static CosmeticSkill getCosmeticSkill(String name) {
		return COSMETIC_SKILLS.get(name);
	}

	public static <T extends CosmeticSkill> T getPlayerCosmeticSkill(@Nullable Player player, T baseCosmetic, ImmutableMap<String, T> skinList) {
		String name = CosmeticsManager.getInstance().getSkillCosmeticName(player, baseCosmetic.getAbilityName());
		return skinList.getOrDefault(name, baseCosmetic);
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
