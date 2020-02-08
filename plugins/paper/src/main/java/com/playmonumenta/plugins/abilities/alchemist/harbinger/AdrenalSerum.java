package com.playmonumenta.plugins.abilities.alchemist.harbinger;

import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.entity.SplashPotion;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

/*
 * Crouch while throwing an Alchemist Potion and you'll instead consume the
 * potion and buff yourself. You gain 20s of Regen II, Strength I, Speed I, but
 * when the effect ends you take 4 damage.
 * At level 2 you also gain Resistance I and Haste II (Cooldown 20s)
 */

public class AdrenalSerum extends Ability {
	private static final int ADRENAL_SERUM_COOLDOWN = 20 * 20;
	private static final int ADRENAL_SERUM_DURATION = 15 * 20;
	private static final double ADRENAL_SERUM_DAMAGE = 4;
	private static final Particle.DustOptions ADRENAL_SERUM_COLOR = new Particle.DustOptions(Color.fromRGB(185, 0, 0), 1.0f);

	public AdrenalSerum(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.ADRENAL_SERUM;
		mInfo.scoreboardId = "AdrenalSerum";
		mInfo.cooldown = ADRENAL_SERUM_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public boolean runCheck() {
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		if (InventoryUtils.testForItemWithName(inMainHand, "Alchemist's Potion")) {
			return mPlayer.isSneaking();
		}
		return false;
	}

	@Override
	public void cast(Action action) {
		mWorld.spawnParticle(Particle.FLAME, mPlayer.getLocation(), 30, 0.45, 0.45, 0.45, 0.025);
		new BukkitRunnable() {
			double mRotation = 0;
			double mY = 0.15;
			double mRadius = 1;
			@Override
			public void run() {
				Location loc = mPlayer.getLocation();
				mRotation += 17;
				mY += 0.2;
				for (int i = 0; i < 3; i++) {
					double degree = Math.toRadians(mRotation + (i * 120));
					loc.add(Math.cos(degree) * mRadius, mY, Math.sin(degree) * mRadius);
					mWorld.spawnParticle(Particle.FLAME, loc, 1, 0.1, 0.1, 0.1, 0.025);
					mWorld.spawnParticle(Particle.REDSTONE, loc, 3, 0.1, 0.1, 0.1, ADRENAL_SERUM_COLOR);
					mWorld.spawnParticle(Particle.SPELL_INSTANT, loc, 1, 0.1, 0.1, 0.1, 0);
					loc.subtract(Math.cos(degree) * mRadius, mY, Math.sin(degree) * mRadius);
				}

				if (mY >= 1.8) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1, 1);
		mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1, 1);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1f, 1.15f);
		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.SPEED, ADRENAL_SERUM_DURATION, 0, true, true));
		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.REGENERATION, ADRENAL_SERUM_DURATION, 1, true, true));
		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.INCREASE_DAMAGE, ADRENAL_SERUM_DURATION, 0, true, true));
		if (getAbilityScore() == 2) {
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, ADRENAL_SERUM_DURATION, 0, true, true));
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.FAST_DIGGING, ADRENAL_SERUM_DURATION, 1, true, true));
		}

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;
				mWorld.spawnParticle(Particle.REDSTONE, mPlayer.getLocation().add(0, 1, 0), 3, 0.25, 0.45, 0.25, ADRENAL_SERUM_COLOR);

				if (mTicks > ADRENAL_SERUM_DURATION) {
					EntityUtils.damageEntity(mPlugin, mPlayer, ADRENAL_SERUM_DAMAGE, null);

					this.cancel();
					mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1, 1.1f);
					mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1, 1.25f);
					BlockData fallingDustData = Material.RED_NETHER_BRICKS.createBlockData();
					mWorld.spawnParticle(Particle.FALLING_DUST, mPlayer.getLocation().add(0, 1, 0), 45, 0.4, 0.45, 0.24, fallingDustData);
					new BukkitRunnable() {
						double mRotation = 0;
						double mY = 1.9;
						double mRadius = 1;
						@Override
						public void run() {
							Location loc = mPlayer.getLocation();
							mRotation += 17;
							mY -= 0.2;
							for (int i = 0; i < 3; i++) {
								double degree = Math.toRadians(mRotation + (i * 120));
								loc.add(Math.cos(degree) * mRadius, mY, Math.sin(degree) * mRadius);
								mWorld.spawnParticle(Particle.REDSTONE, loc, 3, 0.1, 0.1, 0.1, ADRENAL_SERUM_COLOR);
								mWorld.spawnParticle(Particle.SPELL_INSTANT, loc, 1, 0.1, 0.1, 0.1, 0);
								loc.subtract(Math.cos(degree) * mRadius, mY, Math.sin(degree) * mRadius);
							}

							if (mY <= 0) {
								this.cancel();
							}
						}

					}.runTaskTimer(mPlugin, 0, 1);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public boolean playerThrewSplashPotionEvent(SplashPotion potion) {
		// This is sufficient because we are already checking conditions in runCheck()
		potion.remove();
		putOnCooldown();
		// Consumes a potion - potion.remove() causes the potion to be returned to the player
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		inMainHand.setAmount(inMainHand.getAmount() - 1);

		return true;
	}

}
