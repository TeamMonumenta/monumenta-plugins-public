package com.playmonumenta.plugins.bosses.bosses.bluestrike;

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
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

public class Samwell extends BossAbilityGroup {
	public static final String identityTag = "boss_samwell";
	public static final String DAGGER_EFFECT_SOURCE = "SamwellBlackbloodDagger";
	public static final String OBJECTIVE_SHARD_NAME = "temp";
	public static final String OBJECTIVE_FAILS_NAME = "TempA";
	private static final int CRAFT_DURATION = 25 * 20; // Ticks taken to craft dagger (25 secs)
	private static final int DAGGER_DURATION = 30 * 20; // Time limit to hit with dagger (30 secs)

	private static final int detectionRange = 100;
	public final Location mSpawnLoc;
	public final Location mDaggerLoc;
	public final Location mEndLoc;
	public int mPhase = 1;
	public boolean mCraftPhase = false;
	public boolean mDaggerPhase = false;
	public BossBar mCraftingBar;
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

	private final Location mBhrahaviLoc;
	private final Location mIzzyLoc;
	private final Location mLevynLoc;
	private Villager mBhairavi;
	private Villager mIzzy;
	private Villager mLevyn;
	private BlueStrikeDaggerCraftingBoss mBhairaviAbility;
	private BlueStrikeDaggerCraftingBoss mIzzyAbility;
	private BlueStrikeDaggerCraftingBoss mLevynAbility;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new Samwell(plugin, boss, spawnLoc, endLoc);
		});
	}

	public Samwell(Plugin plugin, LivingEntity boss, Location startLoc, Location endLoc) {
		super(plugin, identityTag, boss);
		mSpawnLoc = startLoc;
		mEndLoc = endLoc;
		HashMap<Double, LoSPool> weights = new HashMap<>();
		weights.put(0.2, LoSPool.fromString("~BlueMaskedElite"));
		weights.put(0.8, LoSPool.fromString("~BlueMaskedNormal"));

		mBhrahaviLoc = mSpawnLoc.clone().add(15, 1, 0);
		mIzzyLoc = mSpawnLoc.clone().add(-8, 1, 12);
		mLevynLoc = mSpawnLoc.clone().add(-8, 1, -12);
		mDaggerLoc = mSpawnLoc.clone().add(0, 8, 0);

		mDagger = InventoryUtils.getItemFromLootTable(mSpawnLoc, NamespacedKey.fromString("epic:r3/dungeons/bluestrike/boss/blackblood_dagger"));
		mShards = InventoryUtils.getItemFromLootTable(mSpawnLoc, NamespacedKey.fromString("epic:r3/dungeons/bluestrike/boss/blackblood_shard"));

		mBasePassives = Arrays.asList(
			new SpellBlockBreak(mBoss),
			new SpellConditionalTeleport(boss, startLoc, b -> {
				// Boss isn't stuck in lava or bedrock
				boolean condition1 = b.getLocation().getBlock().getType() == Material.BEDROCK ||
					b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK ||
					b.getLocation().getBlock().getType() == Material.LAVA;

				// Boss isn't too far off arena.
				boolean condition2 = (boss.getLocation().distance(startLoc) > 30) || Math.abs(boss.getLocation().getY() - startLoc.getY()) > 5;

				return condition1 || condition2;
			}),
			new SpellDominion(plugin, boss, mSpawnLoc),
			new SpellTargetVisiblePlayer((Mob) boss, detectionRange, 60, 160),
			new SpellRunAction(() -> {
				if (boss.hasPotionEffect(PotionEffectType.GLOWING)) {
					boss.removePotionEffect(PotionEffectType.GLOWING);
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
		mPhase4Passives = new ArrayList<>(Arrays.asList(
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
		mDagger1Passives = new ArrayList<>(Arrays.asList(
			new SpellSamwellRegeneration(mBoss, this, 1)
		));
		mDagger2Passives = new ArrayList<>(Arrays.asList(
			new SpellSamwellRegeneration(mBoss, this, 3.0 / 4.0)
		));
		mDagger3Passives = new ArrayList<>(Arrays.asList(
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
			new SpellDeathSweep(plugin, mBoss, 1),
			new SpellSamwellSmokeBomb(plugin, mBoss, 1),
			new SpellRealitySlash(plugin, mBoss, this, 1)
		));
		mPhase2Actives = new SpellManager(Arrays.asList(
			new SpellDeathSweep(plugin, mBoss, 2),
			new SpellSamwellSmokeBomb(plugin, mBoss, 2),
			new SpellRealitySlash(plugin, mBoss, this, 2),
			new SpellCrystalBarrage(plugin, mBoss, this, 2)
		));
		mPhase3Actives = new SpellManager(Arrays.asList(
			new SpellDeathSweep(plugin, mBoss, 3),
			new SpellSamwellSmokeBomb(plugin, mBoss, 3),
			new SpellCrystalBarrage(plugin, mBoss, this, 3),
			new SpellRealitySlash(plugin, mBoss, this, 3),
			new SpellSummonLavaTitan(plugin, mBoss, mSpawnLoc, 3)
		));
		mPhase4Actives = new SpellManager(Arrays.asList(
			new SpellDeathSweep(plugin, mBoss, 4),
			new SpellSamwellSmokeBomb(plugin, mBoss, 4),
			new SpellCrystalBarrage(plugin, mBoss, this, 4),
			new SpellRealitySlash(plugin, mBoss, this, 4),
			new SpellSummonLavaTitan(plugin, mBoss, mSpawnLoc, 4)
		));
		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.BLUE, BarStyle.SEGMENTED_10, null, false);

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, bossBar);
	}

	@Override
	public void init() {
		mBoss.setAI(false);
		mCraftingBar = Bukkit.getServer().createBossBar(ChatColor.YELLOW + "" + ChatColor.BOLD + "Crafting...", BarColor.YELLOW, BarStyle.SEGMENTED_6);
		mCraftingBar.setProgress(0);

		// Going to be using Scoreboards for this, easier to track between functions (I hope)
		resetScoreboard();

		int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
		int hpDelta = 8000;
		double finalHp = hpDelta * BossUtils.healthScalingCoef(playerCount, 0.5, 0.4);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, finalHp);
		mBoss.setHealth(finalHp);

		Team team = ScoreboardUtils.getExistingTeamOrCreate("Blue", NamedTextColor.BLUE);
		team.addEntity(mBoss);
		mBoss.setGlowing(true);

		mShardsReq = (int) (5 + Math.floor(PlayerUtils.playersInRange(mSpawnLoc, 100, true).size() / 2.0));

		// Need to Delay this a bit, as BlueStrikeDaggerCraftingBoss will search for a nearby WitherSkeleton.
		// (IK it is quite scuffed)
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mSpawnLoc.getNearbyEntitiesByType(Villager.class, 30).forEach(Entity::remove);
			mBhairavi = mSpawnLoc.getWorld().spawn(mBhrahaviLoc, Villager.class);
			mBhairavi.customName(Component.text("Bhairavi"));
			mIzzy = mSpawnLoc.getWorld().spawn(mIzzyLoc, Villager.class);
			mIzzy.customName(Component.text("Izzy"));
			mLevyn = mSpawnLoc.getWorld().spawn(mLevynLoc, Villager.class);
			mLevyn.customName(Component.text("Levyn"));

			mBhairaviAbility = new BlueStrikeDaggerCraftingBoss(mPlugin, mBhairavi);
			mIzzyAbility = new BlueStrikeDaggerCraftingBoss(mPlugin, mIzzy);
			mLevynAbility = new BlueStrikeDaggerCraftingBoss(mPlugin, mLevyn);

			BossManager.getInstance().manuallyRegisterBoss(mBhairavi, mBhairaviAbility);
			BossManager.getInstance().manuallyRegisterBoss(mIzzy, mIzzyAbility);
			BossManager.getInstance().manuallyRegisterBoss(mLevyn, mLevynAbility);

			mSpawnLoc.getNearbyPlayers(100).stream().forEach(p -> MessagingUtils.sendNPCMessage(p, "Samwell", Component.text("Well, I wasn't sure if you guys would make it this far. How do you like my new place?", NamedTextColor.RED)));

			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				mSpawnLoc.getNearbyPlayers(100).stream().forEach(p -> MessagingUtils.sendNPCMessage(p, "Samwell", Component.text("It'll be the last thing you see... I've got the Blue Wool on my side!", NamedTextColor.RED)));

				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					mBoss.setAI(true);
					changePhaseNormal();
					mSpawnLoc.getNearbyPlayers(100).stream().forEach(p -> {
						com.playmonumenta.plugins.utils.MessagingUtils.sendBoldTitle(p, ChatColor.DARK_RED + "Samwell", ChatColor.RED + "Usurper Of Life");
						p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 2, true, false, false));
					});
					mSpawnLoc.getWorld().playSound(mSpawnLoc, Sound.ENTITY_WITHER_SPAWN, 5, 0.7f);
				}, 10);
			}, 70);
		}, 10);
	}

	@Override
	public void onHurtByEntity(DamageEvent event, Entity damager) {
		if (damager instanceof Player player && event.getType() == DamageEvent.DamageType.MELEE) {
			if (((Plugin) mPlugin).mEffectManager.hasEffect(player, DAGGER_EFFECT_SOURCE) && mPhase <= 3) {
				switch (mPhase) {
					case 1 -> {
						mBoss.setHealth(EntityUtils.getMaxHealth(mBoss) * (3.0 / 4.0));
						mSpawnLoc.getNearbyPlayers(100).stream().forEach(p -> MessagingUtils.sendNPCMessage(p, "Samwell", Component.text("Ugh, what! How did you do that? It... burns!", NamedTextColor.RED)));
						mPhase = 2;
						changePhaseNormal();
						Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
							forceCastSpell(SpellCrystalBarrage.class);
						}, 40);
					}
					case 2 -> {
						mBoss.setHealth(EntityUtils.getMaxHealth(mBoss) * (2.0 / 4.0));
						mSpawnLoc.getNearbyPlayers(100).stream().forEach(p -> MessagingUtils.sendNPCMessage(p, "Samwell", Component.text("No, what?! Why can't it heal? What are you using?", NamedTextColor.RED)));
						mPhase = 3;
						changePhaseNormal();
						forceCastSpell(SpellSummonLavaTitan.class);
					}
					default -> {
						mBoss.setHealth(EntityUtils.getMaxHealth(mBoss) * (1.0 / 4.0));
						mSpawnLoc.getNearbyPlayers(100).stream().forEach(p -> MessagingUtils.sendNPCMessage(p, "Samwell", Component.text("I'm done with you! That dagger is too much. I don't get why Blue can't heal from it? Is it another wool?!", NamedTextColor.RED)));
						mPhase = 4;
						Team blackTeam = ScoreboardUtils.getExistingTeamOrCreate("Black", NamedTextColor.BLACK);
						blackTeam.addEntry(mBoss.getUniqueId().toString());
						changePhaseNormal();
					}
				}
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ITEM_BREAK, 5f, 0.5f);
				MovementUtils.knockAway(player, mBoss, 3.0f, 0.5f);
				clearDaggerAndShards();
				return;
			}
		}

		if (mPhase != 4) {
			// Lower damage so game breaking builds can't realistically ruin this
			event.setDamage(event.getDamage() * 0.1);
		} else if (!mPhase4Damaged) {
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				mSpawnLoc.getNearbyPlayers(100).stream().forEach(p -> MessagingUtils.sendNPCMessage(p, "Samwell", Component.text("It can't be... I can't feel the Blue Wool anymore... Has it run out? Or... has it turned its back on me??", NamedTextColor.RED)));

				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					mSpawnLoc.getNearbyPlayers(100).stream().forEach(p -> MessagingUtils.sendNPCMessage(p, "Samwell", "&c&oPlease&c! &4&lHarrakfar&c! Heal me!"));
					mSpawnLoc.getWorld().playSound(mSpawnLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 10, 1.2f);
				}, 70);
			}, 20);
			mPhase4Damaged = true;
		}
	}

	public void startCraftPhase() {
		changePhaseCraft();

		mCraftingBar.setProgress(0);
		resetScoreboard();
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), 75, true);
		mCraftingBar.setVisible(true);

		for (Player player : players) {
			mCraftingBar.addPlayer(player);
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
				mCraftingBar.setProgress((double) mTimerCrafting / CRAFT_DURATION);
				if (!mCraftPhase) {
					mTimerCrafting = 0;
					mCraftingBar.setVisible(false);
					failCraft();
					craftItem.remove();
					this.cancel();
					return;
				}
				if (mTimerCrafting >= CRAFT_DURATION) {
					mTimerCrafting = 0;
					mCraftingBar.setVisible(false);
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
			@Override public void run() {
				if (daggerEntity.isDead() || !daggerEntity.isValid()) {
					this.cancel();
					return;
				}

				Player player = EntityUtils.getNearestPlayer(daggerEntity.getLocation(), 1);
				if (player != null) {
					((Plugin) mPlugin).mEffectManager.addEffect(player, DAGGER_EFFECT_SOURCE, new SamwellBlackbloodDagger(30 * 20));
					daggerEntity.remove();
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		new BukkitRunnable() {
			int mCurrentPhase = mPhase;
			int mTimer = 0;

			@Override
			public void run() {
				mTimer++;

				if (mCurrentPhase != mPhase || !mDaggerPhase || mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
					return;
				}

				if (mTimer > DAGGER_DURATION) {
					List<Player> players = EntityUtils.getNearestPlayers(mBoss.getLocation(), 100);
					players.forEach(p -> p.sendMessage(ChatColor.AQUA + "" + ChatColor.ITALIC + "As the dagger loses its magic, it vaporizes into thin air..."));
					clearDagger();
					changePhaseNormal();
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public void death(EntityDeathEvent event) {
		if (mDefeated) {
			return;
		}
		mDefeated = true;
		World world = mSpawnLoc.getWorld();

		clearAllAdds();
		clearDaggerAndShards();

		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
		if (players.size() <= 0) {
			return;
		}

		String[] dio = new String[]{
			"I... I shouldn't have ever left the Valley...",
			"None of this was worth it... None of it...",
			"Listening to you... was the biggest mistake... I ever...",
			"&omade..."
		};

		for (Player player : players) {
			player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
			player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 40, 10));
			player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 40, 1));

			if (Math.abs(player.getLocation().getY() - mSpawnLoc.getY()) > 4
				|| player.getLocation().distance(mSpawnLoc) > 30) {
				// Feeling nice today?
				player.teleport(mSpawnLoc);
			}
		}

		changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
		mBoss.setHealth(100);
		mBoss.setInvulnerable(true);
		mBoss.setAI(false);
		mBoss.setGravity(false);
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 1000, 10));
		mBoss.removePotionEffect(PotionEffectType.GLOWING);
		mBoss.teleport(mSpawnLoc.clone().add(0, 3, 0));
		event.setCancelled(true);
		event.setReviveHealth(100);

		new BukkitRunnable() {
			double mRadius = 0;
			Location mLoc = mSpawnLoc.clone();

			@Override
			public void run() {
				mRadius += 1.5;
				for (double degree = 0; degree < 360; degree += 5) {
					double radian = Math.toRadians(degree);
					Location loc = mLoc.clone();
					loc.add(FastUtils.cos(radian) * mRadius, 1, FastUtils.sin(radian) * mRadius);
					world.spawnParticle(Particle.CLOUD, loc, 1, 1, 1, 1, 0.35);
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
				if (mT % 20 == 0) {
					world.playSound(mSpawnLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 10, 1);
				}
				world.spawnParticle(Particle.EXPLOSION_LARGE, mSpawnLoc.clone().add(0, 5, 0), 1, 10, 10, 10);

				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}

				mT++;
			}
		}.runTaskTimer(mPlugin, 0, 1);

		new BukkitRunnable() {
			int mT = 0;

			@Override public void run() {
				players.forEach(p -> MessagingUtils.sendNPCMessage(p, "Samwell", ChatColor.RED + dio[mT]));
				mT++;
				if (mT == dio.length) {
					this.cancel();
					world.playSound(mSpawnLoc, Sound.ENTITY_VILLAGER_DEATH, SoundCategory.HOSTILE, 5, 1);
					mBoss.setHealth(0);

					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
							com.playmonumenta.plugins.utils.MessagingUtils.sendBoldTitle(player, ChatColor.GREEN + "VICTORY", ChatColor.DARK_RED + "Samwell, Usurper Of Life");
							player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 100, 0.8f);
							player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
							player.removePotionEffect(PotionEffectType.REGENERATION);
						}
						mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
						mBoss.remove();
					}, 20 * 4);
				}
			}
		}.runTaskTimer(mPlugin, 0, 20 * 3);
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

			@Override public void run() {
				mT++;
				List<LivingEntity> livingEntities = EntityUtils.getNearbyMobs(mSpawnLoc, 50);
				for (LivingEntity e : livingEntities) {
					if (EntityUtils.isHostileMob(e)
						&& e != mBoss) {
						e.setHealth(0);
					}
				}

				if (mT >= 10) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	// Clears Dagger and Shard items from player's inventory + world
	public void clearDaggerAndShards() {
		List<Item> itemList = (List<Item>) mBoss.getWorld().getEntitiesByClass(Item.class);
		for (Item item : itemList) {
			if (isBossItem(item.getItemStack())) {
				item.remove();
			}
		}

		List<Player> playerList = EntityUtils.getNearestPlayers(mBoss.getLocation(), 100);
		playerList.forEach(p -> ((Plugin) mPlugin).mEffectManager.clearEffects(p, DAGGER_EFFECT_SOURCE));
		for (Player player : playerList) {
			for (ItemStack itemStack : player.getInventory()) {
				if (isBossItem(itemStack)) {
					itemStack.setAmount(0);
				}
			}
		}
	}

	// Clears Daggers only.
	public void clearDagger() {
		List<Item> itemList = (List<Item>) mBoss.getWorld().getEntitiesByClass(Item.class);
		for (Item item : itemList) {
			if (isDaggerItem(item.getItemStack())) {
				item.remove();
			}
		}

		List<Player> playerList = EntityUtils.getNearestPlayers(mBoss.getLocation(), 100);
		playerList.forEach(p -> ((Plugin) mPlugin).mEffectManager.clearEffects(p, DAGGER_EFFECT_SOURCE));
		for (Player player : playerList) {
			for (ItemStack itemStack : player.getInventory()) {
				if (isDaggerItem(itemStack)) {
					itemStack.setAmount(0);
				}
			}
		}
	}

	public void changePhaseNormal() {
		clearTargetMobs();
		mCraftPhase = false;
		mDaggerPhase = false;
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
		return ScoreboardUtils.getScoreboardValue(mBoss, Samwell.OBJECTIVE_SHARD_NAME).orElse(0);
	}

	public void addShards(int amount) {
		ScoreboardUtils.setScoreboardValue(mBoss, Samwell.OBJECTIVE_SHARD_NAME, getShards() + amount);
	}

	public int getFails() {
		return ScoreboardUtils.getScoreboardValue(mBoss, Samwell.OBJECTIVE_FAILS_NAME).orElse(0);
	}

	public void addFail() {
		ScoreboardUtils.setScoreboardValue(mBoss, Samwell.OBJECTIVE_FAILS_NAME, getFails() + 1);
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
}
