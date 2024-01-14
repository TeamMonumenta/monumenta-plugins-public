package com.playmonumenta.plugins.bosses.bosses.sirius;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.CrowdControlImmunity;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBasePassiveAbility;
import com.playmonumenta.plugins.bosses.spells.sirius.PassiveStarBlightConversion;
import com.playmonumenta.plugins.bosses.spells.sirius.miniboss.SpellStarblightCharge;
import com.playmonumenta.plugins.bosses.spells.sirius.miniboss.SpellSwordCleave;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class SiriusMeleeBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_siriusmeleeboss";
	private @Nullable PassiveStarBlightConversion mConverter;
	private @Nullable Sirius mSirius;
	private static final int DETECTIONRANGE = 80;

	public SiriusMeleeBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mBoss.getLocation(), 100, EnumSet.of(EntityType.SLIME));
		for (LivingEntity mob : mobs) {
			if (mob.getScoreboardTags().contains(Sirius.identityTag)) {
				Sirius sirius = BossUtils.getBossOfClass(mob, Sirius.class);
				if (sirius == null) {
					MMLog.warning("SiriusMeleeBoss: Didn't find Sirius! (Likely bug)");
				} else {
					mConverter = sirius.mStarBlightConverter;
					mSirius = sirius;
				}
			}
		}

		if (mConverter != null && mSirius != null) {
			SpellManager activeSpells = new SpellManager(List.of(
				new SpellSwordCleave(plugin, mConverter, mBoss),
				new SpellStarblightCharge(plugin, mBoss, mConverter, mSirius)
			));

			List<Spell> passiveSpells = List.of(
				//Add maybe some sort of counter strike
				new SpellBasePassiveAbility(4 * 20, new CrowdControlImmunity(mBoss))
			);

			super.constructBoss(activeSpells, passiveSpells, DETECTIONRANGE, null);
		} else {
			MMLog.warning("SiriusMeleeBoss: Didn't find StarblightConverter! (Likely bug)");

		}

	}

}
