package com.playmonumenta.plugins.bosses.bosses.sirius;

import com.playmonumenta.plugins.abilities.warlock.reaper.VoodooBonds;
import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.SerializedLocationBossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBasePassiveAbility;
import com.playmonumenta.plugins.bosses.spells.sirius.PassiveDeclaration;
import com.playmonumenta.plugins.bosses.spells.sirius.PassiveStarBlight;
import com.playmonumenta.plugins.bosses.spells.sirius.PassiveStarBlightConversion;
import com.playmonumenta.plugins.bosses.spells.sirius.PassiveTentacleManager;
import com.playmonumenta.plugins.bosses.spells.sirius.SpellBlightBomb;
import com.playmonumenta.plugins.bosses.spells.sirius.SpellBlightWall;
import com.playmonumenta.plugins.bosses.spells.sirius.SpellBlightedBolts;
import com.playmonumenta.plugins.bosses.spells.sirius.SpellBlightedPods;
import com.playmonumenta.plugins.bosses.spells.sirius.SpellCosmicPortals;
import com.playmonumenta.plugins.bosses.spells.sirius.SpellFromTheStars;
import com.playmonumenta.plugins.bosses.spells.sirius.SpellSiriusBeams;
import com.playmonumenta.plugins.bosses.spells.sirius.SpellSummonTheStars;
import com.playmonumenta.plugins.effects.CustomTimerEffect;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.StarBlight;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.AmethystCluster;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Sirius extends SerializedLocationBossAbilityGroup {
	public static final String identityTag = "boss_sirius";
	public static final String MOB_TAG = "siriusmob";
	public static final String FAIL_PARTICIPATION_TAG = "siriusfaildeclerationparticipation";
	public static final String PARTICIPATION_TAG = "siriusdeclerationparticipation";
	public Location mCornerOne;
	public Location mStartLocation;
	public Location mCornerTwo;
	public Location mSpawnCornerOne;
	public Location mSpawnCornerTwo;
	public Location mTuulenLocation;
	public Location mAuroraLocation;
	private final SpellBlightedPods mPods;
	private final SiriusNPCBoss mTuulenBoss;
	private final SiriusNPCBoss mAuroraBoss;
	public Villager mTuulen;
	public Villager mAurora;
	private static final double DAMAGE_PER_PHASE = 750;
	public static final int DAMAGE_PHASE_DURATION = 14 * 20;
	private @Nullable BossBar mDamagePhaseHPBar;

	public PassiveStarBlightConversion mStarBlightConverter;
	private final SpellSummonTheStars mSpawner;
	public List<BlockDisplay> mDisplays;
	public List<List<BlockDisplay>> mTentacles;
	public List<List<Transformation>> mTentacleRestTransforms;
	private final List<Transformation> mStoredTransformations;
	private List<BlockDisplay> mTempDisplay;
	private List<BlockDisplay> mGate;
	private final PassiveTentacleManager mTentacleManager;
	private boolean mCollisionOn;
	private double mHp;
	private double mMaxHp;
	private double mDefenseScaling;
	public double mDeclerationScaleAmount;
	public int mBlocks;
	private int mMobsKilled;
	private int mMobsToMove;
	private int mPlayerCount;
	private final int mStartingPlayerCount;
	//Defense scaling stuff

	public boolean mDamagePhase;
	public boolean mAnimationLock;
	public boolean mCheeseLock;
	public boolean mDone;
	private boolean mClose;
	private static final double SCALING_X = 0.6;
	private static final double SCALING_Y = 0.35;
	// memo stuff
	private Set<Player> mPlayers = new HashSet<>();

	//z is star blight line

	public Sirius(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);
		mDeclerationScaleAmount = 0.5;
		mCheeseLock = false;
		mDisplays = new ArrayList<>();
		mCollisionOn = true;
		mDone = false;
		mBoss.setVisibleByDefault(false);
		mStartLocation = spawnLoc;
		mBoss.teleport(mBoss.getLocation().add(0, 2, 0));
		mBoss.addScoreboardTag("Boss");
		mBoss.customName(Component.text("Sirius, the Final Herald", NamedTextColor.DARK_AQUA, TextDecoration.BOLD));
		mBlocks = 20;
		mHp = 0;
		mBoss.setHealth(20);
		mTempDisplay = new ArrayList<>();
		mDamagePhase = false;
		mAnimationLock = false;
		mClose = false;
		mCornerOne = spawnLoc.clone().add(73, 38, 45);
		mCornerTwo = spawnLoc.clone().subtract(42, 35, 45);
		mSpawnCornerOne = spawnLoc.clone().add(37, 1, 39);
		mSpawnCornerTwo = spawnLoc.clone().add(-41, -6, -36);
		mTuulenLocation = spawnLoc.clone().add(-24, 0, -24);
		mAuroraLocation = spawnLoc.clone().add(-24, 0, 24);
		mTuulen = mBoss.getWorld().spawn(mTuulenLocation, Villager.class);
		mTuulen.customName(Component.text("Silver Knight Tuulen"));
		mAurora = mBoss.getWorld().spawn(mAuroraLocation, Villager.class);
		mAurora.customName(Component.text("Aurora"));
		mTuulenBoss = new SiriusNPCBoss(mPlugin, mTuulen);
		mAuroraBoss = new SiriusNPCBoss(mPlugin, mAurora);
		BossManager.getInstance().manuallyRegisterBoss(mAurora, mAuroraBoss);
		BossManager.getInstance().manuallyRegisterBoss(mTuulen, mTuulenBoss);
		mStarBlightConverter = new PassiveStarBlightConversion(this);
		mSpawner = new SpellSummonTheStars(plugin, this);
		mStoredTransformations = new ArrayList<>();
		mBoss.setPersistent(true);
		mMobsToMove = getPlayersInArena(false).size() * 20;
		mTentacleManager = new PassiveTentacleManager(this, mPlugin);
		PassiveDeclaration declarations = new PassiveDeclaration(plugin, this, mStarBlightConverter, mSpawner);
		mPods = new SpellBlightedPods(this, plugin);
		List<Spell> passives = List.of(
			new SpellBasePassiveAbility(20, mTentacleManager),
			new SpellBasePassiveAbility(declarations.cooldownTicks(), declarations),
			new PassiveStarBlight(this, mStarBlightConverter),
			new SpellBasePassiveAbility(mSpawner.cooldownTicks(), mSpawner),
			mStarBlightConverter
		);
		SpellManager spells = new SpellManager(List.of(
			new SpellBlightWall(plugin, this, declarations),
			new SpellBlightBomb(this, plugin, mStarBlightConverter),
			new SpellCosmicPortals(this, plugin),
			new SpellFromTheStars(this, plugin, mTentacleManager, declarations),
			mPods
		));
		//Boss visuals
		spawnGate();
		//Teleport players out of the roof
		for (Player p : getPlayersInArena(false)) {
			if (p.getLocation().getY() > mStartLocation.getY() + 20) {
				switch (FastUtils.randomIntInRange(0, 1)) {
					case 0:
						p.teleport(mAuroraLocation);
						break;
					case 1:
						p.teleport(mTuulenLocation);
						break;
					default:
						break;
				}
			}
		}
		startDeathTracker();
		mTentacles = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			mTentacles.add(new ArrayList<>());
		}
		mTentacleRestTransforms = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			mTentacleRestTransforms.add(new ArrayList<>());
		}
		makeVisual();
		mBoss.setVisibleByDefault(false);
		mStartingPlayerCount = (mPlayers = getPlayersInArena(false)).size();
		Bukkit.getScheduler().runTaskLater(plugin, this::startVisual, 10);

		// Disable White Tesseract for the duration of the fight. The tag is cleared in SQ login/death files and the win mcfunction
		getPlayersInArena(false).forEach(player -> player.addScoreboardTag("WhiteTessDisabled"));

		// "Why So Sirius" Classless Advancement
		classlessAdvancementHandler();

		//bossbar and constructing
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			BossBarManager bossBar = new BossBarManager(boss, 100, BarColor.PURPLE, BarStyle.SEGMENTED_10, null, false);
			constructBoss(spells, passives, 100, bossBar, 100, 1);
			recalculatePlayers();
			mPlayerCount = mPlayers.size();
			mDefenseScaling = BossUtils.healthScalingCoef(mPlayerCount, SCALING_X, SCALING_Y);
			mBoss.removePotionEffect(PotionEffectType.INVISIBILITY);
		}, 6 * 20);

	}

	public void changeHp(boolean declerationFail, int distance) {
		if (declerationFail) {
			applyDeclerationFail(distance > 0);
		}
		//glowing sometimes doesnt get removed so confirming it does
		if (!mDamagePhase) {
			mBoss.setGlowing(false);
		}
		mBlocks -= (int) (distance * mDeclerationScaleAmount);
		//pass
		if (distance > 0) {
			for (Player p : mPlayers) {
				p.playSound(p, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.HOSTILE, 0.8f, 1.5f);
				p.playSound(p, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.HOSTILE, 0.3f, 0.1f);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					p.playSound(p, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 0.3f, 0.1f);
				}, 5);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					p.playSound(p, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 0.3f, 0.1f);
				}, 10);
			}
		}
		//fail
		if (distance < 0) {
			for (Player p : mPlayers) {
				p.playSound(p, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 2f, 0.1f);
				p.playSound(p, Sound.ENTITY_WARDEN_EMERGE, SoundCategory.HOSTILE, 0.8f, 1.2f);
			}
		}
		if (mBlocks >= 37) {
			mClose = true;
		}
		if (mBlocks <= 0) {
			mBoss.setHealth(0.1);
			if (!mCheeseLock) {
				for (double i = mBoss.getLocation().getX(); i < mSpawnLoc.clone().add(19, 2, 0).getX(); i++) {
					mStarBlightConverter.restoreLine((int) Math.abs(mStarBlightConverter.mCornerOne.getX() - i));
				}
				updateCollisionBox(distance);
				removeCollisionBox();
				mBoss.teleport(mSpawnLoc.clone().add(19, 2, 0));
				victoryVisual();
				updateCollisionBox(distance);
			}
		} else if (mBlocks >= 40) {
			mBoss.setHealth(40);
			loseAnimation();
			for (Player p : getPlayersInArena(false)) {
				com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(p, "Stasis");
				com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(p, VoodooBonds.PROTECTION_EFFECT);
				if (p.isInvulnerable()) {
					p.setInvulnerable(false);
				}
				DamageUtils.damage(null, p, DamageEvent.DamageType.OTHER, 999999999, null, true, false, "Devoid from Protection");
				if (p.getHealth() > 0) {
					p.setHealth(0); // For good measure
				}
			}
			mPlayers.clear();
		} else {
			mBoss.setHealth(mBlocks);
			//move the entire boss
			mBoss.teleport(mBoss.getLocation().add(distance, 0, 0));
			for (BlockDisplay dis : mDisplays) {
				dis.teleport(dis.getLocation().add(distance, 0, 0));
			}
			if (distance >= 1) {
				for (int i = 0; i < distance; i++) {
					mStarBlightConverter.restoreLine((int) Math.abs(mStarBlightConverter.mCornerOne.getX() - mBoss.getLocation().getX() + i));
				}
				updateCollisionBox(distance);
			}
			for (Player p : mPlayers) {
				p.sendMessage(Component.text("[Tuulen]", NamedTextColor.GOLD).append(Component.text(" Great work adventurers, it seems like all of the blight you have destroyed has injured him.", NamedTextColor.GRAY)));
			}
			if (!mBoss.isDead()) {
				mStarBlightConverter.convertBehind();
			}
		}
	}

	public void startDamagePhase() {
		startDamagePhase(null, null, null, null);
	}

	public void startDamagePhase(@Nullable String passNpc, @Nullable Component passMessage, @Nullable String failNpc, @Nullable Component failMessage) {
		recalculatePlayers();
		if (!mDamagePhase) {
			mHp = DAMAGE_PER_PHASE * mDefenseScaling;
			mMaxHp = DAMAGE_PER_PHASE * mDefenseScaling;
			mDamagePhaseHPBar = BossBar.bossBar(Component.text("Core Stability Remaining", NamedTextColor.AQUA), 1, BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_10);
			for (Player p : mPlayers) {
				p.showBossBar(mDamagePhaseHPBar);
			}
			mBoss.setGlowing(true);
			mDamagePhase = true;
			//tp creates a transformation so this one needs to be delayed.
			Bukkit.getScheduler().runTaskLater(mPlugin, this::makeDamageVisual, 1);
			stopCollision();
			mStarBlightConverter.restoreFullCircle(mBoss.getLocation(), 9);
			for (Player p : mPlayers) {
				p.playSound(p, Sound.ENTITY_ALLAY_DEATH, SoundCategory.HOSTILE, 0.3f, 0.6f);
				p.playSound(p, Sound.ITEM_TOTEM_USE, SoundCategory.HOSTILE, 0.4f, 0.6f);
				p.playSound(p, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.HOSTILE, 0.7f, 0.6f);
				p.playSound(p, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 0.3f, 0.6f);
				p.playSound(p, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.HOSTILE, 0.6f, 0.1f);
			}
			new BukkitRunnable() {
				int mTicks = 0;
				final ChargeUpManager mBar = new ChargeUpManager(mBoss, DAMAGE_PHASE_DURATION, Component.text("Core Exposed", NamedTextColor.RED), BossBar.Color.RED, BossBar.Overlay.NOTCHED_10, 75);

				@Override
				public void run() {
					mBar.nextTick(5);
					if (mHp <= 0) {
						changeHp(true, 1);
						mBar.remove();
						if (passMessage != null && passNpc != null) {
							for (Player p : mPlayers) {
								MessagingUtils.sendNPCMessage(p, passNpc, passMessage);
							}
						}
						for (Player p : mPlayers) {
							p.hideBossBar(mDamagePhaseHPBar);
						}
						mBoss.setGlowing(false);
						mDamagePhase = false;
						startCollision();
						undoDamageVisual();
						this.cancel();
						return;
					}
					if (mTicks > DAMAGE_PHASE_DURATION) {
						mBar.remove();
						for (Player p : mPlayers) {
							p.hideBossBar(mDamagePhaseHPBar);
						}
						if (failMessage != null && failNpc != null) {
							for (Player p : mPlayers) {
								MessagingUtils.sendNPCMessage(p, failNpc, failMessage);
							}
						}
						mBoss.setGlowing(false);
						mDamagePhase = false;
						undoDamageVisual();
						startCollision();
						this.cancel();
					}
					if (mCheeseLock) {
						mBoss.setGlowing(false);
						mDamagePhase = false;
						undoDamageVisual();
						startCollision();
						for (Player p : mPlayers) {
							p.hideBossBar(mDamagePhaseHPBar);
						}
						this.cancel();
					}
					mTicks += 5;
				}
			}.runTaskTimer(mPlugin, 0, 5);
		}
	}


	@Override
	public void onHurt(DamageEvent event) {
		if (mDamagePhase) {
			if (event.getDamager() != null) {
				com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(event.getDamager(), PARTICIPATION_TAG, new CustomTimerEffect(20 * 20, "Participated").displays(false));
			}
			double mDamage = event.getDamage();
			mHp -= mDamage;
			if (mDamagePhaseHPBar != null) {
				mDamagePhaseHPBar.progress(Math.max((float) (mHp / mMaxHp), 0));
			}
		}
		if (event.getType() != DamageEvent.DamageType.TRUE || event.getDamager() != null) {
			event.setCancelled(true);
		}
	}

	@Override
	public void entityPotionEffectEvent(EntityPotionEffectEvent event) {
		if (event.getAction() == EntityPotionEffectEvent.Action.ADDED && event.getNewEffect() != null && event.getNewEffect().getType().equals(PotionEffectType.GLOWING) && !mDamagePhase) {
			event.setCancelled(true);
		}
	}

	public Set<Player> getPlayersInArena(boolean obeyStealth) {
		Set<Player> players = new HashSet<>();
		BoundingBox box = new BoundingBox(mCornerOne.getX(), mCornerOne.getY(), mCornerOne.getZ(), mCornerTwo.getX(), mCornerTwo.getY(), mCornerTwo.getZ());
		for (Player p : PlayerUtils.playersInRange(mSpawnLoc, 100, true, !obeyStealth)) {
			if (p.getBoundingBox().overlaps(box) && p.getGameMode() != GameMode.SPECTATOR) {
				players.add(p);
			}
		}
		return players;
	}

	public Set<Player> getPlayers() {
		return Set.copyOf(mPlayers);
	}

	public Set<Player> getValidDeclarationPlayersInArena() {
		Set<Player> valid = new HashSet<>();
		for (Player p : getPlayers()) {
			Effect declarationParticipation = com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.getActiveEffect(p, FAIL_PARTICIPATION_TAG);
			if (declarationParticipation == null || declarationParticipation.getMagnitude() < 3) {
				valid.add(p);
			}
		}
		return valid;
	}

	public void applyDeclerationFail(boolean pass) {
		for (Player p : getPlayers()) {
			Effect declarationParticipation = com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.getActiveEffect(p, PARTICIPATION_TAG);
			Effect fail = com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.getActiveEffect(p, FAIL_PARTICIPATION_TAG);
			if (declarationParticipation == null) {
				if (fail != null) {
					if (fail.getMagnitude() + 1 == 3) {
						p.sendMessage(Component.text("Aurora and Tuulen’s protection for you against the blight is fading.", NamedTextColor.GRAY));
					}
					com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(p, FAIL_PARTICIPATION_TAG, new CustomTimerEffect(2 * 60 * 20, (int) (1 + fail.getMagnitude()), "").displays(false).deleteOnLogout(true));
				} else {
					com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(p, FAIL_PARTICIPATION_TAG, new CustomTimerEffect(2 * 60 * 20, 1, "").displays(false).deleteOnLogout(true));
				}
			} else {
				com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(p, PARTICIPATION_TAG);
				if (fail != null) {
					com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(p, FAIL_PARTICIPATION_TAG);
					p.sendMessage(Component.text("Aurora and Tuulen’s protection for you against the blight is regaining power.", NamedTextColor.GRAY));
				}
				if (pass) {
					cleanse(p);
				}
			}
		}
	}

	private void cleanse(Player p) {
		Effect blight = EffectManager.getInstance().getActiveEffect(p, PassiveStarBlight.STARBLIGHTAG);
		if (blight != null) {
			double magnitude = blight.getMagnitude() * -1;
			EffectManager.getInstance().clearEffects(p, PassiveStarBlight.STARBLIGHTAG);
			if (magnitude + 5 < 0) {
				p.playSound(p, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.HOSTILE, 0.7f, 1.2f);
				p.playSound(p, Sound.ITEM_TRIDENT_RETURN, SoundCategory.HOSTILE, 0.8f, 1f);
				p.playSound(p, Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.HOSTILE, 0.5f, 1.5f);
				Effect fail = EffectManager.getInstance().getActiveEffect(p, Sirius.FAIL_PARTICIPATION_TAG);
				if (fail == null || fail.getMagnitude() < 3) {
					EffectManager.getInstance().addEffect(p, PassiveStarBlight.STARBLIGHTAG, new StarBlight(PassiveStarBlight.STARBLIGHTDURATION, magnitude + 5, PassiveStarBlight.STARBLIGHTAG));
				} else {
					EffectManager.getInstance().addEffect(p, PassiveStarBlight.STARBLIGHTAG, new StarBlight(PassiveStarBlight.STARBLIGHTDURATION, magnitude + (5 / 2.0), PassiveStarBlight.STARBLIGHTAG));
				}
			}
		}
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		Set<Player> ps = getPlayersInArena(false); // fine, better safe than sorry

		BossUtils.endBossFightEffects(new ArrayList<>(ps));
		mStarBlightConverter.restoreAll();
		mSpawner.wipeMobs();
		removeCollisionBox();
		for (Player p : ps) {
			com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(p, PassiveStarBlight.STARBLIGHTAG);
			com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(p, SpellBlightedBolts.BLIGHTEDBOLTTAG);
		}
		for (BlockDisplay dis : mDisplays) {
			dis.remove();
		}
		mTuulen.remove();
		mAurora.remove();
		removeGate();
		//incase the kill breaks
		mBoss.remove();
	}

	@Override
	public void unload() {
		recalculatePlayers();
		mStarBlightConverter.restoreAll();
		mSpawner.wipeMobs();
		for (Player p : getPlayersInArena(false)) { // fine, better safe than sorry
			com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(p, PassiveStarBlight.STARBLIGHTAG);
			com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(p, SpellBlightedBolts.BLIGHTEDBOLTTAG);
		}
		for (BlockDisplay dis : mDisplays) {
			dis.remove();
		}
		mTuulen.remove();
		mAurora.remove();
		removeGate();
		super.unload();
	}

	private BlockDisplay createBlockDisplay(BlockData data, Matrix4f matrix) {
		return createBlockDisplay(data, matrix, -40);
	}

	private BlockDisplay createBlockDisplay(BlockData data, Matrix4f matrix, int yoffset) {
		BlockDisplay display = mBoss.getWorld().spawn(mBoss.getLocation().subtract(0, yoffset, 0.5f), BlockDisplay.class);
		display.setBlock(data);
		display.setInterpolationDelay(-1);
		display.setTransformationMatrix(matrix);
		display.setBrightness(new Display.Brightness(15, 15));
		display.addScoreboardTag("SiriusDisplay");
		mDisplays.add(display);
		return display;
	}


	public Location getValidLoc() {
		return getValidLoc(0);
	}

	private Location getValidLoc(int attempts) {
		Location loc;
		if (attempts > 20) {
			return mBoss.getLocation();
		} else {
			loc = mBoss.getLocation().add(
				//x
				Math.floor(FastUtils.randomDoubleInRange(-Math.abs(mBoss.getLocation().getX() - mSpawnCornerTwo.getX()), 1)) + 0.5,
				//y
				FastUtils.randomIntInRange(0, 5),
				//z
				Math.floor(FastUtils.randomDoubleInRange(-Math.abs(mSpawnCornerOne.getZ() - mSpawnCornerTwo.getZ()) / 2.0, Math.abs(mSpawnCornerOne.getZ() - mSpawnCornerTwo.getZ()) / 2.0)) + 0.5);
		}
		loc = LocationUtils.fallToGround(loc, mBoss.getLocation().getY() - 10);

		if (loc.getBlock().isSolid() || loc.getY() == mBoss.getLocation().getY() - 10 || LocationUtils.getVectorTo(loc, mBoss.getLocation()).lengthSquared() < 25) {
			return getValidLoc(attempts + 1);
		} else {
			return loc;
		}
	}

	@Override
	public boolean hasNearbyEntityDeathTrigger() {
		return true;
	}

	@Override
	public void nearbyEntityDeath(EntityDeathEvent event) {
		if (!(event.getEntity() instanceof Player) && event.getEntity().getScoreboardTags().contains(MOB_TAG)) {
			if (event.getEntity().getScoreboardTags().contains("Elite")) {
				mMobsKilled += 5;
			} else {
				mMobsKilled++;
			}
			LivingEntity entity = event.getEntity();
			entity.getWorld().playSound(event.getEntity().getLocation(), Sound.BLOCK_SCULK_SENSOR_CLICKING, SoundCategory.HOSTILE, 0.4f, 0.1f);
			new PPLine(Particle.REDSTONE, entity.getLocation(), mBoss.getLocation()).countPerMeter(2).data(new Particle.DustOptions(Color.fromRGB(0, 128, 128), 0.5f)).spawnAsBoss();
			if (mMobsKilled >= mMobsToMove) {
				mMobsKilled = 0;
				changeHp(false, 1);
			}
		}
	}


	private void startCollisionBox() {
		updateCollisionBox(0);
	}


	public void updateCollisionBox(int blocksMoved) {
		if (!mBoss.isDead()) {
			Location loc = mBoss.getLocation().subtract(2, 2, 2);
			//clean previous
			Location old = loc.clone().subtract(blocksMoved + 1, 0, 0);
			checkHitboxArea(blocksMoved + 1, old, Material.BARRIER, Material.AIR);
			old = loc.clone().add(6 + blocksMoved, 0, 0);
			checkHitboxArea(blocksMoved + 6, old, Material.BARRIER, Material.AIR);
			if (mCollisionOn) {
				checkHitboxArea(6, loc, Material.AIR, Material.BARRIER);
			}
			List<Entity> entities = new ArrayList<>();
			for (Player p : PlayerUtils.playersInRange(getPlayers(), mBoss.getLocation(), 5, true, true)) {
				if (p.getLocation().getBlock().getType().equals(Material.BARRIER)) {
					entities.add(p);
				}
			}
			for (Entity e : EntityUtils.getNearbyMobs(mBoss.getLocation(), 5.0, mBoss, true)) {
				if (e.getLocation().getBlock().getType().equals(Material.BARRIER) && !e.equals(mBoss)) {
					entities.add(e);
				}
			}
			for (Entity e : entities) {
				Vector vec = LocationUtils.getDirectionTo(e.getLocation().clone(), mBoss.getLocation());
				vec.setY(0);
				vec.normalize();
				//force them towards the tomb
				if (vec.getX() == 0 && vec.getY() == 0) {
					vec.setX(-1);
				}
				Location playerLoc = e.getLocation().clone();
				while (playerLoc.getBlock().getType().equals(Material.BARRIER)) {
					playerLoc.add(vec);
				}
				e.teleport(playerLoc);
			}
		}
	}

	private static void checkHitboxArea(int blocksMoved, Location old, Material toReplace, Material replaceWith) {
		for (int x = 0; x < blocksMoved; x++) {
			for (int y = 0; y < 6; y++) {
				for (int z = 0; z < 5; z++) {
					Location current = old.clone().add(x, y, z);
					if (current.getBlock().getType().equals(toReplace)) {
						current.getBlock().setType(replaceWith);
					}
				}
			}
		}
	}

	private void removeCollisionBox() {
		Location loc = mBoss.getLocation().subtract(2, 2, 2);
		checkHitboxArea(6, loc, Material.BARRIER, Material.AIR);

	}

	//arena is too big for nearby playear death also need a timer for scaling declerations
	public void startDeathTracker() {
		new BukkitRunnable() {
			int mTicks = 0;
			Set<Player> mLastTickPlayers = mPlayers;

			@Override
			public void run() {
				Set<Player> mCurrentPlayears = getPlayersInArena(false); // needed.
				mLastTickPlayers.removeAll(mCurrentPlayears);
				for (Player p : mLastTickPlayers) {
					EffectManager.getInstance().clearEffects(p, PassiveStarBlight.STARBLIGHTAG);
					EffectManager.getInstance().clearEffects(p, SpellBlightedBolts.BLIGHTEDBOLTTAG);
					if (mDamagePhaseHPBar != null) {
						p.hideBossBar(mDamagePhaseHPBar);
					}
				}
				if (!mLastTickPlayers.isEmpty()) {
					mPlayers = mCurrentPlayears; // update the current players
					mPlayerCount = mCurrentPlayears.size();
					if (mPlayerCount == 0) {
						loseAnimation();
						mTuulen.remove();
						mAurora.remove();
					} else {
						mMobsToMove = mPlayerCount * 20;
						mDefenseScaling = BossUtils.healthScalingCoef(mPlayerCount, SCALING_X, SCALING_Y);
					}
				}
				if (mTicks % (90 * 20) == 0) {
					mDeclerationScaleAmount += 0.5;
				}
				if (mBoss.isDead()) {
					this.cancel();
				}
				mLastTickPlayers = mCurrentPlayears;
				mTicks += 20;
			}
		}.runTaskTimer(mPlugin, 0, 20);
	}

	public void stopCollision() {
		mCollisionOn = false;
		removeCollisionBox();
	}

	public void startCollision() {
		mCollisionOn = true;
		updateCollisionBox(0);
	}

	public void classlessAdvancementHandler() {
		new BukkitRunnable() {
			int mTicks = 0;
			final HashSet<UUID> mHs = new HashSet<>();
			List<Player> mPlayers = Collections.emptyList();

			@Override
			public void run() {
				mPlayers = new ArrayList<>(getPlayers());
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

				if (mDone) {
					this.cancel();
					mPlayers.forEach(p -> {
						if (mHs.contains(p.getUniqueId()) && AbilityUtils.isClassless(p)) {
							AdvancementUtils.grantAdvancement(p, "monumenta:challenges/r3/sirius/so_sirius");
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

	private void undoDamageVisual() {
		//4 temp displays
		for (Player p : mPlayers) {
			p.playSound(p, Sound.ENTITY_WARDEN_DIG, SoundCategory.HOSTILE, 0.9f, 2f);
			p.playSound(p, Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 0.4f, 0.9f);
			p.playSound(p, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1.1f, 0.6f);
			p.playSound(p, Sound.BLOCK_DEEPSLATE_BRICKS_BREAK, SoundCategory.HOSTILE, 2f, 0.6f);
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				p.playSound(p, Sound.BLOCK_DEEPSLATE_BRICKS_BREAK, SoundCategory.HOSTILE, 2f, 0.6f);
			}, 7);
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				p.playSound(p, Sound.BLOCK_DEEPSLATE_BRICKS_BREAK, SoundCategory.HOSTILE, 2f, 0.6f);
				p.playSound(p, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 0.3f, 0.6f);
			}, 14);
		}
		for (Display display : mTempDisplay) {
			display.remove();
			mDisplays.remove(display);
		}
		for (int i = 0; i < mDisplays.size() - 4; i++) {
			if (!mTentacles.get(0).contains(mDisplays.get(i)) && !mTentacles.get(1).contains(mDisplays.get(i)) && !mTentacles.get(2).contains(mDisplays.get(i)) && !mTentacles.get(3).contains(mDisplays.get(i))) {
				mDisplays.get(i).setInterpolationDuration(10);
				mDisplays.get(i).setInterpolationDelay(-1);
				mDisplays.get(i).setTransformation(mStoredTransformations.get(i));
			}
		}
	}

	private void spawnGate() {
		mGate = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			BlockDisplay display = mBoss.getWorld().spawn(mSpawnLoc.clone().add(-43.5, -10.5, -1.5 - i), BlockDisplay.class);
			display.setBlock(Bukkit.createBlockData(Material.IRON_BARS));
			display.setTransformation(new Transformation(new Vector3f(0, 0, 0), new AxisAngle4f(), new Vector3f(3, 15, 5), new AxisAngle4f()));
			display.setInterpolationDelay(-1);
			display.setInterpolationDuration(20);
			display.addScoreboardTag("SiriusDisplay");
			EntityUtils.setRemoveEntityOnUnload(display);
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> display.setTransformation(new Transformation(new Vector3f(0, 10, 0), new AxisAngle4f(), new Vector3f(3, 11, 5), new AxisAngle4f())), 1);
			mGate.add(display);
		}
	}


	private void removeGate() {
		for (BlockDisplay dis : mGate) {
			dis.setInterpolationDelay(-1);
			dis.setInterpolationDuration(20);
			Transformation trans = dis.getTransformation();
			dis.setTransformation(new Transformation(new Vector3f(0, -trans.getScale().y, 0), trans.getLeftRotation(), trans.getScale(), trans.getRightRotation()));
			Bukkit.getScheduler().runTaskLater(mPlugin, dis::remove, 20);
		}

	}

	private void loseAnimation() {
		mTentacleManager.mCancelMovements = true;
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks == 0) {
					mBoss.setVisibleByDefault(false);
					for (BlockDisplay mDisplay : mDisplays) {
						mDisplay.setInterpolationDuration(39);
						Transformation trans = mDisplay.getTransformation();
						mDisplay.setTransformation(new Transformation(new Vector3f(0, 20, 0).add(trans.getTranslation()), trans.getLeftRotation(), trans.getScale(), trans.getRightRotation()));
						mDisplay.setInterpolationDelay(-1);
					}
				}
				if (mTicks == 40) {
					for (int i = 0; i < mDisplays.size() && i < mStoredTransformations.size(); i++) {
						mDisplays.get(i).setInterpolationDuration(10);
						mDisplays.get(i).setTransformation(mStoredTransformations.get(i));
						mDisplays.get(i).setInterpolationDelay(-1);
					}
				}
				if (mTicks == 50) {
					mStarBlightConverter.blightArena(List.of(mBoss.getLocation()), 0, 40, 20, mPlugin);
				}
				if (mTicks == 60) {
					for (Player p : PlayerUtils.playersInRange(mBoss.getLocation(), 200, true, true)) {
						p.sendMessage(Component.text("[Sirius]", NamedTextColor.GOLD).append(Component.text(" Vermin skittering across this earth. You have no hope of conquering the Stars.", NamedTextColor.AQUA)));
					}
				}
				if (mTicks > 160) {
					this.cancel();
					removeGate();
					mBoss.remove();
					mStarBlightConverter.restoreAll();
					mSpawner.wipeMobs();
					for (Display dis : mDisplays) {
						dis.remove();
					}
				}
				mTicks++;

			}
		}.runTaskTimer(mPlugin, 5, 1);

	}

	private void startVisual() {
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks == 1) {
					for (BlockDisplay mDisplay : mDisplays) {
						mDisplay.setInterpolationDelay(-1);
						mDisplay.setInterpolationDuration(20);
						Transformation trans = mDisplay.getTransformation();
						mDisplay.setTransformation(new Transformation(trans.getTranslation().sub(0, 42, 0), trans.getLeftRotation(), trans.getScale(), trans.getRightRotation()));
					}
					for (List<BlockDisplay> displays : mTentacles) {
						for (int i = 1; i < displays.size(); i++) {
							Display dis = displays.get(i);
							dis.setVisibleByDefault(false);
							Transformation trans = dis.getTransformation();
							Vector3f scale = new Vector3f(0, 0, 0);
							dis.setTransformation(new Transformation(trans.getTranslation(), trans.getLeftRotation(), scale, trans.getRightRotation()));
						}
					}

				}
				if (mTicks < 21) {
					Vector direction = mBoss.getLocation().add(0, 42 - mTicks * 2.1, 0).toVector().subtract(mBoss.getLocation().add(0, 42 - mTicks * 2.1 - 2, 0).toVector()).normalize();
					new PPCircle(Particle.FLAME, mBoss.getLocation().add(0, 42 - mTicks * 2.1, 0), 3.5).count(50).extra(1).delta(-direction.getX(), direction.getY(), -direction.getZ()).directionalMode(true).ringMode(false).spawnAsBoss();
					new PPCircle(Particle.FLAME, mBoss.getLocation().add(0, 42 - mTicks * 2.1, 0), 3.7).count(30).ringMode(true).spawnAsBoss();
				}
				if (mTicks % 5 == 0) {
					mBoss.getWorld().playSound(mBoss.getLocation().add(0, 42 - mTicks * 2.1 - 2, 0), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 2, 1);
				}


				if (mTicks == 22) {
					for (Player p : PlayerUtils.playersInRange(mPlayers, mBoss.getLocation(), 12, true, false)) {
						double maxOriginalDamage = 1000;
						Vector vec = LocationUtils.getVectorTo(mBoss.getLocation().clone(), p.getLocation().clone());
						vec.setY(0); //remove y
						double ratio = Math.max(0, (12 - vec.length()) / (5 * vec.length()));
						if (ratio >= 0.5) {
							com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(p, "Stasis");
							com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(p, VoodooBonds.PROTECTION_EFFECT);
							if (p.isInvulnerable()) {
								p.setInvulnerable(false);
							}
							DamageUtils.damage(mBoss, p, DamageEvent.DamageType.OTHER, 999999999, null, true, false, "crushing weight.");
							if (p.getHealth() > 0) {
								p.setHealth(0); // For good measure
							}
							AdvancementUtils.grantAdvancement(p, "monumenta:challenges/r3/sirius/starfall");
							mPlayers.remove(p); // get rid of them early on.
						} else {
							DamageUtils.damage(mBoss, p, DamageEvent.DamageType.BLAST, maxOriginalDamage * ratio, null, true, true, "crushing weight.");
						}
					}
					for (int i = 0; i < mStoredTransformations.size(); i++) {
						Display mDisplay = mDisplays.get(i);
						mDisplay.teleport(mBoss.getLocation().subtract(0, 2, 0.5));
						mDisplay.setInterpolationDelay(-1);
						mDisplay.setInterpolationDuration(0);
						mDisplay.setTransformation(mStoredTransformations.get(i));
					}
					for (List<BlockDisplay> displays : mTentacles) {
						for (int i = 1; i < displays.size(); i++) {
							Display dis = displays.get(i);
							dis.setVisibleByDefault(false);
							Transformation trans = dis.getTransformation();
							Vector3f scale = new Vector3f(trans.getScale().x, 0, trans.getScale().z);
							dis.setInterpolationDelay(-1);
							dis.setInterpolationDuration(0);
							dis.setTransformation(new Transformation(trans.getTranslation(), trans.getLeftRotation(), scale, trans.getRightRotation()));
						}
					}

					new BukkitRunnable() {
						int mPos = 1;

						@Override
						public void run() {
							if (mPos >= mTentacles.get(0).size()) {
								this.cancel();
								return;
							}
							for (int i = 0; i < 4; i++) {
								mTentacles.get(i).get(mPos).setVisibleByDefault(true);
							}
							Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
								for (int i = 0; i < 4; i++) {
									Display dis = mTentacles.get(i).get(mPos);
									for (Player p : PlayerUtils.playersInRange(mPlayers, dis.getLocation(), 5, true, true)) {
										p.playSound(p, Sound.BLOCK_CHORUS_FLOWER_GROW, SoundCategory.HOSTILE, 5, 0.5f);
									}
									dis.setInterpolationDelay(-1);
									dis.setInterpolationDuration(9);
									dis.setTransformation(mTentacleRestTransforms.get(i).get(mPos));
									dis.setInterpolationDelay(-1);
									dis.setInterpolationDuration(9);
									World world = dis.getWorld();
									world.playSound(dis.getLocation(), Sound.ENTITY_WARDEN_EMERGE, SoundCategory.HOSTILE, 0.8f, 2);
									world.playSound(dis.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 0.3f, 0.6f);
									world.playSound(dis.getLocation(), Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.HOSTILE, 1.1f, 0.7f);
									world.playSound(dis.getLocation(), Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.HOSTILE, 1.3f, 0.1f);
								}
								mPos++;
							}, 1);
						}
					}.runTaskTimer(mPlugin, 1, 9);
					mBoss.setVisibleByDefault(true);
					Location loc = mBoss.getLocation().subtract(0, 2, 0);
					new PPCircle(Particle.EXPLOSION_LARGE, loc, 12).count(30).ringMode(false).spawnAsBoss();
					new PPCircle(Particle.BLOCK_DUST, loc, 12).count(100).ringMode(false).data(Bukkit.createBlockData(Material.COBBLED_DEEPSLATE)).spawnAsBoss();
					new PPCircle(Particle.BLOCK_DUST, loc, 12).count(100).ringMode(false).data(Bukkit.createBlockData(Material.AMETHYST_BLOCK)).spawnAsBoss();
					new PPCircle(Particle.BLOCK_DUST, loc, 12).count(100).ringMode(false).data(Bukkit.createBlockData(Material.POLISHED_DEEPSLATE)).spawnAsBoss();
					new PPCircle(Particle.FLAME, loc, 12).count(60).ringMode(false).spawnAsBoss();
					new PPCircle(Particle.LAVA, loc, 12).count(60).ringMode(false).spawnAsBoss();
					//randomize smoke about
					for (double theta = 0; theta < 2 * Math.PI; theta += Math.PI / 8) {
						int r = FastUtils.randomIntInRange(3, 12);
						new PartialParticle(Particle.LAVA, mBoss.getLocation().add(r * Math.cos(theta), 0, r * Math.sin(theta)), 5).delta(0, 3, 0).spawnAsBoss();
					}
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.PLAYERS, 1.0f, 0.1f);
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 0.6f, 2.0f);
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 2.0f, 0.1f);
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 2.0f, 0.1f);
					for (Player p : mPlayers) {
						com.playmonumenta.plugins.utils.MessagingUtils.sendBoldTitle(p, Component.text("Sirius", NamedTextColor.AQUA), Component.text("The Final Herald", NamedTextColor.DARK_AQUA));

						p.sendMessage(Component.text("[Tuulen]", NamedTextColor.GOLD).append(Component.text(" Is it Sirius?", NamedTextColor.GRAY)));
						p.sendMessage(Component.text("[Aurora]", NamedTextColor.GOLD).append(Component.text(" It's very serious - it's coming right for us!", NamedTextColor.DARK_PURPLE)));
					}
					new BukkitRunnable() {
						final int DURATION = 3 * 20;
						int mTicks = 0;

						@Override
						public void run() {
							mStarBlightConverter.convertHalfSphere((int) (90 * (mTicks / ((double) DURATION))));
							if (mTicks > DURATION) {
								updateCollisionBox(0);
								this.cancel();
							}
							mTicks++;
						}
					}.runTaskTimer(mPlugin, 0, 1);
				}
				if (mTicks == 23) {
					startCollisionBox();
					this.cancel();
					Location loc = mBoss.getLocation().subtract(0, 2, 0);
					new PPCircle(Particle.EXPLOSION_LARGE, loc, 12).count(30).ringMode(false).spawnAsBoss();
					new PPCircle(Particle.BLOCK_DUST, loc, 12).count(100).ringMode(false).data(Bukkit.createBlockData(Material.COBBLED_DEEPSLATE)).spawnAsBoss();
					new PPCircle(Particle.BLOCK_DUST, loc, 12).count(100).ringMode(false).data(Bukkit.createBlockData(Material.AMETHYST_BLOCK)).spawnAsBoss();
					new PPCircle(Particle.BLOCK_DUST, loc, 12).count(100).ringMode(false).data(Bukkit.createBlockData(Material.POLISHED_DEEPSLATE)).spawnAsBoss();
					new PPCircle(Particle.FLAME, loc, 12).count(60).ringMode(false).spawnAsBoss();
					new PPCircle(Particle.LAVA, loc, 12).count(60).ringMode(false).spawnAsBoss();
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}


	//Visuals of the boss and is not worth reading. Skip to 2260 for more code
	private void makeVisual() {
		AmethystCluster mClusterBlockData = (AmethystCluster) Bukkit.createBlockData(Material.AMETHYST_CLUSTER);
		mClusterBlockData.setFacing(BlockFace.DOWN);
		createBlockDisplay(
			Bukkit.createBlockData(Material.BLACKSTONE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.3750f, 0.2500f, 0.8125f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.BLACKSTONE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.6250f, 0.0000f, 0.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.BLACKSTONE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.8125f, 0.2500f, -0.8125f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.BLACKSTONE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 1.5000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.MUD),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -0.7500f, 0.2500f, 1.3125f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.MUD),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.8125f, 0.2500f, -1.5000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.BLACKSTONE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, -1.6875f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.BLACKSTONE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -0.8125f, 0.2500f, -1.5625f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.MUD),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.5000f, 0.3125f, -0.8750f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.BLACKSTONE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.8750f, 0.1875f, 0.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.ANDESITE),
			new Matrix4f(3.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.0000f, 4.0000f, 0.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.9375f, 2.0000f, 1.8750f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.COBBLESTONE),
			new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 0.0000f, 3.0000f, 3.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.COBBLESTONE),
			new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 1.0000f, 3.0000f, 3.0625f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.ANDESITE),
			new Matrix4f(2.0625f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.0000f, 4.0000f, 1.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -0.3750f, 3.8125f, 1.5000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.COBBLED_DEEPSLATE),
			new Matrix4f(3.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 3.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 3.0000f, 0.0000f, -1.0000f, 1.0000f, -1.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.COBBLED_DEEPSLATE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.0000f, 1.0000f, 2.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.MUD),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 1.0000f, 2.0625f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.SMOOTH_BASALT),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.0000f, 1.0000f, 2.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.SMOOTH_BASALT),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.7500f, 1.0000f, 1.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 2.0625f, 1.0000f, 0.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.MUD),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 2.0000f, 1.0000f, -1.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.SMOOTH_BASALT),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.0000f, 1.0000f, -2.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.MUD),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 1.0000f, -2.0625f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.SMOOTH_BASALT),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.0000f, 1.0000f, -2.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.MUD),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.5625f, 1.2500f, -1.5625f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.COBBLED_DEEPSLATE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -2.0000f, 1.0000f, -1.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.MUD),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -2.0625f, 1.0000f, 0.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.SMOOTH_BASALT),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -2.0000f, 1.0000f, 1.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.6875f, 2.0000f, -1.7500f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.COBBLED_DEEPSLATE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.6250f, 2.0000f, -1.7500f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0625f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 2.5625f, 3.0000f, 2.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.COBBLESTONE),
			new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 2.8125f, 3.0000f, 1.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.COBBLED_DEEPSLATE),
			new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 2.6250f, 3.0000f, 0.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.COBBLESTONE),
			new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 2.0000f, 3.0000f, -1.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.COBBLED_DEEPSLATE),
			new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 1.0000f, 3.0000f, -1.0625f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.COBBLED_DEEPSLATE),
			new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 0.0000f, 3.0000f, -0.8750f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, -1.0000f, 3.0000f, 0.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.COBBLED_DEEPSLATE),
			new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, -1.0625f, 3.0000f, 1.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.BLACK_CONCRETE),
			new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, -0.8125f, 3.0000f, 2.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.ANDESITE),
			new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 2.2500f, 2.7500f, -0.5000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.COBBLESTONE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.3750f, 3.7500f, -0.7500f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.ANDESITE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.5000f, 3.7500f, 0.5625f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.COBBLESTONE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.3125f, 3.8750f, 1.4375f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.COBBLESTONE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.7500f, 3.7500f, 0.9375f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.ANDESITE),
			new Matrix4f(3.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.0000f, 3.8125f, -1.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 3.5625f, -1.8750f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.ANDESITE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.7500f, 3.3750f, -1.7500f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.COBBLESTONE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -0.7500f, 3.3750f, -1.7500f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.ANDESITE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.7500f, 3.7500f, -0.7500f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.COBBLED_DEEPSLATE),
			new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, -0.3125f, 2.7500f, -0.6250f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.MUD),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.8125f, 0.2500f, 1.8125f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.5625f, 1.2500f, 1.7500f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.COBBLED_DEEPSLATE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.6875f, 1.2500f, 1.5625f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.BLACKSTONE),
			new Matrix4f(2.9375f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 3.0000f, 0.0000f, -0.9375f, 0.0000f, -1.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -2.1875f, 2.0000f, -1.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.COBBLED_DEEPSLATE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -2.3125f, 2.0000f, 0.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.COBBLED_DEEPSLATE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -2.1875f, 2.0000f, 1.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, -0.8750f, 3.0000f, 2.8125f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.BLACK_CONCRETE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.0000f, 2.0000f, -1.6875f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.0000f, 2.0000f, -2.1875f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.SMOOTH_BASALT),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 2.0000f, -2.3125f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.BLACK_CONCRETE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.7500f, 2.0000f, 1.5625f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.0000f, 2.0000f, 2.1875f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.SMOOTH_BASALT),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 2.0000f, 2.3125f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.COBBLESTONE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.8750f, 3.8750f, 0.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.ANDESITE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.6250f, 3.6875f, 0.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.STONE),
			new Matrix4f(2.0625f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 2.0000f, 0.0000f, -0.4375f, 4.1875f, -0.4375f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.BLACKSTONE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.8125f, 0.2500f, 0.8125f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 2.0000f, 3.0000f, 2.7500f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.ANDESITE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.7500f, 3.6250f, 1.3125f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.BLACK_CONCRETE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.4375f, 2.0000f, -1.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.COBBLED_DEEPSLATE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.8125f, 2.0000f, 0.0000f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.6250f, 2.0000f, 0.9375f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.SMOOTH_BASALT),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.7500f, 2.0625f, 1.6250f, 1.0000f)
		);
		mTentacles.get(0).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(0.5389f, -0.2174f, -0.1496f, 0.0000f, 0.2639f, 0.4427f, 0.3072f, 0.0000f, -0.0009f, -0.3417f, 0.4932f, 0.0000f, -2.3750f, 3.6875f, 1.1316f, 1.0000f)
		));

		mTentacles.get(0).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(0.0792f, 0.3062f, -0.3873f, 0.0000f, -0.8365f, 0.5000f, 0.2241f, 0.0000f, 0.2623f, 0.3062f, 0.2958f, 0.0000f, -2.3125f, 3.3793f, 1.5435f, 1.0000f)
		));
		mTentacles.get(0).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(0.1246f, 0.2202f, -0.3098f, 0.0000f, -0.6786f, 0.6995f, 0.2241f, 0.0000f, 0.2661f, 0.1823f, 0.2366f, 0.0000f, -3.1250f, 3.9544f, 1.8080f, 1.0000f)
		));
		mTentacles.get(0).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(-0.0016f, 0.1898f, -0.2324f, 0.0000f, -0.9374f, 0.2665f, 0.2241f, 0.0000f, 0.1044f, 0.2182f, 0.1775f, 0.0000f, -3.5804f, 4.6447f, 1.9899f, 1.0000f)
		));
		mTentacles.get(0).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(-0.1353f, 0.1330f, -0.2324f, 0.0000f, -0.8513f, -0.4744f, 0.2241f, 0.0000f, -0.0804f, 0.2281f, 0.1775f, 0.0000f, -4.2644f, 4.9509f, 2.1971f, 1.0000f)
		));
		mTentacles.get(0).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(-0.1408f, 0.0365f, -0.1781f, 0.0000f, -0.5000f, -0.8365f, 0.2241f, 0.0000f, -0.1408f, 0.1206f, 0.1360f, 0.0000f, -5.0079f, 4.7293f, 2.4193f, 1.0000f)
		));
		mTentacles.get(0).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(-0.1232f, -0.0286f, -0.1549f, 0.0000f, -0.0560f, -0.9729f, 0.2241f, 0.0000f, -0.1571f, 0.0363f, 0.1183f, 0.0000f, -5.4999f, 4.0000f, 2.6418f, 1.0000f)
		));
		mTentacles.get(0).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(-0.0698f, -0.0380f, -0.1272f, 0.0000f, 0.3202f, -0.9415f, 0.1052f, 0.0000f, -0.1238f, -0.0334f, 0.0779f, 0.0000f, -5.6070f, 3.0990f, 2.8488f, 1.0000f)
		));
		mTentacles.get(1).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(0.1542f, -0.2174f, 0.5376f, 0.0000f, -0.3050f, 0.4427f, 0.2665f, 0.0000f, -0.4932f, -0.3417f, 0.0033f, 0.0000f, 0.0674f, 2.4139f, -1.9125f, 1.0000f)
		));
		mTentacles.get(1).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(0.3879f, 0.3062f, 0.0759f, 0.0000f, -0.2313f, 0.5000f, -0.8346f, 0.0000f, -0.2935f, 0.3062f, 0.2648f, 0.0000f, -0.3439f, 2.1057f, -1.8465f, 1.0000f)
		));
		mTentacles.get(1).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(0.3109f, 0.2202f, 0.1220f, 0.0000f, -0.2299f, 0.6995f, -0.6767f, 0.0000f, -0.2343f, 0.1823f, 0.2681f, 0.0000f, -0.6154f, 2.6807f, -2.6567f, 1.0000f)
		));
		mTentacles.get(1).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(0.2323f, 0.1898f, -0.0036f, 0.0000f, -0.2321f, 0.2665f, -0.9355f, 0.0000f, -0.1766f, 0.2182f, 0.1060f, 0.0000f, -0.8011f, 3.3711f, -3.1106f, 1.0000f)
		));
		mTentacles.get(1).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(0.2312f, 0.1330f, -0.1373f, 0.0000f, -0.2314f, -0.4744f, -0.8493f, 0.0000f, -0.1781f, 0.2281f, -0.0789f, 0.0000f, -1.0142f, 3.6773f, -3.7928f, 1.0000f)
		));
		mTentacles.get(1).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(0.1769f, 0.0365f, -0.1424f, 0.0000f, -0.2284f, -0.8365f, -0.4981f, 0.0000f, -0.1372f, 0.1206f, -0.1397f, 0.0000f, -1.2427f, 3.4556f, -4.5344f, 1.0000f)
		));
		mTentacles.get(1).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(0.1538f, -0.0286f, -0.1246f, 0.0000f, -0.2246f, -0.9729f, -0.0541f, 0.0000f, -0.1196f, 0.0363f, -0.1561f, 0.0000f, -1.4694f, 2.7264f, -5.0245f, 1.0000f)
		));
		mTentacles.get(1).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(0.1266f, -0.0380f, -0.0709f, 0.0000f, -0.1025f, -0.9415f, 0.3211f, 0.0000f, -0.0789f, -0.0334f, -0.1231f, 0.0000f, -1.6773f, 1.8253f, -5.1298f, 1.0000f)
		));
		mTentacles.get(2).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(-0.5389f, -0.2174f, 0.1496f, 0.0000f, -0.2639f, 0.4427f, -0.3072f, 0.0000f, 0.0009f, -0.3417f, -0.4932f, 0.0000f, 2.7902f, 2.5693f, -0.0465f, 1.0000f)
		));
		mTentacles.get(2).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(-0.0792f, 0.3062f, 0.3873f, 0.0000f, 0.8365f, 0.5000f, -0.2241f, 0.0000f, -0.2623f, 0.3062f, -0.2958f, 0.0000f, 2.7277f, 2.2611f, -0.4584f, 1.0000f)
		));
		mTentacles.get(2).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(-0.1246f, 0.2202f, 0.3098f, 0.0000f, 0.6786f, 0.6995f, -0.2241f, 0.0000f, -0.2661f, 0.1823f, -0.2366f, 0.0000f, 3.5402f, 2.8362f, -0.7229f, 1.0000f)
		));
		mTentacles.get(2).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(0.0016f, 0.1898f, 0.2324f, 0.0000f, 0.9374f, 0.2665f, -0.2241f, 0.0000f, -0.1044f, 0.2182f, -0.1775f, 0.0000f, 3.9956f, 3.5265f, -0.9048f, 1.0000f)
		));
		mTentacles.get(2).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(0.1353f, 0.1330f, 0.2324f, 0.0000f, 0.8513f, -0.4744f, -0.2241f, 0.0000f, 0.0804f, 0.2281f, -0.1775f, 0.0000f, 4.6796f, 3.8327f, -1.1121f, 1.0000f)
		));
		mTentacles.get(2).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(0.1408f, 0.0365f, 0.1781f, 0.0000f, 0.5000f, -0.8365f, -0.2241f, 0.0000f, 0.1408f, 0.1206f, -0.1360f, 0.0000f, 5.4231f, 3.6111f, -1.3342f, 1.0000f)
		));
		mTentacles.get(2).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(0.1232f, -0.0286f, 0.1549f, 0.0000f, 0.0560f, -0.9729f, -0.2241f, 0.0000f, 0.1571f, 0.0363f, -0.1183f, 0.0000f, 5.9151f, 2.8818f, -1.5568f, 1.0000f)
		));
		mTentacles.get(2).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(0.0698f, -0.0380f, 0.1272f, 0.0000f, -0.3202f, -0.9415f, -0.1052f, 0.0000f, 0.1238f, -0.0334f, -0.0779f, 0.0000f, 6.0222f, 1.9808f, -1.7638f, 1.0000f)
		));
		mTentacles.get(3).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(-0.1496f, -0.2174f, -0.5389f, 0.0000f, 0.3072f, 0.4427f, -0.2639f, 0.0000f, 0.4932f, -0.3417f, 0.0009f, 0.0000f, 1.0246f, 2.5850f, 3.1012f, 1.0000f)
		));
		mTentacles.get(3).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(-0.3873f, 0.3062f, -0.0792f, 0.0000f, 0.2241f, 0.5000f, 0.8365f, 0.0000f, 0.2958f, 0.3062f, -0.2623f, 0.0000f, 1.4365f, 2.2767f, 3.0387f, 1.0000f)
		));
		mTentacles.get(3).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(-0.3098f, 0.2202f, -0.1246f, 0.0000f, 0.2241f, 0.6995f, 0.6786f, 0.0000f, 0.2366f, 0.1823f, -0.2661f, 0.0000f, 1.7010f, 2.8518f, 3.8512f, 1.0000f)
		));
		mTentacles.get(3).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(-0.2324f, 0.1898f, 0.0016f, 0.0000f, 0.2241f, 0.2665f, 0.9374f, 0.0000f, 0.1775f, 0.2182f, -0.1044f, 0.0000f, 1.8829f, 3.5422f, 4.3066f, 1.0000f)
		));
		mTentacles.get(3).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(-0.2324f, 0.1330f, 0.1353f, 0.0000f, 0.2241f, -0.4744f, 0.8513f, 0.0000f, 0.1775f, 0.2281f, 0.0804f, 0.0000f, 2.0902f, 3.8484f, 4.9906f, 1.0000f)
		));
		mTentacles.get(3).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(-0.1781f, 0.0365f, 0.1408f, 0.0000f, 0.2241f, -0.8365f, 0.5000f, 0.0000f, 0.1360f, 0.1206f, 0.1408f, 0.0000f, 2.3123f, 3.6267f, 5.7341f, 1.0000f)
		));
		mTentacles.get(3).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(-0.1549f, -0.0286f, 0.1232f, 0.0000f, 0.2241f, -0.9729f, 0.0560f, 0.0000f, 0.1183f, 0.0363f, 0.1571f, 0.0000f, 2.5348f, 2.8975f, 6.2261f, 1.0000f)
		));
		mTentacles.get(3).add(createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(-0.1272f, -0.0380f, 0.0698f, 0.0000f, 0.1052f, -0.9415f, -0.3202f, 0.0000f, 0.0779f, -0.0334f, 0.1238f, 0.0000f, 2.7418f, 1.9964f, 6.3332f, 1.0000f)
		));
		createBlockDisplay(
			Bukkit.createBlockData(Material.CALCITE),
			new Matrix4f(-0.1785f, 0.8703f, 0.4590f, 0.0000f, -0.6494f, 0.2462f, -0.7195f, 0.0000f, -0.7392f, -0.4265f, 0.5212f, 0.0000f, -0.7090f, 2.3407f, -1.3630f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.AMETHYST_BLOCK),
			new Matrix4f(-0.3493f, 0.4644f, 0.4742f, 0.0000f, -0.1948f, 0.0739f, -0.2158f, 0.0000f, -0.4509f, -0.5592f, 0.2156f, 0.0000f, -1.4328f, 2.8464f, -1.9265f, 1.0000f)
		);
		createBlockDisplay(
			mClusterBlockData,
			new Matrix4f(-0.4969f, 0.5788f, 0.6466f, 0.0000f, 0.6494f, -0.2462f, 0.7195f, 0.0000f, 0.5756f, 0.7774f, -0.2535f, 0.0000f, -2.6520f, 2.4513f, -2.6987f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-0.0894f, 0.4113f, -0.2699f, 0.0000f, -0.3698f, -0.2371f, -0.2388f, 0.0000f, -0.3245f, 0.1569f, 0.3466f, 0.0000f, -1.5654f, 1.9213f, -1.3900f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-0.0490f, -0.2778f, -0.4128f, 0.0000f, -0.4888f, 0.1044f, -0.0122f, 0.0000f, 0.0930f, 0.4024f, -0.2819f, 0.0000f, -1.0354f, 2.5070f, -1.6196f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-0.4192f, -0.2666f, -0.0565f, 0.0000f, -0.2006f, 0.3719f, -0.2673f, 0.0000f, 0.1845f, -0.2014f, -0.4188f, 0.0000f, -1.3488f, 2.2016f, -1.5636f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(0.2007f, 0.1309f, -0.4389f, 0.0000f, -0.3572f, -0.2551f, -0.2394f, 0.0000f, -0.2866f, 0.4096f, -0.0089f, 0.0000f, -1.5473f, 3.3017f, -0.9263f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-0.3575f, -0.2846f, -0.2029f, 0.0000f, -0.3489f, 0.2739f, 0.2307f, 0.0000f, -0.0202f, 0.3066f, -0.3945f, 0.0000f, -0.9802f, 3.2752f, -1.3219f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-0.4262f, 0.2320f, 0.1205f, 0.0000f, -0.0563f, 0.1436f, -0.4756f, 0.0000f, -0.2553f, -0.4190f, -0.0963f, 0.0000f, -0.7988f, 3.1080f, -1.6274f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-0.2610f, -0.3587f, -0.2308f, 0.0000f, -0.3244f, 0.3426f, -0.1655f, 0.0000f, 0.2769f, 0.0634f, -0.4115f, 0.0000f, -2.0722f, 2.4196f, -0.7137f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-0.0490f, -0.2778f, -0.4128f, 0.0000f, -0.4888f, 0.1044f, -0.0122f, 0.0000f, 0.0930f, 0.4024f, -0.2819f, 0.0000f, -1.9220f, 2.9467f, -0.6689f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.CALCITE),
			new Matrix4f(-0.2516f, -0.9659f, -0.0606f, 0.0000f, 0.9390f, -0.2588f, 0.2263f, 0.0000f, -0.2343f, 0.0000f, 0.9722f, 0.0000f, 2.1265f, 2.9033f, 1.2937f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.AMETHYST_BLOCK),
			new Matrix4f(-0.2441f, -0.6609f, 0.2572f, 0.0000f, 0.2817f, -0.0776f, 0.0679f, 0.0000f, -0.0830f, 0.2968f, 0.6838f, 0.0000f, 2.9814f, 2.3443f, 1.5262f, 1.0000f)
		);
		createBlockDisplay(
			mClusterBlockData,
			new Matrix4f(-0.3309f, -0.8589f, 0.3908f, 0.0000f, -0.9390f, 0.2588f, -0.2263f, 0.0000f, 0.0932f, -0.4419f, -0.8922f, 0.0000f, 4.1198f, 2.4851f, 2.4869f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(0.3277f, -0.3330f, -0.1782f, 0.0000f, 0.3363f, 0.1500f, 0.3382f, 0.0000f, -0.1718f, -0.3415f, 0.3223f, 0.0000f, 2.4395f, 2.9979f, 2.1899f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(0.3650f, 0.3407f, 0.0258f, 0.0000f, 0.2143f, -0.2577f, 0.3710f, 0.0000f, 0.2661f, -0.2598f, -0.3342f, 0.0000f, 2.5074f, 2.7077f, 1.4231f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(0.1859f, 0.1129f, 0.4502f, 0.0000f, 0.3644f, -0.3360f, -0.0661f, 0.0000f, 0.2876f, 0.3527f, -0.2072f, 0.0000f, 2.5447f, 2.8628f, 1.8345f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(0.3369f, 0.0628f, -0.3641f, 0.0000f, 0.3299f, 0.1707f, 0.3347f, 0.0000f, 0.1663f, -0.4658f, 0.0736f, 0.0000f, 2.1638f, 1.6364f, 1.7527f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(0.2934f, 0.1878f, 0.3587f, 0.0000f, -0.0433f, -0.4258f, 0.2584f, 0.0000f, 0.4026f, -0.1827f, -0.2336f, 0.0000f, 2.3000f, 1.9552f, 1.1539f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(0.0829f, -0.3859f, 0.3069f, 0.0000f, 0.4725f, -0.0267f, -0.1613f, 0.0000f, 0.1409f, 0.3168f, 0.3603f, 0.0000f, 2.4892f, 2.2467f, 0.9711f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(0.2728f, 0.2949f, 0.2978f, 0.0000f, 0.3167f, -0.3777f, 0.0840f, 0.0000f, 0.2745f, 0.1428f, -0.3928f, 0.0000f, 2.0763f, 2.2008f, 2.6317f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(0.3650f, 0.3407f, 0.0258f, 0.0000f, 0.2143f, -0.2577f, 0.3710f, 0.0000f, 0.2661f, -0.2598f, -0.3342f, 0.0000f, 2.0357f, 1.7638f, 2.3005f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.CALCITE),
			new Matrix4f(0.7594f, 0.3310f, 0.5601f, 0.0000f, -0.5585f, -0.1097f, 0.8222f, 0.0000f, 0.3336f, -0.9372f, 0.1016f, 0.0000f, -1.5390f, 2.7891f, 1.9264f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.AMETHYST_BLOCK),
			new Matrix4f(0.6221f, -0.0615f, 0.4144f, 0.0000f, -0.1676f, -0.0329f, 0.2467f, 0.0000f, -0.0051f, -0.7429f, -0.1026f, 0.0000f, -1.8530f, 2.7582f, 2.9252f, 1.0000f)
		);
		createBlockDisplay(
			mClusterBlockData,
			new Matrix4f(0.8279f, -0.1344f, 0.5445f, 0.0000f, 0.5585f, 0.1097f, -0.8222f, 0.0000f, 0.0508f, 0.9848f, 0.1659f, 0.0000f, -2.6936f, 1.8365f, 3.7515f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-0.0910f, 0.2783f, 0.4053f, 0.0000f, -0.3039f, -0.3559f, 0.1761f, 0.0000f, 0.3865f, -0.2143f, 0.2339f, 0.0000f, -1.7071f, 1.9152f, 2.2699f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-0.4865f, -0.1118f, -0.0286f, 0.0000f, 0.0380f, -0.2725f, 0.4175f, 0.0000f, -0.1090f, 0.4041f, 0.2736f, 0.0000f, -1.7065f, 2.7350f, 2.3375f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-0.1457f, -0.4571f, 0.1407f, 0.0000f, -0.1014f, 0.1733f, 0.4579f, 0.0000f, -0.4674f, 0.1049f, -0.1431f, 0.0000f, -1.7649f, 2.2978f, 2.3465f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-0.3597f, 0.3410f, 0.0660f, 0.0000f, -0.3116f, -0.3587f, 0.1557f, 0.0000f, 0.1535f, 0.0709f, 0.4705f, 0.0000f, -0.7527f, 2.7206f, 3.0191f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-0.2890f, -0.3879f, 0.1266f, 0.0000f, 0.3244f, -0.1245f, 0.3595f, 0.0000f, -0.2474f, 0.2899f, 0.3236f, 0.0000f, -1.1396f, 3.2048f, 2.7113f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(0.2081f, -0.1782f, 0.4183f, 0.0000f, -0.3838f, 0.1777f, 0.2667f, 0.0000f, -0.2437f, -0.4320f, -0.0628f, 0.0000f, -1.4892f, 3.3009f, 2.5606f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-0.3450f, -0.3615f, 0.0181f, 0.0000f, -0.0161f, 0.0403f, 0.4981f, 0.0000f, -0.3616f, 0.3431f, -0.0395f, 0.0000f, -0.8822f, 1.7129f, 2.7610f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-0.4865f, -0.1118f, -0.0286f, 0.0000f, 0.0380f, -0.2725f, 0.4175f, 0.0000f, -0.1090f, 0.4041f, 0.2736f, 0.0000f, -0.6429f, 2.1556f, 2.9827f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.CALCITE),
			new Matrix4f(0.1113f, -0.4982f, -0.8599f, 0.0000f, -0.0036f, 0.8651f, -0.5017f, 0.0000f, 0.9938f, 0.0590f, 0.0945f, 0.0000f, 0.0251f, 4.2618f, -0.0764f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.AMETHYST_BLOCK),
			new Matrix4f(0.3815f, -0.3227f, -0.5593f, 0.0000f, -0.0011f, 0.2595f, -0.1505f, 0.0000f, 0.6457f, 0.1934f, 0.3289f, 0.0000f, 0.0816f, 4.9735f, -0.8428f, 1.0000f)
		);
		createBlockDisplay(
			mClusterBlockData,
			new Matrix4f(0.5536f, -0.4160f, -0.7214f, 0.0000f, 0.0036f, -0.8651f, 0.5017f, 0.0000f, -0.8327f, -0.2804f, -0.4774f, 0.0000f, 0.6867f, 6.3337f, -0.9935f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-0.2219f, 0.1666f, -0.4160f, 0.0000f, 0.2193f, 0.4451f, 0.0612f, 0.0000f, 0.3907f, -0.1553f, -0.2706f, 0.0000f, 0.7913f, 4.8292f, -0.0448f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-0.1080f, 0.4322f, 0.2270f, 0.0000f, 0.3264f, 0.2368f, -0.2956f, 0.0000f, -0.3630f, 0.0843f, -0.3334f, 0.0000f, 0.0694f, 4.5999f, -0.3657f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(0.3704f, 0.3283f, 0.0707f, 0.0000f, -0.1239f, 0.2314f, -0.4256f, 0.0000f, -0.3122f, 0.2977f, 0.2528f, 0.0000f, 0.4373f, 4.7909f, -0.2146f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-0.4447f, 0.2242f, -0.0441f, 0.0000f, 0.2154f, 0.4436f, 0.0828f, 0.0000f, 0.0762f, 0.0546f, -0.4911f, 0.0000f, 0.5903f, 4.1002f, -1.2895f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(0.2463f, 0.4210f, 0.1101f, 0.0000f, 0.3046f, -0.0764f, -0.3891f, 0.0000f, -0.3108f, 0.2587f, -0.2941f, 0.0000f, -0.0537f, 4.1385f, -1.0392f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(0.3134f, 0.0642f, -0.3843f, 0.0000f, -0.2766f, 0.3840f, -0.1614f, 0.0000f, 0.2743f, 0.3138f, 0.2762f, 0.0000f, -0.3100f, 4.3365f, -0.8172f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(0.1819f, 0.4122f, 0.2167f, 0.0000f, 0.0371f, 0.2191f, -0.4479f, 0.0000f, -0.4643f, 0.1790f, 0.0492f, 0.0000f, 1.3958f, 4.4137f, -0.6964f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-0.1080f, 0.4322f, 0.2270f, 0.0000f, 0.3264f, 0.2368f, -0.2956f, 0.0000f, -0.3630f, 0.0843f, -0.3334f, 0.0000f, 1.1356f, 4.1698f, -1.1150f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.CALCITE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.8911f, 0.4538f, 0.0000f, 0.0000f, -0.4538f, 0.8911f, 0.0000f, -0.0675f, 3.9705f, 1.4352f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.AMETHYST_BLOCK),
			new Matrix4f(0.6842f, -0.1394f, 0.2738f, 0.0000f, 0.0000f, 0.2673f, 0.1361f, 0.0000f, -0.3073f, -0.3105f, 0.6097f, 0.0000f, 0.2432f, 4.8499f, 1.9119f, 1.0000f)
		);
		createBlockDisplay(
			mClusterBlockData,
			new Matrix4f(0.8892f, -0.2076f, 0.4077f, 0.0000f, -0.0000f, -0.8911f, -0.4538f, 0.0000f, 0.4575f, 0.4035f, -0.7924f, 0.0000f, -0.2375f, 5.6610f, 3.0737f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(0.2500f, 0.4285f, -0.0623f, 0.0000f, -0.2500f, 0.2016f, 0.3832f, 0.0000f, 0.3536f, -0.1604f, 0.3151f, 0.0000f, -0.2920f, 4.0292f, 2.3605f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-0.4225f, 0.2595f, 0.0643f, 0.0000f, 0.1725f, 0.1727f, 0.4364f, 0.0000f, 0.2042f, 0.3909f, -0.2355f, 0.0000f, 0.0177f, 4.3437f, 1.6663f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-0.1831f, 0.0414f, 0.4634f, 0.0000f, 0.2369f, 0.4370f, 0.0545f, 0.0000f, -0.4004f, 0.2395f, -0.1797f, 0.0000f, -0.1664f, 4.2447f, 2.0549f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-0.1233f, 0.3904f, -0.2870f, 0.0000f, -0.2682f, 0.1917f, 0.3760f, 0.0000f, 0.4036f, 0.2466f, 0.1621f, 0.0000f, 1.1190f, 4.1879f, 2.0369f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-0.2770f, 0.1475f, 0.3893f, 0.0000f, 0.4065f, -0.0045f, 0.2910f, 0.0000f, 0.0894f, 0.4777f, -0.1174f, 0.0000f, 0.8130f, 4.3863f, 1.4488f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(0.3333f, 0.0937f, 0.3607f, 0.0000f, -0.0833f, 0.4904f, -0.0505f, 0.0000f, -0.3633f, -0.0264f, 0.3425f, 0.0000f, 0.4950f, 4.5412f, 1.2785f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-0.3715f, 0.1179f, 0.3132f, 0.0000f, 0.2801f, 0.3656f, 0.1946f, 0.0000f, -0.1831f, 0.3200f, -0.3377f, 0.0000f, 0.5425f, 3.7647f, 2.8033f, 1.0000f)
		);
		createBlockDisplay(
			Bukkit.createBlockData(Material.TUFF),
			new Matrix4f(-0.4225f, 0.2595f, 0.0643f, 0.0000f, 0.1725f, 0.1727f, 0.4364f, 0.0000f, 0.2042f, 0.3909f, -0.2355f, 0.0000f, 0.9950f, 3.9065f, 2.5248f, 1.0000f)
		);
		for (int i = 0; i < 4; i++) {
			for (BlockDisplay trans : mTentacles.get(i)) {
				mTentacleRestTransforms.get(i).add(trans.getTransformation());
			}
		}

		for (Display dis : mDisplays) {
			mStoredTransformations.add(dis.getTransformation());
		}
	}

	private void makeDamageVisual() {
		for (BlockDisplay display : mDisplays) {
			display.setInterpolationDuration(10);
			display.setInterpolationDelay(-1);
		}
		mTempDisplay = new ArrayList<>();
		mDisplays.get(0).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.5000f, 0.2500f, 1.4375f, 1.0000f));
		mDisplays.get(1).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.5625f, 0.0000f, -1.2500f, 1.0000f));
		mDisplays.get(2).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.8125f, 0.2500f, -0.8125f, 1.0000f));
		mDisplays.get(3).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.1875f, 0.3750f, 1.8125f, 1.0000f));
		mDisplays.get(4).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.8750f, 1.3125f, 1.3125f, 1.0000f));
		mDisplays.get(5).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.2500f, 0.2500f, -1.5000f, 1.0000f));
		mDisplays.get(6).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -0.6250f, 0.0000f, -1.6875f, 1.0000f));
		mDisplays.get(7).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -0.8125f, 0.2500f, -1.5625f, 1.0000f));
		mDisplays.get(8).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.5625f, 0.3125f, -1.2500f, 1.0000f));
		mDisplays.get(9).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.8750f, 0.7500f, 0.7500f, 1.0000f));
		mDisplays.get(10).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.8750f, 4.0000f, -1.0625f, 1.0000f));
		mDisplays.get(11).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.9375f, 2.0000f, 1.8750f, 1.0000f));
		mDisplays.get(12).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, -0.2500f, 2.9375f, 2.9375f, 1.0000f));
		mDisplays.get(13).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 1.2500f, 2.1875f, 3.0625f, 1.0000f));
		mDisplays.get(14).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.0625f, 3.5000f, 1.1250f, 1.0000f));
		mDisplays.get(15).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.1250f, 1.0625f, 1.5625f, 1.0000f));
		mDisplays.get(16).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.0000f, 0.0000f, -1.4375f, 1.0000f));
		mDisplays.get(17).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.0000f, 1.0625f, 2.1250f, 1.0000f));
		mDisplays.get(18).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.6250f, 1.0000f, 2.0625f, 1.0000f));
		mDisplays.get(19).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.8125f, 2.2500f, 2.0000f, 1.0000f));
		mDisplays.get(20).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.3750f, 1.0000f, 1.3125f, 1.0000f));
		mDisplays.get(21).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 2.0625f, 0.8750f, -0.4375f, 1.0000f));
		mDisplays.get(22).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.8750f, 1.0000f, -1.0000f, 1.0000f));
		mDisplays.get(23).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.5000f, 1.0000f, -1.6875f, 1.0000f));
		mDisplays.get(24).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -0.5000f, 1.0000f, -2.0625f, 1.0000f));
		mDisplays.get(25).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.0000f, 1.0000f, -2.0000f, 1.0000f));
		mDisplays.get(26).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.5625f, 1.2500f, -1.5625f, 1.0000f));
		mDisplays.get(27).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -2.0000f, 1.0000f, -1.4375f, 1.0000f));
		mDisplays.get(28).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -2.0625f, 1.3750f, 0.8125f, 1.0000f));
		mDisplays.get(29).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -2.0000f, 1.0000f, 1.0000f, 1.0000f));
		mDisplays.get(30).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.6875f, 2.0000f, -1.7500f, 1.0000f));
		mDisplays.get(31).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.6875f, 1.8750f, -1.6250f, 1.0000f));
		mDisplays.get(32).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0625f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 2.5625f, 2.4375f, 2.4375f, 1.0000f));
		mDisplays.get(33).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 2.8125f, 2.1250f, 0.7500f, 1.0000f));
		mDisplays.get(34).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 2.6875f, 3.0000f, 0.0000f, 1.0000f));
		mDisplays.get(35).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 2.1250f, 3.0000f, -1.0000f, 1.0000f));
		mDisplays.get(36).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 0.6875f, 2.0625f, -1.0625f, 1.0000f));
		mDisplays.get(37).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 0.0000f, 3.0000f, -0.8750f, 1.0000f));
		mDisplays.get(38).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, -0.7500f, 3.0000f, -0.3750f, 1.0000f));
		mDisplays.get(39).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, -1.0625f, 3.0000f, 1.1875f, 1.0000f));
		mDisplays.get(40).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, -0.8125f, 3.0000f, 2.0000f, 1.0000f));
		mDisplays.get(41).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 2.5000f, 2.7500f, -0.5000f, 1.0000f));
		mDisplays.get(42).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.3750f, 3.7500f, -0.7500f, 1.0000f));
		mDisplays.get(43).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.5000f, 2.9375f, 1.6250f, 1.0000f));
		mDisplays.get(44).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.3125f, 3.8750f, 1.4375f, 1.0000f));
		mDisplays.get(45).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.7500f, 3.8125f, 0.9375f, 1.0000f));
		mDisplays.get(46).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -0.6250f, 3.0000f, -1.6875f, 1.0000f));
		mDisplays.get(47).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.0625f, 3.5625f, -1.3750f, 1.0000f));
		mDisplays.get(48).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.0625f, 3.3750f, -1.5625f, 1.0000f));
		mDisplays.get(49).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -0.7500f, 3.3750f, -1.7500f, 1.0000f));
		mDisplays.get(50).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.5000f, 3.5000f, -1.1875f, 1.0000f));
		mDisplays.get(51).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, -0.3125f, 2.7500f, -0.6250f, 1.0000f));
		mDisplays.get(52).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.8125f, 0.2500f, 1.8750f, 1.0000f));
		mDisplays.get(53).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.5625f, 1.2500f, 1.7500f, 1.0000f));
		mDisplays.get(54).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.6875f, 1.2500f, 1.5625f, 1.0000f));
		mDisplays.get(55).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.3750f, 0.3125f, 1.6875f, 1.0000f));
		mDisplays.get(56).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -2.1875f, 2.0000f, -1.3750f, 1.0000f));
		mDisplays.get(57).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -2.3125f, 2.2500f, 0.3750f, 1.0000f));
		mDisplays.get(58).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -2.1875f, 2.0000f, 1.0000f, 1.0000f));
		mDisplays.get(59).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, -0.8750f, 3.0625f, 2.8125f, 1.0000f));
		mDisplays.get(60).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.0000f, 2.0000f, -1.6875f, 1.0000f));
		mDisplays.get(61).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.3750f, 2.0000f, -2.1875f, 1.0000f));
		mDisplays.get(62).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -0.4375f, 1.2500f, -2.3125f, 1.0000f));
		mDisplays.get(63).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.7500f, 2.0000f, 1.5625f, 1.0000f));
		mDisplays.get(64).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.5625f, 2.0000f, 2.1875f, 1.0000f));
		mDisplays.get(65).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.3750f, 1.3125f, 2.3125f, 1.0000f));
		mDisplays.get(66).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.8750f, 3.7500f, 0.2500f, 1.0000f));
		mDisplays.get(67).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.6250f, 2.8750f, -0.6250f, 1.0000f));
		mDisplays.get(68).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.4375f, 4.0000f, 0.7500f, 1.0000f));
		mDisplays.get(69).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.8125f, 0.2500f, 0.8125f, 1.0000f));
		mDisplays.get(70).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 2.0000f, 3.0000f, 2.7500f, 1.0000f));
		mDisplays.get(71).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.7500f, 3.6250f, 1.3125f, 1.0000f));
		mDisplays.get(72).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.4375f, 2.0000f, -1.0000f, 1.0000f));
		mDisplays.get(73).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 2.0000f, 1.2500f, -0.3125f, 1.0000f));
		mDisplays.get(74).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.6250f, 2.0000f, 1.9375f, 1.0000f));
		mDisplays.get(75).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.7500f, 1.5625f, 1.6250f, 1.0000f));
		mDisplays.get(76).setTransformationMatrix(new Matrix4f(0.5389f, -0.2174f, -0.1496f, 0.0000f, 0.2639f, 0.4427f, 0.3072f, 0.0000f, -0.0009f, -0.3417f, 0.4932f, 0.0000f, -2.3750f, 3.6875f, 1.1316f, 1.0000f));
		mDisplays.get(77).setTransformationMatrix(new Matrix4f(0.0792f, 0.3062f, -0.3873f, 0.0000f, -0.8365f, 0.5000f, 0.2241f, 0.0000f, 0.2623f, 0.3062f, 0.2958f, 0.0000f, -2.3125f, 3.3793f, 1.5435f, 1.0000f));
		mDisplays.get(78).setTransformationMatrix(new Matrix4f(0.1246f, 0.2202f, -0.3098f, 0.0000f, -0.6786f, 0.6995f, 0.2241f, 0.0000f, 0.2661f, 0.1823f, 0.2366f, 0.0000f, -3.1250f, 3.9544f, 1.8080f, 1.0000f));
		mDisplays.get(79).setTransformationMatrix(new Matrix4f(-0.0016f, 0.1898f, -0.2324f, 0.0000f, -0.9374f, 0.2665f, 0.2241f, 0.0000f, 0.1044f, 0.2182f, 0.1775f, 0.0000f, -3.5804f, 4.6447f, 1.9899f, 1.0000f));
		mDisplays.get(80).setTransformationMatrix(new Matrix4f(-0.1353f, 0.1330f, -0.2324f, 0.0000f, -0.8513f, -0.4744f, 0.2241f, 0.0000f, -0.0804f, 0.2281f, 0.1775f, 0.0000f, -4.2644f, 4.9509f, 2.1971f, 1.0000f));
		mDisplays.get(81).setTransformationMatrix(new Matrix4f(-0.1408f, 0.0365f, -0.1781f, 0.0000f, -0.5000f, -0.8365f, 0.2241f, 0.0000f, -0.1408f, 0.1206f, 0.1360f, 0.0000f, -5.0079f, 4.7293f, 2.4193f, 1.0000f));
		mDisplays.get(82).setTransformationMatrix(new Matrix4f(-0.1232f, -0.0286f, -0.1549f, 0.0000f, -0.0560f, -0.9729f, 0.2241f, 0.0000f, -0.1571f, 0.0363f, 0.1183f, 0.0000f, -5.4999f, 4.0000f, 2.6418f, 1.0000f));
		mDisplays.get(83).setTransformationMatrix(new Matrix4f(-0.0698f, -0.0380f, -0.1272f, 0.0000f, 0.3202f, -0.9415f, 0.1052f, 0.0000f, -0.1238f, -0.0334f, 0.0779f, 0.0000f, -5.6070f, 3.0990f, 2.8488f, 1.0000f));
		mDisplays.get(84).setTransformationMatrix(new Matrix4f(0.1542f, -0.2174f, 0.5376f, 0.0000f, -0.3050f, 0.4427f, 0.2665f, 0.0000f, -0.4932f, -0.3417f, 0.0033f, 0.0000f, 0.0674f, 2.4139f, -1.9125f, 1.0000f));
		mDisplays.get(85).setTransformationMatrix(new Matrix4f(0.3879f, 0.3062f, 0.0759f, 0.0000f, -0.2313f, 0.5000f, -0.8346f, 0.0000f, -0.2935f, 0.3062f, 0.2648f, 0.0000f, -0.3439f, 2.1057f, -1.8465f, 1.0000f));
		mDisplays.get(86).setTransformationMatrix(new Matrix4f(0.3109f, 0.2202f, 0.1220f, 0.0000f, -0.2299f, 0.6995f, -0.6767f, 0.0000f, -0.2343f, 0.1823f, 0.2681f, 0.0000f, -0.6154f, 2.6807f, -2.6567f, 1.0000f));
		mDisplays.get(87).setTransformationMatrix(new Matrix4f(0.2323f, 0.1898f, -0.0036f, 0.0000f, -0.2321f, 0.2665f, -0.9355f, 0.0000f, -0.1766f, 0.2182f, 0.1060f, 0.0000f, -0.8011f, 3.3711f, -3.1106f, 1.0000f));
		mDisplays.get(88).setTransformationMatrix(new Matrix4f(0.2312f, 0.1330f, -0.1373f, 0.0000f, -0.2314f, -0.4744f, -0.8493f, 0.0000f, -0.1781f, 0.2281f, -0.0789f, 0.0000f, -1.0142f, 3.6773f, -3.7928f, 1.0000f));
		mDisplays.get(89).setTransformationMatrix(new Matrix4f(0.1769f, 0.0365f, -0.1424f, 0.0000f, -0.2284f, -0.8365f, -0.4981f, 0.0000f, -0.1372f, 0.1206f, -0.1397f, 0.0000f, -1.2427f, 3.4556f, -4.5344f, 1.0000f));
		mDisplays.get(90).setTransformationMatrix(new Matrix4f(0.1538f, -0.0286f, -0.1246f, 0.0000f, -0.2246f, -0.9729f, -0.0541f, 0.0000f, -0.1196f, 0.0363f, -0.1561f, 0.0000f, -1.4694f, 2.7264f, -5.0245f, 1.0000f));
		mDisplays.get(91).setTransformationMatrix(new Matrix4f(0.1266f, -0.0380f, -0.0709f, 0.0000f, -0.1025f, -0.9415f, 0.3211f, 0.0000f, -0.0789f, -0.0334f, -0.1231f, 0.0000f, -1.6773f, 1.8253f, -5.1298f, 1.0000f));
		mDisplays.get(92).setTransformationMatrix(new Matrix4f(-0.5389f, -0.2174f, 0.1496f, 0.0000f, -0.2639f, 0.4427f, -0.3072f, 0.0000f, 0.0009f, -0.3417f, -0.4932f, 0.0000f, 2.7902f, 2.5693f, -0.0465f, 1.0000f));
		mDisplays.get(93).setTransformationMatrix(new Matrix4f(-0.0792f, 0.3062f, 0.3873f, 0.0000f, 0.8365f, 0.5000f, -0.2241f, 0.0000f, -0.2623f, 0.3062f, -0.2958f, 0.0000f, 2.7277f, 2.2611f, -0.4584f, 1.0000f));
		mDisplays.get(94).setTransformationMatrix(new Matrix4f(-0.1246f, 0.2202f, 0.3098f, 0.0000f, 0.6786f, 0.6995f, -0.2241f, 0.0000f, -0.2661f, 0.1823f, -0.2366f, 0.0000f, 3.5402f, 2.8362f, -0.7229f, 1.0000f));
		mDisplays.get(95).setTransformationMatrix(new Matrix4f(0.0016f, 0.1898f, 0.2324f, 0.0000f, 0.9374f, 0.2665f, -0.2241f, 0.0000f, -0.1044f, 0.2182f, -0.1775f, 0.0000f, 3.9956f, 3.5265f, -0.9048f, 1.0000f));
		mDisplays.get(96).setTransformationMatrix(new Matrix4f(0.1353f, 0.1330f, 0.2324f, 0.0000f, 0.8513f, -0.4744f, -0.2241f, 0.0000f, 0.0804f, 0.2281f, -0.1775f, 0.0000f, 4.6796f, 3.8327f, -1.1121f, 1.0000f));
		mDisplays.get(97).setTransformationMatrix(new Matrix4f(0.1408f, 0.0365f, 0.1781f, 0.0000f, 0.5000f, -0.8365f, -0.2241f, 0.0000f, 0.1408f, 0.1206f, -0.1360f, 0.0000f, 5.4231f, 3.6111f, -1.3342f, 1.0000f));
		mDisplays.get(98).setTransformationMatrix(new Matrix4f(0.1232f, -0.0286f, 0.1549f, 0.0000f, 0.0560f, -0.9729f, -0.2241f, 0.0000f, 0.1571f, 0.0363f, -0.1183f, 0.0000f, 5.9151f, 2.8818f, -1.5568f, 1.0000f));
		mDisplays.get(99).setTransformationMatrix(new Matrix4f(0.0698f, -0.0380f, 0.1272f, 0.0000f, -0.3202f, -0.9415f, -0.1052f, 0.0000f, 0.1238f, -0.0334f, -0.0779f, 0.0000f, 6.0222f, 1.9808f, -1.7638f, 1.0000f));
		mDisplays.get(100).setTransformationMatrix(new Matrix4f(-0.1496f, -0.2174f, -0.5389f, 0.0000f, 0.3072f, 0.4427f, -0.2639f, 0.0000f, 0.4932f, -0.3417f, 0.0009f, 0.0000f, 1.0246f, 2.5850f, 3.1012f, 1.0000f));
		mDisplays.get(101).setTransformationMatrix(new Matrix4f(-0.3873f, 0.3062f, -0.0792f, 0.0000f, 0.2241f, 0.5000f, 0.8365f, 0.0000f, 0.2958f, 0.3062f, -0.2623f, 0.0000f, 1.4365f, 2.2767f, 3.0387f, 1.0000f));
		mDisplays.get(102).setTransformationMatrix(new Matrix4f(-0.3098f, 0.2202f, -0.1246f, 0.0000f, 0.2241f, 0.6995f, 0.6786f, 0.0000f, 0.2366f, 0.1823f, -0.2661f, 0.0000f, 1.7010f, 2.8518f, 3.8512f, 1.0000f));
		mDisplays.get(103).setTransformationMatrix(new Matrix4f(-0.2324f, 0.1898f, 0.0016f, 0.0000f, 0.2241f, 0.2665f, 0.9374f, 0.0000f, 0.1775f, 0.2182f, -0.1044f, 0.0000f, 1.8829f, 3.5422f, 4.3066f, 1.0000f));
		mDisplays.get(104).setTransformationMatrix(new Matrix4f(-0.2324f, 0.1330f, 0.1353f, 0.0000f, 0.2241f, -0.4744f, 0.8513f, 0.0000f, 0.1775f, 0.2281f, 0.0804f, 0.0000f, 2.0902f, 3.8484f, 4.9906f, 1.0000f));
		mDisplays.get(105).setTransformationMatrix(new Matrix4f(-0.1781f, 0.0365f, 0.1408f, 0.0000f, 0.2241f, -0.8365f, 0.5000f, 0.0000f, 0.1360f, 0.1206f, 0.1408f, 0.0000f, 2.3123f, 3.6267f, 5.7341f, 1.0000f));
		mDisplays.get(106).setTransformationMatrix(new Matrix4f(-0.1549f, -0.0286f, 0.1232f, 0.0000f, 0.2241f, -0.9729f, 0.0560f, 0.0000f, 0.1183f, 0.0363f, 0.1571f, 0.0000f, 2.5348f, 2.8975f, 6.2261f, 1.0000f));
		mDisplays.get(107).setTransformationMatrix(new Matrix4f(-0.1272f, -0.0380f, 0.0698f, 0.0000f, 0.1052f, -0.9415f, -0.3202f, 0.0000f, 0.0779f, -0.0334f, 0.1238f, 0.0000f, 2.7418f, 1.9964f, 6.3332f, 1.0000f));
		mDisplays.get(108).setTransformationMatrix(new Matrix4f(-0.1785f, 0.8703f, 0.4590f, 0.0000f, -0.6494f, 0.2462f, -0.7195f, 0.0000f, -0.7392f, -0.4265f, 0.5212f, 0.0000f, -0.7090f, 2.3407f, -1.3630f, 1.0000f));
		mDisplays.get(109).setTransformationMatrix(new Matrix4f(-0.3493f, 0.4644f, 0.4742f, 0.0000f, -0.1948f, 0.0739f, -0.2158f, 0.0000f, -0.4509f, -0.5592f, 0.2156f, 0.0000f, -1.4328f, 2.8464f, -1.9265f, 1.0000f));
		mDisplays.get(110).setTransformationMatrix(new Matrix4f(-0.4969f, 0.5788f, 0.6466f, 0.0000f, 0.6494f, -0.2462f, 0.7195f, 0.0000f, 0.5756f, 0.7774f, -0.2535f, 0.0000f, -2.6520f, 2.4513f, -2.6987f, 1.0000f));
		mDisplays.get(111).setTransformationMatrix(new Matrix4f(-0.0894f, 0.4113f, -0.2699f, 0.0000f, -0.3698f, -0.2371f, -0.2388f, 0.0000f, -0.3245f, 0.1569f, 0.3466f, 0.0000f, -1.5654f, 1.9213f, -1.3900f, 1.0000f));
		mDisplays.get(112).setTransformationMatrix(new Matrix4f(-0.0490f, -0.2778f, -0.4128f, 0.0000f, -0.4888f, 0.1044f, -0.0122f, 0.0000f, 0.0930f, 0.4024f, -0.2819f, 0.0000f, -1.0354f, 2.5070f, -1.6196f, 1.0000f));
		mDisplays.get(113).setTransformationMatrix(new Matrix4f(-0.4192f, -0.2666f, -0.0565f, 0.0000f, -0.2006f, 0.3719f, -0.2673f, 0.0000f, 0.1845f, -0.2014f, -0.4188f, 0.0000f, -1.3488f, 2.2016f, -1.5636f, 1.0000f));
		mDisplays.get(114).setTransformationMatrix(new Matrix4f(0.2007f, 0.1309f, -0.4389f, 0.0000f, -0.3572f, -0.2551f, -0.2394f, 0.0000f, -0.2866f, 0.4096f, -0.0089f, 0.0000f, -1.5473f, 3.3017f, -0.9263f, 1.0000f));
		mDisplays.get(115).setTransformationMatrix(new Matrix4f(-0.3575f, -0.2846f, -0.2029f, 0.0000f, -0.3489f, 0.2739f, 0.2307f, 0.0000f, -0.0202f, 0.3066f, -0.3945f, 0.0000f, -0.9802f, 3.2752f, -1.3219f, 1.0000f));
		mDisplays.get(116).setTransformationMatrix(new Matrix4f(-0.4262f, 0.2320f, 0.1205f, 0.0000f, -0.0563f, 0.1436f, -0.4756f, 0.0000f, -0.2553f, -0.4190f, -0.0963f, 0.0000f, -0.7988f, 3.1080f, -1.6274f, 1.0000f));
		mDisplays.get(117).setTransformationMatrix(new Matrix4f(-0.2610f, -0.3587f, -0.2308f, 0.0000f, -0.3244f, 0.3426f, -0.1655f, 0.0000f, 0.2769f, 0.0634f, -0.4115f, 0.0000f, -2.0722f, 2.4196f, -0.7137f, 1.0000f));
		mDisplays.get(118).setTransformationMatrix(new Matrix4f(-0.0490f, -0.2778f, -0.4128f, 0.0000f, -0.4888f, 0.1044f, -0.0122f, 0.0000f, 0.0930f, 0.4024f, -0.2819f, 0.0000f, -1.9220f, 2.9467f, -0.6689f, 1.0000f));
		mDisplays.get(119).setTransformationMatrix(new Matrix4f(-0.2516f, -0.9659f, -0.0606f, 0.0000f, 0.9390f, -0.2588f, 0.2263f, 0.0000f, -0.2343f, 0.0000f, 0.9722f, 0.0000f, 1.9847f, 2.9033f, 1.8562f, 1.0000f));
		mDisplays.get(120).setTransformationMatrix(new Matrix4f(-0.2441f, -0.6609f, 0.2572f, 0.0000f, 0.2817f, -0.0776f, 0.0679f, 0.0000f, -0.0830f, 0.2968f, 0.6838f, 0.0000f, 2.8395f, 2.3443f, 2.0887f, 1.0000f));
		mDisplays.get(121).setTransformationMatrix(new Matrix4f(-0.3309f, -0.8589f, 0.3908f, 0.0000f, -0.9390f, 0.2588f, -0.2263f, 0.0000f, 0.0932f, -0.4419f, -0.8922f, 0.0000f, 3.9779f, 2.4851f, 3.0494f, 1.0000f));
		mDisplays.get(122).setTransformationMatrix(new Matrix4f(0.3277f, -0.3330f, -0.1782f, 0.0000f, 0.3363f, 0.1500f, 0.3382f, 0.0000f, -0.1718f, -0.3415f, 0.3223f, 0.0000f, 2.2977f, 2.9979f, 2.7524f, 1.0000f));
		mDisplays.get(123).setTransformationMatrix(new Matrix4f(0.3650f, 0.3407f, 0.0258f, 0.0000f, 0.2143f, -0.2577f, 0.3710f, 0.0000f, 0.2661f, -0.2598f, -0.3342f, 0.0000f, 2.3655f, 2.7077f, 1.9856f, 1.0000f));
		mDisplays.get(124).setTransformationMatrix(new Matrix4f(0.1859f, 0.1129f, 0.4502f, 0.0000f, 0.3644f, -0.3360f, -0.0661f, 0.0000f, 0.2876f, 0.3527f, -0.2072f, 0.0000f, 2.4028f, 2.8628f, 2.3970f, 1.0000f));
		mDisplays.get(125).setTransformationMatrix(new Matrix4f(0.3369f, 0.0628f, -0.3641f, 0.0000f, 0.3299f, 0.1707f, 0.3347f, 0.0000f, 0.1663f, -0.4658f, 0.0736f, 0.0000f, 2.0220f, 1.6364f, 2.3152f, 1.0000f));
		mDisplays.get(126).setTransformationMatrix(new Matrix4f(0.2934f, 0.1878f, 0.3587f, 0.0000f, -0.0433f, -0.4258f, 0.2584f, 0.0000f, 0.4026f, -0.1827f, -0.2336f, 0.0000f, 2.1582f, 1.9552f, 1.7164f, 1.0000f));
		mDisplays.get(127).setTransformationMatrix(new Matrix4f(0.0829f, -0.3859f, 0.3069f, 0.0000f, 0.4725f, -0.0267f, -0.1613f, 0.0000f, 0.1409f, 0.3168f, 0.3603f, 0.0000f, 2.3474f, 2.2467f, 1.5336f, 1.0000f));
		mDisplays.get(128).setTransformationMatrix(new Matrix4f(0.2728f, 0.2949f, 0.2978f, 0.0000f, 0.3167f, -0.3777f, 0.0840f, 0.0000f, 0.2745f, 0.1428f, -0.3928f, 0.0000f, 1.9345f, 2.2008f, 3.1942f, 1.0000f));
		mDisplays.get(129).setTransformationMatrix(new Matrix4f(0.3650f, 0.3407f, 0.0258f, 0.0000f, 0.2143f, -0.2577f, 0.3710f, 0.0000f, 0.2661f, -0.2598f, -0.3342f, 0.0000f, 1.8939f, 1.7638f, 2.8630f, 1.0000f));
		mDisplays.get(130).setTransformationMatrix(new Matrix4f(0.7594f, 0.0067f, 0.6505f, 0.0000f, -0.5585f, -0.5061f, 0.6572f, 0.0000f, 0.3336f, -0.8624f, -0.3807f, 0.0000f, -1.8220f, 2.6175f, 2.0029f, 1.0000f));
		mDisplays.get(131).setTransformationMatrix(new Matrix4f(0.6221f, -0.2605f, 0.3281f, 0.0000f, -0.1676f, -0.1518f, 0.1972f, 0.0000f, -0.0051f, -0.5921f, -0.4603f, 0.0000f, -2.1359f, 2.0913f, 2.8525f, 1.0000f));
		mDisplays.get(132).setTransformationMatrix(new Matrix4f(0.8279f, -0.3887f, 0.4043f, 0.0000f, 0.5585f, 0.5061f, -0.6572f, 0.0000f, 0.0508f, 0.7699f, 0.6361f, 0.0000f, -2.9765f, 0.8800f, 3.1073f, 1.0000f));
		mDisplays.get(133).setTransformationMatrix(new Matrix4f(-0.0910f, 0.0383f, 0.4901f, 0.0000f, -0.3039f, -0.3962f, -0.0254f, 0.0000f, 0.3865f, -0.3026f, 0.0954f, 0.0000f, -1.9900f, 1.6889f, 1.8635f, 1.0000f));
		mDisplays.get(134).setTransformationMatrix(new Matrix4f(-0.4865f, -0.0825f, -0.0807f, 0.0000f, 0.0380f, -0.4447f, 0.2253f, 0.0000f, -0.1090f, 0.2131f, 0.4390f, 0.0000f, -1.9894f, 2.3651f, 2.3319f, 1.0000f));
		mDisplays.get(135).setTransformationMatrix(new Matrix4f(-0.1457f, -0.4663f, -0.1067f, 0.0000f, -0.1014f, -0.0789f, 0.4832f, 0.0000f, -0.4674f, 0.1624f, -0.0715f, 0.0000f, -2.0479f, 1.9820f, 2.1211f, 1.0000f));
		mDisplays.get(136).setTransformationMatrix(new Matrix4f(-0.3597f, 0.2624f, 0.2276f, 0.0000f, -0.3116f, -0.3885f, -0.0445f, 0.0000f, 0.1535f, -0.1739f, 0.4429f, 0.0000f, -1.0356f, 2.0119f, 2.9150f, 1.0000f));
		mDisplays.get(137).setTransformationMatrix(new Matrix4f(-0.2890f, -0.3992f, -0.0843f, 0.0000f, 0.3244f, -0.2875f, 0.2491f, 0.0000f, -0.2474f, 0.0893f, 0.4252f, 0.0000f, -1.4225f, 2.5851f, 2.8905f, 1.0000f));
		mDisplays.get(138).setTransformationMatrix(new Matrix4f(0.2081f, -0.3634f, 0.2731f, 0.0000f, -0.3838f, 0.0206f, 0.3198f, 0.0000f, -0.2437f, -0.3428f, -0.2704f, 0.0000f, -1.7721f, 2.7436f, 2.8081f, 1.0000f));
		mDisplays.get(139).setTransformationMatrix(new Matrix4f(-0.3450f, -0.3221f, -0.1651f, 0.0000f, -0.0161f, -0.2141f, 0.4515f, 0.0000f, -0.3616f, 0.3168f, 0.1374f, 0.0000f, -1.1652f, 1.2682f, 2.1877f, 1.0000f));
		mDisplays.get(140).setTransformationMatrix(new Matrix4f(-0.4865f, -0.0825f, -0.0807f, 0.0000f, 0.0380f, -0.4447f, 0.2253f, 0.0000f, -0.1090f, 0.2131f, 0.4390f, 0.0000f, -0.9258f, 1.5407f, 2.6010f, 1.0000f));
		mDisplays.get(141).setTransformationMatrix(new Matrix4f(0.8053f, -0.4357f, -0.4021f, 0.0000f, 0.2161f, 0.8473f, -0.4851f, 0.0000f, 0.5521f, 0.3038f, 0.7765f, 0.0000f, 1.0424f, 3.7175f, -1.0697f, 1.0000f));
		mDisplays.get(142).setTransformationMatrix(new Matrix4f(0.7206f, -0.2047f, -0.0365f, 0.0000f, 0.0648f, 0.2542f, -0.1455f, 0.0000f, 0.1303f, 0.3417f, 0.6548f, 0.0000f, 1.5229f, 4.4372f, -1.6599f, 1.0000f));
		mDisplays.get(143).setTransformationMatrix(new Matrix4f(0.9686f, -0.2484f, -0.0023f, 0.0000f, -0.2161f, -0.8473f, 0.4851f, 0.0000f, -0.1225f, -0.4694f, -0.8744f, 0.0000f, 1.7744f, 5.9085f, -1.5549f, 1.0000f));
		mDisplays.get(144).setTransformationMatrix(new Matrix4f(0.1397f, 0.1147f, -0.4662f, 0.0000f, 0.0131f, 0.4844f, 0.1231f, 0.0000f, 0.4799f, -0.0466f, 0.1323f, 0.0000f, 1.4041f, 4.4578f, -0.5890f, 1.0000f));
		mDisplays.get(145).setTransformationMatrix(new Matrix4f(-0.3173f, 0.3864f, -0.0034f, 0.0000f, 0.3864f, 0.3173f, 0.0009f, 0.0000f, 0.0029f, -0.0020f, -0.5000f, 0.0000f, 1.2257f, 4.0622f, -1.2878f, 1.0000f));
		mDisplays.get(146).setTransformationMatrix(new Matrix4f(0.1235f, 0.4090f, 0.2598f, 0.0000f, 0.1976f, 0.2022f, -0.4124f, 0.0000f, -0.4424f, 0.2046f, -0.1117f, 0.0000f, 1.3125f, 4.3358f, -0.9528f, 1.0000f));
		mDisplays.get(147).setTransformationMatrix(new Matrix4f(-0.2913f, 0.1067f, -0.3922f, 0.0000f, -0.0052f, 0.4815f, 0.1348f, 0.0000f, 0.4064f, 0.0826f, -0.2793f, 0.0000f, 2.3403f, 3.7290f, -1.4336f, 1.0000f));
		mDisplays.get(148).setTransformationMatrix(new Matrix4f(-0.0019f, 0.4667f, 0.1793f, 0.0000f, 0.4994f, 0.0108f, -0.0226f, 0.0000f, -0.0250f, 0.1790f, -0.4662f, 0.0000f, 1.7354f, 3.5994f, -1.7437f, 1.0000f));
		mDisplays.get(149).setTransformationMatrix(new Matrix4f(0.4758f, 0.1490f, -0.0374f, 0.0000f, -0.1253f, 0.3059f, -0.3751f, 0.0000f, -0.0889f, 0.3664f, 0.3284f, 0.0000f, 1.3698f, 3.7221f, -1.8177f, 1.0000f));
		mDisplays.get(150).setTransformationMatrix(new Matrix4f(-0.1212f, 0.4398f, 0.2046f, 0.0000f, 0.3192f, 0.2311f, -0.3078f, 0.0000f, -0.3653f, 0.0560f, -0.3368f, 0.0000f, 2.3528f, 4.2211f, -0.5081f, 1.0000f));
		mDisplays.get(151).setTransformationMatrix(new Matrix4f(-0.3173f, 0.3864f, -0.0034f, 0.0000f, 0.3864f, 0.3173f, 0.0009f, 0.0000f, 0.0029f, -0.0020f, -0.5000f, 0.0000f, 2.5446f, 3.9291f, -0.9328f, 1.0000f));
		mDisplays.get(152).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.7433f, 0.6690f, 0.0000f, 0.0000f, -0.6690f, 0.7433f, 0.0000f, 1.2450f, 3.6794f, 1.5576f, 1.0000f));
		mDisplays.get(153).setTransformationMatrix(new Matrix4f(0.6842f, -0.2056f, 0.2284f, 0.0000f, 0.0000f, 0.2230f, 0.2007f, 0.0000f, -0.3073f, -0.4577f, 0.5085f, 0.0000f, 1.5557f, 4.4055f, 2.2457f, 1.0000f));
		mDisplays.get(154).setTransformationMatrix(new Matrix4f(0.8892f, -0.3061f, 0.3401f, 0.0000f, -0.0000f, -0.7433f, -0.6690f, 0.0000f, 0.4575f, 0.5949f, -0.6609f, 0.0000f, 1.0750f, 4.8882f, 3.5779f, 1.0000f));
		mDisplays.get(155).setTransformationMatrix(new Matrix4f(0.2500f, 0.4300f, 0.0507f, 0.0000f, -0.2500f, 0.0955f, 0.4223f, 0.0000f, 0.3536f, -0.2365f, 0.2628f, 0.0000f, 1.0205f, 3.4966f, 2.4666f, 1.0000f));
		mDisplays.get(156).setTransformationMatrix(new Matrix4f(-0.4225f, 0.2340f, 0.1293f, 0.0000f, 0.1725f, 0.0539f, 0.4662f, 0.0000f, 0.2042f, 0.4386f, -0.1263f, 0.0000f, 1.3302f, 3.9801f, 1.8775f, 1.0000f));
		mDisplays.get(157).setTransformationMatrix(new Matrix4f(-0.1831f, -0.0799f, 0.4583f, 0.0000f, 0.2369f, 0.4079f, 0.1658f, 0.0000f, -0.4004f, 0.2778f, -0.1116f, 0.0000f, 1.1461f, 3.7838f, 2.2271f, 1.0000f));
		mDisplays.get(158).setTransformationMatrix(new Matrix4f(-0.1233f, 0.4514f, -0.1761f, 0.0000f, -0.2682f, 0.0878f, 0.4128f, 0.0000f, 0.4036f, 0.1963f, 0.2204f, 0.0000f, 2.4315f, 3.7336f, 2.1951f, 1.0000f));
		mDisplays.get(159).setTransformationMatrix(new Matrix4f(-0.2770f, 0.0417f, 0.4142f, 0.0000f, 0.4065f, -0.0797f, 0.2799f, 0.0000f, 0.0894f, 0.4918f, 0.0103f, 0.0000f, 2.1255f, 4.0775f, 1.6783f, 1.0000f));
		mDisplays.get(160).setTransformationMatrix(new Matrix4f(0.3333f, -0.0028f, 0.3727f, 0.0000f, -0.0833f, 0.4868f, 0.0782f, 0.0000f, -0.3633f, -0.1142f, 0.3240f, 0.0000f, 1.8075f, 4.2712f, 1.5540f, 1.0000f));
		mDisplays.get(161).setTransformationMatrix(new Matrix4f(-0.3715f, 0.0329f, 0.3330f, 0.0000f, 0.2801f, 0.3028f, 0.2826f, 0.0000f, -0.1831f, 0.3965f, -0.2434f, 0.0000f, 1.8550f, 3.1265f, 2.8259f, 1.0000f));
		mDisplays.get(162).setTransformationMatrix(new Matrix4f(-0.4225f, 0.2340f, 0.1293f, 0.0000f, 0.1725f, 0.0539f, 0.4662f, 0.0000f, 0.2042f, 0.4386f, -0.1263f, 0.0000f, 2.3075f, 3.3356f, 2.5936f, 1.0000f));
		//Special
		mTempDisplay.add(createBlockDisplay(
			Bukkit.createBlockData(Material.AMETHYST_BLOCK),
			new Matrix4f(1.2500f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.2500f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.2500f, 0.0000f, -1.1875f, 1.7500f, -1.6250f, 1.0000f),
			2));
		mTempDisplay.add(createBlockDisplay(
			Bukkit.createBlockData(Material.AMETHYST_BLOCK),
			new Matrix4f(1.2500f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.2500f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.2500f, 0.0000f, 1.1250f, 1.8750f, -1.1250f, 1.0000f),
			2));
		mTempDisplay.add(createBlockDisplay(
			Bukkit.createBlockData(Material.AMETHYST_BLOCK),
			new Matrix4f(1.2500f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.2500f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.2500f, 0.0000f, 0.6875f, 1.8750f, 1.2500f, 1.0000f),
			2));
		mTempDisplay.add(createBlockDisplay(
			Bukkit.createBlockData(Material.AMETHYST_BLOCK),
			new Matrix4f(1.2500f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.2500f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.2500f, 0.0000f, -1.7500f, 2.7500f, 0.9375f, 1.0000f),
			2));
	}


	private void victoryVisual() {
		int intduration = 20;
		mBoss.setVisibleByDefault(false);
		mBoss.setInvulnerable(true);
		mBoss.teleport(mBoss.getLocation().add(3, -4, 0));
		for (Display display : mDisplays) {
			display.teleport(mBoss.getLocation().subtract(0, 2, 0));
		}
		//mBox.shift(new Location(mBoss.getWorld(), 3, -4, 0));
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mDisplays.get(0).setInterpolationDelay(-1);
			mDisplays.get(0).setInterpolationDuration(intduration);
			mDisplays.get(0).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.3750f, 0.2500f, 0.8125f, 1.0000f));
			mDisplays.get(1).setInterpolationDelay(-1);
			mDisplays.get(1).setInterpolationDuration(intduration);
			mDisplays.get(1).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.6250f, 0.0000f, 0.0000f, 1.0000f));
			mDisplays.get(2).setInterpolationDelay(-1);
			mDisplays.get(2).setInterpolationDuration(intduration);
			mDisplays.get(2).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.8125f, 0.2500f, -0.8125f, 1.0000f));
			mDisplays.get(3).setInterpolationDelay(-1);
			mDisplays.get(3).setInterpolationDuration(intduration);
			mDisplays.get(3).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 1.5000f, 1.0000f));
			mDisplays.get(4).setInterpolationDelay(-1);
			mDisplays.get(4).setInterpolationDuration(intduration);
			mDisplays.get(4).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -0.7500f, 0.2500f, 1.3125f, 1.0000f));
			mDisplays.get(5).setInterpolationDelay(-1);
			mDisplays.get(5).setInterpolationDuration(intduration);
			mDisplays.get(5).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.8125f, 0.2500f, -1.5000f, 1.0000f));
			mDisplays.get(6).setInterpolationDelay(-1);
			mDisplays.get(6).setInterpolationDuration(intduration);
			mDisplays.get(6).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, -1.6875f, 1.0000f));
			mDisplays.get(7).setInterpolationDelay(-1);
			mDisplays.get(7).setInterpolationDuration(intduration);
			mDisplays.get(7).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -0.8125f, 0.2500f, -1.5625f, 1.0000f));
			mDisplays.get(8).setInterpolationDelay(-1);
			mDisplays.get(8).setInterpolationDuration(intduration);
			mDisplays.get(8).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.5000f, 0.3125f, -0.8750f, 1.0000f));
			mDisplays.get(9).setInterpolationDelay(-1);
			mDisplays.get(9).setInterpolationDuration(intduration);
			mDisplays.get(9).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.8750f, 0.1875f, 0.0000f, 1.0000f));
			mDisplays.get(10).setInterpolationDelay(-1);
			mDisplays.get(10).setInterpolationDuration(intduration);
			mDisplays.get(10).setTransformationMatrix(new Matrix4f(3.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.0000f, 4.0000f, 0.0000f, 1.0000f));
			mDisplays.get(11).setInterpolationDelay(-1);
			mDisplays.get(11).setInterpolationDuration(intduration);
			mDisplays.get(11).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.9375f, 2.0000f, 1.8750f, 1.0000f));
			mDisplays.get(12).setInterpolationDelay(-1);
			mDisplays.get(12).setInterpolationDuration(intduration);
			mDisplays.get(12).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 0.0000f, 3.0000f, 3.0000f, 1.0000f));
			mDisplays.get(13).setInterpolationDelay(-1);
			mDisplays.get(13).setInterpolationDuration(intduration);
			mDisplays.get(13).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 1.0000f, 3.0000f, 3.0625f, 1.0000f));
			mDisplays.get(14).setInterpolationDelay(-1);
			mDisplays.get(14).setInterpolationDuration(intduration);
			mDisplays.get(14).setTransformationMatrix(new Matrix4f(2.0625f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.0000f, 4.0000f, 1.0000f, 1.0000f));
			mDisplays.get(15).setInterpolationDelay(-1);
			mDisplays.get(15).setInterpolationDuration(intduration);
			mDisplays.get(15).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -0.3750f, 3.8125f, 1.5000f, 1.0000f));
			mDisplays.get(16).setInterpolationDelay(-1);
			mDisplays.get(16).setInterpolationDuration(intduration);
			mDisplays.get(16).setTransformationMatrix(new Matrix4f(3.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 3.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 3.0000f, 0.0000f, -1.0000f, 1.0000f, -1.0000f, 1.0000f));
			mDisplays.get(17).setInterpolationDelay(-1);
			mDisplays.get(17).setInterpolationDuration(intduration);
			mDisplays.get(17).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.0000f, 1.0000f, 2.0000f, 1.0000f));
			mDisplays.get(18).setInterpolationDelay(-1);
			mDisplays.get(18).setInterpolationDuration(intduration);
			mDisplays.get(18).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 1.0000f, 2.0625f, 1.0000f));
			mDisplays.get(19).setInterpolationDelay(-1);
			mDisplays.get(19).setInterpolationDuration(intduration);
			mDisplays.get(19).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.0000f, 1.0000f, 2.0000f, 1.0000f));
			mDisplays.get(20).setInterpolationDelay(-1);
			mDisplays.get(20).setInterpolationDuration(intduration);
			mDisplays.get(20).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.7500f, 1.0000f, 1.0000f, 1.0000f));
			mDisplays.get(21).setInterpolationDelay(-1);
			mDisplays.get(21).setInterpolationDuration(intduration);
			mDisplays.get(21).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 2.0625f, 1.0000f, 0.0000f, 1.0000f));
			mDisplays.get(22).setInterpolationDelay(-1);
			mDisplays.get(22).setInterpolationDuration(intduration);
			mDisplays.get(22).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 2.0000f, 1.0000f, -1.0000f, 1.0000f));
			mDisplays.get(23).setInterpolationDelay(-1);
			mDisplays.get(23).setInterpolationDuration(intduration);
			mDisplays.get(23).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.0000f, 1.0000f, -2.0000f, 1.0000f));
			mDisplays.get(24).setInterpolationDelay(-1);
			mDisplays.get(24).setInterpolationDuration(intduration);
			mDisplays.get(24).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 1.0000f, -2.0625f, 1.0000f));
			mDisplays.get(25).setInterpolationDelay(-1);
			mDisplays.get(25).setInterpolationDuration(intduration);
			mDisplays.get(25).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.0000f, 1.0000f, -2.0000f, 1.0000f));
			mDisplays.get(26).setInterpolationDelay(-1);
			mDisplays.get(26).setInterpolationDuration(intduration);
			mDisplays.get(26).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.5625f, 1.2500f, -1.5625f, 1.0000f));
			mDisplays.get(27).setInterpolationDelay(-1);
			mDisplays.get(27).setInterpolationDuration(intduration);
			mDisplays.get(27).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -2.0000f, 1.0000f, -1.0000f, 1.0000f));
			mDisplays.get(28).setInterpolationDelay(-1);
			mDisplays.get(28).setInterpolationDuration(intduration);
			mDisplays.get(28).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -2.0625f, 1.0000f, 0.0000f, 1.0000f));
			mDisplays.get(29).setInterpolationDelay(-1);
			mDisplays.get(29).setInterpolationDuration(intduration);
			mDisplays.get(29).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -2.0000f, 1.0000f, 1.0000f, 1.0000f));
			mDisplays.get(30).setInterpolationDelay(-1);
			mDisplays.get(30).setInterpolationDuration(intduration);
			mDisplays.get(30).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.6875f, 2.0000f, -1.7500f, 1.0000f));
			mDisplays.get(31).setInterpolationDelay(-1);
			mDisplays.get(31).setInterpolationDuration(intduration);
			mDisplays.get(31).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.6250f, 2.0000f, -1.7500f, 1.0000f));
			mDisplays.get(32).setInterpolationDelay(-1);
			mDisplays.get(32).setInterpolationDuration(intduration);
			mDisplays.get(32).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0625f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 2.5625f, 3.0000f, 2.0000f, 1.0000f));
			mDisplays.get(33).setInterpolationDelay(-1);
			mDisplays.get(33).setInterpolationDuration(intduration);
			mDisplays.get(33).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 2.8125f, 3.0000f, 1.0000f, 1.0000f));
			mDisplays.get(34).setInterpolationDelay(-1);
			mDisplays.get(34).setInterpolationDuration(intduration);
			mDisplays.get(34).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 2.6250f, 3.0000f, 0.0000f, 1.0000f));
			mDisplays.get(35).setInterpolationDelay(-1);
			mDisplays.get(35).setInterpolationDuration(intduration);
			mDisplays.get(35).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 2.0000f, 3.0000f, -1.0000f, 1.0000f));
			mDisplays.get(36).setInterpolationDelay(-1);
			mDisplays.get(36).setInterpolationDuration(intduration);
			mDisplays.get(36).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 1.0000f, 3.0000f, -1.0625f, 1.0000f));
			mDisplays.get(37).setInterpolationDelay(-1);
			mDisplays.get(37).setInterpolationDuration(intduration);
			mDisplays.get(37).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 0.0000f, 3.0000f, -0.8750f, 1.0000f));
			mDisplays.get(38).setInterpolationDelay(-1);
			mDisplays.get(38).setInterpolationDuration(intduration);
			mDisplays.get(38).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, -1.0000f, 3.0000f, 0.0000f, 1.0000f));
			mDisplays.get(39).setInterpolationDelay(-1);
			mDisplays.get(39).setInterpolationDuration(intduration);
			mDisplays.get(39).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, -1.0625f, 3.0000f, 1.0000f, 1.0000f));
			mDisplays.get(40).setInterpolationDelay(-1);
			mDisplays.get(40).setInterpolationDuration(intduration);
			mDisplays.get(40).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, -0.8125f, 3.0000f, 2.0000f, 1.0000f));
			mDisplays.get(41).setInterpolationDelay(-1);
			mDisplays.get(41).setInterpolationDuration(intduration);
			mDisplays.get(41).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 2.2500f, 2.7500f, -0.5000f, 1.0000f));
			mDisplays.get(42).setInterpolationDelay(-1);
			mDisplays.get(42).setInterpolationDuration(intduration);
			mDisplays.get(42).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.3750f, 3.7500f, -0.7500f, 1.0000f));
			mDisplays.get(43).setInterpolationDelay(-1);
			mDisplays.get(43).setInterpolationDuration(intduration);
			mDisplays.get(43).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.5000f, 3.7500f, 0.5625f, 1.0000f));
			mDisplays.get(44).setInterpolationDelay(-1);
			mDisplays.get(44).setInterpolationDuration(intduration);
			mDisplays.get(44).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.3125f, 3.8750f, 1.4375f, 1.0000f));
			mDisplays.get(45).setInterpolationDelay(-1);
			mDisplays.get(45).setInterpolationDuration(intduration);
			mDisplays.get(45).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.7500f, 3.7500f, 0.9375f, 1.0000f));
			mDisplays.get(46).setInterpolationDelay(-1);
			mDisplays.get(46).setInterpolationDuration(intduration);
			mDisplays.get(46).setTransformationMatrix(new Matrix4f(3.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.0000f, 3.8125f, -1.0000f, 1.0000f));
			mDisplays.get(47).setInterpolationDelay(-1);
			mDisplays.get(47).setInterpolationDuration(intduration);
			mDisplays.get(47).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 3.5625f, -1.8750f, 1.0000f));
			mDisplays.get(48).setInterpolationDelay(-1);
			mDisplays.get(48).setInterpolationDuration(intduration);
			mDisplays.get(48).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.7500f, 3.3750f, -1.7500f, 1.0000f));
			mDisplays.get(49).setInterpolationDelay(-1);
			mDisplays.get(49).setInterpolationDuration(intduration);
			mDisplays.get(49).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -0.7500f, 3.3750f, -1.7500f, 1.0000f));
			mDisplays.get(50).setInterpolationDelay(-1);
			mDisplays.get(50).setInterpolationDuration(intduration);
			mDisplays.get(50).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.7500f, 3.7500f, -0.7500f, 1.0000f));
			mDisplays.get(51).setInterpolationDelay(-1);
			mDisplays.get(51).setInterpolationDuration(intduration);
			mDisplays.get(51).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, -0.3125f, 2.7500f, -0.6250f, 1.0000f));
			mDisplays.get(52).setInterpolationDelay(-1);
			mDisplays.get(52).setInterpolationDuration(intduration);
			mDisplays.get(52).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.8125f, 0.2500f, 1.8125f, 1.0000f));
			mDisplays.get(53).setInterpolationDelay(-1);
			mDisplays.get(53).setInterpolationDuration(intduration);
			mDisplays.get(53).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.5625f, 1.2500f, 1.7500f, 1.0000f));
			mDisplays.get(54).setInterpolationDelay(-1);
			mDisplays.get(54).setInterpolationDuration(intduration);
			mDisplays.get(54).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.6875f, 1.2500f, 1.5625f, 1.0000f));
			mDisplays.get(55).setInterpolationDelay(-1);
			mDisplays.get(55).setInterpolationDuration(intduration);
			mDisplays.get(55).setTransformationMatrix(new Matrix4f(2.9375f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 3.0000f, 0.0000f, -0.9375f, 0.0000f, -1.0000f, 1.0000f));
			mDisplays.get(56).setInterpolationDelay(-1);
			mDisplays.get(56).setInterpolationDuration(intduration);
			mDisplays.get(56).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -2.1875f, 2.0000f, -1.0000f, 1.0000f));
			mDisplays.get(57).setInterpolationDelay(-1);
			mDisplays.get(57).setInterpolationDuration(intduration);
			mDisplays.get(57).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -2.3125f, 2.0000f, 0.0000f, 1.0000f));
			mDisplays.get(58).setInterpolationDelay(-1);
			mDisplays.get(58).setInterpolationDuration(intduration);
			mDisplays.get(58).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -2.1875f, 2.0000f, 1.0000f, 1.0000f));
			mDisplays.get(59).setInterpolationDelay(-1);
			mDisplays.get(59).setInterpolationDuration(intduration);
			mDisplays.get(59).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, -0.8750f, 3.0000f, 2.8125f, 1.0000f));
			mDisplays.get(60).setInterpolationDelay(-1);
			mDisplays.get(60).setInterpolationDuration(intduration);
			mDisplays.get(60).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.0000f, 2.0000f, -1.6875f, 1.0000f));
			mDisplays.get(61).setInterpolationDelay(-1);
			mDisplays.get(61).setInterpolationDuration(intduration);
			mDisplays.get(61).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.0000f, 2.0000f, -2.1875f, 1.0000f));
			mDisplays.get(62).setInterpolationDelay(-1);
			mDisplays.get(62).setInterpolationDuration(intduration);
			mDisplays.get(62).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 2.0000f, -2.3125f, 1.0000f));
			mDisplays.get(63).setInterpolationDelay(-1);
			mDisplays.get(63).setInterpolationDuration(intduration);
			mDisplays.get(63).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.7500f, 2.0000f, 1.5625f, 1.0000f));
			mDisplays.get(64).setInterpolationDelay(-1);
			mDisplays.get(64).setInterpolationDuration(intduration);
			mDisplays.get(64).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.0000f, 2.0000f, 2.1875f, 1.0000f));
			mDisplays.get(65).setInterpolationDelay(-1);
			mDisplays.get(65).setInterpolationDuration(intduration);
			mDisplays.get(65).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 2.0000f, 2.3125f, 1.0000f));
			mDisplays.get(66).setInterpolationDelay(-1);
			mDisplays.get(66).setInterpolationDuration(intduration);
			mDisplays.get(66).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.8750f, 3.8750f, 0.0000f, 1.0000f));
			mDisplays.get(67).setInterpolationDelay(-1);
			mDisplays.get(67).setInterpolationDuration(intduration);
			mDisplays.get(67).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.6250f, 3.6875f, 0.0000f, 1.0000f));
			mDisplays.get(68).setInterpolationDelay(-1);
			mDisplays.get(68).setInterpolationDuration(intduration);
			mDisplays.get(68).setTransformationMatrix(new Matrix4f(2.0625f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 2.0000f, 0.0000f, -0.4375f, 4.1875f, -0.4375f, 1.0000f));
			mDisplays.get(69).setInterpolationDelay(-1);
			mDisplays.get(69).setInterpolationDuration(intduration);
			mDisplays.get(69).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.8125f, 0.2500f, 0.8125f, 1.0000f));
			mDisplays.get(70).setInterpolationDelay(-1);
			mDisplays.get(70).setInterpolationDuration(intduration);
			mDisplays.get(70).setTransformationMatrix(new Matrix4f(-1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.0000f, 0.0000f, 2.0000f, 3.0000f, 2.7500f, 1.0000f));
			mDisplays.get(71).setInterpolationDelay(-1);
			mDisplays.get(71).setInterpolationDuration(intduration);
			mDisplays.get(71).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.7500f, 3.6250f, 1.3125f, 1.0000f));
			mDisplays.get(72).setInterpolationDelay(-1);
			mDisplays.get(72).setInterpolationDuration(intduration);
			mDisplays.get(72).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.4375f, 2.0000f, -1.0000f, 1.0000f));
			mDisplays.get(73).setInterpolationDelay(-1);
			mDisplays.get(73).setInterpolationDuration(intduration);
			mDisplays.get(73).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.8125f, 2.0000f, 0.0000f, 1.0000f));
			mDisplays.get(74).setInterpolationDelay(-1);
			mDisplays.get(74).setInterpolationDuration(intduration);
			mDisplays.get(74).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.6250f, 2.0000f, 0.9375f, 1.0000f));
			mDisplays.get(75).setInterpolationDelay(-1);
			mDisplays.get(75).setInterpolationDuration(intduration);
			mDisplays.get(75).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.7500f, 2.0625f, 1.6250f, 1.0000f));
			mDisplays.get(76).setInterpolationDelay(-1);
			mDisplays.get(76).setInterpolationDuration(intduration);
			mDisplays.get(76).setTransformationMatrix(new Matrix4f(0.5389f, -0.2174f, -0.1496f, 0.0000f, 0.2639f, 0.4427f, 0.3072f, 0.0000f, -0.0009f, -0.3417f, 0.4932f, 0.0000f, -2.3750f, 3.6875f, 1.1316f, 1.0000f));
			mDisplays.get(77).setInterpolationDelay(-1);
			mDisplays.get(77).setInterpolationDuration(intduration);
			mDisplays.get(77).setTransformationMatrix(new Matrix4f(0.0792f, 0.3062f, -0.3873f, 0.0000f, -0.8365f, 0.5000f, 0.2241f, 0.0000f, 0.2623f, 0.3062f, 0.2958f, 0.0000f, -2.3125f, 3.3793f, 1.5435f, 1.0000f));
			mDisplays.get(78).setInterpolationDelay(-1);
			mDisplays.get(78).setInterpolationDuration(intduration);
			mDisplays.get(78).setTransformationMatrix(new Matrix4f(0.1246f, 0.2202f, -0.3098f, 0.0000f, -0.6786f, 0.6995f, 0.2241f, 0.0000f, 0.2661f, 0.1823f, 0.2366f, 0.0000f, -3.1250f, 3.9544f, 1.8080f, 1.0000f));
			mDisplays.get(79).setInterpolationDelay(-1);
			mDisplays.get(79).setInterpolationDuration(intduration);
			mDisplays.get(79).setTransformationMatrix(new Matrix4f(-0.0016f, 0.1898f, -0.2324f, 0.0000f, -0.9374f, 0.2665f, 0.2241f, 0.0000f, 0.1044f, 0.2182f, 0.1775f, 0.0000f, -3.5804f, 4.6447f, 1.9899f, 1.0000f));
			mDisplays.get(80).setInterpolationDelay(-1);
			mDisplays.get(80).setInterpolationDuration(intduration);
			mDisplays.get(80).setTransformationMatrix(new Matrix4f(-0.1353f, 0.1330f, -0.2324f, 0.0000f, -0.8513f, -0.4744f, 0.2241f, 0.0000f, -0.0804f, 0.2281f, 0.1775f, 0.0000f, -4.2644f, 4.9509f, 2.1971f, 1.0000f));
			mDisplays.get(81).setInterpolationDelay(-1);
			mDisplays.get(81).setInterpolationDuration(intduration);
			mDisplays.get(81).setTransformationMatrix(new Matrix4f(-0.1408f, 0.0365f, -0.1781f, 0.0000f, -0.5000f, -0.8365f, 0.2241f, 0.0000f, -0.1408f, 0.1206f, 0.1360f, 0.0000f, -5.0079f, 4.7293f, 2.4193f, 1.0000f));
			mDisplays.get(82).setInterpolationDelay(-1);
			mDisplays.get(82).setInterpolationDuration(intduration);
			mDisplays.get(82).setTransformationMatrix(new Matrix4f(-0.1232f, -0.0286f, -0.1549f, 0.0000f, -0.0560f, -0.9729f, 0.2241f, 0.0000f, -0.1571f, 0.0363f, 0.1183f, 0.0000f, -5.4999f, 4.0000f, 2.6418f, 1.0000f));
			mDisplays.get(83).setInterpolationDelay(-1);
			mDisplays.get(83).setInterpolationDuration(intduration);
			mDisplays.get(83).setTransformationMatrix(new Matrix4f(-0.0698f, -0.0380f, -0.1272f, 0.0000f, 0.3202f, -0.9415f, 0.1052f, 0.0000f, -0.1238f, -0.0334f, 0.0779f, 0.0000f, -5.6070f, 3.0990f, 2.8488f, 1.0000f));
			mDisplays.get(84).setInterpolationDelay(-1);
			mDisplays.get(84).setInterpolationDuration(intduration);
			mDisplays.get(84).setTransformationMatrix(new Matrix4f(0.1542f, -0.2174f, 0.5376f, 0.0000f, -0.3050f, 0.4427f, 0.2665f, 0.0000f, -0.4932f, -0.3417f, 0.0033f, 0.0000f, 0.0674f, 2.4139f, -1.9125f, 1.0000f));
			mDisplays.get(85).setInterpolationDelay(-1);
			mDisplays.get(85).setInterpolationDuration(intduration);
			mDisplays.get(85).setTransformationMatrix(new Matrix4f(0.3879f, 0.3062f, 0.0759f, 0.0000f, -0.2313f, 0.5000f, -0.8346f, 0.0000f, -0.2935f, 0.3062f, 0.2648f, 0.0000f, -0.3439f, 2.1057f, -1.8465f, 1.0000f));
			mDisplays.get(86).setInterpolationDelay(-1);
			mDisplays.get(86).setInterpolationDuration(intduration);
			mDisplays.get(86).setTransformationMatrix(new Matrix4f(0.3318f, 0.2202f, 0.0374f, 0.0000f, -0.3972f, 0.6995f, -0.5941f, 0.0000f, -0.1570f, 0.1823f, 0.3196f, 0.0000f, -0.6154f, 2.6807f, -2.6567f, 1.0000f));
			mDisplays.get(87).setInterpolationDelay(-1);
			mDisplays.get(87).setInterpolationDuration(intduration);
			mDisplays.get(87).setTransformationMatrix(new Matrix4f(0.1994f, 0.1898f, -0.1193f, 0.0000f, -0.6688f, 0.2665f, -0.6941f, 0.0000f, -0.0999f, 0.2182f, 0.1800f, 0.0000f, -0.8604f, 3.3711f, -3.0148f, 1.0000f));
			mDisplays.get(88).setInterpolationDelay(-1);
			mDisplays.get(88).setInterpolationDuration(intduration);
			mDisplays.get(88).setTransformationMatrix(new Matrix4f(0.0664f, 0.1330f, -0.2606f, 0.0000f, -0.7642f, -0.4744f, -0.4370f, 0.0000f, -0.1818f, 0.2281f, 0.0702f, 0.0000f, -1.3322f, 3.6875f, -3.5108f, 1.0000f));
			mDisplays.get(89).setInterpolationDelay(-1);
			mDisplays.get(89).setInterpolationDuration(intduration);
			mDisplays.get(89).setTransformationMatrix(new Matrix4f(0.0330f, 0.0289f, -0.2258f, 0.0000f, -0.7127f, -0.6751f, -0.1907f, 0.0000f, -0.1579f, 0.1672f, -0.0017f, 0.0000f, -2.0442f, 3.3750f, -3.9423f, 1.0000f));
			mDisplays.get(90).setInterpolationDelay(-1);
			mDisplays.get(90).setInterpolationDuration(intduration);
			mDisplays.get(90).setTransformationMatrix(new Matrix4f(0.0611f, -0.0460f, -0.1848f, 0.0000f, -0.4659f, -0.8824f, 0.0654f, 0.0000f, -0.1661f, 0.0821f, -0.0754f, 0.0000f, -2.7550f, 2.8125f, -4.1060f, 1.0000f));
			mDisplays.get(91).setInterpolationDelay(-1);
			mDisplays.get(91).setInterpolationDuration(intduration);
			mDisplays.get(91).setTransformationMatrix(new Matrix4f(0.0020f, -0.0380f, -0.1451f, 0.0000f, 0.2268f, -0.9415f, 0.2493f, 0.0000f, -0.1461f, -0.0334f, 0.0068f, 0.0000f, -3.1295f, 2.0000f, -4.0358f, 1.0000f));
			mDisplays.get(92).setInterpolationDelay(-1);
			mDisplays.get(92).setInterpolationDuration(intduration);
			mDisplays.get(92).setTransformationMatrix(new Matrix4f(-0.5389f, -0.2174f, 0.1496f, 0.0000f, -0.2639f, 0.4427f, -0.3072f, 0.0000f, 0.0009f, -0.3417f, -0.4932f, 0.0000f, 2.7902f, 2.5693f, -0.0465f, 1.0000f));
			mDisplays.get(93).setInterpolationDelay(-1);
			mDisplays.get(93).setInterpolationDuration(intduration);
			mDisplays.get(93).setTransformationMatrix(new Matrix4f(-0.2902f, 0.1417f, 0.3817f, 0.0000f, 0.6427f, 0.7351f, 0.2158f, 0.0000f, -0.2500f, 0.3080f, -0.3044f, 0.0000f, 2.7277f, 2.2611f, -0.4584f, 1.0000f));
			mDisplays.get(94).setInterpolationDelay(-1);
			mDisplays.get(94).setInterpolationDuration(intduration);
			mDisplays.get(94).setTransformationMatrix(new Matrix4f(-0.3109f, 0.1447f, 0.2060f, 0.0000f, 0.4949f, 0.8567f, 0.1452f, 0.0000f, -0.1555f, 0.1471f, -0.3379f, 0.0000f, 3.3155f, 3.0625f, -0.2314f, 1.0000f));
			mDisplays.get(95).setInterpolationDelay(-1);
			mDisplays.get(95).setInterpolationDuration(intduration);
			mDisplays.get(95).setTransformationMatrix(new Matrix4f(0.1063f, 0.0016f, 0.2805f, 0.0000f, -0.3273f, 0.9374f, 0.1187f, 0.0000f, -0.2628f, -0.1044f, 0.1002f, 0.0000f, 3.6280f, 4.0000f, -0.3564f, 1.0000f));
			mDisplays.get(96).setInterpolationDelay(-1);
			mDisplays.get(96).setInterpolationDuration(intduration);
			mDisplays.get(96).setTransformationMatrix(new Matrix4f(0.1201f, -0.0370f, 0.2724f, 0.0000f, -0.6364f, 0.6755f, 0.3725f, 0.0000f, -0.1978f, -0.2181f, 0.0576f, 0.0000f, 3.3155f, 4.8750f, -0.2939f, 1.0000f));
			mDisplays.get(97).setInterpolationDelay(-1);
			mDisplays.get(97).setInterpolationDuration(intduration);
			mDisplays.get(97).setTransformationMatrix(new Matrix4f(0.0365f, 0.0899f, -0.2085f, 0.0000f, -0.8365f, 0.5410f, 0.0871f, 0.0000f, 0.1206f, 0.1713f, 0.0950f, 0.0000f, 2.6280f, 5.2500f, 0.2686f, 1.0000f));
			mDisplays.get(98).setInterpolationDelay(-1);
			mDisplays.get(98).setInterpolationDuration(intduration);
			mDisplays.get(98).setTransformationMatrix(new Matrix4f(-0.0174f, -0.1116f, 0.1650f, 0.0000f, -0.9798f, 0.1977f, 0.0305f, 0.0000f, -0.0360f, -0.1612f, -0.1128f, 0.0000f, 2.0030f, 6.0000f, 0.2686f, 1.0000f));
			mDisplays.get(99).setInterpolationDelay(-1);
			mDisplays.get(99).setInterpolationDuration(intduration);
			mDisplays.get(99).setTransformationMatrix(new Matrix4f(-0.0186f, -0.0772f, 0.1272f, 0.0000f, -0.9923f, 0.0656f, -0.1052f, 0.0000f, -0.0002f, -0.1282f, -0.0779f, 0.0000f, 1.1280f, 6.1875f, 0.3311f, 1.0000f));
			mDisplays.get(100).setInterpolationDelay(-1);
			mDisplays.get(100).setInterpolationDuration(intduration);
			mDisplays.get(100).setTransformationMatrix(new Matrix4f(-0.1496f, -0.2174f, -0.5389f, 0.0000f, 0.3072f, 0.4427f, -0.2639f, 0.0000f, 0.4932f, -0.3417f, 0.0009f, 0.0000f, 1.0246f, 2.5850f, 3.1012f, 1.0000f));
			mDisplays.get(101).setInterpolationDelay(-1);
			mDisplays.get(101).setInterpolationDuration(intduration);
			mDisplays.get(101).setTransformationMatrix(new Matrix4f(-0.2958f, 0.3062f, -0.2623f, 0.0000f, -0.2241f, 0.5000f, 0.8365f, 0.0000f, 0.3873f, 0.3062f, -0.0792f, 0.0000f, 1.4365f, 2.2767f, 3.0387f, 1.0000f));
			mDisplays.get(102).setInterpolationDelay(-1);
			mDisplays.get(102).setInterpolationDuration(intduration);
			mDisplays.get(102).setTransformationMatrix(new Matrix4f(-0.1309f, 0.2202f, -0.3072f, 0.0000f, -0.3214f, 0.6995f, 0.6383f, 0.0000f, 0.3554f, 0.1823f, -0.0208f, 0.0000f, 1.1827f, 2.8518f, 3.8363f, 1.0000f));
			mDisplays.get(103).setInterpolationDelay(-1);
			mDisplays.get(103).setInterpolationDuration(intduration);
			mDisplays.get(103).setTransformationMatrix(new Matrix4f(-0.1176f, 0.1314f, -0.2427f, 0.0000f, -0.6998f, 0.4289f, 0.5713f, 0.0000f, 0.1792f, 0.2370f, 0.0415f, 0.0000f, 1.0512f, 3.5000f, 4.3363f, 1.0000f));
			mDisplays.get(104).setInterpolationDelay(-1);
			mDisplays.get(104).setInterpolationDuration(intduration);
			mDisplays.get(104).setTransformationMatrix(new Matrix4f(-0.0988f, 0.2106f, -0.1894f, 0.0000f, -0.8991f, -0.0288f, 0.4368f, 0.0000f, 0.0866f, 0.2134f, 0.1922f, 0.0000f, 0.4532f, 3.8750f, 4.7522f, 1.0000f));
			mDisplays.get(105).setInterpolationDelay(-1);
			mDisplays.get(105).setInterpolationDuration(intduration);
			mDisplays.get(105).setTransformationMatrix(new Matrix4f(-0.1395f, 0.1226f, -0.1356f, 0.0000f, -0.7863f, -0.5120f, 0.3459f, 0.0000f, -0.0270f, 0.1549f, 0.1679f, 0.0000f, -0.2968f, 4.0000f, 5.1651f, 1.0000f));
			mDisplays.get(106).setInterpolationDelay(-1);
			mDisplays.get(106).setInterpolationDuration(intduration);
			mDisplays.get(106).setTransformationMatrix(new Matrix4f(-0.1704f, 0.0669f, -0.0805f, 0.0000f, -0.5181f, -0.6483f, 0.5579f, 0.0000f, -0.0148f, 0.1368f, 0.1452f, 0.0000f, -1.0468f, 3.6250f, 5.4813f, 1.0000f));
			mDisplays.get(107).setInterpolationDelay(-1);
			mDisplays.get(107).setInterpolationDuration(intduration);
			mDisplays.get(107).setTransformationMatrix(new Matrix4f(-0.1306f, 0.0467f, -0.0571f, 0.0000f, -0.4087f, -0.8887f, 0.2078f, 0.0000f, -0.0410f, 0.0505f, 0.1352f, 0.0000f, -1.5468f, 3.1250f, 6.0516f, 1.0000f));
			mDisplays.get(108).setInterpolationDelay(-1);
			mDisplays.get(108).setInterpolationDuration(intduration);
			mDisplays.get(108).setTransformationMatrix(new Matrix4f(-0.1785f, 0.8703f, 0.4590f, 0.0000f, -0.6494f, 0.2462f, -0.7195f, 0.0000f, -0.7392f, -0.4265f, 0.5212f, 0.0000f, -0.7090f, 2.3407f, -1.3630f, 1.0000f));
			mDisplays.get(109).setInterpolationDelay(-1);
			mDisplays.get(109).setInterpolationDuration(intduration);
			mDisplays.get(109).setTransformationMatrix(new Matrix4f(-0.3493f, 0.4644f, 0.4742f, 0.0000f, -0.1948f, 0.0739f, -0.2158f, 0.0000f, -0.4509f, -0.5592f, 0.2156f, 0.0000f, -1.4328f, 2.8464f, -1.9265f, 1.0000f));
			mDisplays.get(110).setInterpolationDelay(-1);
			mDisplays.get(110).setInterpolationDuration(intduration);
			mDisplays.get(110).setTransformationMatrix(new Matrix4f(-0.4969f, 0.5788f, 0.6466f, 0.0000f, 0.6494f, -0.2462f, 0.7195f, 0.0000f, 0.5756f, 0.7774f, -0.2535f, 0.0000f, -2.6520f, 2.4513f, -2.6987f, 1.0000f));
			mDisplays.get(111).setInterpolationDelay(-1);
			mDisplays.get(111).setInterpolationDuration(intduration);
			mDisplays.get(111).setTransformationMatrix(new Matrix4f(-0.0894f, 0.4113f, -0.2699f, 0.0000f, -0.3698f, -0.2371f, -0.2388f, 0.0000f, -0.3245f, 0.1569f, 0.3466f, 0.0000f, -1.5654f, 1.9213f, -1.3900f, 1.0000f));
			mDisplays.get(112).setInterpolationDelay(-1);
			mDisplays.get(112).setInterpolationDuration(intduration);
			mDisplays.get(112).setTransformationMatrix(new Matrix4f(-0.0490f, -0.2778f, -0.4128f, 0.0000f, -0.4888f, 0.1044f, -0.0122f, 0.0000f, 0.0930f, 0.4024f, -0.2819f, 0.0000f, -1.0354f, 2.5070f, -1.6196f, 1.0000f));
			mDisplays.get(113).setInterpolationDelay(-1);
			mDisplays.get(113).setInterpolationDuration(intduration);
			mDisplays.get(113).setTransformationMatrix(new Matrix4f(-0.4192f, -0.2666f, -0.0565f, 0.0000f, -0.2006f, 0.3719f, -0.2673f, 0.0000f, 0.1845f, -0.2014f, -0.4188f, 0.0000f, -1.3488f, 2.2016f, -1.5636f, 1.0000f));
			mDisplays.get(114).setInterpolationDelay(-1);
			mDisplays.get(114).setInterpolationDuration(intduration);
			mDisplays.get(114).setTransformationMatrix(new Matrix4f(0.2007f, 0.1309f, -0.4389f, 0.0000f, -0.3572f, -0.2551f, -0.2394f, 0.0000f, -0.2866f, 0.4096f, -0.0089f, 0.0000f, -1.5473f, 3.3017f, -0.9263f, 1.0000f));
			mDisplays.get(115).setInterpolationDelay(-1);
			mDisplays.get(115).setInterpolationDuration(intduration);
			mDisplays.get(115).setTransformationMatrix(new Matrix4f(-0.3575f, -0.2846f, -0.2029f, 0.0000f, -0.3489f, 0.2739f, 0.2307f, 0.0000f, -0.0202f, 0.3066f, -0.3945f, 0.0000f, -0.9802f, 3.2752f, -1.3219f, 1.0000f));
			mDisplays.get(116).setInterpolationDelay(-1);
			mDisplays.get(116).setInterpolationDuration(intduration);
			mDisplays.get(116).setTransformationMatrix(new Matrix4f(-0.4262f, 0.2320f, 0.1205f, 0.0000f, -0.0563f, 0.1436f, -0.4756f, 0.0000f, -0.2553f, -0.4190f, -0.0963f, 0.0000f, -0.7988f, 3.1080f, -1.6274f, 1.0000f));
			mDisplays.get(117).setInterpolationDelay(-1);
			mDisplays.get(117).setInterpolationDuration(intduration);
			mDisplays.get(117).setTransformationMatrix(new Matrix4f(-0.2610f, -0.3587f, -0.2308f, 0.0000f, -0.3244f, 0.3426f, -0.1655f, 0.0000f, 0.2769f, 0.0634f, -0.4115f, 0.0000f, -2.0722f, 2.4196f, -0.7137f, 1.0000f));
			mDisplays.get(118).setInterpolationDelay(-1);
			mDisplays.get(118).setInterpolationDuration(intduration);
			mDisplays.get(118).setTransformationMatrix(new Matrix4f(-0.0490f, -0.2778f, -0.4128f, 0.0000f, -0.4888f, 0.1044f, -0.0122f, 0.0000f, 0.0930f, 0.4024f, -0.2819f, 0.0000f, -1.9220f, 2.9467f, -0.6689f, 1.0000f));
			mDisplays.get(119).setInterpolationDelay(-1);
			mDisplays.get(119).setInterpolationDuration(intduration);
			mDisplays.get(119).setTransformationMatrix(new Matrix4f(-0.2516f, -0.9659f, -0.0606f, 0.0000f, 0.9390f, -0.2588f, 0.2263f, 0.0000f, -0.2343f, 0.0000f, 0.9722f, 0.0000f, 2.1265f, 2.9033f, 1.2937f, 1.0000f));
			mDisplays.get(120).setInterpolationDelay(-1);
			mDisplays.get(120).setInterpolationDuration(intduration);
			mDisplays.get(120).setTransformationMatrix(new Matrix4f(-0.2441f, -0.6609f, 0.2572f, 0.0000f, 0.2817f, -0.0776f, 0.0679f, 0.0000f, -0.0830f, 0.2968f, 0.6838f, 0.0000f, 2.9814f, 2.3443f, 1.5262f, 1.0000f));
			mDisplays.get(121).setInterpolationDelay(-1);
			mDisplays.get(121).setInterpolationDuration(intduration);
			mDisplays.get(121).setTransformationMatrix(new Matrix4f(-0.3309f, -0.8589f, 0.3908f, 0.0000f, -0.9390f, 0.2588f, -0.2263f, 0.0000f, 0.0932f, -0.4419f, -0.8922f, 0.0000f, 4.1198f, 2.4851f, 2.4869f, 1.0000f));
			mDisplays.get(122).setInterpolationDelay(-1);
			mDisplays.get(122).setInterpolationDuration(intduration);
			mDisplays.get(122).setTransformationMatrix(new Matrix4f(0.3277f, -0.3330f, -0.1782f, 0.0000f, 0.3363f, 0.1500f, 0.3382f, 0.0000f, -0.1718f, -0.3415f, 0.3223f, 0.0000f, 2.4395f, 2.9979f, 2.1899f, 1.0000f));
			mDisplays.get(123).setInterpolationDelay(-1);
			mDisplays.get(123).setInterpolationDuration(intduration);
			mDisplays.get(123).setTransformationMatrix(new Matrix4f(0.3650f, 0.3407f, 0.0258f, 0.0000f, 0.2143f, -0.2577f, 0.3710f, 0.0000f, 0.2661f, -0.2598f, -0.3342f, 0.0000f, 2.5074f, 2.7077f, 1.4231f, 1.0000f));
			mDisplays.get(124).setInterpolationDelay(-1);
			mDisplays.get(124).setInterpolationDuration(intduration);
			mDisplays.get(124).setTransformationMatrix(new Matrix4f(0.1859f, 0.1129f, 0.4502f, 0.0000f, 0.3644f, -0.3360f, -0.0661f, 0.0000f, 0.2876f, 0.3527f, -0.2072f, 0.0000f, 2.5447f, 2.8628f, 1.8345f, 1.0000f));
			mDisplays.get(125).setInterpolationDelay(-1);
			mDisplays.get(125).setInterpolationDuration(intduration);
			mDisplays.get(125).setTransformationMatrix(new Matrix4f(0.3369f, 0.0628f, -0.3641f, 0.0000f, 0.3299f, 0.1707f, 0.3347f, 0.0000f, 0.1663f, -0.4658f, 0.0736f, 0.0000f, 2.1638f, 1.6364f, 1.7527f, 1.0000f));
			mDisplays.get(126).setInterpolationDelay(-1);
			mDisplays.get(126).setInterpolationDuration(intduration);
			mDisplays.get(126).setTransformationMatrix(new Matrix4f(0.2934f, 0.1878f, 0.3587f, 0.0000f, -0.0433f, -0.4258f, 0.2584f, 0.0000f, 0.4026f, -0.1827f, -0.2336f, 0.0000f, 2.3000f, 1.9552f, 1.1539f, 1.0000f));
			mDisplays.get(127).setInterpolationDelay(-1);
			mDisplays.get(127).setInterpolationDuration(intduration);
			mDisplays.get(127).setTransformationMatrix(new Matrix4f(0.0829f, -0.3859f, 0.3069f, 0.0000f, 0.4725f, -0.0267f, -0.1613f, 0.0000f, 0.1409f, 0.3168f, 0.3603f, 0.0000f, 2.4892f, 2.2467f, 0.9711f, 1.0000f));
			mDisplays.get(128).setInterpolationDelay(-1);
			mDisplays.get(128).setInterpolationDuration(intduration);
			mDisplays.get(128).setTransformationMatrix(new Matrix4f(0.2728f, 0.2949f, 0.2978f, 0.0000f, 0.3167f, -0.3777f, 0.0840f, 0.0000f, 0.2745f, 0.1428f, -0.3928f, 0.0000f, 2.0763f, 2.2008f, 2.6317f, 1.0000f));
			mDisplays.get(129).setInterpolationDelay(-1);
			mDisplays.get(129).setInterpolationDuration(intduration);
			mDisplays.get(129).setTransformationMatrix(new Matrix4f(0.3650f, 0.3407f, 0.0258f, 0.0000f, 0.2143f, -0.2577f, 0.3710f, 0.0000f, 0.2661f, -0.2598f, -0.3342f, 0.0000f, 2.0357f, 1.7638f, 2.3005f, 1.0000f));
			mDisplays.get(130).setInterpolationDelay(-1);
			mDisplays.get(130).setInterpolationDuration(intduration);
			mDisplays.get(130).setTransformationMatrix(new Matrix4f(0.7594f, 0.3310f, 0.5601f, 0.0000f, -0.5585f, -0.1097f, 0.8222f, 0.0000f, 0.3336f, -0.9372f, 0.1016f, 0.0000f, -1.5390f, 2.7891f, 1.9264f, 1.0000f));
			mDisplays.get(131).setInterpolationDelay(-1);
			mDisplays.get(131).setInterpolationDuration(intduration);
			mDisplays.get(131).setTransformationMatrix(new Matrix4f(0.6221f, -0.0615f, 0.4144f, 0.0000f, -0.1676f, -0.0329f, 0.2467f, 0.0000f, -0.0051f, -0.7429f, -0.1026f, 0.0000f, -1.8530f, 2.7582f, 2.9252f, 1.0000f));
			mDisplays.get(132).setInterpolationDelay(-1);
			mDisplays.get(132).setInterpolationDuration(intduration);
			mDisplays.get(132).setTransformationMatrix(new Matrix4f(0.8279f, -0.1344f, 0.5445f, 0.0000f, 0.5585f, 0.1097f, -0.8222f, 0.0000f, 0.0508f, 0.9848f, 0.1659f, 0.0000f, -2.6936f, 1.8365f, 3.7515f, 1.0000f));
			mDisplays.get(133).setInterpolationDelay(-1);
			mDisplays.get(133).setInterpolationDuration(intduration);
			mDisplays.get(133).setTransformationMatrix(new Matrix4f(-0.0910f, 0.2783f, 0.4053f, 0.0000f, -0.3039f, -0.3559f, 0.1761f, 0.0000f, 0.3865f, -0.2143f, 0.2339f, 0.0000f, -1.7071f, 1.9152f, 2.2699f, 1.0000f));
			mDisplays.get(134).setInterpolationDelay(-1);
			mDisplays.get(134).setInterpolationDuration(intduration);
			mDisplays.get(134).setTransformationMatrix(new Matrix4f(-0.4865f, -0.1118f, -0.0286f, 0.0000f, 0.0380f, -0.2725f, 0.4175f, 0.0000f, -0.1090f, 0.4041f, 0.2736f, 0.0000f, -1.7065f, 2.7350f, 2.3375f, 1.0000f));
			mDisplays.get(135).setInterpolationDelay(-1);
			mDisplays.get(135).setInterpolationDuration(intduration);
			mDisplays.get(135).setTransformationMatrix(new Matrix4f(-0.1457f, -0.4571f, 0.1407f, 0.0000f, -0.1014f, 0.1733f, 0.4579f, 0.0000f, -0.4674f, 0.1049f, -0.1431f, 0.0000f, -1.7649f, 2.2978f, 2.3465f, 1.0000f));
			mDisplays.get(136).setInterpolationDelay(-1);
			mDisplays.get(136).setInterpolationDuration(intduration);
			mDisplays.get(136).setTransformationMatrix(new Matrix4f(-0.3597f, 0.3410f, 0.0660f, 0.0000f, -0.3116f, -0.3587f, 0.1557f, 0.0000f, 0.1535f, 0.0709f, 0.4705f, 0.0000f, -0.7527f, 2.7206f, 3.0191f, 1.0000f));
			mDisplays.get(137).setInterpolationDelay(-1);
			mDisplays.get(137).setInterpolationDuration(intduration);
			mDisplays.get(137).setTransformationMatrix(new Matrix4f(-0.2890f, -0.3879f, 0.1266f, 0.0000f, 0.3244f, -0.1245f, 0.3595f, 0.0000f, -0.2474f, 0.2899f, 0.3236f, 0.0000f, -1.1396f, 3.2048f, 2.7113f, 1.0000f));
			mDisplays.get(138).setInterpolationDelay(-1);
			mDisplays.get(138).setInterpolationDuration(intduration);
			mDisplays.get(138).setTransformationMatrix(new Matrix4f(0.2081f, -0.1782f, 0.4183f, 0.0000f, -0.3838f, 0.1777f, 0.2667f, 0.0000f, -0.2437f, -0.4320f, -0.0628f, 0.0000f, -1.4892f, 3.3009f, 2.5606f, 1.0000f));
			mDisplays.get(139).setInterpolationDelay(-1);
			mDisplays.get(139).setInterpolationDuration(intduration);
			mDisplays.get(139).setTransformationMatrix(new Matrix4f(-0.3450f, -0.3615f, 0.0181f, 0.0000f, -0.0161f, 0.0403f, 0.4981f, 0.0000f, -0.3616f, 0.3431f, -0.0395f, 0.0000f, -0.8822f, 1.7129f, 2.7610f, 1.0000f));
			mDisplays.get(140).setInterpolationDelay(-1);
			mDisplays.get(140).setInterpolationDuration(intduration);
			mDisplays.get(140).setTransformationMatrix(new Matrix4f(-0.4865f, -0.1118f, -0.0286f, 0.0000f, 0.0380f, -0.2725f, 0.4175f, 0.0000f, -0.1090f, 0.4041f, 0.2736f, 0.0000f, -0.6429f, 2.1556f, 2.9827f, 1.0000f));
			mDisplays.get(141).setInterpolationDelay(-1);
			mDisplays.get(141).setInterpolationDuration(intduration);
			mDisplays.get(141).setTransformationMatrix(new Matrix4f(0.1113f, -0.4982f, -0.8599f, 0.0000f, -0.0036f, 0.8651f, -0.5017f, 0.0000f, 0.9938f, 0.0590f, 0.0945f, 0.0000f, 0.0251f, 4.2618f, -0.0764f, 1.0000f));
			mDisplays.get(142).setInterpolationDelay(-1);
			mDisplays.get(142).setInterpolationDuration(intduration);
			mDisplays.get(142).setTransformationMatrix(new Matrix4f(0.3815f, -0.3227f, -0.5593f, 0.0000f, -0.0011f, 0.2595f, -0.1505f, 0.0000f, 0.6457f, 0.1934f, 0.3289f, 0.0000f, 0.0816f, 4.9735f, -0.8428f, 1.0000f));
			mDisplays.get(143).setInterpolationDelay(-1);
			mDisplays.get(143).setInterpolationDuration(intduration);
			mDisplays.get(143).setTransformationMatrix(new Matrix4f(0.5536f, -0.4160f, -0.7214f, 0.0000f, 0.0036f, -0.8651f, 0.5017f, 0.0000f, -0.8327f, -0.2804f, -0.4774f, 0.0000f, 0.6867f, 6.3337f, -0.9935f, 1.0000f));
			mDisplays.get(144).setInterpolationDelay(-1);
			mDisplays.get(144).setInterpolationDuration(intduration);
			mDisplays.get(144).setTransformationMatrix(new Matrix4f(-0.2219f, 0.1666f, -0.4160f, 0.0000f, 0.2193f, 0.4451f, 0.0612f, 0.0000f, 0.3907f, -0.1553f, -0.2706f, 0.0000f, 0.7913f, 4.8292f, -0.0448f, 1.0000f));
			mDisplays.get(145).setInterpolationDelay(-1);
			mDisplays.get(145).setInterpolationDuration(intduration);
			mDisplays.get(145).setTransformationMatrix(new Matrix4f(-0.1080f, 0.4322f, 0.2270f, 0.0000f, 0.3264f, 0.2368f, -0.2956f, 0.0000f, -0.3630f, 0.0843f, -0.3334f, 0.0000f, 0.0694f, 4.5999f, -0.3657f, 1.0000f));
			mDisplays.get(146).setInterpolationDelay(-1);
			mDisplays.get(146).setInterpolationDuration(intduration);
			mDisplays.get(146).setTransformationMatrix(new Matrix4f(0.3704f, 0.3283f, 0.0707f, 0.0000f, -0.1239f, 0.2314f, -0.4256f, 0.0000f, -0.3122f, 0.2977f, 0.2528f, 0.0000f, 0.4373f, 4.7909f, -0.2146f, 1.0000f));
			mDisplays.get(147).setInterpolationDelay(-1);
			mDisplays.get(147).setInterpolationDuration(intduration);
			mDisplays.get(147).setTransformationMatrix(new Matrix4f(-0.4447f, 0.2242f, -0.0441f, 0.0000f, 0.2154f, 0.4436f, 0.0828f, 0.0000f, 0.0762f, 0.0546f, -0.4911f, 0.0000f, 0.5903f, 4.1002f, -1.2895f, 1.0000f));
			mDisplays.get(148).setInterpolationDelay(-1);
			mDisplays.get(148).setInterpolationDuration(intduration);
			mDisplays.get(148).setTransformationMatrix(new Matrix4f(0.2463f, 0.4210f, 0.1101f, 0.0000f, 0.3046f, -0.0764f, -0.3891f, 0.0000f, -0.3108f, 0.2587f, -0.2941f, 0.0000f, -0.0537f, 4.1385f, -1.0392f, 1.0000f));
			mDisplays.get(149).setInterpolationDelay(-1);
			mDisplays.get(149).setInterpolationDuration(intduration);
			mDisplays.get(149).setTransformationMatrix(new Matrix4f(0.3134f, 0.0642f, -0.3843f, 0.0000f, -0.2766f, 0.3840f, -0.1614f, 0.0000f, 0.2743f, 0.3138f, 0.2762f, 0.0000f, -0.3100f, 4.3365f, -0.8172f, 1.0000f));
			mDisplays.get(150).setInterpolationDelay(-1);
			mDisplays.get(150).setInterpolationDuration(intduration);
			mDisplays.get(150).setTransformationMatrix(new Matrix4f(0.1819f, 0.4122f, 0.2167f, 0.0000f, 0.0371f, 0.2191f, -0.4479f, 0.0000f, -0.4643f, 0.1790f, 0.0492f, 0.0000f, 1.3958f, 4.4137f, -0.6964f, 1.0000f));
			mDisplays.get(151).setInterpolationDelay(-1);
			mDisplays.get(151).setInterpolationDuration(intduration);
			mDisplays.get(151).setTransformationMatrix(new Matrix4f(-0.1080f, 0.4322f, 0.2270f, 0.0000f, 0.3264f, 0.2368f, -0.2956f, 0.0000f, -0.3630f, 0.0843f, -0.3334f, 0.0000f, 1.1356f, 4.1698f, -1.1150f, 1.0000f));
			mDisplays.get(152).setInterpolationDelay(-1);
			mDisplays.get(152).setInterpolationDuration(intduration);
			mDisplays.get(152).setTransformationMatrix(new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.8911f, 0.4538f, 0.0000f, 0.0000f, -0.4538f, 0.8911f, 0.0000f, -0.0675f, 3.9705f, 1.4352f, 1.0000f));
			mDisplays.get(153).setInterpolationDelay(-1);
			mDisplays.get(153).setInterpolationDuration(intduration);
			mDisplays.get(153).setTransformationMatrix(new Matrix4f(0.6842f, -0.1394f, 0.2738f, 0.0000f, 0.0000f, 0.2673f, 0.1361f, 0.0000f, -0.3073f, -0.3105f, 0.6097f, 0.0000f, 0.2432f, 4.8499f, 1.9119f, 1.0000f));
			mDisplays.get(154).setInterpolationDelay(-1);
			mDisplays.get(154).setInterpolationDuration(intduration);
			mDisplays.get(154).setTransformationMatrix(new Matrix4f(0.8892f, -0.2076f, 0.4077f, 0.0000f, -0.0000f, -0.8911f, -0.4538f, 0.0000f, 0.4575f, 0.4035f, -0.7924f, 0.0000f, -0.2375f, 5.6610f, 3.0737f, 1.0000f));
			mDisplays.get(155).setInterpolationDelay(-1);
			mDisplays.get(155).setInterpolationDuration(intduration);
			mDisplays.get(155).setTransformationMatrix(new Matrix4f(0.2500f, 0.4285f, -0.0623f, 0.0000f, -0.2500f, 0.2016f, 0.3832f, 0.0000f, 0.3536f, -0.1604f, 0.3151f, 0.0000f, -0.2920f, 4.0292f, 2.3605f, 1.0000f));
			mDisplays.get(156).setInterpolationDelay(-1);
			mDisplays.get(156).setInterpolationDuration(intduration);
			mDisplays.get(156).setTransformationMatrix(new Matrix4f(-0.4225f, 0.2595f, 0.0643f, 0.0000f, 0.1725f, 0.1727f, 0.4364f, 0.0000f, 0.2042f, 0.3909f, -0.2355f, 0.0000f, 0.0177f, 4.3437f, 1.6663f, 1.0000f));
			mDisplays.get(157).setInterpolationDelay(-1);
			mDisplays.get(157).setInterpolationDuration(intduration);
			mDisplays.get(157).setTransformationMatrix(new Matrix4f(-0.1831f, 0.0414f, 0.4634f, 0.0000f, 0.2369f, 0.4370f, 0.0545f, 0.0000f, -0.4004f, 0.2395f, -0.1797f, 0.0000f, -0.1664f, 4.2447f, 2.0549f, 1.0000f));
			mDisplays.get(158).setInterpolationDelay(-1);
			mDisplays.get(158).setInterpolationDuration(intduration);
			mDisplays.get(158).setTransformationMatrix(new Matrix4f(-0.1233f, 0.3904f, -0.2870f, 0.0000f, -0.2682f, 0.1917f, 0.3760f, 0.0000f, 0.4036f, 0.2466f, 0.1621f, 0.0000f, 1.1190f, 4.1879f, 2.0369f, 1.0000f));
			mDisplays.get(159).setInterpolationDelay(-1);
			mDisplays.get(159).setInterpolationDuration(intduration);
			mDisplays.get(159).setTransformationMatrix(new Matrix4f(-0.2770f, 0.1475f, 0.3893f, 0.0000f, 0.4065f, -0.0045f, 0.2910f, 0.0000f, 0.0894f, 0.4777f, -0.1174f, 0.0000f, 0.8130f, 4.3863f, 1.4488f, 1.0000f));
			mDisplays.get(160).setInterpolationDelay(-1);
			mDisplays.get(160).setInterpolationDuration(intduration);
			mDisplays.get(160).setTransformationMatrix(new Matrix4f(0.3333f, 0.0937f, 0.3607f, 0.0000f, -0.0833f, 0.4904f, -0.0505f, 0.0000f, -0.3633f, -0.0264f, 0.3425f, 0.0000f, 0.4950f, 4.5412f, 1.2785f, 1.0000f));
			mDisplays.get(161).setInterpolationDelay(-1);
			mDisplays.get(161).setInterpolationDuration(intduration);
			mDisplays.get(161).setTransformationMatrix(new Matrix4f(-0.3715f, 0.1179f, 0.3132f, 0.0000f, 0.2801f, 0.3656f, 0.1946f, 0.0000f, -0.1831f, 0.3200f, -0.3377f, 0.0000f, 0.5425f, 3.7647f, 2.8033f, 1.0000f));
			mDisplays.get(162).setInterpolationDelay(-1);
			mDisplays.get(162).setInterpolationDuration(intduration);
			mDisplays.get(162).setTransformationMatrix(new Matrix4f(-0.4225f, 0.2595f, 0.0643f, 0.0000f, 0.1725f, 0.1727f, 0.4364f, 0.0000f, 0.2042f, 0.3909f, -0.2355f, 0.0000f, 0.9950f, 3.9065f, 2.5248f, 1.0000f));
		}, 1);
		mAnimationLock = true;
		mCheeseLock = true;
		SpellSiriusBeams mBeam = new SpellSiriusBeams(this, mPlugin);
		new BukkitRunnable() {
			int mTicks = 0;
			@Nullable LivingEntity mTentacleBox1 = null;
			@Nullable LivingEntity mTentacleBox2 = null;
			@Nullable LivingEntity mTentacleBox4 = null;
			final BlockDisplay mTuulenDisplay = mBoss.getWorld().spawn(mTuulenLocation.clone().subtract(0.75f, 0.75f, 0.75f), BlockDisplay.class);
			final BlockDisplay mAuroraDisplay = mBoss.getWorld().spawn(mAuroraLocation.clone().subtract(0.75f, 0.75f, 0.75f), BlockDisplay.class);
			int mAliveTentacles = 3;
			final ChargeUpManager mBar = new ChargeUpManager(mBoss, 30 * 20, Component.text("Companions Corrupted in ", NamedTextColor.RED), BossBar.Color.RED, BossBar.Overlay.NOTCHED_10, 75);

			@Override
			public void run() {
				recalculatePlayers(); // this is the final part, make sure we have everyone we need.
				mBar.nextTick();
				if (mTicks % 20 == 0) {
					mBar.setTitle(Component.text("Companions Corrupted in " + ((30 * 20 - mTicks) / 20), NamedTextColor.RED));
				}
				if (mTicks == 0) {
					mTentacleBox1 = ((LivingEntity) LibraryOfSoulsIntegration.summon(mBoss.getLocation().add(-6, 2, 3), "BlightedPod"));
					mTentacleBox2 = ((LivingEntity) LibraryOfSoulsIntegration.summon(mBoss.getLocation().add(-2, 2, 6), "BlightedPod"));
					mTentacleBox4 = ((LivingEntity) LibraryOfSoulsIntegration.summon(mBoss.getLocation().add(-3, 2, -4), "BlightedPod"));
					if (mTentacleBox1 != null) {
						EntityUtils.setAttributeBase(mTentacleBox1, Attribute.GENERIC_MAX_HEALTH, DAMAGE_PER_PHASE * mDefenseScaling);
						mTentacleBox1.setHealth(DAMAGE_PER_PHASE * mDefenseScaling);
						mTentacleBox1.setInvisible(true);
						mTentacleBox1.setGlowing(true);
						mTentacleBox1.setCustomNameVisible(false);
						mTentacleBox1.customName(Component.text("Sirius Tentacle"));
					}
					if (mTentacleBox2 != null) {
						EntityUtils.setAttributeBase(mTentacleBox2, Attribute.GENERIC_MAX_HEALTH, DAMAGE_PER_PHASE * mDefenseScaling);
						mTentacleBox2.setHealth(DAMAGE_PER_PHASE * mDefenseScaling);
						mTentacleBox2.setInvisible(true);
						mTentacleBox2.setGlowing(true);
						mTentacleBox2.setCustomNameVisible(false);
						mTentacleBox2.customName(Component.text("Sirius Tentacle"));
					}
					if (mTentacleBox4 != null) {
						EntityUtils.setAttributeBase(mTentacleBox4, Attribute.GENERIC_MAX_HEALTH, DAMAGE_PER_PHASE * mDefenseScaling);
						mTentacleBox4.setHealth(DAMAGE_PER_PHASE * mDefenseScaling);
						mTentacleBox4.setInvisible(true);
						mTentacleBox4.setGlowing(true);
						mTentacleBox4.setCustomNameVisible(false);
						mTentacleBox4.customName(Component.text("Sirius Tentacle"));
					}
					mTuulenDisplay.setBlock(Bukkit.getServer().createBlockData(Material.WARPED_WART_BLOCK));
					mAuroraDisplay.setBlock(Bukkit.getServer().createBlockData(Material.WARPED_WART_BLOCK));
					mTuulenDisplay.setTransformation(new Transformation(new Vector3f(0, 0, 0), new AxisAngle4f(), new Vector3f(1.5f, 0.1f, 1.5f), new AxisAngle4f()));
					mAuroraDisplay.setTransformation(new Transformation(new Vector3f(0, 0, 0), new AxisAngle4f(), new Vector3f(1.5f, 0.1f, 1.5f), new AxisAngle4f()));
					mTuulenDisplay.setInterpolationDuration(1);
					mAuroraDisplay.setInterpolationDuration(1);
					mTuulenDisplay.setInterpolationDelay(-1);
					mAuroraDisplay.setInterpolationDelay(-1);
					for (Player p : mPlayers) {
						p.sendMessage(Component.text("[Sirius]", NamedTextColor.GOLD).append(Component.text(" If I am to die, I am taking them with me...", NamedTextColor.AQUA, TextDecoration.BOLD)));
						p.sendMessage(Component.text("[Tuulen]", NamedTextColor.GOLD).append(Component.text(" What is this?! The blight is gathering around me! Please, destroy Sirius now, before it’s too late!", NamedTextColor.GRAY, TextDecoration.BOLD)));
						p.sendMessage(Component.text("[Aurora]", NamedTextColor.GOLD).append(Component.text(" I as well... I can’t... hold it off anymore...", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)));
					}
				}
				if (mTicks == 2) {
					mTuulenDisplay.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1.5f, 2.25f, 1.5f), new AxisAngle4f()));
					mTuulenDisplay.setInterpolationDuration(30 * 20 - 2);
					mTuulenDisplay.setInterpolationDelay(-1);
					mAuroraDisplay.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1.5f, 2.25f, 1.5f), new AxisAngle4f()));
					mAuroraDisplay.setInterpolationDuration(30 * 20 - 2);
					mAuroraDisplay.setInterpolationDelay(-1);
				}
				if (mAliveTentacles > 0) {
					//whip a tentancle back
					if (mTentacleBox1 != null && mTentacleBox1.isDead()) {
						for (Display dis : mTentacles.get(0)) {
							dis.setInterpolationDelay(-1);
							dis.setInterpolationDuration(20);
							Transformation trans = dis.getTransformation();
							dis.setTransformation(new Transformation(trans.getTranslation().sub(0, 30, 0), trans.getLeftRotation(), trans.getScale(), trans.getRightRotation()));
						}
						mTentacleBox1 = null;
						mAliveTentacles--;
					}
					if (mTentacleBox2 != null && mTentacleBox2.isDead()) {
						for (Display dis : mTentacles.get(3)) {
							dis.setInterpolationDelay(-1);
							dis.setInterpolationDuration(20);
							Transformation trans = dis.getTransformation();
							dis.setTransformation(new Transformation(trans.getTranslation().sub(0, 30, 0), trans.getLeftRotation(), trans.getScale(), trans.getRightRotation()));
						}
						mTentacleBox2 = null;
						mAliveTentacles--;
					}
					if (mTentacleBox4 != null && mTentacleBox4.isDead()) {
						for (Display dis : mTentacles.get(1)) {
							dis.setInterpolationDelay(-1);
							dis.setInterpolationDuration(20);
							Transformation trans = dis.getTransformation();
							dis.setTransformation(new Transformation(trans.getTranslation().sub(0, 30, 0), trans.getLeftRotation(), trans.getScale(), trans.getRightRotation()));
						}
						mTentacleBox4 = null;
						mAliveTentacles--;
					}
				} else {
					//fall
					for (Display dis : mDisplays) {
						Transformation trans = dis.getTransformation();
						if (trans.getTranslation().y > -30) {
							dis.setInterpolationDelay(-1);
							dis.setInterpolationDuration(20);
							dis.setTransformation(new Transformation(trans.getTranslation().add(0, -30, 0), trans.getLeftRotation(), trans.getScale(), trans.getRightRotation()));
						}
					}
					mBoss.setVisibleByDefault(false);
					removeCollisionBox();
					mDone = true;
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						mStarBlightConverter.restoreAll();
						mSpawner.wipeMobs();
						for (Player player : mPlayers) {
							com.playmonumenta.plugins.utils.MessagingUtils.sendBoldTitle(player, Component.text("VICTORY", NamedTextColor.DARK_AQUA), Component.text("Sirius, The Final Herald", NamedTextColor.AQUA));
							player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.HOSTILE, 100, 0.8f);
							if (mClose) {
								AdvancementUtils.grantAdvancement(player, "monumenta:challenges/r3/sirius/condition");
							}
							if (!mPods.mEvolved && mStartingPlayerCount >= 10) {
								AdvancementUtils.grantAdvancement(player, "monumenta:challenges/r3/sirius/blight");
							}
						}
						mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
						Collections.shuffle(mDisplays);
						mBeam.cancel();
						new BukkitRunnable() {
							int mTicks = 0;
							final int mRate = mDisplays.size() / (6 * 20 - 21);
							int mPos = 0;

							@Override
							public void run() {
								for (int i = 0; i < mRate; i++) {
									BlockDisplay display = mDisplays.get(i + mPos);
									Transformation trans = display.getTransformation();
									display.setInterpolationDuration(20);
									display.setInterpolationDelay(-1);
									display.setTransformation(new Transformation(trans.getTranslation().add(0, 15, 0), trans.getLeftRotation(), new Vector3f(0, 0, 0), trans.getRightRotation()));
								}
								mPos += mRate;
								if (mTicks >= 7 * 20 - 21) {
									this.cancel();
								}
								mTicks++;
							}
						}.runTaskTimer(mPlugin, 0, 1);
					}, 21);
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						mBoss.remove();
						death(null);
					}, 7 * 20);
					mTuulenDisplay.remove();
					mAuroraDisplay.remove();
					this.cancel();
				}
				if (mTicks >= 30 * 20) {
					mStarBlightConverter.blightArena(List.of(mBoss.getLocation()), 0, 20, 20, mPlugin);
					for (Player p : getPlayersInArena(false)) {
						AdvancementUtils.grantAdvancement(p, "monumenta:challenges/r3/sirius/fault");
						com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(p, "Stasis");
						com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(p, VoodooBonds.PROTECTION_EFFECT);
						if (p.isInvulnerable()) {
							p.setInvulnerable(false);
						}
						DamageUtils.damage(mBoss, p, DamageEvent.DamageType.TRUE, 999999999, null, true, false, "never ending blight.");
						if (p.getHealth() > 0) {
							p.setHealth(0); // For good measure
						}
					}
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						mTuulenDisplay.remove();
						mAuroraDisplay.remove();
					}, 20);
					this.cancel();
				}
				if (mBoss.isDead()) {
					mTuulenDisplay.remove();
					mAuroraDisplay.remove();
					if (mTentacleBox1 != null) {
						mTentacleBox1.remove();
					}
					if (mTentacleBox2 != null) {
						mTentacleBox2.remove();
					}
					if (mTentacleBox4 != null) {
						mTentacleBox4.remove();
					}
					this.cancel();
				}
				//redundancy
				if (mTicks != 0 && mTentacleBox1 == null && mTentacleBox2 == null && mTentacleBox4 == null) {
					mAliveTentacles = 0;
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, intduration, 1);
	}

	private void recalculatePlayers() {
		mPlayers = getPlayersInArena(false);
	}

	@Override
	public boolean hasNearbyPlayerDeathTrigger() {
		return true;
	}

	@Override
	public void nearbyPlayerDeath(PlayerDeathEvent event) {
		Player p = event.getPlayer();
		mPlayers.remove(p); // remove players that die
		EffectManager.getInstance().clearEffects(p, PassiveStarBlight.STARBLIGHTAG);
		EffectManager.getInstance().clearEffects(p, SpellBlightedBolts.BLIGHTEDBOLTTAG);
		if (mDamagePhaseHPBar != null) {
			p.hideBossBar(mDamagePhaseHPBar);
		}
	}
}
