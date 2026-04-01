package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import java.util.EnumSet;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class Combustion implements Enchantment {

	private static final int MOB_SPREAD_PER_LEVEL = 1;
	private static final int DURATION_INCREASE_PER_LEVEL = 20;
	private static final int SPREAD_RADIUS = 2;
	private static final int THRESHOLD = 20;
	private static final String DAMAGED_THIS_TICK_METADATA = "CombustionThisTick";
	private static final String DAMAGE_DEALT_METADATA = "CombustionDamageDealt";

	private static final EnumSet<DamageEvent.DamageType> ACTIVATION_DAMAGE_TYPES = EnumSet.of(
		DamageEvent.DamageType.MELEE,
		DamageEvent.DamageType.MELEE_ENCH,
		DamageEvent.DamageType.FIRE,
		DamageEvent.DamageType.MELEE_SKILL,
		DamageEvent.DamageType.PROJECTILE
	);

	@Override
	public String getName() {
		return "Combustion";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.COMBUSTION;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND, Slot.HEAD, Slot.CHEST, Slot.LEGS, Slot.FEET, Slot.PROJECTILE);
	}


	@Override
	public void onKill(Plugin plugin, Player player, double level, EntityDeathEvent event, LivingEntity enemy) {
		if ((enemy.getFireTicks() >= THRESHOLD) || Inferno.hasInferno(plugin, enemy)) {
			EntityDamageEvent e = enemy.getLastDamageCause();
			if (e != null && (MetadataUtils.happenedThisTick(enemy, DAMAGED_THIS_TICK_METADATA) && enemy.hasMetadata(DAMAGE_DEALT_METADATA))) {
				int applications = 0;
				for (LivingEntity entity : EntityUtils.getNearbyMobs(event.getEntity().getLocation(), SPREAD_RADIUS)) {
					if (applications < level * MOB_SPREAD_PER_LEVEL) {
						EntityUtils.applyFire(plugin, (int) (enemy.getFireTicks() + DURATION_INCREASE_PER_LEVEL * level), entity, player, true);

						entity.getWorld().playSound(entity.getLocation(), Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 0.8f, 0.8f);
						entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 0.5f, 0.8f);
						applications++;
					}
				}
			}
		}
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		if (ACTIVATION_DAMAGE_TYPES.contains(event.getType())) {
			//Store the highest damage dealt with a combustion weapon this tick
			double damage = event.getDamage();
			if (MetadataUtils.checkOnceThisTick(plugin, enemy, DAMAGED_THIS_TICK_METADATA) && enemy.hasMetadata(DAMAGE_DEALT_METADATA) && enemy.getMetadata(DAMAGE_DEALT_METADATA).getFirst().asDouble() > damage) {
				return;
			}
			enemy.setMetadata(DAMAGE_DEALT_METADATA, new FixedMetadataValue(plugin, damage));
		}
	}
}

