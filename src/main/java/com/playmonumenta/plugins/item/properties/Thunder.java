package com.playmonumenta.plugins.item.properties;

import java.util.EnumSet;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.item.properties.ItemPropertyManager.ItemSlot;

public class Thunder implements ItemProperty {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Thunder Aspect";
	private static final PotionEffect THUNDER_SLOWNESS = new PotionEffect(PotionEffectType.SLOW, 50, 8, false, true);
	private static final PotionEffect THUNDER_WEAKNESS = new PotionEffect(PotionEffectType.WEAKNESS, 50, 8, false, true);
	private Random mRandom = new Random();

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND);
	}

	@Override
	public void onAttack(Plugin plugin, Player player, int level, LivingEntity target, EntityDamageByEntityEvent event) {
		double rand = mRandom.nextDouble();

		if (event.getDamage() >= 4 && rand < level * 0.1) {
			target.addPotionEffect(THUNDER_SLOWNESS);
			target.addPotionEffect(THUNDER_WEAKNESS);

			if (target instanceof Guardian) {
				event.setDamage(event.getDamage() + 1.0);
			}

			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.4f, 1.0f);
		}
	}
}
