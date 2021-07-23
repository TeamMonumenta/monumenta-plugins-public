package com.playmonumenta.plugins.enchantments.infusions.delves;

import java.util.EnumSet;
import java.util.NavigableSet;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentDamageDealt;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;

public class Execution implements BaseEnchantment {

	public static final String PROPERTY_NAME = ChatColor.GRAY + "Execution";
	private static final int DURATION = 4 * 20;
	private static final double PERCENT_DAMAGE_PER_LEVEL = 0.015;
	private static final String PERCENT_DAMAGE_EFFECT_NAME = "ExecutionPercentDamageEffect";
	private static final EnumSet<DamageCause> AFFECTED_DAMAGE_CAUSES = EnumSet.of(
			DamageCause.ENTITY_ATTACK,
			DamageCause.ENTITY_SWEEP_ATTACK,
			DamageCause.CUSTOM,
			DamageCause.PROJECTILE
	);

	@Override
	public @NotNull String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND, ItemSlot.MAINHAND);
	}

	@Override
	public void onKill(@NotNull Plugin plugin, @NotNull Player player, int level, @NotNull Entity enemy, EntityDeathEvent entityDeathEvent) {
		BlockData fallingDustData = Material.ANVIL.createBlockData();
		World world = player.getWorld();
		world.spawnParticle(Particle.FALLING_DUST, enemy.getLocation().add(0, enemy.getHeight() / 2, 0), 3,
				(enemy.getWidth() / 2) + 0.1, enemy.getHeight() / 3, (enemy.getWidth() / 2) + 0.1, fallingDustData);
		NavigableSet<Effect> damageEffects = plugin.mEffectManager.getEffects(player, PERCENT_DAMAGE_EFFECT_NAME);
		if (damageEffects != null) {
			for (Effect effect : damageEffects) {
				if (effect.getMagnitude() == PERCENT_DAMAGE_PER_LEVEL * DelveInfusionUtils.getModifiedLevel(plugin, player, level)) {
					effect.setDuration(DURATION);
				} else {
					effect.setDuration(1);
					plugin.mEffectManager.addEffect(player, PERCENT_DAMAGE_EFFECT_NAME, new PercentDamageDealt(DURATION, PERCENT_DAMAGE_PER_LEVEL * DelveInfusionUtils.getModifiedLevel(plugin, player, level), AFFECTED_DAMAGE_CAUSES));
				}
			}
		} else {
			plugin.mEffectManager.addEffect(player, PERCENT_DAMAGE_EFFECT_NAME, new PercentDamageDealt(DURATION, PERCENT_DAMAGE_PER_LEVEL * DelveInfusionUtils.getModifiedLevel(plugin, player, level), AFFECTED_DAMAGE_CAUSES));
		}
	}

}
