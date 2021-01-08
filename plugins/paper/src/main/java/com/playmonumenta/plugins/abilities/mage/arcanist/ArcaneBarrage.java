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
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

public class ArcaneBarrage extends Ability {

	private static final String BARRAGE_EFFECT_NAME = "BarragePercentDamageRecievedEffect";
	private static final int SPELL_VULN_DURATION = 20 * 4;
	private static final double SPELL_VULN_AMOUNT = 0.2;
	private static final double OVERLOAD_AMOUNT_1 = 2;
	private static final double OVERLOAD_AMOUNT_2 = 4;
	private static final EnumSet<DamageCause> SPELL_VULN_DAMAGE_CAUSES = EnumSet.of(DamageCause.CUSTOM);

	private static final double HALF_HITBOX_LENGTH = 0.275;
	private static final int RANGE = 12;
	private static final int MISSILE_COUNT = 3;
	private static final int DAMAGE_1 = 8;
	private static final int DAMAGE_2 = 10;
	private static final int DURATION = 20 * 10;
	private static final int COOLDOWN = 20 * 20;

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(16, 144, 192), 1.0f);

	private final int mDuration;
	private final int mMissileCount;
	private final int mDamage;

	private BukkitRunnable mParticleRunnable;

	private double mOverloadAmount;
	private int mMissiles = 0;
	private int mSummonTick = 0;
	private boolean mOverloadIsActive = false;

	public ArcaneBarrage(Plugin plugin, Player player) {
		super(plugin, player, "Arcane Barrage");
		mInfo.mScoreboardId = "ArcaneBarrage";
		mInfo.mShorthandName = "AB";
		mInfo.mDescriptions.add("Right-click while not sneaking and looking up to summon 3 Arcane Missiles around you for up to 10 seconds. If missiles are active, right-clicking while not sneaking with a Wand fires a missile in the target direction, piercing through enemies within 12 blocks and dealing 8 damage. If cast with Overload, your missiles deal 2 (Overload I) or 4 (Overload II) extra damage. Cooldown: 20s.");
		mInfo.mDescriptions.add("Missiles deal 10 damage and apply 20% Spell Vulnerability for 4 seconds. Mana lances cast while Barrage is active puts Mana Lance on cooldown and casts a missile with the same base damage as the Mana Lance at its level would do instead (does not consume a missile).");
		mInfo.mLinkedSpell = Spells.ARCANE_BARRAGE;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.ALL;
		mInfo.mIgnoreCooldown = true;
		mDuration = DURATION;
		mMissileCount = MISSILE_COUNT;
		mDamage = getAbilityScore() == 1 ? DAMAGE_1 : DAMAGE_2;
	}

	@Override
	public void cast(Action action) {
		if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)
				&& !mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)
				&& !mPlayer.isSneaking() && mPlayer.getLocation().getPitch() < -50 && mMissiles == 0) {
			mMissiles = mMissileCount;
			mSummonTick = mPlayer.getTicksLived();
			World world = mPlayer.getWorld();
			world.playSound(mPlayer.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 0.4f, 1.75f);
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1f, 1.5f);

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
						world.spawnParticle(Particle.REDSTONE, loc, 15, 0.2, 0.2, 0.2, 0.1, COLOR);
						loc.subtract(dx, 0, dz);
					}
				}
			};

			mParticleRunnable.runTaskTimer(mPlugin, 0, 1);

			putOnCooldown();
		} else if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) && mMissiles > 0) {
			//Fires a missile replacing mana lance and sets mana lance on cooldown if barrage is level 2, as well as makes the damage equal to the mana lance damage.
			ManaLance manaLance = AbilityManager.getManager().getPlayerAbility(mPlayer, ManaLance.class);
			int damage = 0;
			if (manaLance != null && !manaLance.isOnCooldown() && getAbilityScore() == 2) {
				damage = manaLance.getDamage();
				manaLance.putOnCooldown();
			} else {
				mMissiles--;
			}
			//Fire missile.
			Location loc = mPlayer.getEyeLocation();
			Vector shift = mPlayer.getLocation().getDirection();
			World world = mPlayer.getWorld();

			world.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 0.75f, 1.5f);
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.25f, 0.5f);
			for (int i = 0; i < RANGE; i++) {
				loc.add(shift);
				world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 5, 0.1, 0.1, 0.1, 0.1);
				world.spawnParticle(Particle.SPELL_WITCH, loc, 5, 0, 0, 0, 0.5);
				world.spawnParticle(Particle.REDSTONE, loc, 20, 0.2, 0.2, 0.2, 0.1, COLOR);
			}

			for (LivingEntity mob : EntityUtils.getMobsInLine(mPlayer.getEyeLocation(), shift, RANGE, HALF_HITBOX_LENGTH)) {
				int damageToBeDealt = damage > 0 ? damage : mDamage;
				if (mOverloadIsActive) {
					Overload overload = AbilityManager.getManager().getPlayerAbility(mPlayer, Overload.class);
					mOverloadAmount = overload.getAbilityScore() == 1 ? OVERLOAD_AMOUNT_1 : OVERLOAD_AMOUNT_2;
					damageToBeDealt += mOverloadAmount;
				}
				EntityUtils.damageEntity(mPlugin, mob, damageToBeDealt, mPlayer, MagicType.ARCANE, true, mInfo.mLinkedSpell);
				MovementUtils.knockAway(mPlayer.getLocation(), mob, 0.2f, 0.2f);
				if (getAbilityScore() == 2) {
					mPlugin.mEffectManager.addEffect(mob, BARRAGE_EFFECT_NAME, new PercentDamageReceived(SPELL_VULN_DURATION, SPELL_VULN_AMOUNT, SPELL_VULN_DAMAGE_CAUSES));
				}
			}
		}
	}

	@Override
	public boolean runCheck() {
		//Cancels barrage if mana lance is up and barrage is level one.
		ManaLance manaLance = AbilityManager.getManager().getPlayerAbility(mPlayer, ManaLance.class);
		if (manaLance != null && !manaLance.isOnCooldown() && mMissiles != 0 && getAbilityScore() == 1) {
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

	public int getMissiles() {
		return mMissiles;
	}
}
