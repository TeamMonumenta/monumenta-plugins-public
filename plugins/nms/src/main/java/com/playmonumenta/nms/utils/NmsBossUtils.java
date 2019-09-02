package com.playmonumenta.nms.utils;

import java.util.Iterator;
import java.util.Set;

import org.bukkit.craftbukkit.v1_13_R2.entity.CraftEntity;
import org.bukkit.entity.LivingEntity;

import net.minecraft.server.v1_13_R2.EntityCreature;
import net.minecraft.server.v1_13_R2.EntityInsentient;
import net.minecraft.server.v1_13_R2.PathfinderGoalSelector;

public class NmsBossUtils {
	private static Class<?> itemClazz = null;

	public static void removeVexSpawnAIFromEvoker(LivingEntity boss) {
		try {
			if (itemClazz == null) {
				itemClazz = Class.forName("net.minecraft.server.v1_13_R2.PathfinderGoalSelector$PathfinderGoalSelectorItem");
			}

			if (((CraftEntity) boss).getHandle() instanceof EntityInsentient && ((CraftEntity) boss).getHandle() instanceof EntityCreature) {
				EntityInsentient ei = (EntityInsentient)((CraftEntity) boss).getHandle();
				Set<?> goalB = (Set<?>) Utils.getPrivateField("b", PathfinderGoalSelector.class, ei.goalSelector);
				Iterator<?> it = goalB.iterator();
				while (it.hasNext()) {
					Object selector = it.next();
					Object goal = Utils.getPrivateField("a", itemClazz, selector);
					if (goal.getClass().getName().equals("net.minecraft.server.v1_13_R2.EntityEvoker$c")) {
						it.remove();
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
