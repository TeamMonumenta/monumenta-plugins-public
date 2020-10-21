package com.playmonumenta.plugins.abilities.alchemist;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class AlchemicalArtillery extends Ability {
	private static final String ALCHEMICAL_ARTILLERY_METAKEY = "AlchemicalArtilleryArrowGotTheDankPot";
	private static final int ALCHEMICAL_ARTILLERY_1_RADIUS = 3;
	private static final int ALCHEMICAL_ARTILLERY_2_RADIUS = 4;
	private static final int ALCHEMICAL_ARTILLERY_1_COST = 4;
	private static final int ALCHEMICAL_ARTILLERY_2_COST = 3;
	private static final int ALCHEMICAL_ARTILLERY_ACTIVITY_PERIOD = 20 * 5;

	private final int mRadius;
	private final int mCost;
	private boolean mActive = false;

	public AlchemicalArtillery(Plugin plugin, Player player) {
		super(plugin, player, "Alchemical Artillery");
		mInfo.mLinkedSpell = Spells.ALCHEMICAL_ARTILLERY;
		mInfo.mScoreboardId = "Artillery";
		mInfo.mShorthandName = "AAr";
		mInfo.mDescriptions.add("Left click with a bow without shifting to prime your next arrow shot within 5 seconds. Shooting a primed arrow consumes 4 Alchemist Potions and applies all potion abilities and Basilisk Poison in a 3 block radius of where the arrow lands.");
		mInfo.mDescriptions.add("The cost is reduced to 3 potions and the radius is increased to 4 blocks.");
		mInfo.mCooldown = 0;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mRadius = getAbilityScore() == 1 ? ALCHEMICAL_ARTILLERY_1_RADIUS : ALCHEMICAL_ARTILLERY_2_RADIUS;
		mCost = getAbilityScore() == 1 ? ALCHEMICAL_ARTILLERY_1_COST : ALCHEMICAL_ARTILLERY_2_COST;
	}

	@Override
	public void cast(Action action) {
		if (!mActive) {
			if (!mPlayer.isSneaking() && InventoryUtils.isBowItem(mPlayer.getInventory().getItemInMainHand())) {
				mActive = true;
				World world = mPlayer.getWorld();
				world.playSound(mPlayer.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_OPEN, 1, 2.5f);
				new BukkitRunnable() {
					int mTicks = 0;
					@Override
					public void run() {
						mTicks++;
						world.spawnParticle(Particle.SMOKE_LARGE, mPlayer.getLocation(), 1, 0.25, 0, 0.25, 0);
						if (mTicks == 3) {
							world.playSound(mPlayer.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_OPEN, 1, 2.5f);
						}
						if (!mActive || mTicks > ALCHEMICAL_ARTILLERY_ACTIVITY_PERIOD) {
							mActive = false;
							this.cancel();
						}
					}
				}.runTaskTimer(mPlugin, 0, 1);
			}
		}
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		if (proj instanceof Arrow || proj instanceof SpectralArrow) {
			AbstractArrow arrow = (AbstractArrow) proj;

			if (arrow.hasMetadata(ALCHEMICAL_ARTILLERY_METAKEY)) {
				// Must remove metadata or the arrow could bounce and go boom x2
				arrow.removeMetadata(ALCHEMICAL_ARTILLERY_METAKEY, mPlugin);

				// Delayed run to get real location of arrow
				new BukkitRunnable() {
					@Override
					public void run() {
						Location loc = EntityUtils.getProjectileHitLocation(event);

						World world = mPlayer.getWorld();
						world.spawnParticle(Particle.SPELL_MOB, loc, 15 * (int) Math.pow(mRadius, 2), mRadius, 0.5, mRadius, 0);
						world.spawnParticle(Particle.FLAME, loc, 3 * (int) Math.pow(mRadius, 2), 0, 0, 0, 0.06 * mRadius);
						world.spawnParticle(Particle.SMOKE_LARGE, loc, 5 * (int) Math.pow(mRadius, 2), 0, 0, 0, 0.08 * mRadius);
						world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1);
						world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.5f, 1);

						AlchemistPotions ap = AbilityManager.getManager().getPlayerAbility(mPlayer, AlchemistPotions.class);
						BasiliskPoison bp = AbilityManager.getManager().getPlayerAbility(mPlayer, BasiliskPoison.class);

						List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, mRadius);

						if (ap != null) {
							ap.createAura(loc, mRadius);

							for (LivingEntity mob : mobs) {
								ap.apply(mob);
							}
						}

						if (bp != null) {
							for (LivingEntity mob : mobs) {
								bp.apply(mob);
							}
						}
					}
				}.runTaskLater(mPlugin, 1);
			}
		}
	}

	@Override
	public boolean playerShotArrowEvent(AbstractArrow arrow) {
		if (mActive) {
			if (AbilityUtils.removeAlchemistPotions(mPlayer, mCost)) {
				mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.FIREWORKS_SPARK);
				arrow.setMetadata(ALCHEMICAL_ARTILLERY_METAKEY, new FixedMetadataValue(mPlugin, mRadius));
				mActive = false;
			}
		}

		return true;
	}

}
