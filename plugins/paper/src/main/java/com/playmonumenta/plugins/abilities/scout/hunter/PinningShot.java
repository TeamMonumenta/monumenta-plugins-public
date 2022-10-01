package com.playmonumenta.plugins.abilities.scout.hunter;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class PinningShot extends Ability {

	private static final double PINNING_SHOT_1_DAMAGE_MULTIPLIER = 0.1;
	private static final double PINNING_SHOT_2_DAMAGE_MULTIPLIER = 0.2;
	private static final int PINNING_SHOT_DURATION = (int) (20 * 2.5);
	private static final double PINNING_SLOW = 1.0;
	private static final double PINNING_SLOW_BOSS = 0.3;
	private static final double PINNING_WEAKEN_1 = 0.3;
	private static final double PINNING_WEAKEN_2 = 0.6;

	public static final String CHARM_DAMAGE = "Pinning Shot Max Health Damage";
	public static final String CHARM_WEAKEN = "Pinning Shot Weakness Amplifier";

	private final double mDamageMultiplier;
	private final double mWeaken;

	private final Map<LivingEntity, Boolean> mPinnedMobs = new HashMap<LivingEntity, Boolean>();

	public PinningShot(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Pinning Shot");
		mInfo.mScoreboardId = "PinningShot";
		mInfo.mShorthandName = "PSh";
		mInfo.mLinkedSpell = ClassAbility.PINNING_SHOT;
		mInfo.mDescriptions.add(String.format("The first time you shoot a non-boss enemy, pin it for %ss. Pinned enemies are afflicted with %d%% Slowness and %d%% Weaken (Bosses receive %d%% Slowness and no Weaken). Shooting a pinned non-boss enemy deals %d%% of its max health on top of regular damage and removes the pin. A mob cannot be pinned more than once.",
			PINNING_SHOT_DURATION / 20.0, (int)(PINNING_SLOW * 100), (int)(PINNING_WEAKEN_1 * 100), (int)(PINNING_SLOW_BOSS * 100), (int)(PINNING_SHOT_1_DAMAGE_MULTIPLIER * 100)));
		mInfo.mDescriptions.add(String.format("Weaken increased to %d%% and bonus damage increased to %d%% max health.", (int)(PINNING_WEAKEN_2 * 100), (int)(PINNING_SHOT_2_DAMAGE_MULTIPLIER * 100)));
		mDisplayItem = new ItemStack(Material.CROSSBOW, 1);

		mDamageMultiplier = (isLevelOne() ? PINNING_SHOT_1_DAMAGE_MULTIPLIER : PINNING_SHOT_2_DAMAGE_MULTIPLIER) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
		mWeaken = (isLevelOne() ? PINNING_WEAKEN_1 : PINNING_WEAKEN_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_WEAKEN);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() != DamageType.PROJECTILE || !(event.getDamager() instanceof Projectile proj) || !EntityUtils.isAbilityTriggeringProjectile(proj, true)) {
			return false;
		}

		World world = mPlayer.getWorld();
		if (mPinnedMobs.containsKey(enemy)) { // pinned once already
			if (mPinnedMobs.get(enemy)) { // currently pinned
				mPinnedMobs.put(enemy, false);
				Location loc = enemy.getEyeLocation();
				world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 0.5f);
				new PartialParticle(Particle.FIREWORKS_SPARK, loc, 20, 0, 0, 0, 0.2).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.SNOWBALL, loc, 30, 0, 0, 0, 0.25).spawnAsPlayerActive(mPlayer);
				EntityUtils.setSlowTicks(mPlugin, enemy, 1);
				EntityUtils.setWeakenTicks(mPlugin, enemy, 1);
				if (!EntityUtils.isBoss(enemy)) {
					DamageUtils.damage(mPlayer, enemy, DamageType.OTHER, EntityUtils.getMaxHealth(enemy) * mDamageMultiplier, mInfo.mLinkedSpell, true, false);
				}
			}
		} else {
			Location loc = enemy.getLocation();
			world.playSound(loc, Sound.BLOCK_SLIME_BLOCK_PLACE, 1, 0.5f);
			new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 8, 0, 0, 0, 0.2).spawnAsPlayerActive(mPlayer);
			if (EntityUtils.isBoss(enemy)) {
				EntityUtils.applySlow(mPlugin, PINNING_SHOT_DURATION, PINNING_SLOW_BOSS, enemy);
			} else {
				EntityUtils.applySlow(mPlugin, PINNING_SHOT_DURATION, PINNING_SLOW, enemy);
				EntityUtils.applyWeaken(mPlugin, PINNING_SHOT_DURATION, mWeaken, enemy);
			}
			mPinnedMobs.put(enemy, true);
			new BukkitRunnable() {
				@Override
				public void run() {
					mPinnedMobs.put(enemy, false);
				}
			}.runTaskLater(mPlugin, PINNING_SHOT_DURATION);
		}
		return false; // prevents multiple applications itself
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond) {
			mPinnedMobs.entrySet().removeIf((entry) -> !entry.getKey().isValid() || entry.getKey().isDead());
		}
	}

}
