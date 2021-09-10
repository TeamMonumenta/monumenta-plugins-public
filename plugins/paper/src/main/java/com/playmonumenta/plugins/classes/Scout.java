package com.playmonumenta.plugins.classes;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.scout.Agility;
import com.playmonumenta.plugins.abilities.scout.HuntingCompanion;
import com.playmonumenta.plugins.abilities.scout.EagleEye;
import com.playmonumenta.plugins.abilities.scout.WindBomb;
import com.playmonumenta.plugins.abilities.scout.Sharpshooter;
import com.playmonumenta.plugins.abilities.scout.SwiftCuts;
import com.playmonumenta.plugins.abilities.scout.Swiftness;
import com.playmonumenta.plugins.abilities.scout.Volley;
import com.playmonumenta.plugins.abilities.scout.hunter.PinningShot;
import com.playmonumenta.plugins.abilities.scout.hunter.SplitArrow;
import com.playmonumenta.plugins.abilities.scout.hunter.PredatorStrike;
import com.playmonumenta.plugins.abilities.scout.ranger.Quickdraw;
import com.playmonumenta.plugins.abilities.scout.ranger.TacticalManeuver;
import com.playmonumenta.plugins.abilities.scout.ranger.WhirlingBlade;


public class Scout extends PlayerClass {

	Scout(Plugin plugin, Player player) {
		mAbilities.add(new Agility(plugin, player));
		mAbilities.add(new HuntingCompanion(plugin, player));
		mAbilities.add(new EagleEye(plugin, player));
		mAbilities.add(new WindBomb(plugin, player));
		mAbilities.add(new Sharpshooter(plugin, player));
		mAbilities.add(new SwiftCuts(plugin, player));
		mAbilities.add(new Swiftness(plugin, player));
		mAbilities.add(new Volley(plugin, player));
		mClass = 6;
		mClassName = "Scout";

		mSpecOne.mAbilities.add(new Quickdraw(plugin, player));
		mSpecOne.mAbilities.add(new WhirlingBlade(plugin, player));
		mSpecOne.mAbilities.add(new TacticalManeuver(plugin, player));
		mSpecOne.mSpecQuestScoreboard = "Quest103e";
		mSpecOne.mSpecialization = 11;
		mSpecOne.mSpecName = "Ranger";

		mSpecTwo.mAbilities.add(new PinningShot(plugin, player));
		mSpecTwo.mAbilities.add(new SplitArrow(plugin, player));
		mSpecTwo.mAbilities.add(new PredatorStrike(plugin, player));
		mSpecTwo.mSpecQuestScoreboard = "Quest103l";
		mSpecTwo.mSpecialization = 12;
		mSpecTwo.mSpecName = "Hunter";

	}
}
