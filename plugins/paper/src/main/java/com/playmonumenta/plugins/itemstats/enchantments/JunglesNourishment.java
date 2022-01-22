package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class JunglesNourishment implements Enchantment {

	private static final int HEAL = 8;
	private static final int DURATION = 20 * 5;
	private static final int COOLDOWN = 20 * 25;

	@Override
	public String getName() {
		return "Jungles Nourishment";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.JUNGLES_NOURISHMENT;
	}

	@Override
	public void onConsume(Plugin plugin, Player player, double level, PlayerItemConsumeEvent event) {
		if (InventoryUtils.testForItemWithLore(event.getItem(), "Jungle's Nourishment")) {
			PlayerUtils.healPlayer(plugin, player, HEAL, player);
			plugin.mPotionManager.addPotion(player, PotionID.ITEM,
					new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, DURATION, 0, true, true));
			player.setCooldown(event.getItem().getType(), COOLDOWN);
			player.setFoodLevel(24);
			World world = player.getWorld();
			world.spawnParticle(Particle.SPELL, player.getLocation().add(0, 1, 0), 20, 0.25, 0.5, 0.25, 1);
			world.spawnParticle(Particle.SPELL_INSTANT, player.getLocation().add(0, 1, 0), 25, 0.5, 0.45, 0.25, 1);
			world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1, 1.25f);
		}
	}
}
