package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.SpawnerUtils;
import java.util.EnumSet;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

public class Adrenaline implements Enchantment {

	public static final String PERCENT_SPEED_EFFECT_NAME = "AdrenalinePercentSpeedEffect";
	public static final int DURATION = 20 * 3;
	public static final int SPAWNER_DURATION = 20 * 6;
	public static final double PERCENT_SPEED_PER_LEVEL = 0.1;
	public static final double PROJECTILE_REDUCTION = 0.5;

	private static final Particle.DustOptions RED_COLOR = new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.0f);

	@Override
	public String getName() {
		return "Adrenaline";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND, Slot.HEAD, Slot.CHEST, Slot.LEGS, Slot.FEET, Slot.PROJECTILE);
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.ADRENALINE;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		DamageType type = event.getType();
		if (type == DamageType.MELEE || type == DamageType.PROJECTILE) {
			new PartialParticle(Particle.REDSTONE, player.getLocation().add(0, 1, 0), 12, 0.4, 0.5, 0.4, RED_COLOR).spawnAsPlayerBuff(player);
			double speedAmount = PERCENT_SPEED_PER_LEVEL * value * (type == DamageType.PROJECTILE ? PROJECTILE_REDUCTION : 1);
			plugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(DURATION, speedAmount, PERCENT_SPEED_EFFECT_NAME));
		}
	}

	@Override
	public void onBlockBreak(Plugin plugin, Player player, double value, BlockBreakEvent event) {
		if (ItemUtils.isPickaxe(player.getInventory().getItemInMainHand()) && event.getBlock().getType() == Material.SPAWNER) {
			if (!SpawnerUtils.tryBreakSpawner(event.getBlock(), 1 + Plugin.getInstance().mItemStatManager.getEnchantmentLevel(event.getPlayer(), EnchantmentType.DRILLING), false)) {
				return;
			}
			new PartialParticle(Particle.REDSTONE, player.getLocation().add(0, 1, 0), 12, 0.4, 0.5, 0.4, RED_COLOR).spawnAsPlayerBuff(player);
			double speedAmount = PERCENT_SPEED_PER_LEVEL * value * 0.5;
			plugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(SPAWNER_DURATION, speedAmount, PERCENT_SPEED_EFFECT_NAME));
		}
	}
}
