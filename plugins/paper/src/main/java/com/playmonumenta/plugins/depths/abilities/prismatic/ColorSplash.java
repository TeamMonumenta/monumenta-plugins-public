package com.playmonumenta.plugins.depths.abilities.prismatic;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
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
import com.playmonumenta.plugins.depths.abilities.windwalker.DepthsWindWalk;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ColorSplash extends DepthsAbility {

	public static final String ABILITY_NAME = "Color Splash";
	public static final int COOLDOWN = 30 * 20;
	public static final int ACTIVATION_DELAY = 20;

	public static final int FROSTBORN_DURATION = 10 * 20;
	public static final int FROSTBORN_ICE_DURATION = 10 * 20;
	public static final double FROSTBORN_RADIUS = 15;
	public static final int FROSTBORN_DAMAGE_INTERVAL = 10;
	public static final double[] FROSTBORN_DAMAGE = {8, 10, 12, 14, 16, 20};

	public static final int FLAMECALLER_DURATION = 10 * 20;
	public static final int FLAMECALLER_INTERVAL = 5;
	public static final double FLAMECALLER_IMPACT_RADIUS = 2.5;
	public static final double[] FLAMECALLER_DAMAGE = {4, 5, 6, 7, 8, 10};

	public static final int DAWNBRINGER_DURATION = 10 * 20;
	public static final int DAWNBRINGER_HEAL_INTERVAL = 20;
	public static final double DAWNBRINGER_CONE_LENGTH = 15;
	public static final double DAWNBRINGER_CONE_HALFANGLE = Math.PI / 4;
	public static final double DAWNBRINGER_MAX_OVERHEAL = 10;
	public static final int DAWNBRINGER_OVERHEAL_DURATION = 8 * 20;
	public static final double DAWNBRINGER_EXTRA_REVIVE_RADIUS = 2;
	public static final Particle.DustOptions DAWNBRINGER_EFFECT_OPTIONS = new Particle.DustOptions(Color.fromRGB(255, 200, 0), 1f);
	public static final double[] DAWNBRINGER_HEALING = {4, 5, 6, 7, 8, 10};

	public static final int EARTHBOUND_DURATION = 10 * 20;
	public static final int EARTHBOUND_RADIUS = 8;
	public static final double EARTHBOUND_HP_THRESHOLD = 0.4;
	public static final int EARTHBOUND_STUN_DURATION = 3 * 20;
	public static final int EARTHBOUND_TAUNT_INTERVAL = 20;
	public static final String EARTHBOUND_RESISTANCE_EFFECT_NAME = "ColorSplashEarthboundResistance";
	public static final Particle.DustOptions EARTHBOUND_TAUNT_OPTIONS = new Particle.DustOptions(Color.YELLOW, 1.5f);
	public static final double[] EARTHBOUND_RESISTANCE = {0.2, 0.24, 0.28, 0.32, 0.36, 0.44};

	public static final int SHADOWDANCER_DURATION = 10 * 20;
	public static final int SHADOWDANCER_STEALTH_INTERVAL = 20;
	public static final Particle.DustOptions SHADOWDANCER_STEALTH_OPTIONS = new Particle.DustOptions(Color.BLACK, 2);
	public static final double[] SHADOWDANCER_DAMAGE_MULTIPLIER = {0.12, 0.14, 0.16, 0.18, 0.20, 0.24};

	public static final int STEELSAGE_DURATION = 10 * 20;
	public static final double[] STEELSAGE_PROJ_DAMAGE_MULTIPLIER = {0.2, 0.24, 0.28, 0.32, 0.36, 0.44};

	public static final int WINDWALKER_DURATION = 10 * 20;
	public static final int WINDWALKER_LAND_COOLDOWN = 12;
	public static final int WINDWALKER_IFRAMES = 10;
	public static final double WINDWALKER_RADIUS = 3;
	public static final int WINDWALKER_EFFECTS_DURATION = 2 * 20;
	public static final double WINDWALKER_VULNERABILITY = 0.2;
	public static final String WINDWALKER_SPEED_EFFECT_NAME = "ColorSplashWindwalkerSpeed";
	public static final String WINDWALKER_SPEED_ATTR_NAME = "ColorSplashWindwalkerSpeedAttr";
	public static final double[] WINDWALKER_SPEED = {0.2, 0.24, 0.28, 0.32, 0.36, 0.44};

	private boolean mDawnbringerActive;
	private boolean mShadowdancerActive;
	private boolean mSteelsageActive;
	private boolean mWindwalkerActive;
	private boolean mWindwalkIframes;
	private boolean mCanCastWindwalk;

	public static final DepthsAbilityInfo<ColorSplash> INFO =
		new DepthsAbilityInfo<>(ColorSplash.class, ABILITY_NAME, ColorSplash::new, DepthsTree.PRISMATIC, DepthsTrigger.SWAP)
			.linkedSpell(ClassAbility.COLOR_SPLASH)
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", ColorSplash::cast, DepthsTrigger.SWAP))
			.displayItem(Material.BEACON)
			.descriptions(ColorSplash::getDescription);

	public ColorSplash(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDawnbringerActive = false;
		mShadowdancerActive = false;
		mSteelsageActive = false;
		mWindwalkerActive = false;
		mWindwalkIframes = false;
		mCanCastWindwalk = false;
	}

	public boolean cast() {
		if (isOnCooldown()) {
			if (mWindwalkerActive && mCanCastWindwalk) {
				castWindwalk();
				return true;
			}
			return false;
		}

		putOnCooldown();
		DepthsPlayer dPlayer = DepthsManager.getInstance().getDepthsPlayer(mPlayer);
		if (dPlayer == null) {
			return false;
		}

		List<DepthsTree> availableTrees = dPlayer.mEligibleTrees;
		DepthsTree selectedTree = availableTrees.get(FastUtils.randomIntInRange(0, availableTrees.size() - 1));
		preActivationAesthetics(selectedTree);

		ItemStatManager.PlayerItemStats playerItemStats = Plugin.getInstance().mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		// Start the actual ability after a short delay.
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			switch (selectedTree) {
				case FROSTBORN -> frostborn(playerItemStats);
				case FLAMECALLER -> flamecaller(playerItemStats);
				case DAWNBRINGER -> dawnbringer();
				case EARTHBOUND -> earthbound();
				case SHADOWDANCER -> shadowdancer();
				case STEELSAGE -> steelsage();
				case WINDWALKER -> windwalker();
				default -> {
					// This should not happen.
				}
			}
		}, ACTIVATION_DELAY);

		return true;
	}

	private void preActivationAesthetics(DepthsTree selectedTree) {
		switch (selectedTree) {
			case FROSTBORN -> {
				new PPExplosion(Particle.CLOUD, mPlayer.getLocation()).count(50).spawnAsPlayerActive(mPlayer);
				new PPCircle(Particle.CLOUD, mPlayer.getLocation(), 2).countPerMeter(5).ringMode(true)
					.delta(0, 0, 1).rotateDelta(true).spawnAsPlayerActive(mPlayer);
				new PPCircle(Particle.BLOCK_CRACK, mPlayer.getLocation(), 4).ringMode(false).countPerMeter(5)
					.data(Material.ICE.createBlockData()).spawnAsPlayerActive(mPlayer);

				mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 2, 1.5f);
				mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 2, 1.5f);
			}
			case FLAMECALLER -> {
				mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 2, 1.5f);
				new PPCircle(Particle.FLAME, mPlayer.getLocation(), 2).countPerMeter(3).ringMode(false).spawnAsPlayerActive(mPlayer);
			}
			case DAWNBRINGER -> {
				new PPLine(Particle.END_ROD, mPlayer.getLocation(), new Vector(0, 1, 0), 10).delta(0.3, 0, 0.3)
					.countPerMeter(5).spawnAsPlayerActive(mPlayer);
				new PPLine(Particle.REDSTONE, mPlayer.getLocation(), new Vector(1, 0.5, 0), 3).countPerMeter(2)
					.data(DAWNBRINGER_EFFECT_OPTIONS).spawnAsPlayerActive(mPlayer);
				new PPLine(Particle.REDSTONE, mPlayer.getLocation(), new Vector(-1, 0.5, 0), 3).countPerMeter(2)
					.data(DAWNBRINGER_EFFECT_OPTIONS).spawnAsPlayerActive(mPlayer);
				new PPLine(Particle.REDSTONE, mPlayer.getLocation(), new Vector(0, 0.5, 1), 3).countPerMeter(2)
					.data(DAWNBRINGER_EFFECT_OPTIONS).spawnAsPlayerActive(mPlayer);
				new PPLine(Particle.REDSTONE, mPlayer.getLocation(), new Vector(0, 0.5, -1), 3).countPerMeter(2)
					.data(DAWNBRINGER_EFFECT_OPTIONS).spawnAsPlayerActive(mPlayer);
				mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 2, 0.75f);
				mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 2, 1.5f);
			}
			case EARTHBOUND -> {
				mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.PLAYERS, 2, 0.5f);
				Block blockUnder = mPlayer.getLocation().subtract(0, 1, 0).getBlock();
				ArrayList<Block> groundBlocks = new ArrayList<>(
					List.of(
						blockUnder,
						blockUnder.getRelative(0, 0, 1),
						blockUnder.getRelative(1, 0, 0),
						blockUnder.getRelative(1, 0, 1),
						blockUnder.getRelative(0, 0, -1),
						blockUnder.getRelative(-1, 0, 0),
						blockUnder.getRelative(1, 0, -1),
						blockUnder.getRelative(-1, 0, 1),
						blockUnder.getRelative(-1, 0, -1)
					)
				);
				groundBlocks.removeIf(block -> !block.isSolid());
				List<Material> groundBlockTypes = groundBlocks.stream().map(Block::getType).toList();
				if (groundBlockTypes.isEmpty()) {
					groundBlockTypes = List.of(Material.PODZOL, Material.DIRT, Material.MUD);
				}
				DisplayEntityUtils.groundBlockQuake(mPlayer.getLocation(), 3, groundBlockTypes, null);
			}
			case SHADOWDANCER -> {
				mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 2, 0.8f);
				drawShadowdancerSymbol();
			}
			case STEELSAGE -> {
				mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_0, SoundCategory.PLAYERS, 2, 1.25f);
			}
			case WINDWALKER -> {
				new PPSpiral(Particle.CLOUD, mPlayer.getLocation(), 4).count(50).spawnAsPlayerActive(mPlayer);
				mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.PLAYERS, 2, 1.75f);
			}
			default -> {
				// This should not happen.
			}
		}
	}

	private void frostborn(ItemStatManager.PlayerItemStats playerItemStats) {
		// For X seconds, leave a trail of ice where you walk and any nearby enemies within X blocks take rarity% damage every second while standing on ice.
		cancelOnDeath(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				// Place ice under the player
				if (PlayerUtils.isOnGround(mPlayer)) {
					Block underPlayer = mPlayer.getLocation().subtract(0, 1, 0).getBlock();
					Block[] blocksToConvert = {
						underPlayer,
						underPlayer.getRelative(1, 0, 0),
						underPlayer.getRelative(-1, 0, 0),
						underPlayer.getRelative(0, 0, 1),
						underPlayer.getRelative(0, 0, -1),
						underPlayer.getRelative(1, 0, 1),
						underPlayer.getRelative(-1, 0, 1),
						underPlayer.getRelative(1, 0, -1),
						underPlayer.getRelative(-1, 0, -1)
					};
					for (Block block : blocksToConvert) {
						if (DepthsUtils.isIce(block.getType())) {
							continue;
						}

						DepthsUtils.iceExposedBlock(block, FROSTBORN_ICE_DURATION, mPlayer, false);
					}
				}

				if (mTicks % FROSTBORN_DAMAGE_INTERVAL == 0) {
					// Damage nearby mobs standing on ice
					List<LivingEntity> nearbyMobsOnIce = new Hitbox.SphereHitbox(mPlayer.getLocation(), FROSTBORN_RADIUS)
						.getHitMobs().stream().filter(mob -> DepthsUtils.isIce(mob.getLocation().subtract(0, 1, 0).getBlock().getType())).toList();
					nearbyMobsOnIce.forEach(mob -> {
						DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), FROSTBORN_DAMAGE[mRarity - 1], true, false, false);
						new PartialParticle(Particle.BLOCK_CRACK, mob.getLocation(), 10).delta(0.2, 0.5, 0.2)
							.data(Material.ICE.createBlockData()).spawnAsPlayerActive(mPlayer);
						new PPLine(Particle.FIREWORKS_SPARK, LocationUtils.getHalfHeightLocation(mPlayer), LocationUtils.getHalfHeightLocation(mob))
							.countPerMeter(1).spawnAsPlayerActive(mPlayer);
					});

					// Hit sounds for mobs (so that it only plays once)
					if (nearbyMobsOnIce.size() > 0) {
						mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 2, 1.3f);
					}

					// Activation effects
					mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 2, 1.65f);
					mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 2, 1.65f);
				}

				if (mTicks >= FROSTBORN_DURATION) {
					cancel();
					return;
				}

				mTicks++;
				if (mTicks % FROSTBORN_DAMAGE_INTERVAL < 5) {
					new PPCircle(Particle.BLOCK_CRACK, mPlayer.getLocation(), 1 + 0.75 * (mTicks % 5)).ringMode(true).countPerMeter(2)
						.data(Material.ICE.createBlockData()).spawnAsPlayerActive(mPlayer);
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1));
	}

	private void flamecaller(ItemStatManager.PlayerItemStats playerItemStats) {
		// Summons a volcanic meteor storm that tracks your cursor for X seconds, dealing rarity% damage per hit.
		cancelOnDeath(new BukkitRunnable() {
			Location mCurrLoc = LocationUtils.rayTraceToBlock(mPlayer, 200);
			int mTicks = 1;
			@Override
			public void run() {
				Location eyeLoc = mPlayer.getEyeLocation();
				RayTraceResult targetingRay = mPlayer.getWorld()
					.rayTrace(eyeLoc, eyeLoc.getDirection(), 200, FluidCollisionMode.NEVER, true, 0.35, e -> e instanceof LivingEntity living && EntityUtils.isHostileMob(living));
				if (targetingRay != null) {
					Entity hitEntity = targetingRay.getHitEntity();
					// If a hostile mob is hit, target it. Otherwise, target hit block.
					if (hitEntity instanceof LivingEntity hitLiving) {
						mCurrLoc = hitLiving.getLocation().setDirection(new Vector(0, 1, 0));
						new PPCircle(Particle.FLAME, mCurrLoc, FLAMECALLER_IMPACT_RADIUS)
							.countPerMeter(0.5).offset(FastUtils.randomDoubleInRange(0, Math.PI)).directionalMode(true)
							.delta(0, 1, 0).extra(0.03).spawnAsPlayerActive(mPlayer);
					} else {
						BlockFace blockFace = targetingRay.getHitBlockFace();
						Block block = targetingRay.getHitBlock();
						if (blockFace != null && block != null) {
							Vector dir = blockFace.getDirection();
							mCurrLoc = BlockUtils.getCenterBlockLocation(block).add(dir.clone().multiply(0.5)).setDirection(dir);
							ArrayList<Vector> axes = new ArrayList<>();
							if (dir.getX() == 0) {
								axes.add(new Vector(1, 0, 0));
							}
							if (dir.getY() == 0) {
								axes.add(new Vector(0, 1, 0));
							}
							if (dir.getZ() == 0) {
								axes.add(new Vector(0, 0, 1));
							}
							new PPCircle(Particle.FLAME, mCurrLoc, FLAMECALLER_IMPACT_RADIUS).axes(axes.get(0), axes.get(1))
								.countPerMeter(0.5).offset(FastUtils.randomDoubleInRange(0, Math.PI)).directionalMode(true)
								.delta(dir.getX(), dir.getY(), dir.getZ()).extra(0.03).spawnAsPlayerActive(mPlayer);
						} else {
							mCurrLoc = targetingRay.getHitPosition().toLocation(mPlayer.getWorld()).setDirection(new Vector(0, 1, 0));
							new PPCircle(Particle.FLAME, mCurrLoc, FLAMECALLER_IMPACT_RADIUS)
								.countPerMeter(0.5).offset(FastUtils.randomDoubleInRange(0, Math.PI)).directionalMode(true)
								.delta(0, 1, 0).extra(0.03).spawnAsPlayerActive(mPlayer);
						}
					}

				}

				if (mTicks % FLAMECALLER_INTERVAL == 0) {
					// Meteor effect
					Location awayLoc = LocationUtils.rayTraceToBlock(mCurrLoc, mCurrLoc.getDirection(), 9, null);
					Vector randomDir = new Vector(FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(-1, 1)).normalize();
					Location meteorLoc = LocationUtils.rayTraceToBlock(awayLoc, randomDir, 2, null);
					Vector toTarget = LocationUtils.getDirectionTo(mCurrLoc, meteorLoc);

					new PPLine(Particle.REDSTONE, meteorLoc, mCurrLoc).countPerMeter(3).data(new Particle.DustOptions(Color.RED, 1)).directionalMode(true)
						.delta(toTarget.getX(), toTarget.getY(), toTarget.getZ()).extra(0.1).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.SMOKE_LARGE, mCurrLoc).count(10).extra(0.1).spawnAsPlayerActive(mPlayer);
					mPlayer.getWorld().playSound(mCurrLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 2, 2);

					// Damage mobs
					new Hitbox.SphereHitbox(mCurrLoc, FLAMECALLER_IMPACT_RADIUS).getHitMobs().forEach(
						mob -> DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), FLAMECALLER_DAMAGE[mRarity - 1], true, false, false)
					);
				}

				if (mTicks >= FLAMECALLER_DURATION) {
					cancel();
					return;
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1));
	}

	private void dawnbringer() {
		// You become a beacon of light for X seconds, healing all players in a cone in your direction for rarity% health every second. Extra healing can provide up to X absorption health. Your teammate revive radius is increased by X blocks for the duration.
		mDawnbringerActive = true;
		cancelOnDeath(new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				if (mTicks % DAWNBRINGER_HEAL_INTERVAL == 0) {
					mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 2, 2);

					// Try to heal players, and over heal if necessary
					Location eyeLoc = mPlayer.getEyeLocation();
					Hitbox.approximateCone(eyeLoc, DAWNBRINGER_CONE_LENGTH, DAWNBRINGER_CONE_HALFANGLE).getHitPlayers(mPlayer, true)
						.forEach(player -> {
							ParticleUtils.launchOrb(eyeLoc.getDirection(), eyeLoc, mPlayer, player, 200, null, DAWNBRINGER_EFFECT_OPTIONS, (hitLiving) -> {
								if (hitLiving instanceof Player hitPlayer) {
									double amountHealed = PlayerUtils.healPlayer(Plugin.getInstance(), hitPlayer, DAWNBRINGER_HEALING[mRarity - 1], mPlayer);

									if (amountHealed < DAWNBRINGER_HEALING[mRarity - 1]) {
										double overHeal = Math.min(DAWNBRINGER_MAX_OVERHEAL, DAWNBRINGER_HEALING[mRarity - 1] - amountHealed);
										AbsorptionUtils.addAbsorption(hitPlayer, overHeal, DAWNBRINGER_MAX_OVERHEAL, DAWNBRINGER_OVERHEAL_DURATION);
									}

									new PartialParticle(Particle.HEART, hitPlayer.getLocation(), 4)
										.delta(0.2).spawnAsPlayerActive(mPlayer);
									hitPlayer.getWorld().playSound(hitPlayer.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0f, 1.2f);
								}
							});
						});
				}

				mTicks++;
				if (mTicks >= DAWNBRINGER_DURATION) {
					cancel();
				}
			}

			@Override
			public synchronized void cancel() {
				// Override it so that cancelOnDeath also sets the boolean to false.
				mDawnbringerActive = false;
				super.cancel();
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1));
	}

	private void earthbound() {
		// You gain rarity% resistance and taunt nearby enemies every second for X seconds. If you drop below 40% hp, disable this effect and stun nearby mobs for X seconds.
		Plugin.getInstance().mEffectManager.addEffect(mPlayer, EARTHBOUND_RESISTANCE_EFFECT_NAME, new PercentDamageReceived(EARTHBOUND_DURATION, -EARTHBOUND_RESISTANCE[mRarity - 1]));
		cancelOnDeath(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks % EARTHBOUND_TAUNT_INTERVAL == 0) {
					// Taunt
					new Hitbox.SphereHitbox(mPlayer.getLocation(), EARTHBOUND_RADIUS).getHitMobs()
						.forEach(mob -> {
							if (mob instanceof Mob mobAI) {
								mobAI.setTarget(mPlayer);
								Location aboveMobHead = LocationUtils.getHeightLocation(mob, 1.2);
								new PartialParticle(Particle.REDSTONE, aboveMobHead, 5).data(EARTHBOUND_TAUNT_OPTIONS).spawnAsPlayerActive(mPlayer);
								new PPLine(Particle.REDSTONE, aboveMobHead.clone().add(0, 1, 0), aboveMobHead.clone().add(0, 2, 0))
									.countPerMeter(4).data(EARTHBOUND_TAUNT_OPTIONS).spawnAsPlayerActive(mPlayer);
							}
						});

					// Taunt effects
					mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 1.4f, 0.1f);
					new PPCircle(Particle.BLOCK_CRACK, mPlayer.getLocation(), 5).ringMode(false).countPerMeter(2)
						.data(Material.DIRT.createBlockData()).spawnAsPlayerActive(mPlayer);
				}

				if (mPlayer.getHealth() <= EntityUtils.getMaxHealth(mPlayer) * EARTHBOUND_HP_THRESHOLD) {
					// Stun nearby mobs
					new Hitbox.SphereHitbox(mPlayer.getLocation(), EARTHBOUND_RADIUS).getHitMobs()
						.forEach(mob -> EntityUtils.applyStun(Plugin.getInstance(), EARTHBOUND_STUN_DURATION, mob));

					// Clear Resistance
					Plugin.getInstance().mEffectManager.clearEffects(mPlayer, EARTHBOUND_RESISTANCE_EFFECT_NAME);

					// Stun effects
					mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_WARDEN_ROAR, SoundCategory.PLAYERS, 3, 1);

					cancel();
					return;
				}

				mTicks++;
				if (mTicks >= EARTHBOUND_DURATION) {
					Plugin.getInstance().mEffectManager.clearEffects(mPlayer, EARTHBOUND_RESISTANCE_EFFECT_NAME);
					cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1));
	}

	private void shadowdancer() {
		// For X seconds, yourself and nearby players enter stealth every second and the next instance of damage dealt after exiting stealth is multiplied by rarity% each time.
		mShadowdancerActive = true;
		cancelOnDeath(new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				if (mTicks % SHADOWDANCER_STEALTH_INTERVAL == 0) {
					AbilityUtils.applyStealth(Plugin.getInstance(), mPlayer, SHADOWDANCER_STEALTH_INTERVAL);
					drawShadowdancerSymbol();
				}

				mTicks++;
				if (mTicks >= SHADOWDANCER_DURATION) {
					cancel();
				}
			}

			@Override
			public synchronized void cancel() {
				// Override it so that cancelOnDeath also sets the boolean to false.
				mShadowdancerActive = false;
				super.cancel();
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1));
	}

	private void steelsage() {
		// Mount an invincible horse for X seconds. While on the horse, you deal rarity% more projectile damage and are immune to melee damage for the duration.
		Entity e = LibraryOfSoulsIntegration.summon(mPlayer.getLocation(), "SteelStallion");
		if (e instanceof Horse horse) {
			mSteelsageActive = true;
			horse.addPassenger(mPlayer);
			horse.setInvulnerable(true);
			mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_HORSE_GALLOP, SoundCategory.PLAYERS, 2, 1.25f);

			cancelOnDeath(new BukkitRunnable() {
				final Horse mHorse = horse;
				int mTicks = 0;
				int mWarningsLeft = 3;

				@Override
				public void run() {
					// Horse trail
					new PartialParticle(Particle.ASH, LocationUtils.getHalfHeightLocation(mHorse), 25).delta(0.1)
						.spawnAsPlayerActive(mPlayer);

					if (mTicks > STEELSAGE_DURATION - 80 && mTicks % 20 == 0) {
						mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, SoundCategory.PLAYERS, 2, 0 + (0.3f * Math.max(0, mWarningsLeft)));
						mWarningsLeft--;
					}

					// If player dismounts the horse, remove it.
					if (mHorse.getPassengers().size() == 0) {
						mHorse.remove();
						cancel();
						return;
					}

					mTicks++;
					if (mTicks >= STEELSAGE_DURATION) {
						mHorse.remove();
						cancel();
					}
				}

				@Override
				public synchronized void cancel() {
					// Override it so that cancelOnDeath also sets the boolean to false.
					mSteelsageActive = false;
					mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_7, SoundCategory.PLAYERS, 2, 2);
					super.cancel();
				}
			}.runTaskTimer(Plugin.getInstance(), 0, 1));
		}

	}

	private void windwalker() {
		// For X seconds gain rarity% speed and every time you swap hands while the duration is running, you wind walk through mobs launching yourself and applying levitation and X vulnerability to non elites for 2 seconds. Gain immunity to damage for 0.5 seconds after dashing.
		mWindwalkerActive = true;
		mCanCastWindwalk = true;
		Plugin.getInstance().mEffectManager.addEffect(mPlayer, WINDWALKER_SPEED_EFFECT_NAME, new PercentSpeed(WINDWALKER_DURATION, WINDWALKER_SPEED[mRarity - 1], WINDWALKER_SPEED_ATTR_NAME));
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			mWindwalkerActive = false;
			mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_HURT, SoundCategory.PLAYERS, 2, 0);
		}, WINDWALKER_DURATION);
	}

	private void castWindwalk() {
		mCanCastWindwalk = false;
		// Velocity
		Vector dir = mPlayer.getEyeLocation().getDirection();
		Vector yVelocity = new Vector(0, dir.getY() * DepthsWindWalk.WIND_WALK_Y_VELOCITY_MULTIPLIER + DepthsWindWalk.WIND_WALK_Y_VELOCITY, 0);
		mPlayer.setVelocity(dir.multiply(DepthsWindWalk.WIND_WALK_VELOCITY_BONUS).add(yVelocity));
		// Cast effects
		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1, 1.75f);
		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 1, 1);
		new PartialParticle(Particle.SMOKE_NORMAL, mPlayer.getLocation(), 90)
			.delta(0.25, 0.45, 0.25).extra(0.1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, mPlayer.getLocation(), 20)
			.delta(0.25, 0.45, 0.25).extra(0.15).spawnAsPlayerActive(mPlayer);
		// Iframes
		mWindwalkIframes = true;
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mWindwalkIframes = false, WINDWALKER_IFRAMES);

		cancelOnDeath(new BukkitRunnable() {
			final ArrayList<UUID> mHitMobs = new ArrayList<>();
			int mTicks = 0;
			@Override
			public void run() {
				if (mPlayer.isDead() || !mPlayer.isOnline() || !mPlayer.getLocation().isChunkLoaded()) {
					cancel();
					return;
				}

				Block block = mPlayer.getLocation().getBlock();
				if (mTicks >= 5 && (PlayerUtils.isOnGroundOrMountIsOnGround(mPlayer) || BlockUtils.isWaterlogged(block) || block.getType() == Material.LAVA || BlockUtils.isClimbable(block))
				) {
					cancel();
					return;
				}

				new PartialParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation().add(0, 0.5, 0), (int) (7/Math.pow(1.1, mTicks)), 0.15, 0.45, 0.15, 0).spawnAsPlayerPassive(mPlayer);

				new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), WINDWALKER_RADIUS).getHitMobs()
					.stream().filter(mob -> !mHitMobs.contains(mob.getUniqueId()) && !EntityUtils.isBoss(mob)).forEach(mob -> {
						mHitMobs.add(mob.getUniqueId());

						new PartialParticle(Particle.SWEEP_ATTACK, LocationUtils.getHalfHeightLocation(mob), 16)
							.delta(0.5).spawnAsPlayerActive(mPlayer);
						mPlayer.getWorld().playSound(mob.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.75f, 1.25f);

						EntityUtils.applyVulnerability(mPlugin, WINDWALKER_EFFECTS_DURATION, WINDWALKER_VULNERABILITY, mob);

						if (!EntityUtils.isCCImmuneMob(mob)) {
							mob.setVelocity(mob.getVelocity().setY(0.5));
							PotionUtils.apply(mob, new PotionUtils.PotionInfo(PotionEffectType.LEVITATION, WINDWALKER_EFFECTS_DURATION, 1, false, false, false));
							EntityUtils.applyStun(mPlugin, 30, mob);
						}
					});

				mTicks++;
			}

			@Override
			public synchronized void cancel() {
				mWindwalkIframes = false;
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
					mCanCastWindwalk = mWindwalkerActive;
					mPlayer.getWorld().playSound(mPlayer, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 1, 0.85f);
				}, WINDWALKER_LAND_COOLDOWN);
				super.cancel();
			}
		}.runTaskTimer(Plugin.getInstance(), 1, 1));
	}

	private void drawShadowdancerSymbol() {
		new PPLine(Particle.REDSTONE, mPlayer.getLocation().add(2.5, 0, 0), mPlayer.getLocation().add(-2.5, 0, 0))
			.data(SHADOWDANCER_STEALTH_OPTIONS).countPerMeter(3).spawnAsPlayerActive(mPlayer);
		new PPLine(Particle.REDSTONE, mPlayer.getLocation().add(0, 0, 2.5), mPlayer.getLocation().add(0, 0, -2.5))
			.data(SHADOWDANCER_STEALTH_OPTIONS).countPerMeter(3).spawnAsPlayerActive(mPlayer);
		new PPLine(Particle.REDSTONE, mPlayer.getLocation().add(2.5, 0, 2.5), mPlayer.getLocation().add(-2.5, 0, -2.5))
			.data(SHADOWDANCER_STEALTH_OPTIONS).countPerMeter(3).spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.REDSTONE, mPlayer.getLocation(), 1.5).ringMode(true).countPerMeter(3)
			.data(SHADOWDANCER_STEALTH_OPTIONS).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (mShadowdancerActive && AbilityUtils.isStealthed(mPlayer)) {
			event.updateDamageWithMultiplier(1 + SHADOWDANCER_DAMAGE_MULTIPLIER[mRarity - 1]);
			AbilityUtils.removeStealth(Plugin.getInstance(), mPlayer, false, null);
		}
		if (mSteelsageActive && (event.getType().equals(DamageEvent.DamageType.PROJECTILE) || event.getType().equals(DamageEvent.DamageType.PROJECTILE_SKILL))) {
			event.updateDamageWithMultiplier(1 + STEELSAGE_PROJ_DAMAGE_MULTIPLIER[mRarity - 1]);
		}
		return false;
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (mSteelsageActive && source != null && event.getType().equals(DamageEvent.DamageType.MELEE)) {
			event.setCancelled(true);
		}
		if (mWindwalkerActive && mWindwalkIframes && !event.getType().equals(DamageEvent.DamageType.TRUE)) {
			event.setCancelled(true);
		}
	}

	public boolean hasIncreasedReviveRadius() {
		return mDawnbringerActive;
	}

	private static Description<ColorSplash> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<ColorSplash>(color)
			.add("Swap hands to cast a unique ability after ")
			.addDuration(ACTIVATION_DELAY)
			.add("s. This ability is chosen at random from one of your available trees.")
			.addCooldown(COOLDOWN)
			.addConditionalTree(DepthsTree.FROSTBORN, getFrostbornDescription(rarity, color))
			.addConditionalTree(DepthsTree.FLAMECALLER, getFlamecallerDescription(rarity, color))
			.addConditionalTree(DepthsTree.DAWNBRINGER, getDawnbringerDescription(rarity, color))
			.addConditionalTree(DepthsTree.EARTHBOUND, getEarthboundDescription(rarity, color))
			.addConditionalTree(DepthsTree.SHADOWDANCER, getShadowdancerDescription(rarity, color))
			.addConditionalTree(DepthsTree.STEELSAGE, getSteelsageDescription(rarity, color))
			.addConditionalTree(DepthsTree.WINDWALKER, getWindwalkerDescription(rarity, color));
	}

	private static Description<ColorSplash> getFrostbornDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<ColorSplash>(color)
			.add(Component.text("\nFrostborn").color(TextColor.color(DepthsUtils.FROSTBORN)))
			.add(" - For ")
			.addDuration(FROSTBORN_DURATION)
			.add("s, leave a trail of ice behind you. Mobs within ")
			.add(FROSTBORN_RADIUS)
			.add(" blocks of you will take ")
			.addDepthsDamage(a -> FROSTBORN_DAMAGE[rarity - 1], FROSTBORN_DAMAGE[rarity - 1], true)
			.add(" magic damage every ")
			.addDuration(FROSTBORN_DAMAGE_INTERVAL)
			.add("s while standing on ice.");
	}

	private static Description<ColorSplash> getFlamecallerDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<ColorSplash>(color)
			.add(Component.text("\nFlamecaller").color(TextColor.color(DepthsUtils.FLAMECALLER)))
			.add(" - For ")
			.addDuration(FLAMECALLER_DURATION)
			.add("s, summon a meteor shower at the location where you are looking, dealing ")
			.addDepthsDamage(a -> FLAMECALLER_DAMAGE[rarity - 1], FLAMECALLER_DAMAGE[rarity - 1], true)
			.add(" magic damage to mobs within ")
			.add(FLAMECALLER_IMPACT_RADIUS)
			.add(" blocks upon impact.");
	}

	private static Description<ColorSplash> getDawnbringerDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<ColorSplash>(color)
			.add(Component.text("\nDawnbringer").color(TextColor.color(DepthsUtils.DAWNBRINGER)))
			.add(" - For ")
			.addDuration(DAWNBRINGER_DURATION)
			.add("s, become a beacon of light, healing all players in a cone in front of you for ")
			.add(a -> DAWNBRINGER_HEALING[rarity - 1], DAWNBRINGER_HEALING[rarity - 1], false, null, true)
			.add(" health, every ")
			.addDuration(DAWNBRINGER_HEAL_INTERVAL)
			.add("s. Extra healing will be converted to absorption, up to ")
			.add(DAWNBRINGER_MAX_OVERHEAL)
			.add(" health, which lasts for ")
			.addDuration(DAWNBRINGER_OVERHEAL_DURATION)
			.add("s. Additionally, you can revive teammates from ")
			.add(DAWNBRINGER_EXTRA_REVIVE_RADIUS)
			.add(" blocks further away than normal.");
	}

	private static Description<ColorSplash> getEarthboundDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<ColorSplash>(color)
			.add(Component.text("\nEarthbound").color(TextColor.color(DepthsUtils.EARTHBOUND)))
			.add(" - For ")
			.addDuration(EARTHBOUND_DURATION)
			.add("s, gain ")
			.addPercent(a -> EARTHBOUND_RESISTANCE[rarity - 1], EARTHBOUND_RESISTANCE[rarity - 1], false, true)
			.add(" resistance, and taunt enemies within ")
			.add(EARTHBOUND_RADIUS)
			.add(" blocks of you every ")
			.addDuration(EARTHBOUND_TAUNT_INTERVAL)
			.add("s. Additionally, if you drop below ")
			.addPercent(EARTHBOUND_HP_THRESHOLD)
			.add(" health, disable this effect and stun nearby mobs for ")
			.addDuration(EARTHBOUND_STUN_DURATION)
			.add("s.");

	}

	private static Description<ColorSplash> getShadowdancerDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<ColorSplash>(color)
			.add(Component.text("\nShadowdancer").color(TextColor.color(DepthsUtils.SHADOWDANCER)))
			.add(" - For ")
			.addDuration(SHADOWDANCER_DURATION)
			.add("s, enter stealth every ")
			.addDuration(SHADOWDANCER_STEALTH_INTERVAL)
			.add("s. The next instance of damage done while in stealth will deal ")
			.addPercent(a -> SHADOWDANCER_DAMAGE_MULTIPLIER[rarity - 1], SHADOWDANCER_DAMAGE_MULTIPLIER[rarity - 1], false, true)
			.add(" more damage and will put you out of stealth.");
	}

	private static Description<ColorSplash> getSteelsageDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<ColorSplash>(color)
			.add(Component.text("\nSteelsage").color(TextColor.color(DepthsUtils.STEELSAGE)))
			.add(" - For ")
			.addDuration(STEELSAGE_DURATION)
			.add("s, ride an invincible horse. While on the horse, any melee damage you would take is negated, and you deal ")
			.addPercent(a -> STEELSAGE_PROJ_DAMAGE_MULTIPLIER[rarity - 1], STEELSAGE_PROJ_DAMAGE_MULTIPLIER[rarity - 1], false, true)
			.add(" more projectile damage.");
	}

	private static Description<ColorSplash> getWindwalkerDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<ColorSplash>(color)
			.add(Component.text("\nWindwalker").color(TextColor.color(DepthsUtils.WINDWALKER)))
			.add(" - For ")
			.addDuration(WINDWALKER_DURATION)
			.add("s, gain ")
			.addPercent(a -> WINDWALKER_SPEED[rarity - 1], WINDWALKER_SPEED[rarity - 1], false, true)
			.add(" speed. Additionally, every time you swap hands, you initiate a Wind Walk, gaining ")
			.addDuration(WINDWALKER_IFRAMES)
			.add("s of Invincibility Frames, launching yourself, applying levitation and ")
			.addPercent(WINDWALKER_VULNERABILITY)
			.add(" vulnerability, and stunning non-Boss mobs hit for 1.5s. This secondary effect has a cooldown of ")
			.addDuration(WINDWALKER_LAND_COOLDOWN)
			.add("s, which starts counting down from the moment the Wind Walk ends.");
	}
}
