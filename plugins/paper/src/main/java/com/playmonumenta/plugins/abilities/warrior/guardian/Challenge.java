package com.playmonumenta.plugins.abilities.warrior.guardian;

import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;

public class Challenge extends Ability {

	private static final double CHALLENGE_1_SOLO_DAMAGE_BONUS = 0.1;
	private static final double CHALLENGE_2_SOLO_DAMAGE_BONUS = 0.2;
	private static final int CHALLENGE_RANGE = 12;
	private static final int CHALLENGE_1_ABSORPTION_AMPLIFIER = 0;
	private static final int CHALLENGE_2_ABSORPTION_AMPLIFIER = 1;
	private static final int CHALLENGE_1_ARMOR = 1;
	private static final int CHALLENGE_2_ARMOR = 2;
	private static final int CHALLENGE_1_ARMOR_MAX = 4;
	private static final int CHALLENGE_2_ARMOR_MAX = 8;
	private static final int CHALLENGE_DURATION = 10 * 20;
	private static final int CHALLENGE_COOLDOWN = 20 * 20;

	private final double mSoloDamageBonus;
	private final int mArmorIncrease;
	private final int mArmorMax;
	private final int mAbsorptionAmplifier;

	private boolean mDamageBonusActive = false;

	public Challenge(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Challenge");
		mInfo.scoreboardId = "Challenge";
		mInfo.mShorthandName = "Ch";
		mInfo.mDescriptions.add("Left-clicking while shifted makes all enemies within 12 blocks target you. You gain Absorption 1 and 0.5 armor per affected mob (max: 4) for 10s. If no mobs changed targets, gain 10% extra melee damage. Cooldown: 20s.");
		mInfo.mDescriptions.add("You gain Absorption II and 1 armor per mob instead to a max of 8. The conditional damage bonus is increased to 20%.");
		mInfo.cooldown = CHALLENGE_COOLDOWN;
		mInfo.ignoreCooldown = true;
		mInfo.linkedSpell = Spells.CHALLENGE;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
		mSoloDamageBonus = getAbilityScore() == 1 ? CHALLENGE_1_SOLO_DAMAGE_BONUS : CHALLENGE_2_SOLO_DAMAGE_BONUS;
		mArmorMax = getAbilityScore() == 1 ? CHALLENGE_1_ARMOR_MAX : CHALLENGE_2_ARMOR_MAX;
		mArmorIncrease = getAbilityScore() == 1 ? CHALLENGE_1_ARMOR : CHALLENGE_2_ARMOR;
		mAbsorptionAmplifier = getAbilityScore() == 1 ? CHALLENGE_1_ABSORPTION_AMPLIFIER : CHALLENGE_2_ABSORPTION_AMPLIFIER;
	}

	@Override
	public void cast(Action action) {
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.linkedSpell) || !mPlayer.isSneaking()) {
			return;
		}

		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mPlayer.getLocation(), CHALLENGE_RANGE, mPlayer);
		int increase = Math.min(mArmorMax, mobs.size() * mArmorIncrease);
		AttributeInstance armor = mPlayer.getAttribute(Attribute.GENERIC_ARMOR);
		armor.setBaseValue(armor.getBaseValue() + increase);

		new BukkitRunnable() {
			@Override
			public void run() {
				mDamageBonusActive = false;
				armor.setBaseValue(armor.getBaseValue() - increase);
			}
		}.runTaskLater(mPlugin, CHALLENGE_DURATION);

		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
				new PotionEffect(PotionEffectType.ABSORPTION, CHALLENGE_DURATION, mAbsorptionAmplifier, false, true));

		boolean challenged = false;

		for (LivingEntity mob : mobs) {
			if (mob instanceof Mob) {
				if (((Mob) mob).getTarget() != null && !((Mob) mob).getTarget().equals(mPlayer)) {
					challenged = true;
				}

				((Mob) mob).setTarget(mPlayer);
			} else if (mob instanceof Player && AbilityManager.getManager().isPvPEnabled((Player)mob)) {
				Vector dir = LocationUtils.getDirectionTo(mPlayer.getLocation(), mob.getLocation());
				Location loc = mob.getLocation();
				loc.setDirection(dir);
				mob.teleport(loc);
			}
		}

		if (!challenged) {
			mDamageBonusActive = true;
		}

		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2, 1);
		mWorld.spawnParticle(Particle.FLAME, mPlayer.getLocation(), 25, 0.4, 1, 0.4, 0.7f);
		mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation().add(0, 1.25, 0), 250, 0, 0, 0, 0.425);
		mWorld.spawnParticle(Particle.CRIT, mPlayer.getLocation().add(0, 1.25, 0), 300, 0, 0, 0, 1);
		mWorld.spawnParticle(Particle.CRIT_MAGIC, mPlayer.getLocation().add(0, 1.25, 0), 300, 0, 0, 0, 1);
		putOnCooldown();
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (mDamageBonusActive && event.getCause() == DamageCause.ENTITY_ATTACK) {
			event.setDamage(event.getDamage() * (1 + mSoloDamageBonus));
		}

		return true;
	}

}
