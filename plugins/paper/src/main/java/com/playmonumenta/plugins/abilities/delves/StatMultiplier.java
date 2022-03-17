package com.playmonumenta.plugins.abilities.delves;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.CrowdControlImmunityBoss;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntitySpawnEvent;

public class StatMultiplier extends DelveModifier {

	private static final String DELVES_PLAYER_TAG = "DelvesPlayer";

	private static final String HEALTH_MODIFIER_NAME = "DelveHealthModifier";
	private static final String SPEED_MODIFIER_NAME = "DelveSpeedModifier";

	private static final double DAMAGE_MULTIPLIER_INCREMENT = 0.03;
	private static final double HEALTH_MULTIPLIER_INCREMENT = 0.012;
	private static final double SPEED_MULTIPLIER_INCREMENT = 0.004;

	private static final Map<String, Double> STAT_COMPENSATION_MAPPINGS = new HashMap<>();

	private static final double DELVE_MOB_STAT_MULTIPLIER_R1 = 0.5;
	private static final double DELVE_MOB_STAT_MULTIPLIER_R2 = 1;

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

	private final double mStatCompensation;
	private final double mDelveMobStatMultiplier;
	private final double mDamageMultiplier;
	private final double mHealthMultiplier;
	private final double mSpeedMultiplier;

	public StatMultiplier(Plugin plugin, @Nullable Player player) {
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
	public void remove(Player player) {
		player.removeScoreboardTag(DELVES_PLAYER_TAG);
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

	public static boolean canUseStatic(Player player) {
		return DelvesUtils.getDelveInfo(player).getDepthPoints() > 0;
	}

	public static boolean isDepthsShard() {
		return ServerProperties.getShardName().contains("depths")
			|| ServerProperties.getShardName().equals("mobs")
			|| ServerProperties.getShardName().startsWith("dev");
	}

	@Override
	public boolean canUse(Player player) {
		return canUseStatic(player);
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (source == null) {
			// only scale entity damage, not environmental damage
			return;
		}

		String spellName = event.getBossSpellName();
		if (spellName != null && (Arcanic.SPELL_NAMES.contains(spellName) || Infernal.SPELL_NAMES.contains(spellName) || Transcendent.SPELL_NAMES.contains(spellName))) {
			// do not scale abilities from delve modifiers (no stat compensation, and they have their own region scaling)
			return;
		}

		if (DelvesUtils.isDelveMob(source)) {
			event.setDamage(event.getDamage() * mDelveMobStatMultiplier);
		} else {
			event.setDamage(event.getDamage() * mStatCompensation);
		}

		event.setDamage(event.getDamage() * mDamageMultiplier);
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
