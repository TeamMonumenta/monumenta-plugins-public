package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class BlueWaterBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_bluewater";
	public static final int detectionRange = 20;

	public static final double[] REGEN_HEAL = {0, 5, 7, 10};

	private int mBlueTimeOfDay = 0;

	public BlueWaterBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		mBlueTimeOfDay = BossUtils.getBlueTimeOfDay(boss);

		List<Spell> passiveSpells = List.of(
			new SpellRunAction(() -> {
				if (!mBoss.isDead() && mBlueTimeOfDay > 0) {
					EntityUtils.healMob(mBoss, REGEN_HEAL[mBlueTimeOfDay]);
				}
			})
		);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null, 100, 20);
	}
}
