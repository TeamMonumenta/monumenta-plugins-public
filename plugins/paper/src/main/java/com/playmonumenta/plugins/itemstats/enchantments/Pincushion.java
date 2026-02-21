package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.Pincushioned;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.util.BoundingBox;

public class Pincushion implements Enchantment {
	public static final int RADIUS = 2;
	public static final double DAMAGE_PERCENTAGE_PER_LEVEL = 0.1;

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.PINCUSHION;
	}

	@Override
	public String getName() {
		return "Pincushion";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.PROJECTILE, Slot.HEAD, Slot.CHEST, Slot.LEGS, Slot.FEET, Slot.OFFHAND);
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (event.getType() != DamageEvent.DamageType.PROJECTILE
			|| !(event.getDamager() instanceof Projectile projectile)
			|| !EntityUtils.isAbilityTriggeringProjectile(projectile, true)) {
			return;
		}

		Pincushioned pincushionedEffect = plugin.mEffectManager.getActiveEffect(enemy, Pincushioned.class);
		if (pincushionedEffect == null) {
			plugin.mEffectManager.addEffect(enemy, Pincushioned.PINCUSHIONED_EFFECT_NAME, new Pincushioned());
		} else {
			if (pincushionedEffect.incrementStacks()) {
				pincushionedEffect.clearStacks();
				ItemStatManager.PlayerItemStats playerItemStats = DamageListener.getProjectileItemStats(projectile);
				if (playerItemStats == null) {
					return;
				}

				ItemStatManager.PlayerItemStats.ItemStatsMap itemStatsMap = playerItemStats.getItemStats();

				// Get enchant levels on weapon
				int punch = (int) itemStatsMap.get(EnchantmentType.PUNCH);

				Location location = LocationUtils.getEntityCenter(enemy);

				// Using bounding box because otherwise it is a bit inconsistent.
				List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(location, RADIUS * 2);
				BoundingBox box = BoundingBox.of(location, RADIUS, RADIUS, RADIUS);

				double damage = event.getFlatDamage() * value * DAMAGE_PERCENTAGE_PER_LEVEL;

				Location enemyLoc = enemy.getEyeLocation();
				for (LivingEntity mob : nearbyMobs) {
					BoundingBox mobBox = mob.getBoundingBox();
					if (box.overlaps(mobBox)) {
						// Deal damage.
						DamageUtils.damage(player, mob, DamageEvent.DamageType.PROJECTILE_ENCH, damage, ClassAbility.PINCUSHION, false);
						Location mobLoc = mob.getEyeLocation();
						Punch.applyPunch(plugin, punch, mob, LocationUtils.getVectorTo(mobLoc, enemyLoc));
						new PPLine(Particle.CRIT, enemyLoc, mobLoc).countPerMeter(3).spawnAsPlayerPassive(player);
					}
				}
				player.getWorld().playSound(enemyLoc, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.5f);
			}
		}
	}
}
