package com.playmonumenta.plugins.abilities.alchemist.harbinger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.point.Raycast;
import com.playmonumenta.plugins.point.RaycastData;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;



public class PurpleHaze extends Ability {

	public static class HazedMob {
		public Player mTriggeredBy;
		public LivingEntity mMob;
		public int mDuration;
		public int mTicksLeft;
		public int mTransfers;

		public HazedMob(LivingEntity mob, Player triggeredBy, int duration, int transfers) {
			this.mMob = mob;
			this.mTriggeredBy = triggeredBy;
			this.mTicksLeft = duration;
			this.mDuration = duration;
			this.mTransfers = transfers;
		}
	}

	private static final int PURPLE_HAZE_1_DURATION = 20 * 8;
	private static final int PURPLE_HAZE_2_DURATION = 20 * 10;

	private static final int PURPLE_HAZE_1_COOLDOWN = 20 * 35;
	private static final int PURPLE_HAZE_2_COOLDOWN = 20 * 20;

	private static final int PURPLE_HAZE_TRANSFER_DEPTH = 2;
	private static final int PURPLE_HAZE_TRANSFER_BREADTH = 2;

	private static final double PURPLE_HAZE_PERCENT_POTION_DAMAGE = 0.2;

	private static final int PURPLE_HAZE_RADIUS = 5;
	private static final int PURPLE_HAZE_RANGE = 32;

	private static final Map<UUID, HazedMob> mHazedMobs = new HashMap<>();
	private static final Map<UUID, HazedMob> newHazedMobs = new HashMap<>();

	private static BukkitRunnable mRunnable = null;

	private final int mDuration;

	private LivingEntity mTarget = null;

	public PurpleHaze(Plugin plugin, Player player) {
		super(plugin, player, "Purple Haze");
		mInfo.mLinkedSpell = ClassAbility.PURPLE_HAZE;
		mInfo.mScoreboardId = "PurpleHaze";
		mInfo.mShorthandName = "PH";
		mInfo.mDescriptions.add("Left-clicking while shifted with a bow while looking at a mob within 32 blocks deals 20% of your potion damage per second and apply potion effect for 8 seconds to that mob. If the target dies, the effects transfer to up to 2 mob up to 5 blocks from the target that died. The maximum number of times effects can spread is a chain 2 mobs deep. Cooldown: 35s.");
		mInfo.mDescriptions.add("Duration increases to 10 seconds. Cooldown reduced to 20s.");
		mInfo.mCooldown = getAbilityScore() == 1 ? PURPLE_HAZE_1_COOLDOWN : PURPLE_HAZE_2_COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mDuration = getAbilityScore() == 1 ? PURPLE_HAZE_1_DURATION : PURPLE_HAZE_2_DURATION;
		mDisplayItem = new ItemStack(Material.POPPED_CHORUS_FRUIT, 1);

		/*
		 * Only one runnable ever exists for purple haze - it is a global list, not tied to any individual players
		 * At least one player must be a warlock for this to start running. Once started, it runs forever.
		 */
		if (mRunnable == null || mRunnable.isCancelled()) {
			mRunnable = new BukkitRunnable() {
				int mCounter = 0;
				@Override
				public void run() {
					mCounter++;
					if (mCounter % 20 == 0) {
						for (Map.Entry<UUID, HazedMob> entry : mHazedMobs.entrySet()) {
							HazedMob e = entry.getValue();
							LivingEntity damagee = e.mMob;
							AlchemistPotions ability = AbilityManager.getManager().getPlayerAbility(e.mTriggeredBy, AlchemistPotions.class);
							if (ability != null) {
								ability.applyEffects(damagee);
								EntityUtils.damageEntity(plugin, damagee, ability.getDamage() * PURPLE_HAZE_PERCENT_POTION_DAMAGE, e.mTriggeredBy, MagicType.ALCHEMY, false /* do not register CustomDamageEvent */, mInfo.mLinkedSpell, false, false, true, true);
								Location loc = damagee.getLocation().add(0, 1, 0);
								World world = damagee.getWorld();
								world.spawnParticle(Particle.SPELL_WITCH, loc, 10, 0, 0.2, 0, 0.0001);
								world.spawnParticle(Particle.FALLING_DUST, loc, 10, 0.2, 0.65, 0.2, Bukkit.createBlockData("purple_concrete"));
								world.spawnParticle(Particle.FALLING_DUST, loc, 10, 0.2, 0.65, 0.2, Bukkit.createBlockData("pink_terracotta"));
								mCounter = 0;
							}
						}
					}

					Iterator<Map.Entry<UUID, HazedMob>> iter = mHazedMobs.entrySet().iterator();
					while (iter.hasNext()) {
						Map.Entry<UUID, HazedMob> e = iter.next();
						HazedMob hazer = e.getValue();
						hazer.mTicksLeft--;
						if (hazer.mTicksLeft <= 0 || hazer.mMob.isDead()) {
							if (hazer.mMob.isDead() && hazer.mTransfers > 0) {
								Location loc = hazer.mMob.getLocation();
								// Perhaps a ball of purple haze going from the dead mob to the next instead?

								World world = hazer.mMob.getWorld();
								world.spawnParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 40, 2, 1, 2, 0.0001);
								world.spawnParticle(Particle.FALLING_DUST, loc.clone().add(0, 1, 0), 15, 1, 1, 1, Bukkit.createBlockData("purple_concrete"));
								world.spawnParticle(Particle.FALLING_DUST, loc.clone().add(0, 1, 0), 15, 1, 1, 1, Bukkit.createBlockData("pink_terracotta"));
								world.playSound(loc, Sound.BLOCK_CHORUS_FLOWER_GROW, 1.0f, 2.0f);
								world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, 0.55f, 1.5f);

								for (int i = 0; i < PURPLE_HAZE_TRANSFER_BREADTH; i++) {
									double closest = PURPLE_HAZE_RADIUS + 1;
									LivingEntity closestMob = null;
									for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, PURPLE_HAZE_RADIUS)) {
										UUID mobUUID = mob.getUniqueId();
										double distance = mob.getLocation().distance(loc);
										if (!mob.isDead() && distance < closest && !newHazedMobs.containsKey(mobUUID) && !mHazedMobs.containsKey(mobUUID)) {
											closest = distance;
											closestMob = mob;
										}
									}

									if (closestMob != null) {
										HazedMob hazed = new HazedMob(closestMob, hazer.mTriggeredBy, hazer.mDuration, hazer.mTransfers - 1);
										newHazedMobs.put(hazed.mMob.getUniqueId(), hazed);
										Location loc2 = hazed.mMob.getLocation();
										world.spawnParticle(Particle.SPELL_WITCH, loc2.clone().add(0, 1, 0), 60, 0.5, 1, 0.5, 0.001);

										Location hazedLoc = hazed.mMob.getEyeLocation();
										Location hazerLoc = hazer.mMob.getEyeLocation();
										Vector dir = LocationUtils.getDirectionTo(hazedLoc, hazerLoc);
										for (int j = 0; j < 50; j++) {
											hazerLoc.add(dir.clone().multiply(0.1));
											if (j % 2 == 0) { // Just to maintain the two colours without having too many particles
												world.spawnParticle(Particle.FALLING_DUST, hazerLoc, 1, 0.1, 0.1, 0.1, Bukkit.createBlockData("purple_concrete"));
											} else {
												world.spawnParticle(Particle.FALLING_DUST, hazerLoc, 1, 0.1, 0.1, 0.1, Bukkit.createBlockData("pink_terracotta"));
											}
											world.spawnParticle(Particle.SPELL_WITCH, hazerLoc, 1, 0.05, 0.05, 0.05, 0.0001);
											if (hazerLoc.distance(hazedLoc) < 0.4) {
												break;
											}
										}
									}
								}
							}
							iter.remove();
						}
					}
					// Adding to the set iterated on creates problems, so all additions to it are executed afterwards.
					for (UUID newHazedMob : newHazedMobs.keySet()) {
						mHazedMobs.put(newHazedMob, newHazedMobs.get(newHazedMob));
					}
					newHazedMobs.clear();
				}
			};
			mRunnable.runTaskTimer(plugin, 0, 1);
		}
	}

	@Override
	public boolean runCheck() {
		if (mPlayer.isSneaking()) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			if (ItemUtils.isSomeBow(mainHand)) {

				// Basically makes sure if the target is in LoS and if there is a path.
				Location eyeLoc = mPlayer.getEyeLocation();
				Raycast ray = new Raycast(eyeLoc, eyeLoc.getDirection(), PURPLE_HAZE_RANGE);
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

	@Override
	public void cast(Action action) {
		LivingEntity entity = mTarget;
		if (entity != null && !mHazedMobs.containsKey(entity.getUniqueId())) {
			HazedMob hazed = new HazedMob(entity, mPlayer, mDuration, PURPLE_HAZE_TRANSFER_DEPTH);
			mHazedMobs.put(entity.getUniqueId(), hazed);
			Location loc = hazed.mMob.getLocation();
			World world = mPlayer.getWorld();
			world.spawnParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 60, 0.5, 1, 0.5, 0.001);
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 0.5f, 0.5f);
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.25f, 0.5f);
			putOnCooldown();
		}
		mTarget = null;
	}
}