package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.IchorCooldown;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.listeners.IchorListener;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class IchorFrostborn implements Infusion {
	private static final int COOLDOWN = 20 * 20;
	private static final String ICHOR_FROSTBORN_COOLDOWN = IchorListener.ITEM_NAME + " - Frostborn";
	private static final double ABSORPTION = 0.1;
	private static final int BUFF_DURATION = 10 * 20;
	public static final String DESCRIPTION = String.format("If you have no absorption, gain %s%% absorption for %s seconds. Cooldown: %s seconds.",
		StringUtils.multiplierToPercentage(ABSORPTION),
		StringUtils.ticksToSeconds(BUFF_DURATION),
		StringUtils.ticksToSeconds(COOLDOWN)
	);

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.ICHOR_FROSTBORN;
	}

	@Override
	public String getName() {
		return IchorListener.ITEM_NAME + " - Frostborn";
	}

	@Override
	public void onConsume(Plugin plugin, Player player, double value, PlayerItemConsumeEvent event) {
		if (plugin.mEffectManager.hasEffect(player, ICHOR_FROSTBORN_COOLDOWN)) {
			return;
		}
		plugin.mEffectManager.addEffect(player, ICHOR_FROSTBORN_COOLDOWN, new IchorCooldown(COOLDOWN, ICHOR_FROSTBORN_COOLDOWN));
		ichorFrostborn(player, 1);
	}

	public static void ichorFrostborn(Player player, double multiplier) {
		if (AbsorptionUtils.getAbsorption(player) > 0) {
			player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.PLAYERS, 1f, 0.5f);
			return;
		}
		double maxHealth = EntityUtils.getMaxHealth(player);
		double absorptionQuantity = maxHealth * ABSORPTION * multiplier;
		AbsorptionUtils.addAbsorption(player, absorptionQuantity, absorptionQuantity, BUFF_DURATION);

		player.playSound(player.getLocation(), Sound.ENTITY_SNOW_GOLEM_SHEAR, SoundCategory.PLAYERS, 1f, 0.5f);
		player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.PLAYERS, 1f, 1f);

		for (int i = 0; i < 10; i++) {
			new PPCircle(Particle.REDSTONE, player.getLocation().add(0, i * 0.2, 0), 1.25).ringMode(true).count(10).data(new Particle.DustOptions(Color.fromRGB(150, 255, 255), 0.85f)).spawnAsPlayerPassive(player);
		}
	}
}
