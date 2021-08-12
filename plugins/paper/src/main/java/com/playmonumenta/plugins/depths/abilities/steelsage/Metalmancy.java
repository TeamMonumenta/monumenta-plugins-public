package com.playmonumenta.plugins.depths.abilities.steelsage;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;

public class Metalmancy extends DepthsAbility {

	public static final String ABILITY_NAME = "Metalmancy";

	public static final double[] DAMAGE = {10, 12.5, 15, 17.5, 20};
	public static final int[] DURATION = {10 * 20, 11 * 20, 12 * 20, 13 * 20, 14 * 20};
	public static final int COOLDOWN = 32 * 20;
	public static final String GOLEM_NAME = "SteelConstruct";
	public static final String GOLEM_TAG = "boss_metalmancy";
	public static final double VELOCITY = 2;
	public static final int DETECTION_RANGE = 32;
	public static final int TICK_INTERVAL = 5;

	private Mob mGolem;
	private Mob mTarget;
	private boolean mOriginalGlowing = false;

	public Metalmancy(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.IRON_BLOCK;
		mTree = DepthsTree.METALLIC;
		mInfo.mLinkedSpell = ClassAbility.METALMANCY;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mIgnoreCooldown = true; // so that the shooting portion functions; activation manually checks for cooldwon
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {

		event.setCancelled(true);

		if (!isTimerActive() && DepthsUtils.isWeaponItem(mPlayer.getInventory().getItemInMainHand())) {
			putOnCooldown();

			World world = mPlayer.getWorld();
			Location loc = mPlayer.getLocation();
			Vector facingDirection = mPlayer.getEyeLocation().getDirection().normalize();
			mGolem = (Mob) LibraryOfSoulsIntegration.summon(mPlayer.getLocation().add(facingDirection).add(0, 1, 0), GOLEM_NAME); // adds facing direction so golem doesn't spawn inside user
			mGolem.setInvulnerable(true);
			mGolem.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(DAMAGE[mRarity - 1]);
			mGolem.setVelocity(facingDirection.multiply(VELOCITY));

			world.playSound(loc, Sound.ENTITY_IRON_GOLEM_REPAIR, 1.0f, 1.0f);
			world.playSound(loc, Sound.BLOCK_CHAIN_BREAK, 1.0f, 1.0f);
			world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);

			new BukkitRunnable() {
				int mTicksElapsed = 0;
				@Override
				public void run() {
					boolean isOutOfTime = mTicksElapsed >= DURATION[mRarity - 1];
					if (isOutOfTime || mGolem == null || mGolem.getHealth() <= 0) {
						if (isOutOfTime) {
							Location golemLoc = mGolem.getLocation();
							world.playSound(golemLoc, Sound.ENTITY_IRON_GOLEM_DEATH, 0.8f, 1.0f);
							world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, golemLoc, 15);
							world.spawnParticle(Particle.SMOKE_NORMAL, golemLoc, 20);
						}
						if (!(mTarget == null)) {
							if (!mOriginalGlowing) {
								mTarget.setGlowing(false);
							}
							mTarget = null;
						}
						mGolem.remove();
						mGolem = null;
						this.cancel();
					}

					if (!(mTarget == null || mTarget.isDead() || mTarget.getHealth() <= 0)) {
						if (mTarget == mGolem) {
							mTarget = null;
						} else {
							mGolem.setTarget(mTarget);
						}
					}

					if (mGolem != null && (mGolem.getTarget() == null || mGolem.getTarget().isDead() || mGolem.getTarget().getHealth() <= 0) && mTicksElapsed >= TICK_INTERVAL * 2) {
						Location golemLoc = mGolem.getLocation();
						List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(golemLoc, DETECTION_RANGE, mGolem);
						nearbyMobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
						LivingEntity nearestMob = EntityUtils.getNearestMob(golemLoc, nearbyMobs);
						if (nearestMob != null) {
							mGolem.setTarget(nearestMob);
						}
					}

					mTicksElapsed += TICK_INTERVAL;
				}
			}.runTaskTimer(mPlugin, 0, TICK_INTERVAL);
		}
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity le, EntityDamageByEntityEvent event) {
		if (mGolem == null || mGolem.getHealth() <= 0 || !(mTarget == null || mTarget.getHealth() <= 0) || le.getLocation().distance(mGolem.getLocation()) > DETECTION_RANGE || !(le instanceof Mob)) {
			return true;
		}

		World world = mPlayer.getWorld();
		mTarget = (Mob) le;
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 0.5f);
		mOriginalGlowing = le.isGlowing();
		le.setGlowing(true);
		world.spawnParticle(Particle.VILLAGER_ANGRY, mGolem.getEyeLocation(), 15);

		return true;
	}

	@Override
	public String getDescription(int rarity) {
		return "Swap hands while holding a weapon to summon an invulnerable steel construct. The Construct attacks the nearest mob within " + DETECTION_RANGE + " blocks. The Construct prioritizes the first enemy you hit with a projectile after summoning, which can be reapplied once that target dies. The Construct deals " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + " damage and taunts non-boss enemies it hits. The Construct disappears after " + DepthsUtils.getRarityColor(rarity) + DURATION[rarity - 1] / 20 + ChatColor.WHITE + " seconds. Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.METALLIC;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SWAP;
	}
}
