package com.playmonumenta.plugins.classes;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.abilities.warlock.AmplifyingHex;
import com.playmonumenta.plugins.abilities.warlock.CholericFlames;
import com.playmonumenta.plugins.abilities.warlock.CursedWound;
import com.playmonumenta.plugins.abilities.warlock.GraspingClaws;
import com.playmonumenta.plugins.abilities.warlock.MelancholicLament;
import com.playmonumenta.plugins.abilities.warlock.PhlegmaticResolve;
import com.playmonumenta.plugins.abilities.warlock.SanguineHarvest;
import com.playmonumenta.plugins.abilities.warlock.SoulRend;
import com.playmonumenta.plugins.abilities.warlock.reaper.DarkPact;
import com.playmonumenta.plugins.abilities.warlock.reaper.JudgementChain;
import com.playmonumenta.plugins.abilities.warlock.reaper.VoodooBonds;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.HauntingShades;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.RestlessSouls;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.WitheringGaze;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class Warlock extends PlayerClass {

	Warlock() {
		mAbilities.add(AmplifyingHex.INFO);
		mAbilities.add(PhlegmaticResolve.INFO);
		mAbilities.add(CholericFlames.INFO);
		mAbilities.add(CursedWound.INFO);
		mAbilities.add(MelancholicLament.INFO);
		mAbilities.add(GraspingClaws.INFO);
		mAbilities.add(SanguineHarvest.INFO);
		mAbilities.add(SoulRend.INFO);
		mClass = 7;
		mClassName = "Warlock";
		mClassColor = NamedTextColor.DARK_PURPLE;
		mChatColor = ChatColor.DARK_PURPLE;
		mDisplayItem = new ItemStack(Material.STONE_HOE, 1);
		mQuestReq = "Quest13";
		mQuestReqMin = 1;
		mClassDescription = "Warlocks use scythes in combination with dark magic to bring suffering and death to their enemies.";
		mClassPassiveDescription = "Killing an enemy while holding a scythe grants 6 seconds of Resistance 1.";
		mClassPassiveName = "Culling";

		mSpecOne.mAbilities.add(DarkPact.INFO);
		mSpecOne.mAbilities.add(JudgementChain.INFO);
		mSpecOne.mAbilities.add(VoodooBonds.INFO);
		mSpecOne.mSpecQuestScoreboard = "Quest103n";
		mSpecOne.mSpecialization = 13;
		mSpecOne.mSpecName = "Reaper";
		mSpecOne.mDisplayItem = new ItemStack(Material.WITHER_SKELETON_SKULL, 1);
		mSpecOne.mDescription = "Reapers use scythes in combination with dark magic to bring death to their enemies. They specialize in melee combat.";

		mSpecTwo.mAbilities.add(HauntingShades.INFO);
		mSpecTwo.mAbilities.add(RestlessSouls.INFO);
		mSpecTwo.mAbilities.add(WitheringGaze.INFO);
		mSpecTwo.mSpecQuestScoreboard = "Quest103g";
		mSpecTwo.mSpecialization = 14;
		mSpecTwo.mSpecName = "Tenebrist";
		mSpecTwo.mDisplayItem = new ItemStack(Material.CRIMSON_NYLIUM, 1);
		mSpecTwo.mDescription = "Tenebrists use scythes in combination with dark magic to bring suffering to their enemies. They specialize in curses and status effects.";

		mTriggerOrder = ImmutableList.of(
			DarkPact.INFO,
			JudgementChain.INFO, // after dark pact
			VoodooBonds.INFO,

			HauntingShades.INFO,
			WitheringGaze.INFO,

			AmplifyingHex.INFO,
			CholericFlames.INFO,
			GraspingClaws.INFO,
			MelancholicLament.INFO,
			SanguineHarvest.INFO
		);
	}

	@Override
	public Boolean getClassAccessPerms(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "Quest13").orElse(0) >= 1;
	}
}
