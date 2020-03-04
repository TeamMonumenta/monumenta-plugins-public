package com.playmonumenta.plugins.utils;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.craftbukkit.v1_13_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import net.minecraft.server.v1_13_R2.ChatMessage;
import net.minecraft.server.v1_13_R2.DamageSource;
import net.minecraft.server.v1_13_R2.Entity;
import net.minecraft.server.v1_13_R2.EntityCreature;
import net.minecraft.server.v1_13_R2.EntityDamageSource;
import net.minecraft.server.v1_13_R2.EntityInsentient;
import net.minecraft.server.v1_13_R2.EntityLiving;
import net.minecraft.server.v1_13_R2.EntityPlayer;
import net.minecraft.server.v1_13_R2.IChatBaseComponent;
import net.minecraft.server.v1_13_R2.PathfinderGoalSelector;
import net.minecraft.server.v1_13_R2.Vec3D;

public class NmsUtils {
	public static void resetPlayerIdleTimer(Player player) {
		CraftPlayer p = (CraftPlayer)player;
		EntityPlayer playerHandle = p.getHandle();
		playerHandle.resetIdleTimer();
	}

	private static Class<?> itemClazz = null;

	public static void removeVexSpawnAIFromEvoker(LivingEntity boss) {
		try {
			if (itemClazz == null) {
				itemClazz = Class.forName("net.minecraft.server.v1_13_R2.PathfinderGoalSelector$PathfinderGoalSelectorItem");
			}

			if (((CraftEntity) boss).getHandle() instanceof EntityInsentient && ((CraftEntity) boss).getHandle() instanceof EntityCreature) {
				EntityInsentient ei = (EntityInsentient)((CraftEntity) boss).getHandle();
				Set<?> goalB = (Set<?>) getPrivateField("b", PathfinderGoalSelector.class, ei.goalSelector);
				Iterator<?> it = goalB.iterator();
				while (it.hasNext()) {
					Object selector = it.next();
					Object goal = getPrivateField("a", itemClazz, selector);
					if (goal.getClass().getName().equals("net.minecraft.server.v1_13_R2.EntityEvoker$c")) {
						it.remove();
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
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

	public static void customDamageEntity(@Nonnull LivingEntity entity, double amount, @Nonnull Player damager) {
		customDamageEntity(entity, amount, damager, null);
	}

	public static void customDamageEntity(@Nonnull LivingEntity entity, double amount, @Nonnull Player damager, @Nullable String killedUsingMsg) {
        DamageSource reason = new CustomDamageSource(((CraftHumanEntity) damager).getHandle(), killedUsingMsg);

        ((CraftLivingEntity)entity).getHandle().damageEntity(reason, (float) amount);
	}

	private static class UnblockableEntityDamageSource extends EntityDamageSource {
		public UnblockableEntityDamageSource(Entity entity) {
			super("custom", entity);
		}

		@Override
		public Vec3D w() {
			return null;
		}

		@Override
		public IChatBaseComponent getLocalizedDeathMessage(EntityLiving entityliving) {
			String s = "death.attack.mob";
			return new ChatMessage(s, new Object[] { entityliving.getScoreboardDisplayName(), this.w.getScoreboardDisplayName()});
		}

	}

	public static void unblockableEntityDamageEntity(@Nonnull LivingEntity damagee, double amount, @Nonnull LivingEntity damager) {
        DamageSource reason = new UnblockableEntityDamageSource(damager == null ? null : ((CraftLivingEntity) damager).getHandle());

        ((CraftLivingEntity)damagee).getHandle().damageEntity(reason, (float) amount);
	}

	private static Object getPrivateField(String fieldName, Class<?> clazz, Object object) throws NoSuchFieldException, IllegalAccessException {
		Field field;
		Object o = null;

		field = clazz.getDeclaredField(fieldName);

		field.setAccessible(true);

		o = field.get(object);

		return o;
	}
}
