package com.playmonumenta.plugins.depths.abilities.windwalker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class OneWithTheWind extends DepthsAbility {

	public static final String ABILITY_NAME = "One with the Wind";
	public static final double[] SPEED = {0.16, 0.2, 0.24, 0.28, 0.32, 0.4};
	public static final double[] PERCENT_DAMAGE_RECEIVED = {0.08, 0.10, 0.12, 0.14, 0.16, 0.20};
	public static final int RANGE = 10;
	public static final String SPEED_EFFECT_NAME = "OneWithTheWindSpeedEffect";
	public static final String RESISTANCE_EFFECT_NAME = "OneWithTheWindResistanceEffect";

	public static final DepthsAbilityInfo<OneWithTheWind> INFO =
		new DepthsAbilityInfo<>(OneWithTheWind.class, ABILITY_NAME, OneWithTheWind::new, DepthsTree.WINDWALKER, DepthsTrigger.PASSIVE)
			.displayItem(Material.LIGHT_GRAY_BANNER)
			.descriptions(OneWithTheWind::getDescription)
			.singleCharm(false);

	private final double mSpeed;
	private final double mResist;
	private final double mRange;

	private boolean mActive = false;

	public OneWithTheWind(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mSpeed = SPEED[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.ONE_WITH_THE_WIND_SPEED_AMPLIFIER.mEffectName);
		mResist = PERCENT_DAMAGE_RECEIVED[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.ONE_WITH_THE_WIND_RESISTANCE_AMPLIFIER.mEffectName);
		mRange = CharmManager.getRadius(mPlayer, CharmEffects.ONE_WITH_THE_WIND_RANGE.mEffectName, RANGE);
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		boolean wasActive = mActive;
		if (PlayerUtils.otherPlayersInRange(mPlayer, mRange, true).isEmpty()) {
			mPlugin.mEffectManager.addEffect(mPlayer, SPEED_EFFECT_NAME, new PercentSpeed(40, mSpeed, SPEED_EFFECT_NAME).displaysTime(false));
			mPlugin.mEffectManager.addEffect(mPlayer, RESISTANCE_EFFECT_NAME, new PercentDamageReceived(40, -mResist).displaysTime(false));
			mActive = true;
		} else {
			mActive = mPlugin.mEffectManager.hasEffect(mPlayer, RESISTANCE_EFFECT_NAME);
		}
		if (wasActive != mActive) {
			ClientModHandler.updateAbility(mPlayer, this);
		}
	}

	private static Description<OneWithTheWind> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<OneWithTheWind>(color)
			.add("If there are no other players in a ")
			.add(a -> a.mRange, RANGE, true)
			.add(" block radius, you gain ")
			.addPercent(a -> a.mResist, PERCENT_DAMAGE_RECEIVED[rarity - 1], false, true)
			.add(" resistance and ")
			.addPercent(a -> a.mSpeed, SPEED[rarity - 1], false, true)
			.add(" speed.");
	}


	@Override
	public @Nullable String getMode() {
		return mActive ? "active" : null;
	}
}

