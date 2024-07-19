package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.EnumSet;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.jetbrains.annotations.Nullable;

public class FireAspect implements Enchantment {
	public static final int FIRE_ASPECT_DURATION = 20 * 4;

	@Override
	public String getName() {
		return "Fire Aspect";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.PROJECTILE);
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
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		if (AbilityUtils.isAspectTriggeringEvent(event, player)) {
			DamageType type = event.getType();
			int duration = (int) (FIRE_ASPECT_DURATION * level * (type == DamageType.MELEE ? player.getCooledAttackStrength(0) : 1));
			ItemStatManager.PlayerItemStats playerItemStats;
			if (event.getDamager() instanceof Projectile proj) {
				playerItemStats = DamageListener.getProjectileItemStats(proj);
			} else {
				playerItemStats = plugin.mItemStatManager.getPlayerItemStats(player);
			}
			apply(plugin, player, playerItemStats, duration, enemy, type);
		}
	}

	public static void apply(Plugin plugin, Player player, @Nullable ItemStatManager.PlayerItemStats playerItemStats, int duration, LivingEntity enemy, DamageType type) {
		EntityUtils.applyFire(plugin, duration, enemy, player, playerItemStats);
		// So that fire resistant mobs don't get fire particles
		if (enemy.getFireTicks() > 0) {
			new PartialParticle(Particle.FLAME, enemy.getLocation().add(0, 1, 0), 6, 0.5, 0.5, 0.5, 0.001).spawnAsPlayerActive(player);
			if (type == DamageType.MELEE) {
				World world = enemy.getWorld();
				Location loc = enemy.getLocation();
				world.playSound(loc, Sound.BLOCK_CANDLE_EXTINGUISH, SoundCategory.PLAYERS, 2.0f, 0.9f);
				world.playSound(loc, Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 0.4f, 1.2f);
			}
		}
	}

	@Override
	public void onProjectileLaunch(Plugin plugin, Player player, double value, ProjectileLaunchEvent event, Projectile projectile) {
		if (EntityUtils.isAbilityTriggeringProjectile(projectile, false)) {
			if (projectile instanceof Arrow || projectile instanceof SpectralArrow) {
				// Set the arrow on fire like vanilla flame so it activates tnt
				// This fire is overwritten by the fire from the enchant, which is equal but applies inferno, etc. properly
				projectile.setFireTicks((int) (FIRE_ASPECT_DURATION * value));
			}
			World world = player.getWorld();
			Location loc = player.getLocation();
			world.playSound(loc, Sound.BLOCK_CANDLE_EXTINGUISH, SoundCategory.PLAYERS, 5.0f, 0.9f);
			world.playSound(loc, Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 0.4f, 0.9f);
			world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 0.3f, 1.1f);
		}
	}
}
