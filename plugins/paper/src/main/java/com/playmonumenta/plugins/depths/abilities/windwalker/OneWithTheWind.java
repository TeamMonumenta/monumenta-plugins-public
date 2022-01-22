package com.playmonumenta.plugins.depths.abilities.windwalker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class OneWithTheWind extends DepthsAbility {

	public static final String ABILITY_NAME = "One with the Wind";
	public static final double[] SPEED = {0.16, 0.2, 0.24, 0.28, 0.32, 0.4};
	public static final double[] PERCENT_DAMAGE_RECEIVED = {-.08, -.10, -.12, -.14, -.16, -.20};
	public static final int RANGE = 10;

	public OneWithTheWind(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.LIGHT_GRAY_BANNER;
		mTree = DepthsTree.WINDWALKER;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mPlayer != null && PlayerUtils.otherPlayersInRange(mPlayer, RANGE, true).size() == 0) {
			Plugin.getInstance().mEffectManager.addEffect(mPlayer, ABILITY_NAME, new PercentSpeed(40, SPEED[mRarity - 1], ABILITY_NAME));
			mPlugin.mEffectManager.addEffect(mPlayer, ABILITY_NAME, new PercentDamageReceived(40, PERCENT_DAMAGE_RECEIVED[mRarity - 1]));
		}
	}

	@Override
	public String getDescription(int rarity) {
		return "If there are no other players in an " + RANGE + " block radius, you gain " + DepthsUtils.getRarityColor(rarity) + (int) DepthsUtils.roundPercent(-PERCENT_DAMAGE_RECEIVED[rarity - 1]) + "%" + ChatColor.WHITE + " resistance and " + DepthsUtils.getRarityColor(rarity) + (int) DepthsUtils.roundPercent(SPEED[rarity - 1]) + "%" + ChatColor.WHITE + " speed.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.WINDWALKER;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.PASSIVE;
	}
}

