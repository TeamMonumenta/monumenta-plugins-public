package com.playmonumenta.plugins.abilities.rogue.swordsage;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.magic.AbilityCastEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
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
		mInfo.classId = 4;
		mInfo.specId = 1;
		mInfo.scoreboardId = "DeadlyRonde";
	}

	boolean active = false;
	boolean cancelled = false;

	@Override
	public boolean AbilityCastEvent(AbilityCastEvent event) {
		if (!active) {
			active = true;

			new BukkitRunnable() {
				int t = 0;
				@Override
				public void run() {
					t++;

					if (t >= 20 * 5 || !active) {
						this.cancel();
						active = false;
					}
				}

			}.runTaskTimer(mPlugin, 0, 1);
		}
		return true;
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (active) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			if (InventoryUtils.isSwordItem(mainHand)) {
				int deadlyRonde = getAbilityScore();
				double damage = deadlyRonde == 1 ? 4 : 6;
				LivingEntity damaged = (LivingEntity) event.getEntity();
				if (event.getCause() == DamageCause.ENTITY_SWEEP_ATTACK) {
					EntityUtils.damageEntity(mPlugin, damaged, damage / 2, mPlayer);
				} else {
					EntityUtils.damageEntity(mPlugin, damaged, damage, mPlayer);
				}
			}

			//The cancelled variable is so we don't spout multiple BukkitRunnables
			if (!cancelled) {
				cancelled = true;
				//Sets active to false 1 tick after the strike so that way the sweep bonus is applied.
				//If we had it cancelled on damage, then entities affected by sweeps wouldn't get damaged.
				new BukkitRunnable() {

					@Override
					public void run() {
						active = false;
						cancelled = false;
					}
				}.runTaskLater(mPlugin, 1);
			}
		}
		return true;
	}

}
