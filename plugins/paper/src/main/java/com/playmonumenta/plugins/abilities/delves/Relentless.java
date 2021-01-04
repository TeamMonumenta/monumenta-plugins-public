package com.playmonumenta.plugins.abilities.delves;

import java.util.Set;

import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntitySpawnEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.CrowdControlImmunityBoss;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.EntityUtils;

public class Relentless extends DelveModifier {

	private static final String SPEED_MODIFIER_NAME = "RelentlessSpeedModifier";

	private static final double[] SPEED_MODIFIER = {
			0.1,
			0.2,
			0.3,
			0.4,
			0.5
	};

	public static final String DESCRIPTION = "Enemies are faster.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Enemies gain " + Math.round(SPEED_MODIFIER[0] * 100) + "% Speed."
			}, {
				"Enemies gain " + Math.round(SPEED_MODIFIER[1] * 100) + "% Speed."
			}, {
				"Enemies gain " + Math.round(SPEED_MODIFIER[2] * 100) + "% Speed."
			}, {
				"Enemies gain " + Math.round(SPEED_MODIFIER[3] * 100) + "% Speed."
			}, {
				"Enemies gain " + Math.round(SPEED_MODIFIER[4] * 100) + "% Speed."
			}
	};

	private final double mSpeedModifier;

	public Relentless(Plugin plugin, Player player) {
		super(plugin, player, Modifier.RELENTLESS);

		int rank = DelvesUtils.getDelveInfo(player).getRank(Modifier.RELENTLESS);
		mSpeedModifier = SPEED_MODIFIER[rank - 1];
	}

	@Override
	public void applyModifiers(LivingEntity mob, EntitySpawnEvent event) {
		if (mob instanceof Attributable) {
			Set<String> tags = mob.getScoreboardTags();
			if (tags == null || !tags.contains(CrowdControlImmunityBoss.identityTag)) {
				EntityUtils.addAttribute(mob, Attribute.GENERIC_MOVEMENT_SPEED,
						new AttributeModifier(SPEED_MODIFIER_NAME, mSpeedModifier, Operation.MULTIPLY_SCALAR_1));
			}
		}
	}

}
