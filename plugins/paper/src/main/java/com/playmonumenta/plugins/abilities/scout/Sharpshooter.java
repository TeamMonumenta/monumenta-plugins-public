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
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;

/*
 * Sharpshooter: Each successful, fully charged arrow hit increases your arrow
 * damage by 1, up to a max of 5. After 4 seconds without a successful hit, the
 * bonus is reduced by 1. At level 2, each hit increases damage by 2 up to 8.
 *
 * TODO: This damage buff
 * stacks with Enchanted Arrow and other bow skills.
 */

public class Sharpshooter extends Ability {
	private static final int SHARPSHOOTER_DECAY_TIMER = 20 * 4;
	private static final int SHARPSHOOTER_1_MAX_BONUS = 5;
	private static final int SHARPSHOOTER_2_MAX_BONUS = 8;
	private static final int SHARPSHOOTER_1_INCREMENT = 1;
	private static final int SHARPSHOOTER_2_INCREMENT = 2;

	public Sharpshooter(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "Sharpshooter";
	}

	private int mSharpshot = 0;
	private int mTicks = 0;

	@Override
	public boolean PlayerShotArrowEvent(Arrow arrow) {
		if (MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, "SharpshooterBonusDamageRegistrationTick")) {
			AbilityUtils.addArrowBonusDamage(mPlugin, arrow, mSharpshot);
		}
		return true;
	}

	@Override
	public boolean LivingEntityShotByPlayerEvent(Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		// Only increment sharpshot if the arrow is critical and not from volley
		if (arrow.isCritical() && !arrow.hasMetadata("Volley")) {
			mTicks = 0;

			if (mSharpshot <= 0) {
				new BukkitRunnable() {
					@Override
					public void run() {
						mTicks++;

						if (mTicks >= SHARPSHOOTER_DECAY_TIMER) {
							mTicks = 0;
							mSharpshot--;
							MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Sharpshooter bonus: " + mSharpshot);
						}

						if (mSharpshot <= 0) {
							this.cancel();
						}
					}

				}.runTaskTimer(mPlugin, 0, 1);
			}

			int max = getAbilityScore() == 1 ? SHARPSHOOTER_1_MAX_BONUS : SHARPSHOOTER_2_MAX_BONUS;
			int increment = getAbilityScore() == 1 ? SHARPSHOOTER_1_INCREMENT : SHARPSHOOTER_2_INCREMENT;
			mSharpshot += increment;
			if (mSharpshot > max) {
				mSharpshot = max;
			}
			MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Sharpshooter bonus: " + mSharpshot);
		}

		return true;
	}

	public int getSharpshot() {
		return mSharpshot;
	}

}
