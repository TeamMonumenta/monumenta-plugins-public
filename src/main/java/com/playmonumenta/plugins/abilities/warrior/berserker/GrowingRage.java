package com.playmonumenta.plugins.abilities.warrior.berserker;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.MessagingUtils;

/* Growing Rage: Getting hit by an attack increases the damage from
 * your attacks and skills by 10% / 15% for 5 s. This buff
 * can stack up to three times, with the cooldown refreshing
 * every time you are hit. At lvl 2, the effect of this
 * buff is doubled when you are below 40% health.
 */

public class GrowingRage extends Ability {

	private static final double GROWING_RAGE_1_DAMAGE_PERCENT = 0.1;
	private static final double GROWING_RAGE_2_DAMAGE_PERCENT = 0.15;
	private static final int GROWING_RAGE_DURATION = 4 * 20; //ticks
	private static final int GROWING_RAGE_STACK_LIMIT = 3;
	private static final double GROWING_RAGE_2_TRIGGER_HEALTH_PERCENT = 0.4;

	private int rageCurrentStack = 0;
	private int rageDuration = 0;

	public GrowingRage(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "Growing Rage";
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		int rage = getAbilityScore();
		double rageDamage = rageCurrentStack * (rage == 1 ? GROWING_RAGE_1_DAMAGE_PERCENT : GROWING_RAGE_2_DAMAGE_PERCENT) * event.getDamage();
		double maxHealth = mPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
		if (mPlayer.getHealth() > 0 && mPlayer.getHealth() <= GROWING_RAGE_2_TRIGGER_HEALTH_PERCENT * maxHealth) {
			rageDamage = rageDamage * 2;
		}
		event.setDamage(event.getDamage() + rageDamage);
		return true;
	}

	@Override
	public boolean PlayerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		if (!mPlayer.isDead() && mPlayer.getHealth() > 0) {
			if (rageCurrentStack < GROWING_RAGE_STACK_LIMIT) {
				rageCurrentStack++;
				if (rageCurrentStack == 1) {
					MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "You have 1 stack of Rage!");
				} else {
					MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "You have " + rageCurrentStack + " stacks of Rage!");
				}
			} else {
				MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Your " + rageCurrentStack + " stacks of Rage are refreshed!");
			}
			boolean run = rageDuration <= 0;
			rageDuration = GROWING_RAGE_DURATION;
			if (run) {
				new BukkitRunnable() {
					@Override
					public void run() {
						rageDuration--;
						if (rageDuration <= 0) {
							this.cancel();
							rageCurrentStack = 0;
							MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Your Rage has worn off!");
						}
					}

				}.runTaskTimer(mPlugin, 0, 1);
			}
		}
		return true;
	}
}