package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellShieldStun;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import java.util.List;
import org.bukkit.entity.LivingEntity;

public class ShieldStunBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_shieldstun";

	public static class Parameters extends BossParameters {
		@BossParam(help = "The number of ticks the shield stun lasts")
		public int DURATION = 5 * 20;
		public DamageType DAMAGE_TYPE = DamageType.MELEE;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ShieldStunBoss(plugin, boss);
	}

	public ShieldStunBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(mBoss, identityTag, new Parameters());
		Spell spell = new SpellShieldStun(p.DURATION, p.DAMAGE_TYPE);
		super.constructBoss(SpellManager.EMPTY, List.of(spell), 10, null, 0);
	}
}
