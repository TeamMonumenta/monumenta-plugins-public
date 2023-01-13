package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBossBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellPurgeNegatives;
import com.playmonumenta.plugins.bosses.spells.lich.SpellAutoAttack;
import com.playmonumenta.plugins.bosses.spells.lich.SpellCrystalRespawn;
import com.playmonumenta.plugins.bosses.spells.lich.SpellDarkOmen;
import com.playmonumenta.plugins.bosses.spells.lich.SpellDesecrate;
import com.playmonumenta.plugins.bosses.spells.lich.SpellDiesIrae;
import com.playmonumenta.plugins.bosses.spells.lich.SpellDimensionDoor;
import com.playmonumenta.plugins.bosses.spells.lich.SpellEdgeKill;
import com.playmonumenta.plugins.bosses.spells.lich.SpellFinalCrystal;
import com.playmonumenta.plugins.bosses.spells.lich.SpellFinalHeatMech;
import com.playmonumenta.plugins.bosses.spells.lich.SpellFinalLaser;
import com.playmonumenta.plugins.bosses.spells.lich.SpellFinalParticle;
import com.playmonumenta.plugins.bosses.spells.lich.SpellFinalSwarm;
import com.playmonumenta.plugins.bosses.spells.lich.SpellGraspingHands;
import com.playmonumenta.plugins.bosses.spells.lich.SpellGravityWell;
import com.playmonumenta.plugins.bosses.spells.lich.SpellMiasma;
import com.playmonumenta.plugins.bosses.spells.lich.SpellRaiseDead;
import com.playmonumenta.plugins.bosses.spells.lich.SpellSalientOfDecay;
import com.playmonumenta.plugins.bosses.spells.lich.SpellShadowRealm;
import com.playmonumenta.plugins.bosses.spells.lich.SpellSoulShackle;
import com.playmonumenta.plugins.cosmetics.VanityManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import com.playmonumenta.scriptedquests.growables.GrowableAPI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Creature;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

/*
 * Lich:
 * I'm too lazy so here's the formal write-up for reference :) -Fwap
 * https://docs.google.com/document/d/149Wa83eyxaJ_EuOn1oCZl4ivoDzj-tw5m04i_xVA87M/edit?usp=sharing
 */
public final class Lich extends BossAbilityGroup {
	public static final String identityTag = "boss_lich";
	public static final int detectionRange = 55;
	public static final int mShieldMin = 5;
	private static final int mCeiling = 35;
	private int mCounter = 0;

	private final Location mSpawnLoc;
	private final Location mEndLoc;
	private static final String START_TAG = "lich_center";
	private static final String mShieldCrystal = "DeathCrystal";
	private static final String mCrystalShield = "CrystalShield";
	private final String mFinalCrystal = "WarpedCrystal";
	@SuppressWarnings("NullAway.Init")
	private static LivingEntity mStart;
	private final LivingEntity mKey;
	private static final double mL = 26.5;
	private static final double mY = 14.5;
	private static final double mS = 8.5;
	private int mPhase;
	private final Collection<EnderCrystal> mCrystal = new ArrayList<>();
	private final List<Location> mCrystalLoc = new ArrayList<>();
	private final List<Location> mPassive2Loc = new ArrayList<>();
	private final List<Location> mTower0 = new ArrayList<>();
	private final List<Location> mTower1 = new ArrayList<>();
	private final List<Location> mTower2 = new ArrayList<>();
	private final List<Location> mTower3 = new ArrayList<>();
	private final List<List<Location>> mTowerGroup = new ArrayList<>();
	private final List<Location> mTp = new ArrayList<>();
	private static final List<Player> mCursed = new ArrayList<>();
	private static boolean mActivated = false;
	private static boolean mGotHit = false;
	private boolean mTrigger = false;
	private boolean mDefeated = false;
	private boolean mCutscene = false;
	private static boolean mDead = false;
	private static boolean mPhaseCD = false;
	private int mPlayerCount;
	private double mDefenseScaling;
	private static final int MAX_HEALTH = 4000;
	private static final int PHYLACT_HP = 1250;
	private static final double SCALING_X = 0.7;
	private static final double SCALING_Y = 0.575;

	private double mPhylactHealth;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) ->
			new Lich(plugin, boss, spawnLoc, endLoc));
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public Lich(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);

		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;
		mPlayerCount = BossUtils.getPlayersInRangeForHealthScaling(mSpawnLoc, detectionRange, mCeiling);
		mDefenseScaling = BossUtils.healthScalingCoef(mPlayerCount, SCALING_X, SCALING_Y);

		mStart = boss;
		for (Entity e : mBoss.getNearbyEntities(detectionRange, detectionRange, detectionRange)) {
			if (e.getScoreboardTags().contains(START_TAG) && e instanceof LivingEntity) {
				mStart = (LivingEntity) e;
				break;
			}
		}

		// load all the crystal spawn locations
		mCrystalLoc.add(mStart.getLocation().clone().add(mL, mY, mS));
		mCrystalLoc.add(mStart.getLocation().clone().add(mS, mY, mL));
		mCrystalLoc.add(mStart.getLocation().clone().add(-mS, mY, mL));
		mCrystalLoc.add(mStart.getLocation().clone().add(-mL, mY, mS));
		mCrystalLoc.add(mStart.getLocation().clone().add(-mL, mY, -mS));
		mCrystalLoc.add(mStart.getLocation().clone().add(-mS, mY, -mL));
		mCrystalLoc.add(mStart.getLocation().clone().add(mS, mY, -mL));
		mCrystalLoc.add(mStart.getLocation().clone().add(mL, mY, -mS));

		// load all phase 2 change animation
		mPassive2Loc.add(mStart.getLocation().clone().add(26, 26.5, 26));
		mPassive2Loc.add(mStart.getLocation().clone().add(-26, 26.5, 26));
		mPassive2Loc.add(mStart.getLocation().clone().add(26, 26.5, -26));
		mPassive2Loc.add(mStart.getLocation().clone().add(-26, 26.5, -26));

		// load all phase 4 key locations
		mTower0.add(mStart.getLocation().clone().add(26, -0.5, 26));
		mTower0.add(mStart.getLocation().clone().add(26, 17.5, 26));
		mTower1.add(mStart.getLocation().clone().add(-26, -0.5, 26));
		mTower1.add(mStart.getLocation().clone().add(-26, 17.5, 26));
		mTower2.add(mStart.getLocation().clone().add(-26, -0.5, -26));
		mTower2.add(mStart.getLocation().clone().add(-26, 17.5, -26));
		mTower3.add(mStart.getLocation().clone().add(26, -0.5, -26));
		mTower3.add(mStart.getLocation().clone().add(26, 17.5, -26));

		// tp loc if boss didn't get hit for 5s
		mTp.add(mStart.getLocation().clone().add(9, 6, 9));
		mTp.add(mStart.getLocation().clone().add(-9, 6, 9));
		mTp.add(mStart.getLocation().clone().add(9, 6, -9));
		mTp.add(mStart.getLocation().clone().add(-9, 6, -9));

		SpellDiesIrae.initDmg(0);

		//summon key mob in shadow realm
		mKey = Objects.requireNonNull((LivingEntity) LibraryOfSoulsIntegration.summon(mStart.getLocation().subtract(0, 41, 0), "ShadowPhylactery"));
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "team empty lichphylactery");
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "team empty crystal");
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "team empty Hekawt");
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "team modify lichphylactery color white");
		UUID keyUUID = mKey.getUniqueId();
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "team join lichphylactery " + keyUUID);
		mPhylactHealth = PHYLACT_HP * mDefenseScaling;
		EntityUtils.setAttributeBase(mKey, Attribute.GENERIC_MAX_HEALTH, mPhylactHealth);
		mKey.setHealth(mPhylactHealth);

		mDefeated = false;
		mActivated = false;
		mDead = false;
		new BukkitRunnable() {
			int mT = 0;
			int mColor = 0;
			boolean mKeyDead = false;

			@Override
			public void run() {
				//sleep-dow realm
				if (!mDefeated) {
					for (Player p : playersInRange(mSpawnLoc, detectionRange, true)) {
						p.removePotionEffect(PotionEffectType.LEVITATION);
						if (p.isSleeping()) {
							BossUtils.bossDamagePercent(mBoss, p, Math.max(0.5, SpellDiesIrae.getDmg()));
							SpellDimensionDoor.getWealmed(mPlugin, p, mBoss, p.getLocation(), false);
						}
					}
				}

				//not getting hit for 9s tps to 4 locations
				//let boss stand on the high ground after ~~holy chest~~ dies irae
				if (mGotHit) {
					mT = 0;
					mGotHit = false;
				} else if (!mCutscene) {
					mT += 5;
					if (mT >= 20 * 9
						    && mBoss.getLocation().getY() <= mStart.getLocation().getY() + 3
						    && !hasRunningSpellOfType(SpellDiesIrae.class, SpellDesecrate.class, SpellDarkOmen.class, SpellSalientOfDecay.class)) {
						mT = 0;
						Collections.shuffle(mTp);
						World world = mBoss.getWorld();
						world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 2.0f, 1.0f);
						world.playSound(mTp.get(0), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 2.0f, 1.0f);
						mBoss.teleport(mTp.get(0));
					}
				}

				// key glow color + prevent log spam
				double health = mKey.getHealth();
				if (health / mPhylactHealth <= 0.34 && mColor == 1) {
					mColor++;
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "team modify lichphylactery color red");
				} else if (health / mPhylactHealth <= 0.67 && mColor == 0) {
					mColor++;
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "team modify lichphylactery color yellow");
				}

				// key death
				if ((mKey.isDead() || !mKey.isValid()) && !mKeyDead) {
					mKeyDead = true;
					List<Player> players = playersInRange(mBoss.getLocation(), detectionRange, true);
					List<Player> shadowed = SpellDimensionDoor.getShadowed();
					for (Player pl : shadowed) {
						if (!players.contains(pl)) {
							players.add(pl);
						}
					}
					for (Player p : players) {
						p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, SoundCategory.HOSTILE, 3.0f, 0.5f);
						p.sendMessage(ChatColor.LIGHT_PURPLE + "WHAT IS THIS... PAIN? I HAVE NOT FELT PAIN IN ETERNITY...");
					}
				}

				if (mActivated || mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
					if (mKey.isValid()) {
						mKey.remove();
					}
				}

				//destroy vehicle to prevent cheese
				if (mBoss.getVehicle() != null) {
					mBoss.getVehicle().remove();
				}
			}

		}.runTaskTimer(mPlugin, 20 * 23, 5);

		new BukkitRunnable() {

			@Override
			public void run() {
				// invulnerable crystals if ghast is present
				for (Location loc : mCrystalLoc) {
					Collection<EnderCrystal> c = loc.getNearbyEntitiesByType(EnderCrystal.class, 3);
					if (c.size() > 0) {
						Collection<Ghast> g = loc.getNearbyEntitiesByType(Ghast.class, 3);
						if (g.size() > 0) {
							for (EnderCrystal e : c) {
								e.setInvulnerable(true);
							}
						} else {
							for (EnderCrystal e : c) {
								e.setInvulnerable(false);
							}
						}
					}
				}

				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
					if (mKey.isValid()) {
						mKey.remove();
					}
				}
			}

		}.runTaskTimer(mPlugin, 0, 2);

		/*
		 * Active starts here
		 */

		SpellManager phase1Spells = new SpellManager(
			Arrays.asList(
				new SpellGraspingHands(mPlugin, mBoss),
				new SpellRaiseDead(mPlugin, mBoss, mStart.getLocation(), detectionRange, mCeiling),
				new SpellSalientOfDecay(mPlugin, mBoss),
				new SpellSoulShackle(mPlugin, mBoss, mStart.getLocation(), detectionRange, mCeiling)));

		SpellManager phase2Spells = new SpellManager(
			Arrays.asList(
				new SpellGraspingHands(mPlugin, mBoss),
				new SpellSoulShackle(mPlugin, mBoss, mStart.getLocation(), detectionRange, mCeiling),
				new SpellRaiseDead(mPlugin, mBoss, mStart.getLocation(), detectionRange, mCeiling),
				new SpellDiesIrae(mPlugin, mBoss, mKey, mStart.getLocation(), detectionRange, mCeiling, mCrystalLoc, mShieldCrystal)));

		SpellManager phase3Spells = new SpellManager(
			Arrays.asList(
				new SpellGraspingHands(mPlugin, mBoss),
				new SpellDesecrate(mPlugin, mBoss),
				new SpellGravityWell(mPlugin, mBoss, mStart.getLocation(), detectionRange),
				new SpellDiesIrae(mPlugin, mBoss, mKey, mStart.getLocation(), detectionRange, mCeiling, mCrystalLoc, mShieldCrystal),
				new SpellDarkOmen(mPlugin, mBoss, mStart.getLocation(), detectionRange) // ult + dialogue?
			));

		/*
		 * Passive starts here
		 */

		List<Spell> passiveSpells = Arrays.asList(
			new SpellBossBlockBreak(mBoss, mStart.getLocation().getY(), 1, 3, 1, false, false),
			new SpellMiasma(mBoss, mStart.getLocation(), mStart.getLocation().getY(), detectionRange),
			new SpellDimensionDoor(mPlugin, mBoss, mStart.getLocation(), detectionRange),
			new SpellPurgeNegatives(mBoss, 4 * 20),
			new SpellShadowRealm(mStart.getLocation(), detectionRange),
			new SpellEdgeKill(mBoss, mStart.getLocation()),
			new SpellAutoAttack(mPlugin, this, mBoss, mStart.getLocation(), 20 * 4, detectionRange, mCeiling, 1));

		List<Spell> phase2PassiveSpells = Arrays.asList(
			new SpellBossBlockBreak(mBoss, mStart.getLocation().getY(), 1, 3, 1, false, false),
			new SpellMiasma(mBoss, mStart.getLocation(), mStart.getLocation().getY(), detectionRange),
			new SpellDimensionDoor(mPlugin, mBoss, mStart.getLocation(), detectionRange),
			new SpellPurgeNegatives(mBoss, 3 * 20),
			new SpellShadowRealm(mStart.getLocation(), detectionRange),
			new SpellEdgeKill(mBoss, mStart.getLocation()),
			new SpellAutoAttack(mPlugin, this, mBoss, mStart.getLocation(), 20 * 4, detectionRange, mCeiling, 2),
			new SpellCrystalRespawn(mPlugin, this, mStart.getLocation(), detectionRange, mCrystalLoc, mShieldCrystal));

		List<Spell> phase3PassiveSpells = Arrays.asList(
			new SpellBossBlockBreak(mBoss, mStart.getLocation().getY(), 1, 3, 1, false, false),
			new SpellMiasma(mBoss, mStart.getLocation(), mStart.getLocation().getY(), detectionRange),
			new SpellDimensionDoor(mPlugin, mBoss, mStart.getLocation(), detectionRange),
			new SpellPurgeNegatives(mBoss, 2 * 20),
			new SpellShadowRealm(mStart.getLocation(), detectionRange),
			new SpellEdgeKill(mBoss, mStart.getLocation()),
			new SpellAutoAttack(mPlugin, this, mBoss, mStart.getLocation(), 20 * 4, detectionRange, mCeiling, 3),
			new SpellCrystalRespawn(mPlugin, this, mStart.getLocation(), detectionRange, mCrystalLoc, mShieldCrystal));

		Map<Integer, BossHealthAction> events = new HashMap<>();
		events.put(100, mBoss -> {
			mCutscene = false;
			List<Player> players = playersInRange(mStart.getLocation(), detectionRange, true);
			for (Player p : players) {
				p.sendMessage(ChatColor.LIGHT_PURPLE + "YOUR RUDENESS SHALL NOT GO UNPUNISHED.");
			}
		});

		events.put(75, mBoss -> {
			// dialogue?
			forceCastSpell(SpellRaiseDead.class);
		});

		events.put(66, mBoss -> {
			mPhaseCD = true;
			changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
			mCutscene = true;
			World world = mBoss.getWorld();
			knockback(world, 10);
			mBoss.setInvulnerable(true);
			mBoss.setAI(false);
			mBoss.setGravity(false);
			// dialogue?
			String[] dio = new String[] {
				"I PROMISED THAT IF I SAW YOU AGAIN, I WOULD DESTROY YOU.",
				"WHY DO YOU PERSIST?"
			};

			// phase transition animation
			new BukkitRunnable() {
				int mT;
				int mDio = 0;
				Location mCenter = mStart.getLocation();

				@Override
				public void run() {
					if (mT % 60 == 0 && mDio < dio.length) {
						for (Player p : playersInRange(mStart.getLocation(), detectionRange, true)) {
							p.sendMessage(ChatColor.LIGHT_PURPLE + dio[mDio]);
						}
						mDio++;
					}
					mT++;
					if (mT == 20 * 1) {
						new PartialParticle(Particle.CLOUD, mBoss.getLocation(), 20, 0.1, 0.1, 0.1, 0.05).spawnAsBoss();
						world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 2.0f, 1.0f);
						mBoss.teleport(mCenter.clone().add(0, 14, 0));
						new PartialParticle(Particle.CLOUD, mBoss.getLocation(), 20, 0.1, 0.1, 0.1, 0.05).spawnAsBoss();
						world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 2.0f, 1.0f);
						switchArmor("LichTwo");
					}
					if (mT == 20 * 3) {
						world.playSound(mBoss.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 5.0f, 1.0f);
					}
					if (mT == 20 * 5) {
						for (int i = 0; i < mCrystalLoc.size(); i++) {
							Location startLoc = mBoss.getLocation().add(0, 1.5, 0);
							Location endLoc = mCrystalLoc.get(i);
							double distance = startLoc.distance(endLoc);
							Vector vec = LocationUtils.getDirectionTo(endLoc.clone().add(0, 1, 0), startLoc);
							new BukkitRunnable() {
								int mInc = 0;
								@Nullable Entity mSkull;

								@Override
								public void run() {
									if (mInc == 0) {
										world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 3.0f, 1.0f);
										try {
											mSkull = EntityUtils.getSummonEntityAt(
												startLoc.clone().add(vec.multiply(2 / distance)),
												EntityType.WITHER_SKULL,
												"{power:[" + vec.getX() * distance / 9.5 + ","
													+ vec.getY() * distance / 9.5 + ","
													+ vec.getZ() * distance / 9.5 + "]}");
											((WitherSkull) mSkull).setCharged(true);
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
									if (mSkull == null) {
										cancel();
										return;
									}

									new PartialParticle(Particle.SOUL_FIRE_FLAME, mSkull.getLocation(), 4,
										0, 0, 0, 0.03).spawnAsEntityActive(boss);

									// slight delay to prevent exploding too early in case the wither skull somehow despawned
									if ((mInc >= 25 && !mSkull.isValid())) {
										if (mSkull.isValid()) {
											mSkull.remove();
										}
										List<Block> badBlockList = new ArrayList<>();
										Location testLoc = endLoc.clone();
										// get blocks around loc
										Location bLoc = endLoc.clone();

										for (int x = -3; x <= 2; x++) {
											testLoc.setX(bLoc.getX() + x);
											for (int z = -3; z <= 2; z++) {
												testLoc.setZ(bLoc.getZ() + z);
												for (int y = 0; y <= 5; y++) {
													testLoc.setY(bLoc.getY() + y);
													badBlockList.add(testLoc.getBlock());
												}
											}
										}
										// delete fake static crystal
										for (Block b : badBlockList) {
											b.setType(Material.AIR);
										}
										new PartialParticle(Particle.EXPLOSION_NORMAL, endLoc.clone().add(0, 2, 0), 96,
											2, 2, 2, 0).spawnAsEntityActive(boss);
										world.playSound(endLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2.5f, 1.0f);
										// spawn the crystal for holy chest
										List<Location> loc = new ArrayList<>();
										loc.add(endLoc);
										spawnCrystal(loc, 1, mShieldCrystal);
										this.cancel();
									}
									mInc++;
								}
							}.runTaskTimer(mPlugin, 2L * i, 1);
						}
					}
					// finish the animation
					if (mT >= 20 * 9) {
						this.cancel();
						mCutscene = false;
						mPhase = 2;
						new PartialParticle(Particle.CLOUD, mBoss.getLocation(), 20, 0.1, 0.1, 0.1, 0.05).spawnAsBoss();
						world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 2.0f, 1.0f);
						mBoss.teleport(mCenter.clone());
						new PartialParticle(Particle.CLOUD, mBoss.getLocation(), 20, 0.1, 0.1, 0.1, 0.05).spawnAsBoss();
						world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 2.0f, 1.0f);

						mBoss.setInvulnerable(false);
						mBoss.setAI(true);
						mBoss.setGravity(true);
						changePhase(phase2Spells, phase2PassiveSpells, null);

						//disallow dies irae instant cast
						new BukkitRunnable() {

							@Override
							public void run() {
								mPhaseCD = false;
							}

						}.runTaskLater(mPlugin, 5 * 20);
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		});

		events.put(50, mBoss -> {
			forceCastSpell(SpellDiesIrae.class);
			//prevent overkilling before ability is cast
			mPhase = 3;
		});

		events.put(33, mBoss -> {
			mPhaseCD = true;
			changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
			mCutscene = true;
			World world = mBoss.getWorld();
			knockback(world, 10);
			mBoss.setInvulnerable(true);
			mBoss.setAI(false);
			mBoss.setGravity(false);
			// dialogue?
			String[] dio = new String[] {
				"ENOUGH. YOU CANNOT DEFEAT ME.",
				"MY SEARCH IS FAR TOO IMPORTANT FOR YOUR MEDDLING.",
				"THE VEIL IS RIPPING APART AND I SEEK THE SOURCE.",
				"REALITY STILL HIDES FROM MY GRASP."
			};
			// phase change animation
			new BukkitRunnable() {
				int mT = 0;
				int mDio = 0;
				Location mCenter = mStart.getLocation();

				@Override
				public void run() {
					if (mT % 60 == 0 && mDio < dio.length) {
						for (Player p : playersInRange(mStart.getLocation(), detectionRange, true)) {
							p.sendMessage(ChatColor.LIGHT_PURPLE + dio[mDio]);
						}
						mDio++;
					}
					if (mT < 8 && mT % 2 == 0) {
						Collections.shuffle(mPassive2Loc);
						Location l = mPassive2Loc.get(0);
						String cmd = "summon minecraft:lightning_bolt " + l.getX() + " " + l.getY() + " " + l.getZ();
						Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
					}
					if (mT == 30) {
						new PartialParticle(Particle.CLOUD, mBoss.getLocation(), 20, 0.1, 0.1, 0.1, 0.05).spawnAsBoss();
						world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 2.0f, 1.0f);
						mBoss.teleport(mCenter.clone().add(0, 14, 0));
						new PartialParticle(Particle.CLOUD, mBoss.getLocation(), 20, 0.1, 0.1, 0.1, 0.05).spawnAsBoss();
						world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 2.0f, 1.0f);
						switchArmor("LichThree");
					}
					if (mT == 20 * 2.5) {
						world.playSound(mBoss.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, SoundCategory.HOSTILE, 3.0f, 1.0f);
					}
					if (mT >= 20 * 2.5 && mT < 64 * 2 + 20 * 2.5 && mT % 2 == 0) {
						// blue flame from small pillar
						for (Location l : mCrystalLoc) {
							Location startLoc = l.clone();
							Location endLoc = mBoss.getLocation().add(0, 1.5, 0);
							Vector vec = LocationUtils.getVectorTo(endLoc, startLoc);

							Location pLoc = startLoc.add(vec.multiply((mT - 20 * 2.5) / 128));
							new PartialParticle(Particle.SOUL_FIRE_FLAME, pLoc, 3, 0, 0, 0, 0.03).spawnAsBoss();
						}
					}
					// put out torches after swirl/flame particle reach boss
					// also make a blast noise from boss
					if (mT == 64 * 2 + 20 * 3.5) {
						// blast noise
						world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 5.0f, 0.5f);
						world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 5.0f, 0.5f);
						new PartialParticle(Particle.CAMPFIRE_COSY_SMOKE, mBoss.getLocation(), 150, 0, 0, 0, 0.75).spawnAsBoss();
						// put out torches by growables
						try {
							GrowableAPI.grow("LichNoFlamePxPz", mStart.getLocation().add(23, 21, 23), 1, 3, false);
							GrowableAPI.grow("LichNoFlameNxPz", mStart.getLocation().add(-23, 21, 23), 1, 3, false);
							GrowableAPI.grow("LichNoFlameNxNz", mStart.getLocation().add(-23, 21, -23), 1, 3, false);
							GrowableAPI.grow("LichNoFlamePxNz", mStart.getLocation().add(23, 21, -23), 1, 3, false);
						} catch (Exception e) {
							e.printStackTrace();
						}
						List<Player> players = playersInRange(mStart.getLocation(), detectionRange, true);
						for (Player p : players) {
							MovementUtils.knockAway(mBoss, p, 3, false);
						}
						// change terracotta to red nether bricks
						new BukkitRunnable() {
							Location mLoc = mStart.getLocation().subtract(0, 1, 0);
							int mT = 0;

							@Override
							public void run() {
								mT++;
								for (Block block : LocationUtils.getEdge(mLoc.clone().subtract(mT, 0, mT), mLoc.clone().add(mT, 0, mT))) {
									if (block.getType() == Material.TERRACOTTA) {
										block.setType(Material.RED_NETHER_BRICKS);
									}
								}
								if (mT >= 45) {
									this.cancel();
								}
							}

						}.runTaskTimer(mPlugin, 0, 1);
					}
					// end animation
					if (mT >= 64 * 2 + 20 * 4.5 + 45) {
						for (Location l : mPassive2Loc) {
							Location test = l.clone();
							for (int x = -6; x <= 6; x++) {
								test.setX(l.getX() + x);
								for (int z = -6; z <= 6; z++) {
									test.setZ(l.getZ() + z);
									for (int y = -7; y <= 4; y++) {
										test.setY(l.getY() + y + 0.2);
										Block block = test.getBlock();
										if (block.getType() == Material.BARRIER) {
											block.setType(Material.AIR);
										}
									}
								}
							}
						}

						new PartialParticle(Particle.CLOUD, mBoss.getLocation(), 20, 0.1, 0.1, 0.1, 0.05).spawnAsBoss();
						world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 2.0f, 1.0f);
						mBoss.teleport(mCenter.clone());
						new PartialParticle(Particle.CLOUD, mBoss.getLocation(), 20, 0.1, 0.1, 0.1, 0.05).spawnAsBoss();
						world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 2.0f, 1.0f);

						mBoss.setInvulnerable(false);
						mBoss.setAI(true);
						mBoss.setGravity(true);
						mPhase = 4;
						changePhase(phase3Spells, phase3PassiveSpells, null);
						mCutscene = false;
						this.cancel();

						//disallow dies irae instant cast
						new BukkitRunnable() {

							@Override
							public void run() {
								mPhaseCD = false;
							}

						}.runTaskLater(mPlugin, 5 * 20);
					}
					mT++;
				}

			}.runTaskTimer(mPlugin, 10, 1);
		});

		events.put(30, mBoss -> {
			World world = mBoss.getWorld();
			world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 0.5f, 10.0f);
			Location southSpawn = mStart.getLocation();
			Location northSpawn = mStart.getLocation();
			southSpawn.setZ(southSpawn.getZ() + 23);
			northSpawn.setZ(northSpawn.getZ() - 23);
			for (Player p : playersInRange(mStart.getLocation(), detectionRange, true)) {
				p.sendMessage(ChatColor.LIGHT_PURPLE + "AKRHH ALMAWT DEFIES THEE. BREAK FREE, RIDERS OF THE VEIL! TO WAR!");
			}
			// south conquest - SkeletalHorse
			// north strife - AshenRemains
			LibraryOfSoulsIntegration.summon(southSpawn, "SkeletalHorse");
			LibraryOfSoulsIntegration.summon(northSpawn, "AshenRemains");
		});

		events.put(20, mBoss -> {
			World world = mBoss.getWorld();
			world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 0.5f, 10.0f);
			Location eastSpawn = mStart.getLocation();
			Location westSpawn = mStart.getLocation();
			eastSpawn.setX(eastSpawn.getX() + 23);
			westSpawn.setX(westSpawn.getX() - 23);
			for (Player p : playersInRange(mStart.getLocation(), detectionRange, true)) {
				p.sendMessage(ChatColor.LIGHT_PURPLE + "AGAIN I SPEAK THE CALL! AKRHH ALMAWT! RIDE FORTH, TORMENTED BEASTS - YOUR MASTER BIDS YOU RIDE!");
			}
			// east demise - RottenHorse
			// west judgement - ExpeditiusEvaluation
			LibraryOfSoulsIntegration.summon(eastSpawn, "RottenHorse");
			LibraryOfSoulsIntegration.summon(westSpawn, "ExpeditiusEvaluation");
		});

		events.put(10, mBoss -> {
			if (!mKey.isDead() || mKey.isValid()) {
				for (Player p : playersInRange(mStart.getLocation(), detectionRange, true)) {
					p.sendMessage(ChatColor.LIGHT_PURPLE + "THE SHADOWS STILL CLOAK MY SOUL. YOU WILL NEVER DESTROY MY BEING.");
				}
			}
		});

		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 1000, 0));
		UUID uuid = mBoss.getUniqueId();
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "team join Hekawt " + uuid);
		mBoss.setAI(false);
		mBoss.setSilent(true);
		mBoss.setInvulnerable(true);
		mBoss.teleport(mStart.getLocation().add(0, 17, 0));
		// new spawn animation
		String[] ani = new String[] {"loadstructure \"isles/lich/Spawn1\" ~-17 ~6 ~-17",
		                             "loadstructure \"isles/lich/Spawn2\" ~-17 ~6 ~-17",
		                             "loadstructure \"isles/lich/Spawn3\" ~-17 ~6 ~-17"};
		String[] end = new String[] {"loadstructure \"isles/lich/Spawn4\" ~-17 ~6 ~-17",
		                             "loadstructure \"isles/lich/clear\" ~-17 ~6 ~-17"};

		EntityEquipment equips = mBoss.getEquipment();
		ItemStack[] a = equips.getArmorContents();
		ItemStack m = equips.getItemInMainHand();
		ItemStack o = equips.getItemInOffHand();
		new BukkitRunnable() {

			@Override
			public void run() {
				mBoss.getEquipment().clear();
				List<Player> players = playersInRange(mStart.getLocation(), detectionRange, true);
				for (Player p : players) {
					p.removePotionEffect(PotionEffectType.GLOWING);
				}
				new BukkitRunnable() {
					int mT = 0;

					@Override
					public void run() {
						// spawn crack from prerecorded locations
						World world = mBoss.getWorld();
						if (mT < ani.length) {
							world.playSound(mBoss.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.HOSTILE, 10.0f, 0.75f);
							String cmd = "execute positioned " + mStart.getLocation().getX() + " "
								             + mStart.getLocation().getY() + " " + mStart.getLocation().getZ() + " run "
								             + ani[mT];
							Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
							mT++;
						} else {
							this.cancel();
							String cmd = "execute positioned " + mStart.getLocation().getX() + " "
								             + mStart.getLocation().getY() + " " + mStart.getLocation().getZ() + " run "
								             + end[0];
							Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
							// make boss visible
							new PartialParticle(Particle.END_ROD, mStart.getLocation().add(0, 17, 0), 750, 6, 6, 6, 0).spawnAsBoss();
							new PartialParticle(Particle.EXPLOSION_HUGE, mStart.getLocation().add(0, 18, 0), 10, 4, 4,
								4, 0).spawnAsEntityActive(boss);
							mBoss.removePotionEffect(PotionEffectType.INVISIBILITY);
							mBoss.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 14, 100));
							mBoss.getEquipment().setArmorContents(a);
							mBoss.getEquipment().setItemInMainHand(m);
							mBoss.getEquipment().setItemInOffHand(o);
							mBoss.setGlowing(true);
							mBoss.setGravity(false);
							mBoss.setAI(true);
							mBoss.setSilent(false);
							List<Player> players = playersInRange(mStart.getLocation(), detectionRange, true);
							for (Player p : players) {
								p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0));
							}
							world.playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.HOSTILE, 5.0f, 0.5f);

							new BukkitRunnable() {
								@Override
								public void run() {
									String cmd = "execute positioned " + mStart.getLocation().getX() + " "
										             + mStart.getLocation().getY() + " " + mStart.getLocation().getZ() + " run "
										             + end[1];
									Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
									world.playSound(mStart.getLocation().add(0, 17, 0), Sound.BLOCK_ENDER_CHEST_CLOSE,
										SoundCategory.HOSTILE, 10.0f, 0.5f);
								}
							}.runTaskLater(mPlugin, 50);

							// dialogue functions
							String[] dio = new String[] {
								"AH, THE SWEET SMELL OF THE DESERT. HOW I HAVE MISSED THIS.",
								"MY MONTHS AWAY FROM THE SANDS HAVE TAKEN A TOLL.",
								"NOW, I BELIEVE YOU HAVE DISTURBED MY SEARCH."
							};
							new BukkitRunnable() {
								int mIter = 0;

								@Override
								public void run() {
									if (mIter < dio.length) {
										for (Player p : playersInRange(mStart.getLocation(), detectionRange, true)) {
											p.sendMessage(ChatColor.LIGHT_PURPLE + dio[mIter].toUpperCase());
										}
										mIter++;
									} else {
										// start fight (need check)
										mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 5, 0));
										mBoss.removePotionEffect(PotionEffectType.WEAKNESS);
										mBoss.setGravity(true);
										mBoss.setInvulnerable(false);
										mPhase = 1;
										for (Player p : playersInRange(mStart.getLocation(), detectionRange, true)) {
											MessagingUtils.sendBoldTitle(p, ChatColor.DARK_GRAY + "Hekawt, The Eternal", ChatColor.GRAY + "Inheritor of Eternity");
											p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10f, 0.75f);
											p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 2 * 20, 2));
										}

										BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange,
											BarColor.PURPLE, BarStyle.SEGMENTED_10, events);
										constructBoss(phase1Spells, passiveSpells, detectionRange, bossBar);
										this.cancel();
									}
								}

							}.runTaskTimer(mPlugin, 20 * 2, 20 * 4);
						}
					}

				}.runTaskTimer(mPlugin, 20 * 1, 20 * 1);
			}
		}.runTaskLater(mPlugin, 1);
	}

	private void switchArmor(String armor) {
		LivingEntity e = (LivingEntity) LibraryOfSoulsIntegration.summon(mSpawnLoc.clone().subtract(0, 10, 0), armor);
		if (e != null) {
			Bukkit.getScheduler().runTask(mPlugin, () -> {
				EntityEquipment equip = e.getEquipment();
				mBoss.getEquipment().setArmorContents(equip.getArmorContents());
				mBoss.getEquipment().setItemInMainHand(equip.getItemInMainHand());
				mBoss.getEquipment().setItemInOffHand(equip.getItemInOffHand());
				e.remove();
			});
		}
	}

	private void knockback(World world, int r) {
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2.0f, 1.0f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 2.0f, 0f);
		for (Player player : playersInRange(mBoss.getLocation(), r, true)) {
			MovementUtils.knockAway(mBoss.getLocation(), player, 0.55f, false);
			player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 5, 1));
		}
	}

	// resurrection pt 1
	@Override
	public void nearbyPlayerDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		if (player.getLocation().distanceSquared(mSpawnLoc) > detectionRange * detectionRange
			    || player.getLocation().getY() - mSpawnLoc.getY() > mCeiling) {
			return;
		}
		mPlayerCount = BossUtils.getPlayersInRangeForHealthScaling(mSpawnLoc, detectionRange, mCeiling);
		mDefenseScaling = BossUtils.healthScalingCoef(mPlayerCount, SCALING_X, SCALING_Y);
		World world = player.getWorld();
		new PartialParticle(Particle.FALLING_DUST, player.getLocation().add(0, 1, 0), 10, 0.4, 0.45, 0.4,
			Material.MELON.createBlockData()).spawnAsBoss();
		new PartialParticle(Particle.SMOKE_NORMAL, player.getLocation().add(0, 1, 0), 30, 0.45, 0.45, 0.45, 0.15).spawnAsBoss();
		Location spawnLoc = player.getLocation();
		if (player.getLocation().getY() <= mStart.getLocation().getY()) {
			spawnLoc.setY(mStart.getLocation().getY());
		}

		// If the player got killed by a spectre, give the spectre owner the Player Slayer advancement
		if (player.getLastDamageCause() instanceof EntityDamageByEntityEvent damageByEntityEvent) {
			for (Player p : world.getPlayers()) {
				if (damageByEntityEvent.getDamager().getScoreboardTags().contains("Undead" + p.getName())) {
					CommandUtils.runCommandViaConsole("advancement grant " + p.getName() + " only monumenta:challenges/r2/lich/player_slayer");
					break;
				}
			}
		}

		summonSpectre(player, spawnLoc);
	}

	@Override
	public boolean hasNearbyPlayerDeathTrigger() {
		return true;
	}

	private boolean mAggro = true;
	private @Nullable Player mTarget = null;
	private double mTotalDamage = 0;

	@Override
	public void onHurt(DamageEvent event) {
		event.setDamage(event.getDamage() / mDefenseScaling);
		mGotHit = true;

		// ridiculous burst prevention, 1% above the next health action
		double damage = event.getDamage();
		if (mPhase == 1 && mBoss.getHealth() - damage <= MAX_HEALTH * 0.51) {
			event.setDamage(mBoss.getHealth() - MAX_HEALTH * 0.51);
		} else if (mPhase == 2 && mBoss.getHealth() - damage <= MAX_HEALTH * 0.34) {
			event.setDamage(mBoss.getHealth() - MAX_HEALTH * 0.34);
		} else if (mPhase == 3 && mBoss.getHealth() - damage <= MAX_HEALTH * 0.31) {
			event.setDamage(mBoss.getHealth() - MAX_HEALTH * 0.31);
		}

		// death check
		if (mBoss.getHealth() - event.getDamage() <= 0) {
			event.setCancelled(true);
			event.setDamage(0);
			mBoss.setHealth(100);
			if (!mActivated) {
				if (mKey != null && mKey.isValid()) {
					mBoss.setHealth(MAX_HEALTH * 0.15);


					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 5.0f, 0.75f);
					SpellManager phase3Spells = new SpellManager(
						Arrays.asList(
							new SpellGraspingHands(mPlugin, mBoss),
							new SpellDesecrate(mPlugin, mBoss),
							new SpellGravityWell(mPlugin, mBoss, mStart.getLocation(), detectionRange),
							new SpellDiesIrae(mPlugin, mBoss, mKey, mStart.getLocation(), detectionRange, mCeiling, mCrystalLoc, mShieldCrystal),
							new SpellDarkOmen(mPlugin, mBoss, mStart.getLocation(), detectionRange) // ult + dialogue?
						));

					SpellManager keyAlive = new SpellManager(
						Arrays.asList(
							new SpellDiesIrae(mPlugin, mBoss, mKey, mStart.getLocation(), detectionRange, mCeiling, mCrystalLoc, mShieldCrystal),
							new SpellSoulShackle(mPlugin, mBoss, mStart.getLocation(), detectionRange, mCeiling),
							new SpellRaiseDead(mPlugin, mBoss, mStart.getLocation(), detectionRange, mCeiling),
							new SpellDarkOmen(mPlugin, mBoss, mStart.getLocation(), detectionRange) // ult + dialogue?
						));

					List<Spell> phase3PassiveSpells = Arrays.asList(
						new SpellBossBlockBreak(mBoss, mStart.getLocation().getY(), 1, 3, 1, false, false),
						new SpellMiasma(mBoss, mStart.getLocation(), mStart.getLocation().getY(), detectionRange),
						new SpellDimensionDoor(mPlugin, mBoss, mStart.getLocation(), detectionRange),
						new SpellPurgeNegatives(mBoss, 2 * 20),
						new SpellShadowRealm(mStart.getLocation(), detectionRange),
						new SpellEdgeKill(mBoss, mStart.getLocation()),
						new SpellAutoAttack(mPlugin, this, mBoss, mStart.getLocation(), 20 * 4, detectionRange, mCeiling, 3),
						new SpellCrystalRespawn(mPlugin, this, mStart.getLocation(), detectionRange, mCrystalLoc, mShieldCrystal));

					changePhase(keyAlive, Collections.emptyList(), null);
					forceCastSpell(SpellDarkOmen.class);
					forceCastSpell(SpellSoulShackle.class);
					forceCastSpell(SpellRaiseDead.class);
					spawnCrystal(mCrystalLoc, 4, mShieldCrystal);
					for (Player p : playersInRange(mStart.getLocation(), detectionRange, true)) {
						p.sendMessage(ChatColor.LIGHT_PURPLE + "YOUR HASTE WILL BE YOUR DOWNFALL.");
					}

					new BukkitRunnable() {

						@Override
						public void run() {
							for (Player p : playersInRange(mStart.getLocation(), detectionRange, true)) {
								p.sendMessage(ChatColor.LIGHT_PURPLE + "YOU WILL NEVER DESTROY MY SOUL, YOU SEE. DIES IRAE! DIES ILLA!");
							}
							forceCastSpell(SpellDiesIrae.class);
							new BukkitRunnable() {

								@Override
								public void run() {
									changePhase(phase3Spells, phase3PassiveSpells, null);
								}

							}.runTaskLater(mPlugin, 20 * 15);
						}

					}.runTaskLater(mPlugin, 50);

				} else {
					mActivated = true;
					mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 100, 10));
					die();
					return;
				}
			}
		}

		LivingEntity source = event.getSource();

		// Custom Aggro
		if (mAggro) {
			mAggro = false;
			Player player = null;
			if (source instanceof Player) {
				player = (Player) source;
			}

			mTarget = player;

			new BukkitRunnable() {
				int mT = 0;

				@Override
				public void run() {
					mT++;
					if (mTarget != null) {
						((Creature) mBoss).setTarget(mTarget);
					}
					if (mT >= 20 * 6 || AbilityUtils.isStealthed(mTarget) || SpellDimensionDoor.getShadowed().contains(mTarget)) {
						this.cancel();
						mAggro = true;
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}

		//damage immunity if over 15 blocks
		if (source instanceof Player player && player.getLocation().distance(mBoss.getLocation()) > 15) {
			event.setCancelled(true);
			player.playSound(mBoss.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.HOSTILE, 1, 5);
			new PartialParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation(), 10, 0, 0, 0, 0.1).spawnAsBoss();
			player.sendMessage(ChatColor.AQUA + "Hekawt has formed a miasma shield around himself! Get closer to pierce through the shield!");
			//stop teleport
			return;
		}

		if (source != null) {
			// teleport
			mTotalDamage += event.getDamage();
			Location loc = mBoss.getLocation();
			if (mTotalDamage / EntityUtils.getMaxHealth(mBoss) >= 0.04
				    && !mActivated
				    && !mCutscene
				    && !hasRunningSpellOfType(SpellDiesIrae.class, SpellDesecrate.class, SpellDarkOmen.class, SpellSalientOfDecay.class)) {
				World world = mBoss.getWorld();
				mTotalDamage = 0;
				boolean locFound = false;
				while (!locFound) {
					double x = FastUtils.randomDoubleInRange(-16, 16);
					double z = FastUtils.randomDoubleInRange(-16, 16);
					Location newLoc = loc.clone().add(x, 0, z);
					newLoc.setY(mSpawnLoc.clone().getY());
					if (newLoc.distance(mSpawnLoc) < 30 && newLoc.getBlock().getType() == Material.AIR) {
						locFound = true;
						for (int i = 0; i < 50; i++) {
							Vector vec = LocationUtils.getDirectionTo(newLoc, loc);
							Location pLoc = mBoss.getEyeLocation();
							pLoc.add(vec.multiply(i * 0.5));
							if (pLoc.distance(mBoss.getEyeLocation()) > newLoc.distance(loc)) {
								break;
							}
							new PartialParticle(Particle.VILLAGER_ANGRY, pLoc, 1, 0, 0, 0, 0).spawnAsBoss();
						}
						Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
							// 10 ticks + final check
							Vector vector = LocationUtils.getDirectionTo(newLoc, loc);
							for (int i = 0; i < 32; i++) {
								Location pLoc = mBoss.getEyeLocation();
								pLoc.add(vector.multiply(i / 2));
								if (pLoc.distance(mBoss.getEyeLocation()) > newLoc.distance(loc)) {
									break;
								}
								BoundingBox box = BoundingBox.of(pLoc, 0.3, 0.3, 0.3);
								for (Player p : playersInRange(mBoss.getLocation(), detectionRange, true)) {
									if (p.getBoundingBox().overlaps(box)) {
										BossUtils.blockableDamage(mBoss, p, DamageEvent.DamageType.MAGIC, 20);
									}
								}
							}
							world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 2.0f, 1.0f);
							world.playSound(newLoc, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 2.0f, 1.0f);
							mBoss.teleport(newLoc);
						}, 10);
					}
				}
			}
		}
	}

	// resurrection pt 2
	public static LivingEntity summonSpectre(Player player, Location loc) {
		PlayerInventory inv = player.getInventory();
		String summonNbt;

		// get class
		String c = AbilityUtils.getClass(player);

		// set spawn nbt
		summonNbt = switch (c) {
			case "Mage" -> "MageUndead";
			case "Warlock" -> "WarlockUndead";
			case "Alchemist" -> "AlchUndead";
			case "Cleric" -> "ClericUndead";
			case "Warrior" -> "WarriorUndead";
			case "Rogue" -> "RogueUndead";
			case "Scout" -> "ScoutUndead";
			default -> "AdventurerUndead";
		};

		// summon undead + get undead
		LivingEntity undead = Objects.requireNonNull((LivingEntity) LibraryOfSoulsIntegration.summon(loc, summonNbt));

		NamedTextColor color = summonNbt.equals("AdventurerUndead") ? NamedTextColor.WHITE : NamedTextColor.GOLD;
		undead.customName(Component.text(player.getName(), color));

		undead.addScoreboardTag("LichSpectre");
		undead.addScoreboardTag("Undead" + player.getName());
		undead.setCustomNameVisible(true);

		VanityManager.VanityData vanityData = com.playmonumenta.plugins.Plugin.getInstance().mVanityManager.getData(player);
		ItemStack helm = null;
		ItemStack chest = null;
		ItemStack legs = null;
		ItemStack boots = null;
		if (inv.getHelmet() != null) {
			ItemStack vanityHelmet = vanityData.getEquipped(EquipmentSlot.HEAD);
			helm = ItemUtils.clone(vanityHelmet != null && !VanityManager.isInvisibleVanityItem(vanityHelmet) ? vanityHelmet : inv.getHelmet());
		}
		if (inv.getChestplate() != null) {
			ItemStack vanityChestplate = vanityData.getEquipped(EquipmentSlot.CHEST);
			chest = ItemUtils.clone(vanityChestplate != null && !VanityManager.isInvisibleVanityItem(vanityChestplate) ? vanityChestplate : inv.getChestplate());
		}
		if (inv.getLeggings() != null) {
			ItemStack vanityLeggings = vanityData.getEquipped(EquipmentSlot.LEGS);
			legs = ItemUtils.clone(vanityLeggings != null && !VanityManager.isInvisibleVanityItem(vanityLeggings) ? vanityLeggings : inv.getLeggings());
		}
		if (inv.getBoots() != null) {
			ItemStack vanityBoots = vanityData.getEquipped(EquipmentSlot.FEET);
			boots = ItemUtils.clone(vanityBoots != null && !VanityManager.isInvisibleVanityItem(vanityBoots) ? vanityBoots : inv.getBoots());
		}
		ItemStack[] items = new ItemStack[] {helm, chest, legs, boots};
		for (ItemStack item : items) {
			if (item == null) {
				continue;
			}
			ItemMeta meta = item.getItemMeta();
			// remove lore + ench
			if (meta.hasLore()) {
				meta.lore(Collections.emptyList());
			}
			if (meta.hasEnchants()) {
				meta.addEnchant(Enchantment.DURABILITY, 1, true);
			}
			item.setItemMeta(meta);
		}

		EntityEquipment equip = undead.getEquipment();
		if (equip != null) {
			equip.setHelmet(helm);
			equip.setChestplate(chest);
			equip.setLeggings(legs);
			equip.setBoots(boots);
		}

		return undead;
	}


	public static Location getLichSpawn() {
		return mStart.getLocation();
	}

	public static boolean phase3over() {
		return mActivated;
	}

	public static void spawnCrystal(List<Location> crystalLoc, double count, String nbt) {
		World world = mStart.getWorld();
		List<Location> missing = new ArrayList<>();
		// find pillars without crystal to spawn
		for (Location l : crystalLoc) {
			Collection<Entity> e = l.getWorld().getNearbyEntities(l, 1.5, 1.5, 1.5);
			e.removeIf(en -> !en.getType().equals(EntityType.ENDER_CRYSTAL));
			if (e.size() == 0) {
				missing.add(l);
			}
		}
		// spawn crystals
		while (count > 0) {
			// stop trying if all crystals are present
			if (missing.size() == 0) {
				count = 0;
			} else {
				// spawn crystal on random tower without crystal
				Collections.shuffle(missing);
				Location spawnLoc = missing.get(0);
				LibraryOfSoulsIntegration.summon(spawnLoc, nbt);
				if (nbt.equals(mShieldCrystal) && playersInRange(mStart.getLocation(), detectionRange, true).size() >= mShieldMin) {
					LibraryOfSoulsIntegration.summon(spawnLoc.clone().subtract(0, 1, 0), mCrystalShield);
				}
				world.playSound(spawnLoc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 5.0f, 1.0f);
				missing.remove(spawnLoc);
				count--;
			}
		}
	}

	public static void cursePlayer(Plugin plugin, Player p) {
		cursePlayer(plugin, p, 30);
	}

	public static void cursePlayer(Plugin plugin, Player p, int time) {
		// Add to scoreboard for AccursedOne scoreboard.
		int score = ScoreboardUtils.getScoreboardValue(p, "LichAccursedOne").orElse(0);
		ScoreboardUtils.setScoreboardValue(p, "LichAccursedOne", score + 1);

		//don't add repeat instances of cursed players
		if (!mCursed.contains(p)) {
			mCursed.add(p);
		}
		p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 1.0f, 0.5f);
		AbilityUtils.increaseHealingPlayer(p, 20 * time, -0.5, "CurseEffect");
		AbilityUtils.increaseDamageRecievedPlayer(p, 20 * time, 1.0, "CurseEffect");
		p.sendMessage(ChatColor.LIGHT_PURPLE + "I CAST DOWN DOOM UPON THEE, AND CURSE YOUR VERY BONES. YOU SHALL JOIN MY REVENANTS.");
		p.sendActionBar(Component.text("You are cursed! You take double damage for " + time + " Seconds.", NamedTextColor.DARK_RED));
		new BukkitRunnable() {
			int mT;

			@Override
			public void run() {
				mT += 10;
				new PartialParticle(Particle.SOUL, p.getLocation().add(0, 0.75, 0), 6, 0.3, 0.3, 0.3, 0.01).spawnAsBoss();
				if ((mT > 20 * time || p.isDead()) && mCursed.contains(p)) {
					this.cancel();
					mCursed.remove(p);
				} else if (!mCursed.contains(p)) {
					this.cancel();
				} else if (mT % 200 == 0) {
					p.sendActionBar(Component.text("Cursed for " + (time - mT / 20) + " seconds.", NamedTextColor.DARK_RED));
				}
			}

		}.runTaskTimer(plugin, 0, 10);
	}

	public static List<Player> getCursed() {
		return mCursed;
	}

	public static void removeCursed(Player p) {
		mCursed.remove(p);
	}

	public static void bossGotHit(boolean gotHit) {
		mGotHit = gotHit;
	}

	public static boolean getCD() {
		return mPhaseCD;
	}

	private void die() {
		mDefeated = true;
		List<Player> players = playersInRange(mBoss.getLocation(), detectionRange, true);
		if (players.isEmpty()) {
			return;
		}
		//force leave shadow realm
		if (SpellDimensionDoor.getShadowed().size() > 0) {
			for (Player p : SpellDimensionDoor.getShadowed()) {
				Location loc = p.getLocation();
				loc.setY(mStart.getLocation().getY());
				p.teleport(loc, PlayerTeleportEvent.TeleportCause.UNKNOWN);
			}
		}

		for (Player player : players) {
			PotionUtils.applyPotion(com.playmonumenta.plugins.Plugin.getInstance(), player, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 17, 10));
			PotionUtils.applyPotion(com.playmonumenta.plugins.Plugin.getInstance(), player, new PotionEffect(PotionEffectType.REGENERATION, 20 * 17, 1));
		}
		String[] dio1 = new String[] {
			"I... WILL... NOT... BE... DESTROYED...",
			"NO! I MUST... SPEAK... THE PARTING VEIL..."
		};
		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (mT < dio1.length) {
					for (Player p : playersInRange(mStart.getLocation(), detectionRange, true)) {
						p.sendMessage(ChatColor.LIGHT_PURPLE + dio1[mT].toUpperCase());
					}
					mT++;
				} else {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 3 * 20);

		mBoss.removePotionEffect(PotionEffectType.GLOWING);
		mBoss.setHealth(0.1);
		mBoss.setInvulnerable(true);
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 60, 10));
		mBoss.setAI(false);
		changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 10, 1);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 3, 1);
		// particles to look like kaul die
		new BukkitRunnable() {
			Location mLoc = mStart.getLocation().subtract(0, 0.5, 0);
			double mRotation = 0;
			double mRadius = 0;
			int mT = 0;

			@Override
			public void run() {
				mT++;
				mRadius = mT;
				for (int i = 0; i < 36; i++) {
					double radian1 = Math.toRadians(mRotation + (10 * i));
					mLoc.add(FastUtils.cos(radian1) * mRadius, 1, FastUtils.sin(radian1) * mRadius);
					new PartialParticle(Particle.CLOUD, mLoc, 3, 0.25, 0.25, 0.25, 0.025).spawnAsBoss();
					mLoc.subtract(FastUtils.cos(radian1) * mRadius, 1, FastUtils.sin(radian1) * mRadius);
				}
				if (mT >= 40) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
		new BukkitRunnable() {
			@Override
			public void run() {
				world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SKELETON_DEATH, SoundCategory.HOSTILE, 15, 1f);
				Location loc = mBoss.getLocation();
				// Lich BEGONE
				mBoss.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 1000, 0));
				mBoss.removePotionEffect(PotionEffectType.GLOWING);
				mBoss.setGlowing(false);
				mBoss.setAI(false);
				mBoss.setSilent(true);
				mBoss.setInvulnerable(true);
				mBoss.teleport(mStart.getLocation().subtract(0, detectionRange + 5, 0));
				List<LivingEntity> en = EntityUtils.getNearbyMobs(mStart.getLocation(), detectionRange);
				en.removeIf(e -> e.getType() == EntityType.ARMOR_STAND);
				en.removeIf(e -> e.getScoreboardTags().contains(identityTag));
				for (LivingEntity e : en) {
					e.setHealth(0);
				}
				// armor stand party
				LibraryOfSoulsIntegration.summon(loc, "LichHead");
				LibraryOfSoulsIntegration.summon(loc, "LichChest");
				LibraryOfSoulsIntegration.summon(loc, "LichPants");
				LibraryOfSoulsIntegration.summon(loc, "LichFeet");
				LibraryOfSoulsIntegration.summon(loc, "LichMainhand");
				LibraryOfSoulsIntegration.summon(loc, "LichOffhand");
				// get all armor stand
				Collection<ArmorStand> armorStands = new ArrayList<>(loc.getNearbyEntitiesByType(ArmorStand.class, 1));
				for (LivingEntity e : armorStands) {
					double x = FastUtils.randomDoubleInRange(-0.6, 0.6);
					double z = FastUtils.randomDoubleInRange(-0.6, 0.6);
					Vector vec = new Vector(x, 0.1, z);
					e.setVelocity(vec);
					new BukkitRunnable() {

						@Override
						public void run() {
							if (e.isOnGround()) {
								this.cancel();
								e.setGravity(false);
								if (e.getScoreboardTags().contains("lichhead")) {
									e.teleport(e.getLocation().subtract(0, 1.4, 0));
								} else if (e.getScoreboardTags().contains("lichchest")) {
									e.teleport(e.getLocation().subtract(0, 1.3, 0));
								} else if (e.getScoreboardTags().contains("lichpants")) {
									e.teleport(e.getLocation().subtract(0, 0.6, 0));
								} else if (e.getScoreboardTags().contains("lichfeet")) {
									e.teleport(e.getLocation().subtract(0, 0.6, 0));
								} else if (e.getScoreboardTags().contains("lichmain")) {
									e.teleport(e.getLocation().subtract(0, 1.4, 0));
								} else if (e.getScoreboardTags().contains("lichoff")) {
									e.teleport(e.getLocation().subtract(0, 1.2, 0));
								}
								// particles
								Location pLoc = e.getLocation();
								pLoc.setY(mStart.getLocation().getY());
								new BukkitRunnable() {
									int mT = 0;

									@Override
									public void run() {
										mT++;
										new PartialParticle(Particle.SPELL_WITCH, pLoc, 2, 0.1, 0.1, 0.1, 0.005).spawnAsBoss();
										if (mT >= 20) {
											this.cancel();
											e.remove();
										}
									}

								}.runTaskTimer(mPlugin, 0, 2);
							}
						}

					}.runTaskTimer(mPlugin, 0, 1);
				}

				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					//prevent players above the barrier ceiling from seeing title
					for (Player p : playersInRange(mStart.getLocation(), detectionRange, true)) {
						Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
							"scoreboard players set " + p.getUniqueId() + " MusicCooldown 10000");
						Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
							"execute as " + p.getUniqueId() + " run function monumenta:mechanisms/music/music_stop");
						p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 100f, 0.8f);
						MessagingUtils.sendTitle(p, Component.text("VICTORY", NamedTextColor.GOLD, TextDecoration.BOLD),
							Component.text("Hekawt, The Eternal", NamedTextColor.DARK_GRAY, TextDecoration.BOLD),
							10, 80, 10);
					}

					Bukkit.getScheduler().runTaskLater(mPlugin, Lich.this::surprise, 4 * 20);
				}, 4 * 20);
			}

		}.runTaskLater(mPlugin, 2 * 20);
	}

	private void surprise() {
		mDefeated = false;
		String[] dio = new String[] {
			"...THE PARTING VEIL GRANTS ME STRENGTH.",
			"IT SUSTAINS ME. I HAVE NO TIME FOR DEATH."
		};
		World world = mStart.getWorld();

		mBoss.teleport(mStart.getLocation().subtract(0, 0.5, 0));
		mBoss.removePotionEffect(PotionEffectType.INVISIBILITY);
		mBoss.setGlowing(true);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 10, 0.5f);

		//prevent players above the barrier ceiling from seeing title
		Component title = Component.text("", NamedTextColor.GOLD, TextDecoration.BOLD)
			.append(Component.text("VI"))
			.append(Component.text("C").decoration(TextDecoration.OBFUSCATED, true))
			.append(Component.text("T"))
			.append(Component.text("OR").decoration(TextDecoration.OBFUSCATED, true))
			.append(Component.text("Y"));

		Component subtitle = Component.text("", NamedTextColor.DARK_GRAY, TextDecoration.BOLD)
			.append(Component.text("H").decoration(TextDecoration.OBFUSCATED, true))
			.append(Component.text("ek"))
			.append(Component.text("aw").decoration(TextDecoration.OBFUSCATED, true))
			.append(Component.text("t, Th"))
			.append(Component.text("e").decoration(TextDecoration.OBFUSCATED, true))
			.append(Component.text(" Eternal"));

		for (Player p : playersInRange(mStart.getLocation(), detectionRange, true)) {
			MessagingUtils.sendTitle(p, title, subtitle, 0, 80, 20);
		}

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			for (Player p : playersInRange(mStart.getLocation(), detectionRange, true)) {
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
					"execute as " + p.getUniqueId() + " at @s run function monumenta:mechanisms/music/music_hekawtp4");
			}
		}, 20 * 3 + 15);

		// haha surprise fuck you I'm not dead dialogues
		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (mT < dio.length) {
					for (Player p : playersInRange(mStart.getLocation(), detectionRange, true)) {
						p.sendMessage(ChatColor.LIGHT_PURPLE + dio[mT].toUpperCase());
					}
					world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 3, 1);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_HURT, SoundCategory.HOSTILE, 3, 1);
				}
				mT++;
				if (mT == dio.length) {
					new PartialParticle(Particle.CLOUD, mBoss.getLocation(), 25, 0.1, 0.1, 0.1, 0.1).spawnAsBoss();
					mBoss.teleport(mStart.getLocation().add(0, 10, 0));
					world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 3.0f, 0.5f);
					new PartialParticle(Particle.CLOUD, mBoss.getLocation(), 25, 0.1, 0.1, 0.1, 0.1).spawnAsBoss();
					particles(world);
				}
				if (mT > dio.length) {
					this.cancel();
					mBoss.setHealth(EntityUtils.getMaxHealth(mBoss));
					p4(world);
				}
			}

		}.runTaskTimer(mPlugin, 20 * 3, 20 * 4);
	}

	private void particles(World world) {
		List<Player> players = playersInRange(mBoss.getLocation(), detectionRange, true);
		for (Player p : players) {
			new BukkitRunnable() {
				int mT = 0;
				boolean mTrigger = false;
				Location mStart = p.getLocation().add(0, 1, 0);
				Vector mVec = LocationUtils.getVectorTo(mBoss.getLocation().add(0, 1, 0), mStart).multiply(1.0 / (20.0 * 4.0d));

				@Override
				public void run() {
					if (!mTrigger) {
						mTrigger = true;
						world.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.HOSTILE, 1, 1.25f);
					}
					Location pLoc = mStart.clone().add(mVec.clone().multiply(mT));
					new PartialParticle(Particle.FIREWORKS_SPARK, pLoc, 2, 0.1, 0.1, 0.1, 0.02).spawnAsBoss();
					if (mT >= 20 * 4) {
						this.cancel();
					}
					mT += 2;
				}

			}.runTaskTimer(mPlugin, 0, 2);

		}
	}

	// Phase 4
	private void p4(World world) {
		mDead = false;
		mCounter = 0;
		world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 3.0f, 1.0f);
		new PartialParticle(Particle.CAMPFIRE_COSY_SMOKE, mBoss.getLocation(), 150, 0, 0, 0, 0.75).spawnAsBoss();
		FallingBlock block = world.spawnFallingBlock(mBoss.getLocation().add(0, 3.5, 0), Bukkit.createBlockData(Material.OBSIDIAN));
		block.setGravity(false);
		block.setTicksLived(1);
		for (Player p : playersInRange(mStart.getLocation(), detectionRange, true)) {
			p.sendMessage(ChatColor.LIGHT_PURPLE + "AND I HAVE NO TIME FOR YOU AND YOUR MEDDLING!");
		}

		List<Spell> death0Passives = Arrays.asList(
			new SpellEdgeKill(mBoss, mStart.getLocation()),
			new SpellFinalParticle(mPlugin, mBoss, mStart.getLocation(), detectionRange, block),
			new SpellFinalSwarm(mPlugin, mStart.getLocation(), detectionRange),
			new SpellFinalCrystal(mPlugin, mBoss, mStart.getLocation(), detectionRange, mCrystalLoc),
			new SpellAutoAttack(mPlugin, this, mBoss, mStart.getLocation(), 20 * 5, detectionRange, mCeiling, 4));
		List<Spell> death1Passives = Arrays.asList(
			new SpellEdgeKill(mBoss, mStart.getLocation()),
			new SpellFinalParticle(mPlugin, mBoss, mStart.getLocation(), detectionRange, block),
			new SpellFinalSwarm(mPlugin, mStart.getLocation(), detectionRange),
			new SpellFinalCrystal(mPlugin, mBoss, mStart.getLocation(), detectionRange, mCrystalLoc),
			new SpellFinalLaser(mPlugin, mBoss, mStart.getLocation(), detectionRange),
			new SpellAutoAttack(mPlugin, this, mBoss, mStart.getLocation(), 20 * 4, detectionRange, mCeiling, 4));
		List<Spell> death2Passives = Arrays.asList(
			new SpellEdgeKill(mBoss, mStart.getLocation()),
			new SpellFinalParticle(mPlugin, mBoss, mStart.getLocation(), detectionRange, block),
			new SpellFinalSwarm(mPlugin, mStart.getLocation(), detectionRange),
			new SpellFinalCrystal(mPlugin, mBoss, mStart.getLocation(), detectionRange, mCrystalLoc),
			new SpellFinalHeatMech(mPlugin, mBoss, mStart.getLocation(), detectionRange),
			new SpellFinalLaser(mPlugin, mBoss, mStart.getLocation(), detectionRange),
			new SpellAutoAttack(mPlugin, this, mBoss, mStart.getLocation(), 20 * 3, detectionRange, mCeiling, 4));
		List<Spell> death3Passives = Arrays.asList(
			new SpellEdgeKill(mBoss, mStart.getLocation()),
			new SpellFinalParticle(mPlugin, mBoss, mStart.getLocation(), detectionRange, block),
			new SpellFinalSwarm(mPlugin, mStart.getLocation(), detectionRange),
			new SpellFinalCrystal(mPlugin, mBoss, mStart.getLocation(), detectionRange, mCrystalLoc),
			new SpellFinalHeatMech(mPlugin, mBoss, mStart.getLocation(), detectionRange),
			new SpellFinalLaser(mPlugin, mBoss, mStart.getLocation(), detectionRange),
			new SpellAutoAttack(mPlugin, this, mBoss, mStart.getLocation(), 20 * 2, detectionRange, mCeiling, 4));

		// partial respawn arena
		String cmd = "execute positioned " + mStart.getLocation().getX() + " " + mStart.getLocation().getY() + " "
			             + mStart.getLocation().getZ() + " run loadstructure \"isles/lich/LichPhase4\" ~-30 ~-2 ~-30";
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);

		//warning smoke ring
		PPCircle indicator = new PPCircle(Particle.SMOKE_LARGE, mStart.getLocation(), 8).ringMode(true).count(20).delta(0.1);
		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				indicator.spawnAsBoss();
				mT++;
				if (mT > 3 * 20) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 2);

		// spawn crystals, delay to allow for arena respawn
		BossBar timer = Bukkit.getServer().createBossBar(null, BarColor.PURPLE, BarStyle.SOLID, BarFlag.PLAY_BOSS_MUSIC);
		timer.setVisible(true); //p4 substitute
		double timeLimit = 200.0d * 20.0d;

		//initialize
		mTowerGroup.add(mTower0);
		mTowerGroup.add(mTower1);
		mTowerGroup.add(mTower2);
		mTowerGroup.add(mTower3);
		List<Player> remaining = playersInRange(mStart.getLocation(), detectionRange, true);

		new BukkitRunnable() {
			int mT = 0;
			int mChat = 0;

			@Override
			public void run() {
				if (!mTrigger && mT % 20 == 0) {
					if (mCounter == 0) {
						mTrigger = true;
						changePhase(SpellManager.EMPTY, death0Passives, null);
						SpellFinalCrystal.setTriggered(false);
						Collections.shuffle(mTowerGroup);
						lastStand(mTowerGroup.get(0));
						mBoss.setHealth(EntityUtils.getMaxHealth(mBoss));
						if (mChat == 0) {
							mChat++;
							for (Player p : playersInRange(mStart.getLocation(), detectionRange, true)) {
								p.sendMessage(ChatColor.AQUA + "The crystals! I can use them against Hekawt!");
							}
						}
					} else if (mCounter == 1) {
						mTrigger = true;
						changePhase(SpellManager.EMPTY, death1Passives, null);
						SpellFinalCrystal.setTriggered(false);
						Collections.shuffle(mTowerGroup);
						lastStand(mTowerGroup.get(0));
						world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 3, 0.9f);
						mBoss.setHealth(EntityUtils.getMaxHealth(mBoss) * 0.75);
						if (mChat == 1) {
							mChat++;
							for (Player p : playersInRange(mStart.getLocation(), detectionRange, true)) {
								p.sendMessage(ChatColor.LIGHT_PURPLE + "HOW MUCH TIME HAS PASSED? I SWORE IT WAS MONTHS... ONLY MONTHS...");
							}
						}
					} else if (mCounter == 2) {
						mTrigger = true;
						changePhase(SpellManager.EMPTY, death2Passives, null);
						SpellFinalCrystal.setTriggered(false);
						Collections.shuffle(mTowerGroup);
						lastStand(mTowerGroup.get(0));
						mBoss.setHealth(EntityUtils.getMaxHealth(mBoss) * 0.5);
						world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 3, 0.8f);
						world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_HURT, SoundCategory.HOSTILE, 3, 1);
						if (mChat == 2) {
							mChat++;
							for (Player p : playersInRange(mStart.getLocation(), detectionRange, true)) {
								p.sendMessage(ChatColor.LIGHT_PURPLE + "THINGS HAVE CHANGED... POWERS HAVE SHIFTED? HAS ETERNITY ABANDONED ME?");
							}
						}
					} else if (mCounter == 3) {
						mTrigger = true;
						changePhase(SpellManager.EMPTY, death3Passives, null);
						SpellFinalCrystal.setTriggered(false);
						Collections.shuffle(mTowerGroup);
						lastStand(mTowerGroup.get(0));
						mBoss.setHealth(EntityUtils.getMaxHealth(mBoss) * 0.25);
						world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 3, 0.7f);
						world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_HURT, SoundCategory.HOSTILE, 3, 0.8f);
						if (mChat == 3) {
							mChat++;
							for (Player p : playersInRange(mStart.getLocation(), detectionRange, true)) {
								p.sendMessage(ChatColor.LIGHT_PURPLE + "WHERE HAVE THE YEARS GONE? WHAT HAVE I LOST IN THE DEPTHS OF THE VEIL?");
							}
						}
					} else if (mCounter == 4) {
						changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
						timer.setVisible(false);
						mBoss.setHealth(100);
						mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 100, 10));
						this.cancel();
						finalAnimation(block);
					}
				}
				//boss bar stuff, 200s time limit
				double progress = (timeLimit - mT) / timeLimit;
				if (progress >= 0) {
					timer.setProgress(progress);
				}
				if (progress <= 0.34) {
					timer.setColor(BarColor.RED);
				} else if (progress <= 0.67) {
					timer.setColor(BarColor.YELLOW);
				}
				if (mT % 20 == 0) {
					int chat = (int) timeLimit;
					timer.setTitle(ChatColor.YELLOW + "Soul dissipating in " + (chat - mT) / 20 + " seconds!");
				}
				for (Player player : remaining) {
					if (!playersInRange(mStart.getLocation(), detectionRange, true).contains(player)) {
						timer.removePlayer(player);
					} else {
						timer.addPlayer(player);
					}
					//kill player if time runs out. show that they are dying extremely quickly
					if (mT > timeLimit) {
						player.setNoDamageTicks(0);
						BossUtils.bossDamagePercent(mBoss, player, 0.1);
					}
				}
				remaining.removeIf(p -> !playersInRange(mStart.getLocation(), detectionRange, true).contains(p));

				//Psychological bell ringing
				int bellCD = 4 * 20;
				if (mT >= timeLimit - 11 * bellCD && mT % bellCD == 0 && mT <= timeLimit) {
					for (Player p : playersInRange(mStart.getLocation(), detectionRange, true)) {
						//play bell sound multiple times to make it louder
						p.playSound(p.getLocation(), Sound.BLOCK_BELL_USE, SoundCategory.HOSTILE, 5, 0.5f);
						p.playSound(p.getLocation(), Sound.BLOCK_BELL_USE, SoundCategory.HOSTILE, 5, 0.5f);
						p.playSound(p.getLocation(), Sound.BLOCK_BELL_USE, SoundCategory.HOSTILE, 5, 0.5f);
					}
					world.playSound(mBoss.getLocation(), Sound.BLOCK_PORTAL_AMBIENT, SoundCategory.HOSTILE, 10, 0.7f);
				}

				mT += 5;
				//stop when boss is despawned
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
					mCounter = 0;
					timer.setVisible(false);
				}
			}

		}.runTaskTimer(mPlugin, 20 * 4, 5);
	}

	private void lastStand(List<Location> tower) {
		List<Player> players = playersInRange(mStart.getLocation(), detectionRange, true);
		double count = Math.min(8, Math.max(3, Math.sqrt(players.size())));
		spawnCrystal(mCrystalLoc, count, mShieldCrystal);

		// get all active crystals
		for (Location l : mCrystalLoc) {
			mCrystal.addAll(l.getNearbyEntitiesByType(EnderCrystal.class, 10));
		}
		//set glowing
		for (EnderCrystal e : mCrystal) {
			e.setGlowing(true);
		}
		// choose random tower to spawn key crystal
		new BukkitRunnable() {
			boolean mCrystalTrigger = false;

			@Override
			public void run() {
				mCrystal.removeIf(en -> !en.isValid());
				if (SpellFinalCrystal.getTriggered()) {
					this.cancel();
					mTrigger = false;
					return;
				}
				if (mCrystal.size() == 0 && !mCrystalTrigger) {
					mCrystalTrigger = true;
					spawnCrystal(tower, 2, mFinalCrystal);

					// get all active crystals
					for (Location l : tower) {
						mCrystal.addAll(l.getNearbyEntitiesByType(EnderCrystal.class, 10));
					}

					for (Player p : playersInRange(mStart.getLocation(), detectionRange, true)) {
						p.sendMessage(ChatColor.AQUA + "The crystals moved into the big tower. Destroy them before it's too late!");
					}
				} else if (mCrystal.size() == 0 && !SpellFinalCrystal.getTriggered()) {
					this.cancel();
					mCounter++;
					// find higher tower location index
					double y = tower.get(1).getY() - tower.get(0).getY();
					int top = 0;
					if (y > 0) {
						top = 1;
					}
					// Grow the lich flame using growables

					try {
						GrowableAPI.grow("LichFlame", tower.get(top).clone().add(0, 3, 0), 1, 4, false);
					} catch (Exception e) {
						e.printStackTrace();
					}
					// beacon effect
					new BukkitRunnable() {

						@Override
						public void run() {
							World world = mBoss.getWorld();
							double y = tower.get(1).getY() - tower.get(0).getY();
							int top = 0;
							if (y > 0) {
								top = 1;
							}
							tower.get(top).getBlock().setType(Material.END_GATEWAY);
							world.playSound(tower.get(top), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED,
								SoundCategory.HOSTILE, 5, 1);
							mTowerGroup.remove(tower);
							mTrigger = false;
						}

					}.runTaskLater(mPlugin, 2 * 20);
				}
				//stop when boss is despawned
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
					mCounter = 0;
				}
			}
		}.runTaskTimer(mPlugin, 0, 20);
	}

	private void finalAnimation(FallingBlock block) {
		// invulnerable players
		List<Player> players = playersInRange(mStart.getLocation(), detectionRange, true);
		for (Player player : players) {
			PotionUtils.applyPotion(com.playmonumenta.plugins.Plugin.getInstance(), player, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 40, 10));
			PotionUtils.applyPotion(com.playmonumenta.plugins.Plugin.getInstance(), player, new PotionEffect(PotionEffectType.REGENERATION, 20 * 40, 1));
		}

		new BukkitRunnable() {

			@Override
			public void run() {
				mDead = true;
				World world = mBoss.getWorld();
				String[] finalDialog = new String[] {
					"I SHOULD NOT HAVE EMERGED... THE VEIL IS FRAYING.",
					"THERE IS POWER OUT THERE THAT COULD BE MINE, IF ONLY I HAD REMAINED.",
					"SEARCHING... SOMETHING HAS BROKEN..."
				};
				String[] endDialog = new String[] {
					"REALITY...",
					"WOULD...",
					"BE...",
					"MINE..."
				};
				// beams
				Collection<EnderCrystal> crystals = new ArrayList<>();
				spawnCrystal(mPassive2Loc, 4, "WarpedCrystal");
				for (Location l : mPassive2Loc) {
					crystals.addAll(l.getNearbyEntitiesByType(EnderCrystal.class, 10));
				}
				for (EnderCrystal ec : crystals) {
					ec.setInvulnerable(true);
					ec.setBeamTarget(mBoss.getLocation().add(0, 2, 0));
					world.playSound(ec.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 5, 0.85f);
				}
				// dialogue + animation
				new BukkitRunnable() {
					int mT = 0;
					int mDio = 0;

					@Override
					public void run() {
						if (mT <= 8) {
							new PartialParticle(Particle.EXPLOSION_HUGE, mBoss.getLocation().add(0, 5, 0), 5, 10, 5, 10, 0).spawnAsBoss();
							world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 10, 1);
						}
						if (mT % 3 == 0 && mDio < finalDialog.length) {
							for (Player p : playersInRange(mStart.getLocation(), detectionRange, true)) {
								p.sendMessage(ChatColor.LIGHT_PURPLE + finalDialog[mDio].toUpperCase());
							}
							mDio++;
						}
						if (mT == 8) {
							this.cancel();
							// explode lich body + make boss invisible + remove ender crystal beam
							for (EnderCrystal e : crystals) {
								e.remove();
							}
							block.remove();
							new PartialParticle(Particle.EXPLOSION_HUGE, mBoss.getLocation(), 2, 0.5, 0.5, 0.5, 0).spawnAsBoss();
							world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE,
								15, 0.8f);
							world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SKELETON_DEATH, SoundCategory.HOSTILE, 15, 0.75f);

							// Lich BEGONE
							mBoss.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 1000, 0));
							mBoss.setAI(false);
							mBoss.setSilent(true);
							mBoss.setInvulnerable(true);
							mBoss.getEquipment().clear();
							mBoss.setGlowing(false);
							// kill mobs
							List<LivingEntity> en = EntityUtils.getNearbyMobs(mStart.getLocation(), detectionRange);
							en.removeIf(e -> e.getType() == EntityType.ARMOR_STAND);
							for (LivingEntity e : en) {
								e.setHealth(0);
							}
							// kill end crystals
							Collection<Entity> ec = mStart.getWorld().getNearbyEntities(mStart.getLocation(), detectionRange, detectionRange, detectionRange);
							ec.removeIf(entity -> !entity.getType().equals(EntityType.ENDER_CRYSTAL));
							for (Entity crystal : ec) {
								crystal.remove();
							}
							// armor stand party
							LibraryOfSoulsIntegration.summon(mBoss.getLocation(), "LichHead");
							LibraryOfSoulsIntegration.summon(mBoss.getLocation(), "LichChest");
							LibraryOfSoulsIntegration.summon(mBoss.getLocation(), "LichPants");
							LibraryOfSoulsIntegration.summon(mBoss.getLocation(), "LichFeet");
							LibraryOfSoulsIntegration.summon(mBoss.getLocation(), "LichMainhand");
							LibraryOfSoulsIntegration.summon(mBoss.getLocation(), "LichOffhand");
							// get all armor stand
							Collection<ArmorStand> armorStands = new ArrayList<>();
							armorStands.addAll(mBoss.getLocation().getNearbyEntitiesByType(ArmorStand.class, 1));
							for (LivingEntity e : armorStands) {
								double x = FastUtils.randomDoubleInRange(-1.2, 1.2);
								double z = FastUtils.randomDoubleInRange(-1.2, 1.2);
								Vector vec = new Vector(x, 0, z);
								e.setVelocity(vec);
								new BukkitRunnable() {

									@Override
									public void run() {
										if (e.isOnGround()) {
											this.cancel();
											e.setGravity(false);
											if (e.getScoreboardTags().contains("lichhead")) {
												e.teleport(e.getLocation().subtract(0, 1.4, 0));
											} else if (e.getScoreboardTags().contains("lichchest")) {
												e.teleport(e.getLocation().subtract(0, 1.3, 0));
											} else if (e.getScoreboardTags().contains("lichpants")) {
												e.teleport(e.getLocation().subtract(0, 0.6, 0));
											} else if (e.getScoreboardTags().contains("lichfeet")) {
												e.teleport(e.getLocation().subtract(0, 0.6, 0));
											} else if (e.getScoreboardTags().contains("lichmain")) {
												e.teleport(e.getLocation().subtract(0, 1.4, 0));
											} else if (e.getScoreboardTags().contains("lichoff")) {
												e.teleport(e.getLocation().subtract(0, 1.2, 0));
											}
											// particles
											Location pLoc = e.getLocation();
											pLoc.setY(mStart.getLocation().getY());
											new BukkitRunnable() {
												int mT = 0;

												@Override
												public void run() {
													mT++;
													new PartialParticle(Particle.SPELL_WITCH, pLoc, 2, 0.1, 0.1, 0.1, 0.005).spawnAsBoss();
													if (mT >= 20) {
														this.cancel();
														e.remove();
													}
												}

											}.runTaskTimer(mPlugin, 6 * 20, 2);
										}
									}

								}.runTaskTimer(mPlugin, 0, 1);
							}
							new BukkitRunnable() {
								int mT = 0;
								int mDio = 0;

								@Override
								public void run() {
									if (mT != 1 && mDio < endDialog.length) {
										for (Player p : playersInRange(mStart.getLocation(), detectionRange, true)) {
											p.sendMessage(ChatColor.LIGHT_PURPLE + endDialog[mDio].toUpperCase());
										}
										mDio++;
									}
									if (mT >= endDialog.length + 4) {
										this.cancel();
										mBoss.remove();
										// kill mobs again
										List<LivingEntity> en = EntityUtils.getNearbyMobs(mStart.getLocation(), detectionRange);
										en.removeIf(e -> e.getType() == EntityType.ARMOR_STAND);
										for (LivingEntity e : en) {
											e.setHealth(0);
										}
										for (Player p : playersInRange(mStart.getLocation(), detectionRange, true)) {
											p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 100f, 0.8f);
											ScoreboardUtils.setScoreboardValue(p, "MusicCooldown", 0);
											MessagingUtils.sendTitle(p, Component.text("VICTORY", NamedTextColor.GOLD, TextDecoration.BOLD),
												Component.text("Hekawt, The Eternal", NamedTextColor.DARK_GRAY, TextDecoration.BOLD),
												10, 80, 10);
										}
										mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);

										for (Player player : playersInRange(mStart.getLocation(), detectionRange, true)) {
											player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
											player.removePotionEffect(PotionEffectType.REGENERATION);
											if (player.getGameMode() != GameMode.CREATIVE) {
												player.setGameMode(GameMode.SURVIVAL);
											}
										}
									}
									mT++;
								}

							}.runTaskTimer(mPlugin, 4 * 20, 20);
						}
						mT++;
					}

				}.runTaskTimer(mPlugin, 2 * 20, 20);
			}

		}.runTaskLater(mPlugin, 20);
	}

	public static boolean bossDead() {
		return mDead;
	}

	public static List<Player> playersInRange(Location bossLoc, double range, boolean stealth) {
		List<Player> players = PlayerUtils.playersInRange(bossLoc, range, stealth);
		List<Player> toRemove = new ArrayList<>();
		for (Player p : players) {
			Location playerLoc = p.getLocation();
			playerLoc.setY(mStart.getLocation().getY());
			if (p.getLocation().getY() > mStart.getLocation().getY() + mCeiling) {
				toRemove.add(p);
			}
		}
		players.removeAll(toRemove);
		return players;
	}

	@Override
	public void init() {
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, MAX_HEALTH);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, detectionRange);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1);
		mBoss.setHealth(MAX_HEALTH);
		mBoss.setPersistent(true);
	}
}
