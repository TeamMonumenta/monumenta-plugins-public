package com.playmonumenta.plugins.adapters;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface VersionAdapter {
	void resetPlayerIdleTimer(Player player);

	void customDamageEntity(@Nullable LivingEntity damager, LivingEntity damagee, double amount);

	void customDamageEntity(@Nullable LivingEntity damager, LivingEntity damagee, double amount, @Nullable String killedUsingMsg);

	void unblockableEntityDamageEntity(LivingEntity damagee, double amount, LivingEntity damager);

	void unblockableEntityDamageEntity(LivingEntity damagee, double amount, LivingEntity damager, @Nullable String cause);

	<T extends org.bukkit.entity.Entity> T duplicateEntity(T entity);

	/**
	 * Gets an entity by its {@link org.bukkit.entity.Entity#getEntityId() id} (i.e. not by its {@link org.bukkit.entity.Entity#getUniqueId() UUID}).
	 */
	org.bukkit.entity.@Nullable Entity getEntityById(World world, int entityId);

	/**
	 * Gets the actual direction of an entity instead of the direction of its head.
	 * This is particularly useful for players as this gives the direction a player is actually looking
	 * instead of one slightly in the past as the head is lagging behind the actual direction.
	 */
	Vector getActualDirection(org.bukkit.entity.Entity entity);

	int getAttackCooldown(LivingEntity entity);

	void setAttackCooldown(LivingEntity entity, int newCooldown);

	/**
	 * Forces the given living entity to stop using its active item, e.g. lowers a raised shield or charges a crossbow (if it has been changing for long enough).
	 *
	 * @param clearActiveItem If false, will keep the item in use. Useful only for crossbows to not cause them to shoot immediately after this method is called.
	 */
	void releaseActiveItem(LivingEntity entity, boolean clearActiveItem);

	void stunShield(Player player, int ticks);
}
