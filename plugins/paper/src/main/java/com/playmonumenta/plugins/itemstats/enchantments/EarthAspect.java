package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.EnumSet;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class EarthAspect implements Enchantment {

	private static final String EARTH_STRING = "EarthAspect";
	private static final int DURATION = 3 * 20;
	private static final double DAMAGE_REDUCTION_PER_LEVEL = -0.05;
	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(120, 148, 82), 0.75f);

	@Override
	public @NotNull String getName() {
		return "Earth Aspect";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.EARTH_ASPECT;
	}

	@Override
	public EnumSet<ItemStatUtils.Slot> getSlots() {
		return EnumSet.of(ItemStatUtils.Slot.MAINHAND, ItemStatUtils.Slot.PROJECTILE);
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		DamageEvent.DamageType type = event.getType();
		if ((type == DamageEvent.DamageType.MELEE && ItemStatUtils.isNotExclusivelyRanged(player.getInventory().getItemInMainHand())) || type == DamageEvent.DamageType.PROJECTILE || event.getAbility() == ClassAbility.EXPLOSIVE) {
			apply(plugin, player, level);
		}
	}

	@Override
	public void onBlockBreak(Plugin plugin, Player player, double level, BlockBreakEvent event) {
		ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
		if (ItemUtils.isPickaxe(item) && event.getBlock().getType() == Material.SPAWNER && !ItemUtils.isPickaxe(player.getInventory().getItemInOffHand())) {
			apply(plugin, player, level);
		}
	}

	public static void apply(Plugin plugin, Player player, double level) {
		plugin.mEffectManager.addEffect(player, EARTH_STRING, new PercentDamageReceived(DURATION, DAMAGE_REDUCTION_PER_LEVEL * level));

		double widthDelta = PartialParticle.getWidthDelta(player);
		double widerWidthDelta = widthDelta * 1.5;
		double doubleWidthDelta = widthDelta * 2;
		double heightDelta = PartialParticle.getHeightDelta(player);

		new PartialParticle(
			Particle.FALLING_DUST,
			LocationUtils.getHalfHeightLocation(player),
			10,
			widerWidthDelta,
			heightDelta,
			widerWidthDelta,
			1,
			Material.COARSE_DIRT.createBlockData()
		).spawnAsEnemy();

		new PartialParticle(
			Particle.REDSTONE,
			LocationUtils.getHeightLocation(player, 0.25),
			10,
			doubleWidthDelta,
			heightDelta / 2,
			doubleWidthDelta,
			1,
			COLOR
		).spawnAsEnemy();

		player.getWorld().playSound(
			player.getLocation(),
			Sound.BLOCK_GRAVEL_BREAK,
			SoundCategory.PLAYERS,
			1.0f,
			1.0f
		);
	}

}
