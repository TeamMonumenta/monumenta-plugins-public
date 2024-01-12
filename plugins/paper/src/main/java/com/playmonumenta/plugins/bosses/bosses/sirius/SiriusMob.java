package com.playmonumenta.plugins.bosses.bosses.sirius;

import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.effects.CustomTimerEffect;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.EffectManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class SiriusMob extends BossAbilityGroup {

	public static final String identityTag = "boss_siriusmob";
	public static final String MOB_KILLED_PARTICIPATION_TAG = "mob_killed_participation";
	private static final int MOBS_TO_PARTICIPATE = 15;

	public SiriusMob(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
	}


	@Override
	public void death(@Nullable EntityDeathEvent event) {
		if (event != null && event.getEntity() instanceof Player player) {
			EffectManager manager = com.playmonumenta.plugins.Plugin.getInstance().mEffectManager;
			Effect effect = manager.getActiveEffect(player, MOB_KILLED_PARTICIPATION_TAG);
			if (effect == null) {
				manager.addEffect(player, MOB_KILLED_PARTICIPATION_TAG, new CustomTimerEffect(2 * 60 * 20, 1, "").displays(false).deleteOnLogout(true));
			} else if (effect.getMagnitude() > MOBS_TO_PARTICIPATE) {
				manager.addEffect(player, Sirius.PARTICIPATION_TAG, new CustomTimerEffect(2 * 60 * 20, "Participated").displays(false));
			} else {
				double mag = effect.getMagnitude();
				manager.clearEffects(player, MOB_KILLED_PARTICIPATION_TAG);
				manager.addEffect(player, MOB_KILLED_PARTICIPATION_TAG, new CustomTimerEffect(2 * 60 * 20, (int) (mag + 1), "").displays(false).deleteOnLogout(true));
			}
		}
	}
}
