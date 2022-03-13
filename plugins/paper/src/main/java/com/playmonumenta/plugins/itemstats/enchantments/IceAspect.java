package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import java.util.EnumSet;
import org.bukkit.Particle;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;

public class IceAspect implements Enchantment {
	public static final int ICE_ASPECT_DURATION = 20 * 5;
	public static final String LEVEL_METAKEY = "IceAspectLevelMetakey";

	@Override
	public String getName() {
		return "Ice Aspect";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public double getPriorityAmount() {
		return 12;
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.ICE_ASPECT;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE || event.getDamager() instanceof Trident) {
			EntityUtils.applySlow(plugin, (int)(ICE_ASPECT_DURATION * player.getCooledAttackStrength(0)), level * 0.1, enemy);
			player.getWorld().spawnParticle(Particle.SNOWBALL, enemy.getLocation().add(0, 1, 0), 8, 0.5, 0.5, 0.5, 0.001);

			if (enemy instanceof Blaze) {
				event.setDamage(event.getDamage() + 1.0);
			}
		}
	}
}
