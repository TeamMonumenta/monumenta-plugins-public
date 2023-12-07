package com.playmonumenta.plugins.depths.abilities.dawnbringer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.effects.CustomRegeneration;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class DepthsRejuvenation extends DepthsAbility {

	public static final String ABILITY_NAME = "Rejuvenation";
	public static final int[] HEAL_INTERVAL = {100, 90, 80, 70, 60, 50};
	public static final int RADIUS = 12;
	public static final double PERCENT_HEAL = .05;
	public static final String REGENERATION_EFFECT = "DepthsRejuvenationRegenerationEffect";

	public static final DepthsAbilityInfo<DepthsRejuvenation> INFO =
		new DepthsAbilityInfo<>(DepthsRejuvenation.class, ABILITY_NAME, DepthsRejuvenation::new, DepthsTree.DAWNBRINGER, DepthsTrigger.PASSIVE)
			.displayItem(Material.NETHER_STAR)
			.descriptions(DepthsRejuvenation::getDescription)
			.singleCharm(false);

	private final double mHealPercent;
	private final double mRadius;
	private final int mHealInterval;

	public DepthsRejuvenation(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mHealPercent = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.REJUVENATION_HEALING.mEffectName, PERCENT_HEAL);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.REJUVENATION_HEAL_RADIUS.mEffectName, RADIUS);
		mHealInterval = HEAL_INTERVAL[mRarity - 1];
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		List<Player> players = PlayerUtils.playersInRange(mPlayer.getLocation(), mRadius, true);
		for (Player player : players) {
			double maxHealth = EntityUtils.getMaxHealth(player);
			double healing = mHealPercent * maxHealth;
			mPlugin.mEffectManager.addEffect(player, REGENERATION_EFFECT, new CustomRegeneration(10, healing, mHealInterval, mPlayer, mPlugin));
		}
	}

	private static Description<DepthsRejuvenation> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<DepthsRejuvenation>(color)
			.add("All players within ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks of you (including yourself) heal ")
			.addPercent(a -> a.mHealPercent, PERCENT_HEAL)
			.add(" of their max health every ")
			.addDuration(a -> a.mHealInterval, HEAL_INTERVAL[rarity - 1], true, true)
			.add(" seconds. A given player will only be healed by the highest Rejuvenation that affects them.");
	}

}

