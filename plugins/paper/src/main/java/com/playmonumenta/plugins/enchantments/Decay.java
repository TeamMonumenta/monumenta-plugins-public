package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

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
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;



public class Decay implements BaseEnchantment {
	public static final String PROPERTY_NAME = ChatColor.GRAY + "Decay";
	public static final int DURATION = 20 * 4;
	private static final String LEVEL_METAKEY = "DecayLevelMetakey";
	public static final String DOT_EFFECT_NAME = "DecayDamageOverTimeEffect";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.MAINHAND);
	}

	@Override
	public void onAttack(Plugin plugin, Player player, int level, LivingEntity target, EntityDamageByEntityEvent event) {
		plugin.mEffectManager.addEffect(target, DOT_EFFECT_NAME, new CustomDamageOverTime((int)(DURATION * player.getCooledAttackStrength(0)), 1, 40 / level, player, null, null, Particle.SQUID_INK, plugin));
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
	/*
	 * TODO: This needs some kind of better registration than expecting it to be called directly
	 *
	 * IF YOU COPY THIS YOU MUST PUT A CORRESPONDING CALL IN EntityDamageByEntityEvent !
	 *
	 * This works this way because you might have the enchantment when you fire the arrow, but switch to a different item before it hits
	 */
	public static void onShootAttack(Plugin plugin, Projectile proj, LivingEntity target, EntityDamageByEntityEvent event) {
		if (proj.hasMetadata(LEVEL_METAKEY) && proj instanceof Trident && proj.getShooter() instanceof Player) {
			int level = proj.getMetadata(LEVEL_METAKEY).get(0).asInt();
			Player player = (Player)proj.getShooter();
			plugin.mEffectManager.addEffect(target, DOT_EFFECT_NAME, new CustomDamageOverTime(DURATION, 1, 40 / level, player, null, null, Particle.SQUID_INK, plugin));
			player.getWorld().spawnParticle(Particle.SQUID_INK, target.getLocation().add(0, 1, 0), 4, 0.4, 0.5, 0.4);
		}
	}

}
