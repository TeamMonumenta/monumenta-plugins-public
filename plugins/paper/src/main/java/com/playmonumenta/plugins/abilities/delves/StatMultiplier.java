package com.playmonumenta.plugins.abilities.delves;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntitySpawnEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.CrowdControlImmunityBoss;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.EntityUtils;

public class StatMultiplier extends DelveModifier {

	private static final String DELVES_PLAYER_TAG = "DelvesPlayer";

	private static final String HEALTH_MODIFIER_NAME = "DelveHealthModifier";
	private static final String SPEED_MODIFIER_NAME = "DelveSpeedModifier";

	private static final double DAMAGE_MULTIPLIER_INCREMENT = 0.03;
	private static final double HEALTH_MULTIPLIER_INCREMENT = 0.02;
	private static final double SPEED_MULTIPLIER_INCREMENT = 0.01;

	private static final Map<String, Double> STAT_COMPENSATION_MAPPINGS = new HashMap<>();

	private static final double DELVE_MOB_STAT_MULTIPLIER_R1 = 0.5;
	private static final double DELVE_MOB_STAT_MULTIPLIER_R2 = 1;

	static {
		STAT_COMPENSATION_MAPPINGS.put("white", 1.8);
		STAT_COMPENSATION_MAPPINGS.put("orange", 1.6);
		STAT_COMPENSATION_MAPPINGS.put("magenta", 1.5);
		STAT_COMPENSATION_MAPPINGS.put("lightblue", 1.2);
		STAT_COMPENSATION_MAPPINGS.put("yellow", 1.1);
		STAT_COMPENSATION_MAPPINGS.put("willows", 1.2);
		STAT_COMPENSATION_MAPPINGS.put("reverie", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("lime", 1.7);
		STAT_COMPENSATION_MAPPINGS.put("pink", 1.5);
		STAT_COMPENSATION_MAPPINGS.put("gray", 1.5);
		STAT_COMPENSATION_MAPPINGS.put("lightgray", 1.2);
		STAT_COMPENSATION_MAPPINGS.put("cyan", 1.2);
		STAT_COMPENSATION_MAPPINGS.put("purple", 1.1);
		STAT_COMPENSATION_MAPPINGS.put("teal", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("forum", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("shiftingcity", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("dev1", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("dev2", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("mobs", 1.0);
	}

	private final double mStatCompensation;
	private final double mDelveMobStatMultiplier;
	private final double mDamageMultiplier;
	private final double mHealthMultiplier;
	private final double mSpeedMultiplier;

	public StatMultiplier(Plugin plugin, Player player) {
		super(plugin, player, null);

		Double statCompensation = STAT_COMPENSATION_MAPPINGS.get(ServerProperties.getShardName());
		mStatCompensation = statCompensation == null ? 1 : statCompensation;

		mDelveMobStatMultiplier = ServerProperties.getClassSpecializationsEnabled()
				? DELVE_MOB_STAT_MULTIPLIER_R2 : DELVE_MOB_STAT_MULTIPLIER_R1;

		if (player != null) {
			int points = DelvesUtils.getDelveInfo(player).getDepthPoints();
			mDamageMultiplier = getDamageMultiplier(points);
			mHealthMultiplier = getHealthMultiplier(points);
			mSpeedMultiplier = getSpeedMultiplier(points);

			player.addScoreboardTag(DELVES_PLAYER_TAG);
		} else {
			mDamageMultiplier = 0;
			mHealthMultiplier = 0;
			mSpeedMultiplier = 0;
		}
	}

	@Override
	public void invalidate() {
		mPlayer.removeScoreboardTag(DELVES_PLAYER_TAG);
	}

	public static double getStatCompensation(String dungeon) {
		return STAT_COMPENSATION_MAPPINGS.get(dungeon);
	}

	public static double getDamageMultiplier(int depthPoints) {
		return 1 + Math.min(DelvesUtils.getLootCapDepthPoints(9001), depthPoints) * DAMAGE_MULTIPLIER_INCREMENT;
	}

	public static double getHealthMultiplier(int depthPoints) {
		return 1 + Math.min(DelvesUtils.getLootCapDepthPoints(9001), depthPoints) * HEALTH_MULTIPLIER_INCREMENT;
	}

	public static double getSpeedMultiplier(int depthPoints) {
		return 1 + Math.min(DelvesUtils.getLootCapDepthPoints(9001), depthPoints) * SPEED_MULTIPLIER_INCREMENT;
	}

	public static boolean canUseStatic(Player player) {
		return DelvesUtils.getDelveInfo(player).getDepthPoints() > 0;
	}

	@Override
	public boolean canUse(Player player) {
		return canUseStatic(player);
	}

	@Override
	protected boolean playerTookCustomDamageEvent(EntityDamageByEntityEvent event) {
		return modifyDamage(event.getDamager(), event);
	}

	@Override
	protected boolean playerTookMeleeDamageEvent(EntityDamageByEntityEvent event) {
		return modifyDamage(event.getDamager(), event);
	}

	@Override
	protected boolean playerTookProjectileDamageEvent(Entity source, EntityDamageByEntityEvent event) {
		return modifyDamage(source, event);
	}

	private boolean modifyDamage(Entity source, EntityDamageByEntityEvent event) {
		if (DelvesUtils.isDelveMob(source) || event.getCause() == DamageCause.CUSTOM) {
			event.setDamage(event.getDamage() * mDelveMobStatMultiplier);
		} else {
			event.setDamage(EntityUtils.getDamageApproximation(event, mStatCompensation));
		}

		event.setDamage(EntityUtils.getDamageApproximation(event, mDamageMultiplier));

		return true;
	}

	@Override
	public void applyModifiers(LivingEntity mob, EntitySpawnEvent event) {
		EntityUtils.scaleMaxHealth(mob, DelvesUtils.isDelveMob(mob) ?
				mHealthMultiplier * mDelveMobStatMultiplier - 1 : mHealthMultiplier * mStatCompensation - 1, HEALTH_MODIFIER_NAME);

		Set<String> tags = mob.getScoreboardTags();
		if (tags == null || !tags.contains(CrowdControlImmunityBoss.identityTag)) {
			EntityUtils.addAttribute(mob, Attribute.GENERIC_MOVEMENT_SPEED,
					new AttributeModifier(SPEED_MODIFIER_NAME, mSpeedMultiplier - 1, Operation.MULTIPLY_SCALAR_1));
		}
	}

}
