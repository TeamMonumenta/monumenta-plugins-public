package com.playmonumenta.plugins.abilities.rogue.swordsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import javax.annotation.Nullable;


public class BladeDance extends Ability {

	private static final int DANCE_1_DAMAGE = 10;
	private static final int DANCE_2_DAMAGE = 18;
	private static final double SLOWNESS_AMPLIFIER = 0.4;
	private static final double WEAKEN_AMP_1 = 0.5;
	private static final double WEAKEN_AMP_2 = 0.7;
	private static final int DURATION = 20 * 2;
	private static final int DANCE_RADIUS = 4;
	private static final float DANCE_KNOCKBACK_SPEED = 0.2f;
	private static final int COOLDOWN = 16 * 20;
	private static final Particle.DustOptions SWORDSAGE_COLOR = new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.0f);

	private final double mWeakenAmp;

	public BladeDance(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Blade Dance");
		mInfo.mScoreboardId = "BladeDance";
		mInfo.mShorthandName = "BD";
		mInfo.mDescriptions.add("When holding two swords, right-click while looking down to enter a defensive stance, parrying all attacks and becoming invulnerable for 0.75 seconds. Afterwards, unleash a powerful attack that deals 10 melee damage to and afflicts 40% Slowness and 50% Weaken to all enemies in a 4 block radius for 2 seconds. Cooldown: 16s.");
		mInfo.mDescriptions.add("The area attack now deals 18 damage and afflicts 70% Weaken.");
		mInfo.mLinkedSpell = ClassAbility.BLADE_DANCE;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mDisplayItem = new ItemStack(Material.STRING, 1);
		mWeakenAmp = getAbilityScore() == 1 ? WEAKEN_AMP_1 : WEAKEN_AMP_2;
	}

	@Override
	public void cast(Action action) {
		if (mPlayer == null) {
			return;
		}

		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ITEM_TRIDENT_RETURN, 1f, 0.75f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 0.5f);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
		world.spawnParticle(Particle.VILLAGER_ANGRY, mPlayer.getLocation().clone().add(0, 1, 0), 6, 0.45, 0.5, 0.45, 0);
		world.spawnParticle(Particle.CLOUD, mPlayer.getLocation().clone().add(0, 1, 0), 20, 0.25, 0.5, 0.25, 0.15);
		world.spawnParticle(Particle.REDSTONE, mPlayer.getLocation().clone().add(0, 1, 0), 6, 0.45, 0.5, 0.45, 0, SWORDSAGE_COLOR);
		mPlayer.setInvulnerable(true);
		new BukkitRunnable() {
			int mTicks = 0;
			float mPitch = 0.5f;

			@Override
			public void run() {
				mTicks += 1;
				Location loc = mPlayer.getLocation();
				double r = DANCE_RADIUS - (3 * mPitch);
				world.spawnParticle(Particle.SWEEP_ATTACK, loc, 3, r, 2, r, 0);
				world.spawnParticle(Particle.REDSTONE, loc, 4, r, 2, r, 0, SWORDSAGE_COLOR);
				world.spawnParticle(Particle.CLOUD, loc, 4, r, 2, r, 0);
				if (mTicks % 2 == 0) {
					world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, mPitch);
					mPitch += 0.1f;
				}

				if (mTicks >= 15) {
					mPlayer.setInvulnerable(false);
					world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1, 1);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 2f);
					world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 0.75f);

					world.spawnParticle(Particle.VILLAGER_ANGRY, mPlayer.getLocation().clone().add(0, 1, 0), 6, 0.45, 0.5, 0.45, 0);

					int damage = getAbilityScore() == 1 ? DANCE_1_DAMAGE : DANCE_2_DAMAGE;

					for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), DANCE_RADIUS)) {
						mob.setNoDamageTicks(0);
						DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, damage, mInfo.mLinkedSpell);
						MovementUtils.knockAway(mPlayer, mob, DANCE_KNOCKBACK_SPEED, true);

						EntityUtils.applySlow(mPlugin, DURATION, SLOWNESS_AMPLIFIER, mob);
						EntityUtils.applyWeaken(mPlugin, DURATION, mWeakenAmp, mob);

						Location mobLoc = mob.getLocation().add(0, 1, 0);
						world.spawnParticle(Particle.SWEEP_ATTACK, mobLoc, 5, 0.35, 0.5, 0.35, 0);
						world.spawnParticle(Particle.CRIT, mobLoc, 10, 0.25, 0.5, 0.25, 0.3);
						world.spawnParticle(Particle.REDSTONE, mobLoc, 15, 0.35, 0.5, 0.35, 0, SWORDSAGE_COLOR);
					}

					new BukkitRunnable() {
						int mTicks = 0;
						double mRadians = 0;

						@Override
						public void run() {
							Vector vec = new Vector(FastUtils.cos(mRadians) * DANCE_RADIUS / 1.5, 0, FastUtils.sin(mRadians) * DANCE_RADIUS / 1.5);

							Location loc2 = mPlayer.getEyeLocation().add(vec);
							world.spawnParticle(Particle.SWEEP_ATTACK, loc2, 5, 1, 0.25, 1, 0);
							world.spawnParticle(Particle.CRIT, loc2, 10, 1, 0.25, 1, 0.3);
							world.spawnParticle(Particle.REDSTONE, loc2, 10, 1, 0.25, 1, 0, SWORDSAGE_COLOR);
							world.playSound(loc2, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1.5f);

							if (mTicks >= 5) {
								this.cancel();
							}

							mTicks++;
							mRadians += Math.toRadians(72);
						}
					}.runTaskTimer(mPlugin, 0, 1);

					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		return mPlayer != null && !mPlayer.isSneaking() && mPlayer.getLocation().getPitch() >= 50 && InventoryUtils.rogueTriggerCheck(mPlugin, mPlayer);
	}
}
