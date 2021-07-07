package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;
import java.util.List;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;


import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;



public class Eruption implements BaseEnchantment {

	private static String PROPERTY_NAME = ChatColor.GRAY + "Eruption";
	private static final float RADIUS = 5.0f;
	private static final float DAMAGE_PER_LEVEL = 4.0f;
	private static final String ICE_NAME = ChatColor.GRAY + "Ice Aspect";
	private static final String THUNDER_NAME = ChatColor.GRAY + "Thunder Aspect";
	private static final String DECAY_NAME = ChatColor.GRAY + "Decay";
	private static final String BLEED_NAME = ChatColor.GRAY + "Bleeding";
	private static final String SAPPER_NAME = ChatColor.GRAY + "Sapper";
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
	public void onBlockBreak(Plugin plugin, Player player, BlockBreakEvent event, ItemStack item, int level) {
		//If we break a spawner with a pickaxe
		if (ItemUtils.isPickaxe(item) && event.getBlock().getType() == Material.SPAWNER) {
			List<LivingEntity> mobs = EntityUtils.getNearbyMobs(event.getBlock().getLocation(), RADIUS);

			//Get enchant levels on pickaxe
			int fire = item.getEnchantmentLevel(Enchantment.FIRE_ASPECT);
			int ice = InventoryUtils.getCustomEnchantLevel(item, ICE_NAME, true);
			int thunder = InventoryUtils.getCustomEnchantLevel(item, THUNDER_NAME, true);
			int decay = InventoryUtils.getCustomEnchantLevel(item, DECAY_NAME, true);
			int bleed = InventoryUtils.getCustomEnchantLevel(item, BLEED_NAME, true);
			int sapper = InventoryUtils.getCustomEnchantLevel(item, SAPPER_NAME, true);


			//Damage any mobs in the area
			for (LivingEntity mob : mobs) {
				EntityUtils.damageEntity(plugin, mob, DAMAGE_PER_LEVEL * level, player);
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

			//Sapper Interaction
			if (sapper > 0) {
				player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 2.0f, 1.6f);
				player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.05f, 1.0f);
				player.getWorld().spawnParticle(Particle.HEART, event.getBlock().getLocation().add(0, 1, 0), 25, 1.5, 1.5, 1.5);
				for (Player p : PlayerUtils.playersInRange(event.getBlock().getLocation(), RADIUS, true)) {
					if (p == player) {
						continue;
					}
					PlayerUtils.healPlayer(p, sapper);
				}
			}

			//Visual feedback
			//Fire- default effect
			if (fire > 0 || fire + ice + thunder + decay == 0) {
				player.playSound(player.getLocation(), Sound.BLOCK_LAVA_POP, 0.6f, 0.9f);
				player.getWorld().spawnParticle(Particle.LAVA, event.getBlock().getLocation(), 25, 1.5, 1.5, 1.5);
			}
			if (ice > 0) {
				player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.6f, 1.3f);
				player.getWorld().spawnParticle(Particle.SNOW_SHOVEL, event.getBlock().getLocation(), 25, 1.5, 1.5, 1.5);
			}
			if (thunder > 0) {
				player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 0.8f);
				player.getWorld().spawnParticle(Particle.REDSTONE, event.getBlock().getLocation(), 12, 1.5, 1.5, 1.5, YELLOW_1_COLOR);
				player.getWorld().spawnParticle(Particle.REDSTONE, event.getBlock().getLocation(), 12, 1.5, 1.5, 1.5, YELLOW_2_COLOR);
			}
			if (decay > 0) {
				player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.4f, 0.7f);
				player.getWorld().spawnParticle(Particle.SQUID_INK, event.getBlock().getLocation(), 25, 1.5, 1.5, 1.5);
			}
			if (bleed > 0) {
				player.playSound(player.getLocation(), Sound.ENTITY_SLIME_SQUISH, 0.7f, 0.7f);
				player.getWorld().spawnParticle(Particle.REDSTONE, event.getBlock().getLocation(), 25, 1.5, 1.5, 1.5, BLEED_COLOR);
			}
		}
	}
}