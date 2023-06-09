package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.EnumSet;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SwiftCuts extends Ability implements AbilityWithChargesOrStacks {
	private static final int EFFECT_DURATION = 10 * 20;
	private static final EnumSet<DamageType> AFFECTED_DAMAGE_TYPES = EnumSet.of(
		DamageType.MELEE,
		DamageType.MELEE_SKILL,
		DamageType.MELEE_ENCH
	);

	public static final double DAMAGE_AMPLIFIER = 0.05;
	public static final int STACKS_CAP_1 = 4;
	public static final int STACKS_CAP_2 = 7;
	public static final double TACTICAL_MANEUVER_CDR = 0.3;
	public static final double WHIRLING_BLADE_BUFF = 0.3;
	public static final double PREDATOR_STRIKE_CDR = 0.2;
	public static final int SPLIT_ARROW_BUFF = 1;
	public static final String EFFECT_NAME = "SwiftCutsExtraDamage";

	public static final String CHARM_DAMAGE = "Swift Cuts Stack Damage";
	public static final String CHARM_STACKS = "Swift Cuts Stacks";
	public static final String CHARM_DURATION = "Swift Cuts Effect Duration";
	public static final String CHARM_ENHANCE = "Swift Cuts Enhancement Effect";

	public static final AbilityInfo<SwiftCuts> INFO =
		new AbilityInfo<>(SwiftCuts.class, "Swift Cuts", SwiftCuts::new)
			.scoreboardId("SwiftCuts")
			.shorthandName("SC")
			.descriptions(
				String.format("Attacking an enemy with a fully charged melee attack grants you a Swift Cuts stack with a maximum of %d stacks. Every stack grants you +%d%% melee damage. After %d seconds of not gaining any stacks, lose all stacks.",
					STACKS_CAP_1,
					(int) (DAMAGE_AMPLIFIER * 100),
					EFFECT_DURATION / 20
				),
				String.format("Maximum stacks increased to %d.",
					STACKS_CAP_2
				),
				String.format("Additional effect applies while having maximum Swift Cuts stacks. Tactical Maneuver: -%d%% Cooldown, Whirling Blade: +%d%% Damage and Radius, Predator Strike: -%d%% Cooldown, Split Arrow: +%d Bounce",
					(int) (TACTICAL_MANEUVER_CDR * 100),
					(int) (WHIRLING_BLADE_BUFF * 100),
					(int) (PREDATOR_STRIKE_CDR * 100),
					SPLIT_ARROW_BUFF
				)
			)
			.simpleDescription("Melee deals increasing damage the more you melee enemies.")
			.displayItem(Material.STONE_SWORD);


	private final double mDamageAmplifier;
	private final int mMaxStacks;
	private final int mDuration;
	private int mStacks;

	public SwiftCuts(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mMaxStacks = (isLevelOne() ? STACKS_CAP_1 : STACKS_CAP_2) + (int) CharmManager.getLevel(mPlayer, CHARM_STACKS);
		mDamageAmplifier = DAMAGE_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, EFFECT_DURATION);
		mStacks = 0;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE && mPlayer.getCooledAttackStrength(0.5f) > 0.9) {
			Location loc = enemy.getLocation();
			World world = mPlayer.getWorld();
			world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.5f, 1.5f);
			new PartialParticle(Particle.SWEEP_ATTACK, loc, 2, 0.25, 0.35, 0.25, 0.001).spawnAsPlayerActive(mPlayer);

			// apply effect then add stacks
			if (!hasEffect()) {
				mStacks = 0;
			}
			double damageBoost = mStacks * mDamageAmplifier;
			mPlugin.mEffectManager.addEffect(mPlayer, EFFECT_NAME, new PercentDamageDealt(mDuration, damageBoost, AFFECTED_DAMAGE_TYPES));
			if (mStacks < mMaxStacks) {
				mStacks += 1;
				//send stack update to client
				ClientModHandler.updateAbility(mPlayer, this);
			}
		}
		return false; // only changes event damage
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mStacks > 0 && oneSecond) {
			//clear and send stack update if needed.
			if (!hasEffect()) {
				mStacks = 0;
				ClientModHandler.updateAbility(mPlayer, this);
			}
		}
	}

	private boolean hasEffect() {
		return mPlugin.mEffectManager.hasEffect(mPlayer, EFFECT_NAME);
	}

	public boolean isEnhancementActive() {
		return isEnhanced() && mStacks == mMaxStacks && hasEffect();
	}

	@Override
	public int getCharges() {
		return mStacks;
	}

	@Override
	public int getMaxCharges() {
		return mMaxStacks;
	}
}
