package com.playmonumenta.plugins.depths.abilities.shadow;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.steelsage.Metalmancy;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.FlatDamageDealt;
import com.playmonumenta.plugins.point.Raycast;
import com.playmonumenta.plugins.point.RaycastData;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

import net.md_5.bungee.api.ChatColor;

public class DepthsAdvancingShadows extends DepthsAbility {

	public static final String ABILITY_NAME = "Advancing Shadows";
	public static final int[] DAMAGE = {5, 7, 9, 11, 13};

	private static final int ADVANCING_SHADOWS_RANGE = 12;
	private static final float ADVANCING_SHADOWS_AOE_KNOCKBACKS_SPEED = 0.5f;
	private static final float ADVANCING_SHADOWS_AOE_KNOCKBACKS_RANGE = 4;
	private static final double ADVANCING_SHADOWS_OFFSET = 2.7;
	private static final int COOLDOWN = 18 * 20;
	private static final int DAMAGE_DURATION = 5 * 20;

	private LivingEntity mTarget = null;

	public DepthsAdvancingShadows(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.WITHER_SKELETON_SKULL;
		mTree = DepthsTree.SHADOWS;
		mInfo.mLinkedSpell = ClassAbility.ADVANCING_SHADOWS;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public void cast(Action action) {

		LivingEntity entity = mTarget;
		if (entity != null && !entity.getScoreboardTags().contains(Metalmancy.IGNORE_TAG) && !entity.getScoreboardTags().contains(Metalmancy.GOLEM_TAG)) {
			int advancingShadows = getAbilityScore();
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
			while (loc.getBlock().getType().isSolid()) {
				loc.subtract(dir.clone().multiply(1.15));
			}

			// Prevent the player from teleporting over void
			if (loc.getY() < 8) {
				boolean safe = false;
				for (int y = 0; y < loc.getY() - 1; y++) {
					Location tempLoc = loc.clone();
					tempLoc.setY(y);
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

			world.spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1.1, 0), 50, 0.35, 0.5, 0.35, 1.0);
			world.spawnParticle(Particle.SMOKE_LARGE, mPlayer.getLocation().add(0, 1.1, 0), 12, 0.35, 0.5, 0.35, 0.05);
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.1f);

			mPlayer.teleport(loc, TeleportCause.UNKNOWN);

			EffectManager.getInstance().addEffect(mPlayer, ABILITY_NAME, new FlatDamageDealt(DAMAGE_DURATION, DAMAGE[mRarity - 1]));
			float range = ADVANCING_SHADOWS_AOE_KNOCKBACKS_RANGE;
			float speed = ADVANCING_SHADOWS_AOE_KNOCKBACKS_SPEED;
			if (advancingShadows > 1) {
				for (LivingEntity mob : EntityUtils.getNearbyMobs(entity.getLocation(),
				                                                  range, mPlayer)) {
					if (mob != entity) {
						MovementUtils.knockAway(entity, mob, speed);
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

		if (!mPlayer.isSneaking() && DepthsUtils.isWeaponItem(mPlayer.getInventory().getItemInMainHand())) {
			int range = ADVANCING_SHADOWS_RANGE;

			// Basically makes sure if the target is in LoS and if there is
			// a path.
			Location eyeLoc = mPlayer.getEyeLocation();
			Raycast ray = new Raycast(eyeLoc, eyeLoc.getDirection(), range);
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

		return false;
	}


	@Override
	public String getDescription(int rarity) {
		return "Right click while holding a weapon to teleport to the target hostile enemy within " + ADVANCING_SHADOWS_RANGE + " blocks and gain " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + " attack damage for " + DAMAGE_DURATION / 20 + " seconds. Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.SHADOWS;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.RIGHT_CLICK;
	}
}

