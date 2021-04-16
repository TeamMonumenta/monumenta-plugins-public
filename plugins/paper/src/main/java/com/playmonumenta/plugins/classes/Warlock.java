package com.playmonumenta.plugins.classes;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.warlock.AmplifyingHex;
import com.playmonumenta.plugins.abilities.warlock.PhlegmaticResolve;
import com.playmonumenta.plugins.abilities.warlock.CholericFlames;
import com.playmonumenta.plugins.abilities.warlock.CursedWound;
import com.playmonumenta.plugins.abilities.warlock.MelancholicLament;
import com.playmonumenta.plugins.abilities.warlock.GraspingClaws;
import com.playmonumenta.plugins.abilities.warlock.SanguineHarvest;
import com.playmonumenta.plugins.abilities.warlock.SoulRend;

import com.playmonumenta.plugins.abilities.warlock.reaper.DarkPact;
import com.playmonumenta.plugins.abilities.warlock.reaper.JudgementChain;
import com.playmonumenta.plugins.abilities.warlock.reaper.VoodooBonds;

import com.playmonumenta.plugins.abilities.warlock.tenebrist.HauntingShades;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.UmbralWail;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.WitheringGaze;
import com.playmonumenta.plugins.utils.ScoreboardUtils;


public class Warlock extends PlayerClass {

	Warlock(Plugin plugin, Player player) {
		mAbilities.add(new AmplifyingHex(plugin, player));
		mAbilities.add(new PhlegmaticResolve(plugin, player));
		mAbilities.add(new CholericFlames(plugin, player));
		mAbilities.add(new CursedWound(plugin, player));
		mAbilities.add(new MelancholicLament(plugin, player));
		mAbilities.add(new GraspingClaws(plugin, player));
		mAbilities.add(new SanguineHarvest(plugin, player));
		mAbilities.add(new SoulRend(plugin, player));
		mClass = 7;
		mClassName = "Warlock";

		mSpecOne.mAbilities.add(new HauntingShades(plugin, player));
		mSpecOne.mAbilities.add(new UmbralWail(plugin, player));
		mSpecOne.mAbilities.add(new WitheringGaze(plugin, player));
		mSpecOne.mSpecQuestScoreboard = "Quest103n";
		mSpecOne.mSpecialization = 13;
		mSpecOne.mSpecName = "Tenebrist";

		mSpecTwo.mAbilities.add(new DarkPact(plugin, player));
		mSpecTwo.mAbilities.add(new JudgementChain(plugin, player));
		mSpecTwo.mAbilities.add(new VoodooBonds(plugin, player));
		mSpecTwo.mSpecQuestScoreboard = "Quest103g";
		mSpecTwo.mSpecialization = 14;
		mSpecTwo.mSpecName = "Reaper";

	}

	@Override
	public Boolean getClassAccessPerms(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "Quest13") >= 1;
	}
}
