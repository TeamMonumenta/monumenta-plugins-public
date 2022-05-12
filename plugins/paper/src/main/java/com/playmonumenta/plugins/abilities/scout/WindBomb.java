package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.bosses.bosses.CrowdControlImmunityBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.WindBombAirTag;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class WindBomb extends Ability {

	private static final int DURATION = 20 * 4;
	private static final double WEAKEN_EFFECT = 0.2;
	private static final int SLOW_FALL_EFFECT = 0;
	private static final int COOLDOWN_1 = 20 * 15;
	private static final int COOLDOWN_2 = 20 * 10;
	private static final double MIDAIR_DAMAGE_BONUS = 0.2;
	private static final int RADIUS = 3;
	private static final double VELOCITY = 1.5;
	private static final String AIR_TAG = "WindBombAirTag";

	private static final int PULL_INTERVAL = 10;
	private static final double PULL_VELOCITY = 0.35;
	private static final double PULL_RADIUS = 16;
	private static final int PULL_DURATION = 3 * 20;
	private static final double PULL_RATIO = 0.12;

	private @Nullable Snowball mProj = null;

	public WindBomb(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Wind Bomb");
		mInfo.mScoreboardId = "WindBomb";
		mInfo.mShorthandName = "WB";
		mInfo.mLinkedSpell = ClassAbility.WIND_BOMB;
		mInfo.mDescriptions.add("Pressing the swap key while sneaking throws a projectile that, upon contact with the ground or an enemy, launches mobs in a 3 block radius into the air, giving them Slow Falling and 20% Weaken for 4s. Cannot be used while holding a trident or snowball. Cooldown: 15s.");
		mInfo.mDescriptions.add("The cooldown is reduced to 10s. Additionally, you deal 20% more damage to enemies made airborne by this skill, until they hit the ground.");
		mInfo.mDescriptions.add("On impact, generate a vortex that pulls mobs within 16 blocks toward the center for 3 seconds.");
		mInfo.mCooldown = isLevelOne() ? COOLDOWN_1 : COOLDOWN_2;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.TNT, 1);
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		event.setCancelled(true);

		ItemStack mainhand = mPlayer.getInventory().getItemInMainHand();
		if (mainhand != null && (mainhand.getType() == Material.TRIDENT || mainhand.getType() == Material.SNOWBALL)) {
			return;
		}

		if (mPlayer.isSneaking()) {
			if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
				return;
			}
			World world = mPlayer.getWorld();
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_HORSE_BREATHE, 1.0f, 0.25f);
			mProj = mPlayer.launchProjectile(Snowball.class);
			mProj.setVelocity(mProj.getVelocity().normalize().multiply(VELOCITY));
			mPlugin.mProjectileEffectTimers.addEntity(mProj, Particle.CLOUD);
			DamageListener.removeProjectileItemStats(mProj);
			putOnCooldown();
		}
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		if (mPlayer != null && this.mProj != null && this.mProj == proj) {
			this.mProj = null;
			Location loc = proj.getLocation();
			World world = proj.getWorld();

			world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.25f);
			new BukkitRunnable() {
				final double mRadius = RADIUS;

				@Override
				public void run() {
					for (double j = 0; j < 360; j += 6) {
						double radian1 = Math.toRadians(j);
						loc.add(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
						new PartialParticle(Particle.CLOUD, loc, 3, 0, 0, 0, 0.125).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
						loc.subtract(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
					}
					this.cancel();
				}
			}.runTaskTimer(mPlugin, 0, 1);

			for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, RADIUS, mPlayer)) {
				if (!EntityUtils.isBoss(mob)) {
					mob.setVelocity(new Vector(0.f, 1.2f, 0.f));
					PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW_FALLING, DURATION, SLOW_FALL_EFFECT, true, false));
				}
				EntityUtils.applyWeaken(mPlugin, DURATION, WEAKEN_EFFECT, mob);
				if (isLevelTwo()) {
					mPlugin.mEffectManager.addEffect(mob, AIR_TAG, new WindBombAirTag(DURATION * 2, MIDAIR_DAMAGE_BONUS, mPlayer));
				}
			}

			if (isEnhanced()) {
				loc.add(0, 2, 0);
				world.spawnParticle(Particle.CLOUD, loc, 35, 4, 4, 4, 0.125);
				world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 25, 2, 2, 2, 0.125);
				world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1f);
				world.playSound(loc, Sound.ITEM_ELYTRA_FLYING, 0.8f, 1);

				new BukkitRunnable() {
					int mTicks = 0;

					@Override
					public void run() {
						mTicks++;
						if (mTicks % PULL_INTERVAL == 0) {
							for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, PULL_RADIUS)) {
								if (!(EntityUtils.isBoss(mob) || mob.getScoreboardTags().contains(CrowdControlImmunityBoss.identityTag))) {
									Vector vector = mob.getLocation().toVector().subtract(loc.toVector());
									double ratio = PULL_RATIO + vector.length() / PULL_RADIUS;
									Vector velocity = mob.getVelocity().add(vector.normalize().multiply(PULL_VELOCITY).multiply(-ratio).add(new Vector(0, 0.1 + 0.2 * ratio, 0)));
									if (mPlugin.mEffectManager.hasEffect(mob, AIR_TAG)) {
										// If mob was launched by the ability, don't change their Y
										velocity.setY(mob.getVelocity().getY());
									}
									mob.setVelocity(velocity);
								}
							}
						}
						world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 6, 2, 2, 2, 0.1);
						world.spawnParticle(Particle.CLOUD, loc, 4, 2, 2, 2, 0.05);
						world.spawnParticle(Particle.CLOUD, loc, 3, 0.1, 0.1, 0.1, 0.15);
						if (mTicks >= PULL_DURATION) {
							this.cancel();
						}
					}
				}.runTaskTimer(mPlugin, 0, 1);
			}

			proj.remove();
		}
	}
}
