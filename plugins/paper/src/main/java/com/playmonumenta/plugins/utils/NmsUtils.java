package com.playmonumenta.plugins.utils;

import java.lang.reflect.Field;

import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import net.minecraft.server.v1_16_R3.ChatMessage;
import net.minecraft.server.v1_16_R3.DamageSource;
import net.minecraft.server.v1_16_R3.Entity;
import net.minecraft.server.v1_16_R3.EntityDamageSource;
import net.minecraft.server.v1_16_R3.EntityLiving;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.IChatBaseComponent;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.Vec3D;

public class NmsUtils {
	public static void resetPlayerIdleTimer(Player player) {
		CraftPlayer p = (CraftPlayer)player;
		EntityPlayer playerHandle = p.getHandle();
		playerHandle.resetIdleTimer();
	}

	private static class CustomDamageSource extends EntityDamageSource {
		String mKilledUsingMsg;

		public CustomDamageSource(Entity damager, @Nullable String killedUsingMsg) {
			super("custom", damager);

			if (killedUsingMsg == null) {
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

	public static void customDamageEntity(LivingEntity entity, double amount, Player damager) {
		customDamageEntity(entity, amount, damager, null);
	}

	public static void customDamageEntity(LivingEntity entity, double amount, Player damager, @Nullable String killedUsingMsg) {
		DamageSource reason = new CustomDamageSource(((CraftHumanEntity) damager).getHandle(), killedUsingMsg);

		((CraftLivingEntity) entity).getHandle().damageEntity(reason, (float) amount);
	}

	private static class UnblockableEntityDamageSource extends EntityDamageSource {
		private final @Nullable String mKilledUsingMsg;

		public UnblockableEntityDamageSource(Entity entity) {
			super("custom", entity);
			mKilledUsingMsg = null;
		}

		public UnblockableEntityDamageSource(Entity damager, @Nullable String killedUsingMsg) {
			super("custom", damager);
			mKilledUsingMsg = killedUsingMsg;
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

	public static void unblockableEntityDamageEntity(LivingEntity damagee, double amount, LivingEntity damager) {
		unblockableEntityDamageEntity(damagee, amount, damager, null);
	}

	public static void unblockableEntityDamageEntity(LivingEntity damagee, double amount, LivingEntity damager, @Nullable String cause) {
		// Don't damage invulnerable entities even though this is unblockable
		if (damagee.isInvulnerable()) {
			return;
		}

		DamageSource reason = new UnblockableEntityDamageSource(damager == null ? null : ((CraftLivingEntity) damager).getHandle(), cause);
		((CraftLivingEntity) damagee).getHandle().damageEntity(reason, (float) amount);
	}

	@SuppressWarnings("unchecked")
	public static <T extends org.bukkit.entity.Entity> T duplicateEntity(T entity) {
		T newEntity = (T)entity.getWorld().spawnEntity(entity.getLocation(), entity.getType());

		NBTTagCompound nbttagcompound = ((CraftEntity) entity).getHandle().save(new NBTTagCompound());
		nbttagcompound.remove("UUID");
		nbttagcompound.remove("UUIDMost");
		nbttagcompound.remove("UUIDLeast");

		((CraftEntity) newEntity).getHandle().load(nbttagcompound);

		return newEntity;
	}

	/**
	 * Gets an entity by its {@link org.bukkit.entity.Entity#getEntityId() id} (i.e. not by its {@link org.bukkit.entity.Entity#getUniqueId() UUID).
	 */
	public static org.bukkit.entity.@Nullable Entity getEntityById(World world, int entityId) {
		net.minecraft.server.v1_16_R3.Entity entity = ((CraftWorld) world).getHandle().getEntity(entityId);
		return entity == null ? null : entity.getBukkitEntity();
	}

	private static final Field attackCooldownField = getAttackCooldownField();

	private static Field getAttackCooldownField() {
		try {
			Field f = EntityLiving.class.getDeclaredField("at");
			f.setAccessible(true);
			return f;
		} catch (NoSuchFieldException e) {
			// Should only happen if Minecraft is updated. If it happens, update the field name in the above code.
			// To find the new field name, see which field is reset by EntityHuman.resetAttackCooldown
			// If the field's type changed from int to another type, update the type used by the getAttackCooldown/setAttackCooldown methods in this class accordingly.
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unboxing.of.nullable")
	public static int getAttackCooldown(LivingEntity entity) {
		try {
			return (int) attackCooldownField.get(((CraftLivingEntity) entity).getHandle());
		} catch (IllegalAccessException e) {
			// Should not happen as the field is set to be accessible
			throw new RuntimeException(e);
		}
	}

	public static void setAttackCooldown(LivingEntity entity, int newCooldown) {
		try {
			attackCooldownField.set(((CraftLivingEntity) entity).getHandle(), newCooldown);
		} catch (IllegalAccessException e) {
			// Should not happen as the field is set to be accessible
			throw new RuntimeException(e);
		}
	}

}
