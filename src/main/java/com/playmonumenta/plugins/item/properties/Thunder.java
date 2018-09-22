package com.playmonumenta.plugins.item.properties;

import com.playmonumenta.plugins.item.properties.ItemPropertyManager.ItemSlot;
import com.playmonumenta.plugins.Plugin;

import java.util.EnumSet;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;
import org.bukkit.World;

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
	public double onAttack(Plugin plugin, World world, Player player, LivingEntity target, double damage, int level, DamageCause cause) {
		double rand = mRandom.nextDouble();

		if (damage >= 4 && rand < level * 0.1) {
			target.addPotionEffect(THUNDER_SLOWNESS);
			target.addPotionEffect(THUNDER_WEAKNESS);

			if (target instanceof Guardian) {
				damage = damage + 1.0;
			}

			world.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.4f, 1.0f);
		}

		return damage;
	}
}
