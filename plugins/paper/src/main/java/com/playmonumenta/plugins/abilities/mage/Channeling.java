package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import javax.annotation.Nullable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;


public class Channeling extends Ability {

	public static final double PERCENT_MELEE_INCREASE = 0.2;

	private boolean mCast = false;

	public Channeling(Plugin plugin, @Nullable Player player) {
		super(plugin, player, null);
	}

	@Override
	public double getPriorityAmount() {
		return 999;
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "Class").orElse(0) == 1;
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		if (!mCast) {
			mCast = true;
		}
		return true;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE
			    && mCast
			    && mPlugin.mItemStatManager.getPlayerItemStats(mPlayer).getItemStats().get(ItemStatUtils.EnchantmentType.MAGIC_WAND) > 0) {
			event.setDamage((event.getDamage() * (1 + PERCENT_MELEE_INCREASE)));
			mCast = false;
		}
		return false; // only changes event damage
	}
}
