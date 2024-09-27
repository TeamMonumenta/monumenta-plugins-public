package com.playmonumenta.plugins.depths.abilities.prismatic;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import static java.awt.Color.HSBtoRGB;

public class Refraction extends DepthsAbility implements AbilityWithDuration {
	public static final String ABILITY_NAME = "Refraction";
	public static final double[] DAMAGE = {5, 5.75, 6.5, 7.25, 8, 9.5};
	public static final int COOLDOWN_TICKS = 20 * 20;
	public static final int DISTANCE = 40;
	public static final int DURATION = (int) (3.5 * 20);
	public static final int EFFECT_DURATION = 3 * 20;
	public static final int BUFF_DURATION = 10 * 20;

	public int mCurrDuration = -1;

	public static final DepthsAbilityInfo<Refraction> INFO =
		new DepthsAbilityInfo<>(Refraction.class, ABILITY_NAME, Refraction::new, DepthsTree.PRISMATIC, DepthsTrigger.SWAP)
			.linkedSpell(ClassAbility.REFRACTION)
			.cooldown(COOLDOWN_TICKS)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Refraction::cast, DepthsTrigger.SWAP))
			.displayItem(Material.SPYGLASS)
			.descriptions(Refraction::getDescription);

	public Refraction(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		DepthsPlayer dPlayer = DepthsManager.getInstance().getDepthsPlayer(mPlayer);
		if (dPlayer == null) {
			return false;
		}

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		Location loc = mPlayer.getEyeLocation();
		Location l = loc.clone().add(loc.getDirection());
		new BukkitRunnable() {
			double mPitch = 0;
			double mRadiusDecrement = 0;
			int mT = 0;

			@Override
			public void run() {
				World world = mPlayer.getWorld();

				mT += 1;
				mPitch += 0.05;
				float initialRotation = loc.getYaw() + 180;
				double maxRotPerTick = 9;
				double currentRotPerTick = maxRotPerTick * (30 - mT) / 30;
				double rotation = maxRotPerTick * 30 / 2 - currentRotPerTick * (30 - mT) / 2;
				double radius = 1.25;

				if (mT < 30) {
					mRadiusDecrement += (radius / 30);
					Vector y = VectorUtils.rotationToVector(l.getYaw(), l.getPitch() - 90);
					Vector x = y.clone().crossProduct(l.getDirection());
					new PPCircle(Particle.ELECTRIC_SPARK, l, radius - mRadiusDecrement)
						.axes(y, x)
						.arcDegree(initialRotation + rotation, initialRotation + rotation + 360)
						.count(5)
						.directionalMode(true)
						.rotateDelta(true)
						.delta(-0.2, 0, 1)
						.extra(Math.toRadians(currentRotPerTick) * radius * 1.4)
						.spawnAsPlayerActive(mPlayer);
					new PPCircle(Particle.ENCHANTMENT_TABLE, l, radius - mRadiusDecrement)
						.axes(y, x)
						.arcDegree(initialRotation + rotation, initialRotation + rotation + 360)
						.count(5)
						.directionalMode(true)
						.rotateDelta(true)
						.delta(-0.2, 0, 1)
						.extra(Math.toRadians(currentRotPerTick) * radius * 1.4)
						.spawnAsPlayerActive(mPlayer);

					new PPCircle(Particle.ENCHANTMENT_TABLE, l, radius)
						.axes(y, x)
						.arcDegree(initialRotation + rotation, initialRotation + rotation + 360)
						.count(5)
						.directionalMode(true)
						.rotateDelta(true)
						.delta(-0.2, 0, 1)
						.extra(Math.toRadians(currentRotPerTick) * radius * 1.4)
						.spawnAsPlayerActive(mPlayer);

				}

				world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 1.5f, 0.5f + (float) mPitch);
				if (mT >= 30) {
					this.cancel();
					pulseLaser(playerItemStats, dPlayer, loc);
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		return true;
	}

	private void pulseLaser(ItemStatManager.PlayerItemStats playerItemStats, DepthsPlayer depthsPlayer, Location loc) {
		mCurrDuration = 0;

		new BukkitRunnable() {
			float mHue = 0.0f;
			double mAngle = 0.5;

			@Override
			public void run() {
				World world = mPlayer.getWorld();

				mCurrDuration++;
				mAngle += 0.5;
				mHue = 0.0f;
				Vector dir = loc.getDirection().normalize();
				Location endLoc = LocationUtils.rayTraceToBlock(loc, loc.getDirection(), DISTANCE, (hitBlockLoc) -> {
					if (mCurrDuration % 5 == 0) {
						new PartialParticle(Particle.FIREWORKS_SPARK, hitBlockLoc, 10, 0.1, 0.1, 0.1, 0.3).spawnAsPlayerActive(mPlayer);
						world.playSound(hitBlockLoc, Sound.ENTITY_ENDER_EYE_DEATH, SoundCategory.PLAYERS, 1, 1.5f);
					}
				});

				world.playSound(mPlayer.getEyeLocation(), Sound.BLOCK_CONDUIT_ACTIVATE, SoundCategory.PLAYERS, 0.8f, 2f);
				world.playSound(mPlayer.getEyeLocation(), Sound.ENTITY_GUARDIAN_ATTACK, SoundCategory.PLAYERS, 0.5f, 2f);

				Location adjustedLoc = loc.clone();
				adjustedLoc.setPitch(adjustedLoc.getPitch() + 90);
				Vector vec = adjustedLoc.getDirection();
				vec = vec.clone().rotateAroundAxis(dir, (-Math.PI / 24) * mAngle);
				vec = vec.normalize().multiply(0.75);

				Location currLoc = loc.clone();

				// every other tick, play particles in the gaps to create a fuller beam
				if (mCurrDuration % 2 == 0) {
					currLoc.add(dir.clone().multiply(0.25));
					vec.rotateAroundAxis(dir, Math.PI / 24);
				}
				Vector dir2 = dir.clone().multiply(0.5);

				PartialParticle spark1 = new PartialParticle(Particle.ELECTRIC_SPARK, currLoc).count(1).extra(0.10).delta(0.10);
				PartialParticle spark2 = new PartialParticle(Particle.ELECTRIC_SPARK, currLoc.clone().add(dir2)).count(1).extra(0.15).delta(0.15);
				PartialParticle redstone = new PartialParticle(Particle.REDSTONE, currLoc.clone().add(vec)).count(1);

				for (int i = 0; i < DISTANCE; i++) {
					Vector vec2 = vec.clone().rotateAroundAxis(dir, Math.PI / 12);

					currLoc.add(dir);

					Particle.DustOptions color = new Particle.DustOptions(Color.fromARGB(HSBtoRGB(mHue, 0.4f, 1.0f)), 1f);
					spark1.location(currLoc).spawnAsPlayerActive(mPlayer);
					spark2.location(currLoc.clone().add(dir2)).spawnAsPlayerActive(mPlayer);
					redstone.location(currLoc.clone().add(vec)).data(color).spawnAsPlayerActive(mPlayer);
					redstone.location(currLoc.clone().subtract(vec)).data(color).spawnAsPlayerActive(mPlayer);
					redstone.location(currLoc.clone().add(dir2).add(vec2)).data(color).spawnAsPlayerActive(mPlayer);
					redstone.location(currLoc.clone().add(dir2).subtract(vec2)).data(color).spawnAsPlayerActive(mPlayer);

					mHue += 0.05f;

					if (mHue >= 1) {
						mHue = 0.0f;
					}


					Block block = currLoc.getBlock();
					if (block.getType().isSolid() || block.getType() == Material.WATER) {
						break;
					}

					vec.rotateAroundAxis(dir, Math.PI / 6);
				}

				Hitbox hitbox = Hitbox.approximateCylinder(loc, endLoc, 1.25, true).accuracy(0.5);

				if (mCurrDuration % 5 == 0) {
					List<LivingEntity> hitMobs = hitbox.getHitMobs();
					List<Player> hitPlayers = hitbox.getHitPlayers(mPlayer, true);

					for (LivingEntity mob : hitMobs) {
						world.playSound(mob.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.PLAYERS, 0.8f, 2f);
						new PartialParticle(Particle.CRIT_MAGIC, mob.getLocation().add(0, 1, 0), 15, 0.1, 0.2, 0.1, 0.15).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.SPELL_INSTANT, mob.getLocation().add(0, 1, 0), 20, 0.1, 0.2, 0.1, 0.15).spawnAsPlayerActive(mPlayer);
						DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), DAMAGE[mRarity - 1], true, false, false);
						if (depthsPlayer.mEligibleTrees.contains(DepthsTree.FLAMECALLER)) {
							EntityUtils.applyFire(mPlugin, EFFECT_DURATION, mob, mPlayer, playerItemStats);
						}
						if (depthsPlayer.mEligibleTrees.contains(DepthsTree.FROSTBORN)) {
							EntityUtils.applySlow(mPlugin, EFFECT_DURATION, 0.1, mob);
						}
						if (depthsPlayer.mEligibleTrees.contains(DepthsTree.SHADOWDANCER)) {
							EntityUtils.applyVulnerability(mPlugin, EFFECT_DURATION, 0.1, mob);
						}
						if (depthsPlayer.mEligibleTrees.contains(DepthsTree.STEELSAGE)) {
							EntityUtils.applyBleed(mPlugin, EFFECT_DURATION, 0.1, mob);
						}
					}

					for (Player player : hitPlayers) {
						new BukkitRunnable() {
							int mT = 0;
							float mPitch = 1.6f;

							@Override
							public void run() {
								world.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1, mPitch);
								if (mT > 2) {
									this.cancel();
								}
								mT += 1;
								mPitch += 0.2f;
							}
						}.runTaskTimer(mPlugin, 0, 1);
						new PartialParticle(Particle.VILLAGER_HAPPY, player.getLocation().add(0, 1, 0), 10, 0.5, 0.2, 0.5, 1).spawnAsPlayerActive(mPlayer);
						if (depthsPlayer.mEligibleTrees.contains(DepthsTree.DAWNBRINGER)) {
							PlayerUtils.healPlayer(mPlugin, player, EntityUtils.getMaxHealth(player) * 0.05, mPlayer);
						}
						if (depthsPlayer.mEligibleTrees.contains(DepthsTree.EARTHBOUND)) {
							mPlugin.mEffectManager.addEffect(player, "REFRACTION_EARTHBOUND_RESIST", new PercentDamageReceived(BUFF_DURATION, -0.1));
						}
						if (depthsPlayer.mEligibleTrees.contains(DepthsTree.WINDWALKER)) {
							mPlugin.mEffectManager.addEffect(player, "REFRACTION_WINDWALKER_SPEED", new PercentSpeed(BUFF_DURATION, 0.1, "Refraction Windwalker Speed"));
						}
					}
				}

				if (mCurrDuration >= DURATION) {
					this.cancel();
					world.playSound(mPlayer.getEyeLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 1.2f, 1f);
				}
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				mCurrDuration = -1;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int getInitialAbilityDuration() {
		return DURATION;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return mCurrDuration >= 0 ? getInitialAbilityDuration() - mCurrDuration : 0;
	}

	private static Description<Refraction> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Refraction>(color)
			.add("Swap hands to begin charging a stationary, powerful beam in the direction you face. After a 1 second windup, it releases a beam of prismatic energy that deals ")
			.addDepthsDamage(a -> DAMAGE[rarity - 1] * 4, DAMAGE[rarity - 1] * 4, true)
			.add(" magic damage per second for ")
			.addDuration(a -> DURATION, DURATION)
			.add(" seconds. Additionally, the beam will apply status effects to allies and enemies depending on your available trees.")
			.addCooldown(COOLDOWN_TICKS)
			.addConditionalTree(DepthsTree.FROSTBORN, getFrostbornDescription(color))
			.addConditionalTree(DepthsTree.FLAMECALLER, getFlamecallerDescription(color))
			.addConditionalTree(DepthsTree.DAWNBRINGER, getDawnbringerDescription(color))
			.addConditionalTree(DepthsTree.EARTHBOUND, getEarthboundDescription(color))
			.addConditionalTree(DepthsTree.SHADOWDANCER, getShadowdancerDescription(color))
			.addConditionalTree(DepthsTree.STEELSAGE, getSteelsageDescription(color))
			.addConditionalTree(DepthsTree.WINDWALKER, getWindwalkerDescription(color));
	}

	private static Description<Refraction> getFrostbornDescription(TextColor color) {
		return new DescriptionBuilder<Refraction>(color)
			.add(Component.text("\nFrostborn").color(TextColor.color(DepthsUtils.FROSTBORN)))
			.add(" - Apply 10% Slow for ")
			.addDuration(EFFECT_DURATION)
			.add(" seconds to mobs hit by the beam.");
	}

	private static Description<Refraction> getFlamecallerDescription(TextColor color) {
		return new DescriptionBuilder<Refraction>(color)
			.add(Component.text("\nFlamecaller").color(TextColor.color(DepthsUtils.FLAMECALLER)))
			.add(" - Apply Fire for ")
			.addDuration(EFFECT_DURATION)
			.add(" seconds to mobs hit by the beam.");
	}

	private static Description<Refraction> getDawnbringerDescription(TextColor color) {
		return new DescriptionBuilder<Refraction>(color)
			.add(Component.text("\nDawnbringer").color(TextColor.color(DepthsUtils.DAWNBRINGER)))
			.add(" - Apply a 5% Max HP Heal to players hit by the beam.");
	}

	private static Description<Refraction> getEarthboundDescription(TextColor color) {
		return new DescriptionBuilder<Refraction>(color)
			.add(Component.text("\nEarthbound").color(TextColor.color(DepthsUtils.EARTHBOUND)))
			.add(" - Apply 10% Resistance for ")
			.addDuration(BUFF_DURATION)
			.add(" seconds to players hit by the beam.");
	}

	private static Description<Refraction> getShadowdancerDescription(TextColor color) {
		return new DescriptionBuilder<Refraction>(color)
			.add(Component.text("\nShadowdancer").color(TextColor.color(DepthsUtils.SHADOWDANCER)))
			.add(" - Apply 10% Vulnerability for ")
			.addDuration(EFFECT_DURATION)
			.add(" seconds to mobs hit by the beam.");
	}

	private static Description<Refraction> getSteelsageDescription(TextColor color) {
		return new DescriptionBuilder<Refraction>(color)
			.add(Component.text("\nSteelsage").color(TextColor.color(DepthsUtils.STEELSAGE)))
			.add(" - Apply 10% Bleed for ")
			.addDuration(EFFECT_DURATION)
			.add(" seconds to mobs hit by the beam.");
	}

	private static Description<Refraction> getWindwalkerDescription(TextColor color) {
		return new DescriptionBuilder<Refraction>(color)
			.add(Component.text("\nWindwalker").color(TextColor.color(DepthsUtils.WINDWALKER)))
			.add(" - Apply 10% Speed for ")
			.addDuration(BUFF_DURATION)
			.add(" seconds to players hit by the beam.");
	}
}

