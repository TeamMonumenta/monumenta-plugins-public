package com.playmonumenta.plugins.adapters;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class VersionAdapter_unsupported implements VersionAdapter {

	public void removeAllMetadata(Plugin plugin) {

	}

	public void resetPlayerIdleTimer(Player player) {

	}

	public void customDamageEntity(@Nullable LivingEntity damager, LivingEntity damagee, double amount, boolean blockable, @Nullable String killedUsingMsg) {
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
	public Entity spawnWorldlessEntity(EntityType type, World world) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getEntityTypeRegistryId(Entity entity) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void disablePerching(Parrot parrot) {

	}

	@Override
	public void setAggressive(Creature entity, DamageAction action) {

	}

	@Override
	public void setFriendly(Creature entity, DamageAction action, Predicate<LivingEntity> predicate, double attackRange) {

	}

	@Override
	public void setHuntingCompanion(Creature entity, DamageAction action, double attackRange) {

	}

	@Override
	public void setAttackRange(Creature entity, double attackRange) {

	}

	@Override
	public Class<?> getResourceKeyClass() {
		return null;
	}

	@Override
	public Object createDimensionTypeResourceKey(String namespace, String key) {
		return null;
	}

	@Override
	public void executeCommandAsBlock(Block block, String command) {

	}

	@Override
	public boolean hasCollision(World world, BoundingBox aabb) {
		return false;
	}

	@Override
	public boolean hasCollisionWithBlocks(World world, BoundingBox aabb, boolean loadChunks) {
		return false;
	}

	@Override
	public boolean hasCollisionWithBlocks(World world, BoundingBox aabb, boolean loadChunks, Predicate<Material> checkedTypes) {
		return false;
	}

	@Override
	public Set<Block> getCollidingBlocks(World world, BoundingBox aabb, boolean loadChunks) {
		return new HashSet<>();
	}

	@Override
	public void mobAIChanges(Mob entity) {
	}

	@Override
	public Object toVanillaChatComponent(Component component) {
		return null;
	}

	@Override
	public boolean isSameItem(@Nullable ItemStack item1, @Nullable ItemStack item2) {
		return item1 == item2;
	}

	@Override
	public void forceDismountVehicle(Entity entity) {
		entity.leaveVehicle();
	}

	@Override
	public ItemStack getUsedProjectile(Player player, ItemStack weapon) {
		return new ItemStack(Material.AIR);
	}

	@Override
	public Component getDisplayName(ItemStack item) {
		return item.displayName();
	}

}
