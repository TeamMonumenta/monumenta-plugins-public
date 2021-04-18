package com.playmonumenta.plugins.abilities.warlock.reaper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.enchantments.Inferno;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.effects.JudgementChainMobEffect;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.tracking.PlayerTracking;

public class JudgementChain extends Ability {

	private static final String CHECK_ONCE_THIS_TICK_METAKEY = "JudgementChainTickRightClicked";

	private static final int COOLDOWN = 25 * 20;
	private static final int DURATION = 20 * 20;
	private static final int BUFF_DURATION = 10 * 20;
	private static final int AMPLIFIER_CAP_1 = 0;
	private static final int AMPLIFIER_CAP_2 = 1;
	private static final double CHAIN_BREAK_DAMAGE = 20;
	private static final int CHAIN_BREAK_RANGE = 3;
	private static final int RANGE = 16;
	private static final double HITBOX_LENGTH = 0.5;
	private static final String EFFECT_NAME = "JudgementChainEffectName";

	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(217, 217, 217), 1.0f);
	private static final Particle.DustOptions DARK_COLOR = new Particle.DustOptions(Color.fromRGB(13, 13, 13), 1.0f);

	private final int mAmplifierCap;

	private LivingEntity mTarget = null;
	private int mRightClicks = 0;

	public JudgementChain(Plugin plugin, Player player) {
		super(plugin, player, "Judgement Chain");
		mInfo.mLinkedSpell = Spells.JUDGEMENT_CHAIN;
		mInfo.mScoreboardId = "JudgementChain";
		mInfo.mShorthandName = "JC";
		mInfo.mDescriptions.add("Double right-click while looking at a non-boss hostile mob to conjure an unbreakable chain, linking the soul of the Reaper and the mob. The mob becomes immortal (as long as there is another mob within 8 blocks) for the next 20 seconds and will only target the Reaper it is chained to, but it is slowed by 25% and deals 50% less damage to the Reaper, and no damage to all other players. All debuffs on the chained mob are inverted to their positive counterpart and transferred to the Reaper for 10s, capped at level 1. Bosses cannot be chained. Shift + Swap while looking up, or move more than 16 blocks away from the mob, to break the chain and cancel the immortality on the mob, dealing 20 damage to it. Cooldown: 25s.");
		mInfo.mDescriptions.add("Players that walk through the chain linking the mob and the Reaper are granted 10s of the inverted debuffs, and mobs that walk through the chain are given 10s of the debuffs on the mob. Additionally, the level of buffs and debuffs is preserved, up to level 2, except for Resistance and Regeneration, and the damage from the active chain break is now applied to all mobs in a 3 block radius of the chained mob.");
		mInfo.mCooldown = COOLDOWN;
		mInfo.mIgnoreCooldown = true;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mAmplifierCap = getAbilityScore() == 1 ? AMPLIFIER_CAP_1 : AMPLIFIER_CAP_2;
	}

	@Override
	public boolean runCheck() {
		return InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand());
	}

	@Override
	public void cast(Action action) {
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
			return;
		}
		if (MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, CHECK_ONCE_THIS_TICK_METAKEY)) {
			mRightClicks++;
			new BukkitRunnable() {
				@Override
				public void run() {
					if (mRightClicks > 0) {
						mRightClicks--;
					}
					this.cancel();
				}
			}.runTaskLater(mPlugin, 5);
		}
		if (mRightClicks < 2) {
			return;
		}
		mRightClicks = 0;

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
					Location l = mPlayer.getEyeLocation();
					mT++;
					if (mTarget != null) {
						Location mLoc = mTarget.getLocation().add(0, mTarget.getHeight() / 2, 0);
						Vector chainVector = new Vector(mLoc.getX() - l.getX(), mLoc.getY() - l.getY(), mLoc.getZ() - l.getZ()).normalize().multiply(0.5);
						Vector shift = chainVector.normalize().multiply(HITBOX_LENGTH);
						BoundingBox box = BoundingBox.of(l, HITBOX_LENGTH, HITBOX_LENGTH, HITBOX_LENGTH);
						box.shift(chainVector);

						world.spawnParticle(Particle.REDSTONE, mLoc, 1, mWidth, mWidth, mWidth, 0, LIGHT_COLOR);
						world.spawnParticle(Particle.REDSTONE, mLoc, 1, mWidth, mWidth, mWidth, 0, DARK_COLOR);
						world.spawnParticle(Particle.CRIT, mLoc, 1, mWidth, mWidth, mWidth, 0);
						int particleReduce = 0;
						for (double r = 0; r <= l.distance(mLoc); r += HITBOX_LENGTH) {
							Location bLoc = box.getCenter().toLocation(world);
							if (particleReduce % 2 == 0) {
								world.spawnParticle(Particle.REDSTONE, bLoc, 1, 0.05, 0.05, 0.05, 0.075, DARK_COLOR);
								world.spawnParticle(Particle.FALLING_DUST, bLoc, 1, 0.05, 0.05, 0.05, Material.CHAIN.createBlockData());
							}
							box.shift(shift);
							particleReduce++;
						}

						Map<PotionEffectType, Integer> effects = getOppositeEffects(mTarget);
						if (EntityUtils.isSlowed(mPlugin, mTarget)) {
							mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.SPEED, BUFF_DURATION, Math.min(mAmplifierCap, (int) (EntityUtils.getSlowAmount(mPlugin, mTarget) * 10) - 1), true, true));
						}
						if (EntityUtils.isWeakened(mPlugin, mTarget)) {
							mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.INCREASE_DAMAGE, BUFF_DURATION, Math.min(mAmplifierCap, (int) (EntityUtils.getWeakenAmount(mPlugin, mTarget) * 10) - 1), true, true));
						}
						for (Map.Entry<PotionEffectType, Integer> effect : effects.entrySet()) {
							if (effect.getKey() == PotionEffectType.DAMAGE_RESISTANCE) {
								// Only do Resistance I regardless of Vulnerability level
								mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_OTHER, new PotionEffect(effect.getKey(), BUFF_DURATION, 0, true, true));
							} else if (effect.getKey() == PotionEffectType.REGENERATION) {
								// Only do Regeneration I regardless of Poison/Wither level
								mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_OTHER, new PotionEffect(effect.getKey(), BUFF_DURATION, 0, true, true));
								// Simulate Regen (1 health every 50 ticks = 1/50 health every tick) since constant application never heals
								PlayerUtils.healPlayer(mPlayer, 1.0d / 50.0d);
							} else {
								mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_OTHER, new PotionEffect(effect.getKey(), BUFF_DURATION, Math.min(mAmplifierCap, effect.getValue()), true, true));
							}
						}
						if (getAbilityScore() > 1) {
							List<LivingEntity> mobs = EntityUtils.getMobsInLine(l, chainVector, l.distance(mLoc), HITBOX_LENGTH);
							mobs.remove(mTarget);
							List<Player> players = EntityUtils.getPlayersInLine(l, chainVector, l.distance(mLoc), HITBOX_LENGTH, mPlayer);

							for (Player pl : players) {
								if (EntityUtils.isSlowed(mPlugin, mTarget)) {
									mPlugin.mPotionManager.addPotion(pl, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.SPEED, BUFF_DURATION, Math.min(mAmplifierCap, (int) (EntityUtils.getSlowAmount(mPlugin, mTarget) * 10) - 1), true, true));
								}
								if (EntityUtils.isWeakened(mPlugin, mTarget)) {
									mPlugin.mPotionManager.addPotion(pl, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.INCREASE_DAMAGE, BUFF_DURATION, Math.min(mAmplifierCap, (int) (EntityUtils.getWeakenAmount(mPlugin, mTarget) * 10) - 1), true, true));
								}
								for (Map.Entry<PotionEffectType, Integer> effect : effects.entrySet()) {
									if (effect.getKey() == PotionEffectType.DAMAGE_RESISTANCE) {
										// Only do Resistance I regardless of Vulnerability level
										mPlugin.mPotionManager.addPotion(pl, PotionID.ABILITY_OTHER, new PotionEffect(effect.getKey(), BUFF_DURATION, 0, true, true));
									} else if (effect.getKey() == PotionEffectType.REGENERATION) {
										// Only do Regeneration I regardless of Poison/Wither level
										mPlugin.mPotionManager.addPotion(pl, PotionID.ABILITY_OTHER, new PotionEffect(effect.getKey(), BUFF_DURATION, 0, true, true));
									} else {
										mPlugin.mPotionManager.addPotion(pl, PotionID.ABILITY_OTHER, new PotionEffect(effect.getKey(), BUFF_DURATION, Math.min(mAmplifierCap, effect.getValue()), true, true));
									}
								}
							}

							for (LivingEntity m : mobs) {
								if (!(m instanceof Player)) {
									if (EntityUtils.isSlowed(mPlugin, mTarget)) {
										EntityUtils.applySlow(mPlugin, BUFF_DURATION, EntityUtils.getSlowAmount(mPlugin, mTarget), m);
									}
									if (EntityUtils.isWeakened(mPlugin, mTarget)) {
										EntityUtils.applyWeaken(mPlugin, BUFF_DURATION, EntityUtils.getWeakenAmount(mPlugin, mTarget), m);
									}
									if (EntityUtils.isBleeding(mPlugin, mTarget)) {
										EntityUtils.applyBleed(mPlugin, BUFF_DURATION, EntityUtils.getBleedLevel(mPlugin, mTarget), m);
									}
									if (mTarget.getFireTicks() > 0 && (m.getFireTicks() <= 0 && !Inferno.mobHasInferno(mPlugin, m)
											&& (!EntityUtils.isFireResistant(m) || PlayerTracking.getInstance().getPlayerCustomEnchantLevel(mPlayer, Inferno.class) > 0)
											&& m.getLocation().getBlock().getType() != Material.WATER)) {
										EntityUtils.applyFire(mPlugin, BUFF_DURATION, m, mPlayer);
									}
									for (PotionEffect effect : mTarget.getActivePotionEffects()) {
										PotionEffectType type = effect.getType();
										if (PotionUtils.hasNegativeEffects(type)) {
											PotionUtils.applyPotion(mPlayer, m, new PotionEffect(type, BUFF_DURATION, Math.min(mAmplifierCap, effect.getAmplifier()), true, true));
										}
									}
								}
							}
						}
					}
					if (mTarget == null || mT >= mRunnableDuration || mPlayer.isDead() || (mTarget != null && (mTarget.isDead() || !mTarget.isValid()))) {
						this.cancel();
						if (mTarget != null) {
							breakChain(false);
						}
						mTarget = null;
					} else if (l.distance(mTarget.getLocation()) > RANGE) {
						this.cancel();
						if (mTarget != null) {
							breakChain(true);
						}
						mTarget = null;
					}
				}

			}.runTaskTimer(mPlugin, 0, 1);

			// This loop only runs at most once!
			putOnCooldown();
		}
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		ItemStack mainHandItem = mPlayer.getInventory().getItemInMainHand();
		if (InventoryUtils.isScytheItem(mainHandItem)) {
			event.setCancelled(true);
			if (mPlayer.isSneaking() && mPlayer.getLocation().getPitch() < -50.0) {
				breakChain(true);
				mTarget = null;
			}
		}
	}

	public void breakChain(boolean doDamage) {
		if (mTarget != null) {
			mPlugin.mEffectManager.clearEffects(mTarget, EFFECT_NAME);

			Location loc = mPlayer.getEyeLocation();
			Location mLoc = mTarget.getLocation();
			World world = mPlayer.getWorld();

			world.spawnParticle(Particle.REDSTONE, mLoc.add(0, mTarget.getHeight() / 2, 0), 15, CHAIN_BREAK_RANGE, CHAIN_BREAK_RANGE, CHAIN_BREAK_RANGE, 0.125, LIGHT_COLOR);
			world.spawnParticle(Particle.REDSTONE, mLoc.add(0, mTarget.getHeight() / 2, 0), 15, CHAIN_BREAK_RANGE, CHAIN_BREAK_RANGE, CHAIN_BREAK_RANGE, 0.125, DARK_COLOR);
			world.spawnParticle(Particle.CRIT, mLoc.add(0, mTarget.getHeight() / 2, 0), 30, CHAIN_BREAK_RANGE, CHAIN_BREAK_RANGE, CHAIN_BREAK_RANGE, 0.125);
			world.playSound(mLoc, Sound.BLOCK_ANVIL_DESTROY, SoundCategory.PLAYERS, 0.7f, 0.6f);
			world.playSound(loc, Sound.BLOCK_ANVIL_DESTROY, SoundCategory.PLAYERS, 0.6f, 0.6f);

			if (doDamage) {
				EntityUtils.damageEntity(mPlugin, mTarget, CHAIN_BREAK_DAMAGE, mPlayer, MagicType.DARK_MAGIC, true, mInfo.mLinkedSpell);
				if (getAbilityScore() > 1) {
					for (LivingEntity m : EntityUtils.getNearbyMobs(mTarget.getLocation(), CHAIN_BREAK_RANGE, mTarget)) {
						EntityUtils.damageEntity(mPlugin, m, CHAIN_BREAK_DAMAGE, mPlayer, MagicType.DARK_MAGIC, true, mInfo.mLinkedSpell);
					}
				}
			}
		}
	}

	private Map<PotionEffectType, Integer> getOppositeEffects(LivingEntity e) {
		Map<PotionEffectType, Integer> effects = new HashMap<>();
		for (PotionEffect effect : e.getActivePotionEffects()) {
			PotionEffectType type = effect.getType();
			if (PotionUtils.hasNegativeEffects(type)) {
				type = PotionUtils.getOppositeEffect(type);
				if (type != null) {
					effects.put(type, effect.getAmplifier());
				}
			}
		}
		if (e.getFireTicks() > 0) {
			effects.put(PotionEffectType.FIRE_RESISTANCE, 0);
		}
		if (EntityUtils.isBleeding(mPlugin, e)) {
			effects.put(PotionEffectType.SPEED, EntityUtils.getBleedLevel(mPlugin, e) - 1);
			effects.put(PotionEffectType.INCREASE_DAMAGE, EntityUtils.getBleedLevel(mPlugin, e) - 1);
		}
		return effects;
	}
}
