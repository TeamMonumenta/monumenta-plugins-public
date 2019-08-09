package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;

public class Thunder implements BaseEnchantment {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Thunder Aspect";
	private static final Particle.DustOptions YELLOW_1_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 20), 1.0f);
	private static final Particle.DustOptions YELLOW_2_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 120), 1.0f);

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
		World world = target.getWorld();

		if (event.getDamage() >= 4 && rand < level * 0.1) {
			if (EntityUtils.isElite(target)) {
				EntityUtils.applyStun(plugin, 10, target);
			} else {
				EntityUtils.applyStun(plugin, 50, target);
			}

			if (target instanceof Guardian || target instanceof IronGolem) {
				event.setDamage(event.getDamage() + 1.0);
			}

			world.spawnParticle(Particle.REDSTONE, target.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, YELLOW_1_COLOR);
			world.spawnParticle(Particle.REDSTONE, target.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, YELLOW_2_COLOR);
			world.spawnParticle(Particle.FIREWORKS_SPARK, target.getLocation().add(0, 1, 0), 15, 0, 0, 0, 0.15);
			player.getWorld().playSound(target.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.65f, 1.5f);
		}
	}
}
