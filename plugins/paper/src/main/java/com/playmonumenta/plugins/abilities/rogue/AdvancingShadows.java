package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.rogue.swordsage.BladeDance;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.point.Raycast;
import com.playmonumenta.plugins.point.RaycastData;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class AdvancingShadows extends Ability {

	private static final int ADVANCING_SHADOWS_RANGE_1 = 11;
	private static final int ADVANCING_SHADOWS_RANGE_2 = 16;
	private static final float ADVANCING_SHADOWS_AOE_KNOCKBACKS_SPEED = 0.5f;
	private static final float ADVANCING_SHADOWS_AOE_KNOCKBACKS_RANGE = 4;
	private static final double ADVANCING_SHADOWS_OFFSET = 2.7;
	private static final int DURATION = 5 * 20;
	private static final double DAMAGE_BONUS_1 = 0.3;
	private static final double DAMAGE_BONUS_2 = 0.4;
	private static final int ADVANCING_SHADOWS_COOLDOWN = 20 * 20;
	private static final double ENHANCEMENT_BONUS_DAMAGE = 0.2;
	private static final int ENHANCEMENT_BONUS_DAMAGE_DURATION = 20 * 5;

	public static final String CHARM_DAMAGE = "Advancing Shadows Damage Multiplier";
	public static final String CHARM_COOLDOWN = "Advancing Shadows Cooldown";
	public static final String CHARM_RANGE = "Advancing Shadows Range";
	public static final String CHARM_KNOCKBACK = "Advancing Shadows Knockback";

	private static final String PERCENT_DAMAGE_DEALT_EFFECT_NAME = "AdvancingShadowsPercentDamageDealtEffect";
	private static final EnumSet<DamageEvent.DamageType> AFFECTED_DAMAGE_TYPES = EnumSet.of(DamageType.MELEE, DamageType.MELEE_ENCH, DamageType.MELEE_SKILL);
	private static final String ENHANCEMENT_EFFECT_NAME = "AdvancingShadowsEnhancementPercentDamageDealtEffect";

	private @Nullable LivingEntity mTarget = null;
	private boolean mHasBladeDance;

	private final double mPercentDamageDealt;
	private final double mActivationRange;

	public AdvancingShadows(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Advancing Shadows");
		mInfo.mLinkedSpell = ClassAbility.ADVANCING_SHADOWS;
		mInfo.mScoreboardId = "AdvancingShadows";
		mInfo.mShorthandName = "AS";
		mInfo.mCooldown = CharmManager.getCooldown(player, CHARM_COOLDOWN, ADVANCING_SHADOWS_COOLDOWN);
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.mIgnoreCooldown = true;
		mInfo.mDescriptions.add(
			String.format("While holding two swords and not sneaking, right click to teleport to the target hostile enemy within %s blocks and gain +%s%% Melee Damage for %s seconds. Cooldown: %ss.",
				ADVANCING_SHADOWS_RANGE_1 - 1,
				(int)(DAMAGE_BONUS_1 * 100),
				DURATION / 20,
				ADVANCING_SHADOWS_COOLDOWN / 20));
		mInfo.mDescriptions.add(
			String.format("Damage increased to +%s%% Melee Damage for %ss, teleport range is increased to %s blocks and all hostile non-target mobs within %s blocks are knocked away from the target.",
				(int)(DAMAGE_BONUS_2 * 100),
				DURATION / 20,
				ADVANCING_SHADOWS_RANGE_2 - 1,
				(int)ADVANCING_SHADOWS_AOE_KNOCKBACKS_RANGE));
		mInfo.mDescriptions.add(
			String.format("You deal %s%% extra damage for %ss to the target.",
				(int)(ENHANCEMENT_BONUS_DAMAGE * 100),
				ENHANCEMENT_BONUS_DAMAGE_DURATION / 20));
		mDisplayItem = new ItemStack(Material.ENDER_EYE, 1);
		mPercentDamageDealt = CharmManager.getLevelPercentDecimal(player, CHARM_DAMAGE) + (isLevelOne() ? DAMAGE_BONUS_1 : DAMAGE_BONUS_2);
		mActivationRange = CharmManager.calculateFlatAndPercentValue(player, CHARM_RANGE, (isLevelOne() ? ADVANCING_SHADOWS_RANGE_1 : ADVANCING_SHADOWS_RANGE_2));

		Bukkit.getScheduler().runTask(plugin, () -> {
			mHasBladeDance = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, BladeDance.class) != null;
		});
	}

	@Override
	public void cast(Action action) {
		if (mPlayer == null || mTarget == null || isTimerActive()) {
			return;
		}
		LivingEntity entity = mTarget;
		double maxRange = mActivationRange;
		double origDistance = mPlayer.getLocation().distance(entity.getLocation());
		if (origDistance <= maxRange) {
			Vector dir = LocationUtils.getDirectionTo(entity.getLocation(), mPlayer.getLocation());
			World world = mPlayer.getWorld();
			Location loc = mPlayer.getLocation();
			while (loc.distance(entity.getLocation()) > ADVANCING_SHADOWS_OFFSET) {
				loc.add(dir.clone().multiply(0.3333));
				new PartialParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 4, 0.3, 0.5, 0.3, 1.0).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.SMOKE_NORMAL, loc.clone().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.025).spawnAsPlayerActive(mPlayer);
				if (loc.distance(entity.getLocation()) < ADVANCING_SHADOWS_OFFSET) {
					double multiplier = ADVANCING_SHADOWS_OFFSET - loc.distance(entity.getLocation());
					loc.subtract(dir.clone().multiply(multiplier));
					break;
				}
			}
			loc.add(0, 1, 0);

			// Just in case the player's teleportation loc is in a block.
			int count = 0;
			while (count < 5 && (!loc.isChunkLoaded() || loc.getBlock().getType().isSolid())) {
				count++;
				loc.subtract(dir.clone().multiply(1.15));
			}

			// If still solid, something is wrong.
			if (!loc.isChunkLoaded() || loc.getBlock().getType().isSolid()) {
				world.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.8f);
				return;
			}

			// Prevent the player from teleporting over void
			if (loc.getY() < 8) {
				boolean safe = false;
				for (int y = 0; y < loc.getY() - 1; y++) {
					Location tempLoc = loc.clone();
					tempLoc.setY(y);
					if (!tempLoc.isChunkLoaded()) {
						continue;
					}
					if (!tempLoc.getBlock().isPassable()) {
						safe = true;
						break;
					}
				}

				// Maybe void - not worth it
				if (!safe) {
					world.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.8f);
					return;
				}

				// Don't teleport players below y = 1.1 to avoid clipping into oblivion
				loc.setY(Math.max(1.1, loc.getY()));
			}

			// Extra safeguard to prevent bizarro teleports
			if (mPlayer.getLocation().distance(loc) > maxRange) {
				world.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.8f);
				return;
			}

			new PartialParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1.1, 0), 50, 0.35, 0.5, 0.35, 1.0).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.SMOKE_LARGE, mPlayer.getLocation().add(0, 1.1, 0), 12, 0.35, 0.5, 0.35, 0.05).spawnAsPlayerActive(mPlayer);
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.1f);

			if (loc.distance(entity.getLocation()) <= origDistance) {
				mPlayer.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
			}

			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_DEALT_EFFECT_NAME, new PercentDamageDealt(DURATION, mPercentDamageDealt, AFFECTED_DAMAGE_TYPES));
			if (isLevelTwo()) {
				for (LivingEntity mob : EntityUtils.getNearbyMobs(entity.getLocation(),
					ADVANCING_SHADOWS_AOE_KNOCKBACKS_RANGE, mPlayer)) {
					if (mob != entity) {
						MovementUtils.knockAway(entity, mob, (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, ADVANCING_SHADOWS_AOE_KNOCKBACKS_SPEED), true);
					}
				}
			}

			if (isEnhanced()) {
				mPlugin.mEffectManager.addEffect(mPlayer, ENHANCEMENT_EFFECT_NAME, new PercentDamageDealt(ENHANCEMENT_BONUS_DAMAGE_DURATION, ENHANCEMENT_BONUS_DAMAGE, null, 0, (player, enemy) -> enemy == entity));
			}

			new PartialParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1.1, 0), 50, 0.35, 0.5, 0.35, 1.0).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.SMOKE_LARGE, mPlayer.getLocation().add(0, 1.1, 0), 12, 0.35, 0.5, 0.35, 0.05).spawnAsPlayerActive(mPlayer);
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.1f);
			mTarget = null;
			putOnCooldown();
		}
	}

	@Override
	public boolean runCheck() {
		if (InventoryUtils.rogueTriggerCheck(mPlugin, mPlayer)) {
			if (!mPlayer.isSneaking()) {
				// *TO DO* - Turn into boolean in constructor -or- look at changing trigger entirely
				if (mHasBladeDance && mPlayer.getLocation().getPitch() >= 50) {
					return false;
				}

				// Basically makes sure if the target is in LoS and if there is
				// a path.
				Location eyeLoc = mPlayer.getEyeLocation();
				Raycast ray = new Raycast(eyeLoc, eyeLoc.getDirection(), (int) Math.ceil(mActivationRange));
				ray.mThroughBlocks = false;
				ray.mThroughNonOccluding = false;
				if (AbilityManager.getManager().isPvPEnabled(mPlayer)) {
					ray.mTargetPlayers = true;
				}

				RaycastData data = ray.shootRaycast();

				List<LivingEntity> rayEntities = data.getEntities();
				if (rayEntities != null && !rayEntities.isEmpty()) {
					for (LivingEntity t : rayEntities) {
						if (!t.getUniqueId().equals(mPlayer.getUniqueId()) && t.isValid() && !t.isDead() && EntityUtils.isHostileMob(t)) {
							mTarget = t;
							return true;
						}
					}
				}
			}
		}
		return false;
	}
}
