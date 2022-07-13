package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.WindBombAirTag;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
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
	private static final int DAMAGE_1 = 6;
	private static final int DAMAGE_2 = 8;
	private static final double MIDAIR_DAMAGE_BONUS = 0.2;
	private static final int RADIUS = 3;
	private static final double VELOCITY = 1.5;

	private final double mDamage;

	private @Nullable Snowball mProj = null;

	public WindBomb(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Wind Bomb");
		mInfo.mScoreboardId = "WindBomb";
		mInfo.mShorthandName = "WB";
		mInfo.mLinkedSpell = ClassAbility.WIND_BOMB;
		mInfo.mDescriptions.add("Pressing the swap key while sneaking throws a projectile that, upon contact with the ground or an enemy, deals 6 projectile damage to mobs in a a 3 block radius and launches them into the air, giving them Slow Falling and 20% Weaken for 4s. Cooldown: 15s.");
		mInfo.mDescriptions.add("The damage is increased to 8 and the cooldown is reduced to 10s. Additionally, you deal 20% more damage to enemies made airborne by this skill, until they hit the ground.");
		mInfo.mCooldown = getAbilityScore() == 1 ? COOLDOWN_1 : COOLDOWN_2;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.TNT, 1);
		mDamage = getAbilityScore() == 1 ? DAMAGE_1 : DAMAGE_2;
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		event.setCancelled(true);

		if (mPlayer != null && mPlayer.isSneaking()) {
			if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
				return;
			}
			World world = mPlayer.getWorld();
			Location loc = mPlayer.getLocation();
			world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, 1.0f, 0.25f);
			mProj = world.spawn(mPlayer.getEyeLocation(), Snowball.class);
			mProj.setVelocity(loc.getDirection().normalize().multiply(VELOCITY));
			mProj.setShooter(mPlayer);
			mPlugin.mProjectileEffectTimers.addEntity(mProj, Particle.CLOUD);
			putOnCooldown();
		}
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		if (mPlayer != null && mProj != null && mProj == proj) {
			mProj = null;
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
				DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.PROJECTILE_SKILL, mDamage, mInfo.mLinkedSpell, true, false);
				if (!EntityUtils.isBoss(mob)) {
					mob.setVelocity(new Vector(0.f, 1.2f, 0.f));
					PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW_FALLING, DURATION, SLOW_FALL_EFFECT, true, false));
				}
				EntityUtils.applyWeaken(mPlugin, DURATION, WEAKEN_EFFECT, mob);
				if (getAbilityScore() > 1) {
					mPlugin.mEffectManager.addEffect(mob, "WindBombAirTag", new WindBombAirTag(DURATION * 2, MIDAIR_DAMAGE_BONUS, mPlayer));
				}
			}

			proj.remove();
		}
	}
}
