package com.playmonumenta.plugins.abilities.cleric.hierophant;

import java.util.EnumSet;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentAttackSpeed;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.effects.ThuribleBonusHealing;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class ThuribleProcession extends Ability implements AbilityWithChargesOrStacks {

	private static final int EFFECTS_DURATION = 20 * 8;
	private static final int PASSIVE_DURATION = 50; //50 ticks; 20 * 2.5
	private static final int THURIBLE_RADIUS = 30;
	private static final int THURIBLE_COOLDOWN = 8;
	private static final double EFFECT_PERCENT_1 = 0.10;
	private static final double EFFECT_PERCENT_2 = 0.15;
	private static final int MAX_BUFFS = 4;
	private static final EnumSet<DamageCause> ALLOWED_DAMAGE_CAUSES = EnumSet.of(DamageCause.ENTITY_ATTACK, DamageCause.PROJECTILE);
	private static final String PERCENT_ATTACK_SPEED_EFFECT_NAME = "ThuribleProcessionPercentAttackSpeedEffect";
	private static final String PERCENT_SPEED_EFFECT_NAME = "ThuribleProcessionPercentSpeedEffect";
	public static final String PERCENT_DAMAGE_EFFECT_NAME = "ThuribleProcessionPercentDamageEffect";
	private static final String PERCENT_HEAL_EFFECT_NAME = "ThuribleProcessionPercentHealEffect";
	private static final String[] EFFECTS_NAMES = new String[] {PERCENT_ATTACK_SPEED_EFFECT_NAME, PERCENT_SPEED_EFFECT_NAME, PERCENT_DAMAGE_EFFECT_NAME, PERCENT_HEAL_EFFECT_NAME};

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(255, 195, 0), 1.0f);


	private int mSeconds = 0;
	private int mBuffs = 0;

	public ThuribleProcession(Plugin plugin, Player player) {
		super(plugin, player, "Thurible Procession");
		mInfo.mScoreboardId = "Thurible";
		mInfo.mShorthandName = "TP";
		mInfo.mDescriptions.add("The Hierophant passively builds up buffs when other players are within 30 blocks, which are applied to all players in the radius. Buffs end and the buildup resets upon a melee attack on a hostile mob, unless the full set of buffs have been obtained. Then all players (including the Hierophant) get 8 seconds of all built-up buffs. After these 8 seconds the timer resets and the Procession begins anew. Progression - +10% Attack Speed (after 4s of no melee), +10% Speed (after 8s of no melee), +10% Attack and Projectile Damage (after 12s of no melee), Cleric's passive heal is doubled, to 10% of max health every 5s (after 16s of no melee)");
		mInfo.mDescriptions.add("Progression - +15% Attack Speed (after 4s of no melee), +15% Speed (after 8s of no melee), +15% Attack and Projectile Damage (after 12s of no melee), Cleric's passive heal is tripled, to 15% of max health every 5s (after 16s of no melee)");
		mInfo.mCooldown = 20 * THURIBLE_COOLDOWN;
		mInfo.mLinkedSpell = ClassAbility.THURIBLE_PROCESSION;
		mDisplayItem = new ItemStack(Material.GLOWSTONE_DUST, 1);
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

			World world = mPlayer.getWorld();
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1, 1);
			world.spawnParticle(Particle.FIREWORKS_SPARK, mPlayer.getLocation(), 60, 0, 0, 0, 0.35);
			world.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation(), 60, 0.4, 0.4, 0.4, 1);
			world.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation(), 200, 5, 3, 5, 1);

			//Reset timer and have 8 second cooldown
			mSeconds = 0;
			putOnCooldown();
		}
		return true;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		List<Player> players = PlayerUtils.playersInRange(mPlayer.getLocation(), THURIBLE_RADIUS, true);
		if (!canCast()) {
			return;
		}
		if (players.size() > 1) {
			if (oneSecond) {
				mSeconds++;
				updateBuffs();
				applyBuffs(PASSIVE_DURATION);
			}
			if (twoHertz) {
				mPlayer.getWorld().spawnParticle(Particle.REDSTONE, mPlayer.getLocation(), 10, 0.4, 0.4, 0.4, COLOR);
			}
		}
	}

	//Recounts number of buffs and applies the passive ones
	private void updateBuffs() {

		//Convert time into number of buffs and cap to maximum effect index for that level
		mBuffs = mSeconds / 4;

		//Cap to 4 effects
		if (mBuffs > MAX_BUFFS) {
			mBuffs = MAX_BUFFS;
		}

		ClientModHandler.updateAbility(mPlayer, this);
	}

	//Applies built up buffs to all around them and themselves, take the duration as parameter (passive/built-up)
	private void applyBuffs(int duration) {

		//Give everyone buffs from the array
		List<Player> players = PlayerUtils.playersInRange(mPlayer.getLocation(), THURIBLE_RADIUS, true);
		if (players.size() > 1) {
			for (Player pl : players) {
				Effect[] effects = getEffectArray();
				for (int i = 0; i < mBuffs; i++) {
					mPlugin.mEffectManager.addEffect(pl, EFFECTS_NAMES[i], effects[i]);
				}
			}
		}
	}

	private Effect[] getEffectArray() {
		Effect[] effects = getAbilityScore() == 1 ? new Effect[]{new PercentAttackSpeed(EFFECTS_DURATION, EFFECT_PERCENT_1, PERCENT_ATTACK_SPEED_EFFECT_NAME), new PercentSpeed(EFFECTS_DURATION, EFFECT_PERCENT_1, PERCENT_SPEED_EFFECT_NAME), new PercentDamageDealt(EFFECTS_DURATION, EFFECT_PERCENT_1, ALLOWED_DAMAGE_CAUSES), new ThuribleBonusHealing(EFFECTS_DURATION, EFFECT_PERCENT_1)}
			: new Effect[]{new PercentAttackSpeed(EFFECTS_DURATION, EFFECT_PERCENT_2, PERCENT_ATTACK_SPEED_EFFECT_NAME), new PercentSpeed(EFFECTS_DURATION, EFFECT_PERCENT_2, PERCENT_SPEED_EFFECT_NAME), new PercentDamageDealt(EFFECTS_DURATION, EFFECT_PERCENT_2, ALLOWED_DAMAGE_CAUSES), new ThuribleBonusHealing(EFFECTS_DURATION, EFFECT_PERCENT_2)};
		return effects;
	}

	@Override
	public int getCharges() {
		return mBuffs;
	}

	@Override
	public int getMaxCharges() {
		return MAX_BUFFS;
	}

}
