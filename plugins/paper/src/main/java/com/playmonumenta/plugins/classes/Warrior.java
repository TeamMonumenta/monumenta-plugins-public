package com.playmonumenta.plugins.classes;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.abilities.warrior.BruteForce;
import com.playmonumenta.plugins.abilities.warrior.CounterStrike;
import com.playmonumenta.plugins.abilities.warrior.DefensiveLine;
import com.playmonumenta.plugins.abilities.warrior.Frenzy;
import com.playmonumenta.plugins.abilities.warrior.Riposte;
import com.playmonumenta.plugins.abilities.warrior.ShieldBash;
import com.playmonumenta.plugins.abilities.warrior.Toughness;
import com.playmonumenta.plugins.abilities.warrior.WeaponMastery;
import com.playmonumenta.plugins.abilities.warrior.berserker.GloriousBattle;
import com.playmonumenta.plugins.abilities.warrior.berserker.MeteorSlam;
import com.playmonumenta.plugins.abilities.warrior.berserker.Rampage;
import com.playmonumenta.plugins.abilities.warrior.guardian.Bodyguard;
import com.playmonumenta.plugins.abilities.warrior.guardian.Challenge;
import com.playmonumenta.plugins.abilities.warrior.guardian.ShieldWall;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;


public class Warrior extends PlayerClass {

	public static final int CLASS_ID = 2;
	public static final int BERSERKER_SPEC_ID = 3;
	public static final int GUARDIAN_SPEC_ID = 4;

	Warrior() {
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
		mClassColor = NamedTextColor.RED;
		mChatColor = ChatColor.RED;
		mDisplayItem = new ItemStack(Material.STONE_AXE, 1);
		mClassDescription = "Warriors specialize in melee combat, being able to both deal and survive heavy damage.";
		mClassPassiveDescription = "Gain a base of 20% Knockback Resistance. This stacks with other skills and items.";
		mClassPassiveName = "Formidable";

		mSpecOne.mAbilities.add(GloriousBattle.INFO);
		mSpecOne.mAbilities.add(MeteorSlam.INFO);
		mSpecOne.mAbilities.add(Rampage.INFO);
		mSpecOne.mSpecQuestScoreboard = "Quest103a";
		mSpecOne.mSpecialization = BERSERKER_SPEC_ID;
		mSpecOne.mSpecName = "Berserker";
		mSpecOne.mDisplayItem = new ItemStack(Material.WITHER_SKELETON_SKULL, 1);
		mSpecOne.mDescription = "Berserkers specialize in melee combat. They thrive in the heart of battle taking heavy risks for great rewards.";

		mSpecTwo.mAbilities.add(Bodyguard.INFO);
		mSpecTwo.mAbilities.add(Challenge.INFO);
		mSpecTwo.mAbilities.add(ShieldWall.INFO);
		mSpecTwo.mSpecQuestScoreboard = "Quest103h";
		mSpecTwo.mSpecialization = GUARDIAN_SPEC_ID;
		mSpecTwo.mSpecName = "Guardian";
		mSpecTwo.mDisplayItem = new ItemStack(Material.SHIELD, 1);
		mSpecTwo.mDescription = "Guardians specialize in melee combat. Their skills are focused on taking heavy damage and drawing enemy attacks to themselves.";

		mTriggerOrder = ImmutableList.of(
			GloriousBattle.INFO,
			MeteorSlam.INFO, // after glorious battle
			Rampage.INFO,

			Bodyguard.INFO,
			Challenge.INFO,
			ShieldWall.INFO
		);
	}
}
