package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class Rebound implements Enchantment {
	public static final float KB_VEL_PER_LEVEL = 0.3f;
	public static final float VERTICAL_LAUNCH = 0.2f;
	public static final int RADIUS = 3;

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.REBOUND;
	}

	@Override
	public String getName() {
		return "Rebound";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND);
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.isBlockedByShield() && source != null) {
			if (player.getCooldown(Material.SHIELD) > 0) {
				float speed = KB_VEL_PER_LEVEL * (float) value;

				List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(source.getLocation(), RADIUS * 2);
				BoundingBox box = BoundingBox.of(source.getLocation(), RADIUS, RADIUS, RADIUS);

				for (LivingEntity enemy : nearbyMobs) {
					BoundingBox enemyBox = enemy.getBoundingBox();
					if (box.overlaps(enemyBox)) {
						Vector vector = enemy.getLocation().toVector().subtract(player.getLocation().toVector());
						vector.setY(0);
						if (vector.length() < 0.001) {
							vector = new Vector(0, VERTICAL_LAUNCH, 0);
						} else {
							vector.normalize()
								.multiply(speed)
								.setY(VERTICAL_LAUNCH);
						}
						MovementUtils.knockAwayDirection(vector, enemy, 0.5f);
					}
				}
			}
		}
	}
}
