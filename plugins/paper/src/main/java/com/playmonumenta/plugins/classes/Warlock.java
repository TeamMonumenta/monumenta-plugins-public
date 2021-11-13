package com.playmonumenta.plugins.classes;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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

import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;


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
		mClassColor = NamedTextColor.DARK_PURPLE;
		mChatColor = ChatColor.DARK_PURPLE;
		mDisplayItem = new ItemStack(Material.STONE_HOE, 1);
		mQuestReq = "Quest13";
		mQuestReqMin = 1;
		mClassDescription = "Warlocks use scythes in combination with dark magic to bring suffering and death to their enemies.";
		mClassPassiveDescription = "Killing an enemy while holding a scythe grants 6 seconds of Resistance 1.";

		mSpecOne.mAbilities.add(new DarkPact(plugin, player));
		mSpecOne.mAbilities.add(new JudgementChain(plugin, player));
		mSpecOne.mAbilities.add(new VoodooBonds(plugin, player));
		mSpecOne.mSpecQuestScoreboard = "Quest103g";
		mSpecOne.mSpecialization = 13;
		mSpecOne.mSpecName = "Reaper";
		mSpecOne.mDisplayItem = new ItemStack(Material.WITHER_SKELETON_SKULL, 1);
		mSpecOne.mDescription = "Reapers use scythes in combination with dark magic to bring death to their enemies. They specialize in melee combat.";

		mSpecTwo.mAbilities.add(new HauntingShades(plugin, player));
		mSpecTwo.mAbilities.add(new UmbralWail(plugin, player));
		mSpecTwo.mAbilities.add(new WitheringGaze(plugin, player));
		mSpecTwo.mSpecQuestScoreboard = "Quest103n";
		mSpecTwo.mSpecialization = 14;
		mSpecTwo.mSpecName = "Tenebrist";
		mSpecTwo.mDisplayItem = new ItemStack(Material.CRIMSON_NYLIUM, 1);
		mSpecTwo.mDescription = "Tenebrists use scythes in combination with dark magic to bring suffering to their enemies. They specialize in curses and status effects.";

	}

	@Override
	public Boolean getClassAccessPerms(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "Quest13") >= 1;
	}
}
