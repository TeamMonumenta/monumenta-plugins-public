package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class WindAspect implements Enchantment {

	@Override
	public String getName() {
		return "Wind Aspect";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.WIND_ASPECT;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		if (AbilityUtils.isAspectTriggeringEvent(event, player) && !EntityUtils.isBoss(enemy) && !enemy.getScoreboardTags().contains("boss_ccimmune")) {
			launch(plugin, player, enemy, level * (event.getType() == DamageEvent.DamageType.MELEE ? player.getCooledAttackStrength(0) : 1));
		}
	}

	public static void launch(Plugin plugin, Player player, LivingEntity e, double level) {
		PotionUtils.applyPotion(player, e, new PotionEffect(PotionEffectType.SLOW_FALLING, 20, 0));
		double kbr = EntityUtils.getAttributeOrDefault(e, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 0);

		if (kbr >= 1) {
			return;
		}

		World world = e.getWorld();
		world.playSound(e.getLocation(), Sound.ENTITY_HORSE_BREATHE, 1.0f, 0.30f);

		double widthDelta = PartialParticle.getWidthDelta(e);
		double doubleWidthDelta = widthDelta * 2;
		double heightDelta = PartialParticle.getHeightDelta(e);

		new PartialParticle(
			Particle.CLOUD,
			LocationUtils.getHeightLocation(e, 0.25),
			10,
			doubleWidthDelta,
			heightDelta / 2,
			doubleWidthDelta
		).spawnAsEnemy();

		double mult = Math.sqrt(level * (1 - kbr));

		// Run at the end of the tick to override knockback
		Bukkit.getScheduler().runTask(plugin, () -> e.setVelocity(new Vector(0.f, 0.6 * mult, 0.f)));
	}

}
