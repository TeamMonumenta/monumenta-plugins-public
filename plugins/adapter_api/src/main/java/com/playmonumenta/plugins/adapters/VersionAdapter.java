package com.playmonumenta.plugins.adapters;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.bukkit.World;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public interface VersionAdapter {

	void removeAllMetadata(Plugin plugin);

	void resetPlayerIdleTimer(Player player);

	void customDamageEntity(@Nullable LivingEntity damager, LivingEntity damagee, double amount, boolean blockable, @Nullable String killedUsingMsg);

	<T extends Entity> T duplicateEntity(T entity);

	/**
	 * Gets an entity by its {@link Entity#getEntityId() id} (i.e. not by its {@link Entity#getUniqueId() UUID}).
	 */
	@Nullable Entity getEntityById(World world, int entityId);

	/**
	 * Gets the actual direction of an entity instead of the direction of its head.
	 * This is particularly useful for players as this gives the direction a player is actually looking
	 * instead of one slightly in the past as the head is lagging behind the actual direction.
	 */
	Vector getActualDirection(Entity entity);

	int getAttackCooldown(LivingEntity entity);

	void setAttackCooldown(LivingEntity entity, int newCooldown);

	/**
	 * Forces the given living entity to stop using its active item, e.g. lowers a raised shield or charges a crossbow (if it has been changing for long enough).
	 *
	 * @param clearActiveItem If false, will keep the item in use. Useful only for crossbows to not cause them to shoot immediately after this method is called.
	 */
	void releaseActiveItem(LivingEntity entity, boolean clearActiveItem);

	void stunShield(Player player, int ticks);

	void cancelStrafe(Mob mob);

	/**
	 * Spawns an entity that will not be present in the world
	 *
	 * @param type  Entity type to spawn - not all types may work!
	 * @param world Any world (required for the constructor, and used for activation range and possibly some more things)
	 * @return Newly spawned entity
	 * @throws IllegalArgumentException if the provided entity type cannot be spawned
	 */
	Entity spawnWorldlessEntity(EntityType type, World world);

	int getEntityTypeRegistryId(Entity entity);

	/**
	 * Prevents the given parrot from moving onto a player's shoulders.
	 * This is not persistent and needs to be re-applied whenever the parrot is loaded again.
	 */
	void disablePerching(Parrot parrot);

	/**
	 * Make entity agro players
	 *
	 * @param entity       The entity who should agro players
	 * @param action       Damage action when this entity hit a player
	 */
	void setAggressive(Creature entity, DamageAction action);

	/**
	 * Make this entity lose all desire to attack any Entity and make this only attack entities accepted by the predicate
	 *
	 *
	 * @param entity        The entity
	 * @param action        The damage action that will cat when this entity hit someone
	 * @param predicate     Predicate used for check which entity attack and which not
	 * @param attackRange   Attack range of this entity
	 */
	void setFriendly(Creature entity, DamageAction action, Predicate<LivingEntity> predicate, double attackRange);


	interface DamageAction {
		void damage(LivingEntity entity);
	}

	/**
	 * Changes the melee attack range of the given entity.
	 * <b>Note that this overrides any custom melee attack</b>, so care should be taken to only apply it to mobs with a basic melee attack.
	 * This needs to be verified by looking at the pathfinder goals of the entity.
	 * This is not persistent and needs to be re-applied whenever the entity is loaded again.
	 *
	 * @param entity       The entity whose attack range should be changed
	 * @param attackRange  Attack range of the entity (calculated from feet to feet)
	 * @param attackHeight Vertical offset to calculate distance from instead of from the feet
	 */
	void setAttackRange(Creature entity, double attackRange, double attackHeight);


}
