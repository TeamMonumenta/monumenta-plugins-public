package com.playmonumenta.plugins.classes;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.mage.ArcaneStrike;
import com.playmonumenta.plugins.abilities.mage.ElementalArrows;
import com.playmonumenta.plugins.abilities.mage.FrostNova;
import com.playmonumenta.plugins.abilities.mage.MagmaShield;
import com.playmonumenta.plugins.abilities.mage.ManaLance;
import com.playmonumenta.plugins.abilities.mage.PrismaticShield;
import com.playmonumenta.plugins.abilities.mage.Spellshock;
import com.playmonumenta.plugins.abilities.mage.ThunderStep;

import com.playmonumenta.plugins.abilities.mage.arcanist.AstralOmen;
import com.playmonumenta.plugins.abilities.mage.arcanist.SpatialShatter;
import com.playmonumenta.plugins.abilities.mage.arcanist.SagesInsight;

import com.playmonumenta.plugins.abilities.mage.elementalist.Blizzard;
import com.playmonumenta.plugins.abilities.mage.elementalist.ElementalSpiritFire;
import com.playmonumenta.plugins.abilities.mage.elementalist.Starfall;




public class Mage extends PlayerClass {

	Mage(Plugin plugin, Player player) {
		mAbilities.add(new ArcaneStrike(plugin, player));
		mAbilities.add(new ElementalArrows(plugin, player));
		mAbilities.add(new FrostNova(plugin, player));
		mAbilities.add(new MagmaShield(plugin, player));
		mAbilities.add(new ManaLance(plugin, player));
		mAbilities.add(new Spellshock(plugin, player));
		mAbilities.add(new ThunderStep(plugin, player));
		mAbilities.add(new PrismaticShield(plugin, player));
		mClass = 1;
		mClassName = "Mage";

		mSpecOne.mAbilities.add(new AstralOmen(plugin, player));
		mSpecOne.mAbilities.add(new SpatialShatter(plugin, player));
		mSpecOne.mAbilities.add(new SagesInsight(plugin, player));
		mSpecOne.mSpecQuestScoreboard = "Quest103i";
		mSpecOne.mSpecialization = 1;
		mSpecOne.mSpecName = "Arcanist";

		mSpecTwo.mAbilities.add(new Blizzard(plugin, player));
		mSpecTwo.mAbilities.add(new ElementalSpiritFire(plugin, player));
		mSpecTwo.mAbilities.add(new Starfall(plugin, player));
		mSpecTwo.mSpecQuestScoreboard = "Quest103b";
		mSpecTwo.mSpecialization = 2;
		mSpecTwo.mSpecName = "Elementalist";
	}
}
