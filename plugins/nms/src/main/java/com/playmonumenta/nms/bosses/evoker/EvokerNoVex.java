package com.playmonumenta.nms.bosses.evoker;

import java.util.Iterator;
import java.util.Set;

import org.bukkit.craftbukkit.v1_13_R2.entity.CraftEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.bossfights.bosses.BossAbilityGroup;
import com.playmonumenta.bossfights.utils.Utils;

import net.minecraft.server.v1_13_R2.EntityCreature;
import net.minecraft.server.v1_13_R2.EntityInsentient;
import net.minecraft.server.v1_13_R2.PathfinderGoalSelector;

/*
 * This isn't a boss AI or tag. It applies an AI that makes a mob able to wander around within a radius.
 * The radius is defined as the distance of the mEndLoc from the mSpawnLoc. The mob will walk at normal speed.
 * If they target a mob, their wandering pathfinding will not interrupt their mob targetting pathfinding.
 * They choose their next wander point and move every 7-9 seconds (or 140-180 ticks)
 * Will make mob passive.
 */

public class EvokerNoVex extends BossAbilityGroup {
	public static final String identityTag = "boss_evoker_no_vex";
	public static final int detectionRange = 50;

	private static Class<?> itemClazz = null;

	private final LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new EvokerNoVex(plugin, boss);
	}

	public EvokerNoVex(Plugin plugin, LivingEntity boss) {
		mBoss = boss;

		try {
			if (itemClazz == null) {
				itemClazz = Class.forName("net.minecraft.server.v1_13_R2.PathfinderGoalSelector$PathfinderGoalSelectorItem");
			}

			if (((CraftEntity) mBoss).getHandle() instanceof EntityInsentient && ((CraftEntity) mBoss).getHandle() instanceof EntityCreature) {
				EntityInsentient ei = (EntityInsentient)((CraftEntity) mBoss).getHandle();
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
		super.constructBoss(plugin, identityTag, mBoss, null, null, detectionRange, null);
	}
}
