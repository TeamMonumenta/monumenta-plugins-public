package com.playmonumenta.plugins.item.properties;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.item.properties.ItemPropertyManager.ItemSlot;

public class Frost implements ItemProperty {
	private static final int FROST_DURATION = 20 * 4;
	private static String PROPERTY_NAME = ChatColor.GRAY + "Frost";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.HAND);
	}

	@Override
	public void onShootAttack(Plugin plugin, Player player, int level, LivingEntity target, EntityDamageByEntityEvent event) {
		LivingEntity entity = (LivingEntity) event.getEntity();
		entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, FROST_DURATION, 1, true, true));
		player.getWorld().spawnParticle(Particle.SNOWBALL, entity.getLocation().add(0, 1.15, 0), 10, 0.2, 0.35, 0.2, 0.05);
	}

}
