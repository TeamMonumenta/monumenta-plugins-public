package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FacingBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_facing";

	public static class Parameters extends BossParameters {

		@BossParam(help = "continuously faces aggro target")
		public boolean PREFER_TARGET = true;
		@BossParam(help = "only set head rotation")
		public boolean HEAD_ROTATION = false;
		@BossParam(help = "targets of the spell")
		public EntityTargets TARGET = EntityTargets.GENERIC_PLAYER_TARGET;
	}

	@Nullable
	private LivingEntity mTarget = null;

	public FacingBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		final Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		List<Spell> passiveSpells = List.of(new SpellRunAction(() -> {
			if (p.PREFER_TARGET && boss instanceof Mob mob && mob.getTarget() != null) {
				mTarget = mob.getTarget();
			}
			if (mTarget == null || !mTarget.isValid() || (mTarget instanceof Player player && AbilityUtils.isStealthed(player))) {
				List<? extends @NotNull LivingEntity> targetsList = p.TARGET.getTargetsList(boss);
				if (targetsList.isEmpty()) {
					return;
				}
				mTarget = targetsList.getFirst();
			}
			Location loc = boss.getLocation();
			Vector targetDir = mTarget.getLocation().toVector().subtract(loc.toVector());
			if (targetDir.lengthSquared() == 0) {
				return;
			}
			double[] targetYawPitch = VectorUtils.vectorToRotation(targetDir);
			if (p.HEAD_ROTATION) {
				NmsUtils.getVersionAdapter().setHeadRotation(boss, (float) targetYawPitch[0], (float) targetYawPitch[1]);
			} else {
				boss.setRotation((float) targetYawPitch[0], (float) targetYawPitch[1]);
			}
		}));

		super.constructBoss(SpellManager.EMPTY, passiveSpells, -1, null, 0, 1);
	}
}
