package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseShatter;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellConditionalTeleport;
import com.playmonumenta.plugins.bosses.spells.SpellPurgeNegatives;
import com.playmonumenta.plugins.bosses.spells.SpellRemoveLevitation;
import com.playmonumenta.plugins.bosses.spells.frostgiant.ArmorOfFrost;
import com.playmonumenta.plugins.bosses.spells.frostgiant.GiantStomp;
import com.playmonumenta.plugins.bosses.spells.frostgiant.RingOfFrost;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellAirGolemStrike;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellFrostRift;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellFrostbite;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellFrostedIce;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellGlacialPrison;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellGreatswordSlam;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellHailstorm;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellSpinDown;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellTitanicRupture;
import com.playmonumenta.plugins.bosses.spells.frostgiant.UltimateSeismicRuin;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.scriptedquests.growables.GrowableAPI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public final class FrostGiant extends SerializedLocationBossAbilityGroup {
	public static final String identityTag = "boss_frostgiant";
	public static final int detectionRange = 80;
	public static final int frostedIceDuration = 30;
	/**
	 * Includes the whole arena platform and much of the surrounding air
	 */
	public static final int ARENA_RADIUS = 40;
	public static final int ARENA_FLOOR_Y = 74;
	/**
	 * The arena is roughly a squircle and this is the "length" of one side for use with spells that travel distances
	 */
	public static final int ARENA_LENGTH = 56;
	/**
	 * Depth of arena platform (including unplayable area) for use with Seismic Ruin
	 */
	public static final int ARENA_DEPTH = 10;
	public static final Material ICE_TYPE = Material.FROSTED_ICE;
	public @Nullable
	static FrostGiant mInstance;
	public boolean mCastStomp = true;
	public boolean mFrostArmorActive = true;
	public boolean mPreventTargetting = false;

	private static final int HAILSTORM_RADIUS = 16;
	private static final int PHASE_1_HP = 100;
	private static final int PHASE_2_HP = 66;
	private static final int PHASE_3_HP = 33;
	private static final int FINAL_CHILL_HP = 15;
	private static final int MAX_HEALTH = 5012;
	private static final double SCALING_X = 0.6;
	private static final double SCALING_Y = 0.35;
	private static final String PLAYER_ANTICHEESE_SLOWNESS_SRC = "FrostGiantAnticheeseSlowness";
	private static final String GOLEM_FREEZE_EFFECT_NAME = "FrostGiantGolemPercentSpeedEffect";
	private static final Particle.DustOptions LIGHT_BLUE_COLOR = new Particle.DustOptions(Color.fromRGB(0, 255, 247), 1.0f);
	private final UltimateSeismicRuin mRuin;
	private final World mWorld;
	private final Location mStartLoc;
	private boolean mFightOver = false;
	private boolean mCutsceneDone = false;
	private @Nullable LivingEntity mTargeted;
	private int mPlayerCount;
	private int mPhase = 0;
	private double mDefenseScaling;
	private ItemStack[] mArmor;
	private ItemStack mMainhand;
	private ItemStack mOffhand;

	public FrostGiant(final Plugin plugin, final LivingEntity boss, final Location spawnLoc, final Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);
		mInstance = this;
		mWorld = mBoss.getWorld();
		mBoss.addScoreboardTag("Boss");
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Constants.TICKS_PER_SECOND * 9999, 0));

		/* These lines are solely to get the compiler to shut up about nulls. Eldrask's armor/weapon is handled later */
		final EntityEquipment equipment = Objects.requireNonNull(mBoss.getEquipment());
		mArmor = equipment.getArmorContents();
		mMainhand = equipment.getItemInMainHand();
		mOffhand = equipment.getItemInOffHand();

		/* If the arena is ever moved this will break horribly but otherwise this saves a ton of calculations */
		mStartLoc = new Location(mWorld, -1486, 75, 143);
		final Location northCardinal = mStartLoc.clone().add(0, 0, -31.0);
		final Location southCardinal = mStartLoc.clone().add(0, 0, 31.0);
		final Location eastCardinal = mStartLoc.clone().add(31.0, 0, 0);
		final Location westCardinal = mStartLoc.clone().add(-31.0, 0, 0);
		mRuin = new UltimateSeismicRuin(mPlugin, mBoss, mStartLoc, northCardinal, southCardinal, eastCardinal, westCardinal);

		mPlayerCount = getArenaParticipants().size();
		mDefenseScaling = BossUtils.healthScalingCoef(mPlayerCount, SCALING_X, SCALING_Y);

		/* Fire immunity and prevent on-fire visual (does not prevent Inferno from applying damage) */
		new BukkitRunnable() {
			@Override
			public void run() {
				mBoss.setFireTicks(0);
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		/* Targetting system - forcefully targets a nearby player if the boss has no target
		 * After targetting the same player for 30 seconds, play a sound and change targets after 1/2 a second */
		new BukkitRunnable() {
			final Mob mBossAsMob = (Mob) mBoss;
			List<Player> mPlayers = new ArrayList<>();
			int mT = 0;

			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
					return;
				}

				mPlayers = (List<Player>) getArenaParticipants();
				mPlayers.removeIf(player -> Objects.equals(player, mTargeted));
				if (!mCutsceneDone || mPreventTargetting || mPlayers.isEmpty()) {
					return;
				}

				if (mBossAsMob.getTarget() == null || !mBossAsMob.getTarget().equals(mTargeted)) {
					mT = 0;
					mTargeted = mBossAsMob.getTarget();

					if (mTargeted instanceof Player) {
						((Player) mTargeted).playSound(mTargeted.getLocation(), Sound.ENTITY_POLAR_BEAR_WARNING, SoundCategory.HOSTILE, 2.0f, 0.5f);
						mTargeted.sendMessage(Component.text("Eldrask focuses his gaze on you...", NamedTextColor.AQUA));
					}
				} else if (mT >= Constants.TICKS_PER_SECOND * 30 && mBossAsMob.getTarget().equals(mTargeted)) {
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						mTargeted = mPlayers.get(FastUtils.RANDOM.nextInt(mPlayers.size()));
						((Player) mTargeted).playSound(mTargeted.getLocation(), Sound.ENTITY_POLAR_BEAR_WARNING, SoundCategory.HOSTILE, 2.0f, 0.5f);
						mTargeted.sendMessage(Component.text("Eldrask switches his gaze to you...", NamedTextColor.AQUA));
					}, 5);

					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						mT = 0;
						mBossAsMob.setTarget(mTargeted);
					}, 10);
				}
				mT += 5;
			}
		}.runTaskTimer(mPlugin, 0, 5);

		/* Anti-Sleep */
		new BukkitRunnable() {
			Collection<Player> mPlayers = getArenaParticipants();

			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
					return;
				}

				mPlayers = getArenaParticipants();
				mPlayers.removeIf(player -> !player.isSleeping());
				mPlayers.forEach(player -> {
					DamageUtils.damage(mBoss, player, DamageEvent.DamageType.TRUE, EntityUtils.getMaxHealth(player) * 0.9,
						null, true, false, "Song of the Sleepers");
					com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player, PLAYER_ANTICHEESE_SLOWNESS_SRC,
						new PercentSpeed(Constants.TICKS_PER_SECOND * 15, -0.3, PLAYER_ANTICHEESE_SLOWNESS_SRC));
					player.sendMessage(Component.text("YOU DARE MOCK OUR BATTLE?", NamedTextColor.DARK_AQUA));
					mWorld.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_DEATH, SoundCategory.HOSTILE, 1, 0.85f);
				});
			}
		}.runTaskTimer(mPlugin, 0, 5);

		/* TODO: Implement charge bar for Shatter (requires a minor rework to ChargeUpManager) */
		final SpellBaseShatter shatter = new SpellBaseShatter(com.playmonumenta.plugins.Plugin.getInstance(), mBoss, 1.5 * HAILSTORM_RADIUS,
			Constants.TICKS_PER_SECOND * 7, (int) (Constants.TICKS_PER_SECOND * 2.5), 4, 20, ARENA_FLOOR_Y, Material.CRIMSON_HYPHAE,
			/* Get living entity targets */
			() -> (List<? extends LivingEntity>) getArenaParticipants(),
			/* Start "aesthetics" */
			(LivingEntity launcher) -> freezeGolems(),
			/* Warning "aesthetics" */
			(LivingEntity launcher, Location loc, float soundPitch) ->
				mWorld.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.HOSTILE, 3.0f, soundPitch),
			/* Launch "aesthetics" */
			(final LivingEntity launcher, final Location loc) -> {
				mWorld.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 3.0f, 0.5f);
				mWorld.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 3.0f, 0.5f);

				unfreezeGolems();
				final List<Player> players = (List<Player>) getArenaParticipants();
				if (players.size() > 1) {
					((Mob) launcher).setTarget(players.get(FastUtils.RANDOM.nextInt(players.size())));
				}
			},
			/* Hit action */
			(final LivingEntity launcher, final LivingEntity target, final Location location) -> {
				DamageUtils.damage(launcher, target, DamageEvent.DamageType.BLAST, 25, null, true, false, "Shatter");
				MovementUtils.knockAway(launcher.getLocation(), target, 3.0f, 0.5f, false);
				AbilityUtils.silencePlayer((Player) target, Constants.TICKS_PER_SECOND * 5);
				mWorld.playSound(location, Sound.ITEM_TRIDENT_HIT_GROUND, SoundCategory.HOSTILE, 0.75f, 0.5f);
				mWorld.playSound(location, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.HOSTILE, 0.5f, 0.5f);

				/* If in Final Heat, add FGShatterLP tag for Shattered Yet Standing advancement */
				if (EntityUtils.getMaxHealth(launcher) * 0.15 > launcher.getHealth()) {
					target.addScoreboardTag("FGShatterLP");
				}
			});

		final SpellManager phase1Spells = new SpellManager(Arrays.asList(
			shatter,
			new SpellAirGolemStrike(mPlugin, this),
			new SpellGlacialPrison(mPlugin, this),
			new RingOfFrost(mPlugin, this, 12, mStartLoc)
		));

		final SpellManager phase2Spells = new SpellManager(Arrays.asList(
			shatter,
			new SpellAirGolemStrike(mPlugin, this),
			new SpellGreatswordSlam(mPlugin, this, frostedIceDuration - 5, 90, mStartLoc),
			new SpellGreatswordSlam(mPlugin, this, frostedIceDuration - 5, 90, mStartLoc),
			new SpellSpinDown(mPlugin, this, mStartLoc),
			new SpellSpinDown(mPlugin, this, mStartLoc)
		));

		final SpellManager phase3Spells = new SpellManager(Arrays.asList(
			shatter,
			new SpellTitanicRupture(mPlugin, this),
			new SpellFrostRift(mPlugin, this),
			new SpellGreatswordSlam(mPlugin, this, Constants.TICKS_PER_SECOND, 90, mStartLoc)
		));

		/* This ain't a final heat this is a final chill */
		final SpellManager finalChillSpells = new SpellManager(Arrays.asList(
			shatter,
			new SpellTitanicRupture(mPlugin, this),
			new SpellFrostRift(mPlugin, this),
			new SpellGreatswordSlam(mPlugin, this, Constants.TICKS_PER_SECOND, 60, mStartLoc)
		));

		final SpellConditionalTeleport conditionalTeleport = new SpellConditionalTeleport(mBoss, mStartLoc,
			b -> b.getLocation().getBlock().getType() == Material.BEDROCK
				|| b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK
				|| b.getLocation().getBlock().getType() == Material.WATER
				|| b.getLocation().getY() - mStartLoc.getY() < -6
				|| mStartLoc.distance(b.getLocation()) > ARENA_RADIUS);

		/* TODO: Eldrask lacks sfx and could benefit from boss_sound effects (requires boss_sound rewrite) */
		final List<Spell> basePassives = Arrays.asList(
			new SpellFrostbite(mPlugin, this),
			new SpellHailstorm(mPlugin, mBoss, HAILSTORM_RADIUS, mStartLoc),
			new GiantStomp(mPlugin, this),
			new SpellBlockBreak(mBoss, 3, 15, 3, 0, 75, false, true,
				true, false, false, false, Material.AIR),
			new SpellRemoveLevitation(mBoss),
			conditionalTeleport
		);

		final List<Spell> phase1Passives = new ArrayList<>(Arrays.asList(
			new ArmorOfFrost(mPlugin, this, 2, true),
			new SpellPurgeNegatives(mBoss, Constants.TICKS_PER_SECOND * 4)
		));
		phase1Passives.addAll(basePassives);

		final List<Spell> phase2Passives = new ArrayList<>(Arrays.asList(
			new ArmorOfFrost(mPlugin, this, 2, true),
			new SpellPurgeNegatives(mBoss, Constants.TICKS_PER_SECOND * 3),
			new SpellFrostedIce(this)
		));
		phase2Passives.addAll(basePassives);

		final List<Spell> phase3Passives = new ArrayList<>(Arrays.asList(
			new ArmorOfFrost(mPlugin, this, 1, true),
			new SpellPurgeNegatives(mBoss, Constants.TICKS_PER_SECOND * 2),
			new SpellFrostedIce(this)
		));
		phase3Passives.addAll(basePassives);

		final List<Spell> finalChillPassives = new ArrayList<>(Arrays.asList(
			new ArmorOfFrost(mPlugin, this, 1, false),
			new SpellPurgeNegatives(mBoss, Constants.TICKS_PER_SECOND * 2),
			new SpellFrostedIce(this)
		));
		finalChillPassives.addAll(basePassives);

		final HashMap<Integer, BossHealthAction> events = new HashMap<>();
		events.put(PHASE_1_HP, mBoss -> {
			mPhase++;
			mPreventTargetting = false;
			sendDialogue("YOU... SHOULD HAVE NOT COME HERE... PERISH...", NamedTextColor.DARK_AQUA, true);
			sendDialogue("An inpenetrable armor forms around Eldrask.", NamedTextColor.AQUA, false);
			changeMainhandItem(new ItemStack(Material.BONE), "Frost Giant's Staff");
		});

		events.put(PHASE_2_HP, mBoss -> {
			phaseChangeTasks(phase1Passives);
			changePhase(phase2Spells, phase2Passives, null);
			mRuin.run();
		});

		events.put(PHASE_3_HP, mBoss -> {
			phaseChangeTasks(phase2Passives);
			changePhase(phase3Spells, phase3Passives, null);
			mRuin.run();
		});

		events.put(FINAL_CHILL_HP, mBoss -> {
			phaseChangeTasks(phase3Passives);
			changePhase(finalChillSpells, finalChillPassives, null);
			mRuin.run();
			mRuin.run();
		});

		final BossBarManager bossBar = new BossBarManager(mBoss, detectionRange, BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_10, events, false);

		/* Draw Hailstorm particles before the fight begins as a hint for players */
		new BukkitRunnable() {
			final Creature mC = (Creature) mBoss;

			@Override
			public void run() {
				final Location particleLoc = mStartLoc.clone().add(0, 0.2, 0);
				new PPCircle(Particle.REDSTONE, particleLoc, HAILSTORM_RADIUS - 0.75).count(60).delta(0.1).extra(1).data(LIGHT_BLUE_COLOR);
				new PPCircle(Particle.CLOUD, particleLoc, HAILSTORM_RADIUS + 5).count(30).delta(2).extra(0.075);

				mC.setTarget(null);
				if (mCutsceneDone) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 10);

		/* Big frost cloud */
		new BukkitRunnable() {
			final Location mParticleLoc = mStartLoc.clone().add(0, 1, 0);
			double mRadius = 0;

			@Override
			public void run() {
				mRadius += 1.5;
				new PPCircle(Particle.CLOUD, mParticleLoc, mRadius).countPerMeter(1).delta(1).extra(0.35).spawnAsEntityActive(mBoss);
				if (mRadius >= 40) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 1, 1);

		/* Summon FG statue, more particles, and make the boss active */
		new BukkitRunnable() {
			@Override
			public void run() {
				try {
					GrowableAPI.grow("FrostGiantStatue", mStartLoc, 1, 2, false);
				} catch (final Exception e) {
					MMLog.warning(() -> "[FrostGiant] Failed to grow the FrostGiantStatue growable! It may be missing or not loaded");
				}

				new BukkitRunnable() {
					int mTicks = 0;
					int mRotation = 0;

					@Override
					public void run() {
						for (int y = 0; y <= 12; y += 3) {
							final Location loc1 = mStartLoc.clone().add(0.5 + FastUtils.cosDeg(mRotation) * 3, y, -0.5 + FastUtils.sinDeg(mRotation) * 3);
							final Location loc2 = mStartLoc.clone().add(0.5 + FastUtils.cosDeg(mRotation + 180) * 3, y,
								-0.5 + FastUtils.sinDeg(mRotation + 180) * 3);

							new PartialParticle(Particle.SPELL_INSTANT, loc1).count(5).delta(0.1).extra(0).spawnAsEntityActive(mBoss);
							new PartialParticle(Particle.SPELL_INSTANT, loc2).count(5).delta(0.1).extra(0).spawnAsEntityActive(mBoss);
						}
						mRotation += 10;
						if (mRotation >= 360) {
							mRotation = 0;
						}

						if (mTicks % Constants.HALF_TICKS_PER_SECOND == 0) {
							new PartialParticle(Particle.BLOCK_CRACK, mStartLoc, 40, 2, 0.35, 2, 0.25,
								Material.COARSE_DIRT.createBlockData()).spawnAsEntityActive(mBoss);
							new PartialParticle(Particle.BLOCK_CRACK, mStartLoc, 75, 5, 0.35, 5, 0.25,
								Material.COARSE_DIRT.createBlockData()).spawnAsEntityActive(mBoss);
							new PartialParticle(Particle.EXPLOSION_NORMAL, mStartLoc, 15, 5, 0.35, 5, 0.15).spawnAsEntityActive(mBoss);
							new PartialParticle(Particle.CLOUD, mStartLoc, 20, 5, 0.35, 5, 0.15).spawnAsEntityActive(mBoss);
						}

						if (mTicks % Constants.TICKS_PER_SECOND == 0) {
							mWorld.playSound(mStartLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 3, 0.25f);
						}

						if (mTicks >= Constants.TICKS_PER_SECOND * 8) {
							mWorld.playSound(mStartLoc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1.5f, 0.5f);
							this.cancel();
						}
						mTicks += 2;
					}
				}.runTaskTimer(mPlugin, 0, 2);

				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					mCutsceneDone = true;
					mBoss.setGravity(true);
					mBoss.setAI(true);
					mBoss.setInvulnerable(false);
					mBoss.setInvisible(false);
					Objects.requireNonNull(mBoss.getEquipment()).setArmorContents(mArmor);
					mBoss.getEquipment().setItemInMainHand(mMainhand);
					mBoss.getEquipment().setItemInOffHand(mOffhand);
					mBoss.removePotionEffect(PotionEffectType.INVISIBILITY);
					com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(mBoss, PercentDamageReceived.GENERIC_NAME);
					getArenaParticipants().forEach(player -> MessagingUtils.sendBoldTitle(player,
						Component.text("Eldrask", NamedTextColor.AQUA), Component.text("The Waking Giant", NamedTextColor.BLUE)));

					final Location blockLoc = mStartLoc.clone();
					for (double y = mStartLoc.getY(); y <= mStartLoc.getY() + 15; y++) {
						for (double x = mStartLoc.getX() - 5; x <= mStartLoc.getX() + 5; x++) {
							for (double z = mStartLoc.getZ() - 5; z <= mStartLoc.getZ() + 5; z++) {
								blockLoc.set(x, y, z);
								blockLoc.getBlock().setType(Material.AIR);
							}
						}
					}

					mWorld.playSound(mStartLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 5.0f, 0.5f);
					mWorld.playSound(mStartLoc, Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 5.0f, 0.75f);

					constructBoss(phase1Spells, phase1Passives, detectionRange, bossBar, Constants.TICKS_PER_SECOND * 10);
					mBoss.teleport(mStartLoc);
				}, Constants.TICKS_PER_SECOND * 10);

				/* Big frost cloud again */
				new BukkitRunnable() {
					final Location mParticleLoc = mStartLoc.clone().add(0, 1, 0);
					double mRadius = 0;

					@Override
					public void run() {
						mRadius += 1.5;
						new PPCircle(Particle.CLOUD, mParticleLoc, mRadius).countPerMeter(1).delta(1).extra(0.35).spawnAsEntityActive(mBoss);
						if (mRadius >= 40) {
							this.cancel();
						}
					}
				}.runTaskTimer(mPlugin, Constants.TICKS_PER_SECOND * 10, 1);
			}
		}.runTaskLater(mPlugin, Constants.TICKS_PER_SECOND * 2);
	}

	public Collection<Player> getArenaParticipants() {
		final Location arenaCenter = mStartLoc.clone();
		arenaCenter.setY(ARENA_FLOOR_Y - 4); /* Height of bedrock layer to account for possible player locations */
		return new Hitbox.UprightCylinderHitbox(arenaCenter, 25, ARENA_RADIUS).getHitPlayers(true);
	}

	@Override
	public void death(@Nullable final EntityDeathEvent event) {
		final Collection<Player> players = getArenaParticipants();
		if (players.isEmpty()) {
			return;
		}

		final List<Spell> passives = getPassives();
		if (passives != null) {
			for (final Spell sp : passives) {
				if (sp instanceof ArmorOfFrost) {
					((ArmorOfFrost) sp).stopSkill();
				}
			}
		}

		mFightOver = true;
		BossUtils.endBossFightEffects(mBoss, players, Constants.TICKS_PER_SECOND * 40, true, false);
		changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
		teleport(mStartLoc);
		sendDialogue("THIS EARTH... WAS OURS ONCE... WE SHAPED IT...", NamedTextColor.DARK_AQUA, true);
		new PPCircle(Particle.CLOUD, mStartLoc.clone().add(0, 1, 0), 3).countPerMeter(1).delta(1)
			.extra(0.35).spawnAsEntityActive(mBoss);

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mBoss.remove();
			//Instantly spawn the FG statue
			try {
				GrowableAPI.grow("FrostGiantStatue", mStartLoc, 1, 300, false);
			} catch (final Exception e) {
				MMLog.warning(() -> "[FrostGiant] Failed to grow the FrostGiantStatue growable! It may be missing or not loaded");
			}
			sendDialogue("DO NOT LET IT... PERISH WITH ME... THE SONG MUST NOT GO... UNSUNG...", NamedTextColor.DARK_AQUA, true);
			mWorld.playSound(mStartLoc, Sound.ENTITY_WITHER_DEATH, SoundCategory.HOSTILE, 3.0f, 0.5f);
		}, Constants.TICKS_PER_SECOND * 3);

		//Initiate the growable "melt" which converts the blocks of the giant into barriers
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			try {
				GrowableAPI.grow("FrostGiantStatueBarrier2", mStartLoc.clone().add(0, 13, 1), 1, 2, false);
			} catch (final Exception e) {
				MMLog.warning(() -> "[FrostGiant] Failed to grow the FrostGiantStatueBarrier2 growable! It may be missing or not loaded");
			}
		}, Constants.TICKS_PER_SECOND * 4);

		/* Big frost cloud again */
		new BukkitRunnable() {
			final Location mParticleLoc = mStartLoc.clone().add(0, 1, 0);
			double mRadius = 0;

			@Override
			public void run() {
				mRadius += 1.5;
				new PPCircle(Particle.CLOUD, mParticleLoc, mRadius).countPerMeter(1).delta(1).extra(0.35).spawnAsEntityActive(mBoss);
				if (mRadius >= 40) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, Constants.TICKS_PER_SECOND * 3, 1);

		new BukkitRunnable() {
			int mTicks = 0;
			float mPitch = 2f;
			int mRotation = 360;
			//The end totem green particle sequence
			boolean mEndingParticles = false;

			@Override
			public void run() {
				if (mTicks <= Constants.TICKS_PER_SECOND * 2) {
					if (mTicks % 10 == 0) {
						mWorld.playSound(mStartLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 0);
					}
					new PartialParticle(Particle.EXPLOSION_LARGE, mStartLoc.clone().add(0, 5, 0), 1, 1, 5, 1).minimumCount(1).spawnAsEntityActive(mBoss);
				}

				if (mTicks >= Constants.TICKS_PER_SECOND * 4 && mTicks <= Constants.TICKS_PER_SECOND * 10 && mTicks % 2 == 0) {
					mWorld.playSound(mStartLoc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.HOSTILE, 3, mPitch);

					for (int y = 0; y <= 12; y += 3) {
						final Location loc1 = mStartLoc.clone().add(0.5 + FastUtils.cosDeg(mRotation) * 3, y, -0.5 + FastUtils.sinDeg(mRotation) * 3);
						final Location loc2 = mStartLoc.clone().add(0.5 + FastUtils.cosDeg(mRotation + 180) * 3, y,
							-0.5 + FastUtils.sinDeg(mRotation + 180) * 3);

						new PartialParticle(Particle.SPELL_WITCH, loc1).count(5).delta(0.1).extra(0).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.SPELL_WITCH, loc2).count(5).delta(0.1).extra(0).spawnAsEntityActive(mBoss);
					}
					mRotation -= 10;
					if (mRotation <= 0) {
						mRotation = 360;
					}
				}
				mPitch -= 0.025f;

				if (mTicks >= Constants.TICKS_PER_SECOND * 4 && mTicks <= Constants.TICKS_PER_SECOND * 10 && mTicks % 10 == 0) {
					mWorld.playSound(mStartLoc, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 2, FastUtils.RANDOM.nextFloat());
					mWorld.playSound(mStartLoc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 2, mPitch);
					new PartialParticle(Particle.BLOCK_CRACK, mStartLoc, 100, 2, 0.35, 2, 0.25,
						Material.BLUE_ICE.createBlockData()).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.BLOCK_CRACK, mStartLoc, 100, 2, 0.35, 2, 0.25,
						Material.IRON_TRAPDOOR.createBlockData()).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.DRAGON_BREATH, mStartLoc.clone().add(0, 1, 0)).count(25).delta(1).extra(0.25).spawnAsEntityActive(mBoss);
				}

				if (!mEndingParticles && mTicks >= Constants.TICKS_PER_SECOND * 10) {
					new PartialParticle(Particle.VILLAGER_HAPPY, mStartLoc.clone().add(0, 5, 0), 300, 1, 5, 1, 0.25).spawnAsEntityActive(mBoss);
					mEndingParticles = true;
				}

				if (mTicks >= Constants.TICKS_PER_SECOND * 14) {
					//Delete barriers after cutscene melt
					final Location blockLoc = mStartLoc.clone();
					for (double x = mStartLoc.getX() - 5; x <= mStartLoc.getX() + 5; x++) {
						for (double y = mStartLoc.getY(); y <= mStartLoc.getY() + 15; y++) {
							for (double z = mStartLoc.getZ() - 5; z <= mStartLoc.getZ() + 5; z++) {
								blockLoc.set(x, y, z);
								blockLoc.getBlock().setType(Material.AIR);
							}
						}
					}

					this.cancel();

					players.forEach(player -> {
						MessagingUtils.sendBoldTitle(player, Component.text("VICTORY", NamedTextColor.AQUA),
							Component.text("Eldrask, The Waking Giant", NamedTextColor.DARK_AQUA));
						player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.HOSTILE, 1, Constants.Note.C4.mPitch);
					});
					mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 1, 1);
	}

	public static boolean testHitByIcicle(final BoundingBox icicleBoundingBox) {
		if (mInstance != null && mInstance.mBoss.isValid() && !mInstance.mBoss.isDead() && icicleBoundingBox.overlaps(mInstance.mBoss.getBoundingBox())) {
			final List<Spell> passives = mInstance.getPassives();
			if (passives != null) {
				for (final Spell sp : passives) {
					if (sp instanceof ArmorOfFrost) {
						((ArmorOfFrost) sp).hitByIcicle();
						return true;
					}
				}
			}
		}
		return false;
	}

	private void phaseChangeTasks(final List<Spell> passives) {
		/* This iterates through the previous passive list instead of the current one to make sure Armor of Frost gets stopped */
		for (final Spell passive : passives) {
			if (passive instanceof ArmorOfFrost) {
				((ArmorOfFrost) passive).stopSkill();
				break;
			}
		}
		teleport(mStartLoc);
		changeArmorPhase(mBoss.getEquipment(), false);
		mPhase++;
		mFrostArmorActive = true;
		mPreventTargetting = false;
		mBoss.setAI(true);

		switch (mPhase) {
			case 2 -> {
				sendDialogue("THE FROST WILL CONSUME YOU...", NamedTextColor.DARK_AQUA, true);
				sendDialogue("The permafrost shield reforms around the giant, blocking damage dealt once more.", NamedTextColor.AQUA, false);
				changeMainhandItem(new ItemStack(Material.IRON_SWORD), "Frost Giant's Greatsword");
			}
			case 3 -> {
				sendDialogue("THE SONG WILL PREVAIL... ALL WILL SUCCUMB TO THE BITTER COLD...", NamedTextColor.DARK_AQUA, true);
				sendDialogue("The permafrost shield reforms again.", NamedTextColor.AQUA, false);
				changeMainhandItem(new ItemStack(Material.IRON_AXE), "Frost Giant's Crusher");
			}
			case 4 -> {
				sendDialogue("I... WILL NOT... BE THE END... OF THE SONG!", NamedTextColor.DARK_AQUA, true);
				sendDialogue("The permafrost shield reforms a final time.", NamedTextColor.AQUA, false);
				changeMainhandItem(new ItemStack(Material.IRON_HOE), "Frost Giant's Crescent");
			}
			default ->
				MMLog.warning(() -> "[FrostGiant] mPhase is somehow " + mPhase + ". The boss is still working but he didn't do his dialogue or weapon correctly");
		}
	}

	public void changeArmorPhase(final EntityEquipment equip, final boolean cracked) {
		if (cracked) {
			equip.setHelmet(modifyItemName(equip.getHelmet(), "Cracked Giant's Crown", false));
			equip.setChestplate(modifyItemName(equip.getChestplate(), "Cracked Giant's Courage", false));
			equip.setLeggings(modifyItemName(equip.getLeggings(), "Cracked Giant's Leggings", false));
			equip.setBoots(modifyItemName(equip.getBoots(), "Cracked Giant's Boots", false));
		} else {
			equip.setHelmet(modifyItemName(equip.getHelmet(), "Frost Giant's Crown", false));
			equip.setChestplate(modifyItemName(equip.getChestplate(), "Frost Giant's Courage", false));
			equip.setLeggings(modifyItemName(equip.getLeggings(), "Frost Giant's Leggings", false));
			equip.setBoots(modifyItemName(equip.getBoots(), "Frost Giant's Boots", false));
		}
	}

	private void changeMainhandItem(final ItemStack baseItem, final String itemName) {
		modifyItemName(baseItem, itemName, true);
		final ItemMeta baseItemMeta = baseItem.getItemMeta();
		baseItemMeta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(UUID.randomUUID(),
			"generic.attack_damage", 0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
		baseItem.setItemMeta(baseItemMeta);
		Objects.requireNonNull(mBoss.getEquipment()).setItemInMainHand(baseItem);
	}

	private ItemStack modifyItemName(final ItemStack baseItem, final String itemName, final boolean underlineName) {
		final ItemMeta baseItemMeta = baseItem.getItemMeta();
		baseItemMeta.displayName(Component.text(itemName, NamedTextColor.AQUA, TextDecoration.BOLD)
			.decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.UNDERLINED, underlineName));
		baseItem.setItemMeta(baseItemMeta);
		ItemUtils.setPlainName(baseItem, itemName);

		return baseItem;
	}

	@Override
	public void bossChangedTarget(final EntityTargetEvent event) {
		if (!mCutsceneDone) {
			event.setCancelled(true);
			event.setTarget(null);
		}
	}

	@Override
	public void onDamage(final DamageEvent event, final LivingEntity damagee) {
		//The "default" Giant attacks need to be cancelled so it does not trigger evasion
		if (event.getDamage() <= 0) {
			event.setCancelled(true);
		}
	}

	@Override
	public void onHurtByEntityWithSource(final DamageEvent event, final Entity damager, final LivingEntity source) {
		/* Make sure the damager originates from the arena to prevent spectator projectile cheese */
		if (source.getLocation().distance(mStartLoc) > ARENA_RADIUS) {
			event.setCancelled(true);
			return;
		}

		/* Allow Armor of Frost to have priority when setting the event's damage */
		if (!mFrostArmorActive) {
			event.setFlatDamage(event.getFlatDamage() / mDefenseScaling);
		}

		if (!mFrostArmorActive && source instanceof Player) {
			((Player) source).playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_HURT, SoundCategory.HOSTILE, 1,
				0.5f + FastUtils.randomFloatInRange(0.0f, 0.129961f));
		}

		if (damager instanceof Projectile) {
			new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					mBoss.setVelocity(new Vector(0, 0, 0));
					mTicks++;
					if (mTicks > 2) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	@Override
	public boolean hasNearbyPlayerDeathTrigger() {
		return true;
	}

	@Override
	public void nearbyPlayerDeath(final PlayerDeathEvent event) {
		mPlayerCount = getArenaParticipants().size();
		mDefenseScaling = BossUtils.healthScalingCoef(mPlayerCount, SCALING_X, SCALING_Y);
	}

	@Override
	public void nearbyBlockBreak(final BlockBreakEvent event) {
		if (event.getBlock().getType() != ICE_TYPE) {
			return;
		}

		final Location eventLoc = event.getBlock().getLocation();
		final Location testLoc = new Location(eventLoc.getWorld(), 0, 0, 0);
		final double testLocX = eventLoc.getX();
		final double testLocY = eventLoc.getY();
		final double testLocZ = eventLoc.getZ();

		for (double x = testLocX - 1; x <= testLocX + 1; x++) {
			for (double y = testLocY - 1; y <= testLocY + 1; y++) {
				for (double z = testLocZ - 1; z <= testLocZ + 1; z++) {
					testLoc.set(x, y, z);
					if (testLoc.getBlock().getType() == ICE_TYPE) {
						testLoc.getBlock().setType(Material.CRACKED_STONE_BRICKS);
					}
				}
			}
		}

		event.setCancelled(true);
	}

	@Override
	public boolean hasNearbyBlockBreakTrigger() {
		return true;
	}

	public void teleport(final Location loc) {
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1.0f, 0.5f);
		new PartialParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.CLOUD, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1).spawnAsEntityActive(mBoss);
		mBoss.teleport(loc);
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1.0f, 0.5f);
		new PartialParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1).spawnAsEntityActive(mBoss);

		delayHailstormDamage(Constants.TICKS_PER_SECOND * 4);
	}

	public void delayHailstormDamage(final int delay) {
		this.getPassives().forEach(passive -> {
			if (passive instanceof SpellHailstorm) {
				((SpellHailstorm) passive).delayDamage(delay);
			}
		});
	}

	@Override
	public void init() {
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, MAX_HEALTH);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, detectionRange);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1);
		mBoss.setHealth(MAX_HEALTH);
		mBoss.setPersistent(true);
		mBoss.setGravity(false);
		mBoss.setAI(false);
		mBoss.setInvulnerable(true);
		mBoss.setInvisible(true);
		mBoss.setSilent(true);
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Constants.TICKS_PER_SECOND * 60, 0));
		com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mBoss, PercentDamageReceived.GENERIC_NAME,
			new PercentDamageReceived(Constants.TICKS_PER_SECOND * 60, -1.0));

		final EntityEquipment equipment = Objects.requireNonNull(mBoss.getEquipment());
		mArmor = equipment.getArmorContents();
		mMainhand = equipment.getItemInMainHand();
		mOffhand = equipment.getItemInOffHand();
		equipment.clear();

		final Collection<Player> initPlayers = getArenaParticipants();
		final String initDialogue = initPlayers.size() <= 1 ? "WHAT CHILD OF ALRIC DARES ENTER THIS PLACE..." :
			"WHAT CHILDREN OF ALRIC DARE ENTER THIS PLACE...";
		sendDialogue(initDialogue, NamedTextColor.DARK_AQUA, true);
		mWorld.playSound(mStartLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 5.0f, 0.5f);
		mWorld.playSound(mStartLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 5.0f, 0.5f);

		// Disable White Tesseract for the duration of the fight. The tag is cleared in SQ login/death files and the win mcfunction
		initPlayers.forEach(player -> player.addScoreboardTag("WhiteTessDisabled"));

		// Ice Cold Vanilla advancement
		new BukkitRunnable() {
			int mTicks = 0;
			final HashSet<UUID> mHs = new HashSet<>();
			Collection<Player> mPlayers = Collections.emptyList();

			@Override
			public void run() {
				mPlayers = getArenaParticipants();
				if (mTicks == 0) {
					mPlayers.forEach(p -> {
						if (AbilityUtils.isClassless(p)) {
							mHs.add(p.getUniqueId());
						}
					});
				}

				mPlayers.forEach(p -> {
					if (mHs.contains(p.getUniqueId()) && !AbilityUtils.isClassless(p)) {
						mHs.remove(p.getUniqueId());
					}
				});

				if (mFightOver) {
					this.cancel();
					mPlayers.forEach(p -> {
						if (mHs.contains(p.getUniqueId()) && AbilityUtils.isClassless(p)) {
							AdvancementUtils.grantAdvancement(p, "monumenta:challenges/r2/fg/ice_cold_vanilla");
						}
					});
					return;
				}

				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
					return;
				}
				mTicks += 10;
			}
		}.runTaskTimer(mPlugin, 0, 10);
	}

	@Override
	public void bossHitByProjectile(final ProjectileHitEvent event) {
		mBoss.setVelocity(new Vector(0, 0, 0));
	}

	public void freezeGolems() {
		mCastStomp = false;
		for (final LivingEntity mob : EntityUtils.getNearbyMobs(mStartLoc, ARENA_RADIUS)) {
			if (mob.getType() == EntityType.IRON_GOLEM) {
				com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mob, GOLEM_FREEZE_EFFECT_NAME,
					new PercentSpeed(Constants.TICKS_PER_SECOND * 20, -1, GOLEM_FREEZE_EFFECT_NAME));
				GlowingManager.startGlowing(mob, NamedTextColor.WHITE, Constants.TICKS_PER_SECOND * 20,
					GlowingManager.BOSS_SPELL_PRIORITY, null, "FrostGiantGolemFreeze");
			}
		}
	}

	public void unfreezeGolems() {
		mCastStomp = true;
		for (final LivingEntity mob : EntityUtils.getNearbyMobs(mStartLoc, ARENA_RADIUS)) {
			com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(mob, GOLEM_FREEZE_EFFECT_NAME);
			GlowingManager.clear(mob, "FrostGiantGolemFreeze");
		}
	}

	public static ChargeUpManager defaultChargeUp(final LivingEntity boss, final int chargeTime, final String text) {
		return new ChargeUpManager(boss, chargeTime, Component.text(text, NamedTextColor.DARK_AQUA), BossBar.Color.BLUE,
			BossBar.Overlay.PROGRESS, detectionRange);
	}

	public void sendDialogue(final String msg, final NamedTextColor color, final boolean playSound) {
		getArenaParticipants().forEach(player -> player.sendMessage(Component.text(msg, color)));
		if (playSound) {
			mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 1.5f, 0.5f + FastUtils.randomFloatInRange(0.0f, 0.129961f));
		}
	}
}
