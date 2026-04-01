package com.playmonumenta.plugins.classes;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.alchemist.AlchemicalArtillery;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.abilities.alchemist.Bezoar;
import com.playmonumenta.plugins.abilities.alchemist.BrutalAlchemy;
import com.playmonumenta.plugins.abilities.alchemist.EnergizingElixir;
import com.playmonumenta.plugins.abilities.alchemist.GruesomeAlchemy;
import com.playmonumenta.plugins.abilities.alchemist.IJustMadeSomeBS;
import com.playmonumenta.plugins.abilities.alchemist.IronTincture;
import com.playmonumenta.plugins.abilities.alchemist.UnstableAmalgam;
import com.playmonumenta.plugins.abilities.alchemist.VolatileReaction;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.Panacea;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.TransmutationRing;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.WardingRemedy;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.EsotericEnhancements;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.ScorchedEarth;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.Taboo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.utils.DescriptionUtils.ALCHEMIST_LORE;


public class Alchemist extends PlayerClass {

	public static final int CLASS_ID = 5;
	public static final int HARBINGER_SPEC_ID = 9;
	public static final int APOTHECARY_SPEC_ID = 10;

	public static final Style BRUTAL_COLOR = Style.style(TextColor.color(0xC697E6));
	public static final Style GRUESOME_COLOR = Style.style(TextColor.color(0x94E622));

	public Alchemist() {
		mAbilities.add(GruesomeAlchemy.INFO);
		mAbilities.add(BrutalAlchemy.INFO);
		mAbilities.add(IronTincture.INFO);
		mAbilities.add(AlchemicalArtillery.INFO);
		mAbilities.add(VolatileReaction.INFO);
		mAbilities.add(UnstableAmalgam.INFO);
		mAbilities.add(EnergizingElixir.INFO);
		mAbilities.add(Bezoar.INFO);
		mClass = CLASS_ID;
		mClassName = "Alchemist";
		mClassColor = TextColor.fromHexString("#81D434");
		mClassGlassFiller = Material.LIME_STAINED_GLASS_PANE;
		mDisplayItem = Material.POTION;
		mClassDescription = "Alchemists employ magic potions to weaken and destroy their enemies.";
		mPassive = AlchemistPotions.INFO;
		mUltimate = IJustMadeSomeBS.INFO;

		mSpecOne.mAbilities.add(Taboo.INFO);
		mSpecOne.mAbilities.add(ScorchedEarth.INFO);
		mSpecOne.mAbilities.add(EsotericEnhancements.INFO);
		mSpecOne.mSpecQuestScoreboard = "Quest103m";
		mSpecOne.mSpecialization = HARBINGER_SPEC_ID;
		mSpecOne.mSpecName = "Harbinger";
		mSpecOne.mDisplayItem = Material.DEAD_BUSH;

		mSpecTwo.mAbilities.add(Panacea.INFO);
		mSpecTwo.mAbilities.add(TransmutationRing.INFO);
		mSpecTwo.mAbilities.add(WardingRemedy.INFO);
		mSpecTwo.mSpecQuestScoreboard = "Quest103f";
		mSpecTwo.mSpecialization = APOTHECARY_SPEC_ID;
		mSpecTwo.mSpecName = "Apothecary";
		mSpecTwo.mDisplayItem = Material.BLUE_ORCHID;

		mTriggerOrder = ImmutableList.of(
			IJustMadeSomeBS.INFO,

			Taboo.INFO,
			ScorchedEarth.INFO,
			VolatileReaction.INFO,

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


	@Override
	public Component getDescription(Player player) {
		return new FormattedDescriptionBuilder<>(() -> AlchemistPotions.INFO)
			.addDashedLine()
			.addLine("*Alchemists employ magic potions to*").styles(ALCHEMIST_LORE)
			.addLine("*weaken and destroy their enemies.*").styles(ALCHEMIST_LORE)
			.addLine()
			.addLine("*Alchemist Potions (Class Passive):*").styles(Style.style(mClassColor))
			.add(AlchemistPotions.getDescription())
			.addDashedLine()
			.get(AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class), player);
	}

	@Override
	public Component getSpecOneDescription(Player player) {
		return new FormattedDescriptionBuilder<>()
			.addDashedLine()
			.addLine("*Harbingers administer vile concoctions*").styles(ALCHEMIST_LORE)
			.addLine("*on both enemies and themselves,*").styles(ALCHEMIST_LORE)
			.addLine("*debilitating and destroying their*").styles(ALCHEMIST_LORE)
			.addLine("*foes by any means necessary.*").styles(ALCHEMIST_LORE)
			.addLine("(AoE, High Damage, Health Drain)")
			.addDashedLine()
			.get();
	}

	@Override
	public Component getSpecTwoDescription(Player player) {
		return new FormattedDescriptionBuilder<>()
			.addDashedLine()
			.addLine("*Apothecaries are forefront casters,*").styles(ALCHEMIST_LORE)
			.addLine("*supporting allies and crippling foes*").styles(ALCHEMIST_LORE)
			.addLine("*with their many ailments and cures.*").styles(ALCHEMIST_LORE)
			.addLine("(Support, Absorption)")
			.addDashedLine()
			.get();
	}
}
