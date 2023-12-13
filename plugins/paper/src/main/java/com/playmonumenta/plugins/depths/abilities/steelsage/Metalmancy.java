package com.playmonumenta.plugins.depths.abilities.steelsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.bosses.bosses.abilities.MetalmancyBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class Metalmancy extends DepthsAbility {

	public static final String ABILITY_NAME = "Metalmancy";

	public static final double[] DAMAGE = {10, 12.5, 15, 17.5, 20, 25};
	public static final int[] DURATION = {10 * 20, 11 * 20, 12 * 20, 13 * 20, 14 * 20, 18 * 20};
	public static final int COOLDOWN = 32 * 20;
	public static final String GOLEM_NAME = "SteelConstruct";
	public static final double VELOCITY = 2;
	public static final int DETECTION_RANGE = 32;
	public static final int TICK_INTERVAL = 5;
	private static final double MAX_TARGET_Y = 4;

	public static final String CHARM_COOLDOWN = "Metalmancy Cooldown";

	public static final DepthsAbilityInfo<Metalmancy> INFO =
		new DepthsAbilityInfo<>(Metalmancy.class, ABILITY_NAME, Metalmancy::new, DepthsTree.STEELSAGE, DepthsTrigger.SWAP)
			.linkedSpell(ClassAbility.METALMANCY)
			.cooldown(CHARM_COOLDOWN, COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Metalmancy::cast, DepthsTrigger.SWAP))
			.displayItem(Material.IRON_BLOCK)
			.descriptions(Metalmancy::getDescription);

	private final int mDuration;
	private final double mDamage;

	private @Nullable Mob mGolem;
	private @Nullable LivingEntity mTarget;

	public Metalmancy(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDuration = CharmManager.getDuration(mPlayer, CharmEffects.METALMANCY_DURATION.mEffectName, DURATION[mRarity - 1]);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.METALMANCY_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			resetTarget();
			return false;
		}

		putOnCooldown();

		if (mGolem != null) {
			mGolem.remove();
			mGolem = null;
		}

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();
		Vector facingDirection = mPlayer.getEyeLocation().getDirection().normalize();
		mGolem = (Mob) LibraryOfSoulsIntegration.summon(mPlayer.getLocation().add(facingDirection).add(0, 1, 0), GOLEM_NAME);
		if (mGolem == null) {
			return false;
		}
		mGolem.setVelocity(facingDirection.multiply(VELOCITY));

		MetalmancyBoss metalmancyBoss = BossUtils.getBossOfClass(mGolem, MetalmancyBoss.class);
		if (metalmancyBoss == null) {
			MMLog.warning("Failed to get MetalmancyBoss");
			return false;
		}

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
		metalmancyBoss.spawn(mPlayer, mDamage, playerItemStats);

		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.PLAYERS, 1.0f, 1.0f);
		world.playSound(loc, Sound.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0f, 1.0f);

		new BukkitRunnable() {
			int mTicksElapsed = 0;
			@Override
			public void run() {
				boolean isOutOfTime = mTicksElapsed >= mDuration;
				if (isOutOfTime || mGolem == null) {
					if (isOutOfTime && mGolem != null) {
						Location golemLoc = mGolem.getLocation();
						world.playSound(golemLoc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.PLAYERS, 0.8f, 1.0f);
						new PartialParticle(Particle.CAMPFIRE_COSY_SMOKE, golemLoc, 15).delta(0.5).extra(0.1).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.SMOKE_NORMAL, golemLoc, 20).delta(0.5).extra(0.1).spawnAsPlayerActive(mPlayer);
					}

					resetTarget();

					if (mGolem != null) {
						mGolem.remove();
						mGolem = null;
					}
					this.cancel();
				}

				if (!(mTarget == null || mTarget.isDead() || mTarget.getHealth() <= 0)) {
					if (mTarget == mGolem) {
						mTarget = null;
					} else if (mGolem != null) {
						mGolem.setTarget(mTarget);
					}
				}

				if (mGolem != null && (mGolem.getTarget() == null || mGolem.getTarget().isDead() || mGolem.getTarget().getHealth() <= 0) && mTicksElapsed >= TICK_INTERVAL * 2) {
					Location golemLoc = mGolem.getLocation();
					List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(golemLoc, DETECTION_RANGE, mGolem);
					nearbyMobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG) || mob.isInvulnerable());
					nearbyMobs.removeIf((mob) -> Math.abs(mob.getLocation().getY() - golemLoc.getY()) > MAX_TARGET_Y);
					LivingEntity nearestMob = EntityUtils.getNearestMob(golemLoc, nearbyMobs);
					if (nearestMob != null) {
						mGolem.setTarget(nearestMob);
					}
				}

				mTicksElapsed += TICK_INTERVAL;
			}
		}.runTaskTimer(mPlugin, 0, TICK_INTERVAL);

		return true;
	}

	@Override
	public void playerQuitEvent(PlayerQuitEvent event) {
		if (mGolem != null) {
			mGolem.remove();
			mGolem = null;
		}
	}

	private void resetTarget() {
		mTarget = null;

		LivingEntity newTarget = EntityUtils.getEntityAtCursor(mPlayer, 40, e -> EntityUtils.isHostileMob(e) && !ScoreboardUtils.checkTag(e, AbilityUtils.IGNORE_TAG) && !e.isDead() && e.isValid());
		if (newTarget != null) {
			mTarget = newTarget;
			PotionUtils.applyColoredGlowing("MetalmancyTarget", newTarget, NamedTextColor.RED, 10);
			mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.PLAYERS, 1.0f, 1.5f);
		}
	}

	private static Description<Metalmancy> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Metalmancy>(color)
			.add("Swap hands while holding a weapon to summon an invulnerable steel construct. The Construct attacks the nearest mob within ")
			.add(DETECTION_RANGE)
			.add(" blocks. The Construct deals ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" projectile damage and taunts non-boss enemies it hits. The Construct disappears after ")
			.addDuration(a -> a.mDuration, DURATION[rarity - 1], false, true)
			.add(" seconds. Triggering while on cooldown will set the Construct's target to the mob you are looking at.")
			.addCooldown(COOLDOWN);
	}


}
