package com.playmonumenta.plugins.classes;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.abilities.scout.ranger.TacticalManeuver;
import com.playmonumenta.plugins.abilities.shaman.*;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.DecayedTotem;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.DesecratingShot;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.DestructiveExpertise;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.Devastation;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.ChainHealingWave;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.Sanctuary;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.SupportExpertise;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.WhirlwindTotem;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;


public class Shaman extends PlayerClass {

	public static final int CLASS_ID = 8;
	public static final int SOOTHSAYER_ID = 15;
	public static final int HEXBREAKER_ID = 16;
	public static final String PERMISSION_STRING = "monumenta.class.shaman";

	public Shaman() {
		mAbilities.add(CleansingTotem.INFO);
		mAbilities.add(TotemicProjection.INFO);
		mAbilities.add(FlameTotem.INFO);
		mAbilities.add(InterconnectedHavoc.INFO);
		mAbilities.add(LightningTotem.INFO);
		mAbilities.add(ChainLightning.INFO);
		mAbilities.add(EarthenTremor.INFO);
		mAbilities.add(AdhesiveTotems.INFO);
		mClass = CLASS_ID;
		mClassName = "Shaman";
		mPermissionString = PERMISSION_STRING;
		mDisplayItem = Material.TOTEM_OF_UNDYING;
		mClassDescription = "Shamans excel in strategic positioning and location defense, using their stationary totems to control the battle.";
		mClassPassiveDescription = String.format("Gain %s%% speed and take %s%% less damage while standing within %s blocks of your totems.",
			StringUtils.multiplierToPercentage(TotemicEmpowerment.SPEED), StringUtils.multiplierToPercentage(TotemicEmpowerment.RESISTANCE), TotemicEmpowerment.RADIUS);
		mClassPassiveName = "Totemic Empowerment";

		mSpecOne.mAbilities.add(Sanctuary.INFO);
		mSpecOne.mAbilities.add(WhirlwindTotem.INFO);
		mSpecOne.mAbilities.add(ChainHealingWave.INFO);
		mSpecOne.mSpecQuestScoreboard = "Quest103o";
		mSpecOne.mSpecialization = SOOTHSAYER_ID;
		mClassColor = TextColor.fromHexString("#009900");
		mSpecOne.mSpecName = "Soothsayer";
		mSpecOne.mDisplayItem = Material.OAK_SAPLING;
		mSpecOne.mDescription = "Focuses on using your tools to support your team and turn the tide of battle.";
		mSpecOne.mPassiveName = "Support Expertise";
		mSpecOne.mPassiveDescription = String.format("Boosts the magic damage done by your class skills by %s%% per specialization point and the power of Totemic Empowerment's buffs by %s%%, as well as providing the base values to players within %s blocks.",
			StringUtils.multiplierToPercentage(SupportExpertise.DAMAGE_BOOST), StringUtils.multiplierToPercentage(SupportExpertise.SELF_BOOST), SupportExpertise.RADIUS);

		mSpecTwo.mAbilities.add(DesecratingShot.INFO);
		mSpecTwo.mAbilities.add(DecayedTotem.INFO);
		mSpecTwo.mAbilities.add(Devastation.INFO);
		mSpecTwo.mSpecQuestScoreboard = "Quest103p";
		mSpecTwo.mSpecialization = HEXBREAKER_ID;
		mSpecTwo.mSpecName = "Hexbreaker";
		mSpecTwo.mDisplayItem = Material.MAGMA_BLOCK;
		mSpecTwo.mDescription = "Deals in dark magics, focusing on harming enemies at the cost of totems.";
		mSpecTwo.mPassiveName = "Destructive Expertise";
		mSpecTwo.mPassiveDescription = String.format("Increases the magic damage done by your class skills by %s%% per specialization point.", StringUtils.multiplierToPercentage(DestructiveExpertise.DAMAGE_BOOST));

		mTriggerOrder = ImmutableList.of(
			InterconnectedHavoc.INFO,
			AdhesiveTotems.INFO,
			CleansingTotem.INFO,
			TotemicProjection.INFO,

			LightningTotem.INFO,
			WhirlwindTotem.INFO,
			ChainHealingWave.INFO,
			ChainLightning.INFO,

			DecayedTotem.INFO,
			Devastation.INFO,
			DesecratingShot.INFO,

			FlameTotem.INFO,
			TacticalManeuver.INFO,
			EarthenTremor.INFO
		);

	}
}
