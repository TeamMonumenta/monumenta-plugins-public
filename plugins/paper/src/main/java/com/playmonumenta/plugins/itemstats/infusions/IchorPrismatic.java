package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.IchorCooldown;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.listeners.IchorListener;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class IchorPrismatic implements Infusion {
	private static final int COOLDOWN = 15 * 20;
	private static final String ICHOR_PRISMATIC_COOLDOWN = IchorListener.ITEM_NAME + " - Prismatic";
	private static final double PRISMATIC_MULTIPLIER = 1.5;
	public static final String DESCRIPTION = String.format("Grants a random %s buff with %s%% greater potency. Any damage buffs granted now affect all damage types. Cooldown: %s seconds.",
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
		int adjustedCooldown = Refresh.reduceCooldown(plugin, player, COOLDOWN);
		if (plugin.mEffectManager.hasEffect(player, ICHOR_PRISMATIC_COOLDOWN)) {
			return;
		}
		plugin.mEffectManager.addEffect(player, ICHOR_PRISMATIC_COOLDOWN, new IchorCooldown(adjustedCooldown, ICHOR_PRISMATIC_COOLDOWN));
		int randInt = FastUtils.randomIntInRange(0, 6);
		switch (randInt) {
			case 0 -> {
				IchorDawnbringer.ichorDawnbringer(plugin, player, PRISMATIC_MULTIPLIER);
				player.sendActionBar(Component.text(IchorListener.ITEM_NAME + " - Dawnbringer has been activated!", NamedTextColor.YELLOW));
			}
			case 1 -> {
				IchorEarthbound.ichorEarthbound(plugin, player, PRISMATIC_MULTIPLIER, true);
				player.sendActionBar(Component.text(IchorListener.ITEM_NAME + " - Earthbound has been activated!", NamedTextColor.YELLOW));
			}
			case 2 -> {
				IchorFlamecaller.ichorFlamecaller(plugin, player, PRISMATIC_MULTIPLIER, true);
				player.sendActionBar(Component.text(IchorListener.ITEM_NAME + " - Flamecaller has been activated!", NamedTextColor.YELLOW));
			}
			case 3 -> {
				IchorFrostborn.ichorFrostborn(plugin, player, PRISMATIC_MULTIPLIER);
				player.sendActionBar(Component.text(IchorListener.ITEM_NAME + " - Frostborn has been activated!", NamedTextColor.YELLOW));
			}
			case 4 -> {
				IchorShadowdancer.ichorShadowdancer(plugin, player, PRISMATIC_MULTIPLIER);
				player.sendActionBar(Component.text(IchorListener.ITEM_NAME + " - Shadowdancer has been activated!", NamedTextColor.YELLOW));
			}
			case 5 -> {
				IchorSteelsage.ichorSteelsage(plugin, player, PRISMATIC_MULTIPLIER, true);
				player.sendActionBar(Component.text(IchorListener.ITEM_NAME + " - Steelsage has been activated!", NamedTextColor.YELLOW));
			}
			default -> {
				IchorWindwalker.ichorWindwalker(plugin, player, PRISMATIC_MULTIPLIER);
				player.sendActionBar(Component.text(IchorListener.ITEM_NAME + " - Windwalker has been activated!", NamedTextColor.YELLOW));
			}
		}
	}
}
