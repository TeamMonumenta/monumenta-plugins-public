package com.playmonumenta.plugins.depths.abilities.frostborn;

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
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
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
import org.jetbrains.annotations.Nullable;

public class Cryobox extends DepthsAbility {

	public static final String ABILITY_NAME = "Cryobox";
	public static final int[] ABSORPTION_HEALTH = {8, 10, 12, 14, 16, 20};
	public static final int COOLDOWN = 90 * 20;
	private static final double TRIGGER_HEALTH = 0.25;
	private static final int KNOCKBACK_RADIUS = 4;
	private static final int ELEVATE_RADIUS = 2;
	private static final float KNOCKBACK_SPEED = 0.7f;
	private static final int DURATION = 12 * 20;
	private static final int ICE_DURATION = 15 * 20;
	private static final int FROZEN_DURATION = 2 * 20;

	public static final String CHARM_COOLDOWN = "Cryobox Cooldown";

	public static final DepthsAbilityInfo<Cryobox> INFO =
		new DepthsAbilityInfo<>(Cryobox.class, ABILITY_NAME, Cryobox::new, DepthsTree.FROSTBORN, DepthsTrigger.LIFELINE)
			.linkedSpell(ClassAbility.CRYOBOX)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.GHAST_TEAR)
			.descriptions(Cryobox::getDescription)
			.priorityAmount(10000);

	private final double mAbsorptionHealth;
	private final int mAbsorptionDuration;
	private final int mIceDuration;
	private final int mFrozenDuration;

	public Cryobox(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mAbsorptionHealth = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.CRYOBOX_ABSORPTION_HEALTH.mEffectName, ABSORPTION_HEALTH[mRarity - 1]);
		mAbsorptionDuration = CharmManager.getDuration(mPlayer, CharmEffects.CRYOBOX_ABSORPTION_DURATION.mEffectName, DURATION);
		mIceDuration = CharmManager.getDuration(mPlayer, CharmEffects.CRYOBOX_ICE_DURATION.mEffectName, ICE_DURATION);
		mFrozenDuration = CharmManager.getDuration(mPlayer, CharmEffects.CRYOBOX_FROZEN_DURATION.mEffectName, FROZEN_DURATION);
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.isBlocked() || isOnCooldown() || event.getType() == DamageEvent.DamageType.TRUE) {
			return;
		}

		// Calculate whether this effect should not be run based on player health.
		// It is intentional that Cryobox saves you from death if you take a buttload of damage somehow.
		double healthRemaining = mPlayer.getHealth() - event.getFinalDamage(true);

		// Health is less than 0 but does not penetrate the absorption shield
		boolean dealDamageLater = healthRemaining < 0 && healthRemaining > -mAbsorptionHealth;

		if (healthRemaining > EntityUtils.getMaxHealth(mPlayer) * TRIGGER_HEALTH) {
			return;
		} else if (dealDamageLater) {
			// The player has taken fatal damage BUT will be saved by the absorption, so set damage to 0 and compensate later
			event.setCancelled(true);
		}

		// Put on cooldown before processing results to prevent infinite recursion
		putOnCooldown();

		Location center = mPlayer.getLocation();

		// Conditions match - prismatic shield
		for (LivingEntity mob : EntityUtils.getNearbyMobs(center, KNOCKBACK_RADIUS, mPlayer)) {
			MovementUtils.knockAway(mPlayer, mob, KNOCKBACK_SPEED, true);
			EntityUtils.disableAI(mPlugin, mob, mFrozenDuration);
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> EntityUtils.disableGravity(mPlugin, mob, mFrozenDuration - 5), 5);
		}
		for (LivingEntity mob : EntityUtils.getNearbyMobs(center, ELEVATE_RADIUS, mPlayer)) {
			if (EntityUtils.isCCImmuneMob(mob) || ScoreboardUtils.checkTag(mob, AbilityUtils.IGNORE_TAG) || mob.getVehicle() != null) {
				continue;
			}
			Location mobLoc = mob.getLocation();
			mobLoc.setY(center.getY() + 3);
			EntityUtils.teleportStack(mob, mobLoc);
		}

		AbsorptionUtils.addAbsorption(mPlayer, mAbsorptionHealth, mAbsorptionHealth, mAbsorptionDuration);
		World world = mPlayer.getWorld();
		new PartialParticle(Particle.FIREWORKS_SPARK, center.clone().add(0, 1.15, 0), 150, 0.2, 0.35, 0.2, 0.5).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SPELL_INSTANT, center.clone().add(0, 1.15, 0), 100, 0.2, 0.35, 0.2, 1).spawnAsPlayerActive(mPlayer);
		world.playSound(center, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.5f, 1.8f);
		world.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 0.8f, 2.0f);
		world.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 0.8f, 1.4f);
		world.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 0.8f, 1.0f);
		world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 2.0f, 1.6f);
		world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.4f, 1.4f);
		world.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 2.0f, 0.5f);
		world.playSound(center, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.0f, 1.4f);
		world.playSound(center, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.0f, 0.1f);
		world.playSound(center, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 0.3f, 2.0f);
		sendActionBarMessage("Cryobox has been activated!");

		if (dealDamageLater) {
			mPlayer.setHealth(1);
			AbsorptionUtils.subtractAbsorption(mPlayer, 1 - (float) healthRemaining);
		}
		if (!ZoneUtils.hasZoneProperty(mPlayer.getLocation(), ZoneUtils.ZoneProperty.ADVENTURE_MODE)) {
			//Ripped this straight from frost giant, epic
			Location[] locs = new Location[] {
				//First Layer
				center.clone().add(1, 0, 0),
				center.clone().add(-1, 0, 0),
				center.clone().add(0, 0, 1),
				center.clone().add(0, 0, -1),

				//Second Layer
				center.clone().add(1, 1, 0),
				center.clone().add(-1, 1, 0),
				center.clone().add(0, 1, 1),
				center.clone().add(0, 1, -1),

				//Top & Bottom
				center.clone().add(0, 2, 0),
				center.clone().add(0, -1, 0),
				};

			for (Location loc : locs) {
				DepthsUtils.spawnIceTerrain(loc, mIceDuration, mPlayer);
			}
		}

	}

	@Override
	public void onHurtFatal(DamageEvent event) {
		onHurt(event, null, null);
	}

	private static Description<Cryobox> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Cryobox>(color)
			.add("When your health drops below ")
			.addPercent(TRIGGER_HEALTH)
			.add(", gain ")
			.add(a -> a.mAbsorptionHealth, ABSORPTION_HEALTH[rarity - 1], false, null, true)
			.add(" absorption health for ")
			.addDuration(a -> a.mAbsorptionDuration, DURATION)
			.add(" seconds and encase yourself in a cage of ice for ")
			.addDuration(a -> a.mIceDuration, ICE_DURATION)
			.add(" seconds. Mobs within ")
			.add(KNOCKBACK_RADIUS)
			.add(" blocks will be knocked back and frozen for ")
			.addDuration(a -> a.mFrozenDuration, FROZEN_DURATION)
			.add(" seconds.")
			.addCooldown(COOLDOWN);
	}


}

