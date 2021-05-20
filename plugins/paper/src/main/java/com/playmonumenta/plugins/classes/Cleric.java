package com.playmonumenta.plugins.classes;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.cleric.CelestialBlessing;
import com.playmonumenta.plugins.abilities.cleric.CleansingRain;
import com.playmonumenta.plugins.abilities.cleric.DivineJustice;
import com.playmonumenta.plugins.abilities.cleric.HandOfLight;
import com.playmonumenta.plugins.abilities.cleric.HeavenlyBoon;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.abilities.cleric.SacredProvisions;
import com.playmonumenta.plugins.abilities.cleric.SanctifiedArmor;

import com.playmonumenta.plugins.abilities.cleric.hierophant.EnchantedPrayer;
import com.playmonumenta.plugins.abilities.cleric.hierophant.HallowedBeam;
import com.playmonumenta.plugins.abilities.cleric.hierophant.ThuribleProcession;

import com.playmonumenta.plugins.abilities.cleric.paladin.ChoirBells;
import com.playmonumenta.plugins.abilities.cleric.paladin.HolyJavelin;
import com.playmonumenta.plugins.abilities.cleric.paladin.LuminousInfusion;




public class Cleric extends PlayerClass {

	Cleric(Plugin plugin, Player player) {
		mAbilities.add(new CelestialBlessing(plugin, player));
		mAbilities.add(new CleansingRain(plugin, player));
		mAbilities.add(new DivineJustice(plugin, player));
		mAbilities.add(new HandOfLight(plugin, player));
		mAbilities.add(new HeavenlyBoon(plugin, player));
		mAbilities.add(new Crusade(plugin, player));
		mAbilities.add(new SacredProvisions(plugin, player));
		mAbilities.add(new SanctifiedArmor(plugin, player));
		mClass = 3;
		mClassName = "Cleric";

		mSpecOne.mAbilities.add(new ChoirBells(plugin, player));
		mSpecOne.mAbilities.add(new HolyJavelin(plugin, player));
		mSpecOne.mAbilities.add(new LuminousInfusion(plugin, player));
		mSpecOne.mSpecQuestScoreboard = "Quest103d";
		mSpecOne.mSpecialization = 5;
		mSpecOne.mSpecName = "Paladin";

		mSpecTwo.mAbilities.add(new EnchantedPrayer(plugin, player));
		mSpecTwo.mAbilities.add(new HallowedBeam(plugin, player));
		mSpecTwo.mAbilities.add(new ThuribleProcession(plugin, player));
		mSpecTwo.mSpecQuestScoreboard = "Quest103k";
		mSpecTwo.mSpecialization = 6;
		mSpecTwo.mSpecName = "Hierophant";

	}
}
