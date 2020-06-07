package com.playmonumenta.plugins.abilities.cleric.hierophant;

import java.util.List;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
 * Thurible Procession:
 * Level 1 - Blocking with a shield in either hand 2 times in a row,
 * holding the block on the second time, procedurally builds up
 * passive buffs, which are applied to all players (except the user)
 * within 20 blocks for as long as the block is held. The Hierophant
 * moves at ~80% normal walking speed when blocking to use this ability.
 * Progression of buffs Haste 1 (0 Seconds of Blocking), Speed 1
 * (2 Seconds of Blocking), Strength 1 (4 seconds of blocking),
 * Resistance 1 (6 seconds of blocking). After blocking for 15 seconds,
 * all players (including the Hiero) are given 15 seconds
 * of Haste 1, Speed 1, Resistance 1, and Strength 1.
 * At level 2, the radius is increased to 30 blocks and the Hiero moves
 * at normal walking speed when blocking for the ability. Buffs applied
 * at the end of the procession are increased to 20 seconds, and it only
 * takes 10 seconds of blocking to trigger. No cooldown.
 */
public class ThuribleProcession extends Ability {

	private static final int EFFECTS_DURATION = 20 * 8;
	private static final int PASSIVE_DURATION = 50; //50 ticks; 20 * 2.5
	private static final int THURIBLE_1_RADIUS = 20;
	private static final int THURIBLE_2_RADIUS = 30;
	private static final int THURIBLE_COOLDOWN = 8;
	private static final PotionEffectType[] EFFECTS = new PotionEffectType[] {PotionEffectType.FAST_DIGGING, PotionEffectType.SPEED, PotionEffectType.INCREASE_DAMAGE, PotionEffectType.DAMAGE_RESISTANCE};

	private int mSeconds = 0;
	private int mBuffs = 0;

	public ThuribleProcession(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Thurible Procession");
		mInfo.mScoreboardId = "Thurible";
		mInfo.mShorthandName = "TP";
		mInfo.mDescriptions.add("The Hierophant passively builds up potion effect buffs, which are applied to all other players within 20 blocks. Buffs end and the buildup resets upon a melee attack on a hostile mob, unless the full set of buffs have been obtained. Then all players (including the Hierophant) get 8 seconds of all built-up buffs. After these 8 seconds the timer resets and the Procession begins anew. Progression - Haste 1 (after 5s of no melee), Speed 1 (after 10s of no melee), Strength 1 (after 15s of no melee)");
		mInfo.mDescriptions.add("Range is extended to 30 blocks, progression changes to Haste 1 (after 3s of no melee), Speed 1 (after 6s of no melee), Strength 1 (after 9s of no melee), Resistance 1 (after 12s of no melee)");
		mInfo.mCooldown = 20 * THURIBLE_COOLDOWN;
		mInfo.mLinkedSpell = Spells.THURIBLE_PROCESSION;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {

		//If on cooldown, do not cast
		if (!canCast()) {
			return true;
		}

		if (mBuffs > 0 && event.getCause() == DamageCause.ENTITY_ATTACK && EntityUtils.isHostileMob(event.getEntity())) {

			updateBuffs();

			//Give everyone buffs from the array
			applyBuffs(EFFECTS_DURATION);

			mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1, 1);
			mWorld.spawnParticle(Particle.FIREWORKS_SPARK, mPlayer.getLocation(), 60, 0, 0, 0, 0.35);
			mWorld.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation(), 60, 0.4, 0.4, 0.4, 1);
			mWorld.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation(), 200, 5, 3, 5, 1);

			//Reset timer and have 8 second cooldown
			mSeconds = 0;
			putOnCooldown();
		}
		return true;
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (!canCast()) {
			return;
		}
		if (oneSecond) {
			mSeconds++;
			updateBuffs();
			applyBuffs(PASSIVE_DURATION);
		}
		if (fourHertz) {
			for (int i = 0; i < mBuffs; i++) {
				Particle.DustOptions color = new Particle.DustOptions(EFFECTS[i].getColor(), 1);
				mWorld.spawnParticle(Particle.REDSTONE, mPlayer.getLocation(), 1, 0.4, 0.4, 0.4, color);
			}
		}
	}

	//Recounts number of buffs and applies the passive ones
	private void updateBuffs() {

		//Convert time into number of buffs and cap to maximum effect index for that level
		mBuffs = getAbilityScore() == 1 ? mSeconds / 5 : mSeconds / 3;

		//If level 2, cap to 4 effects, if level 1, cap to 3 effects
		if (mBuffs > 3) {
			if (getAbilityScore() != 1) {
				mBuffs = 4;
			} else {
				mBuffs = 3;
			}
		}
	}

	//Applies built up buffs to all around them and themselves, take the duration as parameter (passive/built-up)
	private void applyBuffs(int duration) {

		int radius = getAbilityScore() == 1 ? THURIBLE_1_RADIUS : THURIBLE_2_RADIUS;

		//Give everyone buffs from the array
		List<Player> players = PlayerUtils.playersInRange(mPlayer, radius, true);
		for (Player pl : players) {
			for (int i = 0; i < mBuffs; i++) {
				mPlugin.mPotionManager.addPotion(pl, PotionID.ABILITY_OTHER, new PotionEffect(EFFECTS[i], duration, 0, true, true));
			}
		}
	}

}
