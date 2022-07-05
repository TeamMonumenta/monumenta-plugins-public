package com.playmonumenta.plugins.abilities.warlock.reaper;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.effects.CustomRegeneration;
import com.playmonumenta.plugins.effects.JudgementChainMobEffect;
import com.playmonumenta.plugins.effects.Paralyze;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class JudgementChain extends Ability {
	private static final int COOLDOWN = 25 * 20;
	private static final int DURATION = 20 * 20;
	private static final int BUFF_DURATION = 10 * 20;
	private static final double CHAIN_BREAK_DAMAGE = 20;
	private static final int CHAIN_BREAK_DAMAGE_RANGE = 4;
	private static final int CHAIN_BREAK_EFFECT_RANGE = 8;
	private static final double BUFF_AMOUNT = 0.1;
	private static final int RANGE = 16;
	private static final double HITBOX_LENGTH = 0.5;

	private static final String EFFECT_NAME = "JudgementChainEffectName";
	private static final String SPEED_NAME = "JudgementChainSpeedEffect";
	private static final String STRENGTH_NAME = "JudgementChainStrengthEffect";
	private static final String PARA_NAME = "JudgementChainParalyzeEffect";
	private static final String KBR_NAME = "JudgementChainKBREffect";
	private static final String DEF_NAME = "JudgementChainDefenseEffect";
	private static final String HEAL_NAME = "JudgementChainRegenEffect";
	private static final String DOT_NAME = "JudgementChainDOTEffect";
	private static final String HEAL_RATE_NAME = "JudgementChainPercentHealEffect";
	private static final EnumSet<DamageType> AFFECTED_DAMAGE_TYPES = EnumSet.of(
		DamageType.MELEE,
		DamageType.MELEE_ENCH,
		DamageType.MELEE_SKILL,
		DamageType.PROJECTILE,
		DamageType.PROJECTILE_SKILL,
		DamageType.MAGIC
	);

	public static final String CHARM_COOLDOWN = "Judgement Chain Cooldown";
	public static final String CHARM_DAMAGE = "Judgement Chain Damage";
	public static final String CHARM_RANGE = "Judgement Chain Range";
	public static final String CHARM_DURATION = "Judgement Chain Buff Duration";

	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(217, 217, 217), 1.0f);
	private static final Particle.DustOptions DARK_COLOR = new Particle.DustOptions(Color.fromRGB(13, 13, 13), 1.0f);

	private final double mAmplifier;

	private @Nullable LivingEntity mTarget = null;
	private boolean mChainActive = false;

	public JudgementChain(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Judgement Chain");
		mInfo.mLinkedSpell = ClassAbility.JUDGEMENT_CHAIN;
		mInfo.mScoreboardId = "JudgementChain";
		mInfo.mShorthandName = "JC";
		mInfo.mDescriptions.add("Press the swap key while not sneaking targeting a non-boss hostile mob to conjure an unbreakable chain, linking the Reaper and the mob. As long as another mob is within 8 blocks, the mob becomes immortal for 20 seconds, can only target or damage the Reaper, is slowed by 25%, and deals 50% less damage. All debuffs on the chained mob are inverted to their positive counterpart and transferred to the Reaper for 10s, capped at 10%. Pressing swap while a mob is already chained will pull it towards you, dealing 20 magic damage and breaking the chain. Walking 16+ blocks away will deal damage but not pull the mob. Cooldown: 25s.");
		mInfo.mDescriptions.add("When breaking the chain, apply all the positively inverted debuffs to other players and all debuffs (capped at 10%) to other mobs in an 8 block radius of the player for 10s. Additionally, deal 20 magic damage to all mobs in a 4 block radius of the player.");
		mInfo.mCooldown = CharmManager.getCooldown(player, CHARM_COOLDOWN, COOLDOWN);
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.CHAIN, 1);
		mAmplifier = BUFF_AMOUNT;
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		if (mPlayer == null) {
			return;
		}
		ItemStack mainHandItem = mPlayer.getInventory().getItemInMainHand();
		if (ItemUtils.isHoe(mainHandItem)) {
			event.setCancelled(true);
			if (!mPlayer.isSneaking() && mPlayer.isOnGround()) {
				if (mChainActive) {
					breakChain(true, true);
					mTarget = null;
					mChainActive = false;
					ClientModHandler.updateAbility(mPlayer, this);
				} else {
					summonChain();
					mChainActive = true;
				}
			}
		}
	}

	public void summonChain() {
		if (mPlayer == null || mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
			return;
		}

		Location loc = mPlayer.getEyeLocation();
		World world = mPlayer.getWorld();

		LivingEntity e = EntityUtils.getEntityAtCursor(mPlayer, RANGE, false, true, true);
		if (e != null && !EntityUtils.isBoss(e) && EntityUtils.isHostileMob(e)) {
			mTarget = e;
			world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.5f, 0.25f);
			world.playSound(loc, Sound.BLOCK_ANVIL_FALL, SoundCategory.PLAYERS, 1.75f, 1.0f);
			mPlugin.mEffectManager.addEffect(mTarget, EFFECT_NAME, new JudgementChainMobEffect(DURATION, mPlayer, EFFECT_NAME));
			EntityUtils.applyTaunt(mPlugin, mTarget, mPlayer);

			new BukkitRunnable() {
				final int mRunnableDuration = DURATION;
				final double mWidth = mTarget.getWidth() / 2;
				int mT = 0;

				@Override
				public void run() {
					if (mPlayer == null) {
						return;
					}
					Location l = mPlayer.getEyeLocation();
					mT++;
					if (mTarget != null) {
						Location mLoc = mTarget.getLocation().add(0, mTarget.getHeight() / 2, 0);
						Vector chainVector = new Vector(mLoc.getX() - l.getX(), mLoc.getY() - l.getY(), mLoc.getZ() - l.getZ()).normalize().multiply(0.5);
						Vector shift = chainVector.normalize().multiply(HITBOX_LENGTH);
						BoundingBox box = BoundingBox.of(l, HITBOX_LENGTH, HITBOX_LENGTH, HITBOX_LENGTH);
						box.shift(chainVector);

						new PartialParticle(Particle.REDSTONE, mLoc, 1, mWidth, mWidth, mWidth, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.REDSTONE, mLoc, 1, mWidth, mWidth, mWidth, 0, DARK_COLOR).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.CRIT, mLoc, 1, mWidth, mWidth, mWidth, 0).spawnAsPlayerActive(mPlayer);
						int particleReduce = 0;
						for (double r = 0; r <= l.distance(mLoc); r += HITBOX_LENGTH) {
							Location bLoc = box.getCenter().toLocation(world);
							if (particleReduce % 2 == 0) {
								new PartialParticle(Particle.REDSTONE, bLoc, 1, 0.05, 0.05, 0.05, 0.075, DARK_COLOR).spawnAsPlayerActive(mPlayer);
							}
							box.shift(shift);
							particleReduce++;
						}

						applyEffects(mPlayer, false);

						List<LivingEntity> mobs = EntityUtils.getMobsInLine(l, chainVector, l.distance(mLoc), HITBOX_LENGTH);
						mobs.remove(mTarget);
						List<Player> players = EntityUtils.getPlayersInLine(l, chainVector, l.distance(mLoc), HITBOX_LENGTH, mPlayer);

						for (Player pl : players) {
							applyEffects(pl, false);
						}

						for (LivingEntity m : mobs) {
							applyEffects(m, true);
						}
					}
					if (mTarget == null || mT >= mRunnableDuration || mPlayer.isDead() || (mTarget != null && (mTarget.isDead() || !mTarget.isValid()))) {
						this.cancel();
						if (mTarget != null) {
							breakChain(false, false);
							mTarget = null;
							mChainActive = false;
							ClientModHandler.updateAbility(mPlayer, JudgementChain.this);
						}
					} else if (l.distance(mTarget.getLocation()) > RANGE) {
						this.cancel();
						breakChain(true, false);
						mTarget = null;
						mChainActive = false;
						ClientModHandler.updateAbility(mPlayer, JudgementChain.this);
					}
				}

			}.runTaskTimer(mPlugin, 0, 1);

			// This loop only runs at most once!
			putOnCooldown();
		}
	}

	public void breakChain(boolean doDamage, boolean doPull) {
		if (mTarget != null && mPlayer != null) {
			mPlugin.mEffectManager.clearEffects(mTarget, EFFECT_NAME);
			applyEffects(mPlayer, false);

			Location loc = mPlayer.getEyeLocation();
			Location mLoc = mTarget.getLocation();
			World world = mPlayer.getWorld();

			new PartialParticle(Particle.REDSTONE, loc.add(0, mPlayer.getHeight() / 2, 0), 15, CHAIN_BREAK_EFFECT_RANGE, CHAIN_BREAK_EFFECT_RANGE, CHAIN_BREAK_EFFECT_RANGE, 0.125, LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.REDSTONE, loc.add(0, mPlayer.getHeight() / 2, 0), 15, CHAIN_BREAK_EFFECT_RANGE, CHAIN_BREAK_EFFECT_RANGE, CHAIN_BREAK_EFFECT_RANGE, 0.125, DARK_COLOR).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.CRIT, loc.add(0, mPlayer.getHeight() / 2, 0), 30, CHAIN_BREAK_DAMAGE_RANGE, CHAIN_BREAK_DAMAGE_RANGE, CHAIN_BREAK_DAMAGE_RANGE, 0.125).spawnAsPlayerActive(mPlayer);
			world.playSound(mLoc, Sound.BLOCK_ANVIL_DESTROY, SoundCategory.PLAYERS, 0.7f, 0.6f);
			world.playSound(loc, Sound.BLOCK_ANVIL_DESTROY, SoundCategory.PLAYERS, 0.6f, 0.6f);

			if (isLevelTwo()) {
				for (LivingEntity m : EntityUtils.getNearbyMobs(loc, CHAIN_BREAK_EFFECT_RANGE, mTarget)) {
					applyEffects(m, true);
				}
				for (Player p : PlayerUtils.playersInRange(loc, CHAIN_BREAK_EFFECT_RANGE, false)) {
					applyEffects(p, false);
				}
			}

			if (doPull) {
				MovementUtils.pullTowardsStop(mPlayer, mTarget);
				EntityUtils.applyWeaken(mPlugin, 20, 1, mTarget);
			}

			double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, CHAIN_BREAK_DAMAGE);
			DamageUtils.damage(mPlayer, mTarget, DamageType.MAGIC, damage, mInfo.mLinkedSpell, false, false);

			if (doDamage && isLevelTwo()) {
				double damageRadius = CharmManager.getRadius(mPlayer, CHARM_RANGE, CHAIN_BREAK_DAMAGE_RANGE);
				for (LivingEntity m : EntityUtils.getNearbyMobs(loc, damageRadius, mTarget)) {
					DamageUtils.damage(mPlayer, m, DamageType.MAGIC, damage, mInfo.mLinkedSpell);
				}
			}
		}
	}

	private void applyEffects(LivingEntity e, boolean isHostile) {
		int duration = BUFF_DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_DURATION);

		boolean isBleed = EntityUtils.isBleeding(mPlugin, mTarget);
		if (EntityUtils.isSlowed(mPlugin, mTarget) || isBleed) {
			if (isHostile) {
				if (EntityUtils.isSlowed(mPlugin, mTarget)) {
					EntityUtils.applySlow(mPlugin, duration, mAmplifier, e);
				}
				if (isBleed) {
					EntityUtils.applyBleed(mPlugin, duration, mAmplifier, e);
				}
			} else {
				mPlugin.mEffectManager.addEffect(e, SPEED_NAME, new PercentSpeed(duration, mAmplifier, SPEED_NAME));
			}
		}

		if (EntityUtils.isWeakened(mPlugin, mTarget) || isBleed) {
			if (isHostile) {
				if (EntityUtils.isWeakened(mPlugin, mTarget)) {
					EntityUtils.applyWeaken(mPlugin, duration, mAmplifier, e);
				}
				if (isBleed) {
					EntityUtils.applyBleed(mPlugin, duration, mAmplifier, e);
				}
			} else {
				mPlugin.mEffectManager.addEffect(e, STRENGTH_NAME, new PercentDamageDealt(duration, mAmplifier, AFFECTED_DAMAGE_TYPES));
			}
		}

		if (mTarget.getFireTicks() > 0) {
			if (isHostile) {
				e.setFireTicks(Math.max(duration, e.getFireTicks()));
			} else {
				PotionUtils.applyPotion(mPlayer, e, new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, 0, false, true));
			}
		}

		if (EntityUtils.isParalyzed(mPlugin, mTarget)) {
			if (isHostile) {
				mPlugin.mEffectManager.addEffect(e, PARA_NAME, new Paralyze(duration, mPlugin));
			} else {
				mPlugin.mEffectManager.addEffect(e, KBR_NAME, new PercentKnockbackResist(duration, mAmplifier, KBR_NAME));
			}
		}

		if (EntityUtils.isVulnerable(mPlugin, mTarget)) {
			if (isHostile) {
				EntityUtils.applyVulnerability(mPlugin, duration, mAmplifier, e);
			} else {
				mPlugin.mEffectManager.addEffect(e, DEF_NAME, new PercentDamageReceived(duration, -mAmplifier));
			}
		}

		if (mTarget.hasPotionEffect(PotionEffectType.POISON) || mTarget.hasPotionEffect(PotionEffectType.WITHER)
				|| EntityUtils.hasDamageOverTime(mPlugin, mTarget)) {
			if (isHostile) {
				mPlugin.mEffectManager.addEffect(e, DOT_NAME, new CustomDamageOverTime(duration, 1, 20, mPlayer, null, Particle.SQUID_INK));
			} else if (mPlayer != e) {
				mPlugin.mEffectManager.addEffect(e, HEAL_NAME, new CustomRegeneration(duration, 0.333, mPlugin));
			} else {
				// 1 / 60 = 1/60th HP every tick, 60 ticks in 3 second interval
				// We do this because constant re-application doesn't actually do anything
				PlayerUtils.healPlayer(mPlugin, mPlayer, 1.0d / 60.0d, mPlayer);
				// This technically only starts to tick down after cast ends
				mPlugin.mEffectManager.addEffect(e, HEAL_NAME, new CustomRegeneration(duration, 0.333, mPlugin));
			}
		}

		if (mTarget.hasPotionEffect(PotionEffectType.HUNGER)) {
			if (isHostile) {
				PotionUtils.applyPotion(mPlayer, e, new PotionEffect(PotionEffectType.HUNGER, duration, 0, false, true));
			} else {
				mPlugin.mEffectManager.addEffect(e, HEAL_RATE_NAME, new PercentHeal(duration, mAmplifier));
			}
		}
	}

	@Override
	public @Nullable String getMode() {
		return mTarget != null ? "active" : null;
	}
}
