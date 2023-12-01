package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.IchorCooldown;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.listeners.IchorListener;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.List;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.util.Vector;

public class IchorDawnbringer implements Infusion {
	private static final int COOLDOWN = 20 * 20;
	private static final String ICHOR_DAWNBRINGER_COOLDOWN = IchorListener.ITEM_NAME + " - Dawnbringer";
	private static final int RANGE = 10;
	private static final double HEALING_PER = 0.04;
	private static final int HEALING_PLAYER_CAP = 3;
	private static final int BUFF_DURATION = 5 * 20;
	private static final String EFFECT = "IchorDawnHealingEffect";
	public static final String DESCRIPTION = String.format("Gain %s%% healing per player within %s blocks (%s%% cap) for %s seconds, share with these players also. Cooldown: %s seconds.",
		StringUtils.multiplierToPercentage(HEALING_PER),
		RANGE,
		StringUtils.multiplierToPercentage(HEALING_PLAYER_CAP * HEALING_PER),
		StringUtils.ticksToSeconds(BUFF_DURATION),
		StringUtils.ticksToSeconds(COOLDOWN)
	);

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.ICHOR_DAWNBRINGER;
	}

	@Override
	public String getName() {
		return IchorListener.ITEM_NAME + " - Dawnbringer";
	}

	@Override
	public void onConsume(Plugin plugin, Player player, double value, PlayerItemConsumeEvent event) {
		if (plugin.mEffectManager.hasEffect(player, ICHOR_DAWNBRINGER_COOLDOWN)) {
			return;
		}
		plugin.mEffectManager.addEffect(player, ICHOR_DAWNBRINGER_COOLDOWN, new IchorCooldown(COOLDOWN, ICHOR_DAWNBRINGER_COOLDOWN));
		ichorDawnbringer(plugin, player, 1);
	}

	public static void ichorDawnbringer(Plugin plugin, Player player, double multiplier) {
		List<Player> playersInRange = PlayerUtils.playersInRange(player.getLocation(), RANGE, true);
		int count = playersInRange.size() - 1;
		int cappedCount = FastMath.min(count, HEALING_PLAYER_CAP);
		double buffMultiplier = cappedCount * multiplier;
		if (buffMultiplier == 0) {
			player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.3f, 0.5f);
			return;
		}
		for (Player playerIterator : playersInRange) {
			plugin.mEffectManager.addEffect(playerIterator, EFFECT, new PercentHeal(BUFF_DURATION, HEALING_PER * buffMultiplier));
			new PPCircle(Particle.REDSTONE, playerIterator.getLocation().add(new Vector(0, 2.5, 0)), 1).ringMode(true).count(15).data(new Particle.DustOptions(Color.fromRGB(255, 250, 150), 1.1f)).spawnAsPlayerPassive(player);
		}

		player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.3f, 1.0f + cappedCount * 0.2f);
	}
}
