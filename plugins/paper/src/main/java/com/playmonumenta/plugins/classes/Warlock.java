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
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;


public class Warlock extends PlayerClass {

	public static final int CLASS_ID = 7;
	public static final int REAPER_SPEC_ID = 13;
	public static final int TENEBRIST_SPEC_ID = 14;

	Warlock() {
		mAbilities.add(AmplifyingHex.INFO);
		mAbilities.add(PhlegmaticResolve.INFO);
		mAbilities.add(CholericFlames.INFO);
		mAbilities.add(CursedWound.INFO);
		mAbilities.add(MelancholicLament.INFO);
		mAbilities.add(GraspingClaws.INFO);
		mAbilities.add(SanguineHarvest.INFO);
		mAbilities.add(SoulRend.INFO);
		mClass = CLASS_ID;
		mClassName = "Warlock";
		mClassColor = TextColor.fromHexString("#C724B9");
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
		mSpecOne.mSpecialization = REAPER_SPEC_ID;
		mSpecOne.mSpecName = "Reaper";
		mSpecOne.mDisplayItem = new ItemStack(Material.WITHER_SKELETON_SKULL, 1);
		mSpecOne.mDescription = "Reapers use scythes in combination with dark magic to bring death to their enemies. They specialize in melee combat.";

		mSpecTwo.mAbilities.add(HauntingShades.INFO);
		mSpecTwo.mAbilities.add(RestlessSouls.INFO);
		mSpecTwo.mAbilities.add(WitheringGaze.INFO);
		mSpecTwo.mSpecQuestScoreboard = "Quest103g";
		mSpecTwo.mSpecialization = TENEBRIST_SPEC_ID;
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

}
