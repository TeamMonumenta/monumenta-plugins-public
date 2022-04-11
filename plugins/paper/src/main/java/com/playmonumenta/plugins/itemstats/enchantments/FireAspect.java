package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import java.util.EnumSet;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public class FireAspect implements Enchantment {
	private static final int FIRE_ASPECT_DURATION = 20 * 4;

	@Override
	public String getName() {
		return "Fire Aspect";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public double getPriorityAmount() {
		return 10;
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.FIRE_ASPECT;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		DamageType type = event.getType();
		if ((type == DamageType.MELEE && ItemStatUtils.hasMeleeDamage(player.getInventory().getItemInMainHand())) || type == DamageType.PROJECTILE) {
			int duration = (int) (FIRE_ASPECT_DURATION * value * (type == DamageType.MELEE ? player.getCooledAttackStrength(0) : 1));
			ItemStatManager.PlayerItemStats playerItemStats;
			if (event.getDamager() instanceof Projectile proj) {
				playerItemStats = DamageListener.getProjectileItemStats(proj);
			} else {
				playerItemStats = plugin.mItemStatManager.getPlayerItemStats(player);
			}
			EntityUtils.applyFire(plugin, duration, enemy, player, playerItemStats);
			// So that fire resistant mobs don't get fire particles
			if (enemy.getFireTicks() > 0) {
				player.getWorld().spawnParticle(Particle.FLAME, enemy.getLocation().add(0, 1, 0), 6, 0.5, 0.5, 0.5, 0.001);
			}
		}
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, double value, ProjectileLaunchEvent event, Projectile projectile) {
		if (projectile instanceof Arrow || projectile instanceof SpectralArrow) {
			// Set the arrow on fire like vanilla flame so it activates tnt
			// This fire is overwritten by the fire from the enchant, which is equal but applies inferno, etc. properly
			projectile.setFireTicks((int) (FIRE_ASPECT_DURATION * value));
		}
	}
}
