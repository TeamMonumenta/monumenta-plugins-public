package com.playmonumenta.plugins.bosses.bosses.bluestrike;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellConditionalTeleport;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.bosses.spells.SpellTargetVisiblePlayer;
import com.playmonumenta.plugins.bosses.spells.bluestrike.SpellCrystalBarrage;
import com.playmonumenta.plugins.bosses.spells.bluestrike.SpellDeathSweep;
import com.playmonumenta.plugins.bosses.spells.bluestrike.SpellDominion;
import com.playmonumenta.plugins.bosses.spells.bluestrike.SpellRealitySlash;
import com.playmonumenta.plugins.bosses.spells.bluestrike.SpellSamwellRegeneration;
import com.playmonumenta.plugins.bosses.spells.bluestrike.SpellSamwellSmokeBomb;
import com.playmonumenta.plugins.bosses.spells.bluestrike.SpellSummonBlueStrike;
import com.playmonumenta.plugins.bosses.spells.bluestrike.SpellSummonBlueStrikeTargets;
import com.playmonumenta.plugins.bosses.spells.bluestrike.SpellSummonLavaTitan;
import com.playmonumenta.plugins.effects.SamwellBlackbloodDagger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class Samwell extends BossAbilityGroup {
	public static final String identityTag = "boss_samwell";
	public static final String DAGGER_EFFECT_SOURCE = "SamwellBlackbloodDagger";
	public static final String OBJECTIVE_SHARD_NAME = "temp";
	public static final String OBJECTIVE_FAILS_NAME = "TempA";
	private static final int CRAFT_DURATION = Constants.TICKS_PER_SECOND * 25;
	private static final int DAGGER_DURATION = Constants.TICKS_PER_SECOND * 30;
	private static final int HEALTH = 8000;

	private static final int detectionRange = 100;
	public final Location mSpawnLoc;
	public final Location mDaggerLoc;
	public final Location mEndLoc;
	public int mPhase = 1;
	public boolean mCraftPhase = false;
	public boolean mDaggerPhase = false;
	public final BossBar mGatheringBar;
	public final BossBar mCraftingBar;
	private final List<Spell> mInactivePassives;
	private final List<Spell> mBasePassives;
	List<Spell> mPhase1Passives;
	List<Spell> mPhase2Passives;
	List<Spell> mPhase3Passives;
	List<Spell> mPhase4Passives;
	List<Spell> mCraft1Passives;
	List<Spell> mCraft2Passives;
	List<Spell> mCraft3Passives;
	List<Spell> mDagger1Passives;
	List<Spell> mDagger2Passives;
	List<Spell> mDagger3Passives;
	SpellManager mPhase1Actives;
	SpellManager mPhase2Actives;
	SpellManager mPhase3Actives;
	SpellManager mPhase4Actives;
	private final ItemStack mDagger;
	public final ItemStack mShards;
	public int mShardsReq;
	public int mTimerCrafting = 0;
	public boolean mHealedBefore = false;
	public boolean mPhase4Damaged = false;
	public boolean mDefeated = false;
	private int mPlayerCount;
	private double mDefenseScaling;

	private final BlueStrikeDaggerCraftingBoss mBhairaviAbility;
	private final BlueStrikeDaggerCraftingBoss mIzzyAbility;
	private final BlueStrikeDaggerCraftingBoss mLevynAbility;

	public Samwell(Plugin plugin, LivingEntity boss, Location startLoc, Location endLoc) {
		super(plugin, identityTag, boss);
		mSpawnLoc = startLoc;
		mEndLoc = endLoc;
		final HashMap<Double, LoSPool> weights = new HashMap<>();
		weights.put(0.15, LoSPool.fromString("~BlueMaskedElite"));
		weights.put(0.85, LoSPool.fromString("~BlueMaskedNormal"));

		final Location bhairaviLoc = mSpawnLoc.clone().add(15, 1, 0);
		final Location izzyLoc = mSpawnLoc.clone().add(-8, 1, 12);
		final Location levynLoc = mSpawnLoc.clone().add(-8, 1, -12);
		mDaggerLoc = mSpawnLoc.clone().add(0, 8, 0);

		mDagger = InventoryUtils.getItemFromLootTableOrThrow(mSpawnLoc,
			NamespacedKey.fromString("epic:r3/dungeons/bluestrike/boss/blackblood_dagger"));
		mShards = InventoryUtils.getItemFromLootTableOrThrow(mSpawnLoc,
			NamespacedKey.fromString("epic:r3/dungeons/bluestrike/boss/blackblood_shard"));

		mCraftingBar = BossBar.bossBar(Component.text("Crafting...", NamedTextColor.YELLOW, TextDecoration.BOLD),
			0, BossBar.Color.YELLOW, BossBar.Overlay.NOTCHED_6);
		mGatheringBar = BossBar.bossBar(Component.empty(), 0, BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_6);
		// Title of mGatheringBar is properly set in init()

		mSpawnLoc.getNearbyEntitiesByType(Villager.class, 30).forEach(Entity::remove);
		Villager bhairavi = mSpawnLoc.getWorld().spawn(bhairaviLoc, Villager.class);
		bhairavi.customName(Component.text("Bhairavi"));
		Villager izzy = mSpawnLoc.getWorld().spawn(izzyLoc, Villager.class);
		izzy.customName(Component.text("Izzy"));
		Villager levyn = mSpawnLoc.getWorld().spawn(levynLoc, Villager.class);
		levyn.customName(Component.text("Levyn"));

		mBhairaviAbility = new BlueStrikeDaggerCraftingBoss(mPlugin, bhairavi, this);
		mIzzyAbility = new BlueStrikeDaggerCraftingBoss(mPlugin, izzy, this);
		mLevynAbility = new BlueStrikeDaggerCraftingBoss(mPlugin, levyn, this);

		BossManager.getInstance().manuallyRegisterBoss(bhairavi, mBhairaviAbility);
		BossManager.getInstance().manuallyRegisterBoss(izzy, mIzzyAbility);
		BossManager.getInstance().manuallyRegisterBoss(levyn, mLevynAbility);

		mInactivePassives = List.of(
			new SpellDominion(plugin, boss, mSpawnLoc, false)
		);

		mBasePassives = Arrays.asList(
			new SpellBlockBreak(mBoss),
			new SpellConditionalTeleport(mBoss, startLoc, b -> {
				// Boss isn't stuck in lava or bedrock
				boolean condition1 = b.getLocation().getBlock().getType() == Material.BEDROCK ||
					b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK ||
					b.getLocation().getBlock().getType() == Material.LAVA;

				// Boss isn't too far off arena.
				boolean condition2 = (boss.getLocation().distance(startLoc) > 30)
					|| Math.abs(boss.getLocation().getY() - startLoc.getY()) > 5;

				return condition1 || condition2;
			}),
			new SpellDominion(plugin, mBoss, mSpawnLoc, true),
			new SpellTargetVisiblePlayer((Mob) mBoss, detectionRange, 60, 160),
			new SpellRunAction(() -> {
				if (mBoss.hasPotionEffect(PotionEffectType.GLOWING)) {
					mBoss.removePotionEffect(PotionEffectType.GLOWING);
				}
			})
		);

		mPhase1Passives = new ArrayList<>(Arrays.asList(
			new SpellSamwellRegeneration(mBoss, this, 1),
			new SpellSummonBlueStrike(mPlugin, mBoss, mSpawnLoc, weights, 12 * 4, 9, 20)
		));
		mPhase2Passives = new ArrayList<>(Arrays.asList(
			new SpellSamwellRegeneration(mBoss, this, 3.0 / 4.0),
			new SpellSummonBlueStrike(mPlugin, mBoss, mSpawnLoc, weights, 12 * 4, 9, 20)
		));
		mPhase3Passives = new ArrayList<>(Arrays.asList(
			new SpellSamwellRegeneration(mBoss, this, 2.0 / 4.0),
			new SpellSummonBlueStrike(mPlugin, mBoss, mSpawnLoc, weights, 12 * 4, 9, 20)
		));
		mPhase4Passives = new ArrayList<>(List.of(
			new SpellSummonBlueStrike(mPlugin, mBoss, mSpawnLoc, weights, 12 * 4, 9, 20)
		));
		mCraft1Passives = new ArrayList<>(Arrays.asList(
			new SpellSamwellRegeneration(mBoss, this, 1),
			new SpellSummonBlueStrike(mPlugin, mBoss, mSpawnLoc, weights, 15 * 4, 9, 20),
			new SpellSummonBlueStrikeTargets(mBoss, mSpawnLoc, 8 * 4, 1, 1, 4)
		));
		mCraft2Passives = new ArrayList<>(Arrays.asList(
			new SpellSamwellRegeneration(mBoss, this, 3.0 / 4.0),
			new SpellSummonBlueStrike(mPlugin, mBoss, mSpawnLoc, weights, 15 * 4, 9, 20),
			new SpellSummonBlueStrikeTargets(mBoss, mSpawnLoc, 8 * 4, 1, 1, 4)
		));
		mCraft3Passives = new ArrayList<>(Arrays.asList(
			new SpellSamwellRegeneration(mBoss, this, 2.0 / 4.0),
			new SpellSummonBlueStrike(mPlugin, mBoss, mSpawnLoc, weights, 15 * 4, 9, 20),
			new SpellSummonBlueStrikeTargets(mBoss, mSpawnLoc, 8 * 4, 1, 1, 4)
		));
		mDagger1Passives = new ArrayList<>(List.of(
			new SpellSamwellRegeneration(mBoss, this, 1)
		));
		mDagger2Passives = new ArrayList<>(List.of(
			new SpellSamwellRegeneration(mBoss, this, 3.0 / 4.0)
		));
		mDagger3Passives = new ArrayList<>(List.of(
			new SpellSamwellRegeneration(mBoss, this, 2.0 / 4.0)
		));

		mPhase1Passives.addAll(mBasePassives);
		mPhase2Passives.addAll(mBasePassives);
		mPhase3Passives.addAll(mBasePassives);
		mPhase4Passives.addAll(mBasePassives);
		mCraft1Passives.addAll(mBasePassives);
		mCraft2Passives.addAll(mBasePassives);
		mCraft3Passives.addAll(mBasePassives);
		mDagger1Passives.addAll(mBasePassives);
		mDagger2Passives.addAll(mBasePassives);
		mDagger3Passives.addAll(mBasePassives);

		mPhase1Actives = new SpellManager(Arrays.asList(
			new SpellDeathSweep(mPlugin, mBoss, 1),
			new SpellSamwellSmokeBomb(plugin, mBoss, 1),
			new SpellRealitySlash(plugin, mBoss, this, 1)
		));
		mPhase2Actives = new SpellManager(Arrays.asList(
			new SpellDeathSweep(mPlugin, mBoss, 2),
			new SpellSamwellSmokeBomb(plugin, mBoss, 2),
			new SpellRealitySlash(plugin, mBoss, this, 2),
			new SpellCrystalBarrage(plugin, mBoss, this, 2)
		));
		mPhase3Actives = new SpellManager(Arrays.asList(
			new SpellDeathSweep(mPlugin, mBoss, 3),
			new SpellSamwellSmokeBomb(plugin, mBoss, 3),
			new SpellCrystalBarrage(plugin, mBoss, this, 3),
			new SpellRealitySlash(plugin, mBoss, this, 3),
			new SpellSummonLavaTitan(plugin, mBoss, mSpawnLoc, 3)
		));
		mPhase4Actives = new SpellManager(Arrays.asList(
			new SpellDeathSweep(mPlugin, mBoss, 4),
			new SpellSamwellSmokeBomb(plugin, mBoss, 4),
			new SpellCrystalBarrage(plugin, mBoss, this, 4),
			new SpellRealitySlash(plugin, mBoss, this, 4),
			new SpellSummonLavaTitan(plugin, mBoss, mSpawnLoc, 4)
		));

		final BossBarManager bossBar = new BossBarManager(boss, detectionRange, BossBar.Color.BLUE,
			BossBar.Overlay.NOTCHED_10, null, false);
		super.constructBoss(SpellManager.EMPTY, mInactivePassives, detectionRange, bossBar);
	}

	@Override
	public void init() {
		mBoss.setAI(false);

		// Going to be using Scoreboards for this, easier to track between functions (I hope)
		resetScoreboard();

		mPlayerCount = getPlayers().size();
		EntityUtils.setMaxHealthAndHealth(mBoss, HEALTH);
		mDefenseScaling = BossUtils.healthScalingCoef(mPlayerCount, 0.5, 0.4);

		GlowingManager.startGlowing(mBoss, NamedTextColor.BLUE, -1, GlowingManager.BOSS_SPELL_PRIORITY - 1,
			null, "samwell");

		mShardsReq = (int) (3 + Math.floor(getPlayers().size() / 2.0));
		refreshGatheringBar();

		sendMessage("Well, I wasn't sure if you guys would make it this far. How do you like my new place?");

		Bukkit.getScheduler().runTaskLater(mPlugin, () ->
			sendMessage("It'll be the last thing you see... I've got the Blue Wool on my side!"), (int) (Constants.TICKS_PER_SECOND * 3.5));

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mBoss.setAI(true);
			changePhaseNormal();
			getPlayers().forEach(p -> {
				com.playmonumenta.plugins.utils.MessagingUtils.sendBoldTitle(p,
					Component.text("Samwell", NamedTextColor.DARK_RED),
					Component.text("Usurper Of Life", NamedTextColor.RED));
				p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Constants.TICKS_PER_SECOND * 2,
					2, true, false, false));
			});
			mSpawnLoc.getWorld().playSound(mSpawnLoc, Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 5, 0.7f);
		}, Constants.TICKS_PER_SECOND * 4);
	}

	@Override
	public void onHurt(DamageEvent event) {
		event.setFlatDamage(event.getFlatDamage() / mDefenseScaling);
	}

	@Override
	public boolean hasNearbyPlayerDeathTrigger() {
		return true;
	}

	@Override
	public void nearbyPlayerDeath(final PlayerDeathEvent event) {
		mPlayerCount--;
		mDefenseScaling = BossUtils.healthScalingCoef(mPlayerCount, 0.5, 0.4);
		final Player player = event.getPlayer();
		player.hideBossBar(mGatheringBar);
		player.hideBossBar(mCraftingBar);
	}

	@Override
	public void onHurtByEntity(final DamageEvent event, final Entity damager) {
		if (mPhase < 4) {
			if (damager instanceof Player player && event.getType() == DamageEvent.DamageType.MELEE
				&& Plugin.getInstance().mEffectManager.hasEffect(player, DAGGER_EFFECT_SOURCE)) {
				switch (mPhase) {
					case 1 -> {
						mBoss.setHealth(EntityUtils.getMaxHealth(mBoss) * (3.0 / 4.0));
						sendMessage("Ugh, what! How did you do that? It... burns!");
						mPhase = 2;
						changePhaseNormal();
						Bukkit.getScheduler().runTaskLater(mPlugin, () -> forceCastSpell(SpellCrystalBarrage.class),
							Constants.TICKS_PER_SECOND * 2);
					}
					case 2 -> {
						mBoss.setHealth(EntityUtils.getMaxHealth(mBoss) * (2.0 / 4.0));
						sendMessage("No, what?! Why can't it heal? What are you using?");
						mPhase = 3;
						changePhaseNormal();
						forceCastSpell(SpellSummonLavaTitan.class);
					}
					default -> {
						mBoss.setHealth(EntityUtils.getMaxHealth(mBoss) * (1.0 / 4.0));
						sendMessage("I'm done with you! That dagger is too much. I don't get why Blue can't " +
							"heal from it? Is it another wool?!");
						mPhase = 4;
						GlowingManager.startGlowing(mBoss, NamedTextColor.BLACK, -1,
							GlowingManager.BOSS_SPELL_PRIORITY - 1, null, "samwell");
						changePhaseNormal();
					}
				}
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.HOSTILE, 5f, 0.5f);
				MovementUtils.knockAway(player, mBoss, 3.0f, 0.5f);
				clearDaggerAndShards();
				return;
			}

			// Lower damage so game breaking builds can't realistically ruin this
			event.setFlatDamage(event.getFlatDamage() * 0.1);
		} else if (!mPhase4Damaged) {
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				if (mDefeated) {
					return;
				}
				sendMessage("It can't be... I can't feel the Blue Wool anymore... Has it run out? Or... has it turned its back on me??");

				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					if (mDefeated) {
						return;
					}
					sendMessage(Component.empty()
						.append(Component.text("Please", NamedTextColor.RED, TextDecoration.ITALIC))
						.append(Component.text("! ", NamedTextColor.RED))
						.append(Component.text("Harrakfar", NamedTextColor.DARK_RED, TextDecoration.BOLD))
						.append(Component.text("! Heal me!", NamedTextColor.RED)));
					mSpawnLoc.getWorld().playSound(mSpawnLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 10, 1.2f);
				}, 70);
			}, 20);
			mPhase4Damaged = true;
		}
	}

	public void startCraftPhase() {
		changePhaseCraft();

		mCraftingBar.progress(0);
		resetScoreboard();
		List<Player> players = getPlayers();

		for (Player player : players) {
			player.showBossBar(mCraftingBar);
		}

		mBhairaviAbility.craft();
		mLevynAbility.craft();
		mIzzyAbility.craft();

		Item craftItem = mBoss.getWorld().spawn(mDaggerLoc, Item.class);
		craftItem.setItemStack(mDagger);
		craftItem.setGravity(false);
		craftItem.setCanPlayerPickup(false);
		craftItem.setInvulnerable(true);
		craftItem.setGlowing(true);

		mTimerCrafting = 0;
		new BukkitRunnable() {

			@Override
			public void run() {
				mTimerCrafting++;
				mCraftingBar.progress((float) mTimerCrafting / CRAFT_DURATION);
				if (!mCraftPhase) {
					mTimerCrafting = 0;
					for (Player player : players) {
						player.hideBossBar(mCraftingBar);
					}
					failCraft();
					craftItem.remove();
					this.cancel();
					return;
				}
				if (mTimerCrafting >= CRAFT_DURATION) {
					mTimerCrafting = 0;
					for (Player player : players) {
						player.hideBossBar(mCraftingBar);
					}
					spawnDagger();
					craftItem.remove();
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	// Called when Dagger fails to craft.
	public void failCraft() {
		changePhaseNormal();
		clearTargetMobs();
		resetScoreboard();

		mBhairaviAbility.failure();
		mLevynAbility.failure();
		mIzzyAbility.failure();
	}

	// Called when Dagger succeeded in crafting.
	public void spawnDagger() {
		changePhaseDagger();
		clearAllAdds();
		resetScoreboard();

		// Dagger
		Item daggerEntity = mBoss.getWorld().spawn(mDaggerLoc, Item.class);
		daggerEntity.setItemStack(mDagger);
		daggerEntity.setGlowing(true);
		daggerEntity.setUnlimitedLifetime(true);
		daggerEntity.setInvulnerable(true);
		daggerEntity.setCanPlayerPickup(false);

		// Fireworks!!
		Firework firework = mBoss.getWorld().spawn(mDaggerLoc, Firework.class);
		FireworkMeta fm = firework.getFireworkMeta();

		fm.addEffect(FireworkEffect.builder()
			             .with(FireworkEffect.Type.BALL_LARGE)
			             .withColor(Color.RED)
			             .build());

		fm.setPower(0);
		firework.setFireworkMeta(fm);

		mBhairaviAbility.complete();
		mLevynAbility.complete();
		mIzzyAbility.complete();

		new BukkitRunnable() {
			@Override
			public void run() {
				if (daggerEntity.isDead() || !daggerEntity.isValid()) {
					this.cancel();
					return;
				}

				Player player = EntityUtils.getNearestPlayer(daggerEntity.getLocation(), 1);
				if (player != null) {
					Plugin.getInstance().mEffectManager.addEffect(player, DAGGER_EFFECT_SOURCE,
						new SamwellBlackbloodDagger(Constants.TICKS_PER_SECOND * 30));
					daggerEntity.remove();
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		new BukkitRunnable() {
			final int mCurrentPhase = mPhase;
			int mTimer = 0;

			@Override
			public void run() {
				mTimer++;

				if (mCurrentPhase != mPhase || !mDaggerPhase || mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
					return;
				}

				if (mTimer > DAGGER_DURATION) {
					getPlayers().forEach(p -> p.sendMessage(Component.text("As the dagger loses its magic, " +
						"it vaporizes into thin air...", NamedTextColor.AQUA, TextDecoration.ITALIC)));
					clearDagger();
					changePhaseNormal();
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		if (mDefeated) {
			return;
		}
		mDefeated = true;
		World world = mSpawnLoc.getWorld();

		clearAllAdds();
		clearDaggerAndShards();

		List<Player> players = getPlayers();
		if (players.isEmpty()) {
			return;
		}

		BossUtils.endBossFightEffects(mBoss, players, 20 * 25, true, true);

		String[] dio = new String[] {
			"I... I shouldn't have ever left the Valley...",
			"None of this was worth it... None of it...",
			"Listening to you... was the biggest mistake... I ever...",
			"made..."
		};

		for (Player player : players) {
			player.hideBossBar(mCraftingBar);
			player.hideBossBar(mGatheringBar);

			if (Math.abs(player.getLocation().getY() - mSpawnLoc.getY()) > 4
				|| player.getLocation().distance(mSpawnLoc) > 30) {
				// Feeling nice today?
				player.teleport(mSpawnLoc);
			}
		}

		changePhase(SpellManager.EMPTY, mInactivePassives, null);
		mBoss.teleport(mSpawnLoc.clone().add(0, 3, 0));
		if (event != null) {
			event.setCancelled(true);
			event.setReviveHealth(100);
		}

		new BukkitRunnable() {
			double mRadius = 0;
			final Location mLoc = mSpawnLoc.clone();

			@Override
			public void run() {
				mRadius += 1.5;
				for (double degree = 0; degree < 360; degree += 5) {
					double radian = Math.toRadians(degree);
					Location loc = mLoc.clone();
					loc.add(FastUtils.cos(radian) * mRadius, 1, FastUtils.sin(radian) * mRadius);
					new PartialParticle(Particle.CLOUD, loc, 1, 1, 1, 1, 0.35).spawnAsEntityActive(mBoss);
				}
				if (mRadius >= 40) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (mT % Constants.TICKS_PER_SECOND == 0) {
					world.playSound(mSpawnLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 10, 1);
				}
				new PartialParticle(Particle.EXPLOSION_LARGE, mSpawnLoc.clone().add(0, 5, 0)).count(1).delta(10)
					.minimumCount(1).spawnAsEntityActive(mBoss);

				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}

				mT++;
			}
		}.runTaskTimer(mPlugin, 0, 1);

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				sendMessage(Component.text(dio[mT], NamedTextColor.RED).decoration(TextDecoration.ITALIC, mT == 3));
				mT++;
				if (mT == dio.length) {
					this.cancel();
					world.playSound(mSpawnLoc, Sound.ENTITY_VILLAGER_DEATH, SoundCategory.HOSTILE, 5, 1);
					mBoss.setHealth(0);

					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						for (Player player : getPlayers()) {
							com.playmonumenta.plugins.utils.MessagingUtils.sendBoldTitle(player,
								Component.text("VICTORY", NamedTextColor.GREEN),
								Component.text("Samwell, Usurper Of Life", NamedTextColor.DARK_RED));
							player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.HOSTILE, 5, 0.8f);
						}
						mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
						mBoss.remove();
					}, Constants.TICKS_PER_SECOND * 4);
				}
			}
		}.runTaskTimer(mPlugin, 0, Constants.TICKS_PER_SECOND * 3);
	}

	// Called whenever it is the end of a Dagger's construction Phase
	public void clearTargetMobs() {
		// Kill off all mobs which are targetting npcs
		List<LivingEntity> livingEntities = EntityUtils.getNearbyMobs(mSpawnLoc, 50);
		for (LivingEntity e : livingEntities) {
			Set<String> tags = e.getScoreboardTags();
			if (tags.contains(BlueStrikeTargetNPCBoss.identityTag)
				    || tags.contains(BlueStrikeTurretBoss.identityTag)) {
				e.setHealth(0);
			}
		}
	}

	public void clearAllAdds() {
		// Kill all adds
		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				mT++;
				List<LivingEntity> livingEntities = EntityUtils.getNearbyMobs(mSpawnLoc, 50, mBoss);
				for (LivingEntity e : livingEntities) {
					e.setHealth(0);
				}

				if (mT >= 10) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	// Clears Dagger and Shard items from player's inventory + world
	public void clearDaggerAndShards() {
		Collection<Item> itemList = mBoss.getWorld().getEntitiesByClass(Item.class);
		for (Item item : itemList) {
			if (isBossItem(item.getItemStack())) {
				item.remove();
			}
		}

		List<Player> playerList = getPlayers();
		playerList.forEach(p -> Plugin.getInstance().mEffectManager.clearEffects(p, DAGGER_EFFECT_SOURCE));
		for (Player player : playerList) {
			for (ItemStack itemStack : player.getInventory()) {
				if (isBossItem(itemStack)) {
					itemStack.setAmount(0);
				}
			}
			if (isBossItem(player.getItemOnCursor())) {
				player.setItemOnCursor(null);
			}
		}
	}

	// Clears Daggers only.
	public void clearDagger() {
		Collection<Item> itemList = mBoss.getWorld().getEntitiesByClass(Item.class);
		for (Item item : itemList) {
			if (isDaggerItem(item.getItemStack())) {
				item.remove();
			}
		}

		List<Player> playerList = getPlayers();
		playerList.forEach(p -> Plugin.getInstance().mEffectManager.clearEffects(p, DAGGER_EFFECT_SOURCE));
		for (Player player : playerList) {
			for (ItemStack itemStack : player.getInventory()) {
				if (isDaggerItem(itemStack)) {
					itemStack.setAmount(0);
				}
			}
			if (isDaggerItem(player.getItemOnCursor())) {
				player.setItemOnCursor(null);
			}
		}
	}

	public void changePhaseNormal() {
		clearTargetMobs();
		mCraftPhase = false;
		mDaggerPhase = false;

		if (mPhase < 4) {
			List<Player> players = getPlayers();
			refreshGatheringBar();
			for (Player player : players) {
				player.showBossBar(mGatheringBar);
			}
		}

		switch (mPhase) {
			case 1 -> changePhase(mPhase1Actives, mPhase1Passives, null);
			case 2 -> changePhase(mPhase2Actives, mPhase2Passives, null);
			case 3 -> changePhase(mPhase3Actives, mPhase3Passives, null);
			default -> changePhase(mPhase4Actives, mPhase4Passives, null);
		}
	}

	public void changePhaseCraft() {
		mCraftPhase = true;
		mDaggerPhase = false;
		for (Player player : getPlayers()) {
			player.hideBossBar(mGatheringBar);
		}
		switch (mPhase) {
			case 1 -> changePhase(mPhase1Actives, mCraft1Passives, null);
			case 2 -> changePhase(mPhase2Actives, mCraft2Passives, null);
			case 3 -> changePhase(mPhase3Actives, mCraft3Passives, null);
			default -> changePhase(mPhase4Actives, mPhase4Passives, null); // Phase 4 Craft doesn't exist.
		}
	}

	public void changePhaseDagger() {
		mCraftPhase = false;
		mDaggerPhase = true;
		switch (mPhase) {
			case 1 -> changePhase(mPhase1Actives, mDagger1Passives, null);
			case 2 -> changePhase(mPhase2Actives, mDagger2Passives, null);
			case 3 -> changePhase(mPhase3Actives, mDagger3Passives, null);
			default -> changePhase(mPhase4Actives, mPhase4Passives, null); // Phase 4 Dagger doesn't exist.
		}
	}

	// Get number of shards obtained.
	public int getShards() {
		return ScoreboardUtils.getScoreboardValue(mBoss, OBJECTIVE_SHARD_NAME).orElse(0);
	}

	public void addShards(int amount) {
		ScoreboardUtils.setScoreboardValue(mBoss, OBJECTIVE_SHARD_NAME, getShards() + amount);
		refreshGatheringBar();
	}

	private void refreshGatheringBar() {
		mGatheringBar.progress((float) getShards() / mShardsReq);
		mGatheringBar.name(Component.text("Shards Obtained: ", NamedTextColor.YELLOW)
			.append(Component.text(getShards(), NamedTextColor.GREEN))
			.append(Component.text(" / ", NamedTextColor.YELLOW))
			.append(Component.text(mShardsReq, NamedTextColor.RED)));
	}

	public int getFails() {
		return ScoreboardUtils.getScoreboardValue(mBoss, OBJECTIVE_FAILS_NAME).orElse(0);
	}

	public void addFail() {
		ScoreboardUtils.setScoreboardValue(mBoss, OBJECTIVE_FAILS_NAME, getFails() + 1);
	}

	public boolean isBossItem(ItemStack itemStack) {
		return (itemStack != null) && (ItemUtils.getPlainName(itemStack).equals(ItemUtils.getPlainName(mDagger)) || itemStack.isSimilar(mShards));
	}

	public boolean isDaggerItem(ItemStack itemStack) {
		return (itemStack != null) && ItemUtils.getPlainName(itemStack).equals(ItemUtils.getPlainName(mDagger));
	}

	public void resetScoreboard() {
		ScoreboardUtils.setScoreboardValue(mBoss, OBJECTIVE_SHARD_NAME, 0);
		ScoreboardUtils.setScoreboardValue(mBoss, OBJECTIVE_FAILS_NAME, 0);
	}

	public List<Player> getPlayers() {
		return PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
	}

	private void sendMessage(String msg) {
		sendMessage(Component.text(msg, NamedTextColor.RED));
	}

	private void sendMessage(Component msg) {
		for (Player player : getPlayers()) {
			MessagingUtils.sendNPCMessage(player, "Samwell", msg);
		}
	}
}
