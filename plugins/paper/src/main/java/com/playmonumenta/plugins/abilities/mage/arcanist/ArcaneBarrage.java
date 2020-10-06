package com.playmonumenta.plugins.abilities.mage.arcanist;

import java.util.EnumSet;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.mage.ManaLance;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.effects.OverloadBarrage;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

public class ArcaneBarrage extends Ability {

	private static final String OVERLOAD_EFFECT_NAME = "OverloadFlatDamageDealtEffect";
	private static final int OVERLOAD_DURATION = 20 * 4;
	private static final double OVERLOAD_AMOUNT_1 = 1;
	private static final double OVERLOAD_AMOUNT_2 = 2;
	private static final EnumSet<DamageCause> OVERLOAD_DAMAGE_CAUSES = EnumSet.of(DamageCause.CUSTOM);

	private static final double HALF_HITBOX_LENGTH = 0.275;
	private static final int RANGE = 12;
	private static final int MISSILE_COUNT_1 = 3;
	private static final int MISSILE_COUNT_2 = 5;
	private static final int DAMAGE = 7;
	private static final int DURATION_1 = 20 * 10;
	private static final int DURATION_2 = 20 * 20;
	private static final int COOLDOWN = 20 * 20;

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(16, 144, 192), 1.0f);

	private final int mDuration;
	private final int mMissileCount;

	private BukkitRunnable mParticleRunnable;

	private double mOverloadAmount;
	private int mMissiles = 0;
	private int mSummonTick = 0;
	private boolean mOverloadIsActive = false;

	public ArcaneBarrage(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Arcane Barrage");
		mInfo.mScoreboardId = "ArcaneBarrage";
		mInfo.mShorthandName = "AB";
		mInfo.mDescriptions.add("Right-click while not sneaking and looking up to summon 3 Arcane Missiles around you for up to 10 seconds. If missiles are active, right-clicking while not sneaking with a Wand fires a missile in the target direction, piercing through enemies within 12 blocks and dealing 7 damage. If cast with Overload, mobs damaged by the spear take 1 (Overload level 1) or 2 (Overload level 2) more ability damage (from any player) for 4 seconds. Cooldown: 20s.");
		mInfo.mDescriptions.add("Missiles last for 20 seconds instead, and gain 5 missiles when casting.");
		mInfo.mLinkedSpell = Spells.ARCANE_BARRAGE;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.ALL;
		mInfo.mIgnoreCooldown = true;
		mDuration = getAbilityScore() == 1 ? DURATION_1 : DURATION_2;
		mMissileCount = getAbilityScore() == 1 ? MISSILE_COUNT_1 : MISSILE_COUNT_2;
	}

	@Override
	public void cast(Action action) {
		if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)
				&& !mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)
				&& !mPlayer.isSneaking() && mPlayer.getLocation().getPitch() < -50 && mMissiles == 0) {
			mMissiles = mMissileCount;
			mSummonTick = mPlayer.getTicksLived();
			mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 0.4f, 1.75f);
			mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1f, 1.5f);

			mParticleRunnable = new BukkitRunnable() {
				final int mRadius = getAbilityScore() == 1 ? 2 : 3;
				final double mRotationSpacing = 2 * Math.PI / mMissileCount;
				final double mRotationIncrement = Math.PI / 20 / mRadius;
				double mRotationOffset = 0;
				int mTicks = 0;

				@Override
				public void run() {
					mTicks++;
					if (mTicks > mDuration || mMissiles == 0) {
						mMissiles = 0;
						this.cancel();
					}

					mRotationOffset += mRotationIncrement;

					Location loc = mPlayer.getLocation().add(0, 2.5, 0);
					for (int i = 0; i < mMissiles; i++) {
						double rotation = i * mRotationSpacing + mRotationOffset;
						double dx = mRadius * FastUtils.cos(rotation);
						double dz = mRadius * FastUtils.sin(rotation);

						loc.add(dx, 0, dz);
						mWorld.spawnParticle(Particle.REDSTONE, loc, 15, 0.2, 0.2, 0.2, 0.1, COLOR);
						loc.subtract(dx, 0, dz);
					}
				}
			};

			mParticleRunnable.runTaskTimer(mPlugin, 0, 1);

			putOnCooldown();
		} else if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) && mMissiles > 0) {
			mMissiles--;
			Location loc = mPlayer.getEyeLocation();
			Vector shift = mPlayer.getLocation().getDirection();

			mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 0.75f, 1.5f);
			mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.25f, 0.5f);
			for (int i = 0; i < RANGE; i++) {
				loc.add(shift);
				mWorld.spawnParticle(Particle.FIREWORKS_SPARK, loc, 5, 0.1, 0.1, 0.1, 0.1);
				mWorld.spawnParticle(Particle.SPELL_WITCH, loc, 5, 0, 0, 0, 0.5);
				mWorld.spawnParticle(Particle.REDSTONE, loc, 20, 0.2, 0.2, 0.2, 0.1, COLOR);
			}

			for (LivingEntity mob : EntityUtils.getMobsInLine(mPlayer.getEyeLocation(), shift, RANGE, HALF_HITBOX_LENGTH)) {
				EntityUtils.damageEntity(mPlugin, mob, DAMAGE, mPlayer, MagicType.ARCANE, true, mInfo.mLinkedSpell);
				MovementUtils.knockAway(mPlayer.getLocation(), mob, 0.2f, 0.2f);
				if (mOverloadIsActive) {
					Overload overload = AbilityManager.getManager().getPlayerAbility(mPlayer, Overload.class);
					mOverloadAmount = overload.getAbilityScore() == 1 ? OVERLOAD_AMOUNT_1 : OVERLOAD_AMOUNT_2;
					mPlugin.mEffectManager.addEffect(mob, OVERLOAD_EFFECT_NAME, new OverloadBarrage(OVERLOAD_DURATION, mOverloadAmount, OVERLOAD_DAMAGE_CAUSES));
				}
			}
		}
	}

	@Override
	public boolean runCheck() {
		//Cancels if mana lance is able to be cast and missiles are up.
		ManaLance manaLance = AbilityManager.getManager().getPlayerAbility(mPlayer, ManaLance.class);
		if (manaLance != null && !manaLance.isOnCooldown() && mMissiles != 0) {
			return false;
		}
        return (!mPlayer.isSneaking() && InventoryUtils.isWandItem(mPlayer.getInventory().getItemInMainHand()));
	}

	public void activateOverload() {
		mOverloadIsActive = true;
		new BukkitRunnable() {
			@Override
			public void run() {
				mOverloadIsActive = false;
			}
		}.runTaskLater(mPlugin, mDuration - 1);
	}

	public boolean shouldCancelManaLance() {
		return mSummonTick == mPlayer.getTicksLived();
	}

	@Override
	public void invalidate() {
		if (mParticleRunnable != null && !mParticleRunnable.isCancelled()) {
			mParticleRunnable.cancel();
		}
	}

}
