package com.playmonumenta.plugins.abilities.scout;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.MessagingUtils;

/*
 * - Sharpshooter:  Each successful arrow hit increases your arrow damage by +1,
 * up to a max of +5. This bonus lasts for 12s, and the time refreshes with each
 * arrow hit. At level 2, each hit increases damage by +2 up to +8, with a 15s
 * timer instead.
 *
 * TODO: This damage buff
 * stacks with Enchanted Arrow and other bow skills.
 */
public class Sharpshooter extends Ability {

	public Sharpshooter(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "Sharpshooter";
	}

	private int sharpshot = 0;
	private boolean volley = false;

	@Override
	public boolean PlayerShotArrowEvent(Arrow arrow) {
		if (!arrow.hasMetadata("Volley")) {
			volley = false;
		}
		return true;
	}

	@Override
	public boolean LivingEntityShotByPlayerEvent(Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		if (!arrow.isCritical()) {
			return true;
		}
		int sharpshooter = getAbilityScore();
		int time = sharpshooter == 1 ? 20 * 12 : 20 * 15;
		if (sharpshot <= 0) {
			MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "You have begun to stack Sharpshooter!");
			new BukkitRunnable() {
				int t = 0;
				@Override
				public void run() {
					t++;

					if (t >= 20 * 4) {
						sharpshot--;
						t = 0;
						if (sharpshot <= 0) {
							MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Your Sharpshooter stacks have expired");
						}
					}
				}

			}.runTaskTimer(mPlugin, 0, 1);
		}
		if (!volley) {
			if (arrow.hasMetadata("Volley")) {
				volley = true;
			} else {
				volley = false;
			}
			int max = sharpshooter == 1 ? 5 : 8;
			if (sharpshot < max) {
				if (sharpshooter > 1) {
					sharpshot += 2;
				} else if (sharpshooter > 0) {
					sharpshot++;
				}
			}
		}
		event.setDamage(event.getDamage() + sharpshot);
		return true;
	}

	public int getSharpshot() {
		return sharpshot;
	}

}
