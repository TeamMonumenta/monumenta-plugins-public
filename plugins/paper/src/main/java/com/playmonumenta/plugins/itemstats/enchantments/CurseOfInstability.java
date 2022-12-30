package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import java.util.Random;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class CurseOfInstability implements Enchantment {

	private static final DamageType[] POSSIBLE_DAMAGE_TYPES = new DamageType[]{DamageType.BLAST, DamageType.MELEE, DamageType.PROJECTILE, DamageType.MAGIC};

	@Override
	public @NotNull String getName() {
		return "Curse of Instability";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.CURSE_OF_INSTABILITY;
	}

	@Override
	public double getPriorityAmount() {
		return 1;
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		DamageType prevType = event.getType();
		if (prevType.isDefendable() && !prevType.isEnvironmental() && prevType != DamageType.THORNS) {
			Random random = new Random();
			DamageType newType = POSSIBLE_DAMAGE_TYPES[random.nextInt(POSSIBLE_DAMAGE_TYPES.length)];
			event.setType(newType);
		}
	}

}
