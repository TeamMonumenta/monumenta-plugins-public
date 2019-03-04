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
	private int t = 0;

	@Override
	public boolean LivingEntityShotByPlayerEvent(Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		int sharpshooter = getAbilityScore();
		int time = sharpshooter == 1 ? 20 * 12 : 20 * 15;
		if (sharpshot <= 0) {
			MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "You have begun to stack Sharpshooter!");
			new BukkitRunnable() {

				@Override
				public void run() {
					t++;

					if (t >= time || mPlayer.isDead()) {
						this.cancel();
						sharpshot = 0;
						t = 0;
						MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Your Sharpshooter stacks have expired");
					}
				}

			}.runTaskTimer(mPlugin, 0, 1);
		}
		int max = sharpshooter == 1 ? 5 : 8;
		if (sharpshot < max) {
			if (sharpshooter > 1) {
				sharpshot += 2;
			} else if (sharpshooter > 0) {
				sharpshot++;
			}
		}
		t = 0;
		event.setDamage(event.getDamage() + sharpshot);
		return true;
	}

	public int getSharpshot() {
		return sharpshot;
	}

}
