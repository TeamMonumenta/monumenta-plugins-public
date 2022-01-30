package com.playmonumenta.plugins.adapters;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftMob;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class VersionAdapter_v1_18_R1 implements VersionAdapter {
	public void resetPlayerIdleTimer(Player player) {
		CraftPlayer p = (CraftPlayer)player;
		ServerPlayer playerHandle = p.getHandle();
		playerHandle.resetLastActionTime();
	}

	private static class CustomDamageSource extends EntityDamageSource {
		String mKilledUsingMsg;

		public CustomDamageSource(net.minecraft.world.entity.Entity damager, @Nullable String killedUsingMsg) {
			super("custom", damager);

			if (killedUsingMsg == null || killedUsingMsg.isEmpty()) {
				mKilledUsingMsg = "magic";
			} else {
				mKilledUsingMsg = killedUsingMsg;
			}
		}

		@Override
		public Component getLocalizedDeathMessage(net.minecraft.world.entity.LivingEntity entityliving) {
			// death.attack.indirectMagic.item=%1$s was killed by %2$s using %3$s
			String s = "death.attack.indirectMagic.item";
			return new TranslatableComponent(s, new Object[] { entityliving.getScoreboardName(), this.entity.getScoreboardName(), mKilledUsingMsg});
		}
	}

	public void customDamageEntity(@Nullable LivingEntity damager, LivingEntity damagee, double amount) {
		customDamageEntity(damager, damagee, amount, null);
	}

	public void customDamageEntity(@Nullable LivingEntity damager, LivingEntity damagee, double amount, @Nullable String killedUsingMsg) {
		DamageSource reason = new CustomDamageSource(damager == null ? null : ((CraftLivingEntity) damager).getHandle(), killedUsingMsg);

		((CraftLivingEntity) damagee).getHandle().hurt(reason, (float) amount);
	}

	private static class UnblockableEntityDamageSource extends EntityDamageSource {
		private final @Nullable String mKilledUsingMsg;

		public UnblockableEntityDamageSource(net.minecraft.world.entity.Entity entity) {
			super("custom", entity);
			mKilledUsingMsg = null;
		}

		public UnblockableEntityDamageSource(net.minecraft.world.entity.Entity damager, @Nullable String killedUsingMsg) {
			super("custom", damager);
			if (killedUsingMsg == null || !killedUsingMsg.isEmpty()) {
			    mKilledUsingMsg = killedUsingMsg;
			} else {
			    // We don't want to see "Player was killed by Mob using ", so just get rid of the message if it's nothing
			    mKilledUsingMsg = null;
			}
		}

		@Override
		public @Nullable Vec3 getSourcePosition() {
			return null;
		}

		@Override
		public Component getLocalizedDeathMessage(net.minecraft.world.entity.LivingEntity entityliving) {
			assert this.entity != null : "@AssumeAssertion(nullness): always set in constructors of this subclass";
			if (mKilledUsingMsg == null) {
				String s = "death.attack.mob";
				return new TranslatableComponent(s, entityliving.getScoreboardName(), this.entity.getScoreboardName());
			} else {
				// death.attack.indirectMagic.item=%1$s was killed by %2$s using %3$s
				String s = "death.attack.indirectMagic.item";
				return new TranslatableComponent(s, entityliving.getScoreboardName(), this.entity.getScoreboardName(), mKilledUsingMsg);
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
		((CraftLivingEntity) damagee).getHandle().hurt(reason, (float) amount);
	}

	@SuppressWarnings("unchecked")
	public <T extends Entity> T duplicateEntity(T entity) {
		T newEntity = (T)entity.getWorld().spawnEntity(entity.getLocation(), entity.getType());

		CompoundTag nbttagcompound = ((CraftEntity) entity).getHandle().saveWithoutId(new CompoundTag());
		nbttagcompound.remove("UUID");
		nbttagcompound.remove("UUIDMost");
		nbttagcompound.remove("UUIDLeast");

		((CraftEntity) newEntity).getHandle().load(nbttagcompound);

		return newEntity;
	}

	public @Nullable Entity getEntityById(World world, int entityId) {
		net.minecraft.world.entity.Entity entity = ((CraftWorld) world).getHandle().getEntity(entityId);
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

		double rotX = ((CraftEntity) entity).getHandle().getXRot();
		double rotY = ((CraftEntity) entity).getHandle().getYRot();

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
	private static final Field attackCooldownField = getField(net.minecraft.world.entity.LivingEntity.class, "at");

	@SuppressWarnings("unboxing.of.nullable")
	public int getAttackCooldown(LivingEntity entity) {
		return (int) getFieldValue(attackCooldownField, ((CraftLivingEntity) entity).getHandle());
	}

	public void setAttackCooldown(LivingEntity entity, int newCooldown) {
		setFieldValue(attackCooldownField, ((CraftLivingEntity) entity).getHandle(), newCooldown);
	}

	// Update the code in releaseActiveItem() below before updating this, as this may not even be used anymore.
	private static final Method tickActiveItemStack = getMethod(net.minecraft.world.entity.LivingEntity.class, "A");

	public void releaseActiveItem(LivingEntity entity, boolean clearActiveItem) {
		net.minecraft.world.entity.LivingEntity nmsEntity = ((CraftLivingEntity) entity).getHandle();
		if (clearActiveItem) {
			nmsEntity.releaseUsingItem();
		} else {
			// This code is adapted from releaseActiveItem(), without the call to clearActiveItem() (can't use exactly because of private fields)
			ItemStack activeItem = nmsEntity.getUseItem();
			if (!activeItem.isEmpty()) {
				activeItem.releaseUsing(nmsEntity.level, nmsEntity, nmsEntity.getUseItemRemainingTicks());
				if (activeItem.useOnRelease()) {
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
		((CraftMob) mob).getHandle().setXxa(0);
		((CraftMob) mob).getHandle().setZza(0);
	}
}
