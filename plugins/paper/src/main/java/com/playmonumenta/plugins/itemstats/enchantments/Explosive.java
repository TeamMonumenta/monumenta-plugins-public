package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.BoundingBox;

public class Explosive implements Enchantment {
	public static final int RADIUS = 4;
	public static final int HITBOX_LENGTH = 2;
	public static final double DAMAGE_PERCENTAGE_PER_LEVEL = 0.1;

	@Override
	public String getName() {
		return "Explosive";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.EXPLODING;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public double getPriorityAmount() {
		return 5002;
	}

	@Override
	public void onProjectileHit(Plugin plugin, Player player, double level, ProjectileHitEvent event, Projectile projectile) {
		if (projectile instanceof AbstractArrow arrow && !(arrow instanceof Trident) && !arrow.isCritical()) {
			// Ensures only fully charged arrow shots are registered.
			return;
		}

		Location location = EntityUtils.getProjectileHitLocation(event);

		particles(location, player);

		// Using bounding box because otherwise it is a bit inconsistent.
		List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(location, RADIUS);
		BoundingBox box = BoundingBox.of(location, HITBOX_LENGTH, HITBOX_LENGTH, HITBOX_LENGTH);

		for (LivingEntity mob : nearbyMobs) {
			if (mob.getBoundingBox().overlaps(box)) {
				double rawDamage = ItemStatUtils.getAttributeAmount(player.getInventory().getItemInMainHand(), ItemStatUtils.AttributeType.PROJECTILE_DAMAGE_ADD, ItemStatUtils.Operation.ADD, ItemStatUtils.Slot.MAINHAND);
				DamageUtils.damage(player, mob, DamageEvent.DamageType.PROJECTILE, level * DAMAGE_PERCENTAGE_PER_LEVEL * rawDamage, null, true);

				// TODO: Add other enchantments functionality (Aspects)
			}
		}

		// Remove projectile (mainly to avoid tridents doing a double explosion)
		projectile.remove();
	}

	public static void particles(Location loc, Player player) {
		World world = loc.getWorld();
		world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 10, 0.5, 0.5, 0.5, 0.25);
		world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 10, 0.5, 0.5, 0.5, 0.25);
		world.spawnParticle(Particle.FLAME, loc, 10, 0.5, 0.5, 0.5, 0.25);
		player.playSound(player.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.0f, 1.5f);
	}
}
