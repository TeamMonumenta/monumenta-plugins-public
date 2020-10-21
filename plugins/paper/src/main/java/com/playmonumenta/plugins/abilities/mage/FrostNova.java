package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;

public class FrostNova extends Ability {

	private static final float RADIUS = 6.0f;
	private static final int DAMAGE_1 = 4;
	private static final int DAMAGE_2 = 8;
	private static final int AMPLIFIER_1 = 1;
	private static final int AMPLIFIER_2 = 3;
	private static final int COOLDOWN = 18 * 20;
	private static final int DURATION = 4 * 20;

	private final int mDamage;
	private final int mSlownessAmplifier;

	public FrostNova(Plugin plugin, Player player) {
		super(plugin, player, "Frost Nova");
		mInfo.mLinkedSpell = Spells.FROST_NOVA;
		mInfo.mScoreboardId = "FrostNova";
		mInfo.mShorthandName = "FN";
		mInfo.mDescriptions.add("When you strike with a wand while you are sneaking, you unleash a frost nova, dealing 4 damage to all enemies in a 6 block radius and afflicting them with 8 seconds of Slowness 2. Also extinguishes fire on nearby players and mobs. Cooldown 18s.");
		mInfo.mDescriptions.add("Increases the damage to 8 and Slowness 4. Bosses and Elites are hit with 8 seconds of Slowness 2 and 8 damage instead.");
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mDamage = getAbilityScore() == 1 ? DAMAGE_1 : DAMAGE_2;
		mSlownessAmplifier = getAbilityScore() == 1 ? AMPLIFIER_1 : AMPLIFIER_2;
	}

	@Override
	public void cast(Action action) {
		putOnCooldown();

		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), RADIUS, mPlayer)) {
			Vector velocity = mob.getVelocity();
			EntityUtils.damageEntity(mPlugin, mob, mDamage, mPlayer, MagicType.ICE, true, mInfo.mLinkedSpell);
			mob.setVelocity(velocity);
			if (EntityUtils.isElite(mob) || EntityUtils.isBoss(mob)) {
				PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW, DURATION, mSlownessAmplifier - 1, true, false));
			} else {
				PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW, DURATION, mSlownessAmplifier, true, false));
			}

			if (mob.getFireTicks() > 1) {
				mob.setFireTicks(1);
			}
		}

		// Extinguish fire on all nearby players
		for (Player player : PlayerUtils.playersInRange(mPlayer.getLocation(), RADIUS)) {
			if (player.getFireTicks() > 1) {
				player.setFireTicks(1);
			}
		}

		World world = mPlayer.getWorld();
		new BukkitRunnable() {
			double mRadius = 0;
			final Location mLoc = mPlayer.getLocation();
			@Override
			public void run() {
				mRadius += 1.25;
				for (double j = 0; j < 360; j += 18) {
					double radian1 = Math.toRadians(j);
					mLoc.add(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
					world.spawnParticle(Particle.CLOUD, mLoc, 1, 0, 0, 0, 0.1);
					world.spawnParticle(Particle.CRIT_MAGIC, mLoc, 8, 0, 0, 0, 0.65);
					mLoc.subtract(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
				}

				if (mRadius >= RADIUS + 1) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
		Location loc = mPlayer.getLocation().add(0, 1, 0);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 0.65f);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 0.45f);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.25f);
		world.spawnParticle(Particle.CLOUD, loc, 25, 0, 0, 0, 0.35);
		world.spawnParticle(Particle.SPIT, loc, 35, 0, 0, 0, 0.45);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.0f);
	}

	@Override
	public boolean runCheck() {
		if (mPlayer.isSneaking()) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			return InventoryUtils.isWandItem(mainHand);
		}
		return false;
	}

}
