package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.PotionUtils;

public class Decay implements BaseEnchantment {
	public static final String PROPERTY_NAME = ChatColor.GRAY + "Decay";
	private static final int DURATION = 20 * 4;
	private static final String LEVEL_METAKEY = "DecayLevelMetakey";

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
		PotionUtils.applyPotion(player, target, new PotionEffect(PotionEffectType.WITHER, (int)(DURATION * player.getCooledAttackStrength(0)), level - 1, false, true));
		player.getWorld().spawnParticle(Particle.SQUID_INK, target.getLocation().add(0, 1, 0), 4, 0.4, 0.5, 0.4);
	}
	
	// Thrown trident damage handling
	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, int level, Projectile proj, ProjectileLaunchEvent event) {
		if (proj instanceof Trident) {
			proj.setMetadata(LEVEL_METAKEY, new FixedMetadataValue(plugin, level));
		}
	}

	//Trident hit effect
	public static void onShootAttack(Plugin plugin, Projectile proj, LivingEntity target, EntityDamageByEntityEvent event) {
		if (proj.hasMetadata(LEVEL_METAKEY) && proj instanceof Trident && proj.getShooter() instanceof Player) {
			int level = proj.getMetadata(LEVEL_METAKEY).get(0).asInt();
			Player player = (Player)proj.getShooter();
			PotionUtils.applyPotion(player, target, new PotionEffect(PotionEffectType.WITHER, DURATION, level - 1, false, true));
			player.getWorld().spawnParticle(Particle.SQUID_INK, target.getLocation().add(0, 1, 0), 4, 0.4, 0.5, 0.4);
		}
	}

}
