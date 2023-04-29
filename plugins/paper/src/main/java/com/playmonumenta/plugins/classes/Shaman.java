package com.playmonumenta.plugins.classes;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.abilities.scout.ranger.TacticalManeuver;
import com.playmonumenta.plugins.abilities.shaman.AdhesiveTotems;
import com.playmonumenta.plugins.abilities.shaman.CleansingTotem;
import com.playmonumenta.plugins.abilities.shaman.EarthenTremor;
import com.playmonumenta.plugins.abilities.shaman.FlameTotem;
import com.playmonumenta.plugins.abilities.shaman.InterconnectedHavoc;
import com.playmonumenta.plugins.abilities.shaman.LightningCrash;
import com.playmonumenta.plugins.abilities.shaman.LightningTotem;
import com.playmonumenta.plugins.abilities.shaman.TotemicProjection;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.DecayedTotem;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.DesecratingShot;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.Devastation;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.EyeOfTheStorm;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.SharedEmpowerment;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.WhirlwindTotem;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;


public class Shaman extends PlayerClass {

	public static final int CLASS_ID = 8;
	public static final int SOOTHSAYER_ID = 15;
	public static final int HEXBREAKER_ID = 16;
	public static final String PERMISSION_STRING = "monumenta.class.shaman";

	public static final double PASSIVE_SPEED = 0.05;
	public static final double PASSIVE_DR = 0.05;
	public static final double SOOTH_PASSIVE_DAMAGE_BOOST = 0.08;
	public static final double SOOTH_PASSIVE_HEAL_PERCENT = 0.02;
	public static final double HEX_PASSIVE_DAMAGE_BOOST = 0.15;

	Shaman() {
		mAbilities.add(CleansingTotem.INFO);
		mAbilities.add(TotemicProjection.INFO);
		mAbilities.add(FlameTotem.INFO);
		mAbilities.add(InterconnectedHavoc.INFO);
		mAbilities.add(LightningTotem.INFO);
		mAbilities.add(EarthenTremor.INFO);
		mAbilities.add(LightningCrash.INFO);
		mAbilities.add(AdhesiveTotems.INFO);
		mClass = 8;
		mClassName = "Shaman";
		mPermissionString = PERMISSION_STRING;
		mDisplayItem = Material.TOTEM_OF_UNDYING;
		mClassDescription = "Shamans excel in strategic positioning and location defense, using their stationary totems to control the battle.";
		mClassPassiveDescription = String.format("Gain %s%% speed and take %s%% less damage while standing within 8 blocks of your totems.",
			PASSIVE_SPEED * 100, PASSIVE_DR * 100);
		mClassPassiveName = "Totemic Empowerment";

		mSpecOne.mAbilities.add(SharedEmpowerment.INFO);
		mSpecOne.mAbilities.add(WhirlwindTotem.INFO);
		mSpecOne.mAbilities.add(EyeOfTheStorm.INFO);
		mSpecOne.mSpecQuestScoreboard = "Quest103o";
		mSpecOne.mSpecialization = 15;
		mClassColor = TextColor.fromHexString("#009900");
		mSpecOne.mSpecName = "Soothsayer";
		mSpecOne.mDisplayItem = Material.OAK_SAPLING;
		mSpecOne.mDescription = "Focuses on using your tools to support your team and turn the tide of battle.";
		mSpecOne.mPassiveName = "Support Expertise";
		mSpecOne.mPassiveDescription = String.format("Boosts the max health healed done by your Cleansing Totem by %s%% and magic damage done by your class skills by %s%%",
			SOOTH_PASSIVE_HEAL_PERCENT * 100, SOOTH_PASSIVE_DAMAGE_BOOST * 100);

		mSpecTwo.mAbilities.add(DesecratingShot.INFO);
		mSpecTwo.mAbilities.add(DecayedTotem.INFO);
		mSpecTwo.mAbilities.add(Devastation.INFO);
		mSpecTwo.mSpecQuestScoreboard = "Quest103p";
		mSpecTwo.mSpecialization = 16;
		mSpecTwo.mSpecName = "Hexbreaker";
		mSpecTwo.mDisplayItem = Material.MAGMA_BLOCK;
		mSpecTwo.mDescription = "Deals in dark magics, focusing on harming enemies at the cost of totems.";
		mSpecTwo.mPassiveName = "Destructive Expertise";
		mSpecTwo.mPassiveDescription = String.format("Increases the magic damage done by your class skills by %s%%", HEX_PASSIVE_DAMAGE_BOOST * 100);

		mTriggerOrder = ImmutableList.of(
			InterconnectedHavoc.INFO,
			AdhesiveTotems.INFO,
			CleansingTotem.INFO,
			TotemicProjection.INFO,

			LightningTotem.INFO,
			WhirlwindTotem.INFO,
			EyeOfTheStorm.INFO,

			DecayedTotem.INFO,
			Devastation.INFO,
			DesecratingShot.INFO,

			FlameTotem.INFO,
			TacticalManeuver.INFO,
			LightningCrash.INFO
		);

	}
}
