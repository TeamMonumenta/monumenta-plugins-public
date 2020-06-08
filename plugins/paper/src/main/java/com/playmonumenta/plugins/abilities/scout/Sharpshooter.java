package com.playmonumenta.plugins.abilities.scout;

import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.utils.MessagingUtils;

/*
 * Sharpshooter: Each successful, fully charged arrow hit increases your arrow
 * damage by 1, up to a max of 5. After 4 seconds without a successful hit, the
 * bonus is reduced by 1. At level 2, each hit increases damage by 2 up to 8.
 *
 * TODO: This damage buff stacks with Enchanted Arrow and other bow skills.
 */

public class Sharpshooter extends Ability {
	private static final int SHARPSHOOTER_DECAY_TIMER = 20 * 4;
	private static final int SHARPSHOOTER_MAX_STACKS = 10;
	private static final double SHARPSHOOTER_1_INCREMENT = 0.03;
	private static final double SHARPSHOOTER_2_INCREMENT = 0.05;

	private final double mDamageBonusPerStack;

	public Sharpshooter(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Sharpshooter");
		mInfo.mScoreboardId = "Sharpshooter";
		mInfo.mShorthandName = "Ss";
		mInfo.mDescriptions.add("Each enemy hit with a critical arrow gives you a stack of Sharpshooter, up to 10. Stacks decay after 4 seconds of not gaining a stack. Each stack makes your arrows deal 3% more damage.");
		mInfo.mDescriptions.add("Damage per stack increased to 5%.");
		mInfo.mIgnoreTriggerCap = true;

		mDamageBonusPerStack = getAbilityScore() == 1 ? SHARPSHOOTER_1_INCREMENT : SHARPSHOOTER_2_INCREMENT;
	}

	private int mStacks = 0;
	private int mTicksToStackDecay = 0;

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity damagee, EntityDamageByEntityEvent event) {
		if (proj instanceof Arrow) {
			Arrow arrow = (Arrow) proj;

			// Critical arrow and mob is actually going to take damage
			if (arrow.isCritical() && (damagee.getNoDamageTicks() <= 10 || damagee.getLastDamage() < event.getDamage())) {
				mTicksToStackDecay = SHARPSHOOTER_DECAY_TIMER;

				if (mStacks < SHARPSHOOTER_MAX_STACKS) {
					mStacks++;
					MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Sharpshooter Stacks: " + mStacks);
				}
			}

			event.setDamage(event.getDamage() * (1 + mStacks * mDamageBonusPerStack));
		}

		return true;
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (mStacks > 0) {
			mTicksToStackDecay -= 5;

			if (mTicksToStackDecay <= 0) {
				mTicksToStackDecay = SHARPSHOOTER_DECAY_TIMER;
				mStacks--;
				MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Sharpshooter Stacks: " + mStacks);
			}
		}
	}

	public static double getDamageMultiplier(Player player) {
		Sharpshooter ss = AbilityManager.getManager().getPlayerAbility(player, Sharpshooter.class);
		return ss == null ? 1 : (1 + ss.mStacks * ss.mDamageBonusPerStack);
	}

}
