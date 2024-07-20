package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.EnumSet;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public class Bleeding implements Enchantment {

	public static final int DURATION = 20 * 5;
	public static final double AMOUNT_PER_LEVEL = 0.1;
	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(210, 44, 44), 1.0f);

	@Override
	public String getName() {
		return "Bleeding";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.BLEEDING;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.PROJECTILE);
	}

	@Override
	public double getPriorityAmount() {
		return 17;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		if (AbilityUtils.isAspectTriggeringEvent(event, player)) {
			DamageType type = event.getType();
			int duration = (int) (DURATION * (type == DamageType.MELEE ? player.getCooledAttackStrength(0) : 1));
			apply(plugin, player, level, duration, enemy, type);
		}
	}

	public static void apply(Plugin plugin, Player player, double level, int duration, LivingEntity enemy, DamageType type) {
		EntityUtils.applyBleed(plugin, duration, level * AMOUNT_PER_LEVEL, enemy);
		new PartialParticle(Particle.REDSTONE, enemy.getLocation().add(0, 1, 0), 8, 0.3, 0.6, 0.3, COLOR).spawnAsPlayerBuff(player);
		if (type == DamageType.MELEE) {
			World world = enemy.getWorld();
			Location loc = enemy.getLocation();
			world.playSound(loc, Sound.ENTITY_SQUID_HURT, SoundCategory.PLAYERS, 0.25f, 0.4f);
			world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.PLAYERS, 0.25f, 0.9f);
		}
	}

	@Override
	public void onProjectileLaunch(Plugin plugin, Player player, double value, ProjectileLaunchEvent event, Projectile projectile) {
		if (EntityUtils.isAbilityTriggeringProjectile(projectile, false) && !AbilityUtils.isVolley(player, projectile)) {
			World world = player.getWorld();
			Location loc = player.getLocation();
			world.playSound(loc, Sound.ENTITY_SQUID_HURT, SoundCategory.PLAYERS, 0.4f, 0.4f);
			world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.PLAYERS, 0.4f, 0.8f);
		}
	}
}
