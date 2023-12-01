package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.IchorCooldown;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.listeners.IchorListener;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.util.Vector;

public class IchorShadowdancer implements Infusion {
	private static final int COOLDOWN = 20 * 20;
	private static final String ICHOR_SHADOWDANCER_COOLDOWN = IchorListener.ITEM_NAME + " - Shadowdancer";
	private static final double THRESHOLD1 = 0.7;
	private static final double THRESHOLD2 = 0.4;
	private static final double VULN1 = 0.075;
	private static final double VULN2 = 0.1;
	private static final int RANGE1 = 5;
	private static final int RANGE2 = 7;
	private static final int DURATION = 5 * 20;
	public static final String DESCRIPTION = String.format("If you are below %s%% health, apply %s%% vulnerability to enemies within %s blocks for %s seconds. If you are below %s%% health, apply %s%% vulnerability to enemies within %s blocks for %s seconds instead. Cooldown: %s seconds.",
		StringUtils.multiplierToPercentage(THRESHOLD1),
		StringUtils.multiplierToPercentage(VULN1),
		RANGE1,
		StringUtils.ticksToSeconds(DURATION),
		StringUtils.multiplierToPercentage(THRESHOLD2),
		StringUtils.multiplierToPercentage(VULN2),
		RANGE2,
		StringUtils.ticksToSeconds(DURATION),
		StringUtils.ticksToSeconds(COOLDOWN)
	);

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.ICHOR_SHADOWDANCER;
	}

	@Override
	public String getName() {
		return IchorListener.ITEM_NAME + " - Shadowdancer";
	}

	// Want it to run before healing enchants/infusions i.e. kapple.
	@Override
	public double getPriorityAmount() {
		return 999;
	}

	@Override
	public void onConsume(Plugin plugin, Player player, double value, PlayerItemConsumeEvent event) {
		if (plugin.mEffectManager.hasEffect(player, ICHOR_SHADOWDANCER_COOLDOWN)) {
			return;
		}
		plugin.mEffectManager.addEffect(player, ICHOR_SHADOWDANCER_COOLDOWN, new IchorCooldown(COOLDOWN, ICHOR_SHADOWDANCER_COOLDOWN));
		ichorShadowdancer(plugin, player, 1);
	}

	public static void ichorShadowdancer(Plugin plugin, Player player, double multiplier) {
		double healthProportion = player.getHealth() / EntityUtils.getMaxHealth(player);
		if (healthProportion <= THRESHOLD2) {
			for (LivingEntity e : EntityUtils.getNearbyMobs(player.getLocation(), RANGE2)) {
				EntityUtils.applyVulnerability(plugin, DURATION, VULN2 * multiplier, e);
				new PPCircle(Particle.REDSTONE, e.getLocation().add(new Vector(0, e.getHeight() + 0.3, 0)), 1).ringMode(true).count(15).data(new Particle.DustOptions(Color.fromRGB(60, 60, 60), 1.1f)).spawnAsPlayerPassive(player);
			}
			player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.PLAYERS, 1f, 0.9f);
		} else if (healthProportion <= THRESHOLD1) {
			for (LivingEntity e : EntityUtils.getNearbyMobs(player.getLocation(), RANGE1)) {
				EntityUtils.applyVulnerability(plugin, DURATION, VULN1 * multiplier, e);
				new PPCircle(Particle.REDSTONE, e.getLocation().add(new Vector(0, e.getHeight() + 0.3, 0)), 1).ringMode(true).count(15).data(new Particle.DustOptions(Color.fromRGB(20, 20, 20), 1.1f)).spawnAsPlayerPassive(player);
			}
			player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.PLAYERS, 1f, 0.9f);
		} else {
			player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.PLAYERS, 1f, 0.7f);
		}
	}
}
