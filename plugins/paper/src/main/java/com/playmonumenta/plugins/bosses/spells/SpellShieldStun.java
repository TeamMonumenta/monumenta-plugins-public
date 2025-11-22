package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.enchantments.Shielding;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.itemstats.infusions.Sturdy;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import org.bukkit.Sound;
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
				int finalStunTicks = Sturdy.updateStunCooldown(mStunTicks, Plugin.getInstance().mItemStatManager.getInfusionLevel(player, InfusionType.STURDY));
				NmsUtils.getVersionAdapter().stunShield(player, finalStunTicks);
				player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 0.8f, 0.8f + FastUtils.RANDOM.nextFloat() * 0.4F);
				event.setFlatDamage(0);
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
