package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Cryogenesis extends PercentDamageReceived {
	public static final String GENERIC_NAME = "Cryogenesis";

	private static final int DURATION = 5 * 20;
	private static final int HEALTH_THRESHOLD = 4;

	public Cryogenesis(double amount) {
		super(DURATION, amount);
	}

	@Override
	public void onHurt(LivingEntity entity, DamageEvent event) {
		Plugin plugin = Plugin.getInstance();
		if (event.getFlatDamage() > HEALTH_THRESHOLD) {
			super.onHurt(entity, event);
			if (entity instanceof Player player) {
				player.playSound(player, Sound.BLOCK_GLASS_BREAK, 1f, 1f);
				player.playSound(player, Sound.ITEM_TRIDENT_HIT, 1f, 1.25f);
				Bukkit.getScheduler().runTaskLater(plugin, () -> player.playSound(player, Sound.BLOCK_GLASS_BREAK, 1f, 0.75f), 1);
			}
			this.clearEffect();
			double amount = Math.min(mAmount / 2, mAmount - 0.1);
			if (amount > 0) {
				plugin.mEffectManager.addEffect(event.getDamagee(), GENERIC_NAME, new Cryogenesis(amount));
			}
		}
	}

	@Override
	public String toString() {
		return String.format("Cryogenesis duration:%d amount:%f", this.getDuration(), mAmount);
	}
}
