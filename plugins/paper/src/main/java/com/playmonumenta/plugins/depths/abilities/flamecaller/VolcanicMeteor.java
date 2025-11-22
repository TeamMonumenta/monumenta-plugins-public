package com.playmonumenta.plugins.depths.abilities.flamecaller;

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
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class VolcanicMeteor extends DepthsAbility {
	public static final String ABILITY_NAME = "Volcanic Meteor";
	public static final int[] DAMAGE = {36, 45, 54, 63, 72, 90};
	public static final int COOLDOWN_TICKS = 25 * 20;
	public static final int DISTANCE = 25;
	public static final int SIZE = 5;
	public static final int FIRE_TICKS = 3 * 20;

	public static final String CHARM_COOLDOWN = "Volcanic Meteor Cooldown";

	public static final DepthsAbilityInfo<VolcanicMeteor> INFO =
		new DepthsAbilityInfo<>(VolcanicMeteor.class, ABILITY_NAME, VolcanicMeteor::new, DepthsTree.FLAMECALLER, DepthsTrigger.SWAP)
			.linkedSpell(ClassAbility.VOLCANIC_METEOR)
			.cooldown(COOLDOWN_TICKS, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", VolcanicMeteor::cast, DepthsTrigger.SWAP))
			.displayItem(Material.MAGMA_BLOCK)
			.descriptions(VolcanicMeteor::getDescription);

	private final double mRadius;
	private final double mDamage;
	private final int mFireDuration;

	public VolcanicMeteor(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.VOLCANIC_METEOR_RADIUS.mEffectName, SIZE);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.VOLCANIC_METEOR_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mFireDuration = CharmManager.getDuration(mPlayer, CharmEffects.VOLCANIC_METEOR_FIRE_DURATION.mEffectName, FIRE_TICKS);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		Location loc = mPlayer.getEyeLocation();
		World world = mPlayer.getWorld();
		Location playerLoc = mPlayer.getLocation();
		world.playSound(playerLoc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 2.0f, 1.0f);
		world.playSound(playerLoc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 0.4f, 2.0f);
		new PartialParticle(Particle.LAVA, playerLoc, 15, 0.25f, 0.1f, 0.25f).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FLAME, playerLoc, 30, 0.25f, 0.1f, 0.25f, 0.15f).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SOUL_FIRE_FLAME, playerLoc, 30, 0.25f, 0.1f, 0.25f, 0.15f).spawnAsPlayerActive(mPlayer);
		Vector dir = loc.getDirection().normalize();
		for (int i = 0; i < DISTANCE; i++) {
			loc.add(dir);

			new PartialParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0).spawnAsPlayerActive(mPlayer);
			int size = EntityUtils.getNearbyMobs(loc, 2, mPlayer).size();
			if (loc.getBlock().getType().isSolid() || i == 24 || size > 0) {
				launchMeteor(loc, playerItemStats);
				break;
			}
		}
		return true;
	}


	private void launchMeteor(final Location loc, final ItemStatManager.PlayerItemStats playerItemStats) {
		Location ogLoc = loc.clone();
		loc.add(0, 30, 0);
		new BukkitRunnable() {
			double mT = 0;

			@Override
			public void run() {
				mT += 1;
				World world = mPlayer.getWorld();
				for (int i = 0; i < 8; i++) {
					loc.subtract(0, 0.2, 0);
					if (loc.getBlock().getType().isSolid()) {
						if (loc.getY() - ogLoc.getY() <= 2) {
							world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.PLAYERS, 1.0f, 0.1f);
							world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 0.6f, 2.0f);
							world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 2.0f, 0.1f);
							world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 2.0f, 0.1f);
							world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 2.0f, 0.1f);

							new PartialParticle(Particle.FLAME, loc, 175, 0, 0, 0, 0.235F).spawnAsPlayerActive(mPlayer);
							new PartialParticle(Particle.SOUL_FIRE_FLAME, loc, 175, 0, 0, 0, 0.235F).spawnAsPlayerActive(mPlayer);

							new PartialParticle(Particle.SMOKE_LARGE, loc, 75, 0.25, 0.25, 0.25, 0.2F).spawnAsPlayerActive(mPlayer);
							new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 75, 0.25, 0.25, 0.25, 0.2F).spawnAsPlayerActive(mPlayer);
							this.cancel();

							for (LivingEntity e : EntityUtils.getNearbyMobs(loc, mRadius, mPlayer)) {
								double distance = loc.distance(e.getLocation());
								double multiplier = Math.min(Math.max(0.5, (8 - (5 / mRadius * distance)) / 6), 1);

								EntityUtils.applyFire(mPlugin, mFireDuration, e, mPlayer, playerItemStats);
								DamageUtils.damage(mPlayer, e, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), mDamage * multiplier, true, true, false);
							}
							break;
						}
					}
				}
				world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1, 1);
				new PartialParticle(Particle.FLAME, loc, 25, 0.25F, 0.25F, 0.25F, 0.1F).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.SOUL_FIRE_FLAME, loc, 25, 0.25F, 0.25F, 0.25F, 0.1F).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.SMOKE_LARGE, loc, 5, 0.25F, 0.25F, 0.25F, 0.1F).spawnAsPlayerActive(mPlayer);

				if (mT >= 150) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private static Description<VolcanicMeteor> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<>(() -> INFO, color)
			.addTrigger()
			.add(" to summon a falling meteor at the location where you are looking, up to ")
			.add(DISTANCE)
			.add(" blocks away. When the meteor lands, it deals ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" magic damage in a ")
			.add(a -> a.mRadius, SIZE)
			.add(" block radius and applies fire for ")
			.addDuration(a -> a.mFireDuration, FIRE_TICKS)
			.add(" seconds. The damage is reduced depending on the distance from the center.")
			.addCooldown(COOLDOWN_TICKS);
	}

}

