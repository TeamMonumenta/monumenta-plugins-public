package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;

import java.util.EnumSet;

public class JunglesNourishment implements Enchantment {

	private static final int HEAL = 8;
	private static final int DURATION = 20 * 5;
	private static final double PERCENT_DAMAGE_RECEIVED = -0.2;
	private static final String PERCENT_DAMAGE_RECEIVED_EFFECT_NAME = "JunglesNourishmentResistance";
	private static final int COOLDOWN = 20 * 25;

	@Override
	public String getName() {
		return "Jungles Nourishment";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.JUNGLES_NOURISHMENT;
	}

	@Override public EnumSet<ItemStatUtils.Slot> getSlots() {
		return EnumSet.of(ItemStatUtils.Slot.MAINHAND, ItemStatUtils.Slot.OFFHAND);
	}

	@Override
	public void onConsume(Plugin plugin, Player player, double level, PlayerItemConsumeEvent event) {
		if (ItemStatUtils.getEnchantmentLevel(event.getItem(), EnchantmentType.JUNGLES_NOURISHMENT) > 0) {
			PlayerUtils.healPlayer(plugin, player, HEAL, player);
			plugin.mEffectManager.addEffect(player, PERCENT_DAMAGE_RECEIVED_EFFECT_NAME, new PercentDamageReceived(DURATION, PERCENT_DAMAGE_RECEIVED));
			player.setCooldown(event.getItem().getType(), COOLDOWN);
			player.setFoodLevel(24);
			World world = player.getWorld();
			world.spawnParticle(Particle.SPELL, player.getLocation().add(0, 1, 0), 20, 0.25, 0.5, 0.25, 1);
			world.spawnParticle(Particle.SPELL_INSTANT, player.getLocation().add(0, 1, 0), 25, 0.5, 0.45, 0.25, 1);
			world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1, 1.25f);
		}
	}
}
