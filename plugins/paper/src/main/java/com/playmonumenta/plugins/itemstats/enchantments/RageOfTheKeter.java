package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Aesthetics;
import com.playmonumenta.plugins.effects.ItemCooldown;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.itemstats.infusions.Quench;
import com.playmonumenta.plugins.itemstats.infusions.Refresh;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.EnumSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

public final class RageOfTheKeter implements Enchantment {
	private static final double DAMAGE_PERCENT = 0.20;
	private static final double SPEED_PERCENT = 0.15;
	private static final int DURATION = TICKS_PER_SECOND * 15;
	private static final int COOLDOWN = TICKS_PER_SECOND * 25;
	private static final String ATTR_NAME = "KeterExtraSpeedAttr";
	private static final int FOOD_RESTORED = 4;
	private static final int FOOD_SATURATION_RESTORED = 4;
	public static final Material COOLDOWN_ITEM = Material.POPPED_CHORUS_FRUIT;
	private static final EnumSet<DamageType> AFFECTED_DAMAGE_TYPES = DamageType.getAllMeleeProjectileAndMagicTypes();

	public static final String CHARM_COOLDOWN = "Rage of the Keter Cooldown";
	public static final String CHARM_DAMAGE = "Rage of the Keter Damage";
	public static final String CHARM_SPEED = "Rage of the Keter Speed";
	public static final String CHARM_DURATION = "Rage of the Keter Duration";

	private static final Particle.DustOptions OLIVE_COLOR = new Particle.DustOptions(Color.fromRGB(128, 128, 0), 1.0f);
	private static final Particle.DustOptions GREEN_COLOR = new Particle.DustOptions(Color.fromRGB(64, 128, 0), 1.0f);

	@Override
	public String getName() {
		return "Rage of the Keter";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND);
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.RAGE_OF_THE_KETER;
	}

	@Override
	public void onConsume(final Plugin plugin, final Player player, final double level, final PlayerItemConsumeEvent event) {
		if (ItemStatUtils.getEnchantmentLevel(event.getItem(), EnchantmentType.RAGE_OF_THE_KETER) > 0) {
			final ItemStack item = event.getItem();
			final String source = ItemCooldown.toSource(getEnchantmentType());
			if (plugin.mEffectManager.hasEffect(player, source)) {
				player.sendMessage(Component.text("Your " + ItemUtils.getPlainName(item) + " is still on cooldown!", TextColor.fromHexString("#D02E28")));
				event.setCancelled(true);
				return;
			}

			event.setCancelled(true);
			final int duration = (int) (CharmManager.getDuration(player, CHARM_DURATION, DURATION) * Quench.getDurationScaling(plugin, player));
			final int cooldown = CharmManager.getCooldown(player, CHARM_COOLDOWN, Refresh.reduceCooldown(plugin, player, COOLDOWN));

			PlayerUtils.addFoodLevel(player, FOOD_RESTORED);
			PlayerUtils.addFoodSaturationLevel(player, FOOD_SATURATION_RESTORED);
			plugin.mEffectManager.addEffect(player, "KeterExtraDamage",
				new PercentDamageDealt(duration, DAMAGE_PERCENT + CharmManager.getLevelPercentDecimal(player, CHARM_DAMAGE))
					.damageTypes(AFFECTED_DAMAGE_TYPES));
			plugin.mEffectManager.addEffect(player, "KeterExtraSpeed", new PercentSpeed(duration, SPEED_PERCENT + CharmManager.getLevelPercentDecimal(player, CHARM_SPEED), ATTR_NAME));
			plugin.mEffectManager.addEffect(player, ItemCooldown.toSource(getEnchantmentType()),
				new ItemCooldown(cooldown, item, COOLDOWN_ITEM, plugin));

			new PartialParticle(Particle.REDSTONE, player.getLocation().add(0, 1, 0)).count(20).delta(0.25, 0.5, 0.25).extra(1).data(OLIVE_COLOR).spawnAsPlayerBuff(player);
			new PartialParticle(Particle.REDSTONE, player.getLocation().add(0, 1, 0)).count(25).delta(0.25, 0.45, 0.25).extra(1).data(GREEN_COLOR).spawnAsPlayerBuff(player);
			player.playSound(player.getLocation(), Sound.ENTITY_STRIDER_EAT, SoundCategory.PLAYERS, 1, 0.85f);

			plugin.mEffectManager.addEffect(player, "KeterParticles", new Aesthetics(duration,
					// Tick effect
					(entity, fourHertz, twoHertz, oneHertz) -> {
						final Location loc = player.getLocation().add(0, 1, 0);
						new PartialParticle(Particle.REDSTONE, loc).count(2).delta(0.25).extra(0.1).data(OLIVE_COLOR).spawnAsPlayerBuff(player);
						new PartialParticle(Particle.REDSTONE, loc).count(2).delta(0.5).data(GREEN_COLOR).spawnAsPlayerBuff(player);
						new PartialParticle(Particle.REDSTONE, loc).count(2).delta(0.5).extra(0.1).data(OLIVE_COLOR).spawnAsPlayerBuff(player);
					},
					// Lose effect
					(entity) -> {
						final Location loc = player.getLocation().add(0, 1, 0);
						player.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_DEATH, SoundCategory.PLAYERS, 1f, 0.65f);
						new PartialParticle(Particle.REDSTONE, loc).count(2).delta(0.25).extra(0.1).data(OLIVE_COLOR).spawnAsPlayerBuff(player);
						new PartialParticle(Particle.REDSTONE, loc).count(2).delta(0.5).data(GREEN_COLOR).spawnAsPlayerBuff(player);
						new PartialParticle(Particle.REDSTONE, loc).count(2).delta(0.5).extra(0.1).data(OLIVE_COLOR).spawnAsPlayerBuff(player);
					}
				)
			);
		}
	}
}
