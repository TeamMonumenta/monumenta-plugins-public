package com.playmonumenta.plugins.adapters;

import net.minecraft.server.v1_16_R3.ChatMessage;
import net.minecraft.server.v1_16_R3.DamageSource;
import net.minecraft.server.v1_16_R3.EntityDamageSource;
import net.minecraft.server.v1_16_R3.EntityLiving;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.IChatBaseComponent;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.Vec3D;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class VersionAdapter_v1_16_R3 implements VersionAdapter {
	public void resetPlayerIdleTimer(Player player) {
		CraftPlayer p = (CraftPlayer)player;
		EntityPlayer playerHandle = p.getHandle();
		playerHandle.resetIdleTimer();
	}

	private static class CustomDamageSource extends EntityDamageSource {
		String mKilledUsingMsg;

		public CustomDamageSource(net.minecraft.server.v1_16_R3.Entity damager, @Nullable String killedUsingMsg) {
			super("custom", damager);

			if (killedUsingMsg == null || killedUsingMsg.isEmpty()) {
				mKilledUsingMsg = "magic";
			} else {
				mKilledUsingMsg = killedUsingMsg;
			}
		}

		@Override
		public IChatBaseComponent getLocalizedDeathMessage(EntityLiving entityliving) {
			// death.attack.indirectMagic.item=%1$s was killed by %2$s using %3$s
			String s = "death.attack.indirectMagic.item";
			return new ChatMessage(s, new Object[] { entityliving.getScoreboardDisplayName(), this.w.getScoreboardDisplayName(), mKilledUsingMsg});
		}
	}

	public void customDamageEntity(@Nullable LivingEntity damager, LivingEntity damagee, double amount) {
		customDamageEntity(damager, damagee, amount, null);
	}

	public void customDamageEntity(@Nullable LivingEntity damager, LivingEntity damagee, double amount, @Nullable String killedUsingMsg) {
		DamageSource reason = new CustomDamageSource(damager == null ? null : ((CraftLivingEntity) damager).getHandle(), killedUsingMsg);

		((CraftLivingEntity) damagee).getHandle().damageEntity(reason, (float) amount);
	}

	private static class UnblockableEntityDamageSource extends EntityDamageSource {
		private final @Nullable String mKilledUsingMsg;

		public UnblockableEntityDamageSource(net.minecraft.server.v1_16_R3.Entity entity) {
			super("custom", entity);
			mKilledUsingMsg = null;
		}

		public UnblockableEntityDamageSource(net.minecraft.server.v1_16_R3.Entity damager, @Nullable String killedUsingMsg) {
			super("custom", damager);
			if (killedUsingMsg == null || !killedUsingMsg.isEmpty()) {
				mKilledUsingMsg = killedUsingMsg;
			} else {
				// We don't want to see "Player was killed by Mob using ", so just get rid of the message if it's nothing
				mKilledUsingMsg = null;
			}
		}

		@Override
		public @Nullable Vec3D w() {
			return null;
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

	public void unblockableEntityDamageEntity(LivingEntity damagee, double amount, LivingEntity damager) {
		unblockableEntityDamageEntity(damagee, amount, damager, null);
	}

	public void unblockableEntityDamageEntity(LivingEntity damagee, double amount, LivingEntity damager, @Nullable String cause) {
		// Don't damage invulnerable entities even though this is unblockable
		if (damagee.isInvulnerable()) {
			return;
		}

		DamageSource reason = new UnblockableEntityDamageSource(damager == null ? null : ((CraftLivingEntity) damager).getHandle(), cause);
		((CraftLivingEntity) damagee).getHandle().damageEntity(reason, (float) amount);
	}

	@SuppressWarnings("unchecked")
	public <T extends Entity> T duplicateEntity(T entity) {
		T newEntity = (T)entity.getWorld().spawnEntity(entity.getLocation(), entity.getType());

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
}
