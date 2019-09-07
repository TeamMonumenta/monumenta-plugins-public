package com.playmonumenta.nms.utils;

import javax.annotation.Nullable;

import org.bukkit.craftbukkit.v1_13_R2.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftLivingEntity;
import org.bukkit.entity.Player;

import net.minecraft.server.v1_13_R2.ChatMessage;
import net.minecraft.server.v1_13_R2.DamageSource;
import net.minecraft.server.v1_13_R2.Entity;
import net.minecraft.server.v1_13_R2.EntityDamageSource;
import net.minecraft.server.v1_13_R2.EntityLiving;
import net.minecraft.server.v1_13_R2.IChatBaseComponent;

public class NmsEntityUtils {
	public static class CustomDamageSource extends EntityDamageSource {
		String mKilledUsingMsg;
		Entity mDamager;

		public CustomDamageSource(Entity damager, @Nullable String killedUsingMsg) {
			super("custom", damager);

			mDamager = damager;
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

	public static void customDamageEntity(org.bukkit.entity.LivingEntity entity, double amount, Player damager) {
		customDamageEntity(entity, amount, damager, null);
	}

	public static void customDamageEntity(org.bukkit.entity.LivingEntity entity, double amount, Player damager, String killedUsingMsg) {
        DamageSource reason = new CustomDamageSource(((CraftHumanEntity) damager).getHandle(), killedUsingMsg);

        ((CraftLivingEntity)entity).getHandle().damageEntity(reason, (float) amount);
	}
}
