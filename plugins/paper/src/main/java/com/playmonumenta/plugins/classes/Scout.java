package com.playmonumenta.plugins.classes;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.scout.Fleetfooted;
import com.playmonumenta.plugins.abilities.scout.AllOutScout;
import com.playmonumenta.plugins.abilities.scout.HuntingCompanion;
import com.playmonumenta.plugins.abilities.scout.PartingShot;
import com.playmonumenta.plugins.abilities.scout.Sharpshooter;
import com.playmonumenta.plugins.abilities.scout.ShrapnelBomb;
import com.playmonumenta.plugins.abilities.scout.SteelTrap;
import com.playmonumenta.plugins.abilities.scout.Swiftness;
import com.playmonumenta.plugins.abilities.scout.Volley;
import com.playmonumenta.plugins.abilities.scout.WindBomb;
import com.playmonumenta.plugins.abilities.scout.hunter.Deadeye;
import com.playmonumenta.plugins.abilities.scout.hunter.Lockdown;
import com.playmonumenta.plugins.abilities.scout.hunter.PredatorStrike;
import com.playmonumenta.plugins.abilities.scout.hunter.QuiverStorm;
import com.playmonumenta.plugins.abilities.scout.ranger.Elusive;
import com.playmonumenta.plugins.abilities.scout.ranger.GaleShot;
import com.playmonumenta.plugins.abilities.scout.ranger.RendingRazor;
import com.playmonumenta.plugins.abilities.scout.ranger.TacticalManeuver;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.utils.DescriptionUtils.SCOUT_LORE;

public class Scout extends PlayerClass {

	public static final int CLASS_ID = 6;
	public static final int RANGER_SPEC_ID = 11;
	public static final int HUNTER_SPEC_ID = 12;

	public Scout() {
		mAbilities.addAll(List.of(Volley.INFO, HuntingCompanion.INFO, SteelTrap.INFO, WindBomb.INFO, Sharpshooter.INFO,
			ShrapnelBomb.INFO, Swiftness.INFO, PartingShot.INFO));
		mClass = CLASS_ID;
		mClassName = "Scout";
		mClassColor = TextColor.fromHexString("#59B4EB");
		mClassGlassFiller = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
		mDisplayItem = Material.BOW;
		mClassDescription = "Scouts are agile masters of archery and exploration.";
		mPassive = Fleetfooted.INFO;
		mUltimate = AllOutScout.INFO;

		mSpecOne.mAbilities.addAll(List.of(GaleShot.INFO, RendingRazor.INFO, TacticalManeuver.INFO));
		mSpecOne.mSpecQuestScoreboard = "Quest103e";
		mSpecOne.mSpecialization = RANGER_SPEC_ID;
		mSpecOne.mSpecName = "Ranger";
		mSpecOne.mDisplayItem = Material.WHEAT;
		mSpecOne.mDescription = "Rangers are agile experts of exploration that have unparalleled mastery of movement.";
		mSpecOne.mPassive = Elusive.INFO;

		mSpecTwo.mAbilities.addAll(List.of(Lockdown.INFO, QuiverStorm.INFO, PredatorStrike.INFO));
		mSpecTwo.mSpecQuestScoreboard = "Quest103l";
		mSpecTwo.mSpecialization = HUNTER_SPEC_ID;
		mSpecTwo.mSpecName = "Hunter";
		mSpecTwo.mDisplayItem = Material.LEATHER;
		mSpecTwo.mDescription = "Hunters are precise masters of archery that have dedicated themselves to projectile weaponry.";
		mSpecTwo.mPassive = Deadeye.INFO;

		mTriggerOrder = ImmutableList.of(
			AllOutScout.INFO,
			Swiftness.INFO,
			WindBomb.INFO,
			ShrapnelBomb.INFO,
			SteelTrap.INFO,

			Lockdown.INFO,
			GaleShot.INFO,
			PredatorStrike.INFO,

			TacticalManeuver.INFO,
			RendingRazor.INFO, // after wind bomb
			HuntingCompanion.INFO
		);
	}

	@Override
	public Component getDescription(Player player) {
		return new FormattedDescriptionBuilder<>(() -> Fleetfooted.INFO)
			.addDashedLine()
			.addLine("*Scouts are agile masters of archery*").styles(SCOUT_LORE)
			.addLine("*and exploration.*").styles(SCOUT_LORE)
			.addLine()
			.addLine("*Fleetfooted (Class Passive):*").styles(Style.style(mClassColor))
			.add(Fleetfooted.getDescription())
			.addDashedLine()
			.get(AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, Fleetfooted.class), player);
	}

	@Override
	public Component getSpecOneDescription(Player player) {
		return new FormattedDescriptionBuilder<>(() -> Elusive.INFO)
			.addDashedLine()
			.addLine("*Rangers are swift on their feet,*").styles(SCOUT_LORE)
			.addLine("*using swift strikes and ranged tactics*").styles(SCOUT_LORE)
			.addLine("*in order to outspeed any foe.*").styles(SCOUT_LORE)
			.addLine("(DPS, Mobility)")
			.addLine()
			.addLine("*Elusive (Spec. Passive):*").styles(Style.style(mClassColor))
			.add(Elusive.getDescription())
			.addDashedLine()
			.get(AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, Elusive.class), player);
	}

	@Override
	public Component getSpecTwoDescription(Player player) {
		return new FormattedDescriptionBuilder<>(() -> Deadeye.INFO)
			.addDashedLine()
			.addLine("*Hunters prefer to stay at range,*").styles(SCOUT_LORE)
			.addLine("*using precise shots to deal with*").styles(SCOUT_LORE)
			.addLine("*enemies before they get within*").styles(SCOUT_LORE)
			.addLine("*arm's reach.*").styles(SCOUT_LORE)
			.addLine("(Long-Range, Burst Damage)")
			.addLine()
			.addLine("*Deadeye (Spec. Passive):*").styles(Style.style(mClassColor))
			.add(Deadeye.getDescription())
			.addDashedLine()
			.get(AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, Deadeye.class), player);
	}
}
