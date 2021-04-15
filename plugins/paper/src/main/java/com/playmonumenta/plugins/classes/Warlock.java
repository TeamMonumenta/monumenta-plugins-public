package com.playmonumenta.plugins.classes;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.warlock.AmplifyingHex;
import com.playmonumenta.plugins.abilities.warlock.BlasphemousAura;
import com.playmonumenta.plugins.abilities.warlock.ConsumingFlames;
import com.playmonumenta.plugins.abilities.warlock.CursedWound;
import com.playmonumenta.plugins.abilities.warlock.Exorcism;
import com.playmonumenta.plugins.abilities.warlock.GraspingClaws;
import com.playmonumenta.plugins.abilities.warlock.Harvester;
import com.playmonumenta.plugins.abilities.warlock.SoulRend;

import com.playmonumenta.plugins.abilities.warlock.reaper.DarkPact;
import com.playmonumenta.plugins.abilities.warlock.reaper.DeathsTouch;
import com.playmonumenta.plugins.abilities.warlock.reaper.GhoulishTaunt;

import com.playmonumenta.plugins.abilities.warlock.tenebrist.EerieEminence;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.FractalEnervation;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.WitheringGaze;
import com.playmonumenta.plugins.utils.ScoreboardUtils;


public class Warlock extends PlayerClass {

	Warlock(Plugin plugin, Player player) {
		mAbilities.add(new AmplifyingHex(plugin, player));
		mAbilities.add(new BlasphemousAura(plugin, player));
		mAbilities.add(new ConsumingFlames(plugin, player));
		mAbilities.add(new CursedWound(plugin, player));
		mAbilities.add(new Exorcism(plugin, player));
		mAbilities.add(new GraspingClaws(plugin, player));
		mAbilities.add(new Harvester(plugin, player));
		mAbilities.add(new SoulRend(plugin, player));
		mClass = 7;
		mClassName = "Warlock";

		mSpecOne.mAbilities.add(new EerieEminence(plugin, player));
		mSpecOne.mAbilities.add(new FractalEnervation(plugin, player));
		mSpecOne.mAbilities.add(new WitheringGaze(plugin, player));
		mSpecOne.mSpecQuestScoreboard = "Quest103n";
		mSpecOne.mSpecialization = 13;
		mSpecOne.mSpecName = "Tenebrist";

		mSpecTwo.mAbilities.add(new DarkPact(plugin, player));
		mSpecTwo.mAbilities.add(new DeathsTouch(plugin, player));
		mSpecTwo.mAbilities.add(new GhoulishTaunt(plugin, player));
		mSpecTwo.mSpecQuestScoreboard = "Quest103g";
		mSpecTwo.mSpecialization = 14;
		mSpecTwo.mSpecName = "Reaper";

	}

	@Override
	public Boolean getClassAccessPerms(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "Quest13") >= 1;
	}
}
