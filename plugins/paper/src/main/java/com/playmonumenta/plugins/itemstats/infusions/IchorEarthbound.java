package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.IchorCooldown;
import com.playmonumenta.plugins.effects.IchorEarthEffect;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.listeners.IchorListener;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class IchorEarthbound implements Infusion {
	private static final int COOLDOWN = 20 * 20;
	private static final String ICHOR_EARTHBOUND_COOLDOWN = IchorListener.ITEM_NAME + " - Earthbound";
	private static final double RESISTANCE = -0.1;
	private static final double DAMAGE = 0.08;
	private static final int EFFECT_DURATION = 6 * 20;
	private static final int BUFF_DURATION = 8 * 20;
	private static final String EFFECT = "IchorEarthEffect";
	public static final String DESCRIPTION = String.format("The first hit of either melee, projectile, magic, fire or blast damage taken within %s seconds grants %s%% resistance to that type of damage for %s seconds. If you take none, gain %s%% melee damage for %s seconds instead. Cooldown: %s seconds.",
		StringUtils.ticksToSeconds(EFFECT_DURATION),
		StringUtils.multiplierToPercentage(RESISTANCE * -1),
		StringUtils.ticksToSeconds(BUFF_DURATION),
		StringUtils.multiplierToPercentage(DAMAGE),
		StringUtils.ticksToSeconds(BUFF_DURATION),
		StringUtils.ticksToSeconds(COOLDOWN)
	);

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.ICHOR_EARTHBOUND;
	}

	@Override
	public String getName() {
		return IchorListener.ITEM_NAME + " - Earthbound";
	}

	@Override
	public void onConsume(Plugin plugin, Player player, double value, PlayerItemConsumeEvent event) {
		int adjustedCooldown = Refresh.reduceCooldown(plugin, player, COOLDOWN);
		if (plugin.mEffectManager.hasEffect(player, ICHOR_EARTHBOUND_COOLDOWN)) {
			return;
		}
		plugin.mEffectManager.addEffect(player, ICHOR_EARTHBOUND_COOLDOWN, new IchorCooldown(adjustedCooldown, ICHOR_EARTHBOUND_COOLDOWN));
		ichorEarthbound(plugin, player, 1, false);
	}

	public static void ichorEarthbound(Plugin plugin, Player player, double multiplier, boolean isPrismatic) {
		int adjustedEffectDuration = (int) (Quench.getDurationScaling(plugin, player) * EFFECT_DURATION);
		int adjustedBuffDuration = (int) (Quench.getDurationScaling(plugin, player) * BUFF_DURATION);
		plugin.mEffectManager.addEffect(player, EFFECT, new IchorEarthEffect(adjustedEffectDuration, multiplier, RESISTANCE, DAMAGE, adjustedBuffDuration, EFFECT, isPrismatic));

		player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1f, 1.2f);

		for (int i = 0; i < 10; i++) {
			new PPCircle(Particle.REDSTONE, player.getLocation().add(0, i * 0.2, 0), 1.25).ringMode(true).count(10).data(new Particle.DustOptions(Color.fromRGB(210, 180, 150), 0.85f)).spawnAsPlayerPassive(player);
		}
	}
}
