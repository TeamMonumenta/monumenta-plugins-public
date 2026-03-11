package com.playmonumenta.plugins.hunts.bosses;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.HuntsManager;
import com.playmonumenta.plugins.hunts.bosses.spells.BanishSporeHallucination;
import com.playmonumenta.plugins.hunts.bosses.spells.PassiveSporeDefense;
import com.playmonumenta.plugins.hunts.bosses.spells.PassiveSpores;
import com.playmonumenta.plugins.hunts.bosses.spells.SporeBeastArenaRange;
import com.playmonumenta.plugins.hunts.bosses.spells.SporeBeastMobSpawning;
import com.playmonumenta.plugins.hunts.bosses.spells.SporeInfection;
import com.playmonumenta.plugins.hunts.bosses.spells.SporeLasers;
import com.playmonumenta.plugins.hunts.bosses.spells.SporeRelease;
import com.playmonumenta.plugins.hunts.bosses.spells.SporeShower;
import com.playmonumenta.plugins.hunts.bosses.spells.Uproot;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SporousAmalgam extends Quarry {
	public static final String identityTag = "boss_sporous_amalgam";
	public static final TextColor TEXT_COLOR = TextColor.color(178, 255, 23);
	public static final int SPELL_INNER_RADIUS = 20;
	public static final int INNER_RADIUS = 30;
	public static final int OUTER_RADIUS = 40;
	public static final double HEALTH = 8000;

	public static final double LAST_PHASE = 20;
	private static final String LAST_PHASE_SOUL_SUMMON = "UprootedSporousAmalgam";

	public static final String NORMAL_POOL_NAME = "~SporeBeastNormals";
	public static final String ELITE_POOL_NAME = "~SporeBeastElites";
	public static final String PARTY_POOL_NAME = "~SporeBeastParties";

	private static final Map<String, Integer> mSummonsValues = ImmutableMap.of(
		"Fungal Spreader", 1,
		"Afflicted Hound", 1,
		"Intoxicated Mycelia", 1,
		"Pestilence Pot", 1,
		"Spore Grenadier", 4,
		"Sporebound Titan", 3,
		"Mycelian Shredder", 5,
		"Saprotroph Stalk", 4
	);

	private static final int ENTITY_RISE_DURATION = 20 * 2;
	private static final double UPROOT_SELF_DAMAGE_PERCENTAGE = 2.5;

	private static final int BASE_SUMMONS_TO_KILL = 12;
	private int mSummonsToKill;
	private final List<LivingEntity> mSummons = new ArrayList<>();
	private int mKilledSummons = 0;
	private int mSelfDamageTimes = 1;

	private List<Player> mPlayersInOutRange;
	private final List<Player> mPlayersThatSawBanish;
	private final List<Player> mPlayersInBanish;
	private final List<Player> mPlayersThatGotInfected;
	private @Nullable LivingEntity mUprootedBeast = null;

	private final PassiveSpores mPassiveSpores;
	private final PassiveSporeDefense mPassiveSporeDefense;
	private final SporeBeastArenaRange mSporeBeastArenaRange;
	private final SporeBeastMobSpawning mSporeBeastMobSpawning;

	private @Nullable Spell mLastCastSpell = null;

	public SporousAmalgam(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc, INNER_RADIUS, OUTER_RADIUS, HuntsManager.QuarryType.SPOROUS_AMALGAM, 0.5, 0.4);

		mBoss.setAI(false);
		mBoss.teleport(spawnLoc);
		mBoss.setRotation(0, 0);
		mBoss.setRemoveWhenFarAway(false);
		mBoss.addScoreboardTag("Boss");
		mBoss.customName(Component.text("Sporous Amalgam", NamedTextColor.GREEN, TextDecoration.BOLD));
		mBoss.setCustomNameVisible(false);
		EntityUtils.setMaxHealthAndHealth(mBoss, HEALTH);

		mPlayersInOutRange = PlayerUtils.playersInRange(boss.getLocation(), OUTER_RADIUS, true);
		mPlayersThatSawBanish = new ArrayList<>();
		mPlayersInBanish = new ArrayList<>();
		mPlayersThatGotInfected = new ArrayList<>();

		new BukkitRunnable() {

			@Override
			public void run() {
				updatePlayersInOutRange();
				if (mBoss.isDead()) {
					this.cancel();
				}
			}
		}.runTaskTimer(plugin, 0, 1);

		calculateSummonsToKill();
		mKilledSummons = mSummonsToKill / 3;

		mPassiveSpores = new PassiveSpores(plugin, this);
		mPassiveSporeDefense = new PassiveSporeDefense(this);
		mSporeBeastArenaRange = new SporeBeastArenaRange(this);
		mSporeBeastMobSpawning = new SporeBeastMobSpawning(this);

		List<Spell> passives = List.of(
			mPassiveSpores,
			mPassiveSporeDefense,
			mSporeBeastArenaRange,
			new SporeLasers(plugin, this),
			mSporeBeastMobSpawning
		);

		List<Spell> lastPhasePassives = List.of(
			mPassiveSpores,
			new SporeBeastArenaRange(this)
		);

		SpellManager phase1 = new SpellManager(List.of(
			new Uproot(plugin, this),
			new SporeShower(plugin, this),
			new SporeInfection(plugin, this),
			new SporeRelease(plugin, this)
		));

		SpellManager lastPhaseSpells = new SpellManager(List.of(
			new SporeShower(plugin, this)
		));


		Map<Integer, BossBarManager.BossHealthAction> healthEvents = new HashMap<>();
		healthEvents.put((int) LAST_PHASE, eventBoss -> {
			boss.setVisibleByDefault(false);
			boss.setCollidable(false);
			boss.setInvulnerable(true);
			removeSummons();
			for (Player p : mPlayersInOutRange) {
				p.sendMessage(Component.text("The beast has detached from its roots and is going on a rampage, stop it quickly!", TEXT_COLOR));
				p.playSound(boss, Sound.ENTITY_RAVAGER_ROAR, 1f, 0.85f);
				p.playSound(boss, Sound.ENTITY_SLIME_ATTACK, 1f, 0.75f);
				p.playSound(boss, Sound.ENTITY_SLIME_DEATH, 1f, 0.45f);
			}
			mUprootedBeast = (LivingEntity) LibraryOfSoulsIntegration.summon(boss.getLocation().add(0, 1, 0), LAST_PHASE_SOUL_SUMMON);
			new BukkitRunnable() {

				@Override
				public void run() {
					if (mUprootedBeast == null || !mUprootedBeast.isValid()) {
						mBoss.setHealth(0);
						this.cancel();
						return;
					}

					double percentage = mUprootedBeast.getHealth() / EntityUtils.getMaxHealth(mUprootedBeast);
					mBoss.setHealth(HEALTH * percentage);

					if (mUprootedBeast.getLocation().distanceSquared(mSpawnLoc) > SPELL_INNER_RADIUS * SPELL_INNER_RADIUS) {
						mUprootedBeast.setVelocity(LocationUtils.getDirectionTo(mSpawnLoc, mUprootedBeast.getLocation()).multiply(3).add(new Vector(0, 2, 0)));
					}

					if (!mBoss.isValid()) {
						if (mUprootedBeast != null) {
							mUprootedBeast.setHealth(0);
						}
						this.cancel();
					}
				}
			}.runTaskTimer(plugin, 0, 1);

			mPassiveSporeDefense.removeVulnerable();
			mKilledSummons = 0;
			changePhase(lastPhaseSpells, lastPhasePassives, null);
		});

		BossBarManager mBossBar = new BossBarManager(mBoss, OUTER_RADIUS, BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10, healthEvents, true, false, mSpawnLoc);

		super.constructBoss(phase1, passives, OUTER_RADIUS, mBossBar, 40, 1);
	}


	@Override
	public String getUnspoiledLootTable() {
		return "epic:r3/hunts/loot/spore_unspoiled";
	}

	@Override
	public String getSpoiledLootTable() {
		return "epic:r3/hunts/loot/spore_spoiled";
	}

	@Override
		public String getAdvancement() {
		return "monumenta:challenges/r3/hunts/sporous_amalgam";
	}

	@Override
	public String getQuestTag() {
		return "HuntSpore";
	}

	public boolean canRunUproot() {
		if (mPassiveSporeDefense.isVulnerable()) {
			return false;
		}

		return mKilledSummons >= mSummonsToKill;
	}

	public boolean isVulnerable() {
		return mPassiveSporeDefense.isVulnerable();
	}

	public void uproot() {
		mBoss.setHealth(mBoss.getHealth() - HEALTH / 100 * (UPROOT_SELF_DAMAGE_PERCENTAGE * mSelfDamageTimes));
		mSelfDamageTimes++;
		DamageUtils.damage(mBoss, mBoss, DamageEvent.DamageType.TRUE, 1);
		for (Player p : mPlayersInOutRange) {
			for (int i = 0; i < 5; i++) {
				p.playSound(p, Sound.ENTITY_PILLAGER_DEATH, SoundCategory.HOSTILE, 2.0f, 1f - 0.1f * i);
				p.playSound(p, Sound.BLOCK_WOOD_PLACE, SoundCategory.HOSTILE, 2.0f, 1f - 0.1f * i);
				p.playSound(p, Sound.ENTITY_SLIME_HURT, SoundCategory.HOSTILE, 2.0f, 1f - 0.1f * i);
			}
			new PartialParticle(Particle.FLASH, mBoss.getLocation(), 3).delta(0.5).spawnAsBoss();
			p.sendMessage(Component.text("The beast's roots have grown weaker, now's the time to attack!", TEXT_COLOR));
		}
		GlowingManager.startGlowing(mBoss, NamedTextColor.RED, PassiveSporeDefense.VULNERABILITY_DURATION, GlowingManager.BOSS_SPELL_PRIORITY + 1);
		mPassiveSporeDefense.setVulnerableState();
	}

	public void resetKilledSummons() {
		calculateSummonsToKill();
		mKilledSummons = 0;
	}

	private void calculateSummonsToKill() {
		mSummonsToKill = 0;
		int size = mPlayersInOutRange.size();
		for (int i = 0; i < size; i++) {
			mSummonsToKill += (int) (BASE_SUMMONS_TO_KILL * Math.pow(0.9, i));
		}
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		super.death(event);

		if (mUprootedBeast != null) {
			mUprootedBeast.setHealth(0);
		}
		mPassiveSpores.clearSpores();
		removeSummons();
	}

	@Override
	public void onDespawn() {
		super.onDespawn();

		if (mUprootedBeast != null) {
			mUprootedBeast.setHealth(0);
		}
		mPassiveSpores.clearSpores();
		removeSummons();
	}

	@Override
	public void unload() {
		super.unload();

		if (mUprootedBeast != null) {
			mUprootedBeast.setHealth(0);
		}
		mPassiveSpores.clearSpores();
		removeSummons();
	}

	private void removeSummons() {
		for (Entity e : mSummons) {
			if (e.isValid()) {
				for (Entity passenger : e.getPassengers()) {
					if (e.isValid()) {
						passenger.remove();
					}
				}
				e.remove();
			}
		}
	}

	@Override
	public void onHurt(DamageEvent event) {
		super.onHurt(event);
		LivingEntity source = event.getSource();
		if (source instanceof Player player) {
			mSporeBeastArenaRange.addValidPlayer(player);
		}
	}

	@Override
	public String getBanishMessage() {
		return "PLAYER fell victim to the QUARRY's hallucinations";
	}

	public void addSummon(LivingEntity entity) {
		mSummons.add(entity);
		new BukkitRunnable() {
			final LivingEntity mEntity = mSummons.getLast();
			final Location mLocation = mEntity.getLocation();
			final double mYDown = mEntity.getEyeHeight();
			final double mYPerTick = mYDown / ENTITY_RISE_DURATION;
			int mTicks = 0;
			boolean mWasEntityGlowing = false;
			boolean mWasEntityNoGravity = false;
			boolean mWasEntityActive = true;

			@Override
			public void run() {
				if (mTicks == 0) {
					if (mEntity.isGlowing()) {
						mWasEntityGlowing = true;
					}


					if (!mEntity.hasGravity()) {
						mWasEntityNoGravity = true;
					}

					if (!mEntity.hasAI()) {
						mWasEntityActive = false;
					}

					mEntity.setGlowing(true);
					mEntity.setGravity(false);
					mEntity.setAI(false);
					mEntity.teleport(mLocation.subtract(0, mYDown, 0));
				}

				mEntity.teleport(mLocation.add(0, mYPerTick, 0));

				if (mTicks == ENTITY_RISE_DURATION) {
					if (!mWasEntityGlowing) {
						mEntity.setGlowing(false);
					}
					if (!mWasEntityNoGravity) {
						mEntity.setGravity(true);
					}
					if (mWasEntityActive) {
						mEntity.setAI(true);
					}
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	public void addSpores(Player player, float sporeAmount) {
		mPassiveSpores.addSpores(player, sporeAmount);
	}

	public void updateSummonsList() {
		for (int i = 0; i < mSummons.size(); i++) {
			Entity checkEntity = mSummons.get(i);
			if (checkEntity.isDead()) {
				String string = MessagingUtils.plainFromLegacy(checkEntity.getName());
				if (mSummonsValues.containsKey(string)) {
					mKilledSummons += mSummonsValues.get(string);
				}
				mSummons.remove(checkEntity);
				i--;
			} else if (checkEntity.getLocation().distanceSquared(mBoss.getLocation()) > SPELL_INNER_RADIUS * SPELL_INNER_RADIUS) {
				checkEntity.setVelocity(LocationUtils.getDirectionTo(mBoss.getLocation(), checkEntity.getLocation()).multiply(2).add(new Vector(0, 0.5, 0)));
			}
		}

		if (mSummons.isEmpty()) {
			mSporeBeastMobSpawning.hastenWave();
		}
	}

	public boolean canRunSpell(Spell spell) {
		if (mUprootedBeast != null && mUprootedBeast.isValid()) {
			return true;
		}

		if (isVulnerable()) {
			return false;
		}

		if (mLastCastSpell == null || mLastCastSpell != spell) {
			mLastCastSpell = spell;
			return true;
		}
		return false;
	}

	public boolean hasSeenBanish(Player player) {
		return mPlayersThatSawBanish.contains(player);
	}

	public List<Player> getValidPlayersForBanishTargeting() {
		List<Player> playersThatCanBanish = new ArrayList<>();
		boolean allPlayersSawBanish = true;
		for (Player p : mPlayersInOutRange) {
			if (!hasSeenBanish(p)) {
				allPlayersSawBanish = false;
				playersThatCanBanish.add(p);
			}
		}
		if (allPlayersSawBanish) {
			mPlayersThatSawBanish.clear();
			return mPlayersInOutRange;
		} else {
			return playersThatCanBanish;
		}
	}

	public void clearPlayerThatSawBanish() {
		mPlayersThatSawBanish.clear();
	}

	public void doBanishSequence(Player player) {
		mPlayersThatSawBanish.add(player);
		new BanishSporeHallucination(mPlugin, this, player).run();
	}

	public int summons() {
		return mSummons.size();
	}

	public Location getRandomLocationInArena(double minBossDistance, double minEdgeDistance, double yOffset) {
		double length = FastUtils.randomDoubleInRange(minBossDistance, SPELL_INNER_RADIUS - minEdgeDistance);
		double angle = FastUtils.randomIntInRange(1, 360);
		return mBoss.getLocation().add(FastUtils.sinDeg(angle) * length, yOffset, FastUtils.cosDeg(angle) * length);
	}

	public Location getRandomLocationGivenAngle(double minBossDistance, double minEdgeDistance, double yOffset, double angle) {
		double length = FastUtils.randomDoubleInRange(minBossDistance, SPELL_INNER_RADIUS - minEdgeDistance);
		return mBoss.getLocation().add(FastUtils.sinDeg(angle) * length, yOffset, FastUtils.cosDeg(angle) * length);
	}

	public void updatePlayersInOutRange() {
		mPlayersInOutRange = PlayerUtils.playersInRange(mBoss.getLocation(), OUTER_RADIUS, true);
	}

	public List<Player> getPlayersInOutRange() {
		return mPlayersInOutRange;
	}

	public List<Player> getPlayersInInRange() {
		return PlayerUtils.playersInRange(mBoss.getLocation(), SPELL_INNER_RADIUS, true);
	}

	public List<Player> getPlayersInBanish() {
		return mPlayersInBanish;
	}

	public Player getPlayerForInfection() {
		ArrayList<Player> players = new ArrayList<>();
		boolean allPlayersInfected = true;
		for (Player p : mPlayersInOutRange) {
			if (!mPlayersThatGotInfected.contains(p)) {
				players.add(p);
				allPlayersInfected = false;
			}
		}
		if (allPlayersInfected) {
			mPlayersThatGotInfected.clear();
			Player p = FastUtils.getRandomElement(mPlayersInOutRange);
			mPlayersThatGotInfected.add(p);
			return p;
		} else {
			Player p = FastUtils.getRandomElement(players);
			mPlayersThatGotInfected.add(p);
			return p;
		}
	}

	public boolean isLastPhase() {
		return mUprootedBeast != null;
	}

	public void addPlayerInBanish(Player player) {
		mPlayersInBanish.add(player);
	}

	public void removePlayerInBanish(Player player) {
		mPlayersInBanish.remove(player);
	}
}
