package com.playmonumenta.plugins.classes;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.scout.Agility;
import com.playmonumenta.plugins.abilities.scout.BowMastery;
import com.playmonumenta.plugins.abilities.scout.EagleEye;
import com.playmonumenta.plugins.abilities.scout.FinishingBlow;
import com.playmonumenta.plugins.abilities.scout.Sharpshooter;
import com.playmonumenta.plugins.abilities.scout.SwiftCuts;
import com.playmonumenta.plugins.abilities.scout.Swiftness;
import com.playmonumenta.plugins.abilities.scout.Volley;
import com.playmonumenta.plugins.abilities.scout.hunter.EnchantedShot;
import com.playmonumenta.plugins.abilities.scout.hunter.PinningShot;
import com.playmonumenta.plugins.abilities.scout.hunter.SplitArrow;
import com.playmonumenta.plugins.abilities.scout.ranger.Quickdraw;
import com.playmonumenta.plugins.abilities.scout.ranger.Reflexes;
import com.playmonumenta.plugins.abilities.scout.ranger.TacticalManeuver;


public class Scout extends PlayerClass {

	Scout(Plugin plugin, Player player) {
		mAbilities.add(new Agility(plugin, player));
		mAbilities.add(new BowMastery(plugin, player));
		mAbilities.add(new EagleEye(plugin, player));
		mAbilities.add(new FinishingBlow(plugin, player));
		mAbilities.add(new Sharpshooter(plugin, player));
		mAbilities.add(new SwiftCuts(plugin, player));
		mAbilities.add(new Swiftness(plugin, player));
		mAbilities.add(new Volley(plugin, player));
		mClass = 6;
		mClassName = "Scout";

		mSpecOne.mAbilities.add(new Quickdraw(plugin, player));
		mSpecOne.mAbilities.add(new Reflexes(plugin, player));
		mSpecOne.mAbilities.add(new TacticalManeuver(plugin, player));
		mSpecOne.mSpecQuestScoreboard = "Quest103e";
		mSpecOne.mSpecialization = 11;
		mSpecOne.mSpecName = "Ranger";

		mSpecTwo.mAbilities.add(new EnchantedShot(plugin, player));
		mSpecTwo.mAbilities.add(new PinningShot(plugin, player));
		mSpecTwo.mAbilities.add(new SplitArrow(plugin, player));
		mSpecTwo.mSpecQuestScoreboard = "Quest103l";
		mSpecTwo.mSpecialization = 12;
		mSpecTwo.mSpecName = "Hunter";

	}
}
