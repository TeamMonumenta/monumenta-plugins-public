package com.playmonumenta.plugins.adapters;

import javax.annotation.Nullable;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class VersionAdapter_unsupported implements VersionAdapter {
	public void resetPlayerIdleTimer(Player player) {

	}

	public void customDamageEntity(@Nullable LivingEntity damager, LivingEntity damagee, double amount) {
		customDamageEntity(damager, damagee, amount, null);
	}

	public void customDamageEntity(@Nullable LivingEntity damager, LivingEntity damagee, double amount, @Nullable String killedUsingMsg) {
		damagee.damage(amount, damager);
	}

	public void unblockableEntityDamageEntity(LivingEntity damagee, double amount, LivingEntity damager) {
		damagee.damage(amount, damager);
	}

	public void unblockableEntityDamageEntity(LivingEntity damagee, double amount, LivingEntity damager, @Nullable String cause) {
		damagee.damage(amount, damager);
	}

	@SuppressWarnings("unchecked")
	public <T extends Entity> T duplicateEntity(T entity) {
		return (T)entity.getWorld().spawnEntity(entity.getLocation(), entity.getType());
	}

	public @Nullable Entity getEntityById(World world, int entityId) {
		return null;
	}

	public Vector getActualDirection(Entity entity) {
		return entity.getLocation().getDirection();
	}

	public int getAttackCooldown(LivingEntity entity) {
		return 0;
	}

	public void setAttackCooldown(LivingEntity entity, int newCooldown) {

	}

	public void releaseActiveItem(LivingEntity entity, boolean clearActiveItem) {

	}

	public void stunShield(Player player, int ticks) {

	}

	@Override
	public void cancelStrafe(Mob mob) {

	}

	@Override
	public int getEntityTypeRegistryId(Entity entity) {
		throw new UnsupportedOperationException();
	}

}
