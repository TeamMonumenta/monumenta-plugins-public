package com.playmonumenta.plugins.classes;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.shaman.ChainLightning;
import com.playmonumenta.plugins.abilities.shaman.CleansingTotem;
import com.playmonumenta.plugins.abilities.shaman.EarthenTremor;
import com.playmonumenta.plugins.abilities.shaman.FlameTotem;
import com.playmonumenta.plugins.abilities.shaman.GiftOfSentience;
import com.playmonumenta.plugins.abilities.shaman.IgnitionDrive;
import com.playmonumenta.plugins.abilities.shaman.InterconnectedHavoc;
import com.playmonumenta.plugins.abilities.shaman.LightningTotem;
import com.playmonumenta.plugins.abilities.shaman.Spiritualism;
import com.playmonumenta.plugins.abilities.shaman.TotemicProjection;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.DecayedTotem;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.Devastation;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.SpiritcatcherOrbs;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.SpiritualCombos;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.TotemicConsecration;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.WhirlwindTotem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.utils.DescriptionUtils.SHAMAN_LORE;


public class Shaman extends PlayerClass {

	public static final int CLASS_ID = 8;
	public static final int SOOTHSAYER_ID = 15;
	public static final int HEXBREAKER_ID = 16;

	public static final Style TOTEM_COLOR = Style.style(TextColor.color(0xC6927));

	public Shaman() {
		mAbilities.add(CleansingTotem.INFO);
		mAbilities.add(Spiritualism.INFO);
		mAbilities.add(FlameTotem.INFO);
		mAbilities.add(InterconnectedHavoc.INFO);
		mAbilities.add(LightningTotem.INFO);
		mAbilities.add(ChainLightning.INFO);
		mAbilities.add(EarthenTremor.INFO);
		mAbilities.add(IgnitionDrive.INFO);
		mClass = CLASS_ID;
		mClassName = "Shaman";
		mDisplayItem = Material.TOTEM_OF_UNDYING;
		mClassDescription = "Shamans excel in strategic positioning and location defense, using their stationary totems to control the battle.";
		mPassive = TotemicProjection.INFO;
		mUltimate = GiftOfSentience.INFO;

		mSpecOne.mAbilities.add(SpiritualCombos.INFO);
		mSpecOne.mAbilities.add(WhirlwindTotem.INFO);
		mSpecOne.mAbilities.add(TotemicConsecration.INFO);
		mSpecOne.mSpecQuestScoreboard = "Quest103o";
		mSpecOne.mSpecialization = SOOTHSAYER_ID;
		mClassColor = TextColor.fromHexString("#009900");
		mClassGlassFiller = Material.GREEN_STAINED_GLASS_PANE;
		mSpecOne.mSpecName = "Soothsayer";
		mSpecOne.mDisplayItem = Material.OAK_SAPLING;
		mSpecOne.mDescription = "Focuses on using your tools to support your team and turn the tide of battle.";

		mSpecTwo.mAbilities.add(SpiritcatcherOrbs.INFO);
		mSpecTwo.mAbilities.add(DecayedTotem.INFO);
		mSpecTwo.mAbilities.add(Devastation.INFO);
		mSpecTwo.mSpecQuestScoreboard = "Quest103p";
		mSpecTwo.mSpecialization = HEXBREAKER_ID;
		mSpecTwo.mSpecName = "Hexbreaker";
		mSpecTwo.mDisplayItem = Material.MAGMA_BLOCK;
		mSpecTwo.mDescription = "Deals in dark magics, focusing on harming enemies at the cost of totems.";

		mTriggerOrder = ImmutableList.of(
			GiftOfSentience.INFO,
			InterconnectedHavoc.INFO,
			CleansingTotem.INFO,
			TotemicProjection.INFO,

			LightningTotem.INFO,
			WhirlwindTotem.INFO,
			TotemicConsecration.INFO,
			ChainLightning.INFO,
			SpiritualCombos.INFO,

			DecayedTotem.INFO,
			Devastation.INFO,
			SpiritcatcherOrbs.INFO,

			FlameTotem.INFO,
			EarthenTremor.INFO,
			IgnitionDrive.INFO
		);

	}

	@Override
	public Component getDescription(Player player) {
		return new FormattedDescriptionBuilder<>(() -> TotemicProjection.INFO)
			.addDashedLine()
			.addLine("*Shamans use their totems to control and*").styles(SHAMAN_LORE)
			.addLine("*defend strategic locations.*").styles(SHAMAN_LORE)
			.addLine()
			.addLine("*Totemic Projection (Class Passive):*").styles(Style.style(mClassColor))
			.add(TotemicProjection.getDescription())
			.addDashedLine()
			.get(AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, TotemicProjection.class), player);
	}

	@Override
	public Component getSpecOneDescription(Player player) {
		return new FormattedDescriptionBuilder<>()
			.addDashedLine()
			.addLine("*Soothsayers channel the spiritual forces,*").styles(SHAMAN_LORE)
			.addLine("*empowering allies and keeping hordes of*").styles(SHAMAN_LORE)
			.addLine("*enemies at bay.*").styles(SHAMAN_LORE)
			.addLine("(Support, Cooldown Reduction)")
			.addDashedLine()
			.get();
	}

	@Override
	public Component getSpecTwoDescription(Player player) {
		return new FormattedDescriptionBuilder<>()
			.addDashedLine()
			.addLine("*Hexbreakers deal in omens and rituals,*").styles(SHAMAN_LORE)
			.addLine("*sacrificing their totems to deal potent*").styles(SHAMAN_LORE)
			.addLine("*area damage to those who oppose them.*").styles(SHAMAN_LORE)
			.addLine("(Burst Damage, Crowd Control)")
			.addDashedLine()
			.get();
	}
}
