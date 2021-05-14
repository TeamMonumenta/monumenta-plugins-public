package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;
import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class Quake implements BaseEnchantment {

	private static String PROPERTY_NAME = ChatColor.GRAY + "Quake";
	private static final float RADIUS = 3.0f;
	private static final float DAMAGE_MODIFIER_PER_LEVEL = 0.1f;
	private static final Particle.DustOptions YELLOW_1_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 20), 1.0f);
	private static final Particle.DustOptions YELLOW_2_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 120), 1.0f);
	private static final Particle.DustOptions BLEED_COLOR = new Particle.DustOptions(Color.fromRGB(210, 44, 44), 1.0f);


	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.MAINHAND);
	}

	@Override
	public void onKill(Plugin plugin, Player player, int level, Entity target, EntityDeathEvent event) {
		EntityDamageEvent e = target.getLastDamageCause();
		if (e != null && (e.getCause() == DamageCause.ENTITY_ATTACK)) {
			List<LivingEntity> mobs = EntityUtils.getNearbyMobs(target.getLocation(), RADIUS);

			//Get enchant levels on weapon
			ItemStack item = player.getInventory().getItemInMainHand();
			int fire = item.getEnchantmentLevel(Enchantment.FIRE_ASPECT);
			int ice = InventoryUtils.getCustomEnchantLevel(item, IceAspect.PROPERTY_NAME, true);
			int thunder = InventoryUtils.getCustomEnchantLevel(item, ThunderAspect.PROPERTY_NAME, true);
			int decay = InventoryUtils.getCustomEnchantLevel(item, Decay.PROPERTY_NAME, true);
			int bleed = InventoryUtils.getCustomEnchantLevel(item, Bleeding.PROPERTY_NAME, true);

			//Damage any mobs in the area
			for (LivingEntity mob : mobs) {
				EntityUtils.damageEntity(plugin, mob, e.getDamage() * DAMAGE_MODIFIER_PER_LEVEL * level, player);
				if (fire > 0) {
					EntityUtils.applyFire(plugin, 80 * fire, mob, player);
				}
				if (ice > 0) {
					PotionUtils.applyPotion(player, mob, new PotionEffect(PotionEffectType.SLOW, 100, ice - 1, false, true));
				}
				if (thunder > 0) {
					EntityUtils.applyStun(plugin, 10 * thunder, mob);
				}
				if (decay > 0) {
					PotionUtils.applyPotion(player, mob, new PotionEffect(PotionEffectType.WITHER, 80, decay - 1, false, true));
				}
				if (bleed > 0) {
					EntityUtils.applyBleed(plugin, 100, level, mob);
				}
			}

			if (fire + ice + thunder + decay + bleed == 0) {
				player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.5f, 0.5f);
			}
			if (fire > 0) {
				player.playSound(player.getLocation(), Sound.BLOCK_LAVA_POP, 0.6f, 0.9f);
				player.getWorld().spawnParticle(Particle.LAVA, target.getLocation(), 25, 1.5, 1.5, 1.5);
			}
			if (ice > 0) {
				player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.6f, 1.3f);
				player.getWorld().spawnParticle(Particle.SNOW_SHOVEL, target.getLocation(), 25, 1.5, 1.5, 1.5);
			}
			if (thunder > 0) {
				player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 0.8f);
				player.getWorld().spawnParticle(Particle.REDSTONE, target.getLocation(), 12, 1.5, 1.5, 1.5, YELLOW_1_COLOR);
				player.getWorld().spawnParticle(Particle.REDSTONE, target.getLocation(), 12, 1.5, 1.5, 1.5, YELLOW_2_COLOR);
			}
			if (decay > 0) {
				player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.4f, 0.7f);
				player.getWorld().spawnParticle(Particle.SQUID_INK, target.getLocation(), 25, 1.5, 1.5, 1.5);
			}
			if (bleed > 0) {
				player.playSound(player.getLocation(), Sound.BLOCK_SLIME_BLOCK_BREAK, 0.7f, 1.0f);
				player.getWorld().spawnParticle(Particle.REDSTONE, target.getLocation(), 25, 1.5, 1.5, 1.5, BLEED_COLOR);
			}
		}
	}
}
