package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.Warlock;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;


public class Culling extends Ability {

	private static final int PASSIVE_DURATION = 6 * 20;
	private static final String WARLOCK_PASSIVE_EFFECT_NAME = "CullingPercentDamageResistEffect";
	private static final double WARLOCK_PASSIVE_DAMAGE_REDUCTION_PERCENT = -0.1;

	public static final AbilityInfo<Culling> INFO =
		new AbilityInfo<>(Culling.class, null, Culling::new)
			.canUse(player -> ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME).orElse(0) == Warlock.CLASS_ID);

	public Culling(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (EntityUtils.isHostileMob(event.getEntity())
			    && ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand())) {
			mPlugin.mEffectManager.addEffect(mPlayer, WARLOCK_PASSIVE_EFFECT_NAME, new PercentDamageReceived(PASSIVE_DURATION, WARLOCK_PASSIVE_DAMAGE_REDUCTION_PERCENT));
		}
	}
}
