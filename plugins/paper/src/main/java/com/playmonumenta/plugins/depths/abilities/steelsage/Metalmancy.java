package com.playmonumenta.plugins.depths.abilities.steelsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.bosses.bosses.abilities.MetalmancyBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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

	public static final DepthsAbilityInfo<Metalmancy> INFO =
		new DepthsAbilityInfo<>(Metalmancy.class, ABILITY_NAME, Metalmancy::new, DepthsTree.STEELSAGE, DepthsTrigger.SWAP)
			.linkedSpell(ClassAbility.METALMANCY)
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Metalmancy::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP), HOLDING_WEAPON_RESTRICTION))
			.displayItem(new ItemStack(Material.IRON_BLOCK))
			.descriptions(Metalmancy::getDescription);

	private @Nullable Mob mGolem;
	private @Nullable LivingEntity mTarget;

	public Metalmancy(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public void cast() {
		if (isOnCooldown()) {
			resetTarget();
			return;
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
			return;
		}
		mGolem.setVelocity(facingDirection.multiply(VELOCITY));

		MetalmancyBoss metalmancyBoss = BossUtils.getBossOfClass(mGolem, MetalmancyBoss.class);
		if (metalmancyBoss == null) {
			MMLog.warning("Failed to get MetalamcnyBoss");
			return;
		}

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
		metalmancyBoss.spawn(mPlayer, DAMAGE[mRarity - 1], playerItemStats);

		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.NEUTRAL, 1.0f, 1.0f);
		world.playSound(loc, Sound.BLOCK_CHAIN_BREAK, SoundCategory.NEUTRAL, 1.0f, 1.0f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.NEUTRAL, 1.0f, 1.0f);

		new BukkitRunnable() {
			int mTicksElapsed = 0;

			@Override
			public void run() {
				boolean isOutOfTime = mTicksElapsed >= DURATION[mRarity - 1];
				if (isOutOfTime || mGolem == null) {
					if (isOutOfTime && mGolem != null) {
						Location golemLoc = mGolem.getLocation();
						world.playSound(golemLoc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.NEUTRAL, 0.8f, 1.0f);
						new PartialParticle(Particle.CAMPFIRE_COSY_SMOKE, golemLoc, 15).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.SMOKE_NORMAL, golemLoc, 20).spawnAsPlayerActive(mPlayer);
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
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() != DamageType.PROJECTILE || mGolem == null || mGolem.getHealth() <= 0 || !(mTarget == null || mTarget.getHealth() <= 0) || enemy.getLocation().distance(mGolem.getLocation()) > DETECTION_RANGE) {
			return false;
		}

		mTarget = enemy;
		mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.NEUTRAL, 1.0f, 0.5f);
		PotionUtils.applyPotion(mPlayer, mTarget, new PotionEffect(PotionEffectType.GLOWING, DURATION[mRarity - 1], 0, true, false));
		new PartialParticle(Particle.VILLAGER_ANGRY, mGolem.getEyeLocation(), 15).spawnAsPlayerActive(mPlayer);
		return true; // only one retarget per tick
	}

	@Override
	public void playerQuitEvent(PlayerQuitEvent event) {
		if (mGolem != null) {
			mGolem.remove();
			mGolem = null;
		}
	}

	private void resetTarget() {
		if (mTarget != null) {
			mTarget.removePotionEffect(PotionEffectType.GLOWING);
			mTarget = null;
		}
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("Swap hands while holding a weapon to summon an invulnerable steel construct. The Construct attacks the nearest mob within " + DETECTION_RANGE + " blocks. The Construct prioritizes the first enemy you hit with a projectile after summoning, which can be reapplied once that target dies. The Construct deals ")
			.append(Component.text(StringUtils.to2DP(DAMAGE[rarity - 1]), color))
			.append(Component.text(" projectile damage and taunts non-boss enemies it hits. The Construct disappears after "))
			.append(Component.text(DURATION[rarity - 1] / 20, color))
			.append(Component.text(" seconds. Triggering while on cooldown will clear the specified target. Cooldown: " + COOLDOWN / 20 + "s."));
	}


}
