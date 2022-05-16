package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
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
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;


public class ThunderStep extends Ability {

	public static final String NAME = "Thunder Step";
	public static final ClassAbility ABILITY = ClassAbility.THUNDER_STEP;
	public static final Particle.DustOptions COLOR_YELLOW = new Particle.DustOptions(Color.YELLOW, 0.75f);
	public static final Particle.DustOptions COLOR_AQUA = new Particle.DustOptions(Color.AQUA, 0.75f);

	/*
	 * Cloud's standardised constant order:
	 *
	 * Damage/additional damage/bonus damage/healing,
	 * size/distance,
	 * amplifiers/multipliers,
	 * durations,
	 * other skill technicalities eg knockback,
	 * cooldowns
	 */
	public static final int DAMAGE_1 = 5;
	public static final int DAMAGE_2 = 8;
	public static final int SIZE = 4;
	public static final int DISTANCE_1 = 8;
	public static final int DISTANCE_2 = 10;
	public static final double CHECK_INCREMENT = 0.1;
	public static final double STUN_SECONDS = 0.5;
	public static final int STUN_TICKS = (int) (STUN_SECONDS * 20);
	public static final int COOLDOWN_SECONDS = 22;
	public static final int COOLDOWN_TICKS = COOLDOWN_SECONDS * 20;

	public static final double BACK_TELEPORT_MAX_DISTANCE = 64;
	public static final int BACK_TELEPORT_MAX_DELAY = 3 * 20;
	public static final int ENHANCEMENT_BONUS_DAMAGE_TIMER = 30 * 20;
	public static final int ENHANCEMENT_PARALYZE_DURATION = 5 * 20;

	private final int mLevelDamage;
	private final int mLevelDistance;
	private final boolean mDoStun;

	private int mLastCastTick = -1;
	private @Nullable Location mLastCastLocation = null;
	private boolean mCanParalyze = false;

	public ThunderStep(Plugin plugin, @Nullable Player player) {
		super(plugin, player, NAME);
		mInfo.mLinkedSpell = ABILITY;

		mInfo.mScoreboardId = "ThunderStep";
		mInfo.mShorthandName = "TS";
		mInfo.mDescriptions.add(
			String.format(
				"While holding a wand while sneaking, pressing the swap key materializes a flash of thunder," +
					" dealing %s magic damage to all enemies in a %s-block cube around you and knocking them away." +
					" The next moment, you teleport towards where you're looking, travelling up to %s blocks or until you hit a solid block," +
					" and repeat the thunder attack at your destination, ignoring iframes. Cooldown: %ss.",
				DAMAGE_1,
				SIZE,
				DISTANCE_1,
				COOLDOWN_SECONDS
			)
		);
		mInfo.mDescriptions.add(
			String.format(
				"The thunder attacks now also stun all non-boss enemies for %ss. Damage is increased from %s to %s. Teleport range is increased from %s to %s blocks.",
				STUN_SECONDS,
				DAMAGE_1,
				DAMAGE_2,
				DISTANCE_1,
				DISTANCE_2
			)
		);
		mInfo.mDescriptions.add(
			String.format("You are now able to recast this skill after the original cast within %ss." +
				              " If you do, you get teleported back to the original starting location, stunning but not damaging nearby mobs." +
				              " If you instead choose to not recast the skill, your next Thunder Step within %ss will paralyze enemies for %ss.",
				BACK_TELEPORT_MAX_DELAY / 20,
				ENHANCEMENT_BONUS_DAMAGE_TIMER / 20,
				ENHANCEMENT_PARALYZE_DURATION / 20
			)
		);
		mInfo.mCooldown = COOLDOWN_TICKS;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.HORN_CORAL, 1);

		mLevelDamage = isLevelOne() ? DAMAGE_1 : DAMAGE_2;
		mLevelDistance = isLevelOne() ? DISTANCE_1 : DISTANCE_2;
		mDoStun = isLevelTwo();
	}

	/* NOTE
	 * We want to cancel every swap key while holding wand,
	 * if the player has a skill that uses swap key as its trigger
	 * to avoid annoyingly unintentionally swapping hands if the skill is on cooldown, instead of casting.
	 * This means we have to reach this method every time,
	 * so runCheck() is not overridden and defaults to true,
	 * and we also mIgnoreCooldown above.
	 * We run the actual cast condition and cooldown checks within this method,
	 * and can always decide whether to cancel the event
	 */
	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		if (mPlayer != null && ItemUtils.isWand(mPlayer.getInventory().getItemInMainHand())) {
			event.setCancelled(true);

			if (mPlayer.isSneaking()
				    && !ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)) {

				// if enhanced, can teleport back within a short time frame (regardless of if on cooldown or not)
				if (isEnhanced()
					    && mPlayer.getTicksLived() <= mLastCastTick + BACK_TELEPORT_MAX_DELAY
					    && mLastCastLocation != null
					    && mLastCastLocation.getWorld() == mPlayer.getWorld()
					    && mLastCastLocation.distance(mPlayer.getLocation()) < BACK_TELEPORT_MAX_DISTANCE) {

					doDamage(mPlayer.getLocation(), 0, false);
					mLastCastLocation.setDirection(mPlayer.getLocation().getDirection());
					mPlayer.teleport(mLastCastLocation, PlayerTeleportEvent.TeleportCause.UNKNOWN);
					doDamage(mLastCastLocation, 0, false);

					// prevent further back teleports as well as paralyze of any further casts
					mLastCastLocation = null;
					mLastCastTick = -1;
					mCanParalyze = false;
					return;
				}

				// on cooldown and didn't teleport back: stop here
				if (isTimerActive()) {
					return;
				}

				boolean doParalyze = isEnhanced() && mCanParalyze && mPlayer.getTicksLived() <= mLastCastTick + ENHANCEMENT_BONUS_DAMAGE_TIMER;
				mCanParalyze = !doParalyze;

				putOnCooldown();
				mLastCastLocation = mPlayer.getLocation();
				mLastCastTick = mPlayer.getTicksLived();

				float spellDamage = SpellPower.getSpellDamage(mPlugin, mPlayer, mLevelDamage);

				Location playerStartLocation = mPlayer.getLocation();
				doDamage(playerStartLocation, spellDamage, doParalyze);

				World world = mPlayer.getWorld();
				BoundingBox movingPlayerBox = mPlayer.getBoundingBox();
				Vector vector = playerStartLocation.getDirection();
				LocationUtils.travelTillObstructed(
					world,
					movingPlayerBox,
					mLevelDistance,
					vector,
					CHECK_INCREMENT,
					true,
					null, -1, -1
				);
				Location playerEndLocation = movingPlayerBox
					.getCenter()
					.setY(movingPlayerBox.getMinY())
					.toLocation(world)
					.setDirection(vector);

				if (!playerEndLocation.getWorld().getWorldBorder().isInside(playerEndLocation)
					    || ZoneUtils.hasZoneProperty(playerEndLocation, ZoneProperty.NO_MOBILITY_ABILITIES)) {
					return;
				}

				mPlayer.teleport(playerEndLocation, PlayerTeleportEvent.TeleportCause.UNKNOWN);
				doDamage(playerEndLocation, spellDamage, doParalyze);
			}
		}
	}

	private void doDamage(Location location, float spellDamage, boolean enhancementParalyze) {
		World world = location.getWorld();
		world.playSound(location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1f, 1.5f);
		new PartialParticle(Particle.REDSTONE, location, 100, 2.5, 2.5, 2.5, 3, COLOR_YELLOW).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, location, 100, 2.5, 2.5, 2.5, 3, COLOR_AQUA).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FLASH, location.clone().add(location.getDirection()), 1, 0, 0, 0, 10).spawnAsPlayerActive(mPlayer);

		List<LivingEntity> enemies = EntityUtils.getNearbyMobs(location, SIZE);
		// The more enemies, the fewer particles for each one
		int mobParticles = Math.max(
			1,
			20 / Math.max(1, enemies.size()) // Never divide by 0. Always maximum 20 particles for <= 1 enemy
		);

		for (LivingEntity enemy : enemies) {
			if (spellDamage > 0) {
				DamageUtils.damage(mPlayer, enemy, DamageType.MAGIC, spellDamage, ABILITY, true);
			}

			if (mDoStun && !EntityUtils.isBoss(enemy)) {
				EntityUtils.applyStun(mPlugin, STUN_TICKS, enemy);
			}
			if (enhancementParalyze && !EntityUtils.isBoss(enemy)) {
				EntityUtils.paralyze(mPlugin, ENHANCEMENT_PARALYZE_DURATION, enemy);
			}

			Location enemyParticleLocation = enemy.getLocation().add(0, enemy.getHeight() / 2, 0);
			new PartialParticle(Particle.CLOUD, enemyParticleLocation, mobParticles, 0.5, 0.5, 0.5, 0.5).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.END_ROD, enemyParticleLocation, mobParticles, 0.5, 0.5, 0.5, 0.5).spawnAsPlayerActive(mPlayer);
		}
	}

	@Override
	public void playerTeleportEvent(PlayerTeleportEvent event) {
		// Might trigger too often
		mLastCastLocation = null;
	}

}
