package com.playmonumenta.plugins.abilities.scout;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fox;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class HuntingCompanion extends Ability {
	private static final int COOLDOWN = 24 * 20;
	public static final int DURATION = 12 * 20;
	private static final int TICK_INTERVAL = 5;
	public static final String FOX_NAME = "FoxCompanion";
	public static final String FOX_TAG = "FoxCompanion";
	private static final int DETECTION_RANGE = 32;
	// DAMAGE is a percent of bow damage
	public static final double DAMAGE_1 = 0.2;
	public static final double DAMAGE_2 = 0.4;
	public static final int STUN_TIME_1 = 2 * 20;
	public static final int STUN_TIME_2 = 3 * 20;
	public static final String OWNER_METADATA_TAG = "FoxCompanionOwnerMetadataTag";
	private static final double VELOCITY = 0.9;
	private static final double JUMP_HEIGHT = 0.8;

	private Fox mFox;
	private LivingEntity mTarget;
	private double mDamage;
	public int mStunTime;
	private WindBomb mWindBomb;

	public List<Entity> mStunnedMobs;

	public HuntingCompanion(Plugin plugin, Player player) {
		super(plugin, player, "Hunting Companion");
		mInfo.mScoreboardId = "HuntingCompanion";
		mInfo.mShorthandName = "HC";
		mInfo.mDescriptions.add("Swap hands while holding a bow or crossbow to summon an invulnerable fox companion. The fox attacks the nearest mob within " + DETECTION_RANGE + " blocks. The fox prioritizes the first enemy you hit with a projectile after summoning, which can be reapplied once that target dies. The fox deals " + (int) (100 * DAMAGE_1) + "% of your projectile damage attribute (not including class bonuses) when the ability is cast as ability damage, ignoring i-frames. Once per mob, the fox stuns upon attack for " + STUN_TIME_1 / 20 + " seconds, except for elites and bosses. The fox disppears after " + DURATION / 20 + " seconds. Cooldown: " + COOLDOWN / 20 + "s.");
		mInfo.mDescriptions.add("Damage is increased to " + (int) (100 * DAMAGE_2) + "% of your projectile damage and the stun time is increased to " + STUN_TIME_2 / 20 + " seconds.");
		mInfo.mCooldown = COOLDOWN;
		mInfo.mIgnoreCooldown = true;
		mInfo.mLinkedSpell = ClassAbility.HUNTING_COMPANION;

		boolean isLevelOne = getAbilityScore() == 1;
		mDamage = isLevelOne ? DAMAGE_1 : DAMAGE_2;
		mStunTime = isLevelOne ? STUN_TIME_1 : STUN_TIME_2;

		mStunnedMobs = new ArrayList<Entity>();

		Bukkit.getScheduler().runTask(plugin, () -> {
			if (player != null) {
				mWindBomb = AbilityManager.getManager().getPlayerAbility(mPlayer, WindBomb.class);
			}
		});
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		event.setCancelled(true);

		if (mWindBomb != null && mPlayer.isSneaking()) {
			return;
		}

		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		Damageable damageable = (Damageable)inMainHand.getItemMeta();
		if (!isTimerActive() && !mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell) && InventoryUtils.isBowItem(inMainHand) && !(damageable.getDamage() > inMainHand.getType().getMaxDurability())) {
			putOnCooldown();
			mStunnedMobs = new ArrayList<Entity>();

			if (mFox != null) {
				mFox.remove();
				mFox = null;
			}

			World world = mPlayer.getWorld();
			Location loc = mPlayer.getLocation();
			Vector facingDirection = mPlayer.getEyeLocation().getDirection().normalize();
			Vector perp = (new Vector(-facingDirection.getZ(), 0, facingDirection.getX())).normalize(); //projection of the perpendicular vector to facingDirection onto the xz plane

			Vector sideOffset = new Vector(0, 0, 0);
			if (!loc.clone().add(perp).getBlock().isSolid() && !loc.clone().add(perp).add(0, 1, 0).getBlock().isSolid()) {
				sideOffset = perp;
			} else if (!loc.clone().subtract(perp).getBlock().isSolid() && !loc.clone().subtract(perp).add(0, 1, 0).getBlock().isSolid()) {
				sideOffset = perp.clone().multiply(-1);
			}

			mFox = (Fox) LibraryOfSoulsIntegration.summon(loc.clone().add(sideOffset).add(facingDirection.clone().setY(0).normalize().multiply(-0.25)), FOX_NAME); // adds facing direction so golem doesn't spawn inside user
			mFox.setInvulnerable(true);
			mFox.addScoreboardTag(FOX_TAG);

			// Damage calculation - include Base Proj Attr, Focus, Teammate buffs (Blessing/Thurible), and Sharpshooter
			double damage = EntityUtils.getProjSkillDamage(mPlayer, mPlugin) * mDamage;

			mFox.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);

			mFox.setMetadata(OWNER_METADATA_TAG, new FixedMetadataValue(mPlugin, mPlayer.getName()));

			mFox.setVelocity(facingDirection.clone().setY(JUMP_HEIGHT).normalize().multiply(VELOCITY));
			mFox.teleport(mFox.getLocation().setDirection(facingDirection));

			//Shatter if durability is 0 and isn't shattered.
			//This is needed because Hunting Companion doesn't consume durability, but there is a high-damage uncommon bow
			//with 0 durability that should not be infinitely usable.
			if (damageable.getDamage() >= inMainHand.getType().getMaxDurability() && !ItemUtils.isItemShattered(inMainHand)) {
				ItemUtils.shatterItem(inMainHand);
			}

			world.playSound(loc, Sound.ENTITY_FOX_AMBIENT, 1.5f, 0.8f);
			world.playSound(loc, Sound.ENTITY_FOX_AMBIENT, 1.5f, 1.0f);
			world.playSound(loc, Sound.ENTITY_FOX_AMBIENT, 1.5f, 1.2f);
			world.playSound(loc, Sound.ENTITY_FOX_SNIFF, 2.0f, 1.0f);
			world.playSound(loc, Sound.BLOCK_SWEET_BERRY_BUSH_BREAK, 0.75f, 1.2f);
			world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 1.0f);

			new BukkitRunnable() {
				int mTicksElapsed = 0;
				@Override
				public void run() {
					boolean isOutOfTime = mTicksElapsed >= DURATION;
					if (isOutOfTime || mFox == null) {
						if (isOutOfTime) {
							Location foxLoc = mFox.getLocation();

							world.playSound(foxLoc, Sound.ENTITY_FOX_SNIFF, 1.5f, 1.0f);
							world.playSound(loc, Sound.ENTITY_FOX_AMBIENT, 1.5f, 0.8f);
							world.playSound(loc, Sound.ENTITY_FOX_AMBIENT, 1.5f, 1.0f);
							world.playSound(loc, Sound.ENTITY_FOX_AMBIENT, 1.5f, 1.2f);
							world.spawnParticle(Particle.SMOKE_NORMAL, foxLoc, 20);
						}
						if (!(mTarget == null)) {
							mTarget.removePotionEffect(PotionEffectType.GLOWING);
							mTarget = null;
						}
						mFox.remove();
						mFox = null;
						this.cancel();
					}

					if (!(mTarget == null || mTarget.isDead() || mTarget.getHealth() <= 0)) {
						mFox.setTarget(mTarget);
					}

					if (mFox != null && (mFox.getTarget() == null || mFox.getTarget().isDead() || mFox.getTarget().getHealth() <= 0) && mTicksElapsed >= TICK_INTERVAL * 2) {
						Location foxLoc = mFox.getLocation();
						List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(foxLoc, DETECTION_RANGE, mFox);
						nearbyMobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
						LivingEntity nearestMob = EntityUtils.getNearestMob(foxLoc, nearbyMobs);
						if (nearestMob != null) {
							mFox.setTarget(nearestMob);
							world.playSound(foxLoc, Sound.ENTITY_FOX_AGGRO, 1.5f, 1.0f);
						}
					}

					mTicksElapsed += TICK_INTERVAL;
				}
			}.runTaskTimer(mPlugin, 0, TICK_INTERVAL);
		}
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity le, EntityDamageByEntityEvent event) {
		if (mFox == null || mFox.getHealth() <= 0 || !(mTarget == null || mTarget.getHealth() <= 0) || le.getLocation().distance(mFox.getLocation()) > DETECTION_RANGE || !(le instanceof Mob)) {
			return true;
		}

		World world = mPlayer.getWorld();
		mTarget = le;
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 0.5f);
		world.playSound(mFox.getLocation(), Sound.ENTITY_FOX_AGGRO, 1.5f, 1.0f);
		PotionUtils.applyPotion(mPlayer, mTarget, new PotionEffect(PotionEffectType.GLOWING, DURATION, 0, true, false));
		world.spawnParticle(Particle.VILLAGER_ANGRY, mFox.getEyeLocation(), 25);

		return true;
	}

	@Override
	public void playerQuitEvent(PlayerQuitEvent event) {
		if (mFox != null) {
			mFox.remove();
			mFox = null;
		}
	}

	public boolean isThisFox(Fox fox) {
		return fox == mFox;
	}
}
