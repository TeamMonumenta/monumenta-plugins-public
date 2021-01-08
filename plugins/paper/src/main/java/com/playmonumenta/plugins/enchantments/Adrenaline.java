package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;
import java.util.NavigableSet;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.effects.Effect;

public class Adrenaline implements BaseEnchantment {

	private static final String PROPERTY_NAME = ChatColor.GRAY + "Adrenaline";

	private static final String PERCENT_SPEED_EFFECT_NAME = "AdrenalinePercentSpeedEffect";
	private static final int DURATION = 20 * 3;
	private static final double PERCENT_SPEED_PER_LEVEL = 0.1;

	private static final Particle.DustOptions RED_COLOR = new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.0f);

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR);
	}

	@Override
	public void onAttack(Plugin plugin, Player player, int level, LivingEntity target, EntityDamageByEntityEvent event) {
		player.getWorld().spawnParticle(Particle.REDSTONE, player.getLocation().add(0, 1, 0), 12, 0.4, 0.5, 0.4, RED_COLOR);
		NavigableSet<Effect> speedEffects = plugin.mEffectManager.getEffects(player, PERCENT_SPEED_EFFECT_NAME);
		if (speedEffects != null) {
			for (Effect effect : speedEffects) {
				effect.setDuration(DURATION);
			}
		} else {
			plugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(DURATION, PERCENT_SPEED_PER_LEVEL * level, PERCENT_SPEED_EFFECT_NAME));
		}
	}

}
