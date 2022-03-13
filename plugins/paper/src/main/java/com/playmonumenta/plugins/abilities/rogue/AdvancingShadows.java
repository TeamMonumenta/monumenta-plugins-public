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
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
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

	private static final String PERCENT_DAMAGE_DEALT_EFFECT_NAME = "AdvancingShadowsPercentDamageDealtEffect";
	private static final EnumSet<DamageEvent.DamageType> AFFECTED_DAMAGE_TYPES = EnumSet.of(DamageType.MELEE, DamageType.MELEE_ENCH, DamageType.MELEE_SKILL);

	private @Nullable LivingEntity mTarget = null;
	private @Nullable BladeDance mBladeDance;

	private final double mPercentDamageDealt;
	private final int mActivationRange;

	public AdvancingShadows(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Advancing Shadows");
		mInfo.mLinkedSpell = ClassAbility.ADVANCING_SHADOWS;
		mInfo.mScoreboardId = "AdvancingShadows";
		mInfo.mShorthandName = "AS";
		mInfo.mCooldown = ADVANCING_SHADOWS_COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.mDescriptions.add("While holding two swords and not sneaking, right click to teleport to the target hostile enemy within " + (ADVANCING_SHADOWS_RANGE_1 - 1) + " blocks and gain +30% Melee Damage for 5 seconds. Cooldown: 20s.");
		mInfo.mDescriptions.add("Damage increased to +40% Melee Damage for 5s, teleport range is increased to " + (ADVANCING_SHADOWS_RANGE_2 - 1) + " blocks and all hostile non-target mobs within " + ADVANCING_SHADOWS_AOE_KNOCKBACKS_RANGE + " blocks are knocked away from the target.");
		mDisplayItem = new ItemStack(Material.ENDER_EYE, 1);
		mPercentDamageDealt = isLevelOne() ? DAMAGE_BONUS_1 : DAMAGE_BONUS_2;
		mActivationRange = isLevelOne() ? ADVANCING_SHADOWS_RANGE_1 : ADVANCING_SHADOWS_RANGE_2;

		if (player != null) {
			Bukkit.getScheduler().runTask(plugin, () -> {
				mBladeDance = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, BladeDance.class);
			});
		}
	}

	@Override
	public void cast(Action action) {
		if (mPlayer == null || mTarget == null) {
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
				world.spawnParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 4, 0.3, 0.5, 0.3, 1.0);
				world.spawnParticle(Particle.SMOKE_NORMAL, loc.clone().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.025);
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

			world.spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1.1, 0), 50, 0.35, 0.5, 0.35, 1.0);
			world.spawnParticle(Particle.SMOKE_LARGE, mPlayer.getLocation().add(0, 1.1, 0), 12, 0.35, 0.5, 0.35, 0.05);
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.1f);

			if (loc.distance(entity.getLocation()) <= origDistance) {
				mPlayer.teleport(loc, TeleportCause.UNKNOWN);
			}

			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_DEALT_EFFECT_NAME, new PercentDamageDealt(DURATION, mPercentDamageDealt, AFFECTED_DAMAGE_TYPES));
			if (isLevelTwo()) {
				for (LivingEntity mob : EntityUtils.getNearbyMobs(entity.getLocation(),
						ADVANCING_SHADOWS_AOE_KNOCKBACKS_RANGE, mPlayer)) {
					if (mob != entity) {
						MovementUtils.knockAway(entity, mob, ADVANCING_SHADOWS_AOE_KNOCKBACKS_SPEED, true);
					}
				}
			}

			world.spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1.1, 0), 50, 0.35, 0.5, 0.35, 1.0);
			world.spawnParticle(Particle.SMOKE_LARGE, mPlayer.getLocation().add(0, 1.1, 0), 12, 0.35, 0.5, 0.35, 0.05);
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
				if (mBladeDance != null && mPlayer.getLocation().getPitch() >= 50) {
					return false;
				}

				// Basically makes sure if the target is in LoS and if there is
				// a path.
				Location eyeLoc = mPlayer.getEyeLocation();
				Raycast ray = new Raycast(eyeLoc, eyeLoc.getDirection(), mActivationRange);
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
