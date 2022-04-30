package com.playmonumenta.plugins.adapters;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.server.v1_16_R3.ChatMessage;
import net.minecraft.server.v1_16_R3.DamageSource;
import net.minecraft.server.v1_16_R3.EntityCreature;
import net.minecraft.server.v1_16_R3.EntityDamageSource;
import net.minecraft.server.v1_16_R3.EntityHuman;
import net.minecraft.server.v1_16_R3.EntityLiving;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.EntityTypes;
import net.minecraft.server.v1_16_R3.IChatBaseComponent;
import net.minecraft.server.v1_16_R3.IRegistry;
import net.minecraft.server.v1_16_R3.MinecraftKey;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.PathfinderGoalMeleeAttack;
import net.minecraft.server.v1_16_R3.PathfinderGoalNearestAttackableTarget;
import net.minecraft.server.v1_16_R3.PathfinderGoalPanic;
import net.minecraft.server.v1_16_R3.PathfinderGoalPerch;
import net.minecraft.server.v1_16_R3.PathfinderGoalWrapped;
import net.minecraft.server.v1_16_R3.ResourceKey;
import net.minecraft.server.v1_16_R3.Vec3D;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftCreature;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftMob;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftParrot;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class VersionAdapter_v1_16_R3 implements VersionAdapter {

	public void removeAllMetadata(Plugin plugin) {
		CraftServer server = (CraftServer) plugin.getServer();
		server.getEntityMetadata().removeAll(plugin);
		server.getPlayerMetadata().removeAll(plugin);
		server.getWorldMetadata().removeAll(plugin);
		for (World world : Bukkit.getWorlds()) {
			((CraftWorld) world).getBlockMetadata().removeAll(plugin);
		}
	}

	public void resetPlayerIdleTimer(Player player) {
		CraftPlayer p = (CraftPlayer) player;
		EntityPlayer playerHandle = p.getHandle();
		playerHandle.resetIdleTimer();
	}

	private static class CustomDamageSource extends EntityDamageSource {
		private final boolean mBlockable;
		private final String mKilledUsingMsg;

		public CustomDamageSource(net.minecraft.server.v1_16_R3.Entity damager, boolean blockable, @Nullable String killedUsingMsg) {
			super("custom", damager);
			mBlockable = blockable;
			if (killedUsingMsg == null || killedUsingMsg.isEmpty()) {
				// We don't want to see "Player was killed by Mob using ", so just get rid of the message if it's nothing
				mKilledUsingMsg = null;
			} else {
				mKilledUsingMsg = killedUsingMsg;
			}
		}

		@Override
		public @Nullable Vec3D w() {
			return mBlockable ? super.w() : null;
		}

		@Override
		public IChatBaseComponent getLocalizedDeathMessage(EntityLiving entityliving) {
			assert this.w != null : "@AssumeAssertion(nullness): always set in constructors of this subclass";
			if (mKilledUsingMsg == null) {
				String s = "death.attack.mob";
				return new ChatMessage(s, entityliving.getScoreboardDisplayName(), this.w.getScoreboardDisplayName());
			} else {
				// death.attack.indirectMagic.item=%1$s was killed by %2$s using %3$s
				String s = "death.attack.indirectMagic.item";
				return new ChatMessage(s, entityliving.getScoreboardDisplayName(), this.w.getScoreboardDisplayName(), mKilledUsingMsg);
			}
		}
	}

	public void customDamageEntity(@Nullable LivingEntity damager, LivingEntity damagee, double amount, boolean blockable, @Nullable String killedUsingMsg) {
		DamageSource reason = damager == null ? DamageSource.GENERIC : new CustomDamageSource(((CraftLivingEntity) damager).getHandle(), blockable, killedUsingMsg);

		((CraftLivingEntity) damagee).getHandle().damageEntity(reason, (float) amount);
	}

	@SuppressWarnings("unchecked")
	public <T extends Entity> T duplicateEntity(T entity) {
		T newEntity = (T) entity.getWorld().spawnEntity(entity.getLocation(), entity.getType());

		NBTTagCompound nbttagcompound = ((CraftEntity) entity).getHandle().save(new NBTTagCompound());
		nbttagcompound.remove("UUID");
		nbttagcompound.remove("UUIDMost");
		nbttagcompound.remove("UUIDLeast");

		((CraftEntity) newEntity).getHandle().load(nbttagcompound);

		return newEntity;
	}

	public @Nullable Entity getEntityById(World world, int entityId) {
		net.minecraft.server.v1_16_R3.Entity entity = ((CraftWorld) world).getHandle().getEntity(entityId);
		return entity == null ? null : entity.getBukkitEntity();
	}

	private static Field getField(Class<?> clazz, String field) {
		try {
			Field f = clazz.getDeclaredField(field);
			f.setAccessible(true);
			return f;
		} catch (NoSuchFieldException e) {
			// Should only happen if Minecraft is updated.
			// Check the documentation of where this is used for how to find the new name.
			throw new RuntimeException(e);
		}
	}

	private static @Nullable Object getFieldValue(Field field, Object target) {
		try {
			return field.get(target);
		} catch (IllegalAccessException e) {
			// Should not happen as the field is set to be accessible
			throw new RuntimeException(e);
		}
	}

	private static void setFieldValue(Field field, Object target, @Nullable Object value) {
		try {
			field.set(target, value);
		} catch (IllegalAccessException e) {
			// Should not happen as the field is set to be accessible
			throw new RuntimeException(e);
		}
	}

	public Vector getActualDirection(Entity entity) {
		Vector vector = new Vector();

		double rotX = ((CraftEntity) entity).getHandle().yaw;
		double rotY = ((CraftEntity) entity).getHandle().pitch;

		vector.setY(-Math.sin(Math.toRadians(rotY)));

		double xz = Math.cos(Math.toRadians(rotY));

		vector.setX(-xz * Math.sin(Math.toRadians(rotX)));
		vector.setZ(xz * Math.cos(Math.toRadians(rotX)));

		return vector;
	}

	private static Method getMethod(Class<?> clazz, String method, Class<?>... arguments) {
		try {
			Method m = clazz.getDeclaredMethod(method, arguments);
			m.setAccessible(true);
			return m;
		} catch (NoSuchMethodException e) {
			// Should only happen if Minecraft is updated.
			// Check the documentation of where this is used for how to find the new name.
			throw new RuntimeException(e);
		}
	}

	private static @Nullable Object invokeMethod(Method method, Object target, @Nullable Object... args) {
		try {
			return method.invoke(target, args);
		} catch (IllegalAccessException e) {
			// Should not happen as the method is set to be accessible
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			// RuntimeException and Errors can happen, just throw them again (without the InvocationTargetException wrapper)
			if (e.getCause() instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			if (e.getCause() instanceof Error error) {
				throw error;
			}
			// This should not happen as long as the methods have no checked exceptions declared
			throw new RuntimeException(e);
		}
	}

	// To find the new field name, see which field is reset by EntityHuman.resetAttackCooldown
	// If the field's type changed from int to another type, update the type used by the getAttackCooldown/setAttackCooldown methods in this class accordingly.
	private static final Field attackCooldownField = getField(EntityLiving.class, "at");

	@SuppressWarnings("unboxing.of.nullable")
	public int getAttackCooldown(LivingEntity entity) {
		return (int) getFieldValue(attackCooldownField, ((CraftLivingEntity) entity).getHandle());
	}

	public void setAttackCooldown(LivingEntity entity, int newCooldown) {
		setFieldValue(attackCooldownField, ((CraftLivingEntity) entity).getHandle(), newCooldown);
	}

	// Update the code in releaseActiveItem() below before updating this, as this may not even be used anymore.
	private static final Method tickActiveItemStack = getMethod(EntityLiving.class, "t");

	public void releaseActiveItem(LivingEntity entity, boolean clearActiveItem) {
		EntityLiving nmsEntity = ((CraftLivingEntity) entity).getHandle();
		if (clearActiveItem) {
			nmsEntity.releaseActiveItem();
		} else {
			// This code is copied from releaseActiveItem(), without the call to clearActiveItem()
			if (!nmsEntity.activeItem.isEmpty()) {
				nmsEntity.activeItem.a(nmsEntity.world, nmsEntity, nmsEntity.dZ());
				if (nmsEntity.activeItem.m()) {
					invokeMethod(tickActiveItemStack, nmsEntity);
				}
			}
		}
	}

	public void stunShield(Player player, int ticks) {
		player.setCooldown(Material.SHIELD, ticks);
		if (player.getActiveItem() != null && player.getActiveItem().getType() == Material.SHIELD) {
			releaseActiveItem(player, true);
		}
	}

	@Override
	public void cancelStrafe(Mob mob) {
		((CraftMob) mob).getHandle().t(0);
		((CraftMob) mob).getHandle().v(0);
	}

	@Override
	public Entity spawnWorldlessEntity(EntityType type, World world) {
		Optional<EntityTypes<?>> entityTypes = EntityTypes.getByName(type.name().toLowerCase(Locale.ROOT));
		if (entityTypes.isEmpty()) {
			throw new IllegalArgumentException("Invalid entity type " + type.name());
		}
		net.minecraft.server.v1_16_R3.Entity entity = entityTypes.get().a(((CraftWorld) world).getHandle());
		if (entity == null) {
			throw new IllegalArgumentException("Unspawnable entity type " + type.name());
		}
		return entity.getBukkitEntity();
	}

	@Override
	public int getEntityTypeRegistryId(Entity entity) {
		return IRegistry.ENTITY_TYPE.a(((CraftEntity) entity).getHandle().getEntityType());
	}

	@Override
	public void disablePerching(Parrot parrot) {
		((CraftParrot) parrot).getHandle().goalSelector.getTasks().removeIf(w -> w.getGoal() instanceof PathfinderGoalPerch);
	}

	@Override
	public void setAggressive(Creature entity, DamageAction action) {
		EntityCreature entityCreature = ((CraftCreature) entity).getHandle();
		entityCreature.goalSelector.addGoal(0, new CustomMobAgroMeleeAttack16(entityCreature, action, 1.0));
		entityCreature.targetSelector.a(2, new PathfinderGoalNearestAttackableTarget<>(entityCreature, EntityHuman.class, true));
	}

	@Override
	public void setFriendly(Creature entity, DamageAction action, Predicate<LivingEntity> predicate, double attackRange) {
		EntityCreature entityCreature = ((CraftCreature) entity).getHandle();

		//removing panic mode
		Optional<PathfinderGoalWrapped> oldGoal = entityCreature.goalSelector.getTasks().stream().filter(task -> task.getGoal() instanceof PathfinderGoalPanic).findFirst();
		if (oldGoal.isPresent()) {
			PathfinderGoalWrapped goal = oldGoal.get();
			entityCreature.goalSelector.getTasks().remove(goal);
		}

		//removing others PathfinderGoalNearestAttackableTarget
		List<PathfinderGoalWrapped> list = entityCreature.targetSelector.getTasks().stream().filter(task -> task.getGoal() instanceof PathfinderGoalNearestAttackableTarget).toList();
		for (PathfinderGoalWrapped wrapped : list) {
			entityCreature.targetSelector.getTasks().remove(wrapped);
		}


		entityCreature.goalSelector.addGoal(0, new CustomMobAgroMeleeAttack16(entityCreature, action, 1.0) {
			@Override
			protected double a(EntityLiving target) {
				// to make it possible for entities to not attack from their feet,
				// calculate whether the attack can hit here and return positive or negative infinity to allow/disallow the attack.
				double x = a.locX();
				double y = a.locY() + 1;
				double z = a.locZ();
				if (target.h(x, y, z) <= attackRange * attackRange) {
					return Double.POSITIVE_INFINITY;
				} else {
					return Double.NEGATIVE_INFINITY;
				}
			}
		});
		entityCreature.targetSelector.a(2, new PathfinderGoalNearestAttackableTarget<>(entityCreature, EntityLiving.class, 10, false, false, entityLiving -> predicate.test(getLivingEntity(entityLiving))));
	}

	private LivingEntity getLivingEntity(EntityLiving nmsEntity) {
		try {
			return ((EntityLiving) nmsEntity).getBukkitLivingEntity();
		} catch (Exception e) {
			return null;
		}
	}

	public void setAttackRange(Creature entity, double attackRange, double attackHeight) {
		EntityCreature entityCreature = ((CraftCreature) entity).getHandle();
		Optional<PathfinderGoalWrapped> oldGoal = entityCreature.goalSelector.getTasks().stream().filter(task -> task.getGoal() instanceof PathfinderGoalMeleeAttack).findFirst();
		if (oldGoal.isPresent()) {
			PathfinderGoalWrapped goal = oldGoal.get();
			entityCreature.goalSelector.getTasks().remove(goal);
			entityCreature.goalSelector.addGoal(goal.getPriority(), new CustomPathfinderGoalMeleeAttack16(entityCreature, 1.0, true, attackRange, attackHeight));
		}
	}

	@Override
	public Class<?> getResourceKeyClass() {
		return ResourceKey.class;
	}

	@Override
	public Object createDimensionTypeResourceKey(String namespace, String key) {
		return ResourceKey.newResourceKey(IRegistry.K, new MinecraftKey(namespace, key));
	}

}
