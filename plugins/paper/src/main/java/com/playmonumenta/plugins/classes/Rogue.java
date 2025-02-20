package com.playmonumenta.plugins.classes;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.abilities.rogue.AdvancingShadows;
import com.playmonumenta.plugins.abilities.rogue.ByMyBlade;
import com.playmonumenta.plugins.abilities.rogue.DaggerThrow;
import com.playmonumenta.plugins.abilities.rogue.Dethroner;
import com.playmonumenta.plugins.abilities.rogue.Dodging;
import com.playmonumenta.plugins.abilities.rogue.EscapeDeath;
import com.playmonumenta.plugins.abilities.rogue.Skirmisher;
import com.playmonumenta.plugins.abilities.rogue.Smokescreen;
import com.playmonumenta.plugins.abilities.rogue.ViciousCombos;
import com.playmonumenta.plugins.abilities.rogue.assassin.BodkinBlitz;
import com.playmonumenta.plugins.abilities.rogue.assassin.CloakAndDagger;
import com.playmonumenta.plugins.abilities.rogue.assassin.CoupDeGrace;
import com.playmonumenta.plugins.abilities.rogue.swordsage.BladeDance;
import com.playmonumenta.plugins.abilities.rogue.swordsage.DeadlyRonde;
import com.playmonumenta.plugins.abilities.rogue.swordsage.WindWalk;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

public class Rogue extends PlayerClass {

	public static final int CLASS_ID = 4;
	public static final int SWORDSAGE_SPEC_ID = 7;
	public static final int ASSASSIN_SPEC_ID = 8;

	public Rogue() {
		mAbilities.add(AdvancingShadows.INFO);
		mAbilities.add(ByMyBlade.INFO);
		mAbilities.add(DaggerThrow.INFO);
		mAbilities.add(Dodging.INFO);
		mAbilities.add(EscapeDeath.INFO);
		mAbilities.add(Skirmisher.INFO);
		mAbilities.add(Smokescreen.INFO);
		mAbilities.add(ViciousCombos.INFO);
		mClass = CLASS_ID;
		mClassName = "Rogue";
		mClassColor = TextColor.fromHexString("#36393D");
		mClassGlassFiller = Material.GRAY_STAINED_GLASS_PANE;
		mDisplayItem = Material.STONE_SWORD;
		mClassDescription = "Rogues excel in one-on-one battles, using precise strikes to bring down dangerous elite enemies.";
		mPassive = Dethroner.INFO;

		mSpecOne.mAbilities.add(BladeDance.INFO);
		mSpecOne.mAbilities.add(DeadlyRonde.INFO);
		mSpecOne.mAbilities.add(WindWalk.INFO);
		mSpecOne.mSpecQuestScoreboard = "Quest103c";
		mSpecOne.mSpecialization = SWORDSAGE_SPEC_ID;
		mSpecOne.mSpecName = "Swordsage";
		mSpecOne.mDisplayItem = Material.IRON_HELMET;
		mSpecOne.mDescription = "Swordsages specialize in tackling multiple enemies through dexterous movement.";

		mSpecTwo.mAbilities.add(BodkinBlitz.INFO);
		mSpecTwo.mAbilities.add(CloakAndDagger.INFO);
		mSpecTwo.mAbilities.add(CoupDeGrace.INFO);
		mSpecTwo.mSpecQuestScoreboard = "Quest103j";
		mSpecTwo.mSpecialization = ASSASSIN_SPEC_ID;
		mSpecTwo.mSpecName = "Assassin";
		mSpecTwo.mDisplayItem = Material.WITHER_ROSE;
		mSpecTwo.mDescription = "Assassins excel in precise strikes and deception to devastate their enemies.";

		mTriggerOrder = ImmutableList.of(
			BladeDance.INFO,
			WindWalk.INFO,

			CloakAndDagger.INFO,

			AdvancingShadows.INFO, // after blade dance
			DaggerThrow.INFO, // after cloak & dagger
			Smokescreen.INFO,
			EscapeDeath.INFO,

			BodkinBlitz.INFO // after smoke screen
		);
	}
}
