package com.playmonumenta.plugins.depths.abilities.steelsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class SteelStallion extends DepthsAbility {
	public static final String ABILITY_NAME = "Steel Stallion";
	public static final int COOLDOWN = 90 * 20;
	private static final double TRIGGER_HEALTH = 0.25;
	public static final int[] HEALTH = {60, 70, 80, 90, 100, 120};
	public static final double[] SPEED = {.2, .24, .28, .32, .36, .44};
	public static final double[] JUMP_STRENGTH = {.5, .6, .7, .8, .9, 1.3};
	public static final int[] DURATION = {10 * 20, 11 * 20, 12 * 20, 13 * 20, 14 * 20, 18 * 20};
	public static final int TICK_INTERVAL = 5;

	public static final String CHARM_COOLDOWN = "Steel Stallion Cooldown";

	public static final DepthsAbilityInfo<SteelStallion> INFO =
		new DepthsAbilityInfo<>(SteelStallion.class, ABILITY_NAME, SteelStallion::new, DepthsTree.STEELSAGE, DepthsTrigger.LIFELINE)
			.linkedSpell(ClassAbility.STEEL_STALLION)
			.cooldown(CHARM_COOLDOWN, COOLDOWN)
			.displayItem(Material.IRON_HORSE_ARMOR)
			.descriptions(SteelStallion::getDescription)
			.priorityAmount(10000);

	private final double mHealth;
	private final double mJumpStrength;
	private final double mSpeed;
	private final int mDuration;

	private @Nullable Horse mHorse;

	public SteelStallion(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mHealth = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.STEEL_STALLION_HEALTH.mEffectName, HEALTH[mRarity - 1]);
		mJumpStrength = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.STEEL_STALLION_JUMP_STRENGTH.mEffectName, JUMP_STRENGTH[mRarity - 1]);
		mSpeed = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.STEEL_STALLION_HORSE_SPEED.mEffectName, SPEED[mRarity - 1]);
		mDuration = CharmManager.getDuration(mPlayer, CharmEffects.STEEL_STALLION_DURATION.mEffectName, DURATION[mRarity - 1]);
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.isBlocked() || event.getType() == DamageEvent.DamageType.TRUE) {
			return;
		}

		if (mHorse != null) {
			PercentDamageReceived effect = mPlugin.mEffectManager.getActiveEffect(mPlayer, PercentDamageReceived.class);
			if (effect == null || effect.isDebuff() || (effect.isBuff() && effect.getMagnitude() < 1.0)) {
				// Only hurt horse if the player doesn't have +100% resistance
				mHorse.setHealth(Math.max(0, mHorse.getHealth() - event.getFinalDamage(false)));
				mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_HORSE_HURT, SoundCategory.NEUTRAL, 0.8f, 1.0f);
			}
			event.setDamage(0);
			event.setCancelled(true);
			return;
		}

		execute(event);
	}

	@Override
	public void onHurtFatal(DamageEvent event) {
		onHurt(event, null, null);
	}

	private void execute(DamageEvent event) {
		if (isOnCooldown()) {
			return;
		}

		// Calculate whether this effect should not be run based on player health.
		double healthRemaining = mPlayer.getHealth() - event.getFinalDamage(true);

		if (healthRemaining > EntityUtils.getMaxHealth(mPlayer) * TRIGGER_HEALTH) {
			return;
		}

		Location loc = mPlayer.getLocation();
		Entity e = LibraryOfSoulsIntegration.summon(loc, "SteelStallion");
		if (e instanceof Horse horse) {
			horse.addPassenger(mPlayer);
			mHorse = horse;
			EntityUtils.setAttributeBase(mHorse, Attribute.GENERIC_MAX_HEALTH, mHealth * DepthsUtils.getDamageMultiplier());
			EntityUtils.setAttributeBase(mHorse, Attribute.HORSE_JUMP_STRENGTH, mJumpStrength);
			EntityUtils.setAttributeBase(mHorse, Attribute.GENERIC_MOVEMENT_SPEED, mSpeed);
			//Horse absorbs the damage from the hit that triggers it
			mHorse.setHealth(Math.max(0, EntityUtils.getMaxHealth(mHorse) - event.getFinalDamage(false)));
			mHorse.setInvulnerable(true);
			event.setDamage(0);
			event.setCancelled(true);
			putOnCooldown();
		}

		new BukkitRunnable() {
			int mTicksElapsed = 0;
			@Override
			public void run() {
				boolean isOutOfTime = mTicksElapsed >= mDuration;
				if (isOutOfTime || mHorse == null || mHorse.getHealth() <= 0 || mHorse.getPassengers().size() == 0) {
					if (isOutOfTime && mHorse != null) {
						Location horseLoc = mHorse.getLocation();
						World world = horseLoc.getWorld();
						world.playSound(horseLoc, Sound.ENTITY_HORSE_DEATH, SoundCategory.NEUTRAL, 0.8f, 1.0f);
						new PartialParticle(Particle.CAMPFIRE_COSY_SMOKE, horseLoc, 15).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.SMOKE_NORMAL, horseLoc, 20).spawnAsPlayerActive(mPlayer);
					}

					if (mHorse != null) {
						mHorse.remove();
						mHorse = null;
					}
					this.cancel();
				}
				mTicksElapsed += TICK_INTERVAL;
			}
		}.runTaskTimer(mPlugin, 0, TICK_INTERVAL);

		World world = mPlayer.getWorld();
		new PartialParticle(Particle.HEART, loc, 10, 2, 2, 2).spawnAsPlayerActive(mPlayer);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.NEUTRAL, 1, 1);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.NEUTRAL, 1, 0.5f);

		sendActionBarMessage("Steel Stallion has been activated!");
	}

	@Override
	public void playerQuitEvent(PlayerQuitEvent event) {
		if (mHorse != null) {
			mHorse.remove();
			mHorse = null;
		}
	}

	public static boolean isSteelStallion(Entity entity) {
		return entity instanceof Horse && ABILITY_NAME.equals(entity.getName());
	}

	private static Description<SteelStallion> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<SteelStallion>(color)
			.add("When your health drops below ")
			.addPercent(TRIGGER_HEALTH)
			.add(", summon and ride a horse with ")
			.addDepthsDamage(a -> a.mHealth, HEALTH[rarity - 1], true)
			.add(" health that disappears after ")
			.addDuration(a -> a.mDuration, DURATION[rarity - 1], false, true)
			.add(" seconds. While you are riding the horse, all damage you receive is redirected to the horse, including the damage that triggered this ability. The horse has a speed of ")
			.add(a -> a.mSpeed, SPEED[rarity - 1], false, null, true)
			.add(" and a jump strength of ")
			.add(a -> a.mJumpStrength, JUMP_STRENGTH[rarity - 1], false, null, true)
			.add(".")
			.addCooldown(COOLDOWN);
	}


}
