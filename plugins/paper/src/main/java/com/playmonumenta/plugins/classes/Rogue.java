package com.playmonumenta.plugins.classes;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.rogue.AdvancingShadows;
import com.playmonumenta.plugins.abilities.rogue.ByMyBlade;
import com.playmonumenta.plugins.abilities.rogue.DaggerThrow;
import com.playmonumenta.plugins.abilities.rogue.Dethroner;
import com.playmonumenta.plugins.abilities.rogue.Dodging;
import com.playmonumenta.plugins.abilities.rogue.EscapeDeath;
import com.playmonumenta.plugins.abilities.rogue.Skirmisher;
import com.playmonumenta.plugins.abilities.rogue.Smokescreen;
import com.playmonumenta.plugins.abilities.rogue.ViciousCombos;
import com.playmonumenta.plugins.abilities.rogue.assassin.BodkinBlitz;
import com.playmonumenta.plugins.abilities.rogue.assassin.CloakAndDagger;
import com.playmonumenta.plugins.abilities.rogue.assassin.CoupDeGrace;
import com.playmonumenta.plugins.abilities.rogue.swordsage.BladeDance;
import com.playmonumenta.plugins.abilities.rogue.swordsage.DeadlyRonde;
import com.playmonumenta.plugins.abilities.rogue.swordsage.WindWalk;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.utils.DescriptionUtils.ROGUE_LORE;

public class Rogue extends PlayerClass {

	public static final int CLASS_ID = 4;
	public static final int SWORDSAGE_SPEC_ID = 7;
	public static final int ASSASSIN_SPEC_ID = 8;

	public static final Style STEALTH_COLOR = Style.style(TextColor.color(0x68388C));

	public Rogue() {
		mAbilities.add(AdvancingShadows.INFO);
		mAbilities.add(ByMyBlade.INFO);
		mAbilities.add(DaggerThrow.INFO);
		mAbilities.add(Dodging.INFO);
		mAbilities.add(EscapeDeath.INFO);
		mAbilities.add(Skirmisher.INFO);
		mAbilities.add(Smokescreen.INFO);
		mAbilities.add(ViciousCombos.INFO);
		mClass = CLASS_ID;
		mClassName = "Rogue";
		mClassColor = TextColor.fromHexString("#36393D");
		mClassGlassFiller = Material.GRAY_STAINED_GLASS_PANE;
		mDisplayItem = Material.STONE_SWORD;
		mClassDescription = "Rogues excel in one-on-one battles, using precise strikes to bring down dangerous elite enemies.";
		mPassive = Dethroner.INFO;

		mSpecOne.mAbilities.add(BladeDance.INFO);
		mSpecOne.mAbilities.add(DeadlyRonde.INFO);
		mSpecOne.mAbilities.add(WindWalk.INFO);
		mSpecOne.mSpecQuestScoreboard = "Quest103c";
		mSpecOne.mSpecialization = SWORDSAGE_SPEC_ID;
		mSpecOne.mSpecName = "Swordsage";
		mSpecOne.mDisplayItem = Material.IRON_HELMET;
		mSpecOne.mDescription = "Swordsages specialize in tackling multiple enemies through dexterous movement.";

		mSpecTwo.mAbilities.add(BodkinBlitz.INFO);
		mSpecTwo.mAbilities.add(CloakAndDagger.INFO);
		mSpecTwo.mAbilities.add(CoupDeGrace.INFO);
		mSpecTwo.mSpecQuestScoreboard = "Quest103j";
		mSpecTwo.mSpecialization = ASSASSIN_SPEC_ID;
		mSpecTwo.mSpecName = "Assassin";
		mSpecTwo.mDisplayItem = Material.WITHER_ROSE;
		mSpecTwo.mDescription = "Assassins excel in precise strikes and deception to devastate their enemies.";

		mTriggerOrder = ImmutableList.of(
			BladeDance.INFO,
			WindWalk.INFO,

			CloakAndDagger.INFO,

			AdvancingShadows.INFO, // after blade dance
			DaggerThrow.INFO, // after cloak & dagger
			Smokescreen.INFO,
			EscapeDeath.INFO,

			BodkinBlitz.INFO // after smoke screen
		);
	}

	@Override
	public Component getDescription(Player player) {
		return new FormattedDescriptionBuilder<>(() -> Dethroner.INFO)
			.addDashedLine()
			.addLine("*Rogues excel in one-on-one battles*").styles(ROGUE_LORE)
			.addLine("*and can easily bring down Elites.*").styles(ROGUE_LORE)
			.addLine()
			.addLine("*Dethroner (Class Passive):*").styles(Style.style(mClassColor))
			.add(Dethroner.getDescription())
			.addDashedLine()
			.get(AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, Dethroner.class), player);
	}

	@Override
	public Component getSpecOneDescription(Player player) {
		return new FormattedDescriptionBuilder<>()
			.addDashedLine()
			.addLine("*Swordsages dance gracefully in the*").styles(ROGUE_LORE)
			.addLine("*heart of battle, using dextrous movement*").styles(ROGUE_LORE)
			.addLine("*and slashing strikes to duel large*").styles(ROGUE_LORE)
			.addLine("*crowds of enemies at once.*").styles(ROGUE_LORE)
			.addLine("(Area Damage, Crowd Control, Mobility)")
			.addDashedLine()
			.get();
	}

	@Override
	public Component getSpecTwoDescription(Player player) {
		return new FormattedDescriptionBuilder<>()
			.addDashedLine()
			.addLine("*Assassins stay hidden in the shadows,*").styles(ROGUE_LORE)
			.addLine("*evading enemies and dealing fatal burst*").styles(ROGUE_LORE)
			.addLine("*damage with their precise strikes.*").styles(ROGUE_LORE)
			.addLine("(Burst Damage, Stealth)")
			.addDashedLine()
			.get();
	}
}
