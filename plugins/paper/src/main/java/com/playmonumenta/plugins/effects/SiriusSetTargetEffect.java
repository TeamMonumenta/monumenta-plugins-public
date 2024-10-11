package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.sirius.Sirius;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.List;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;

public class SiriusSetTargetEffect extends Effect {
	public static final String effectID = "SiriusSetTargetEffect";
	private final Sirius mSirius;
	private final LivingEntity mTarget;

	public SiriusSetTargetEffect(int duration, Sirius sirius, LivingEntity target) {
		super(duration, effectID);
		mSirius = sirius;
		mTarget = target;
	}

	@Override
	public void onTargetSwap(EntityTargetEvent event) {
		if (event.getTarget() != mTarget) {
			// Ignore everything other than target
			event.setTarget(mTarget);
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		List<Player> pList = mSirius.getPlayers();
		Mob mob = (Mob) entity;
		if (!pList.isEmpty()) {
			mob.setTarget(FastUtils.getRandomElement(pList));
		} else {
			mob.setTarget(null);
		}

	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		LivingEntity entity = event.getEntity().getKiller();
		if (entity != null) {
			Plugin.getInstance().mEffectManager.addEffect(entity, Sirius.PARTICIPATION_TAG, new CustomTimerEffect(30 * 20, "Participated").displays(false));
		}

	}

	@Override
	public String toString() {
		return String.format("SiriusSetTargetEffect duration:%d", this.getDuration());
	}
}
