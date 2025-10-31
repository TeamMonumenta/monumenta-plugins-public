package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.ItemCooldown;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.itemstats.infusions.Quench;
import com.playmonumenta.plugins.itemstats.infusions.Refresh;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.EnumSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;
import static com.playmonumenta.plugins.utils.PlayerUtils.MAX_FOOD_LEVEL;

public final class JunglesNourishment implements Enchantment {
	private static final double HEAL_PERCENT = 0.4;
	private static final int RESISTANCE_DURATION = TICKS_PER_SECOND * 5;
	private static final double PERCENT_DAMAGE_RECEIVED = -0.2;
	private static final String PERCENT_DAMAGE_RECEIVED_EFFECT_NAME = "JunglesNourishmentResistance";
	private static final int COOLDOWN = TICKS_PER_SECOND * 25;
	public static final Material COOLDOWN_ITEM = Material.MELON_SEEDS;

	public static final String CHARM_COOLDOWN = "Jungle's Nourishment Cooldown";
	public static final String CHARM_HEALTH = "Jungle's Nourishment Health";
	public static final String CHARM_RESISTANCE = "Jungle's Nourishment Resistance";
	public static final String CHARM_DURATION = "Jungle's Nourishment Duration";

	@Override
	public String getName() {
		return "Jungles Nourishment";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.JUNGLES_NOURISHMENT;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND);
	}

	@Override
	public void onConsume(final Plugin plugin, final Player player, final double level, final PlayerItemConsumeEvent event) {
		if (ItemStatUtils.getEnchantmentLevel(event.getItem(), EnchantmentType.JUNGLES_NOURISHMENT) > 0) {
			final ItemStack item = event.getItem();
			final String source = ItemCooldown.toSource(getEnchantmentType());
			if (plugin.mEffectManager.hasEffect(player, source)) {
				player.sendMessage(Component.text("Your " + ItemUtils.getPlainName(item) + " is still on cooldown!", TextColor.fromHexString("#D02E28")));
				event.setCancelled(true);
				return;
			}

			final double healPercent = HEAL_PERCENT + CharmManager.getLevelPercentDecimal(player, CHARM_HEALTH);
			final int resistanceDuration = (int) (CharmManager.getDuration(player, CHARM_DURATION, RESISTANCE_DURATION) * Quench.getDurationScaling(plugin, player));
			final double resistancePotency = PERCENT_DAMAGE_RECEIVED - CharmManager.getLevelPercentDecimal(player, CHARM_RESISTANCE);
			final int cooldown = CharmManager.getCooldown(player, CHARM_COOLDOWN, Refresh.reduceCooldown(plugin, player, COOLDOWN));

			PlayerUtils.healPlayer(plugin, player, healPercent * EntityUtils.getMaxHealth(player), player);
			player.setFoodLevel(MAX_FOOD_LEVEL);
			plugin.mEffectManager.addEffect(player, PERCENT_DAMAGE_RECEIVED_EFFECT_NAME,
				new PercentDamageReceived(resistanceDuration, resistancePotency));
			plugin.mEffectManager.addEffect(player, ItemCooldown.toSource(getEnchantmentType()),
				new ItemCooldown(cooldown, item, COOLDOWN_ITEM, plugin));

			new PartialParticle(Particle.SPELL, player.getLocation().add(0, 1, 0)).count(20).delta(0.25, 0.5, 0.25).extra(1).spawnAsPlayerActive(player);
			new PartialParticle(Particle.SPELL_INSTANT, player.getLocation().add(0, 1, 0)).count(25).delta(0.25, 0.45, 0.25).extra(1).spawnAsPlayerActive(player);
			player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1, 1.25f);
		}
	}
}
