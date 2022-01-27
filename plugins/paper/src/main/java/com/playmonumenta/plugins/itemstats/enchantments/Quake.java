package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.EnumSet;
import java.util.List;



public class Quake implements Enchantment {

	private static final float RADIUS = 3.0f;
	private static final float DAMAGE_MODIFIER_PER_LEVEL = 0.1f;
	private static final Particle.DustOptions YELLOW_1_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 20), 1.0f);
	private static final Particle.DustOptions YELLOW_2_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 120), 1.0f);
	private static final Particle.DustOptions BLEED_COLOR = new Particle.DustOptions(Color.fromRGB(210, 44, 44), 1.0f);


	@Override
	public String getName() {
		return "Quake";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public double getPriorityAmount() {
		return 31;
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.QUAKE;
	}

	@Override
	public void onKill(Plugin plugin, Player player, double level, EntityDeathEvent event, LivingEntity target) {
		EntityDamageEvent e = target.getLastDamageCause();
		if (e != null && (e.getCause() == DamageCause.ENTITY_ATTACK)) {
			List<LivingEntity> mobs = EntityUtils.getNearbyMobs(target.getLocation(), RADIUS);

			//Get enchant levels on weapon
			int fire = (int) plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.FIRE_ASPECT);
			int ice = (int) plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.ICE_ASPECT);
			int thunder = (int) plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.THUNDER_ASPECT);
			int decay = (int) plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.DECAY);
			int bleed = (int) plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.BLEEDING);
			/*
			*Damage any mobs in the area
			*Need to cast it because the methods only take integers
			*/
			for (LivingEntity mob : mobs) {
				DamageUtils.damage(player, mob, DamageType.OTHER, e.getDamage() * DAMAGE_MODIFIER_PER_LEVEL * level);
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
					EntityUtils.applyBleed(plugin, 100, bleed, mob);
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
