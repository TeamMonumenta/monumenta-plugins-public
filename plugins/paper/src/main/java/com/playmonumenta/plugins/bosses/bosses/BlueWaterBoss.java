package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.Arrays;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class BlueWaterBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_bluewater";
	public static final int detectionRange = 20;

	public static final double[] REGEN_HEAL = {0, 5, 7, 10};

	private int mBlueTimeOfDay = 0;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new BlueWaterBoss(plugin, boss);
	}

	public BlueWaterBoss(Plugin plugin, LivingEntity boss) {
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
				if (!mBoss.isDead() && mBlueTimeOfDay > 0) {
					mBoss.setHealth(Math.min(mBoss.getHealth() + REGEN_HEAL[mBlueTimeOfDay], EntityUtils.getMaxHealth(mBoss)));
				}
			})
		);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null, 100, 20);
	}
}
