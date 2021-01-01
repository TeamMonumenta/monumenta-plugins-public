package com.playmonumenta.plugins.abilities.delves;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.attribute.Attributable;
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
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.EntityUtils;

public class StatMultiplier extends DelveModifier {

	private static final String STAT_MULTIPLIER_MODIFIER_NAME = "DelveStatMultiplier";
	private static final double STAT_MULTIPLIER_INCREMENT = 0.04;
	private static final Map<String, Double> STAT_COMPENSATION_MAPPINGS = new HashMap<>();

	protected static final String DELVE_MOB_TAG = "delve_mob";
	private static final double DELVE_MOB_STAT_MULTIPLIER_R1 = 0.6;
	private static final double DELVE_MOB_STAT_MULTIPLIER_R2 = 1;
	private static final String DELVE_MOB_HEALTH_MODIFIER_NAME = "DelveMobHealthModifier";

	static {
		STAT_COMPENSATION_MAPPINGS.put("white", 1.6);
		STAT_COMPENSATION_MAPPINGS.put("orange", 1.6);
		STAT_COMPENSATION_MAPPINGS.put("magenta", 1.4);
		STAT_COMPENSATION_MAPPINGS.put("lightblue", 1.1);
		STAT_COMPENSATION_MAPPINGS.put("yellow", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("willows", 1.1);
		STAT_COMPENSATION_MAPPINGS.put("reverie", 0.9);
		STAT_COMPENSATION_MAPPINGS.put("lime", 1.6);
		STAT_COMPENSATION_MAPPINGS.put("pink", 1.4);
		STAT_COMPENSATION_MAPPINGS.put("gray", 1.4);
		STAT_COMPENSATION_MAPPINGS.put("lightgray", 1.1);
		STAT_COMPENSATION_MAPPINGS.put("cyan", 1.1);
		STAT_COMPENSATION_MAPPINGS.put("purple", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("teal", 0.9);
		STAT_COMPENSATION_MAPPINGS.put("shiftingcity", 0.9);
		STAT_COMPENSATION_MAPPINGS.put("dev1", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("dev2", 1.0);
		STAT_COMPENSATION_MAPPINGS.put("mobs", 1.0);
	}

	private final double mStatCompensation;
	private final double mStatMultiplier;
	private final double mDelveMobStatMultiplier;

	public StatMultiplier(Plugin plugin, Player player) {
		super(plugin, player, null);

		Double statCompensation = STAT_COMPENSATION_MAPPINGS.get(ServerProperties.getShardName());
		mStatCompensation = statCompensation == null ? 1 : statCompensation;

		mStatMultiplier = getStatMultiplier(DelvesUtils.getDelveInfo(player).getDepthPoints());

		mDelveMobStatMultiplier = ServerProperties.getClassSpecializationsEnabled()
				? DELVE_MOB_STAT_MULTIPLIER_R2 : DELVE_MOB_STAT_MULTIPLIER_R1;
	}

	public static double getStatCompensation(String dungeon) {
		return STAT_COMPENSATION_MAPPINGS.get(dungeon);
	}

	public static double getStatMultiplier(int depthPoints) {
		return 1 + Math.min(DelvesUtils.getLootCapDepthPoints(9001), depthPoints) * STAT_MULTIPLIER_INCREMENT;
	}

	public static boolean canUseStatic(Player player) {
		return DelvesUtils.getDelveInfo(player).getDepthPoints() > 0;
	}

	@Override
	public boolean canUse(Player player) {
		return canUseStatic(player);
	}

	@Override
	protected void playerTookCustomDamageEvent(EntityDamageByEntityEvent event) {
		modifyDamage(event.getDamager(), event);
	}

	@Override
	protected void playerTookMeleeDamageEvent(EntityDamageByEntityEvent event) {
		modifyDamage(event.getDamager(), event);
	}

	@Override
	protected void playerTookProjectileDamageEvent(Entity source, EntityDamageByEntityEvent event) {
		modifyDamage(source, event);
	}

	private void modifyDamage(Entity source, EntityDamageByEntityEvent event) {
		Set<String> tags = source.getScoreboardTags();
		if (tags != null && tags.contains(DELVE_MOB_TAG) || event.getCause() == DamageCause.CUSTOM) {
			event.setDamage(event.getDamage() * mDelveMobStatMultiplier);
		} else {
			event.setDamage(EntityUtils.getDamageApproximation(event, mStatCompensation));
		}

		event.setDamage(EntityUtils.getDamageApproximation(event, mStatMultiplier));
	}

	@Override
	public void applyModifiers(LivingEntity mob, EntitySpawnEvent event) {
		if (mob instanceof Attributable) {
			double healthProportion = Math.min(1, mob.getHealth() / mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());

			EntityUtils.addAttribute(mob, Attribute.GENERIC_MAX_HEALTH,
					new AttributeModifier(STAT_MULTIPLIER_MODIFIER_NAME, mStatMultiplier * mStatCompensation - 1, Operation.MULTIPLY_SCALAR_1));

			Set<String> tags = mob.getScoreboardTags();
			if (tags != null && tags.contains(DELVE_MOB_TAG)) {
				EntityUtils.addAttribute(mob, Attribute.GENERIC_MAX_HEALTH,
						new AttributeModifier(DELVE_MOB_HEALTH_MODIFIER_NAME, mDelveMobStatMultiplier - 1, Operation.MULTIPLY_SCALAR_1));
			}

			mob.setHealth(healthProportion * mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
		}
	}

}
