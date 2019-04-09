package com.playmonumenta.plugins.abilities.mage.arcanist;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.magic.AbilityCastEvent;
import com.playmonumenta.plugins.utils.InventoryUtils;

/*
 * Sage's Insight: All your melee wand hits do an additional
 * 1/2 damage and whenever you cast a spell, reduce the
 * cooldown of all other spells by 0.75. At level 2, taking
 * damage reduces the cooldown of your spells by 1.5 seconds
 * as well.
 */
public class SagesInsight extends Ability {

	private static final int ARCANIST_1_COOLDOWN_REDUCTION = 15;
	private static final int ARCANIST_2_COOLDOWN_REDUCTION = 30;
	private static final double ARCANIST_DAMAGE = 0.5;

	public SagesInsight(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "SagesInsight";
	}

	@Override
	public boolean AbilityCastEvent(AbilityCastEvent event) {
		mPlugin.mTimers.UpdateCooldowns(mPlayer, ARCANIST_1_COOLDOWN_REDUCTION);
		return true;
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (InventoryUtils.isWandItem(mPlayer.getInventory().getItemInMainHand())) {
			event.setDamage(event.getDamage() + ARCANIST_DAMAGE);
		}
		return true;
	}

	@Override
	public boolean PlayerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		if (getAbilityScore() > 1) {
			mPlugin.mTimers.UpdateCooldowns(mPlayer, ARCANIST_2_COOLDOWN_REDUCTION);
		}
		return true;
	}
}
