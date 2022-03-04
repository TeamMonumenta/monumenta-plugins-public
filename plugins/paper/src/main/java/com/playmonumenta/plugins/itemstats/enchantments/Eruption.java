package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class Eruption implements Enchantment {

	private static final float RADIUS = 5.0f;
	private static final float SAPPER_RADIUS = 8.0f;
	private static final float DAMAGE_PER_LEVEL = 4.0f;
	private static final Particle.DustOptions YELLOW_1_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 20), 1.0f);
	private static final Particle.DustOptions YELLOW_2_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 120), 1.0f);
	private static final Particle.DustOptions BLEED_COLOR = new Particle.DustOptions(Color.fromRGB(210, 44, 44), 1.0f);
	private static final Particle.DustOptions RED_COLOR = new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.0f);

	private static final String PERCENT_SPEED_EFFECT_NAME = "AdrenalinePercentSpeedEffect";
	private static final double PERCENT_SPEED_PER_LEVEL = 0.1;
	private static final int SPEED_DURATION = 20 * 6;

	@Override
	public @NotNull String getName() {
		return "Eruption";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.ERUPTION;
	}

	@Override
	public void onBlockBreak(@NotNull Plugin plugin, @NotNull Player player, double value, @NotNull BlockBreakEvent event) {
		ItemStack item = player.getInventory().getItemInMainHand();
		if (ItemUtils.isPickaxe(item) && event.getBlock().getType() == Material.SPAWNER) {
			double level = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.ERUPTION);
			List<LivingEntity> mobs = EntityUtils.getNearbyMobs(event.getBlock().getLocation(), RADIUS);

			//Get enchant levels on pickaxe
			int fire = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.FIRE_ASPECT);
			int ice = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.ICE_ASPECT);
			int thunder = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.THUNDER_ASPECT);
			int decay = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.DECAY);
			int bleed = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.BLEEDING);
			int sapper = (int) plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.SAPPER);
			int adrenaline = (int) plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.ADRENALINE);
			if (ItemUtils.isPickaxe(player.getInventory().getItem(45))) {
				sapper -= ItemStatUtils.getEnchantmentLevel(player.getInventory().getItem(45), EnchantmentType.SAPPER);
				adrenaline -= ItemStatUtils.getEnchantmentLevel(player.getInventory().getItem(45), EnchantmentType.ADRENALINE);
			}

			//Damage any mobs in the area
			for (LivingEntity mob : mobs) {
				DamageUtils.damage(player, mob, DamageType.OTHER, DAMAGE_PER_LEVEL * level);
				if (fire > 0) {
					EntityUtils.applyFire(plugin, 80 * fire, mob, player);
				}
				if (ice > 0) {
					EntityUtils.applySlow(plugin, IceAspect.ICE_ASPECT_DURATION, ice * 0.1, mob);
				}
				if (thunder > 0) {
					EntityUtils.applyStun(plugin, 10 * thunder, mob);
				}
				if (decay > 0) {
					Decay.apply(plugin, mob, Decay.DURATION, decay, player);
				}
				if (bleed > 0) {
					EntityUtils.applyBleed(plugin, 100, (int) level, mob);
				}
			}

			//Sapper Interaction
			if (sapper > 0) {
				player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 2.0f, 1.6f);
				player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.05f, 1.0f);
				player.getWorld().spawnParticle(Particle.HEART, event.getBlock().getLocation().add(0, 1, 0), 25, 1.5, 1.5, 1.5);
				for (Player p : PlayerUtils.playersInRange(event.getBlock().getLocation(), SAPPER_RADIUS, true)) {
					p.getWorld().spawnParticle(Particle.HEART, p.getEyeLocation(), 5, 1, 1, 1);
					if (p == player) {
						continue;
					}
					PlayerUtils.healPlayer(plugin, p, sapper, player);
				}
			}

			//Adrenaline Interaction
			if (adrenaline > 0) {
				for (Player p : PlayerUtils.playersInRange(event.getBlock().getLocation(), SAPPER_RADIUS, true)) {
					if (p == player) {
						continue;
					}
					p.getWorld().spawnParticle(Particle.REDSTONE, p.getLocation().add(0, 1, 0), 12, 0.4, 0.5, 0.4, RED_COLOR);
					plugin.mEffectManager.addEffect(p, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(SPEED_DURATION, PERCENT_SPEED_PER_LEVEL * adrenaline * 0.5, PERCENT_SPEED_EFFECT_NAME));
				}
			}

			//Visual feedback
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
			if (bleed > 0 || adrenaline > 0) {
				player.playSound(player.getLocation(), Sound.ENTITY_SLIME_SQUISH, 0.7f, 0.7f);
				player.getWorld().spawnParticle(Particle.REDSTONE, event.getBlock().getLocation(), 25, 1.5, 1.5, 1.5, BLEED_COLOR);
			}
			if (fire > 0 || fire + ice + thunder + decay + bleed + adrenaline == 0) {
				player.playSound(player.getLocation(), Sound.BLOCK_LAVA_POP, 0.6f, 0.9f);
				player.getWorld().spawnParticle(Particle.LAVA, event.getBlock().getLocation(), 25, 1.5, 1.5, 1.5);
			}
		}
	}
}
