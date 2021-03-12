package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.Color;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;

public class Bleeding implements BaseEnchantment {
	private static final int DURATION = 20 * 5;
	public static final String PROPERTY_NAME = ChatColor.GRAY + "Bleeding";
	private static final String LEVEL_METAKEY = "BleedingLevelMetakey";
	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(210, 44, 44), 1.0f);

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
		EntityUtils.applyBleed(plugin, (int)(DURATION * player.getCooledAttackStrength(0)), level, target);
		player.getWorld().spawnParticle(Particle.REDSTONE, target.getLocation().add(0, 1, 0), 8, 0.3, 0.6, 0.3, COLOR);
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
			EntityUtils.applyBleed(plugin, DURATION, level, target);
			player.getWorld().spawnParticle(Particle.REDSTONE, target.getLocation().add(0, 1, 0), 8, 0.3, 0.6, 0.3, COLOR);
		}
	}
}
