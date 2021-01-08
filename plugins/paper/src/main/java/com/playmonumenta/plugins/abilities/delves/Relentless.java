package com.playmonumenta.plugins.abilities.delves;

import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntitySpawnEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.BlockBreakBoss;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;

public class Relentless extends DelveModifier {

	private static final String KBR_MODIFIER_NAME = "RelentlessKBRModifier";

	private static final double[] KBR_MODIFIER = {
			0.1,
			0.2,
			0.3,
			0.4,
			0.5
	};

	private static final double[] BLOCK_BREAK_CHANCE = {
			0.02,
			0.04,
			0.06,
			0.08,
			0.1
	};

	public static final String DESCRIPTION = "Enemies are harder to stop.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Enemies gain " + KBR_MODIFIER[0] + " Knockback Resistance.",
				"Enemies have a " + Math.round(BLOCK_BREAK_CHANCE[0] * 100) + "% chance to have Block Break."
			}, {
				"Enemies gain " + KBR_MODIFIER[1] + " Knockback Resistance.",
				"Enemies have a " + Math.round(BLOCK_BREAK_CHANCE[1] * 100) + "% chance to have Block Break."
			}, {
				"Enemies gain " + KBR_MODIFIER[2] + " Knockback Resistance.",
				"Enemies have a " + Math.round(BLOCK_BREAK_CHANCE[2] * 100) + "% chance to have Block Break."
			}, {
				"Enemies gain " + KBR_MODIFIER[3] + " Knockback Resistance.",
				"Enemies have a " + Math.round(BLOCK_BREAK_CHANCE[3] * 100) + "% chance to have Block Break."
			}, {
				"Enemies gain " + KBR_MODIFIER[4] + " Knockback Resistance.",
				"Enemies have a " + Math.round(BLOCK_BREAK_CHANCE[4] * 100) + "% chance to have Block Break."
			}
	};

	private final double mKBRModifier;
	private final double mBlockBreakChance;

	public Relentless(Plugin plugin, Player player) {
		super(plugin, player, Modifier.RELENTLESS);

		int rank = DelvesUtils.getDelveInfo(player).getRank(Modifier.RELENTLESS);
		mKBRModifier = KBR_MODIFIER[rank - 1];
		mBlockBreakChance = BLOCK_BREAK_CHANCE[rank - 1];
	}

	@Override
	public void applyModifiers(LivingEntity mob, EntitySpawnEvent event) {
		if (mob instanceof Attributable) {
			EntityUtils.addAttribute(mob, Attribute.GENERIC_KNOCKBACK_RESISTANCE,
					new AttributeModifier(KBR_MODIFIER_NAME, mKBRModifier, Operation.ADD_NUMBER));
		}

		if (FastUtils.RANDOM.nextDouble() < mBlockBreakChance) {
			mob.addScoreboardTag(BlockBreakBoss.identityTag);
		}
	}

}
