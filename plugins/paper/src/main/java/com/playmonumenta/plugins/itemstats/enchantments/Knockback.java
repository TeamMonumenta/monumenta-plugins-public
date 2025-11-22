package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.EnumSet;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class Knockback implements Enchantment {
	public static final float KB_VEL_PER_LEVEL = 0.5f;
	public static final float VERTICAL_LAUNCH = 0.2f;

	@Override
	public String getName() {
		return "Knockback";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.KNOCKBACK;
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
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		// TODO: Override the Minecraft default attack knockback (which is 0.4f), then we can add 0.2f to speed and remove the need to fire the KB event 1 tick late
		if (event.getType() == DamageEvent.DamageType.MELEE) {
			float speed = KB_VEL_PER_LEVEL * (float) level;
			Vector vector = enemy.getLocation().clone().toVector().subtract(player.getLocation().toVector());
			vector.setY(0);
			if (vector.length() < 0.001) {
				vector = new Vector(0, VERTICAL_LAUNCH, 0);
			} else {
				vector.normalize()
					.multiply(speed)
					.setY(VERTICAL_LAUNCH);
			}
			// Sorry. Java requires that I input a "final" into the lambda expression.
			final Vector dir = vector.clone();
			Bukkit.getScheduler().runTask(plugin, () -> MovementUtils.knockAwayDirection(dir, enemy, 0.5f, true, false));
		}
	}
}
