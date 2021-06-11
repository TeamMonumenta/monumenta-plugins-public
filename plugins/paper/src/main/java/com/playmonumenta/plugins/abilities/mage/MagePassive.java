package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;



public class MagePassive extends Ability {

	private static final double PERCENT_MELEE_INCREASE = 0.2;

	private boolean mCast = false;

	public MagePassive(Plugin plugin, Player player) {
		super(plugin, player, null);
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "Class") == 1;
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		if (!mCast) {
			mCast = true;
		}
		return true;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			if (ItemUtils.isWand(mainHand) && mCast) {
				event.setDamage((event.getDamage() * (1 + PERCENT_MELEE_INCREASE)));
				mCast = false;
			}
		}
		return true;
	}
}