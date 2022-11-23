package com.playmonumenta.plugins.classes;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.abilities.scout.Agility;
import com.playmonumenta.plugins.abilities.scout.EagleEye;
import com.playmonumenta.plugins.abilities.scout.HuntingCompanion;
import com.playmonumenta.plugins.abilities.scout.Sharpshooter;
import com.playmonumenta.plugins.abilities.scout.SwiftCuts;
import com.playmonumenta.plugins.abilities.scout.Swiftness;
import com.playmonumenta.plugins.abilities.scout.Versatile;
import com.playmonumenta.plugins.abilities.scout.Volley;
import com.playmonumenta.plugins.abilities.scout.WindBomb;
import com.playmonumenta.plugins.abilities.scout.hunter.PinningShot;
import com.playmonumenta.plugins.abilities.scout.hunter.PredatorStrike;
import com.playmonumenta.plugins.abilities.scout.hunter.SplitArrow;
import com.playmonumenta.plugins.abilities.scout.ranger.Quickdraw;
import com.playmonumenta.plugins.abilities.scout.ranger.TacticalManeuver;
import com.playmonumenta.plugins.abilities.scout.ranger.WhirlingBlade;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;


public class Scout extends PlayerClass {

	Scout() {
		mAbilities.add(Agility.INFO);
		mAbilities.add(HuntingCompanion.INFO);
		mAbilities.add(EagleEye.INFO);
		mAbilities.add(WindBomb.INFO);
		mAbilities.add(Sharpshooter.INFO);
		mAbilities.add(SwiftCuts.INFO);
		mAbilities.add(Swiftness.INFO);
		mAbilities.add(Volley.INFO);
		mClass = 6;
		mClassName = "Scout";
		mClassColor = NamedTextColor.AQUA;
		mChatColor = ChatColor.AQUA;
		mDisplayItem = new ItemStack(Material.BOW, 1);
		mClassDescription = "Scouts are agile masters of archery and exploration.";
		mClassPassiveDescription = String.format("You gain %d%% of your Bow Damage %% as Attack Damage and you gain %d%% of your Attack Damage %% as Bow Damage.", (int) (Versatile.DAMAGE_MULTIPLY_MELEE * 100), (int) (Versatile.DAMAGE_MULTIPLY_PROJ * 100));
		mClassPassiveName = "Versatile";

		mSpecOne.mAbilities.add(Quickdraw.INFO);
		mSpecOne.mAbilities.add(WhirlingBlade.INFO);
		mSpecOne.mAbilities.add(TacticalManeuver.INFO);
		mSpecOne.mSpecQuestScoreboard = "Quest103e";
		mSpecOne.mSpecialization = 11;
		mSpecOne.mSpecName = "Ranger";
		mSpecOne.mDisplayItem = new ItemStack(Material.LARGE_FERN, 1);
		mSpecOne.mDescription = "Rangers are agile masters of archery and that have unparalleled mastery of movement.";

		mSpecTwo.mAbilities.add(PinningShot.INFO);
		mSpecTwo.mAbilities.add(SplitArrow.INFO);
		mSpecTwo.mAbilities.add(PredatorStrike.INFO);
		mSpecTwo.mSpecQuestScoreboard = "Quest103l";
		mSpecTwo.mSpecialization = 12;
		mSpecTwo.mSpecName = "Hunter";
		mSpecTwo.mDisplayItem = new ItemStack(Material.LEATHER, 1);
		mSpecTwo.mDescription = "Hunters are agile masters of exploration that have dedicated their lives to archery.";

		mTriggerOrder = ImmutableList.of(
			EagleEye.INFO,
			Swiftness.INFO,
			WindBomb.INFO,
			HuntingCompanion.INFO, // after wind bomb

			PredatorStrike.INFO,

			Quickdraw.INFO, // after eagle eye
			TacticalManeuver.INFO,
			WhirlingBlade.INFO // after wind bomb
		);
	}
}
