package com.playmonumenta.plugins.abilities.rogue.swordsage;

import java.util.Random;

import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.magic.AbilityCastEvent;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class DeadlyRonde extends Ability {

	/*
	 * Deadly Ronde: After using a skill, your next sword
	 * attack deals 4 / 6 extra damage, also adding half of
	 * that bonus to sweeping attacks. At lvl 2, the sweep
	 * attack takes the full bonus and all attacks also
	 * staggers the single mob you melee hit, afflicting
	 * it with Slowness II for 4 s.
	 */

	public DeadlyRonde(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "DeadlyRonde";
	}

	boolean cancelled = false;
	BukkitRunnable activeRunnable = null;

	@Override
	public boolean AbilityCastEvent(AbilityCastEvent event) {
		/* Re-up the duration every time an ability is cast */
		if (activeRunnable != null) {
			activeRunnable.cancel();
		}

		activeRunnable = new BukkitRunnable() {
			@Override
			public void run() {
				activeRunnable = null;
			}
		};
		activeRunnable.runTaskLater(mPlugin, 20 * 5);
		mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1, 2f);
		return true;
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (activeRunnable != null) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			if (InventoryUtils.isSwordItem(mainHand)) {
				double damage = getAbilityScore() == 1 ? 4 : 6;
				if (event.getCause() == DamageCause.ENTITY_SWEEP_ATTACK) {
					event.setDamage(event.getFinalDamage() + damage / 2);
				} else {
					event.setDamage(event.getFinalDamage() + damage);
				}
			}

			//The cancelled variable is so we don't spout multiple BukkitRunnables
			if (!cancelled) {
				cancelled = true;
				//Deactivates 1 tick after the strike so that way the sweep bonus is applied.
				//If we had it cancelled on damage, then entities affected by sweeps wouldn't get damaged.
				new BukkitRunnable() {
					@Override
					public void run() {
						activeRunnable.cancel();
						activeRunnable = null;
						cancelled = false;
					}
				}.runTaskLater(mPlugin, 1);
			}
		}
		return true;
	}

}
