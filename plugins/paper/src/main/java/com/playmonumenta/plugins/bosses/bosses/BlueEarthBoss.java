package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.Arrays;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class BlueEarthBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_blueearth";
	public static final int detectionRange = 20;

	public static final double[] KB_RESIST = {0, 0.1, 0.15, 0.2};
	public static final String KB_RESIST_EFFECT_NAME = "BossBlueEarthKBResistEffect";

	private int mBlueTimeOfDay = 0;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new BlueEarthBoss(plugin, boss);
	}

	public BlueEarthBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		if (ScoreboardUtils.getScoreboardValue("$IsDungeon", "const").orElse(0) == 1) {
			long time = boss.getWorld().getTime();
			mBlueTimeOfDay = (int) Math.floor(time / 6000.0);

			// Pretty sure Time ranges from 0 to 23999, but just in case...
			if (mBlueTimeOfDay > 3) {
				mBlueTimeOfDay = 3;
			}
		}

		List<Spell> passiveSpells = Arrays.asList(
			new SpellRunAction(() -> {
				if (mBlueTimeOfDay > 0) {
					EffectManager.getInstance().addEffect(mBoss, KB_RESIST_EFFECT_NAME, new PercentKnockbackResist(100, KB_RESIST[mBlueTimeOfDay], KB_RESIST_EFFECT_NAME));
				}
			})
		);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null, 100, 20);
	}
}
