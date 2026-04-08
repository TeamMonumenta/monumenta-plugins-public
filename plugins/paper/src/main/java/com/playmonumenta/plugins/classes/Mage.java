package com.playmonumenta.plugins.classes;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.mage.ArcaneStrike;
import com.playmonumenta.plugins.abilities.mage.Channeling;
import com.playmonumenta.plugins.abilities.mage.ElementalArrows;
import com.playmonumenta.plugins.abilities.mage.FrostNova;
import com.playmonumenta.plugins.abilities.mage.MagmaShield;
import com.playmonumenta.plugins.abilities.mage.ManaLance;
import com.playmonumenta.plugins.abilities.mage.PrismaticShield;
import com.playmonumenta.plugins.abilities.mage.Spellshock;
import com.playmonumenta.plugins.abilities.mage.ThunderStep;
import com.playmonumenta.plugins.abilities.mage.arcanist.AstralOmen;
import com.playmonumenta.plugins.abilities.mage.arcanist.CosmicMoonblade;
import com.playmonumenta.plugins.abilities.mage.arcanist.SagesInsight;
import com.playmonumenta.plugins.abilities.mage.elementalist.Blizzard;
import com.playmonumenta.plugins.abilities.mage.elementalist.ElementalSpiritFire;
import com.playmonumenta.plugins.abilities.mage.elementalist.Starfall;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.utils.DescriptionUtils.MAGE_LORE;

public class Mage extends PlayerClass {

	public static final int CLASS_ID = 1;
	public static final int ARCANIST_SPEC_ID = 1;
	public static final int ELEMENTALIST_SPEC_ID = 2;

	public static final Style ARCANE_COLOR = Style.style(TextColor.color(0xB548E8));
	public static final Style FIRE_COLOR = Style.style(TextColor.color(0xE26928));
	public static final Style ICE_COLOR = Style.style(TextColor.color(0x7CEBFF));
	public static final Style THUNDER_COLOR = Style.style(TextColor.color(0xF0D330));


	public Mage() {
		mAbilities.add(ArcaneStrike.INFO);
		mAbilities.add(ElementalArrows.INFO);
		mAbilities.add(FrostNova.INFO);
		mAbilities.add(MagmaShield.INFO);
		mAbilities.add(ManaLance.INFO);
		mAbilities.add(Spellshock.INFO);
		mAbilities.add(ThunderStep.INFO);
		mAbilities.add(PrismaticShield.INFO);
		mClass = CLASS_ID;
		mClassName = "Mage";
		mClassColor = TextColor.fromHexString("#A129D3");
		mClassGlassFiller = Material.PURPLE_STAINED_GLASS_PANE;
		mDisplayItem = Material.BLAZE_ROD;
		mClassDescription = "Mages are masters of area control, freezing, wounding, and igniting enemies with their strikes.";
		mPassive = Channeling.INFO;

		mSpecOne.mAbilities.add(AstralOmen.INFO);
		mSpecOne.mAbilities.add(CosmicMoonblade.INFO);
		mSpecOne.mAbilities.add(SagesInsight.INFO);
		mSpecOne.mSpecQuestScoreboard = "Quest103i";
		mSpecOne.mSpecialization = ARCANIST_SPEC_ID;
		mSpecOne.mSpecName = "Arcanist";
		mSpecOne.mDisplayItem = Material.DRAGON_BREATH;
		mSpecOne.mDescription = "Arcanists are mages that specialize at controlling their skill cooldowns and getting up close.";

		mSpecTwo.mAbilities.add(Blizzard.INFO);
		mSpecTwo.mAbilities.add(ElementalSpiritFire.INFO);
		mSpecTwo.mAbilities.add(Starfall.INFO);
		mSpecTwo.mSpecQuestScoreboard = "Quest103b";
		mSpecTwo.mSpecialization = ELEMENTALIST_SPEC_ID;
		mSpecTwo.mSpecName = "Elementalist";
		mSpecTwo.mDisplayItem = Material.BLAZE_POWDER;
		mSpecTwo.mDescription = "Elementalists are the undisputed masters of the elements. They excel at zoning and crowd control.";

		mTriggerOrder = ImmutableList.of(
			CosmicMoonblade.INFO,

			Blizzard.INFO,
			Starfall.INFO,

			FrostNova.INFO,
			MagmaShield.INFO, // after blizzard
			ManaLance.INFO,
			ThunderStep.INFO
		);
	}

	@Override
	public Component getDescription(Player player) {
		return new FormattedDescriptionBuilder<>(() -> Channeling.INFO)
			.addDashedLine()
			.addLine("*Mages are masters of area damage*").styles(MAGE_LORE)
			.addLine("*and use wands to assail their foes.*").styles(MAGE_LORE)
			.addLine()
			.addLine("*Channeling (Class Passive):*").styles(Style.style(mClassColor))
			.add(Channeling.getDescription())
			.addDashedLine()
			.get(AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, Channeling.class), player);
	}

	@Override
	public Component getSpecOneDescription(Player player) {
		return new FormattedDescriptionBuilder<>()
			.addDashedLine()
			.addLine("*Arcanists utilize arcane magic to*").styles(MAGE_LORE)
			.addLine("*accelerate their spell combos, dealing*").styles(MAGE_LORE)
			.addLine("*devastating area damage to anything*").styles(MAGE_LORE)
			.addLine("*that gets in their way.*").styles(MAGE_LORE)
			.addLine("(Burst Damage, Cooldown Reduction)")
			.addDashedLine()
			.get();
	}

	@Override
	public Component getSpecTwoDescription(Player player) {
		return new FormattedDescriptionBuilder<>()
			.addDashedLine()
			.addLine("*Elementalists wield the power of the*").styles(MAGE_LORE)
			.addLine("*elements, controlling large zones and*").styles(MAGE_LORE)
			.addLine("*dealing huge bursts of area damage.*").styles(MAGE_LORE)
			.addLine("(Area Damage, Crowd Control)")
			.addDashedLine()
			.get();
	}
}
