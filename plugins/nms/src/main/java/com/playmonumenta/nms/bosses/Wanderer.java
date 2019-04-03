package com.playmonumenta.nms.bosses;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.bossfights.bosses.BossAbilityGroup;
import com.playmonumenta.bossfights.spells.Spell;
import com.playmonumenta.bossfights.spells.SpellConditionalTeleport;
import com.playmonumenta.bossfights.utils.Utils;
import com.playmonumenta.nms.pathfinders.PathfinderGoalStrollWithinRadius;

import net.minecraft.server.v1_13_R2.EntityCreature;
import net.minecraft.server.v1_13_R2.EntityHuman;
import net.minecraft.server.v1_13_R2.EntityInsentient;
import net.minecraft.server.v1_13_R2.PathfinderGoalFloat;
import net.minecraft.server.v1_13_R2.PathfinderGoalHurtByTarget;
import net.minecraft.server.v1_13_R2.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_13_R2.PathfinderGoalRandomLookaround;
import net.minecraft.server.v1_13_R2.PathfinderGoalSelector;

/*
 * This isn't a boss AI or tag. It applies an AI that makes a mob able to wander around within a radius.
 * The radius is defined as the distance of the mEndLoc from the mSpawnLoc. The mob will walk at normal speed.
 * If they target a mob, their wandering pathfinding will not interrupt their mob targetting pathfinding.
 * They choose their next wander point and move every 7-9 seconds (or 140-180 ticks)
 * Will make mob passive.
 */

public class Wanderer extends BossAbilityGroup {
	public static final String identityTag = "ai_wanderer";
	public static final String WANDERER_RADIUS_TAG = "WandererRadius";
	public static final String WANDERER_LOCATION_TAG = "WandererLocation;";
	public static final int detectionRange = 50;

	private final LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new Wanderer(plugin, boss);
	}

	public Wanderer(Plugin plugin, LivingEntity boss) {
		mBoss = boss;
		mBoss.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(detectionRange);
		Location loc = mBoss.getLocation();
			try {
				double radius = 0;
				boolean tagFound = false;
				for (String tag : mBoss.getScoreboardTags()) {
					if (tag.contains(WANDERER_RADIUS_TAG)) {
						String[] str = tag.split(";");

						// This could fail if the number at position 1 is not a double, causing an
						// error.
						// Put in try brackets
						radius = Double.parseDouble(str[1]);
						for (String tagLoc : mBoss.getScoreboardTags()) {
							if (tagLoc.contains(WANDERER_LOCATION_TAG)) {
								String[] tagStr = tagLoc.split(";");
								double x = Double.parseDouble(tagStr[1]);
								double y = Double.parseDouble(tagStr[2]);
								double z = Double.parseDouble(tagStr[3]);
								loc = new Location(mBoss.getWorld(), x, y, z);
								tagFound = true;
							}
						}
						double walkSpeed = mBoss.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue();
						if (((CraftEntity) mBoss).getHandle() instanceof EntityInsentient && ((CraftEntity) mBoss).getHandle() instanceof EntityCreature) {
							EntityInsentient ei = (EntityInsentient) ((CraftEntity) mBoss).getHandle();
							EntityCreature ec = (EntityCreature) ((CraftEntity) mBoss).getHandle();
							Set goalB = (Set) Utils.getPrivateField("b", PathfinderGoalSelector.class, ei.goalSelector);
							goalB.clear();
							Set goalC = (Set) Utils.getPrivateField("c", PathfinderGoalSelector.class, ei.goalSelector);
							goalC.clear();
							Set targetB = (Set) Utils.getPrivateField("b", PathfinderGoalSelector.class, ei.targetSelector);
							targetB.clear();
							Set targetC = (Set) Utils.getPrivateField("c", PathfinderGoalSelector.class, ei.targetSelector);
							targetC.clear();

							ei.goalSelector.a(0, new PathfinderGoalFloat(ec));
							ei.goalSelector.a(7, new PathfinderGoalStrollWithinRadius(ec, walkSpeed, radius, loc));
							ei.goalSelector.a(8, new PathfinderGoalLookAtPlayer(ec, EntityHuman.class, 8.0F));
							ei.goalSelector.a(8, new PathfinderGoalRandomLookaround(ec));
							ei.targetSelector.a(1, new PathfinderGoalHurtByTarget(ec, true));
							break;
						}
					}
				}
				if (!tagFound) {
					mBoss.addScoreboardTag(WANDERER_LOCATION_TAG + loc.getX() + ";" + loc.getY() + ";" + loc.getZ());
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			List<Spell> passiveSpells = Arrays.asList(
					new SpellConditionalTeleport(mBoss, loc, b -> b.getLocation().getBlock().isLiquid())
				);
		super.constructBoss(plugin, identityTag, mBoss, null, passiveSpells, detectionRange, null);
	}
}
