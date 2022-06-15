package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Aesthetics;
import com.playmonumenta.plugins.effects.ItemCooldown;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.EnumSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

public class RageOfTheKeter implements Enchantment {

	private static final double DAMAGE_PERCENT = 0.15;
	private static final double SPEED_PERCENT = 0.15;
	private static final int DURATION = 20 * 15;
	private static final int COOLDOWN = 20 * 25;
	private static final String ATTR_NAME = "KeterExtraSpeedAttr";
	public static final String CHARM_COOLDOWN = "Rage of the Keter Cooldown";
	public static final String CHARM_DAMAGE = "Rage of the Keter Damage";
	public static final String CHARM_SPEED = "Rage of the Keter Speed";
	public static final Material COOLDOWN_ITEM = Material.POPPED_CHORUS_FRUIT;
	private static final EnumSet<DamageType> AFFECTED_DAMAGE_TYPES = EnumSet.of(
		DamageType.MELEE,
		DamageType.MELEE_ENCH,
		DamageType.MELEE_SKILL,
		DamageType.PROJECTILE,
		DamageType.PROJECTILE_SKILL,
		DamageType.MAGIC
	);

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
	public void onConsume(Plugin plugin, Player player, double level, PlayerItemConsumeEvent event) {
		if (ItemStatUtils.getEnchantmentLevel(event.getItem(), EnchantmentType.RAGE_OF_THE_KETER) > 0) {
			ItemStack item = event.getItem();
			String source = ItemCooldown.toSource(getEnchantmentType());
			if (plugin.mEffectManager.hasEffect(player, source)) {
				player.sendMessage(Component.text("Your " + ItemUtils.getPlainName(item) + " is still on cooldown!", TextColor.fromHexString("#D02E28")));
				event.setCancelled(true);
				return;
			}

			event.setCancelled(true);
			World world = player.getWorld();
			plugin.mEffectManager.addEffect(player, "KeterExtraDamage", new PercentDamageDealt(DURATION, DAMAGE_PERCENT + CharmManager.getLevelPercent(player, CHARM_DAMAGE), AFFECTED_DAMAGE_TYPES));
			plugin.mEffectManager.addEffect(player, "KeterExtraSpeed", new PercentSpeed(DURATION, SPEED_PERCENT + CharmManager.getLevelPercent(player, CHARM_SPEED), ATTR_NAME));
			plugin.mEffectManager.addEffect(player, "KeterParticles", new Aesthetics(DURATION,
					(entity, fourHertz, twoHertz, oneHertz) -> {
						// Tick effect
						Location loc = player.getLocation().add(0, 1, 0);
						new PartialParticle(Particle.REDSTONE, loc, 2, 0.25, 0.25, 0.25, 0.1, GREEN_COLOR).spawnAsPlayerBuff(player);
						new PartialParticle(Particle.REDSTONE, loc, 2, 0.5, 0.5, 0.5, 0, GREEN_COLOR).spawnAsPlayerBuff(player);
						new PartialParticle(Particle.REDSTONE, loc, 2, 0.5, 0.5, 0.5, 0.1, OLIVE_COLOR).spawnAsPlayerBuff(player);
					}, (entity) -> {
					// Lose effect
					Location loc = player.getLocation();
					world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_DEATH, 1f, 0.65f);
					new PartialParticle(Particle.REDSTONE, loc, 2, 0.25, 0.25, 0.25, 0.1, OLIVE_COLOR).spawnAsPlayerBuff(player);
					new PartialParticle(Particle.REDSTONE, loc, 2, 0.5, 0.5, 0.5, 0, GREEN_COLOR).spawnAsPlayerBuff(player);
					new PartialParticle(Particle.REDSTONE, loc, 2, 0.5, 0.5, 0.5, 0.1, OLIVE_COLOR).spawnAsPlayerBuff(player);
				})
			);

			player.setFoodLevel(Math.min(20, player.getFoodLevel() + 6));
			player.setSaturation(Math.min(player.getFoodLevel(), Math.min(player.getSaturation() + 6, 20)));

			new PartialParticle(Particle.REDSTONE, player.getLocation().add(0, 1, 0), 20, 0.25, 0.5, 0.25, 1, OLIVE_COLOR).spawnAsPlayerBuff(player);
			new PartialParticle(Particle.REDSTONE, player.getLocation().add(0, 1, 0), 25, 0.5, 0.45, 0.25, 1, GREEN_COLOR).spawnAsPlayerBuff(player);
			world.playSound(player.getLocation(), Sound.ENTITY_STRIDER_EAT, 1, 0.85f);
			int cooldown = CharmManager.getCooldown(player, CHARM_COOLDOWN, COOLDOWN);
			player.setCooldown(COOLDOWN_ITEM, cooldown);
			plugin.mEffectManager.addEffect(player, ItemCooldown.toSource(getEnchantmentType()), new ItemCooldown(cooldown, item, plugin));
		}
	}
}
