package com.playmonumenta.plugins.bosses.spells.intruder;

import com.playmonumenta.libraryofsouls.LibraryOfSoulsAPI;
import com.playmonumenta.libraryofsouls.Soul;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.bosses.intruder.IntruderBoss;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.delves.mobabilities.StatMultiplierBoss;
import com.playmonumenta.plugins.effects.DamageImmunity;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPBezier;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import com.playmonumenta.scriptedquests.growables.GrowableAPI;
import com.playmonumenta.scriptedquests.growables.GrowableProgress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SpellAmalgamatingDreamscape extends Spell {
	private static final String SPELL_NAME = "Amalgamating Dreamscape (☠)";
	public static final String ANTIHEAL_SOURCE = "AmalgamatingDreamscapeHealing";
	public static final String WEAKNESS_SOURCE = "AmalgamatingDreamscapeDamage";
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mPortalSpawnLoc;
	private final Location mCenter;
	private final Runnable mFinishAction;
	private final ChargeUpManager mChargeUpManager;
	private final IntruderBoss.Dialogue mDialogue;
	private final IntruderBoss.Narration mNarration;
	private final List<ExaltedDungeon> mChosenSummons = new ArrayList<>();
	// 2 min 30s
	private static final int DURATION = 100 * 20;
	private static final List<String> NARRATIONS = List.of(
		"The dreamscape warps and distorts around you... A familiar reality materialises in front of your eyes.",
		"The dreamscape disfigures again, reforming as another familiar memory.",
		"The dreamscape contorts one final time, the visions become fainter..."
	);
	private final List<Block> mChangedBlocks = new ArrayList<>();

	@Nullable
	private BukkitTask mCountdownTask;
	@Nullable
	private Entity mExaltedBoss;
	private final List<String> mNarrations = new ArrayList<>();
	private boolean mStalker = false;

	private enum ExaltedDungeon {
		WHITE("ManifestedSoulTempestMount", List.of("#DreamscapeWhite1", "#DreamscapeWhite2"), "DreamscapeWhite"),
		ORANGE("ManifestedWingedTitan", List.of("#DreamscapeOrange1", "#DreamscapeOrange2"), "DreamscapeOrange"),
		MAGENTA("ManifestedCVaatktheSoulblighter", List.of("#DreamscapeMagenta1", "#DreamscapeMagenta2"), "DreamscapeMagenta"),
		LIGHT_BLUE("ManifestedHeraldofSecretStars", List.of("#DreamscapeLightblue1", "#DreamscapeLightblue2"), "DreamscapeLightBlue"),
		YELLOW("~DreamscapeYellowBoss", List.of("#DreamscapeYellow1", "#DreamscapeYellow2"), "DreamscapeYellow"),
		WILLOWS("ManifestedVeiltornHelminthMount", List.of("#DreamscapeWillows1", "#DreamscapeWillows2"), "DreamscapeWillows");

		public final String mBossSoul;
		public final List<String> mSouls;
		private final String mTeleportMarkerName;
		private final String mSpawnMarkerName;
		private final String mBossSpawnMarkerName;

		private static final String TP_SUFFIX = "TP";
		private static final String BOSS_SUFFIX = "Boss";

		ExaltedDungeon(String boss, List<String> souls, String markerName) {
			mBossSoul = boss;
			mSouls = souls;
			mTeleportMarkerName = markerName + TP_SUFFIX;
			mSpawnMarkerName = markerName;
			mBossSpawnMarkerName = mSpawnMarkerName + BOSS_SUFFIX;
		}
	}

	public SpellAmalgamatingDreamscape(Plugin plugin, LivingEntity boss, Location bossSpawnLoc, Runnable finishAction, IntruderBoss.Dialogue dialogue, IntruderBoss.Narration narration) {
		mPlugin = plugin;
		mBoss = boss;
		mPortalSpawnLoc = bossSpawnLoc.clone().add(0, 30, 0);
		mCenter = bossSpawnLoc;
		mFinishAction = finishAction;
		mChargeUpManager = new ChargeUpManager(boss, DURATION, Component.text("Casting ", NamedTextColor.GOLD).append(Component.text(SPELL_NAME, NamedTextColor.RED)), BossBar.Color.RED, BossBar.Overlay.PROGRESS, IntruderBoss.DETECTION_RANGE * 2);
		mDialogue = dialogue;
		mNarration = narration;
	}


	@Override
	public void run() {
		mStalker = mBoss.getScoreboardTags().contains(IntruderBoss.STALKER_ACTIVE_TAG);
		mBoss.setAI(false);
		mBoss.setInvulnerable(true);

		mDialogue.dialogue(2 * 20, List.of("THOSE MEMORIES. THEY WERE NOT ENOUGH.", "I GIVE THEM ONE MORE CHANCE."),
			() -> {
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.5f, 0.1f);
				mBoss.teleport(mCenter.clone().add(0, 5, 0));
				mBoss.setInvisible(true);

				new PPBezier(Particle.DUST_COLOR_TRANSITION, mBoss.getLocation(), mBoss.getLocation().add(0, 20, 0), mPortalSpawnLoc)
					.data(new Particle.DustTransition(Color.fromRGB(0x6b0000), Color.BLACK, 2.0f))
					.count(100)
					.delay(25)
					.spawnAsBoss();

				Vector vec = new Vector(12, 0, 0);
				for (int i = 0; i < 4; i++) {
					new PPBezier(Particle.SMOKE_LARGE, mBoss.getLocation(), mBoss.getLocation().add(0, 9, 0).add(vec), mPortalSpawnLoc)
						.count(50)
						.delay(25)
						.spawnAsBoss();
					vec = VectorUtils.rotateYAxis(vec, 90);
				}

				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					new PartialParticle(Particle.FLASH, mPortalSpawnLoc.clone().subtract(0, 1, 0)).minimumCount(1).spawnAsBoss();
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.HOSTILE, 3.0f, 0.2f);
					@Nullable GrowableProgress growable = null;
					try {
						growable = GrowableAPI.grow("AmalgamatingDreamscapePortal", mPortalSpawnLoc.clone().add(0, 2, 0), 1, 4, false);
					} catch (IllegalArgumentException e) {
						MMLog.severe("Exception in spawning Dreamscape Portal: \n" + Arrays.toString(e.getStackTrace()));
					}

					mNarrations.clear();
					mNarrations.addAll(NARRATIONS);

					// First cutscene
					if (growable != null) {
						growable.whenComplete(growableProgress -> breakMain());
					}
				}, 25);
			}, 20);
	}

	private void breakMain() {
		List<Block> blocks = new ArrayList<>(BlockUtils.getBlocksInCube(mCenter, 30).stream()
			.filter(block -> block.isSolid() && block.getType() != Material.REDSTONE_BLOCK)
			.toList());
		Collections.shuffle(blocks);
		IntruderBoss.playersInRange(mBoss.getLocation()).forEach(player ->
			player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 5 * 20, 1, true, false)));

		mChargeUpManager.setTime(0);
		mChargeUpManager.setChargeTime(4 * 20);
		mChargeUpManager.setTitle(Component.text("Channeling ", NamedTextColor.GOLD).append(Component.text(SPELL_NAME, NamedTextColor.RED)));

		new BukkitRunnable() {
			@Override
			public void run() {
				if (mChargeUpManager.nextTick()) {
					IntruderBoss.playersInRange(mBoss.getLocation()).forEach(player ->
						player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 3 * 20, 1, true, false)));
					this.cancel();
					startSummon();
				} else if ((mChargeUpManager.getTime() - 1) % 10 == 0) {
					new BukkitRunnable() {
						final Vector mVector = new Vector(1, 0, 0);
						final Vector mVector2 = new Vector(-1, 0, 0);
						int mHeightProgress = 0;

						@Override
						public void run() {
							int newHeightProgress = mHeightProgress + 10;

							for (int i = 0; i < 10 * 2; i++) {
								new PartialParticle(Particle.CRIT_MAGIC, mPortalSpawnLoc.clone().subtract(mVector)).spawnAsBoss();
								mVector.add(new Vector(0, 1 / 2.0, 0));
								mVector.rotateAroundY(Math.toRadians(4));
							}

							for (int i = 0; i < 10 * 2; i++) {
								new PartialParticle(Particle.CRIT_MAGIC, mPortalSpawnLoc.clone().subtract(mVector2)).spawnAsBoss();
								mVector2.add(new Vector(0, 1 / 2.0, 0));
								mVector2.rotateAroundY(Math.toRadians(4));
							}

							mHeightProgress = newHeightProgress;
							if (mHeightProgress > 32) {
								new PartialParticle(Particle.EXPLOSION_LARGE, mCenter)
									.count(50)
									.delta(15, 1, 15)
									.extra(4)
									.spawnAsBoss();
								this.cancel();
							}
						}
					}.runTaskTimer(mPlugin, 0, 1);

					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 4.0f, 0.25f);
					int size = blocks.size();
					for (int i = 0; i < size / 4; i++) {
						Block block = blocks.remove(size - 1 - i);
						TemporaryBlockChangeManager.INSTANCE.changeBlock(block, Material.NETHER_WART_BLOCK, 10 * 20);
						mChangedBlocks.add(block);
					}
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void cutscene(Location loc) {
		Location location = loc.clone();
		List<Block> blocks = new ArrayList<>(BlockUtils.getBlocksInCube(location, 30).stream()
			.filter(block -> block.isSolid() && block.getType() != Material.REDSTONE_BLOCK)
			.toList());
		Collections.shuffle(blocks);
		IntruderBoss.playersInRange(mBoss.getLocation()).forEach(player -> {
				player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 5 * 20, 1, true, false));
			}
		);
		mChargeUpManager.setTime(0);
		mChargeUpManager.setChargeTime(4 * 20);
		mChargeUpManager.setTitle(Component.text("Channeling ", NamedTextColor.GOLD).append(Component.text(SPELL_NAME, NamedTextColor.RED)));


		new BukkitRunnable() {
			@Override
			public void run() {
				if (mChargeUpManager.nextTick()) {
					IntruderBoss.playersInRange(mBoss.getLocation()).forEach(player ->
						player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 3 * 20, 1, true, false)));
					this.cancel();
					summonNext();
				} else if ((mChargeUpManager.getTime() - 1) % 10 == 0) {
					mBoss.getWorld().playSound(location, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 4.0f, 0.25f);
					int size = blocks.size();
					for (int i = 0; i < size / 4; i++) {
						Block block = blocks.remove(size - 1 - i);
						TemporaryBlockChangeManager.INSTANCE.changeBlock(block, Material.NETHER_WART_BLOCK, 10 * 20);
						mChangedBlocks.add(block);
					}
					new PartialParticle(Particle.EXPLOSION_LARGE, location)
						.count(35)
						.delta(20, 1, 20)
						.extra(4)
						.spawnAsBoss();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void startSummon() {
		List<ExaltedDungeon> summonsClone = new ArrayList<>(List.of(ExaltedDungeon.values()));
		Collections.shuffle(summonsClone);
		mChosenSummons.clear();
		mChosenSummons.addAll(summonsClone.subList(0, Math.min(NARRATIONS.size(), summonsClone.size())));
		summonNext();
	}

	private void summonNext() {
		if (mChosenSummons.isEmpty() || mNarrations.isEmpty()) {
			finishSpell();
			return;
		}
		mNarration.narration(mNarrations.get(0));
		mNarrations.remove(0);

		resetAreas();
		mChargeUpManager.setTime(DURATION);
		mChargeUpManager.setChargeTime(DURATION);
		mChargeUpManager.setTitle(Component.text("Casting ", NamedTextColor.GOLD).append(Component.text(SPELL_NAME, NamedTextColor.RED)));
		ExaltedDungeon exaltedDungeon = mChosenSummons.get(0);
		List<LivingEntity> entities = EntityUtils.getNearbyMobs(mCenter, IntruderBoss.DETECTION_RANGE, 400, IntruderBoss.DETECTION_RANGE,
			entity -> {
				String name = entity.getName();
				return name.equals(exaltedDungeon.mTeleportMarkerName) || name.equals(exaltedDungeon.mSpawnMarkerName) || name.equals(exaltedDungeon.mBossSpawnMarkerName);
			});

		try {
			Location tpLocation = entities.stream().filter(entity -> entity.getName().equals(exaltedDungeon.mTeleportMarkerName)).toList().get(0).getLocation();
			List<Location> spawnLocations = entities.stream().filter(entity -> entity.getName().equals(exaltedDungeon.mSpawnMarkerName)).map(Entity::getLocation).toList();
			Location bossSpawnLocation = entities.stream().filter(entity -> entity.getName().equals(exaltedDungeon.mBossSpawnMarkerName)).toList().get(0).getLocation();

			List<String> mSouls = new ArrayList<>(exaltedDungeon.mSouls);

			IntruderBoss.playersInRange(mBoss.getLocation()).forEach(player -> {
				player.teleport(tpLocation);
				player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_EMERGE, SoundCategory.HOSTILE, 2.0f, 1.5f);
				player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 5.5f, 0.2f);
				player.playSound(player.getLocation(), Sound.AMBIENT_UNDERWATER_LOOP_ADDITIONS_ULTRA_RARE, SoundCategory.HOSTILE, 5.5f, 0.1f, 3);
			});
			if (mStalker) {
				LibraryOfSoulsIntegration.summon(mBoss.getLocation(), "AntumbralStalker");
			}
			mChosenSummons.remove(0);

			mChargeUpManager.setTitle(Component.text("Casting ", NamedTextColor.GOLD).append(Component.text(SPELL_NAME, NamedTextColor.RED)));
			Entity summon = Objects.requireNonNull(exaltedDungeon.mBossSoul.startsWith(LibraryOfSoulsAPI.SOUL_POOL_PREFIX) ?
				LoSPool.fromString(exaltedDungeon.mBossSoul).spawn(bossSpawnLocation) :
				LibraryOfSoulsIntegration.summon(bossSpawnLocation, exaltedDungeon.mBossSoul)
			);
			bossSummoned(summon);

			mCountdownTask = new BukkitRunnable() {
				@Override
				public void run() {
					if (mChargeUpManager.previousTick(5)) {
						mNarration.narration("You feel the apparitions closing in on you...");
						new PartialParticle(Particle.FLASH, bossSpawnLocation).minimumCount(1).spawnAsBoss();
						// Punish players who take too long
						IntruderBoss.playersInRange(tpLocation).forEach(player -> {
							EffectManager.getInstance().clearEffects(player, "NightmarishCarvingsReincarnation");
							// Fix bypassing reincarnation
							PlayerUtils.killPlayer(player, mBoss, SPELL_NAME, true, true, true);
						});
						this.cancel();
					} else if (mChargeUpManager.getTime() % 20 == 0) {
						if (mSouls.isEmpty()) {
							return;
						}
						String soulName = mSouls.get(0);

						if (soulName.startsWith(LibraryOfSoulsAPI.SOUL_PARTY_PREFIX)) {
							// Pool of mobs
							Map<Soul, Integer> randomSouls = LibraryOfSoulsAPI.getRandomSouls(soulName, new Random(-1));
							randomSouls.forEach((key, value) -> {
								new BukkitRunnable() {
									private final Location mSpawnLoc = FastUtils.getRandomElement(spawnLocations);
									int mLeft = value;

									@Override
									public void run() {
										Entity summon = key.summon(mSpawnLoc);
										mobSummoned(summon);
										mLeft--;
										if (mLeft <= 0) {
											this.cancel();
										}
									}
								}.runTaskTimer(mPlugin, 0, 1);
							});
						}

						mSouls.remove(0);
					}
				}

			}.runTaskTimer(mPlugin, 0, 5);
		} catch (Exception e) {
			MMLog.severe("Failed to get marker: \n" + Arrays.toString(e.getStackTrace()));
			MMLog.severe("Exalted dungeon: " + exaltedDungeon.toString());
		}
	}

	private void resetAreas() {
		EntityUtils.getNearbyMobs(mBoss.getLocation(), IntruderBoss.DETECTION_RANGE, mBoss).forEach(Entity::remove);
		TemporaryBlockChangeManager.INSTANCE.revertChangedBlocks(mChangedBlocks, Material.NETHER_WART_BLOCK);
		mChangedBlocks.clear();
	}

	private void bossSummoned(Entity summon) {
		summon.addScoreboardTag(StatMultiplierBoss.identityTag);
		summon.addScoreboardTag(StatMultiplierBoss.identityTag + "[damagemult=1.4]");
		EntityUtils.scaleMaxHealth((LivingEntity) summon, 0.2, "AmalgamatingDreamscape");

		new PartialParticle(Particle.SMOKE_LARGE, summon.getLocation())
			.count(15)
			.extra(0.2)
			.spawnAsBoss();

		// Gets the topMost hostile mob from this boss to listen to the death event.
		List<LivingEntity> entityStack = new ArrayList<>();
		EntityUtils.getStackedMobsAbove(summon, entityStack);
		entityStack.removeIf(entity -> entity.getScoreboardTags().contains(EntityUtils.IGNORE_DEATH_TRIGGERS_TAG));
		if (entityStack.isEmpty()) {
			MMLog.severe(String.format("Amalgamating Dreamscape failed to assign mExaltedBoss (%s) as all entities mounted on the summon have tag: '%s'!", MessagingUtils.plainText(summon.name()), EntityUtils.IGNORE_DEATH_TRIGGERS_TAG));
		}
		mExaltedBoss = entityStack.get(0);

		EffectManager.getInstance().addEffect(summon, "AmalgamatingDreamscape",
			new DamageImmunity(20, EnumSet.of(DamageEvent.DamageType.FALL)));
	}

	private void mobSummoned(Entity summon) {
		summon.addScoreboardTag(StatMultiplierBoss.identityTag);
		summon.addScoreboardTag(StatMultiplierBoss.identityTag + "[damagemult=1.4]");
		EntityUtils.scaleMaxHealth((LivingEntity) summon, 0.2, "AmalgamatingDreamscape");

		summon.setVelocity(VectorUtils.randomUnitVector().setY(0.6).multiply(0.7));
		new PartialParticle(Particle.SMOKE_NORMAL, summon.getLocation())
			.count(3)
			.extra(0.15)
			.spawnAsBoss();
		EffectManager.getInstance().addEffect(summon, "AmalgamatingDreamscape",
			new PercentDamageReceived(20, -1.0, EnumSet.of(DamageEvent.DamageType.FALL)));
	}

	private void finishSpell() {
		resetAreas();
		mChargeUpManager.remove();
		mBoss.setAI(true);
		mBoss.setInvulnerable(false);
		mBoss.setInvisible(false);

		IntruderBoss.playersInRange(mBoss.getLocation()).forEach(player -> {
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 1, true, false));
			player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20, 1, true, false));
			player.teleport(mCenter.clone().add(new Vector(0, 3, 5)));
		});
		if (mStalker) {
			LibraryOfSoulsIntegration.summon(mBoss.getLocation(), "AntumbralStalker");
		}
		mFinishAction.run();
		cancel();
	}

	public void bossNearbyDeathEvent(EntityDeathEvent event) {
		if (mExaltedBoss != null && mCountdownTask != null && !mCountdownTask.isCancelled() && event.getEntity() == mExaltedBoss) {
			mCountdownTask.cancel();
			mExaltedBoss.getWorld().playSound(mExaltedBoss.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, SoundCategory.HOSTILE, 5.0f, 0.25f);
			cutscene(event.getEntity().getLocation());
		}
	}

	// Remove all spells before this
	@Override
	public int cooldownTicks() {
		return 0;
	}

	@Override
	public boolean onlyForceCasted() {
		return true;
	}

	@Override
	public void cancel() {
		super.cancel();
		if (mCountdownTask != null && !mCountdownTask.isCancelled()) {
			mCountdownTask.cancel();
		}
	}
}
