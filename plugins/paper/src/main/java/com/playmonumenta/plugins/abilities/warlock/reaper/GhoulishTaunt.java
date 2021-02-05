package com.playmonumenta.plugins.abilities.warlock.reaper;

import java.util.List;
import java.util.NavigableSet;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.effects.Aesthetics;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentAttackSpeed;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class GhoulishTaunt extends Ability {

	private static final String AESTHETICS_EFFECT_NAME = "GhoulishTauntAestheticsEffect";
	private static final String PERCENT_SPEED_EFFECT_NAME = "GhoulishTauntPercentSpeedEffect";
	private static final String PERCENT_ATTACK_SPEED_EFFECT_NAME = "GhoulishTauntPercentAttackSpeedEffect";
	private static final int DURATION = 20 * 10;
	private static final int DURATION_INCREASE_ON_KILL = 20 * 1;
	private static final double PERCENT_1 = 0.1;
	private static final double PERCENT_2 = 0.15;

	private static final double CLEAVE_RADIUS = 2.0;
	private static final double CLEAVE_PERCENT_DAMAGE_1 = 0.3;
	private static final double CLEAVE_PERCENT_DAMAGE_2 = 0.5;
	private static final int WEAKNESS_AMPLIFIER_1 = 0;
	private static final int WEAKNESS_AMPLIFIER_2 = 1;
	private static final int WEAKNESS_RADIUS = 12;
	private static final int COOLDOWN = 20 * 20;

	private int mLeftClicks = 0;
	private final double mPercent;
	private final double mCleavePercentDamage;
	private final int mWeaknessAmplifier;


	public GhoulishTaunt(Plugin plugin, Player player) {
		super(plugin, player, "Ghoulish Taunt");
		mInfo.mScoreboardId = "GhoulishTaunt";
		mInfo.mShorthandName = "GT";
		mInfo.mDescriptions.add("Left clicking twice with a scythe unleashes a devilish shriek, causing all mobs within a 12 block range to target you and afflicting them with Weakness I for 10 seconds. For the next 10 seconds, gain +10% speed and +10% attack speed, and your melee attacks cleave in a 2 block radius from the strike, dealing 30% of the damage from the original attack. Each kill during this time increases the duration of the buffs by 1 second. Cooldown: 20s.");
		mInfo.mDescriptions.add("Apply Weakness II to mobs instead, and gain +15% speed and +15% attack speed and 50% cleaving damage instead.");
		mInfo.mLinkedSpell = Spells.GHOULISH_TAUNT;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mInfo.mIgnoreCooldown = true;
		mPercent = getAbilityScore() == 1 ? PERCENT_1 : PERCENT_2;
		mCleavePercentDamage = getAbilityScore() == 1 ? CLEAVE_PERCENT_DAMAGE_1 : CLEAVE_PERCENT_DAMAGE_2;
		mWeaknessAmplifier = getAbilityScore() == 1 ? WEAKNESS_AMPLIFIER_1 : WEAKNESS_AMPLIFIER_2;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		// Just use the other buffs as a flag for the cleave
		if (event.getCause() == DamageCause.ENTITY_ATTACK && mPlugin.mEffectManager.getEffects(mPlayer, PERCENT_SPEED_EFFECT_NAME) != null) {
			Location loc = event.getEntity().getLocation().add(0, 1, 0);
			World world = mPlayer.getWorld();
			world.spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0, 0, 0);
			world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 0.4f);
			double damage = event.getDamage() * mCleavePercentDamage;
			for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, CLEAVE_RADIUS)) {
				if (mob != event.getEntity()) {
					EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer, null, false, mInfo.mLinkedSpell);
				}
			}
		}

		return true;
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		NavigableSet<Effect> aestheticsEffects = mPlugin.mEffectManager.getEffects(mPlayer, AESTHETICS_EFFECT_NAME);
		if (aestheticsEffects != null) {
			for (Effect effect : aestheticsEffects) {
				effect.setDuration(effect.getDuration() + DURATION_INCREASE_ON_KILL);
			}
		}
		NavigableSet<Effect> speedEffects = mPlugin.mEffectManager.getEffects(mPlayer, PERCENT_SPEED_EFFECT_NAME);
		if (speedEffects != null) {
			for (Effect effect : speedEffects) {
				effect.setDuration(effect.getDuration() + DURATION_INCREASE_ON_KILL);
			}
		}
		NavigableSet<Effect> attackSpeedEffects = mPlugin.mEffectManager.getEffects(mPlayer, PERCENT_ATTACK_SPEED_EFFECT_NAME);
		if (attackSpeedEffects != null) {
			for (Effect effect : attackSpeedEffects) {
				effect.setDuration(effect.getDuration() + DURATION_INCREASE_ON_KILL);
			}
		}
	}

	@Override
	public void cast(Action action) {
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
			return;
		}
		mLeftClicks++;
		new BukkitRunnable() {
			@Override
			public void run() {
				if (mLeftClicks > 0) {
					mLeftClicks--;
				}
				this.cancel();
			}
		}.runTaskLater(mPlugin, 5);
		if (mLeftClicks < 2) {
			return;
		}

		Location loc = mPlayer.getEyeLocation();
		World world = mPlayer.getWorld();
		world.playSound(loc, Sound.ENTITY_GHAST_SCREAM, SoundCategory.PLAYERS, 0.6f, 0.4f);
		world.playSound(loc, Sound.ENTITY_GHAST_SCREAM, SoundCategory.PLAYERS, 0.6f, 0.6f);
		world.playSound(loc, Sound.ENTITY_GHAST_SCREAM, SoundCategory.PLAYERS, 0.6f, 0.8f);
		world.playSound(loc, Sound.ENTITY_GHAST_HURT, SoundCategory.PLAYERS, 0.6f, 0.6f);
		world.spawnParticle(Particle.ENCHANTMENT_TABLE, loc, 400, 0, 0, 0, 4);

		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, WEAKNESS_RADIUS);
		for (LivingEntity mob : mobs) {
			PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.WEAKNESS, DURATION, mWeaknessAmplifier));
			if (mob instanceof Mob) {
				EntityUtils.applyTaunt(mPlugin, mob, mPlayer);
			}
		}

		mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(DURATION, mPercent, PERCENT_SPEED_EFFECT_NAME));
		mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_ATTACK_SPEED_EFFECT_NAME, new PercentAttackSpeed(DURATION, mPercent, PERCENT_ATTACK_SPEED_EFFECT_NAME));
		mPlugin.mEffectManager.addEffect(mPlayer, AESTHETICS_EFFECT_NAME, new Aesthetics(DURATION,
				(entity, fourHertz, twoHertz, oneHertz) -> {
					entity.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, entity.getLocation(), 5, 0, 0, 0, 3);
				},
				null));

		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		return InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand());
	}

}
