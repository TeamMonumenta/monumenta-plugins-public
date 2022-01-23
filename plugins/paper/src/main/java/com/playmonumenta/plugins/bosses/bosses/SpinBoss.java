package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public final class SpinBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_spin";

	public static class Parameters extends BossParameters {
		/**
		 * Rate of spin in degrees per tick
		 */
		public int SPIN_RATE = 10;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new SpinBoss(plugin, boss);
	}

	public SpinBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		final Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		List<Spell> passiveSpells = Arrays.asList(new SpellRunAction(() -> {
			Location loc = boss.getLocation();
			loc.setYaw((loc.getYaw() + p.SPIN_RATE));
			boss.teleport(loc);
		}));

		super.constructBoss(SpellManager.EMPTY, passiveSpells, -1, null, 100, 1);
	}
}

