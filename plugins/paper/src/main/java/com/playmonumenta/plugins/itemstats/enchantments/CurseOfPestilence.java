package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CurseOfPestilence implements Enchantment {
	private static final double CHANCE = 0.1;
	private final List<Consumer<Player>> mAilments = List.of(
		p -> p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * 6, 0, false, false)),
		p -> p.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 20 * 6, 1, false, false)),
		p -> EffectManager.getInstance().addEffect(p, "Curse of Pestilence - 1", new PercentDamageReceived(20 * 6, 0.15, DamageEvent.DamageType.getAllMeleeProjectileAndMagicTypes())),
		p -> EffectManager.getInstance().addEffect(p, "Curse of Pestilence - 2", new PercentSpeed(20 * 6, -0.15, "Curse of Pestilence slowness")),
		p -> EffectManager.getInstance().addEffect(p, "Curse of Pestilence - 3", new PercentHeal(20 * 6, -0.30))
	);

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.CURSE_OF_PESTILENCE;
	}

	@Override
	public String getName() {
		return "Curse of Pestilence";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND, Slot.HEAD, Slot.CHEST, Slot.LEGS, Slot.FEET, Slot.PROJECTILE);
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE || event.getType() == DamageType.PROJECTILE) {
			double chance = CHANCE * level;
			if (FastUtils.RANDOM.nextDouble() <= chance) {
				FastUtils.getRandomElement(mAilments).accept(player);
			}
		}
	}
}
