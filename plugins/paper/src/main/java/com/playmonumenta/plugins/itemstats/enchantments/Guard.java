package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.OnHitTimerEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.NavigableSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class Guard implements Enchantment {
	public static final double HEALTH_RATIO = 0.35;
	public static final int PAST_HIT_DURATION_TIME_MAINHAND = 6 * 20;
	public static final int PAST_HIT_DURATION_TIME_OFFHAND = 4 * 20;
	public static final int PAST_HIT_DURATION_TIME_HEALTH = 2 * 20;
	private static final String GUARD_EFFECT_NAME = "GuardEffect";

	@Override
	public String getName() {
		return "Guard";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.GUARD;
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.isBlockedByShield()) {
			plugin.mEffectManager.addEffect(player, GUARD_EFFECT_NAME, new OnHitTimerEffect(player.getInventory().getItemInMainHand().getType() == Material.SHIELD ? PAST_HIT_DURATION_TIME_MAINHAND : PAST_HIT_DURATION_TIME_OFFHAND));
		} else if (event.getFinalDamage(true) / EntityUtils.getMaxHealth(player) >= HEALTH_RATIO) {
			plugin.mEffectManager.addEffect(player, GUARD_EFFECT_NAME, new OnHitTimerEffect(PAST_HIT_DURATION_TIME_HEALTH));
		} else {
			return;
		}

		new PartialParticle(Particle.REDSTONE, player.getLocation().add(0, 1, 0), 24, 0.4, 0.5, 0.4, new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.0f)).spawnAsPlayerBuff(player);
		player.sendActionBar(Component.text("Guard", NamedTextColor.RED));
	}

	public static double applyGuard(DamageEvent event, Plugin plugin, Player player) {
		NavigableSet<Effect> guard = plugin.mEffectManager.getEffects(player, GUARD_EFFECT_NAME);
		if (event.getFinalDamage(true) / EntityUtils.getMaxHealth(player) >= HEALTH_RATIO || guard != null) {
			return plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.GUARD);
		} else {
			return 0;
		}
	}

}
