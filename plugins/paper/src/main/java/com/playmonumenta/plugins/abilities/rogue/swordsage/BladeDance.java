package com.playmonumenta.plugins.abilities.rogue.swordsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import javax.annotation.Nullable;
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


public class BladeDance extends Ability {

	private static final int DANCE_1_DAMAGE = 4;
	private static final int DANCE_2_DAMAGE = 7;
	private static final double SLOWNESS_AMPLIFIER = 1;
	private static final int SLOW_DURATION_1 = 2 * 20;
	private static final int SLOW_DURATION_2 = (int) (2.5 * 20);
	private static final int DANCE_RADIUS = 4;
	private static final float DANCE_KNOCKBACK_SPEED = 0.2f;
	private static final int COOLDOWN_1 = 18 * 20;
	private static final int COOLDOWN_2 = 16 * 20;
	private static final Particle.DustOptions SWORDSAGE_COLOR = new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.0f);

	private final int mDamage;
	private final int mSlowDuration;

	public BladeDance(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Blade Dance");
		mInfo.mScoreboardId = "BladeDance";
		mInfo.mShorthandName = "BD";
		mInfo.mDescriptions.add("When holding two swords, right-click while looking down to enter a defensive stance, parrying all attacks and becoming invulnerable for 0.75 seconds. Afterwards, unleash a powerful attack that deals 4 melee damage to enemies in a 4 block radius. Damaged enemies are rooted for 2 seconds. Cooldown: 18s.");
		mInfo.mDescriptions.add("The area attack now deals 7 damage and roots for 2.5. Cooldown: 16s.");
		mInfo.mLinkedSpell = ClassAbility.BLADE_DANCE;
		mInfo.mCooldown = isLevelOne() ? COOLDOWN_1 : COOLDOWN_2;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mDisplayItem = new ItemStack(Material.STRING, 1);
		mDamage = isLevelOne() ? DANCE_1_DAMAGE : DANCE_2_DAMAGE;
		mSlowDuration = isLevelOne() ? SLOW_DURATION_1 : SLOW_DURATION_2;
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
		new PartialParticle(Particle.VILLAGER_ANGRY, mPlayer.getLocation().clone().add(0, 1, 0), 6, 0.45, 0.5, 0.45, 0).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, mPlayer.getLocation().clone().add(0, 1, 0), 20, 0.25, 0.5, 0.25, 0.15).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, mPlayer.getLocation().clone().add(0, 1, 0), 6, 0.45, 0.5, 0.45, 0, SWORDSAGE_COLOR).spawnAsPlayerActive(mPlayer);
		mPlayer.setInvulnerable(true);
		new BukkitRunnable() {
			int mTicks = 0;
			float mPitch = 0.5f;

			@Override
			public void run() {
				mTicks += 1;
				Location loc = mPlayer.getLocation();
				double r = DANCE_RADIUS - (3 * mPitch);
				new PartialParticle(Particle.SWEEP_ATTACK, loc, 3, r, 2, r, 0).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.REDSTONE, loc, 4, r, 2, r, 0, SWORDSAGE_COLOR).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.CLOUD, loc, 4, r, 2, r, 0).spawnAsPlayerActive(mPlayer);
				if (mTicks % 2 == 0) {
					world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, mPitch);
					mPitch += 0.1f;
				}

				if (mTicks >= 15) {
					mPlayer.setInvulnerable(false);
					world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1, 1);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 2f);
					world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 0.75f);

					new PartialParticle(Particle.VILLAGER_ANGRY, mPlayer.getLocation().clone().add(0, 1, 0), 6, 0.45, 0.5, 0.45, 0).spawnAsPlayerActive(mPlayer);

					for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), DANCE_RADIUS)) {
						DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, mDamage, mInfo.mLinkedSpell, true);
						MovementUtils.knockAway(mPlayer, mob, DANCE_KNOCKBACK_SPEED, true);

						if (!EntityUtils.isBoss(mob)) {
							EntityUtils.applySlow(mPlugin, mSlowDuration, SLOWNESS_AMPLIFIER, mob);
						}

						Location mobLoc = mob.getLocation().add(0, 1, 0);
						new PartialParticle(Particle.SWEEP_ATTACK, mobLoc, 5, 0.35, 0.5, 0.35, 0).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.CRIT, mobLoc, 10, 0.25, 0.5, 0.25, 0.3).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.REDSTONE, mobLoc, 15, 0.35, 0.5, 0.35, 0, SWORDSAGE_COLOR).spawnAsPlayerActive(mPlayer);
					}

					new BukkitRunnable() {
						int mTicks = 0;
						double mRadians = 0;

						@Override
						public void run() {
							Vector vec = new Vector(FastUtils.cos(mRadians) * DANCE_RADIUS / 1.5, 0, FastUtils.sin(mRadians) * DANCE_RADIUS / 1.5);

							Location loc2 = mPlayer.getEyeLocation().add(vec);
							new PartialParticle(Particle.SWEEP_ATTACK, loc2, 5, 1, 0.25, 1, 0).spawnAsPlayerActive(mPlayer);
							new PartialParticle(Particle.CRIT, loc2, 10, 1, 0.25, 1, 0.3).spawnAsPlayerActive(mPlayer);
							new PartialParticle(Particle.REDSTONE, loc2, 10, 1, 0.25, 1, 0, SWORDSAGE_COLOR).spawnAsPlayerActive(mPlayer);
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
