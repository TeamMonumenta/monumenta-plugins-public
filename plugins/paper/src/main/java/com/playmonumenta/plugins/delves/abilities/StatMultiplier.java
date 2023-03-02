package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.bosses.bosses.CrowdControlImmunityBoss;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.delves.mobabilities.StatMultiplierBoss;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DungeonUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.structures.StructuresPlugin;
import com.playmonumenta.structures.managers.RespawningStructure;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class StatMultiplier {

	private static final String HEALTH_MODIFIER_NAME = "DelveHealthModifier";
	private static final String SPEED_MODIFIER_NAME = "DelveSpeedModifier";

	private static final double DAMAGE_MULTIPLIER_INCREMENT = 0.03;
	private static final double HEALTH_MULTIPLIER_INCREMENT = 0.012;
	private static final double SPEED_MULTIPLIER_INCREMENT = 0.004;

	private static final Map<String, Double> STAT_COMPENSATION_MAPPINGS = new HashMap<>();
	private static final Map<String, Double> STAT_COMPENSATION_MAPPINGS_RING_POI = new HashMap<>();

	public static final double DELVE_MOB_STAT_MULTIPLIER_R1 = 0.4;
	public static final double DELVE_MOB_STAT_MULTIPLIER_R2 = 1;
	public static final double DELVE_MOB_STAT_MULTIPLIER_R3 = 1.6;

	static {
		//r1 shards
		STAT_COMPENSATION_MAPPINGS.put("white", 1.7);
		STAT_COMPENSATION_MAPPINGS.put("orange", 1.5);
		STAT_COMPENSATION_MAPPINGS.put("magenta", 1.4);
		STAT_COMPENSATION_MAPPINGS.put("lightblue", 1.2);
		STAT_COMPENSATION_MAPPINGS.put("yellow", 1.1);
		STAT_COMPENSATION_MAPPINGS.put("willows", 1.2);
		STAT_COMPENSATION_MAPPINGS.put("reverie", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("corridors", 1.0);

		//r2 shards
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

		//r3 shards
		STAT_COMPENSATION_MAPPINGS.put("blue", 1.2);
		STAT_COMPENSATION_MAPPINGS.put("brown", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("ruin", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("portal", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("skt", 1.0);

		//dev shards
		STAT_COMPENSATION_MAPPINGS.put("dev1", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("dev2", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("mobs", 1.0);

		// Wolfswood
		final double FOREST = 1.2;
		STAT_COMPENSATION_MAPPINGS_RING_POI.put("Coven Fortress", FOREST);
		STAT_COMPENSATION_MAPPINGS_RING_POI.put("Locum Vernantia", FOREST);
		STAT_COMPENSATION_MAPPINGS_RING_POI.put("Shadowcast Bastille", FOREST);
		STAT_COMPENSATION_MAPPINGS_RING_POI.put("Aminita Colony", FOREST);
		STAT_COMPENSATION_MAPPINGS_RING_POI.put("Chanterelle Village", FOREST);
		STAT_COMPENSATION_MAPPINGS_RING_POI.put("Bewitched Dominion", FOREST);
		STAT_COMPENSATION_MAPPINGS_RING_POI.put("Vibrant Hollow", FOREST);

		// Pelias' Keep
		final double KEEP = 1.2;
		STAT_COMPENSATION_MAPPINGS_RING_POI.put("Quelled Convent", KEEP);
		STAT_COMPENSATION_MAPPINGS_RING_POI.put("Silvic Quarry", KEEP);
		STAT_COMPENSATION_MAPPINGS_RING_POI.put("Doomed Encampment", KEEP);
		STAT_COMPENSATION_MAPPINGS_RING_POI.put("Forsaken Manor", KEEP);
		STAT_COMPENSATION_MAPPINGS_RING_POI.put("Arx Spirensis", KEEP);
		STAT_COMPENSATION_MAPPINGS_RING_POI.put("Submerged Citadel", KEEP);

		// Exalted
		STAT_COMPENSATION_MAPPINGS.put("whiteexalted", 1.05);
		STAT_COMPENSATION_MAPPINGS.put("orangeexalted", 1.05);
		STAT_COMPENSATION_MAPPINGS.put("magentaexalted", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("lightblueexalted", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("yellowexalted", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("willowsexalted", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("reverieexalted", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("limeexalted", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("pinkexalted", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("grayexalted", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("lightgrayexalted", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("cyanexalted", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("purpleexalted", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("tealexalted", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("forumexalted", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("shiftingcityexalted", 1.0);
	}

	public static double getStatCompensation(String dungeon, boolean exalted) {
		return STAT_COMPENSATION_MAPPINGS.getOrDefault(dungeon + (exalted ? "exalted" : ""), 1.0);
	}

	public static double getStatCompensation(String shard, Location loc) {
		String shardType = shard.split("-")[0];

		if (shardType.equals("ring")) {
			List<RespawningStructure> structures = StructuresPlugin.getInstance().mRespawnManager.getStructures(loc.toVector(), false);
			for (RespawningStructure rs : structures) {
				String name = (String) rs.getConfig().get("name");
				Double value = STAT_COMPENSATION_MAPPINGS_RING_POI.get(name);
				if (value != null) {
					return value;
				}
			}
			//no match -> return default value from shard mapping
		} else {
			Player player = EntityUtils.getNearestPlayer(loc, 64);
			if (player != null) {
				DungeonUtils.DungeonCommandMapping mapping = DungeonUtils.DungeonCommandMapping.getByShard(shardType);
				if (mapping != null) {
					String typeName = mapping.getTypeName();
					boolean exaltedDungeon = false;
					if (typeName != null) {
						exaltedDungeon = ScoreboardUtils.getScoreboardValue(player, typeName).orElse(0) == 1;
					}
					if (exaltedDungeon) {
						shardType += "exalted";
					}
				}
			}
		}

		return STAT_COMPENSATION_MAPPINGS.getOrDefault(shardType, 1.0);
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

	public static boolean isDepthsShard() {
		return ServerProperties.getShardName().startsWith("depths")
			|| ServerProperties.getShardName().equals("mobs")
			|| ServerProperties.getShardName().startsWith("dev");
	}

	public static void applyModifiers(LivingEntity mob, int level) {
		if (level == 0) {
			//somehow we run a delve with 0 score delvescore
			return;
		}

		double statCompensation = getStatCompensation(ServerProperties.getShardName(), mob.getLocation());

		//stat
		Player nearestPlayer = EntityUtils.getNearestPlayer(mob.getLocation(), 64);
		double healthMulti = DelvesUtils.isDelveMob(mob) ?
			getHealthMultiplier(level) * (ServerProperties.getClassSpecializationsEnabled(nearestPlayer) ? (ServerProperties.getAbilityEnhancementsEnabled(nearestPlayer) ? DELVE_MOB_STAT_MULTIPLIER_R3 : DELVE_MOB_STAT_MULTIPLIER_R2) : DELVE_MOB_STAT_MULTIPLIER_R1) :
			getHealthMultiplier(level) * statCompensation;


		EntityUtils.scaleMaxHealth(mob, healthMulti - 1, HEALTH_MODIFIER_NAME);

		Set<String> tags = mob.getScoreboardTags();
		if (!tags.contains(CrowdControlImmunityBoss.identityTag)) {
			EntityUtils.addAttribute(mob, Attribute.GENERIC_MOVEMENT_SPEED,
					new AttributeModifier(SPEED_MODIFIER_NAME, getSpeedMultiplier(level) - 1, Operation.MULTIPLY_SCALAR_1));
		}

		mob.addScoreboardTag(StatMultiplierBoss.identityTag);
		mob.addScoreboardTag(StatMultiplierBoss.identityTag + "[damagestatmult=" + statCompensation + ",damagemult=" + getDamageMultiplier(level) + "]");
	}

}
