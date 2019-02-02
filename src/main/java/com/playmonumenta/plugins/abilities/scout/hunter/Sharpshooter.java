package com.playmonumenta.plugins.abilities.scout.hunter;

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
 * Sharpshooter:  Each time you hit something with an arrow 
 * it gives you a level of “Sharpshot” for 7s. Each level 
 * you have of Sharpshot increases your arrow damage by 1. 
 * Every time you land an arrow shot, your level of Sharpshot 
 * is increased (max of 5 Levels) and its duration is refreshed.
 * At level 2, you can have a max of 7 Sharpshot Levels, and 
 * its duration is increased to 9 seconds. This damage buff
 * stacks with Enchanted Arrow and other bow skills.
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
		int time = sharpshooter == 1 ? 20 * 7 : 20 * 9;
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
		int max = sharpshooter == 1 ? 5 : 7;
		if (sharpshot < max) 
			sharpshot++;
		t = 0;
		event.setDamage(event.getDamage() + sharpshot);
		return true;
	}

}
