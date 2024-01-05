package com.playmonumenta.plugins.bosses.bosses.sirius;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.CrowdControlImmunity;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBasePassiveAbility;
import com.playmonumenta.plugins.bosses.spells.sirius.PassiveStarBlightConversion;
import com.playmonumenta.plugins.bosses.spells.sirius.miniboss.SpellStarblightGround;
import com.playmonumenta.plugins.bosses.spells.sirius.miniboss.SpellStarblightLeap;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class SiriusJumpBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_siriusjumpboss";
	private @Nullable PassiveStarBlightConversion mConverter;
	private static final int DETECTIONRANGE = 80;

	public SiriusJumpBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mBoss.getLocation(), 100, EnumSet.of(EntityType.SLIME));
		for (LivingEntity mob : mobs) {
			if (mob.getScoreboardTags().contains(Sirius.identityTag)) {
				Sirius mSirius = BossUtils.getBossOfClass(mob, Sirius.class);
				if (mSirius == null) {
					MMLog.warning("SiriusJumpBoss: Didn't find Sirius! (Likely bug)");
				} else {
					mConverter = mSirius.mStarBlightConverter;
				}
			}
		}

		if (mConverter != null) {
			SpellManager activeSpells = new SpellManager(List.of(
				new SpellStarblightLeap(boss, plugin)
				//Probably needs a jump Spell or a tanking spell
			));

			List<Spell> passiveSpells = List.of(
				new SpellStarblightGround(boss, mConverter),
				new SpellBasePassiveAbility(4 * 20, new CrowdControlImmunity(mBoss))
			);

			super.constructBoss(activeSpells, passiveSpells, DETECTIONRANGE, null);
		} else {
			MMLog.warning("SiriusJumpBoss: Didn't find StarblightConverter! (Likely bug)");

		}

	}
}
