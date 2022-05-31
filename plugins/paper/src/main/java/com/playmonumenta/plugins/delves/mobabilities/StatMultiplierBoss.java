package com.playmonumenta.plugins.delves.mobabilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.BossParameters;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.delves.abilities.Arcanic;
import com.playmonumenta.plugins.delves.abilities.Infernal;
import com.playmonumenta.plugins.delves.abilities.StatMultiplier;
import com.playmonumenta.plugins.delves.abilities.Transcendent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import java.util.Collections;
import org.bukkit.entity.LivingEntity;

public class StatMultiplierBoss extends BossAbilityGroup {
	public static final String identityTag = "StatMultiplierBoss";

	public static class Parameters extends BossParameters {
		public double DAMAGE_STAT_MULT = 1;
		public double DAMAGE_MULT = 1;
	}

	private final double mDamageRegionStatMult;
	private final double mDamageStatMult;
	private final double mDamageMultiplier;

	public StatMultiplierBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = Parameters.getParameters(boss, identityTag, new Parameters());
		mDamageRegionStatMult = (ServerProperties.getClassSpecializationsEnabled() ? StatMultiplier.DELVE_MOB_STAT_MULTIPLIER_R2 : StatMultiplier.DELVE_MOB_STAT_MULTIPLIER_R1);
		mDamageStatMult = p.DAMAGE_STAT_MULT;
		mDamageMultiplier = p.DAMAGE_MULT;

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), -1, null);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		String spellName = event.getBossSpellName();
		if (!(spellName != null && (Arcanic.SPELL_NAMES.contains(spellName) || Infernal.SPELL_NAMES.contains(spellName) || Transcendent.SPELL_NAMES.contains(spellName)))) {
			// Abilities from delve modifiers only scale based on points, and not from stat compensation or region scaling (they have their own region scaling)
			if (DelvesUtils.isDelveMob(mBoss)) {
				// Delve mobs get point and region scaling
				event.setDamage(event.getDamage() * mDamageRegionStatMult);
			} else {
				// Other entity damage gets scaled according to stat compensation as well as points
				event.setDamage(event.getDamage() * mDamageStatMult);
			}

		}

		event.setDamage(event.getDamage() * mDamageMultiplier);
	}
}
