package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.Arrays;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
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
		Player nearestPlayer = EntityUtils.getNearestPlayer(boss.getLocation(), 30);
		mBlueTimeOfDay = ScoreboardUtils.getScoreboardValue(nearestPlayer, "BlueTimeOfDay").orElse(0);
		mBlueTimeOfDay = Math.min(3, Math.max(0, mBlueTimeOfDay));

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
