package com.playmonumenta.plugins.depths.abilities.windwalker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.Iterator;
import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
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
	private static final int STUN_DURATION = 30;
	private static final int WIND_WALK_RADIUS = 3;
	public static final double WIND_WALK_Y_VELOCITY = 0.2;
	public static final double WIND_WALK_Y_VELOCITY_MULTIPLIER = 0.2;
	public static final double WIND_WALK_VELOCITY_BONUS = 1.5;

	public static final String CHARM_COOLDOWN = "Wind Walk Cooldown";

	public static final DepthsAbilityInfo<DepthsWindWalk> INFO =
		new DepthsAbilityInfo<>(DepthsWindWalk.class, "Wind Walk", DepthsWindWalk::new, DepthsTree.WINDWALKER, DepthsTrigger.RIGHT_CLICK)
			.linkedSpell(ClassAbility.WIND_WALK_DEPTHS)
			.cooldown(CHARM_COOLDOWN, COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", DepthsWindWalk::cast, DepthsTrigger.RIGHT_CLICK))
			.displayItem(Material.WHITE_DYE)
			.descriptions(DepthsWindWalk::getDescription);

	private final double mVuln;
	private final int mDuration;
	private final int mLevitationDuration;
	private final int mStunDuration;

	private boolean mIsWalking = false;

	public DepthsWindWalk(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mVuln = VULNERABILITY[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.WIND_WALK_VULNERABILITY_AMPLIFIER.mEffectName);
		mDuration = CharmManager.getDuration(mPlayer, CharmEffects.WIND_WALK_VULNERABILITY_DURATION.mEffectName, VULN_DURATION);
		mLevitationDuration = CharmManager.getDuration(mPlayer, CharmEffects.WIND_WALK_LEVITATION_DURATION.mEffectName, LEVITATION_DURATION);
		mStunDuration = CharmManager.getDuration(mPlayer, CharmEffects.WIND_WALK_STUN_DURATION.mEffectName, STUN_DURATION);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		Location loc = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1, 1.75f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 1, 1f);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 90, 0.25, 0.45, 0.25, 0.1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, loc, 20, 0.25, 0.45, 0.25, 0.15).spawnAsPlayerActive(mPlayer);
		Vector direction = loc.getDirection();
		Vector yVelocity = new Vector(0, direction.getY() * WIND_WALK_Y_VELOCITY_MULTIPLIER + WIND_WALK_Y_VELOCITY, 0);
		mPlayer.setVelocity(direction.multiply(CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.WIND_WALK_VELOCITY.mEffectName, WIND_WALK_VELOCITY_BONUS)).add(yVelocity));
		// Have them dodge melee attacks while casting
		mIsWalking = true;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mIsWalking = false, 10);

		cancelOnDeath(new BukkitRunnable() {
			final List<LivingEntity> mMobsNotHit = EntityUtils.getNearbyMobs(mPlayer.getLocation(), 32);
			boolean mTickOne = true;
			int mTicks = 0;

			@Override
			public void run() {
				if (mPlayer.isDead() || !mPlayer.isOnline() || !mPlayer.getLocation().isChunkLoaded()) {
					this.cancel();
					return;
				}

				new PartialParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation().add(0, 0.5, 0), (int) (7/Math.pow(1.1, mTicks)), 0.15, 0.45, 0.15, 0).spawnAsPlayerPassive(mPlayer);

				Iterator<LivingEntity> iter = mMobsNotHit.iterator();
				while (iter.hasNext()) {
					LivingEntity mob = iter.next();

					if (mob.getLocation().distance(mPlayer.getLocation()) < WIND_WALK_RADIUS) {
						if (!EntityUtils.isBoss(mob)) {
							new PartialParticle(Particle.SWEEP_ATTACK, mob.getLocation().add(0, 1, 0), 16, 0.5, 0.5, 0.5, 0).spawnAsPlayerActive(mPlayer);
							new PartialParticle(Particle.CLOUD, mob.getLocation().add(0, 1, 0), 20, 0.25, 0.45, 0.25, 0.1).spawnAsPlayerActive(mPlayer);
							world.playSound(mob.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.75f, 1.25f);

							EntityUtils.applyVulnerability(mPlugin, mDuration, mVuln, mob);

							if (!EntityUtils.isCCImmuneMob(mob)) {
								mob.setVelocity(mob.getVelocity().setY(0.5));
								PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.LEVITATION, mLevitationDuration, 0, true, false));
								EntityUtils.applyStun(mPlugin, mStunDuration, mob);
							}
						}

						iter.remove();
					}
					mTicks++;
				}

				Material block = mPlayer.getLocation().getBlock().getType();
				if (!mTickOne && (PlayerUtils.isOnGround(mPlayer) || block == Material.WATER || block == Material.LAVA || block == Material.LADDER)) {
					this.cancel();
					return;
				}
				mTickOne = false;
			}

		}.runTaskTimer(mPlugin, 0, 1));

		return true;
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


	private static Description<DepthsWindWalk> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<DepthsWindWalk>(color)
			.add("Right click to dash in the target direction, applying ")
			.addPercent(a -> a.mVuln, VULNERABILITY[rarity - 1], false, true)
			.add(" vulnerability for ")
			.addDuration(a -> a.mDuration, VULN_DURATION)
			.add(" seconds, Levitation I for ")
			.addDuration(a -> a.mLevitationDuration, LEVITATION_DURATION)
			.add(" seconds, and stun for ")
			.addDuration(a -> a.mStunDuration, STUN_DURATION)
			.add(" seconds to all non-Boss enemies dashed through. Gain immunity to melee damage for 0.5s when triggered.")
			.addCooldown(COOLDOWN[rarity - 1], true);
	}
}
