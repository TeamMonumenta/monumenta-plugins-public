package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.abilities.HuntingCompanionBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fox;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class HuntingCompanion extends Ability {
	private static final int COOLDOWN = 24 * 20;
	public static final int DURATION = 12 * 20;
	private static final int TICK_INTERVAL = 5;
	public static final String FOX_NAME = "FoxCompanion";
	private static final int DETECTION_RANGE = 32;
	private static final double DAMAGE_FRACTION_1 = 0.2;
	private static final double DAMAGE_FRACTION_2 = 0.4;
	private static final int STUN_TIME_1 = 2 * 20;
	private static final int STUN_TIME_2 = 3 * 20;
	private static final double VELOCITY = 0.9;
	private static final double JUMP_HEIGHT = 0.8;
	private static final double MAX_TARGET_Y = 4;
	private static final double HEALING_PERCENT = 0.05;

	private @Nullable Fox mFox;
	private @Nullable LivingEntity mTarget;
	private final double mDamageFraction;
	public int mStunTime;
	private @Nullable WindBomb mWindBomb;

	public List<Entity> mStunnedMobs;

	public HuntingCompanion(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Hunting Companion");
		mInfo.mScoreboardId = "HuntingCompanion";
		mInfo.mShorthandName = "HC";
		mInfo.mDescriptions.add("Swap hands while holding a bow, crossbow, or trident to summon an invulnerable fox companion. The fox attacks the nearest mob within " + DETECTION_RANGE + " blocks. The fox prioritizes the first enemy you hit with a projectile after summoning, which can be reapplied once that target dies. The fox deals melee damage equal to " + (int) (100 * DAMAGE_FRACTION_1) + "% of your projectile damage. Once per mob, the fox stuns upon attack for " + STUN_TIME_1 / 20 + " seconds, except for elites and bosses. When a mob that was damaged by the fox dies, you heal 5% of your max health. The fox disappears after " + DURATION / 20 + " seconds. Cooldown: " + COOLDOWN / 20 + "s.");
		mInfo.mDescriptions.add("Damage is increased to " + (int) (100 * DAMAGE_FRACTION_2) + "% of your projectile damage and the stun time is increased to " + STUN_TIME_2 / 20 + " seconds.");
		mInfo.mCooldown = COOLDOWN;
		mInfo.mIgnoreCooldown = true;
		mInfo.mLinkedSpell = ClassAbility.HUNTING_COMPANION;
		mDisplayItem = new ItemStack(Material.SWEET_BERRIES, 1);

		boolean isLevelOne = getAbilityScore() == 1;
		mDamageFraction = isLevelOne ? DAMAGE_FRACTION_1 : DAMAGE_FRACTION_2;
		mStunTime = isLevelOne ? STUN_TIME_1 : STUN_TIME_2;

		mStunnedMobs = new ArrayList<Entity>();

		if (player != null) {
			Bukkit.getScheduler().runTask(plugin, () -> {
				mWindBomb = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, WindBomb.class);
			});
		}
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		event.setCancelled(true);

		if (mPlayer == null || (mWindBomb != null && mPlayer.isSneaking())) {
			return;
		}

		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		if (!isTimerActive() && !mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell) && ItemUtils.isBowOrTrident(inMainHand) && !ItemStatUtils.isShattered(inMainHand)) {
			putOnCooldown();
			mStunnedMobs = new ArrayList<>();

			if (mFox != null) {
				mFox.remove();
				mFox = null;
			}

			World world = mPlayer.getWorld();
			Location loc = mPlayer.getLocation();
			Vector facingDirection = mPlayer.getEyeLocation().getDirection().normalize();
			Vector perp = new Vector(-facingDirection.getZ(), 0, facingDirection.getX()).normalize(); //projection of the perpendicular vector to facingDirection onto the xz plane

			Vector sideOffset = new Vector(0, 0, 0);
			Location pos = loc.clone().add(perp);
			Location neg = loc.clone().subtract(perp);
			if (pos.isChunkLoaded() && !pos.getBlock().isSolid() && !pos.add(0, 1, 0).getBlock().isSolid()) {
				sideOffset = perp;
			} else if (neg.isChunkLoaded() && !neg.getBlock().isSolid() && !neg.add(0, 1, 0).getBlock().isSolid()) {
				sideOffset = perp.clone().multiply(-1);
			} else if (!loc.isChunkLoaded()) {
				// Player is standing somewhere that's not loaded, abort
				return;
			}

			mFox = (Fox) LibraryOfSoulsIntegration.summon(loc.clone().add(sideOffset).add(facingDirection.clone().setY(0).normalize().multiply(-0.25)), FOX_NAME); // adds facing direction so golem doesn't spawn inside user
			if (mFox == null) {
				return;
			}

			if (LocationUtils.isInSnowyBiome(mFox.getLocation())) {
				mFox.setFoxType(Fox.Type.SNOW);
			}

			double multiply = mPlugin.mItemStatManager.getAttributeAmount(mPlayer, ItemStatUtils.AttributeType.PROJECTILE_DAMAGE_MULTIPLY);
			double damage = mDamageFraction * ItemStatUtils.getAttributeAmount(mPlayer.getInventory().getItemInMainHand(), ItemStatUtils.AttributeType.PROJECTILE_DAMAGE_ADD, ItemStatUtils.Operation.ADD, ItemStatUtils.Slot.MAINHAND) * (multiply != 0 ? multiply : 1);

			BossManager bossManager = BossManager.getInstance();
			if (bossManager != null) {
				List<BossAbilityGroup> abilities = BossManager.getInstance().getAbilities(mFox);
				if (abilities != null) {
					for (BossAbilityGroup ability : abilities) {
						if (ability instanceof HuntingCompanionBoss huntingCompanionBoss) {
							ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
							huntingCompanionBoss.spawn(mPlayer, damage, mStunTime, HEALING_PERCENT, playerItemStats);
							break;
						}
					}
				}
			}

			mFox.setVelocity(facingDirection.clone().setY(JUMP_HEIGHT).normalize().multiply(VELOCITY));
			mFox.teleport(mFox.getLocation().setDirection(facingDirection));

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
						if (isOutOfTime && mFox != null) {
							Location foxLoc = mFox.getLocation();

							world.playSound(foxLoc, Sound.ENTITY_FOX_SNIFF, 1.5f, 1.0f);
							world.playSound(foxLoc, Sound.ENTITY_FOX_AMBIENT, 1.5f, 0.8f);
							world.playSound(foxLoc, Sound.ENTITY_FOX_AMBIENT, 1.5f, 1.0f);
							world.playSound(foxLoc, Sound.ENTITY_FOX_AMBIENT, 1.5f, 1.2f);
							new PartialParticle(Particle.SMOKE_NORMAL, foxLoc, 20).spawnAsPlayerActive(mPlayer);
						}
						if (mTarget != null) {
							mTarget.removePotionEffect(PotionEffectType.GLOWING);
							mTarget = null;
						}
						if (mFox != null) {
							mFox.remove();
							mFox = null;
						}
						this.cancel();
						return;
					}

					if (mTarget != null && !mTarget.isDead() && mTarget.getHealth() > 0) {
						mFox.setTarget(mTarget);
					}

					LivingEntity target = mFox.getTarget();
					if ((target == null || target.isDead() || target.getHealth() <= 0) && mTicksElapsed >= TICK_INTERVAL * 2) {
						Location foxLoc = mFox.getLocation();
						List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(foxLoc, DETECTION_RANGE, mFox);
						nearbyMobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG) || mob.isInvulnerable());
						nearbyMobs.removeIf((mob) -> Math.abs(mob.getLocation().getY() - foxLoc.getY()) > MAX_TARGET_Y);
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
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() != DamageType.PROJECTILE || mFox == null || mFox.getHealth() <= 0 || !(mTarget == null || mTarget.getHealth() <= 0) || enemy.getLocation().distance(mFox.getLocation()) > DETECTION_RANGE) {
			return false;
		}

		World world = mPlayer.getWorld();
		mTarget = enemy;
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 0.5f);
		world.playSound(mFox.getLocation(), Sound.ENTITY_FOX_AGGRO, 1.5f, 1.0f);
		PotionUtils.applyPotion(mPlayer, mTarget, new PotionEffect(PotionEffectType.GLOWING, DURATION, 0, true, false));
		new PartialParticle(Particle.VILLAGER_ANGRY, mFox.getEyeLocation(), 25).spawnAsPlayerActive(mPlayer);
		return true; // only one targeting instance per tick
	}

	@Override
	public void playerQuitEvent(PlayerQuitEvent event) {
		if (mFox != null) {
			mFox.remove();
			mFox = null;
		}
	}
}
