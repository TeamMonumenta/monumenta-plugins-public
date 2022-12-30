package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemUtils;
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
	public static final String CHARM_SPEED = "Adrenaline Speed";
	public static final String CHARM_DURATION = "Adrenaline Duration";

	private static final Particle.DustOptions RED_COLOR = new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.0f);

	@Override
	public String getName() {
		return "Adrenaline";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.ADRENALINE;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE) {
			new PartialParticle(Particle.REDSTONE, player.getLocation().add(0, 1, 0), 12, 0.4, 0.5, 0.4, RED_COLOR).spawnAsPlayerBuff(player);
			double speedAmount = CharmManager.calculateFlatAndPercentValue(player, CHARM_SPEED, PERCENT_SPEED_PER_LEVEL * value);
			int duration = (int) CharmManager.calculateFlatAndPercentValue(player, CHARM_DURATION, DURATION);
			plugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(duration, speedAmount, PERCENT_SPEED_EFFECT_NAME));
		}
	}

	@Override
	public void onBlockBreak(Plugin plugin, Player player, double value, BlockBreakEvent event) {
		if (ItemUtils.isPickaxe(player.getItemInHand()) && event.getBlock().getType() == Material.SPAWNER) {
			new PartialParticle(Particle.REDSTONE, player.getLocation().add(0, 1, 0), 12, 0.4, 0.5, 0.4, RED_COLOR).spawnAsPlayerBuff(player);
			double speedAmount = CharmManager.calculateFlatAndPercentValue(player, CHARM_SPEED, PERCENT_SPEED_PER_LEVEL * value * 0.5);
			int duration = (int) CharmManager.calculateFlatAndPercentValue(player, CHARM_DURATION, SPAWNER_DURATION);
			plugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(duration, speedAmount, PERCENT_SPEED_EFFECT_NAME));
		}
	}
}
