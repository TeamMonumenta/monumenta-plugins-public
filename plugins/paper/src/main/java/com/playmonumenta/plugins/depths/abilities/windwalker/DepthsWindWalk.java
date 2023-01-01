package com.playmonumenta.plugins.depths.abilities.windwalker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.bosses.bosses.CrowdControlImmunityBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.Iterator;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class DepthsWindWalk extends DepthsAbility {

	private static final double[] VULNERABILITY = {0.15, 0.175, 0.2, 0.225, 0.25, 0.4};
	public static final int[] COOLDOWN = {14 * 20, 13 * 20, 12 * 20, 11 * 20, 10 * 20, 8 * 20};
	private static final int LEVITATION_DURATION = 20 * 2;
	private static final int VULN_DURATION = 20 * 5;
	private static final int WIND_WALK_RADIUS = 3;
	private static final double WIND_WALK_Y_VELOCITY = 0.2;
	private static final double WIND_WALK_Y_VELOCITY_MULTIPLIER = 0.2;
	private static final double WIND_WALK_VELOCITY_BONUS = 1.5;

	public static final DepthsAbilityInfo<DepthsWindWalk> INFO =
		new DepthsAbilityInfo<>(DepthsWindWalk.class, "Wind Walk", DepthsWindWalk::new, DepthsTree.WINDWALKER, DepthsTrigger.SHIFT_RIGHT_CLICK)
			.linkedSpell(ClassAbility.WIND_WALK)
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", DepthsWindWalk::cast,
				new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(true).keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS), HOLDING_WEAPON_RESTRICTION))
			.displayItem(new ItemStack(Material.WHITE_DYE))
			.descriptions(DepthsWindWalk::getDescription, MAX_RARITY);

	private boolean mIsWalking = false;

	public DepthsWindWalk(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		putOnCooldown();

		Location loc = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 1.75f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1, 1f);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 90, 0.25, 0.45, 0.25, 0.1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, loc, 20, 0.25, 0.45, 0.25, 0.15).spawnAsPlayerActive(mPlayer);
		Vector direction = loc.getDirection();
		Vector yVelocity = new Vector(0, direction.getY() * WIND_WALK_Y_VELOCITY_MULTIPLIER + WIND_WALK_Y_VELOCITY, 0);
		mPlayer.setVelocity(direction.multiply(WIND_WALK_VELOCITY_BONUS).add(yVelocity));
		// Have them dodge melee attacks while casting
		mIsWalking = true;
		new BukkitRunnable() {

			@Override
			public void run() {
				mIsWalking = false;
			}

		}.runTaskLater(mPlugin, 10);

		cancelOnDeath(new BukkitRunnable() {
			final List<LivingEntity> mMobsNotHit = EntityUtils.getNearbyMobs(mPlayer.getLocation(), 32);
			boolean mTickOne = true;

			@Override
			public void run() {
				if (mPlayer.isDead() || !mPlayer.isOnline() || !mPlayer.getLocation().isChunkLoaded()) {
					this.cancel();
					return;
				}

				new PartialParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation().add(0, 1, 0), 7, 0.25, 0.45, 0.25, 0).spawnAsPlayerActive(mPlayer);

				Iterator<LivingEntity> iter = mMobsNotHit.iterator();
				while (iter.hasNext()) {
					LivingEntity mob = iter.next();

					if (mob.getLocation().distance(mPlayer.getLocation()) < WIND_WALK_RADIUS) {
						if (!EntityUtils.isBoss(mob)) {
							new PartialParticle(Particle.SWEEP_ATTACK, mob.getLocation().add(0, 1, 0), 16, 0.5, 0.5, 0.5, 0).spawnAsPlayerActive(mPlayer);
							world.playSound(mob.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, 1.25f);

							EntityUtils.applyVulnerability(mPlugin, VULN_DURATION, VULNERABILITY[mRarity - 1], mob);

							if (EntityUtils.isElite(mob)) {
								new PartialParticle(Particle.EXPLOSION_NORMAL, mob.getLocation().add(0, 1, 0), 20, 0.25, 0.45, 0.25, 0.1).spawnAsPlayerActive(mPlayer);
								world.playSound(mob.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.75f, 0.75f);
							} else {
								new PartialParticle(Particle.CLOUD, mob.getLocation().add(0, 1, 0), 20, 0.25, 0.45, 0.25, 0.1).spawnAsPlayerActive(mPlayer);

								if (!ScoreboardUtils.checkTag(mob, CrowdControlImmunityBoss.identityTag)) {
									mob.setVelocity(mob.getVelocity().setY(0.5));
									PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.LEVITATION, LEVITATION_DURATION, 0, true, false));
								}
							}
						}

						iter.remove();
					}
				}

				Material block = mPlayer.getLocation().getBlock().getType();
				if (!mTickOne && (mPlayer.isOnGround() || block == Material.WATER || block == Material.LAVA || block == Material.LADDER)) {
					this.cancel();
					return;
				}
				mTickOne = false;
			}

		}.runTaskTimer(mPlugin, 0, 1));
	}

	//Cancel melee damage within 10 ticks of casting
	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (source != null
			    && (event.getType() == DamageEvent.DamageType.MELEE)
			    && mIsWalking) {
			event.setCancelled(true);
		}
	}


	private static String getDescription(int rarity) {
		return "Right click while sneaking to dash in the target direction, applying " + DepthsUtils.getRarityColor(rarity) + DepthsUtils.roundPercent(VULNERABILITY[rarity - 1]) + "%" + ChatColor.WHITE + " vulnerability for 5 seconds to all enemies dashed through, and apply Levitation I to non-elites dashed through for 2 seconds. Gain immunity to melee damage for half a second when triggered. Cooldown: " + DepthsUtils.getRarityColor(rarity) + (COOLDOWN[rarity - 1] / 20) + "s" + ChatColor.WHITE + ".";
	}
}
