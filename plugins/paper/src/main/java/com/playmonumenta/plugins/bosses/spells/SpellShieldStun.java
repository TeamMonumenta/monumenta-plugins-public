package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.enchantments.Shielding;
import com.playmonumenta.plugins.utils.NmsUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SpellShieldStun extends Spell {

	private final int mStunTicks;
	private final DamageType mDamageType;

	public SpellShieldStun(int stunTicks) {
		this(stunTicks, DamageType.MELEE);
	}

	public SpellShieldStun(int stunTicks, DamageType damageType) {
		mStunTicks = stunTicks;
		mDamageType = damageType;
	}

	@Override
	public void run() {

	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (damagee instanceof Player player && event.getType() == mDamageType) {
			if (event.isBlockedByShield()) {
				NmsUtils.getVersionAdapter().stunShield(player, mStunTicks);
				event.setDamage(0);
			} else if (Shielding.doesShieldingApply(player, damagee) && event.getDamage() > 0) {
				Shielding.disable(player);
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
