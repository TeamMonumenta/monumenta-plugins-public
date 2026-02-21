package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.EnumSet;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class Parry implements Enchantment {
	public static int STUN_DURATION_PER_LEVEL = Constants.TICKS_PER_SECOND;
	public static double VULN_PER_LEVEL = 0.05;
	public static int VULN_DURATION = 5 * Constants.TICKS_PER_SECOND;

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.isBlockedByShield() && source != null) {
			new BukkitRunnable() {
				@Override
				public void run() {
					if (player.getCooldown(Material.SHIELD) > 0) {
						EntityUtils.applyStun(plugin, (int) (STUN_DURATION_PER_LEVEL * value), source);
						EntityUtils.applySelfishVulnerability(plugin, VULN_DURATION, VULN_PER_LEVEL * value, source, player);
					}
				}
			}.runTaskLater(plugin, 1);
		}
	}

	@Override
	public String getName() {
		return "Parry";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.PARRY;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND);
	}
}
