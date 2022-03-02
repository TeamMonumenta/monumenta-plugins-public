package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.enchantments.Shielding;
import com.playmonumenta.plugins.utils.NmsUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SpellShieldStun extends Spell {

	private int mStunTicks;

	public SpellShieldStun(int stunTicks) {
		mStunTicks = stunTicks;
	}

	@Override
	public void run() {

	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (damagee instanceof Player player && event.getType() == DamageType.MELEE) {
			if (event.isBlockedByShield()) {
				NmsUtils.getVersionAdapter().stunShield(player, mStunTicks);
				event.setDamage(0);
			} else if (Shielding.doesShieldingApply(player, damagee)) {
				Shielding.disable(player);
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
