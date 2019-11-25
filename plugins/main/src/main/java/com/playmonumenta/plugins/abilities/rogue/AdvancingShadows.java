package com.playmonumenta.plugins.abilities.rogue;

import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.point.Raycast;
import com.playmonumenta.plugins.point.RaycastData;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

public class AdvancingShadows extends Ability {

	private LivingEntity target = null;

	private static final int ADVANCING_SHADOWS_RANGE_1 = 11;
	private static final int ADVANCING_SHADOWS_RANGE_2 = 16;
	private static final float ADVANCING_SHADOWS_AOE_KNOCKBACKS_SPEED = 0.5f;
	private static final float ADVANCING_SHADOWS_AOE_KNOCKBACKS_RANGE = 4;
	private static final double ADVANCING_SHADOWS_OFFSET = 2.7;
	private static final int ADVANCING_SHADOWS_STRENGTH_DURATION = 5 * 20;
	private static final int ADVANCING_SHADOWS_STRENGTH_EFFECT_LEVEL = 1;
	private static final int ADVANCING_SHADOWS_COOLDOWN = 20 * 20;

	public AdvancingShadows(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.ADVANCING_SHADOWS;
		mInfo.scoreboardId = "AdvancingShadows";
		mInfo.cooldown = ADVANCING_SHADOWS_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public void cast(Action action) {

		LivingEntity entity = target;
		if (entity != null) {
			int advancingShadows = getAbilityScore();
			Vector dir = LocationUtils.getDirectionTo(entity.getLocation(), mPlayer.getLocation());
			Location loc = mPlayer.getLocation();
			while (loc.distance(entity.getLocation()) > ADVANCING_SHADOWS_OFFSET) {
				loc.add(dir.clone().multiply(0.3333));
				mWorld.spawnParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 4, 0.3, 0.5, 0.3, 1.0);
				mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc.clone().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.025);
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
					mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.8f);
					return;
				}

				// Don't teleport players below y = 1.1 to avoid clipping into oblivion
				loc.setY(Math.max(1.1, loc.getY()));
			}

			mWorld.spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1.1, 0), 50, 0.35, 0.5, 0.35, 1.0);
			mWorld.spawnParticle(Particle.SMOKE_LARGE, mPlayer.getLocation().add(0, 1.1, 0), 12, 0.35, 0.5, 0.35, 0.05);
			mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.1f);

			mPlayer.teleport(loc);

			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
			                                 new PotionEffect(PotionEffectType.INCREASE_DAMAGE, ADVANCING_SHADOWS_STRENGTH_DURATION,
			                                                  ADVANCING_SHADOWS_STRENGTH_EFFECT_LEVEL, true, true));

			if (advancingShadows > 1) {
				for (LivingEntity mob : EntityUtils.getNearbyMobs(entity.getLocation(),
				                                                  ADVANCING_SHADOWS_AOE_KNOCKBACKS_RANGE, mPlayer)) {
					if (mob != entity) {
						MovementUtils.KnockAway(entity, mob, ADVANCING_SHADOWS_AOE_KNOCKBACKS_SPEED);
					}
				}
			}

			mWorld.spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1.1, 0), 50, 0.35, 0.5, 0.35, 1.0);
			mWorld.spawnParticle(Particle.SMOKE_LARGE, mPlayer.getLocation().add(0, 1.1, 0), 12, 0.35, 0.5, 0.35, 0.05);
			mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.1f);
			target = null;
			putOnCooldown();
		}
	}

	@Override
	public boolean runCheck() {
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
		if (InventoryUtils.isSwordItem(mainHand) && InventoryUtils.isSwordItem(offHand)) {
			if (!mPlayer.isSneaking()) {
				int advancingShadows = getAbilityScore();
				int range = (advancingShadows == 1) ? ADVANCING_SHADOWS_RANGE_1 : ADVANCING_SHADOWS_RANGE_2;

				// Basically makes sure if the target is in LoS and if there is
				// a path.
				Location eyeLoc = mPlayer.getEyeLocation();
				Raycast ray = new Raycast(eyeLoc, eyeLoc.getDirection(), range);
				ray.throughBlocks = false;
				ray.throughNonOccluding = false;
				if (AbilityManager.getManager().isPvPEnabled(mPlayer)) {
					ray.targetPlayers = true;
				}

				RaycastData data = ray.shootRaycast();

				List<LivingEntity> rayEntities = data.getEntities();
				if (rayEntities != null && !rayEntities.isEmpty()) {
					for (LivingEntity t : rayEntities) {
						if (!t.getUniqueId().equals(mPlayer.getUniqueId()) && t.isValid() && !t.isDead() && EntityUtils.isHostileMob(t)) {
							target = t;
							return true;
						}
					}
				}
			}
		}
		return false;
	}

}
