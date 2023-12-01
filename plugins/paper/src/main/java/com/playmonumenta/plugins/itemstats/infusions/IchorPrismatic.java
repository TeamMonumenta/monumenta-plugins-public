package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.IchorCooldown;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.listeners.IchorListener;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class IchorPrismatic implements Infusion {
	private static final int COOLDOWN = 15 * 20;
	private static final String ICHOR_PRISMATIC_COOLDOWN = IchorListener.ITEM_NAME + " - Prismatic";
	private static final double PRISMATIC_MULTIPLIER = 1.5;
	public static final String DESCRIPTION = String.format("Grants a random %s buff with %s%% greater potency. Cooldown: %s seconds.",
		IchorListener.ITEM_NAME,
		StringUtils.multiplierToPercentage(PRISMATIC_MULTIPLIER - 1),
		StringUtils.ticksToSeconds(COOLDOWN)
	);

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.ICHOR_PRISMATIC;
	}

	@Override
	public String getName() {
		return IchorListener.ITEM_NAME + " - Prismatic";
	}

	// Need to run early to match Windwalker and Shadowdancer
	@Override
	public double getPriorityAmount() {
		return 999;
	}

	@Override
	public void onConsume(Plugin plugin, Player player, double value, PlayerItemConsumeEvent event) {
		if (plugin.mEffectManager.hasEffect(player, ICHOR_PRISMATIC_COOLDOWN)) {
			return;
		}
		plugin.mEffectManager.addEffect(player, ICHOR_PRISMATIC_COOLDOWN, new IchorCooldown(COOLDOWN, ICHOR_PRISMATIC_COOLDOWN));
		int randInt = FastUtils.randomIntInRange(0, 6);
		switch (randInt) {
			case 0 -> IchorDawnbringer.ichorDawnbringer(plugin, player, PRISMATIC_MULTIPLIER);
			case 1 -> IchorEarthbound.ichorEarthbound(plugin, player, PRISMATIC_MULTIPLIER);
			case 2 -> IchorFlamecaller.ichorFlamecaller(plugin, player, PRISMATIC_MULTIPLIER);
			case 3 -> IchorFrostborn.ichorFrostborn(player, PRISMATIC_MULTIPLIER);
			case 4 -> IchorShadowdancer.ichorShadowdancer(plugin, player, PRISMATIC_MULTIPLIER);
			case 5 -> IchorSteelsage.ichorSteelsage(plugin, player, PRISMATIC_MULTIPLIER);
			default -> IchorWindwalker.ichorWindwalker(plugin, player, PRISMATIC_MULTIPLIER);
		}
	}
}
