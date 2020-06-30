package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

public class Adrenaline implements BaseEnchantment {

	private static final String PROPERTY_NAME = ChatColor.GRAY + "Adrenaline";
	private static final String ADRENALINE_MODIFIER = "AdrenalineSpeedModifier";

	private static final int DURATION = 20 * 3;
	private static final double SPEED_PER_LEVEL = 0.1;
	private static final Particle.DustOptions RED_COLOR = new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.0f);

	private static final Map<Player, BukkitRunnable> TIMERS = new HashMap<Player, BukkitRunnable>();

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

		removeEffects(player);
		applyEffects(player, level);

		BukkitRunnable timer = TIMERS.get(player);
		if (timer != null && !timer.isCancelled()) {
			timer.cancel();
		}

		timer = new BukkitRunnable() {
			@Override
			public void run() {
				removeEffects(player);
				this.cancel();
			}
		};
		timer.runTaskLater(plugin, DURATION);

		TIMERS.put(player, timer);
	}

	private static void applyEffects(Player player, int level) {
		AttributeInstance speed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
		if (speed != null) {
			speed.addModifier(new AttributeModifier(ADRENALINE_MODIFIER,
					SPEED_PER_LEVEL * level, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}
	}

	private static void removeEffects(Player player) {
		AttributeInstance speed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
		if (speed != null) {
			for (AttributeModifier modifier : speed.getModifiers()) {
				if (modifier != null && modifier.getName().startsWith(ADRENALINE_MODIFIER)) {
					speed.removeModifier(modifier);
				}
			}
		}
	}

}
