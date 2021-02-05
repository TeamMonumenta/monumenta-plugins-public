package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;
import java.util.NavigableSet;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentSpeed;

public class CurseOfCrippling implements BaseEnchantment {
	private static String PROPERTY_NAME = ChatColor.RED + "Curse of Crippling";
	private static final String PERCENT_SPEED_EFFECT_NAME = "CripplingPercentSpeedEffect";
	private static final int DURATION = 5 * 20;
	private static final double PERCENT_SPEED = 0.3;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean useEnchantLevels() {
		return false;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR);
	}

	@Override
	public void onHurt(Plugin plugin, Player player, int level, EntityDamageEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			NavigableSet<Effect> speedEffects = plugin.mEffectManager.getEffects(player, PERCENT_SPEED_EFFECT_NAME);
			if (speedEffects != null) {
				for (Effect effect : speedEffects) {
					effect.setDuration(DURATION);
				}
			} else {
				plugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(DURATION, PERCENT_SPEED, PERCENT_SPEED_EFFECT_NAME));
			}
			player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 16, 0.4, 0.5, 0.4);
			player.playSound(player.getLocation(), Sound.ENTITY_EVOKER_FANGS_ATTACK, SoundCategory.HOSTILE, 0.25f, 0.8f);
		}
	}

}
