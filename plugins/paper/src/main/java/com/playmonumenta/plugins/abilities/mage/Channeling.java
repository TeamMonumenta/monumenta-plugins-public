package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;


public class Channeling extends Ability {

	public static final double PERCENT_MELEE_INCREASE = 0.2;

	public static final AbilityInfo<Channeling> INFO =
		new AbilityInfo<>(Channeling.class, null, Channeling::new)
			.canUse(player -> ScoreboardUtils.getScoreboardValue(player, "Class").orElse(0) == 1)
			.priorityAmount(999);

	private boolean mCast = false;

	public Channeling(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		mCast = true;
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
