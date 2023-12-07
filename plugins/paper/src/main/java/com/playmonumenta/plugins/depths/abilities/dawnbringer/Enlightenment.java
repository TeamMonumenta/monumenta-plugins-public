package com.playmonumenta.plugins.depths.abilities.dawnbringer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class Enlightenment extends DepthsAbility {

	//Technical implementation of this ability is handled in the depths listener, so that any member of the party can benefit from it

	public static final String ABILITY_NAME = "Enlightenment";
	public static final double[] XP_MULTIPLIER = {0.3, 0.35, 0.4, 0.45, 0.5, 1.0};
	public static final double[] RARITY_INCREASE = {0.03, 0.04, 0.05, 0.05, 0.07, 0.3};

	public static final DepthsAbilityInfo<Enlightenment> INFO =
		new DepthsAbilityInfo<>(Enlightenment.class, ABILITY_NAME, Enlightenment::new, DepthsTree.DAWNBRINGER, DepthsTrigger.PASSIVE)
			.displayItem(Material.EXPERIENCE_BOTTLE)
			.descriptions(Enlightenment::getDescription)
			.singleCharm(false);

	private final double mXPMultiplier;
	private final double mRarityIncrease;

	public Enlightenment(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mXPMultiplier = XP_MULTIPLIER[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.ENLIGHTENMENT_XP_MULTIPLIER.mEffectName);
		mRarityIncrease = RARITY_INCREASE[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.ENLIGHTENMENT_RARITY_INCREASE.mEffectName);
	}

	public double getXPMultiplier() {
		return 1 + mXPMultiplier;
	}

	public double getRarityIncrease() {
		return mRarityIncrease;
	}

	private static Description<Enlightenment> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Enlightenment>(color)
			.add("All players in your party gain ")
			.addPercent(a -> a.mXPMultiplier, XP_MULTIPLIER[rarity - 1], false, true)
			.add(" more experience. Does not stack if multiple players in the party have the skill. Additionally, your chances of finding higher rarity abilities are increased by ")
			.addPercent(a -> a.mRarityIncrease, RARITY_INCREASE[rarity - 1], false, true)
			.add(".");
	}

}

