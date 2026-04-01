package com.playmonumenta.plugins.classes;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.warrior.BruteForce;
import com.playmonumenta.plugins.abilities.warrior.CounterStrike;
import com.playmonumenta.plugins.abilities.warrior.DefensiveLine;
import com.playmonumenta.plugins.abilities.warrior.Formidable;
import com.playmonumenta.plugins.abilities.warrior.Frenzy;
import com.playmonumenta.plugins.abilities.warrior.RecklessSwing;
import com.playmonumenta.plugins.abilities.warrior.Riposte;
import com.playmonumenta.plugins.abilities.warrior.ShieldBash;
import com.playmonumenta.plugins.abilities.warrior.Toughness;
import com.playmonumenta.plugins.abilities.warrior.WeaponMastery;
import com.playmonumenta.plugins.abilities.warrior.berserker.Bloodlust;
import com.playmonumenta.plugins.abilities.warrior.berserker.GloriousBattle;
import com.playmonumenta.plugins.abilities.warrior.berserker.MeteorSlam;
import com.playmonumenta.plugins.abilities.warrior.berserker.Rampage;
import com.playmonumenta.plugins.abilities.warrior.guardian.Bodyguard;
import com.playmonumenta.plugins.abilities.warrior.guardian.Challenge;
import com.playmonumenta.plugins.abilities.warrior.guardian.ShieldWall;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.utils.DescriptionUtils.WARRIOR_LORE;


public class Warrior extends PlayerClass {

	public static final int CLASS_ID = 2;
	public static final int BERSERKER_SPEC_ID = 3;
	public static final int GUARDIAN_SPEC_ID = 4;

	public Warrior() {
		mAbilities.add(BruteForce.INFO);
		mAbilities.add(CounterStrike.INFO);
		mAbilities.add(DefensiveLine.INFO);
		mAbilities.add(Frenzy.INFO);
		mAbilities.add(Riposte.INFO);
		mAbilities.add(ShieldBash.INFO);
		mAbilities.add(Toughness.INFO);
		mAbilities.add(WeaponMastery.INFO);
		mClass = CLASS_ID;
		mClassName = "Warrior";
		mClassColor = TextColor.fromHexString("#D32818");
		mClassGlassFiller = Material.RED_STAINED_GLASS_PANE;
		mDisplayItem = Material.STONE_AXE;
		mClassDescription = "Warriors specialize in melee combat, being able to both deal and survive heavy damage.";
		mPassive = Formidable.INFO;
		mUltimate = RecklessSwing.INFO;

		mSpecOne.mAbilities.add(GloriousBattle.INFO);
		mSpecOne.mAbilities.add(MeteorSlam.INFO);
		mSpecOne.mAbilities.add(Rampage.INFO);
		mSpecOne.mSpecQuestScoreboard = "Quest103a";
		mSpecOne.mSpecialization = BERSERKER_SPEC_ID;
		mSpecOne.mSpecName = "Berserker";
		mSpecOne.mDisplayItem = Material.WITHER_SKELETON_SKULL;
		mSpecOne.mDescription = "Berserkers specialize in melee combat. They thrive in the heart of battle taking heavy risks for great rewards.";
		mSpecOne.mPassive = Bloodlust.INFO;

		mSpecTwo.mAbilities.add(Bodyguard.INFO);
		mSpecTwo.mAbilities.add(Challenge.INFO);
		mSpecTwo.mAbilities.add(ShieldWall.INFO);
		mSpecTwo.mSpecQuestScoreboard = "Quest103h";
		mSpecTwo.mSpecialization = GUARDIAN_SPEC_ID;
		mSpecTwo.mSpecName = "Guardian";
		mSpecTwo.mDisplayItem = Material.SHIELD;
		mSpecTwo.mDescription = "Guardians specialize in melee combat. Their skills are focused on taking heavy damage and drawing enemy attacks to themselves.";

		mTriggerOrder = ImmutableList.of(
			RecklessSwing.INFO,

			GloriousBattle.INFO,
			MeteorSlam.INFO,
			Rampage.INFO,
			Bloodlust.INFO,

			Bodyguard.INFO,
			Challenge.INFO,
			ShieldWall.INFO
		);
	}

	@Override
	public Component getDescription(Player player) {
		return new FormattedDescriptionBuilder<>(() -> Formidable.INFO)
			.addDashedLine()
			.addLine("*Warriors favor melee combat and*").styles(WARRIOR_LORE)
			.addLine("*can deal and tank heavy damage.*").styles(WARRIOR_LORE)
			.addLine()
			.addLine("*Formidable (Class Passive):*").styles(Style.style(mClassColor))
			.add(Formidable.getDescription())
			.addDashedLine()
			.get(AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, Formidable.class), player);
	}

	@Override
	public Component getSpecOneDescription(Player player) {
		return new FormattedDescriptionBuilder<>(() -> Bloodlust.INFO)
			.addDashedLine()
			.addLine("*Berserkers are relentless fighters, capable*").styles(WARRIOR_LORE)
			.addLine("*of closing the distance and getting up close*").styles(WARRIOR_LORE)
			.addLine("*and personal with overwhelming attacks.*").styles(WARRIOR_LORE)
			.addLine("(Bruiser, Mobility, Self Healing)")
			.addLine()
			.addLine("*Bloodlust (Spec. Passive):*").styles(Style.style(mClassColor))
			.add(Bloodlust.getDescription())
			.addDashedLine()
			.get(AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, Bloodlust.class), player);
	}

	@Override
	public Component getSpecTwoDescription(Player player) {
		return new FormattedDescriptionBuilder<>()
			.addDashedLine()
			.addLine("*Guardians are known for their resilience,*").styles(WARRIOR_LORE)
			.addLine("*acting as a shield for allies and a*").styles(WARRIOR_LORE)
			.addLine("*fortress that will withstand any foe.*").styles(WARRIOR_LORE)
			.addLine("(Tank, Absorption)")
			.addDashedLine()
			.get();
	}
}
