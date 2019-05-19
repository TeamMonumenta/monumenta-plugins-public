package com.playmonumenta.plugins.abilities.rogue.assassin;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;

/*
 * Preparation: Right click twice while looking up to make
 * your next ability within 5 seconds have additional effects:
 * By My Blade: deals an additional 12 / 18 damage
 * Advancing Shadows: deals 6 / 9 damage in a 4 block radius
 * Dagger Throw: stuns hit enemies for 1 / 1.5 seconds
 * Smokescreen: Slowness and Weakness duration increased by 5 / 8 seconds
 * Escape Death: Gain 3 / 5 seconds of invulnerability
 * Cooldown: 30 / 20 seconds if you use an ability in the 5 second period.
 */

public class Preparation extends Ability {

	private static final String CHECK_ONCE_THIS_TICK_METAKEY = "PreparationTickRightClicked";

	private static final int PREPARATION_1_COOLDOWN = 20 * 30;
	private static final int PREPARATION_2_COOLDOWN = 20 * 20;
	private static final int PREPARATION_ACTIVATION_PERIOD = 20 * 5;

	private static final int PREPARATION_1_BMB_DAMAGE = 12;
	private static final int PREPARATION_2_BMB_DAMAGE = 18;
	private static final int PREPARATION_1_AS_DAMAGE = 6;
	private static final int PREPARATION_2_AS_DAMAGE = 9;
	private static final int PREPARATION_1_DT_DURATION = 20 * 1;
	private static final int PREPARATION_2_DT_DURATION = (int)(20 * 1.5);
	private static final int PREPARATION_1_SS_DURATION = 20 * 5;
	private static final int PREPARATION_2_SS_DURATION = 20 * 8;
	private static final int PREPARATION_1_ED_DURATION = 20 * 3;
	private static final int PREPARATION_2_ED_DURATION = 20 * 5;

	private int mRightClicks = 0;
	private boolean mActive = false;

	public Preparation(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "Preparation";
		mInfo.linkedSpell = Spells.PREPARATION;
		mInfo.cooldown = getAbilityScore() == 1 ? PREPARATION_1_COOLDOWN : PREPARATION_2_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public void cast() {
		// Prevent two right clicks being registered from one action (e.g. blocking)
		if (MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, CHECK_ONCE_THIS_TICK_METAKEY)) {
			mRightClicks++;
			new BukkitRunnable() {
				@Override
				public void run() {
					if (mRightClicks > 0) {
						mRightClicks--;
					}
				}
			}.runTaskLater(mPlugin, 5);
		}

		if (mRightClicks < 2) {
			return;
		}
		mRightClicks = 0;

		mActive = true;
		mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1, 1.25f);
		mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1.75f);
		mWorld.spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1, 0), 60, 0.35, 0.4, 0.35, 1);
		mWorld.spawnParticle(Particle.SWEEP_ATTACK, mPlayer.getLocation().add(0, 1, 0), 7, 0.5, 0.5, 0.5, 1);
		new BukkitRunnable() {
			int t = 0;
			@Override
			public void run() {
				t++;
				mWorld.spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1, 0), 1, 0.4, 0.4, 0.4, 0);
				mWorld.spawnParticle(Particle.TOWN_AURA, mPlayer.getLocation().add(0, 1, 0), 7, 0.35, 0.4, 0.35, 1);
				mWorld.spawnParticle(Particle.PORTAL, mPlayer.getLocation().add(0, 1, 0), 6, 0.35, 0.4, 0.35, 0.1);
				if (t >= PREPARATION_ACTIVATION_PERIOD) {
					if (mActive) {
						mActive = false;
						mWorld.spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1, 0), 50, 0.4, 0.4, 0.4, 1);
						mWorld.spawnParticle(Particle.SPELL_MOB, mPlayer.getLocation().add(0, 1, 0), 50, 0.4, 0.4, 0.4, 0);
						mWorld.spawnParticle(Particle.SMOKE_LARGE, mPlayer.getLocation().add(0, 1, 0), 7, 0.5, 0.5, 0.5, 0);
						mPlugin.mTimers.removeCooldown(mPlayer.getUniqueId(), Spells.PREPARATION);
						mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1, 0.75f);
						mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1, 0.7f);
					}
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack oHand = mPlayer.getInventory().getItemInOffHand();
		return mPlayer.getLocation().getPitch() < -50 && !mPlayer.isSneaking() && InventoryUtils.isSwordItem(mHand) && InventoryUtils.isSwordItem(oHand);
	}

	public int getBonus(Spells spell) {
		if (mActive) {
			mActive = false;
			if (spell == Spells.BY_MY_BLADE) {
				return getAbilityScore() == 1 ? PREPARATION_1_BMB_DAMAGE : PREPARATION_2_BMB_DAMAGE;
			} else if (spell == Spells.ADVANCING_SHADOWS) {
				return getAbilityScore() == 1 ? PREPARATION_1_AS_DAMAGE : PREPARATION_2_AS_DAMAGE;
			} else if (spell == Spells.DAGGER_THROW) {
				return getAbilityScore() == 1 ? PREPARATION_1_DT_DURATION : PREPARATION_2_DT_DURATION;
			} else if (spell == Spells.SMOKESCREEN) {
				return getAbilityScore() == 1 ? PREPARATION_1_SS_DURATION : PREPARATION_2_SS_DURATION;
			} else if (spell == Spells.ESCAPE_DEATH) {
				return getAbilityScore() == 1 ? PREPARATION_1_ED_DURATION : PREPARATION_2_ED_DURATION;
			}
		}
		return 0;
	}

}
