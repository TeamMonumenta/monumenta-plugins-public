package com.playmonumenta.plugins.abilities.mage;

import java.util.List;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.enchantments.SpellDamage;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;


public class ThunderStep extends Ability {
	public static final String NAME = "Thunder Step";
	public static final Spells SPELL = Spells.THUNDER_STEP;
	public static final Particle.DustOptions COLOR_YELLOW = new Particle.DustOptions(Color.YELLOW, 0.75f);
	public static final Particle.DustOptions COLOR_AQUA = new Particle.DustOptions(Color.AQUA, 0.75f);

	/*
	 * Cloud's standardised constant order:
	 * damage/extra damage/bonus damage, size/distance, amplifiers/multipliers, durations, other skill technicalities eg knockback, cooldowns
	 *
	 * For pairs of values where one is used to calculate the other, like seconds or hearts, the resulting value goes second
	 */
	public static final int DAMAGE_1 = 5;
	public static final int DAMAGE_2 = 8;
	public static final int SIZE = 4;
	public static final int DISTANCE_1 = 8;
	public static final int DISTANCE_2 = 10;
	public static final double CHECK_INCREMENT = 0.1;
	public static final int STUN_SECONDS = 1;
	public static final int STUN_TICKS = STUN_SECONDS * 20;
	public static final int COOLDOWN_SECONDS = 22;
	public static final int COOLDOWN_TICKS = COOLDOWN_SECONDS * 20;

	private final int mLevelDamage;
	private final int mLevelDistance;
	private final boolean mDoStun;

	public ThunderStep(Plugin plugin, Player player) {
		super(plugin, player, NAME);
		mInfo.mLinkedSpell = SPELL;

		mInfo.mScoreboardId = "ThunderStep";
		mInfo.mShorthandName = "TS";
		mInfo.mDescriptions.add(
			String.format(
				"While holding a wand and sneaking, pressing the swap key materializes a flash of thunder, dealing %s damage to all enemies in a %s-block cube around you and knocking them away. The next moment, you teleport towards where you're looking, travelling up to %s blocks or until you hit a solid block and repeating the thunder attack at your destination. This second attack ignores iframes. Swapping hands while holding a wand no longer does its vanilla function. Cooldown: %ss.",
				DAMAGE_1,
				SIZE,
				DISTANCE_1,
				COOLDOWN_SECONDS
			)
		);
		mInfo.mDescriptions.add(
			String.format(
				"The thunder attacks now stun all non-boss enemies they damage for %ss. Damage is increased from %s to %s. Teleport range is increased from %s to %s blocks.",
				STUN_SECONDS,
				DAMAGE_1,
				DAMAGE_2,
				DISTANCE_1,
				DISTANCE_2
			)
		);
		mInfo.mCooldown = COOLDOWN_TICKS;
		mInfo.mIgnoreCooldown = true;

		boolean isUpgraded = getAbilityScore() == 2;
		mLevelDamage = isUpgraded ? DAMAGE_2 : DAMAGE_1;
		mLevelDistance = isUpgraded ? DISTANCE_2 : DISTANCE_1;
		mDoStun = isUpgraded;
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
		if (
			InventoryUtils.isWandItem(
				mPlayer.getInventory().getItemInMainHand()
			)
		) {
			event.setCancelled(true);

			if (
				!isTimerActive()
				&& mPlayer.isSneaking()
				&& !ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)
			) {
				putOnCooldown();

				Location playerStartLocation = mPlayer.getLocation();
				float spellDamage = SpellDamage.getSpellDamage(mPlayer, mLevelDamage);
				doDamage(playerStartLocation, spellDamage, false);

				World world = mPlayer.getWorld();
				BoundingBox movingPlayerBox = mPlayer.getBoundingBox();
				Vector vector = playerStartLocation.getDirection();
				LocationUtils.travelTillObstructed(
					world,
					movingPlayerBox,
					mLevelDistance,
					vector,
					CHECK_INCREMENT
				);
				Location playerEndLocation = movingPlayerBox
					.getCenter()
					.setY(
						movingPlayerBox.getMinY()
					)
					.toLocation(world)
					.setDirection(vector);

				new BukkitRunnable() {
					@Override
					public void run() {
						if (mPlayer == null || mPlayer.isDead() || !mPlayer.isValid()) {
							this.cancel();
						}

						mPlayer.teleport(playerEndLocation);
						doDamage(playerStartLocation, spellDamage, true);
					}
				}.runTaskLater(Plugin.getInstance(), 1);
			}
		}
	}

	private void doDamage(Location location, float spellDamage, boolean bypassIFrames) {
		World world = mPlayer.getWorld();
		world.playSound(location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1f, 1.5f);
		world.spawnParticle(Particle.REDSTONE, location, 100, 2.5, 2.5, 2.5, 3, COLOR_YELLOW);
		world.spawnParticle(Particle.REDSTONE, location, 100, 2.5, 2.5, 2.5, 3, COLOR_AQUA);
		world.spawnParticle(Particle.FLASH, location.clone().add(location.getDirection()), 1, 0, 0, 0, 10);

		List<LivingEntity> enemies = EntityUtils.getNearbyMobs(location, SIZE);
		// The more enemies, the less particles for each one
		int mobParticles = Math.max(
			1,
			20 / Math.max(1, enemies.size()) // Never divide by 0. Always maximum 20 particles for <= 1 enemy
		);

		for (LivingEntity enemy : enemies) {
			EntityUtils.damageEntity(mPlugin, enemy, spellDamage, mPlayer, MagicType.ARCANE, true, mInfo.mLinkedSpell, true, true, bypassIFrames);
			if (mDoStun && !EntityUtils.isBoss(enemy)) {
				EntityUtils.applyStun(mPlugin, STUN_TICKS, enemy);
			}

			Location enemyParticleLocation = enemy.getLocation().add(0, enemy.getHeight() / 2, 0);
			world.spawnParticle(Particle.CLOUD, enemyParticleLocation, mobParticles, 0.5, 0.5, 0.5, 0.5);
			world.spawnParticle(Particle.END_ROD, enemyParticleLocation, mobParticles, 0.5, 0.5, 0.5, 0.5);
		}
	}
}