package com.playmonumenta.plugins.abilities.cleric.hierophant;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentAttackSpeed;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.effects.ThuribleBonusHealing;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ThuribleProcession extends Ability implements AbilityWithChargesOrStacks {

	private static final int EFFECTS_DURATION = 20 * 8;
	private static final int PASSIVE_DURATION = 50; //50 ticks; 20 * 2.5
	private static final int THURIBLE_RADIUS = 30;
	private static final int THURIBLE_COOLDOWN = 8 * 20;
	private static final double EFFECT_PERCENT_1 = 0.10;
	private static final double EFFECT_PERCENT_2 = 0.15;
	private static final int MAX_BUFFS = 4;
	private static final double DAMAGE_BREAK_PERCENT = 0.6;
	private static final EnumSet<DamageType> AFFECTED_DAMAGE_TYPES = EnumSet.of(DamageType.MELEE, DamageType.PROJECTILE);
	private static final String PERCENT_ATTACK_SPEED_EFFECT_NAME = "ThuribleProcessionPercentAttackSpeedEffect";
	private static final String PERCENT_SPEED_EFFECT_NAME = "ThuribleProcessionPercentSpeedEffect";
	public static final String PERCENT_DAMAGE_EFFECT_NAME = "ThuribleProcessionPercentDamageEffect";
	private static final String PERCENT_HEAL_EFFECT_NAME = "ThuribleProcessionPercentHealEffect";
	private static final String[] EFFECTS_NAMES = new String[] {PERCENT_ATTACK_SPEED_EFFECT_NAME, PERCENT_SPEED_EFFECT_NAME, PERCENT_DAMAGE_EFFECT_NAME, PERCENT_HEAL_EFFECT_NAME};

	public static final String CHARM_DAMAGE = "Thurible Procession Damage Amplifier";
	public static final String CHARM_ATTACK = "Thurible Procession Attack Speed Amplifier";
	public static final String CHARM_SPEED = "Thurible Procession Speed Amplifier";
	public static final String CHARM_EFFECT_DURATION = "Thurible Procession Effect Duration";
	public static final String CHARM_COOLDOWN = "Thurible Procession Cooldown";
	public static final String CHARM_HEAL = "Thurible Procession Heal";

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(255, 195, 0), 1.0f);

	public static final AbilityInfo<ThuribleProcession> INFO =
		new AbilityInfo<>(ThuribleProcession.class, "Thurible Procession", ThuribleProcession::new)
			.linkedSpell(ClassAbility.THURIBLE_PROCESSION)
			.scoreboardId("Thurible")
			.shorthandName("TP")
			.descriptions(
				"The Hierophant passively builds up buffs when other players are within 30 blocks, which are applied to all players in the radius. " +
					"Buffs end and the buildup resets upon taking damage that causes you to drop below 60% of your max health, unless the full set of buffs have been obtained. " +
					"Then all players (including the Hierophant) get 8 seconds of all built-up buffs. After these 8 seconds the timer resets and the Procession begins anew. " +
					"Progression - +10% Attack Speed (after 4s of no health threshold reached), " +
					"+10% Speed (after 8s of no health threshold reached), " +
					"+10% Attack and Projectile Damage (after 12s of no health threshold reached), " +
					"Cleric's passive heal is doubled, to 10% of max health every 5s (after 16s of no health threshold reached)",
				"Progression - +15% Attack Speed (after 4s of no health threshold reached), " +
					"+15% Speed (after 8s of no health threshold reached), " +
					"+15% Attack and Projectile Damage (after 12s of no health threshold reached), " +
					"Cleric's passive heal is tripled, to 15% of max health every 5s (after 16s of no health threshold reached)")
			.cooldown(THURIBLE_COOLDOWN, CHARM_COOLDOWN)
			.displayItem(new ItemStack(Material.GLOWSTONE_DUST, 1));

	private int mSeconds = 0;
	private int mBuffs = 0;

	public ThuribleProcession(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public void onHurt(DamageEvent event, Entity damager, LivingEntity source) {
		if (mBuffs > 0 && mPlayer.getHealth() - event.getFinalDamage(true) <= EntityUtils.getMaxHealth(mPlayer) * DAMAGE_BREAK_PERCENT) {
			updateBuffs();

			//Give everyone buffs from the array
			applyBuffs(EFFECTS_DURATION);

			World world = mPlayer.getWorld();
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1, 1);
			new PartialParticle(Particle.FIREWORKS_SPARK, mPlayer.getLocation(), 60, 0, 0, 0, 0.35).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.SPELL_INSTANT, mPlayer.getLocation(), 60, 0.4, 0.4, 0.4, 1).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.SPELL_INSTANT, mPlayer.getLocation(), 200, 5, 3, 5, 1).spawnAsPlayerActive(mPlayer);

			//Reset timer and have 8 second cooldown
			mSeconds = 0;
			putOnCooldown();
		}
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (isOnCooldown()) {
			return;
		}
		if (twoHertz && mBuffs > 0) {
			new PartialParticle(Particle.REDSTONE, mPlayer.getLocation(), 10, 0.4, 0.4, 0.4, COLOR).spawnAsPlayerPassive(mPlayer);
		}
		if (oneSecond) {
			List<Player> players = PlayerUtils.playersInRange(mPlayer.getLocation(), THURIBLE_RADIUS, true);
			if (players.size() > 1) {
				mSeconds++;
				updateBuffs();
				applyBuffs(PASSIVE_DURATION);
			}
		}
	}

	//Recounts number of buffs and applies the passive ones
	private void updateBuffs() {
		int previousBuffs = mBuffs;

		//Convert time into number of buffs and cap to maximum effect index for that level
		mBuffs = mSeconds / 4;

		//Cap to 4 effects
		if (mBuffs > MAX_BUFFS) {
			mBuffs = MAX_BUFFS;
		}

		if (previousBuffs != mBuffs) {
			ClientModHandler.updateAbility(mPlayer, this);
		}
	}

	//Applies built up buffs to all around them and themselves, take the duration as parameter (passive/built-up)
	private void applyBuffs(int duration) {
		//Give everyone buffs from the array
		List<Player> players = PlayerUtils.playersInRange(mPlayer.getLocation(), THURIBLE_RADIUS, true);
		if (players.size() > 1) {
			duration = CharmManager.getDuration(mPlayer, CHARM_EFFECT_DURATION, duration);
			for (Player pl : players) {
				Effect[] effects = getEffectArray(duration);
				for (int i = 0; i < mBuffs; i++) {
					mPlugin.mEffectManager.addEffect(pl, EFFECTS_NAMES[i], effects[i]);
				}
			}
		}
	}

	private Effect[] getEffectArray(int duration) {
		double effectPercent = isLevelOne() ? EFFECT_PERCENT_1 : EFFECT_PERCENT_2;
		return new Effect[] {new PercentAttackSpeed(duration, CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ATTACK) + effectPercent, PERCENT_ATTACK_SPEED_EFFECT_NAME),
		                     new PercentSpeed(duration, CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED) + effectPercent, PERCENT_SPEED_EFFECT_NAME),
		                     new PercentDamageDealt(duration, CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE) + effectPercent, AFFECTED_DAMAGE_TYPES),
		                     new ThuribleBonusHealing(duration, CharmManager.getLevelPercentDecimal(mPlayer, CHARM_HEAL) + effectPercent)};
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
