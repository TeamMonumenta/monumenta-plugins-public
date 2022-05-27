package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.bosses.bosses.CrowdControlImmunityBoss;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.delves.mobabilities.StatMultiplierBoss;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.LivingEntity;

public class StatMultiplier {

	private static final String HEALTH_MODIFIER_NAME = "DelveHealthModifier";
	private static final String SPEED_MODIFIER_NAME = "DelveSpeedModifier";

	private static final double DAMAGE_MULTIPLIER_INCREMENT = 0.03;
	private static final double HEALTH_MULTIPLIER_INCREMENT = 0.012;
	private static final double SPEED_MULTIPLIER_INCREMENT = 0.004;

	private static final Map<String, Double> STAT_COMPENSATION_MAPPINGS = new HashMap<>();

	public static final double DELVE_MOB_STAT_MULTIPLIER_R1 = 0.5;
	public static final double DELVE_MOB_STAT_MULTIPLIER_R2 = 1;

	static {
		STAT_COMPENSATION_MAPPINGS.put("white", 1.7);
		STAT_COMPENSATION_MAPPINGS.put("orange", 1.5);
		STAT_COMPENSATION_MAPPINGS.put("magenta", 1.4);
		STAT_COMPENSATION_MAPPINGS.put("lightblue", 1.2);
		STAT_COMPENSATION_MAPPINGS.put("yellow", 1.1);
		STAT_COMPENSATION_MAPPINGS.put("willows", 1.2);
		STAT_COMPENSATION_MAPPINGS.put("reverie", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("lime", 1.6);
		STAT_COMPENSATION_MAPPINGS.put("pink", 1.4);
		STAT_COMPENSATION_MAPPINGS.put("gray", 1.4);
		STAT_COMPENSATION_MAPPINGS.put("lightgray", 1.15);
		STAT_COMPENSATION_MAPPINGS.put("cyan", 1.15);
		STAT_COMPENSATION_MAPPINGS.put("purple", 1.1);
		STAT_COMPENSATION_MAPPINGS.put("teal", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("forum", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("shiftingcity", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("depths", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("dev1", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("dev2", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("mobs", 1.0);
	}

	public static double getStatCompensation(String dungeon) {
		return STAT_COMPENSATION_MAPPINGS.getOrDefault(dungeon, 1.0);
	}

	public static double getDamageMultiplier(int depthPoints) {
		if (isDepthsShard()) {
			double basePoints = Math.min(25, depthPoints);
			double bonusPoints = Math.max(0, (depthPoints - 25) / 2.0);
			return 1 + ((basePoints + bonusPoints) * DAMAGE_MULTIPLIER_INCREMENT);
		}
		return 1 + Math.min(DelvesUtils.getLootCapDepthPoints(9001), depthPoints) * DAMAGE_MULTIPLIER_INCREMENT;
	}

	public static double getHealthMultiplier(int depthPoints) {
		if (isDepthsShard()) {
			double basePoints = Math.min(25, depthPoints);
			double bonusPoints = Math.max(0, (depthPoints - 25) / 2.0);
			return 1 + ((basePoints + bonusPoints) * HEALTH_MULTIPLIER_INCREMENT);
		}
		return 1 + Math.min(DelvesUtils.getLootCapDepthPoints(9001), depthPoints) * HEALTH_MULTIPLIER_INCREMENT;
	}

	public static double getSpeedMultiplier(int depthPoints) {
		return 1 + Math.min(DelvesUtils.getLootCapDepthPoints(9001), depthPoints) * SPEED_MULTIPLIER_INCREMENT;
	}

	public static double getDelveMobStatMultiplier(int point) {
		return STAT_COMPENSATION_MAPPINGS.get(ServerProperties.getShardName()) == null ? 1 : STAT_COMPENSATION_MAPPINGS.get(ServerProperties.getShardName());
	}

	public static boolean isDepthsShard() {
		return ServerProperties.getShardName().contains("depths")
			|| ServerProperties.getShardName().equals("mobs")
			|| ServerProperties.getShardName().startsWith("dev");
	}

	public static void applyModifiers(LivingEntity mob, int level) {
		if (level == 0) {
			//somehow we run a delve with 0 score delvescore
			return;
		}
		//stat
		double healthMulti = DelvesUtils.isDelveMob(mob) ?
			                    getHealthMultiplier(level) * (ServerProperties.getClassSpecializationsEnabled() ? DELVE_MOB_STAT_MULTIPLIER_R2 : DELVE_MOB_STAT_MULTIPLIER_R1) :
			                    getHealthMultiplier(level) * STAT_COMPENSATION_MAPPINGS.getOrDefault(ServerProperties.getShardName(), 1.0);

		EntityUtils.scaleMaxHealth(mob, healthMulti - 1, HEALTH_MODIFIER_NAME);

		Set<String> tags = mob.getScoreboardTags();
		if (!tags.contains(CrowdControlImmunityBoss.identityTag)) {
			EntityUtils.addAttribute(mob, Attribute.GENERIC_MOVEMENT_SPEED,
					new AttributeModifier(SPEED_MODIFIER_NAME, getSpeedMultiplier(level) - 1, Operation.MULTIPLY_SCALAR_1));
		}

		mob.addScoreboardTag(StatMultiplierBoss.identityTag);
		mob.addScoreboardTag(StatMultiplierBoss.identityTag + "[damagestatmult=" + getDelveMobStatMultiplier(level) + ",damagemult=" + getDamageMultiplier(level) + "]");
	}

}
