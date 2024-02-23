package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;

public final class FacingBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_facing";

	public static class Parameters extends BossParameters {

		@BossParam(help = "prefers aggro target")
		public boolean PREFER_TARGET = true;
		@BossParam(help = "targets of the spell")
		public EntityTargets TARGET = EntityTargets.GENERIC_PLAYER_TARGET;
	}

	public FacingBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		final Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());
		List<? extends LivingEntity> targets = p.TARGET.getTargetsList(mBoss);
		if (targets == null || targets.isEmpty()) {
			return;
		}
		LivingEntity facingTarget = targets.get(0);

		if (p.PREFER_TARGET && ((Mob) boss).getTarget() != null) {
			facingTarget = ((Mob) boss).getTarget();
		}

		LivingEntity finalFacingTarget = facingTarget;
		List<Spell> passiveSpells = List.of(new SpellRunAction(() -> {
			Location loc = boss.getLocation();
			Vector targetDir = finalFacingTarget.getLocation().toVector().subtract(loc.toVector());
			double[] targetYawPitch = VectorUtils.vectorToRotation(targetDir);
			boss.setRotation((float) targetYawPitch[0], (float) targetYawPitch[1]);
		}));

		super.constructBoss(SpellManager.EMPTY, passiveSpells, -1, null, 0, 1);
	}
}

