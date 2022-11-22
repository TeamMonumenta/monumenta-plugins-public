package com.playmonumenta.plugins.classes;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.abilities.alchemist.AlchemicalArtillery;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.abilities.alchemist.Bezoar;
import com.playmonumenta.plugins.abilities.alchemist.BrutalAlchemy;
import com.playmonumenta.plugins.abilities.alchemist.EmpoweringOdor;
import com.playmonumenta.plugins.abilities.alchemist.EnergizingElixir;
import com.playmonumenta.plugins.abilities.alchemist.GruesomeAlchemy;
import com.playmonumenta.plugins.abilities.alchemist.IronTincture;
import com.playmonumenta.plugins.abilities.alchemist.UnstableAmalgam;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.Panacea;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.TransmutationRing;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.WardingRemedy;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.EsotericEnhancements;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.ScorchedEarth;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.Taboo;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;


public class Alchemist extends PlayerClass {

	Alchemist() {
		mAbilities.add(GruesomeAlchemy.INFO);
		mAbilities.add(BrutalAlchemy.INFO);
		mAbilities.add(IronTincture.INFO);
		mAbilities.add(AlchemicalArtillery.INFO);
		mAbilities.add(EmpoweringOdor.INFO);
		mAbilities.add(UnstableAmalgam.INFO);
		mAbilities.add(EnergizingElixir.INFO);
		mAbilities.add(Bezoar.INFO);
		mClass = 5;
		mClassName = "Alchemist";
		mClassColor = NamedTextColor.GREEN;
		mChatColor = ChatColor.GREEN;
		mDisplayItem = new ItemStack(Material.POTION, 1);
		mClassDescription = "Alchemists employ magic potions to weaken and destroy their enemies.";
		mClassPassiveDescription = ("You gain 1 potion every %s seconds, up to a maximum of %s. Each skill point" +
			                            " increases your potion damage by %s. Each spec point and enhancement point increases potion damage by %s.")
			                           .formatted(StringUtils.ticksToSeconds(AlchemistPotions.POTIONS_TIMER_BASE), AlchemistPotions.MAX_CHARGES,
				                           AlchemistPotions.DAMAGE_PER_SKILL_POINT, AlchemistPotions.DAMAGE_PER_SPEC_POINT);
		mClassPassiveName = "Alchemist Potions";

		mSpecOne.mAbilities.add(Taboo.INFO);
		mSpecOne.mAbilities.add(ScorchedEarth.INFO);
		mSpecOne.mAbilities.add(EsotericEnhancements.INFO);
		mSpecOne.mSpecQuestScoreboard = "Quest103m";
		mSpecOne.mSpecialization = 9;
		mSpecOne.mSpecName = "Harbinger";
		mSpecOne.mDisplayItem = new ItemStack(Material.DEAD_BUSH, 1);
		mSpecOne.mDescription = "Harbingers use special potions to weaken and destroy their enemies. Harbingers prefer slinging deadly potions and using strategy.";

		mSpecTwo.mAbilities.add(Panacea.INFO);
		mSpecTwo.mAbilities.add(TransmutationRing.INFO);
		mSpecTwo.mAbilities.add(WardingRemedy.INFO);
		mSpecTwo.mSpecQuestScoreboard = "Quest103f";
		mSpecTwo.mSpecialization = 10;
		mSpecTwo.mSpecName = "Apothecary";
		mSpecTwo.mDisplayItem = new ItemStack(Material.BLUE_ORCHID, 1);
		mSpecTwo.mDescription = "Apothecaries employ magic potions to weaken enemies and support friends.";

		mTriggerOrder = ImmutableList.of(
			Taboo.INFO,
			ScorchedEarth.INFO,

			Panacea.INFO,
			TransmutationRing.INFO,
			WardingRemedy.INFO,

			GruesomeAlchemy.INFO, // after Taboo, Warding Remedy, and Panacea
			IronTincture.INFO,
			AlchemicalArtillery.INFO, // after Taboo
			UnstableAmalgam.INFO,
			EnergizingElixir.INFO
		);
	}
}
