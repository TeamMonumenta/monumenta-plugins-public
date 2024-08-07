package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.EnumSet;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

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
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.PROJECTILE);
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		if (AbilityUtils.isAspectTriggeringEvent(event, player) && !EntityUtils.isCCImmuneMob(enemy) && EntityUtils.isHostileMob(enemy)) {
			DamageEvent.DamageType type = event.getType();
			launch(plugin, player, enemy, level * (type == DamageEvent.DamageType.MELEE ? player.getCooledAttackStrength(0) : 1), type);
		}
	}

	public static void launch(Plugin plugin, Player player, LivingEntity e, double level, @Nullable DamageEvent.DamageType type) {
		PotionUtils.applyPotion(player, e, new PotionEffect(PotionEffectType.SLOW_FALLING, 20, 0));
		double kbr = EntityUtils.getAttributeOrDefault(e, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 0);

		if (kbr >= 1) {
			return;
		}

		World world = e.getWorld();
		Location loc = e.getLocation();
		if (type == DamageEvent.DamageType.MELEE) {
			world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 1.0f, 1.2f);
			world.playSound(loc, Sound.ENTITY_WITCH_THROW, SoundCategory.PLAYERS, 0.5f, 0.4f);
			world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 0.7f, 2.0f);
		} else {
			world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 1.0f, 0.30f);
		}

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
		Bukkit.getScheduler().runTask(plugin, () -> e.setVelocity(new Vector(0.f, 0.5 * mult, 0.f)));
	}

	@Override
	public void onProjectileLaunch(Plugin plugin, Player player, double value, ProjectileLaunchEvent event, Projectile projectile) {
		if (EntityUtils.isAbilityTriggeringProjectile(projectile, false) && !AbilityUtils.isVolley(player, projectile)) {
			Location loc = player.getLocation();
			AbilityUtils.playPassiveAbilitySound(loc, Sound.ENTITY_HORSE_BREATHE, 1.2f, 0.8f);
			AbilityUtils.playPassiveAbilitySound(loc, Sound.ITEM_TRIDENT_RIPTIDE_2, 0.4f, 2.0f);
			AbilityUtils.playPassiveAbilitySound(loc, Sound.ENTITY_WITCH_THROW, 1.0f, 0.4f);
			AbilityUtils.playPassiveAbilitySound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 2.0f);
		}
	}

}
