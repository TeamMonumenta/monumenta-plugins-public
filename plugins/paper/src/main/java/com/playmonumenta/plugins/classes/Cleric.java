package com.playmonumenta.plugins.classes;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.abilities.cleric.CelestialBlessing;
import com.playmonumenta.plugins.abilities.cleric.CleansingRain;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.abilities.cleric.DivineJustice;
import com.playmonumenta.plugins.abilities.cleric.HandOfLight;
import com.playmonumenta.plugins.abilities.cleric.HeavenlyBoon;
import com.playmonumenta.plugins.abilities.cleric.Illuminate;
import com.playmonumenta.plugins.abilities.cleric.SanctifiedArmor;
import com.playmonumenta.plugins.abilities.cleric.TouchofRadiance;
import com.playmonumenta.plugins.abilities.cleric.hierophant.EnchantedPrayer;
import com.playmonumenta.plugins.abilities.cleric.hierophant.HallowedBeam;
import com.playmonumenta.plugins.abilities.cleric.hierophant.Rejuvenation;
import com.playmonumenta.plugins.abilities.cleric.hierophant.ThuribleProcession;
import com.playmonumenta.plugins.abilities.cleric.paladin.ChoirBells;
import com.playmonumenta.plugins.abilities.cleric.paladin.HolyJavelin;
import com.playmonumenta.plugins.abilities.cleric.paladin.LuminousInfusion;
import com.playmonumenta.plugins.abilities.cleric.paladin.Unwavering;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;


public class Cleric extends PlayerClass {

	public static final int CLASS_ID = 3;
	public static final int PALADIN_SPEC_ID = 5;
	public static final int HIEROPHANT_SPEC_ID = 6;

	public Cleric() {
		mAbilities.add(CelestialBlessing.INFO);
		mAbilities.add(CleansingRain.INFO);
		mAbilities.add(DivineJustice.INFO);
		mAbilities.add(HandOfLight.INFO);
		mAbilities.add(HeavenlyBoon.INFO);
		mAbilities.add(TouchofRadiance.INFO);
		mAbilities.add(Illuminate.INFO);
		mAbilities.add(SanctifiedArmor.INFO);
		mClass = CLASS_ID;
		mClassName = "Cleric";
		mClassColor = TextColor.fromHexString("#FFC644");
		mClassGlassFiller = Material.YELLOW_STAINED_GLASS_PANE;
		mDisplayItem = Material.POPPY;
		mClassDescription = "Clerics are mighty healers and specialize in fighting 'Heretics'. A Heretic is defined as a Humanoid or Undead mob.";
		mPassive = Crusade.INFO;

		mSpecOne.mAbilities.add(HolyJavelin.INFO);
		mSpecOne.mAbilities.add(ChoirBells.INFO);
		mSpecOne.mAbilities.add(LuminousInfusion.INFO);
		mSpecOne.mSpecQuestScoreboard = "Quest103d";
		mSpecOne.mSpecialization = PALADIN_SPEC_ID;
		mSpecOne.mSpecName = "Paladin";
		mSpecOne.mDisplayItem = Material.GHAST_TEAR;
		mSpecOne.mDescription = "Paladins are forefront fighters, charging into battle and vanquishing Heretics with heavy attacks in quick succession.";
		mSpecOne.mPassive = Unwavering.INFO;

		mSpecTwo.mAbilities.add(EnchantedPrayer.INFO);
		mSpecTwo.mAbilities.add(ThuribleProcession.INFO);
		mSpecTwo.mAbilities.add(HallowedBeam.INFO);
		mSpecTwo.mSpecQuestScoreboard = "Quest103k";
		mSpecTwo.mSpecialization = HIEROPHANT_SPEC_ID;
		mSpecTwo.mSpecName = "Hierophant";
		mSpecTwo.mDisplayItem = Material.DRAGON_BREATH;
		mSpecTwo.mDescription = "Hierophants specialize in support and have mastered healing. They will make sure the living stay living.";
		mSpecTwo.mPassive = Rejuvenation.INFO;

		mTriggerOrder = ImmutableList.of(
			EnchantedPrayer.INFO,
			HallowedBeam.INFO,

			ChoirBells.INFO,
			HolyJavelin.INFO,
			LuminousInfusion.INFO,

			CelestialBlessing.INFO,
			CleansingRain.INFO,
			DivineJustice.INFO,
			HandOfLight.INFO, // after cleansing rain and luminous infusion
			TouchofRadiance.INFO,
			Illuminate.INFO
		);
	}
}
