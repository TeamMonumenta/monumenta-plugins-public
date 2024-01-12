package com.playmonumenta.plugins.depths.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.SerializedLocationBossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.bosses.spells.vesperidys.SpellBreakPlatform;
import com.playmonumenta.plugins.depths.bosses.spells.vesperidys.SpellPlatformWave;
import com.playmonumenta.plugins.depths.bosses.spells.vesperidys.SpellSeekingEyes;
import com.playmonumenta.plugins.depths.bosses.spells.vesperidys.SpellSoulLink;
import com.playmonumenta.plugins.depths.bosses.spells.vesperidys.SpellStarProjectiles;
import com.playmonumenta.plugins.depths.bosses.spells.vesperidys.SpellStarStorm;
import com.playmonumenta.plugins.depths.bosses.spells.vesperidys.SpellVesperidysAnticheese;
import com.playmonumenta.plugins.depths.bosses.spells.vesperidys.SpellVesperidysAutoAttack;
import com.playmonumenta.plugins.depths.bosses.spells.vesperidys.SpellVesperidysDarkHole;
import com.playmonumenta.plugins.depths.bosses.spells.vesperidys.SpellVesperidysFeintParticleBeam;
import com.playmonumenta.plugins.depths.bosses.spells.vesperidys.SpellVesperidysSummonAdds;
import com.playmonumenta.plugins.depths.bosses.spells.vesperidys.SpellVesperidysTeleport;
import com.playmonumenta.plugins.depths.bosses.vesperidys.VesperidysVoidCrystalEarth;
import com.playmonumenta.plugins.effects.VoidCorruption;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.scriptedquests.managers.SongManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
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
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Light;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BoundingBox;

public class Vesperidys extends SerializedLocationBossAbilityGroup {
	public static final String identityTag = "boss_vesperidys";
	public static final String mobTag = "DD2BossFight3";

	public static final String bossName = "Vesperidys";
	public static final String MUSIC_TITLE = "epic:music.vesperidys_phase1";
	public static final double MUSIC_DURATION = 6 * 60 + 4; // seconds
	public static final String MUSIC_TITLE_2 = "epic:music.vesperidys_phase2";
	public static final double MUSIC_DURATION_2 = 5 * 60 + 56; // seconds

	public static final int detectionRange = 100;
	public static final int REALITY_DISTORTION_TICKS = 3 * 20;

	public static final double BOSS_HEALTH = 17500;
	// Top block y level of the platforms to clone from
	public static final int CLONE_PLATFORMS_Y_DIFFERENCE = -17;

	public int mPhase = 0;

	public boolean mInvincible = false;
	public List<Player> mInvinicibleWarned = new ArrayList<>();
	public List<ArmorStand> mEyes = new ArrayList<>();
	public List<BukkitRunnable> mEyesRunnable = new ArrayList<>();
	public final Team mCastTeam;

	private final Plugin mMonuPlugin;
	public final PlatformList mPlatformList;
	public final LivingEntity mBossTwo;

	public boolean mFullPlatforms = true;

	private final List<Spell> mBasePassives;
	private final List<Spell> mPhase1Passives;
	SpellManager mPhase1Actives;
	private final List<Spell> mPhase2Passives;
	SpellManager mPhase2Actives;
	private final List<Spell> mPhase3Passives;
	SpellManager mPhase3Actives;
	private final List<Spell> mPhase4Passives;
	SpellManager mPhase4Actives;
	private final List<Spell> mPhase5Passives;
	SpellManager mPhase5Actives;
	SpellManager mDarkHoleActive;

	public final @Nullable DepthsParty mParty;

	public SpellVesperidysDarkHole mDarkHole;
	public SpellVesperidysTeleport mTeleportSpell;
	public SpellVesperidysAnticheese mAnticheese;
	public SpellVesperidysAutoAttack mAutoAttack;

	public static final double ARMOR_STAND_BLOCK_OFFSET = -1.6875;
	public static final double ARMOR_STAND_PARTICLES_OFFSET = 1.8;
	public static final double[] EYE_ORDER = {0, 180, 36, 144, 72, 108};

	public boolean mDefeated = false;

	public int mSpellCooldowns = 15 * 20;

	public Vesperidys(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);
		mBossTwo = boss;
		mMonuPlugin = plugin;
		mParty = DepthsUtils.getPartyFromNearbyPlayers(mSpawnLoc);
		mPlatformList = new PlatformList();
		mCastTeam = createTeams();

		if (mParty != null && mParty.getAscension() >= 15) {
			mSpellCooldowns -= 4 * 20;
		} else if (mParty != null && mParty.getAscension() >= 8) {
			mSpellCooldowns -= 2 * 20;
		}

		int numCrystals = 0;
		if (mParty != null && mParty.getAscension() >= 15) {
			if (mParty.getPlayers().size() > 2) {
				numCrystals = 3;
			} else {
				numCrystals = 2;
			}
		} else if (mParty != null && mParty.getAscension() >= 4) {
			if (mParty.getPlayers().size() > 2) {
				numCrystals = 2;
			} else {
				numCrystals = 1;
			}
		}

		for (int i = -2; i <= 2; i++) {
			for (int j = -2; j <= 2; j++) {
				Location loc = mSpawnLoc.clone().add(i * 7, -1, j * 7);
				Platform platform = new Platform(loc, i, j);
				platform.generateInstantFull();
				mPlatformList.setPlatform(i, j, platform);
			}
		}

		mDarkHole = new SpellVesperidysDarkHole(mMonuPlugin, boss, this);
		mTeleportSpell = new SpellVesperidysTeleport(mMonuPlugin, boss, this, 2000);
		mAnticheese = new SpellVesperidysAnticheese(mMonuPlugin, boss, spawnLoc, this);
		mAutoAttack = new SpellVesperidysAutoAttack(plugin, boss, this, detectionRange, 4, 3, 2 * 20, 3 * 20, 2 * 20, 10 * 20);

		mBasePassives = Arrays.asList(
			mAnticheese,
			mTeleportSpell,
			new SpellVesperidysSummonAdds(plugin, boss, this, 30 * 4, 60 * 4, numCrystals)
		);

		mDarkHoleActive = new SpellManager(Arrays.asList(
			mDarkHole
		));

		mPhase1Passives = new ArrayList<>(Arrays.asList(
			mAutoAttack
		));

		List<Spell> phase1ActivesList = new ArrayList<>(Arrays.asList(
			new SpellStarStorm(plugin, boss, this),
			new SpellStarProjectiles(plugin, boss, this)
		));

		mPhase2Passives = new ArrayList<>(Arrays.asList(
			mAutoAttack
		));

		List<Spell> phase2ActivesList = new ArrayList<>(Arrays.asList(
			new SpellStarStorm(plugin, boss, this),
			new SpellStarProjectiles(plugin, boss, this)
		));

		mPhase3Passives = new ArrayList<>(Arrays.asList(
			mAutoAttack
		));

		List<Spell> phase3ActivesList = new ArrayList<>(Arrays.asList(
			new SpellStarStorm(plugin, boss, this),
			new SpellStarProjectiles(plugin, boss, this),
			new SpellSeekingEyes(plugin, boss, this)
		));

		mPhase4Passives = new ArrayList<>(Arrays.asList(
			mAutoAttack
		));

		List<Spell> phase4ActivesList = new ArrayList<>(Arrays.asList(
			new SpellStarStorm(plugin, boss, this),
			new SpellStarProjectiles(plugin, boss, this),
			new SpellSeekingEyes(plugin, boss, this)
		));

		mPhase5Passives = new ArrayList<>(Arrays.asList(
			mAutoAttack
		));

		List<Spell> phase5ActivesList = new ArrayList<>(Arrays.asList(
			new SpellStarStorm(plugin, boss, this),
			new SpellStarProjectiles(plugin, boss, this),
			new SpellSeekingEyes(plugin, boss, this)
		));

		mPhase1Passives.addAll(mBasePassives);
		mPhase2Passives.addAll(mBasePassives);
		mPhase3Passives.addAll(mBasePassives);
		mPhase4Passives.addAll(mBasePassives);
		mPhase5Passives.addAll(mBasePassives);

		if (mParty != null && mParty.getAscension() >= 15) {
			phase1ActivesList.addAll(List.of(
				new SpellPlatformWave(plugin, boss, this, 2, 25),
				new SpellBreakPlatform(mMonuPlugin, boss, this, 2 * 20, 10),
				new SpellVesperidysFeintParticleBeam(plugin, boss, this, 7, 15, 4)
			));
			phase2ActivesList.addAll(List.of(
				new SpellPlatformWave(plugin, boss, this, 2, 25),
				new SpellBreakPlatform(mMonuPlugin, boss, this, 2 * 20, 6),
				new SpellVesperidysFeintParticleBeam(plugin, boss, this, 7, 15, 4)
			));
			phase3ActivesList.addAll(List.of(
				new SpellPlatformWave(plugin, boss, this, 3, 20),
				new SpellBreakPlatform(mMonuPlugin, boss, this, 2 * 20, 10),
				new SpellVesperidysFeintParticleBeam(plugin, boss, this, 8, 12, 4)
			));
			phase4ActivesList.addAll(List.of(
				new SpellPlatformWave(plugin, boss, this, 3, 20),
				new SpellBreakPlatform(mMonuPlugin, boss, this, 2 * 20, 3),
				new SpellVesperidysFeintParticleBeam(plugin, boss, this, 8, 12, 4)
			));
			phase5ActivesList.addAll(List.of(
				new SpellPlatformWave(plugin, boss, this, 3, 20),
				new SpellBreakPlatform(mMonuPlugin, boss, this, 2 * 20, 3),
				new SpellVesperidysFeintParticleBeam(plugin, boss, this, 8, 12, 4)
			));
		} else if (mParty != null && mParty.getAscension() >= 8) {
			phase1ActivesList.addAll(List.of(
				new SpellPlatformWave(plugin, boss, this, 2, 30),
				new SpellBreakPlatform(mMonuPlugin, boss, this, 3 * 20, 8),
				new SpellVesperidysFeintParticleBeam(plugin, boss, this, 6, 20, 3)
			));
			phase2ActivesList.addAll(List.of(
				new SpellPlatformWave(plugin, boss, this, 2, 30),
				new SpellBreakPlatform(mMonuPlugin, boss, this, 3 * 20, 5),
				new SpellVesperidysFeintParticleBeam(plugin, boss, this, 6, 20, 3)
			));
			phase3ActivesList.addAll(List.of(
				new SpellPlatformWave(plugin, boss, this, 2, 30),
				new SpellBreakPlatform(mMonuPlugin, boss, this, 3 * 20, 8),
				new SpellVesperidysFeintParticleBeam(plugin, boss, this, 7, 15, 3)
			));
			phase4ActivesList.addAll(List.of(
				new SpellPlatformWave(plugin, boss, this, 2, 30),
				new SpellBreakPlatform(mMonuPlugin, boss, this, 3 * 20, 2),
				new SpellVesperidysFeintParticleBeam(plugin, boss, this, 7, 15, 3)
			));
			phase5ActivesList.addAll(List.of(
				new SpellPlatformWave(plugin, boss, this, 2, 30),
				new SpellBreakPlatform(mMonuPlugin, boss, this, 3 * 20, 2),
				new SpellVesperidysFeintParticleBeam(plugin, boss, this, 7, 15, 3)
			));
		} else {
			phase1ActivesList.addAll(List.of(
				new SpellPlatformWave(plugin, boss, this, 2, 40),
				new SpellBreakPlatform(mMonuPlugin, boss, this, 5 * 20, 4),
				new SpellVesperidysFeintParticleBeam(plugin, boss, this, 5, 30, 2)
				));
			phase2ActivesList.addAll(List.of(
				new SpellPlatformWave(plugin, boss, this, 2, 40),
				new SpellBreakPlatform(mMonuPlugin, boss, this, 5 * 20, 4),
				new SpellVesperidysFeintParticleBeam(plugin, boss, this, 5, 30, 2)
				));
			phase3ActivesList.addAll(List.of(
				new SpellPlatformWave(plugin, boss, this, 2, 40),
				new SpellBreakPlatform(mMonuPlugin, boss, this, 5 * 20, 6),
				new SpellVesperidysFeintParticleBeam(plugin, boss, this, 6, 20, 2)
			));
			phase4ActivesList.addAll(List.of(
				new SpellPlatformWave(plugin, boss, this, 2, 40),
				new SpellBreakPlatform(mMonuPlugin, boss, this, 5 * 20, 2),
				new SpellVesperidysFeintParticleBeam(plugin, boss, this, 6, 20, 2)
			));
			phase5ActivesList.addAll(List.of(
				new SpellPlatformWave(plugin, boss, this, 2, 40),
				new SpellBreakPlatform(mMonuPlugin, boss, this, 5 * 20, 2),
				new SpellVesperidysFeintParticleBeam(plugin, boss, this, 6, 20, 2)
			));
		}

		if (mParty != null && mParty.getAscension() >= 4) {
			phase1ActivesList.addAll(List.of(
				new SpellSeekingEyes(plugin, boss, this)
			));
			phase2ActivesList.addAll(List.of(
				new SpellSeekingEyes(plugin, boss, this)
			));
		}

		if (mParty != null && mParty.getAscension() >= 12) {
			phase3ActivesList.addAll(List.of(
				new SpellSoulLink(plugin, boss, this)
			));
			phase4ActivesList.addAll(List.of(
				new SpellSoulLink(plugin, boss, this)
			));
			phase5ActivesList.addAll(List.of(
				new SpellSoulLink(plugin, boss, this)
			));
		}

		mPhase1Actives = new SpellManager(phase1ActivesList);
		mPhase2Actives = new SpellManager(phase2ActivesList);
		mPhase3Actives = new SpellManager(phase3ActivesList);
		mPhase4Actives = new SpellManager(phase4ActivesList);
		mPhase5Actives = new SpellManager(phase5ActivesList);

		Map<Integer, BossBarManager.BossHealthAction> events = new HashMap<>();
		events.put(90, (mBoss) -> {
			changePhase(mDarkHoleActive, mBasePassives, null);
			forceCastSpell(SpellVesperidysDarkHole.class);

			TextComponent[] dio = new TextComponent[]{
				obfuscate("You have no place among the stars.", 0, NamedTextColor.DARK_AQUA),
				obfuscate("You have no place upon the dirt.", 0, NamedTextColor.DARK_AQUA),
				obfuscate("How DARE you separate us from the Ventricles!", 0, NamedTextColor.DARK_AQUA),
				obfuscate("HOW DARE YOU SEPARATE US!!", 0, NamedTextColor.DARK_AQUA).decorate(TextDecoration.BOLD)
			};
			int[] dioDelay = {0, 3 * 20, 6 * 20, 9 * 20};
			int[] sounds = {2, 3, 5, 6};

			List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
			for (int i = 0; i < dio.length; i++) {
				TextComponent dialogue = dio[i];
				int finalI = i;
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					players.forEach(p -> com.playmonumenta.scriptedquests.utils.MessagingUtils.sendNPCMessage(p, "The Vesperidys", dialogue));
					voiceOfVesperidys(sounds[finalI]);
				}, dioDelay[i]);
			}
		});
		events.put(75, (mBoss) -> {
			for (Platform platform : mPlatformList.getAllPlatforms()) {
				platform.generateInstantFull();
			}
			mSpellCooldowns -= 10;
			mPhase = 2;
			realityDistortion(false);

			TextComponent[] dio = new TextComponent[]{
				obfuscate("Still you try. Oh how foolish.", 0, NamedTextColor.DARK_AQUA),
				obfuscate("We are beyond reckoning. You should know this truth.", 0, NamedTextColor.DARK_AQUA),
				obfuscate("There is so much of us you still cannot hope to defeat.", 0, NamedTextColor.DARK_AQUA),
				};
			int[] dioDelay = {0, 3 * 20, 6 * 20};
			int[] sounds = {0, 4, 5};
			List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
			for (int i = 0; i < dio.length; i++) {
				TextComponent dialogue = dio[i];
				int finalI = i;
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					players.forEach(p -> com.playmonumenta.scriptedquests.utils.MessagingUtils.sendNPCMessage(p, "The Vesperidys", dialogue));
					voiceOfVesperidys(sounds[finalI]);
				}, dioDelay[i]);
			}

			changePhase(mDarkHoleActive, mPhase2Passives, null);
		});
		events.put(50, (mBoss) -> {
			mPhase = 3;
			mFullPlatforms = true;
			mAnticheese.antiCheeseCooldown();
			for (Platform platform : mPlatformList.getAllPlatforms()) {
				platform.generateFull();
			}
			mSpellCooldowns -= 10;

			changePhase(mDarkHoleActive, mBasePassives, null);
			forceCastSpell(SpellVesperidysDarkHole.class);

			TextComponent[] dio = new TextComponent[]{
				obfuscate("When we are done with you we shall feast upon the bones of this world!", 0, NamedTextColor.DARK_AQUA),
				obfuscate("There is no hope left!", 0, NamedTextColor.DARK_AQUA),
				obfuscate("THERE. IS. NO. HOPE!!", 0, NamedTextColor.DARK_AQUA).decorate(TextDecoration.BOLD)
			};
			int[] dioDelay = {0, 3 * 20, 6 * 20};
			int[] sounds = {1, 3, 6};
			List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
			for (int i = 0; i < dio.length; i++) {
				TextComponent dialogue = dio[i];
				int finalI = i;
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					players.forEach(p -> com.playmonumenta.scriptedquests.utils.MessagingUtils.sendNPCMessage(p, "The Vesperidys", dialogue));
					voiceOfVesperidys(sounds[finalI]);
				}, dioDelay[i]);
			}
			SongManager.playBossSong(players, new SongManager.Song(MUSIC_TITLE_2, SoundCategory.RECORDS, MUSIC_DURATION_2, true, 1.0f, 1.0f, false), true, mBoss, true, 0, 5);
		});
		events.put(25, (mBoss) -> {
			for (Platform platform : mPlatformList.getAllPlatforms()) {
				platform.generateInstantFull();
			}
			mSpellCooldowns -= 10;
			mPhase = 4;

			TextComponent[] dio = new TextComponent[] {
				obfuscate("Our blood will soil your prison for all eternity foolish dirtspawn.", 0, NamedTextColor.DARK_AQUA),
				obfuscate("My death will unleash the monster your silverspawn has trapped here.", 0, NamedTextColor.DARK_AQUA),
				obfuscate("No one will leave this prison the same.", 0, NamedTextColor.DARK_AQUA),
				};
			int[] dioDelay = {0, 3 * 20, 6 * 20};
			int[] sounds = {2, 5, 1};
			List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
			for (int i = 0; i < dio.length; i++) {
				TextComponent dialogue = dio[i];
				int finalI = i;
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					players.forEach(p -> com.playmonumenta.scriptedquests.utils.MessagingUtils.sendNPCMessage(p, "The Vesperidys", dialogue));
					voiceOfVesperidys(sounds[finalI]);
				}, dioDelay[i]);
			}

			realityDistortion(true);
			changePhase(mDarkHoleActive, mPhase4Passives, null);
		});
		events.put(10, (mBoss) -> {
			TextComponent[] dio = new TextComponent[] {
				obfuscate("My blood will leave you a servant of the others above!", 0, NamedTextColor.DARK_AQUA),
				obfuscate("You will be ours!", 0, NamedTextColor.DARK_AQUA),
				obfuscate("YOU. WILL. BE. OURS!!", 0, NamedTextColor.DARK_AQUA).decorate(TextDecoration.BOLD)
			};
			int[] dioDelay = {0, 3 * 20, 6 * 20};
			int[] sounds = {3, 0, 6};
			List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
			for (int i = 0; i < dio.length; i++) {
				TextComponent dialogue = dio[i];
				int finalI = i;
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					players.forEach(p -> com.playmonumenta.scriptedquests.utils.MessagingUtils.sendNPCMessage(p, "The Vesperidys", dialogue));
					voiceOfVesperidys(sounds[finalI]);
				}, dioDelay[i]);
			}

			mPhase = 5;
			mSpellCooldowns -= 10;
			changePhase(mDarkHoleActive, mBasePassives, null);
			forceCastSpell(SpellVesperidysDarkHole.class);
		});

		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.BLUE, BarStyle.SEGMENTED_10, events, false);
		super.constructBoss(SpellManager.EMPTY, mBasePassives, detectionRange, bossBar);
	}

	@Override
	public void init() {
		EntityUtils.setMaxHealthAndHealth(mBoss, DepthsParty.getAscensionScaledHealth(BOSS_HEALTH, mParty));

		mBoss.setInvisible(true);
		mBoss.setInvulnerable(true);
		mBoss.setCollidable(false);
		mBoss.setGlowing(false);
		mBoss.setAI(false);
		mBoss.getEquipment().clear();
		mBoss.setGravity(false);

		mBoss.teleport(mSpawnLoc.clone().add(0, 4, 0));

		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);

		SongManager.playBossSong(players, new SongManager.Song(MUSIC_TITLE, SoundCategory.RECORDS, MUSIC_DURATION, true, 1.0f, 1.0f, false), true, mBoss, true, 0, 5);

		TextComponent[] dio = new TextComponent[]{
			obfuscate("Yet still does the filthy dirtspawn limps in.", 0, NamedTextColor.DARK_AQUA),
			obfuscate("We are The Vesperidys.", 0, NamedTextColor.DARK_AQUA),
			obfuscate("We shall punish your insolence.", 0, NamedTextColor.DARK_AQUA),
		};
		int[] dioDelay = {20, 4 * 20, 7 * 20};
		int[] sounds = {0, 1, 6};

		// Cutscene
		new BukkitRunnable() {
			int mTicks = 0;
			int mEyeIndex = 0;

			@Override
			public void run() {
				//launch event related spawn commands
				if (mTicks >= 5 * 20 + 10) {
					mBoss.setInvisible(false);
					mBoss.getEquipment().setHelmet(mTeleportSpell.mHelmet, true);
					mBoss.getEquipment().setChestplate(mTeleportSpell.mChestplate, true);
					mBoss.getEquipment().setLeggings(mTeleportSpell.mLeggings, true);
					mBoss.getEquipment().setBoots(mTeleportSpell.mBoots, true);
					mBoss.getEquipment().setItemInMainHand(mTeleportSpell.mMainhand, true);
					mBoss.getEquipment().setItemInOffHand(mTeleportSpell.mOffhand, true);

					new PartialParticle(Particle.FLASH, mBoss.getLocation().add(0, 1.5, 0))
						.spawnAsBoss();
					new PPExplosion(Particle.REDSTONE, mBoss.getLocation().add(0, 1.5, 0))
						.data(new Particle.DustOptions(Color.fromRGB(200, 200, 200), 0))
						.extra(1)
						.count(20)
						.spawnAsBoss();
					new PPExplosion(Particle.FLAME, mBoss.getLocation().add(0, 1.5, 0))
						.extra(1)
						.count(20)
						.spawnAsBoss();
					new PPExplosion(Particle.SOUL_FIRE_FLAME, mBoss.getLocation().add(0, 1.5, 0))
						.extra(1)
						.count(20)
						.spawnAsBoss();

					mBoss.getWorld().playSound(mSpawnLoc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 2f, 1f);

					for (int i = 0; i < dio.length; i++) {
						TextComponent dialogue = dio[i];
						int finalI = i;
						Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
							players.forEach(p -> com.playmonumenta.scriptedquests.utils.MessagingUtils.sendNPCMessage(p, "The Vesperidys", dialogue));
							voiceOfVesperidys(sounds[finalI]);
						}, dioDelay[i]);
					}

					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						mPhase = 1;

						mBoss.setInvulnerable(false);
						mBoss.setCollidable(true);
						mBoss.setAI(true);
						mBoss.setGlowing(true);
						mBoss.setGravity(true);
						mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 5 * 20, 0));

						changePhase(mPhase1Actives, mPhase1Passives, null);
						if (mParty != null && mParty.getAscension() >= 15) {
							forceCastSpell(SpellBreakPlatform.class);
						}

						for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
							MessagingUtils.sendBoldTitle(player, Component.text("The " + bossName, NamedTextColor.DARK_RED), Component.text("The Abyssal Overmind", NamedTextColor.DARK_RED));
							player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.HOSTILE, 2f, 1f);
							mMonuPlugin.mEffectManager.addEffect(player, "VesperidysVoidCorruption", new VoidCorruption(Integer.MAX_VALUE, mMonuPlugin, Vesperidys.this, mBoss, Math.min(50, corruptionEqulibrium())));
							player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0));
						}
					}, dioDelay[2]);
					this.cancel();
					return;
				} else {
					if (mTicks % 10 == 0 && mEyeIndex < 6) {
						mBoss.getWorld().playSound(mSpawnLoc, Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.HOSTILE, 2f, 1f);

						double followAngle = EYE_ORDER[mEyeIndex]; // 0, 36, 72, 108, 144, 180

						summonEyes(followAngle, true, mEyeIndex + 1);
						mEyeIndex++;
					}

					// Black Hole Particles?
					for (int i = 0; i < mTicks / 20.0; i++) {
						double r = 5 - mTicks / 25.0;
						double theta = FastUtils.randomDoubleInRange(0, 2 * Math.PI);
						double alpha = FastUtils.randomDoubleInRange(0, 2 * Math.PI);

						double x = r * FastUtils.sin(alpha) * FastUtils.cos(theta);
						double z = r * FastUtils.sin(alpha) * FastUtils.sin(theta);
						double y = r * FastUtils.cos(alpha);

						Location pLoc = LocationUtils.getEntityCenter(mBoss).clone().add(x, y, z);

						if (FastUtils.randomIntInRange(0, 2) == 0 && mTicks > 3 * 20) {
							new PartialParticle(Particle.SOUL_FIRE_FLAME, pLoc, 1, -x/r, -y/r, -z/r)
								.directionalMode(true)
								.extra(0.1)
								.spawnAsBoss();
						} else {
							int shade = FastUtils.randomIntInRange(150, 255);
							new PartialParticle(Particle.REDSTONE, pLoc, 1, -x/r, -y/r, -z/r, new Particle.DustOptions(Color.fromRGB(shade, shade, shade), 1.0f))
								.directionalMode(true)
								.extra(0.1)
								.spawnAsBoss();
						}
					}

					if (mTicks > 3 * 20) {
						new PartialParticle(Particle.SONIC_BOOM, LocationUtils.getEntityCenter(mBoss))
							.spawnAsBoss();
					}

					if (mTicks == 3 * 20) {
						mBoss.getWorld().playSound(mSpawnLoc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 2f, 0.5f);
					}
				}

				mTicks += 1;
			}

		}.runTaskTimer(mMonuPlugin, 0, 1);
	}

	@Override
	public void onHurt(DamageEvent event) {
		super.onHurt(event);

		if (mInvincible) {
			// Normal shield.
			if (event.getSource() instanceof Player player) {
				event.setCancelled(true);

				player.playSound(mBoss.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.HOSTILE, 1, 5);

				if (!mInvinicibleWarned.contains(player)) {
					player.sendMessage(Component.text(bossName + " has formed an invincible void shield around itself, causing it to be impervious to all attacks!", NamedTextColor.AQUA));
					mInvinicibleWarned.add(player);
				}
			}
		} else {
			if (event.getSource() instanceof Player player) {
				// Ridiculous burst prevention (Maximum of 3% of Vesperidys' Max HP)
				double maxHealth = EntityUtils.getAttributeBaseOrDefault(mBoss, Attribute.GENERIC_MAX_HEALTH, BOSS_HEALTH);
				if (event.getDamage() > maxHealth * 0.03) {
					event.setDamage(maxHealth * 0.03);
				}

				event.setDamage(event.getDamage() * crystalResistanceMultiplier());

				if (mParty != null && mParty.getAscension() >= 15) {
					double distance = player.getLocation().distance(mBoss.getLocation());
					double minDistance = 10;

					if (distance > minDistance) {
						event.setDamage(0);
						Location loc = mBoss.getLocation();
						loc.add(0, 1, 0);

						new PartialParticle(Particle.FIREWORKS_SPARK, loc, 5, 0, 0, 0, 0.3)
							.spawnForPlayer(ParticleCategory.BOSS, player);
						player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.HOSTILE, 0.2f, 1.5f);
					}
				} else if (mParty != null && mParty.getAscension() >= 4) {
					// Range Resistance
					double distance = player.getLocation().distance(mBoss.getLocation());
					double minPercent = 0.5;
					double maxDistance = 20;
					double minDistance = 10;
					if (distance > minDistance) {
						double percentDamage = Math.max(minPercent, Math.min(1.0, -((1 - minPercent) / (maxDistance - minDistance)) * (distance - minDistance) + 1));
						event.setDamage(event.getDamage() * percentDamage);
						if (distance > minDistance + (maxDistance - minDistance) / 2) {
							Location loc = mBoss.getLocation();
							loc.add(0, 1, 0);

							new PartialParticle(Particle.FIREWORKS_SPARK, loc, 5, 0, 0, 0, 0.3)
								.spawnForPlayer(ParticleCategory.BOSS, player);
							player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.HOSTILE, 0.2f, 1.5f);
						}
					}
				}

				// Earthbound Crystal Absorb Damage
				List<LivingEntity> shulkers = EntityUtils.getNearbyMobs(mBoss.getLocation(), 30, EnumSet.of(EntityType.SHULKER));
				Collections.shuffle(shulkers);
				for (LivingEntity mob : shulkers) {
					if (mob.getScoreboardTags().contains(VesperidysVoidCrystalEarth.identityTag)) {
						try {
							VesperidysVoidCrystalEarth earthCrystal = BossUtils.getBossOfClass(mob, VesperidysVoidCrystalEarth.class);
							if (earthCrystal != null) {
								earthCrystal.absorbDamage(mBoss, event);
								break;
							}
						} catch (Exception e) {
							MMLog.warning("Exception for depths on Vesperidys Earthbound Check", e);
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		if (mDefeated) {
			return;
		}
		mDefeated = true;

		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
		if (players.size() <= 0) {
			return;
		}

		TextComponent[] dio = new TextComponent[] {
			obfuscate("You will be devoured by the vessels still above.", 0, NamedTextColor.DARK_AQUA),
			obfuscate("Pikkxys will feast on the core of the earth. Hypollotye will drink the seas from their basins!", 0, NamedTextColor.DARK_AQUA),
			obfuscate("Together, they all will leave this vile place a shell for others to graze upon!", 0, NamedTextColor.DARK_AQUA),
			obfuscate("I will NOT be scattered for nothing!", 0, NamedTextColor.DARK_AQUA),
			obfuscate("I WILL... ", 0, NamedTextColor.DARK_AQUA).decorate(TextDecoration.BOLD),
			obfuscate("NOT... ", 0, NamedTextColor.DARK_AQUA).decorate(TextDecoration.BOLD)
		};
		int[] sounds = {0, 1, 2, 3, 6, 7};

		for (Player player : players) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 40, 10));
			player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 40, 1));
		}

		for (Platform platform : mPlatformList.getAllPlatforms()) {
			platform.generateInstantFull();
		}

		clearAllAdds();
		changePhase(SpellManager.EMPTY, Collections.emptyList(), null);

		mBoss.setHealth(100);
		mBoss.setInvulnerable(true);
		mBoss.setAI(false);
		mBoss.setGravity(false);
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 1000, 10));
		mBoss.removePotionEffect(PotionEffectType.GLOWING);
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
		}.runTaskTimer(mMonuPlugin, 0, 1);

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (mT % 20 == 0) {
					mBoss.getWorld().playSound(mSpawnLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2f, 1);
				}
				new PartialParticle(Particle.EXPLOSION_LARGE, mSpawnLoc.clone().add(0, 5, 0), 1, 10, 10, 10).spawnAsEntityActive(mBoss);

				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}

				mT++;
			}
		}.runTaskTimer(mMonuPlugin, 0, 1);


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
		}.runTaskTimer(mMonuPlugin, 0, 1);

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (mT % 20 == 0) {
					mBoss.getWorld().playSound(mSpawnLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2f, 1);
				}
				new PartialParticle(Particle.EXPLOSION_LARGE, mSpawnLoc.clone().add(0, 5, 0), 1, 10, 10, 10).spawnAsEntityActive(mBoss);

				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}

				mT++;
			}
		}.runTaskTimer(mMonuPlugin, 0, 1);

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				players.forEach(p -> com.playmonumenta.scriptedquests.utils.MessagingUtils.sendNPCMessage(p, "The Vesperidys", dio[mT]));
				voiceOfVesperidys(sounds[mT]);
				mT++;
				if (mT == dio.length) {
					new PartialParticle(Particle.FLASH, mBoss.getLocation().add(0, 1.5, 0))
						.spawnAsBoss();
					new PPExplosion(Particle.REDSTONE, mBoss.getLocation().add(0, 1.5, 0))
						.data(new Particle.DustOptions(Color.fromRGB(200, 200, 200), 0))
						.extra(1)
						.count(20)
						.spawnAsBoss();
					new PPExplosion(Particle.FLAME, mBoss.getLocation().add(0, 1.5, 0))
						.extra(1)
						.count(20)
						.spawnAsBoss();
					new PPExplosion(Particle.SOUL_FIRE_FLAME, mBoss.getLocation().add(0, 1.5, 0))
						.extra(1)
						.count(20)
						.spawnAsBoss();

					this.cancel();
					mBoss.getWorld().playSound(mSpawnLoc, Sound.ENTITY_WITHER_DEATH, SoundCategory.HOSTILE, 2f, 1);
					mBoss.setHealth(0);

					Bukkit.getScheduler().runTaskLater(mMonuPlugin, () -> {
						for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
							MessagingUtils.sendBoldTitle(player, Component.text("VICTORY", NamedTextColor.GREEN), Component.text(bossName + ", the Abyssal Overmind", NamedTextColor.DARK_RED));
							player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.HOSTILE, 100, 0.8f);
							player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 15, 0));
						}

						for (Platform platform : mPlatformList.getAllPlatforms()) {
							platform.destroy();
						}

						mBoss.remove();

						DepthsManager.getInstance().bossDefeated(mBoss.getLocation(), detectionRange);
					}, 20 * 4);
				}
			}
		}.runTaskTimer(mMonuPlugin, 0, 20 * 3);
	}

	// Re-add all phase actives / passives based on current phase. Typical done after dark hole.
	public void resetPhase(int delay) {
		Bukkit.getScheduler().runTaskLater(mMonuPlugin, () -> {
			mInvincible = false;

			switch (mPhase) {
				case 1:
					changePhase(mPhase1Actives, mPhase1Passives, null);
					if (mParty != null && mParty.getAscension() >= 15) {
						forceCastSpell(SpellStarStorm.class);
					}
					break;
				case 2:
					changePhase(mPhase2Actives, mPhase2Passives, null);
					if (mParty != null && mParty.getAscension() >= 4) {
						forceCastSpell(SpellSeekingEyes.class);
					}
					break;
				case 3:
					changePhase(mPhase3Actives, mPhase3Passives, null);
					if (mParty != null && mParty.getAscension() >= 12) {
						forceCastSpell(SpellSoulLink.class);
					} else {
						forceCastSpell(SpellSeekingEyes.class);
					}
					break;
				case 4:
					changePhase(mPhase4Actives, mPhase4Passives, null);
					if (mParty != null && mParty.getAscension() >= 12) {
						forceCastSpell(SpellSoulLink.class);
					} else {
						forceCastSpell(SpellSeekingEyes.class);
					}
					break;
				case 5:
					changePhase(mPhase5Actives, mPhase5Passives, null);
					if (mParty != null && mParty.getAscension() >= 12) {
						forceCastSpell(SpellBreakPlatform.class);
					} else {
						forceCastSpell(SpellSeekingEyes.class);
					}
					break;
				default:
					break;
			}
		}, delay);
	}

	public void realityDistortion(boolean finalPhase) {
		ChargeUpManager chargeUp = new ChargeUpManager(mBoss, REALITY_DISTORTION_TICKS, Component.text("Casting", NamedTextColor.GREEN).append(Component.text(" Reality Distortion...", NamedTextColor.DARK_RED, TextDecoration.BOLD)), net.kyori.adventure.bossbar.BossBar.Color.RED, net.kyori.adventure.bossbar.BossBar.Overlay.NOTCHED_10, 100);
		mTeleportSpell.teleportPlatform(0, 0);

		List<Player> hitPlayers = new ArrayList<>();

		List<Platform> platformHit = new ArrayList<>();
		if (!finalPhase) {
			// Phase 3, hit in a plus shape except the centre platform.
			int pattern = FastUtils.randomIntInRange(0, 2);

			switch (pattern) {
				case 0 -> {
					// T-shapes
					platformHit.add(mPlatformList.getPlatform(0, 0));
					platformHit.add(mPlatformList.getPlatform(1, 1));
					platformHit.add(mPlatformList.getPlatform(2, 2));
					platformHit.add(mPlatformList.getPlatform(1, -1));
					platformHit.add(mPlatformList.getPlatform(2, -2));
					platformHit.add(mPlatformList.getPlatform(-1, 1));
					platformHit.add(mPlatformList.getPlatform(-2, 2));
					platformHit.add(mPlatformList.getPlatform(-1, -1));
					platformHit.add(mPlatformList.getPlatform(-2, -2));
				}
				case 1 -> {
					// Missing Middle
					platformHit.add(mPlatformList.getPlatform(0, 0));
					platformHit.add(mPlatformList.getPlatform(1, 2));
					platformHit.add(mPlatformList.getPlatform(-1, 2));
					platformHit.add(mPlatformList.getPlatform(1, -2));
					platformHit.add(mPlatformList.getPlatform(-1, -2));
					platformHit.add(mPlatformList.getPlatform(2, 1));
					platformHit.add(mPlatformList.getPlatform(2, -1));
					platformHit.add(mPlatformList.getPlatform(-2, -1));
					platformHit.add(mPlatformList.getPlatform(-2, 1));
				}
				default -> {
					// X-Shaped
					platformHit.add(mPlatformList.getPlatform(1, 0));
					platformHit.add(mPlatformList.getPlatform(2, 0));
					platformHit.add(mPlatformList.getPlatform(-1, 0));
					platformHit.add(mPlatformList.getPlatform(-2, 0));
					platformHit.add(mPlatformList.getPlatform(0, 1));
					platformHit.add(mPlatformList.getPlatform(0, 2));
					platformHit.add(mPlatformList.getPlatform(0, -1));
					platformHit.add(mPlatformList.getPlatform(0, -2));
				}
			}
		} else {
			// Final Phase, hit all outer platforms leaving 3 x 3.
			for (int i = -2; i <= 2; i++) {
				platformHit.add(mPlatformList.getPlatform(2, i));
				platformHit.add(mPlatformList.getPlatform(-2, i));
			}
			for (int j = -1; j <= 1; j++) {
				platformHit.add(mPlatformList.getPlatform(j, 2));
				platformHit.add(mPlatformList.getPlatform(j, -2));
			}
		}

		List<Block> platformBlocks = new ArrayList<>();
		for (Platform platform : platformHit) {
			platformBlocks.addAll(platform.mBlocks);
		}

		new BukkitRunnable() {
			int mTicks = 0;
			int mX = -17;

			@Override
			public synchronized void cancel() {
				super.cancel();
				resetPhase(0);
			}

			@Override
			public void run() {
				if (chargeUp.nextTick(1)) {
					for (Platform platform: platformHit) {
						for (Player player : platform.getPlayersOnPlatform()) {
							if (!hitPlayers.contains(player)) {
								hitPlayers.add(player);
								dealPercentageAndCorruptionDamage(player, 0.5, "Reality Distortion");
								MovementUtils.knockAway(platform.getCenter(), player, 0.0f, 0.75f);
							}
						}
						platform.destroy();
					}

					mAnticheese.blockTick(true);
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 1f, 1);

					this.cancel();
					return;
				} else if (Math.abs(mX) <= 17) {
					Location center = Objects.requireNonNull(mPlatformList.getPlatform(0, 0)).getCenter();

					for (int y = -17; y <= 17; y++) {

						Block block = center.clone().add(mX, 0, y).getBlock();

						if (platformBlocks.contains(block)) {
							block.setType(Material.STRIPPED_CRIMSON_HYPHAE);

							new PartialParticle(Particle.CLOUD, block.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0.25).spawnAsBoss();
						}
					}

					mX++;

					if (mTicks % 5 == 0) {
						mBoss.getWorld().playSound(center.clone().add(mX, 0, 0), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 5f, 1);
					}
				}

				mTicks++;
			}
		}.runTaskTimer(mMonuPlugin, 0, 1);
	}

	public class Platform {
		private final Location mCenter;
		public final ArrayList<Block> mBlocks;
		public boolean mBroken;
		public boolean mFull;
		public int mX;
		public int mY;

		public Platform(Location center, int x, int y) {
			mCenter = center;
			mBlocks = new ArrayList<>();
			mBroken = true;
			mX = x;
			mY = y;
		}

		public Location getCenter() {
			return mCenter.clone();
		}

		public List<Player> getPlayersOnPlatform() {
			BoundingBox box = BoundingBox.of(mCenter, 3.5, 30, 3.5);
			List<Player> nearPlayers = PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true);
			List<Player> output = new ArrayList<>();

			for (Player player : nearPlayers) {
				if (player.getBoundingBox().overlaps(box)) {
					output.add(player);
				}
			}

			return output;
		}

		public List<LivingEntity> getMobsOnPlatform() {
			BoundingBox box = BoundingBox.of(mCenter, 3.5, 30, 3.5);
			List<LivingEntity> entities = EntityUtils.getNearbyMobs(mSpawnLoc, detectionRange);
			List<LivingEntity> output = new ArrayList<>();

			for (LivingEntity entity : entities) {
				if (entity.getBoundingBox().overlaps(box) && ScoreboardUtils.checkTag(entity, mobTag)) {
					output.add(entity);
				}
			}

			return output;
		}

		public List<LivingEntity> getMechsOnPlatform() {
			BoundingBox box = BoundingBox.of(mCenter, 3.5, 30, 3.5);
			List<LivingEntity> entities = EntityUtils.getNearbyMobs(mSpawnLoc, detectionRange);
			List<LivingEntity> output = new ArrayList<>();

			for (LivingEntity entity : entities) {
				if (entity.getBoundingBox().overlaps(box) && ScoreboardUtils.checkTag(entity, "Boss")) {
					output.add(entity);
				}
			}

			return output;
		}

		public List<Player> getPlayersOnEdge() {
			List<Player> nearPlayers = PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true);
			List<Player> output = new ArrayList<>();

			for (int i = -3; i <= 3; i++) {
				for (int j = -3; j <= 3; j++) {
					if (Math.abs(i) >= 3 || Math.abs(j) >= 3) {
						BoundingBox box = BoundingBox.of(getCenter().add(i, 0, j), 0.5, 30, 0.5);
						for (Player player : nearPlayers) {
							if (!output.contains(player) && player.getBoundingBox().overlaps(box)) {
								output.add(player);
							}
						}
					}
				}
			}
			return output;
		}

		public void clearEdgeBlocks() {
			mFull = false;

			for (int i = -3; i <= 3; i++) {
				for (int j = -3; j <= 3; j++) {
					for (int k = -2; k <= 0; k++) {
						if (Math.abs(i) >= 3 || Math.abs(j) >= 3) {
							Location loc = mCenter.clone().add(i, k, j);
							Block block = loc.getBlock();

							if (mBlocks.contains(block)) {
								block.setType(Material.AIR);
								mBlocks.remove(block);
							}
						}
					}
				}
			}
		}

		/**
		 * Generates Full Platform Instantly. 7 x 7
		 */
		public void generateInstantFull() {
			mBroken = false;
			mFull = true;
			mBlocks.clear();
			for (int i = -3; i <= 3; i++) {
				for (int j = -3; j <= 3; j++) {
					for (int k = -2; k <= 0; k++) {
						Location loc = mCenter.clone().add(i, k, j);

						// Check block under 23 blocks. (the "clones")
						Location cloneLoc = loc.clone().add(0, CLONE_PLATFORMS_Y_DIFFERENCE, 0);
						Material material = cloneLoc.getBlock().getType();

						Block block = loc.getBlock();
						if (block.getType() != material) {
							block.setType(material);
						}
						if (!mBlocks.contains(block)) {
							mBlocks.add(block);
						}
					}
				}
			}
		}

		/**
		 * Generates only the Inner Platforms immediately. 5 x 5
		 */
		public void generateInstantInner() {
			mBroken = false;
			mBlocks.clear();
			for (int i = -2; i <= 2; i++) {
				for (int j = -2; j <= 2; j++) {
					for (int k = -2; k <= 0; k++) {
						Location loc = mCenter.clone().add(i, k, j);

						// Check block under 23 blocks. (the "clones")
						Location cloneLoc = loc.clone().add(0, CLONE_PLATFORMS_Y_DIFFERENCE, 0);
						Material material = cloneLoc.getBlock().getType();

						Block block = loc.getBlock();
						if (block.getType() != material) {
							block.setType(material);
						}
						if (!mBlocks.contains(block)) {
							mBlocks.add(block);
						}
					}
				}
			}
		}

		/**
		 * Generates Full Platform, resulting in zero spaces. Nice animation.
		 */
		public void generateFull() {
			mFull = true;
			mBroken = false;
			mBlocks.clear();
			new BukkitRunnable() {
				int mCount = 0;

				@Override
				public void run() {
					for (int i = -3; i <= 3; i++) {
						for (int j = -3; j <= 3; j++) {
							for (int k = -2; k <= 0; k++) {

								// Check block under 23 blocks. (the "clones")
								if (FastUtils.RANDOM.nextInt(3) == 0 || mCount >= 4) {
									Location loc = mCenter.clone().add(i, k, j);
									Location cloneLoc = loc.clone().add(0, CLONE_PLATFORMS_Y_DIFFERENCE, 0);
									Material material = cloneLoc.getBlock().getType();

									Block block = loc.getBlock();
									if (block.getType() != material) {
										block.setType(material);
									}

									if (!mBlocks.contains(block)) {
										mBlocks.add(block);
									}
								}
							}
						}
					}

					if (mCount >= 4) {
						this.cancel();
						return;
					}

					mCount++;
				}
			}.runTaskTimer(mMonuPlugin, 0, 10);
		}

		/**
		 * Generates the inner section of the platform. Used only in the "Space Phases". Has animation.
		 */
		public void generateInner() {
			mBroken = false;
			mBlocks.clear();
			new BukkitRunnable() {
				int mCount = 0;

				@Override
				public void run() {
					for (int i = -2; i <= 2; i++) {
						for (int j = -2; j <= 2; j++) {
							for (int k = -2; k <= 0; k++) {

								// Check block under 23 blocks. (the "clones")
								if (FastUtils.RANDOM.nextInt(3) == 0 || mCount >= 4) {
									Location loc = mCenter.clone().add(i, k, j);
									Location cloneLoc = loc.clone().add(0, CLONE_PLATFORMS_Y_DIFFERENCE, 0);
									Material material = cloneLoc.getBlock().getType();

									Block block = loc.getBlock();
									if (block.getType() != material) {
										block.setType(material);
									}

									if (!mBlocks.contains(block)) {
										mBlocks.add(block);
									}
								}
							}
						}
					}

					if (mCount >= 4) {
						this.cancel();
						return;
					}

					mCount++;
				}
			}.runTaskTimer(mMonuPlugin, 0, 10);
		}

		public void destroy() {
			mBroken = true;
			mBoss.getWorld().playSound(mCenter, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 1.0f, 0.5f);

			for (int x = -3; x <= 3; x++) {
				for (int y = -5; y <= 5; y++) {
					for (int z = -3; z <= 3; z++) {
						Block block = getCenter().add(x, y, z).getBlock();
						if (block.getType() != Material.AIR && block.getType() != Material.LIGHT) {
							block.setType(Material.LIGHT);
							Light light = (Light) block.getBlockData();
							light.setLevel(15);
							new PartialParticle(Particle.VILLAGER_HAPPY, block.getLocation(), 1, 0, 0, 0, 0.25).spawnAsBoss();
						}
					}
				}
			}
			mBlocks.clear();
		}
	}

	/**
	 * Data of Platforms is stored as a XY grid, for easy selection.
	 *
	 * Platforms are stored 23 blocks under. Each platform is 5 x 5 in size.
	 */
	public static class PlatformList {
		public HashMap<Integer, HashMap<Integer, Platform>> mPlatformHashMap;

		public PlatformList() {
			mPlatformHashMap = new HashMap<>();
		}

		public void setPlatform(int x, int y, Platform platform) {
			if (mPlatformHashMap.containsKey(x)) {
				HashMap<Integer, Platform> innerMap = mPlatformHashMap.get(x);
				innerMap.put(y, platform);
			} else {
				HashMap<Integer, Platform> innerMap = new HashMap<>();
				innerMap.put(y, platform);
				mPlatformHashMap.put(x, innerMap);
			}
		}

		public List<Platform> getAllPlatforms() {
			ArrayList<Platform> platforms = new ArrayList<>();
			for (HashMap<Integer, Platform> innerMap : mPlatformHashMap.values()) {
				platforms.addAll(innerMap.values());
			}

			return platforms;
		}

		public @Nullable Platform getPlatform(int x, int y) {
			if (mPlatformHashMap.containsKey(x) && mPlatformHashMap.get(x).containsKey(y)) {
				return mPlatformHashMap.get(x).get(y);
			} else {
				return null;
			}
		}

		public @Nullable List<Platform> getPlatformRow(int x) {
			if (mPlatformHashMap.containsKey(x)) {
				return new ArrayList<>(mPlatformHashMap.get(x).values());
			} else {
				return null;
			}
		}

		public @Nullable List<Platform> getPlatformColumn(int y) {
			if (mPlatformHashMap.size() > 0) {
				List<Platform> platformList = new ArrayList<>();
				for (HashMap<Integer, Platform> innerMap : mPlatformHashMap.values()) {
					if (innerMap.containsKey(y)) {
						platformList.add(innerMap.get(y));
					}
				}
				return platformList;
			} else {
				return null;
			}
		}


		public @Nullable Platform getPlatformNearestToEntity(LivingEntity livingEntity) {
			double minDistance = Double.POSITIVE_INFINITY;
			Platform closestPlatform = null;
			if (mPlatformHashMap.size() > 0) {
				for (HashMap<Integer, Platform> innerMap : mPlatformHashMap.values()) {
					for (Platform platform : innerMap.values()) {
						if (platform.getCenter().distance(livingEntity.getLocation()) < minDistance && !platform.mBroken) {
							minDistance = platform.getCenter().distance(livingEntity.getLocation());
							closestPlatform = platform;
						}
					}
				}
			}

			return closestPlatform;
		}

		public List<Platform> getRandomPlatforms(@Nullable List<Platform> exclusionList, int amount) {
			ArrayList<Platform> validPlatforms = new ArrayList<>();
			for (HashMap<Integer, Platform> innerMap : mPlatformHashMap.values()) {
				for (Platform platform : innerMap.values()) {
					if ((exclusionList == null || !exclusionList.contains(platform)) && !platform.mBroken) {
						validPlatforms.add(platform);
					}
				}
			}

			Collections.shuffle(validPlatforms);

			List<Platform> platforms = new ArrayList<>();
			for (Platform platform : validPlatforms) {
				platforms.add(platform);
				if (platforms.size() >= amount) {
					break;
				}
			}

			return platforms;
		}

		public Platform getRandomPlatform(@Nullable List<Platform> exclusionList) {
			return getRandomPlatforms(exclusionList, 1).get(0);
		}

		public List<Platform> getShuffledPlatforms(@Nullable List<Platform> exclusionList) {
			ArrayList<Platform> validPlatforms = new ArrayList<>();
			for (HashMap<Integer, Platform> innerMap : mPlatformHashMap.values()) {
				for (Platform platform : innerMap.values()) {
					if ((exclusionList == null || !exclusionList.contains(platform)) && !platform.mBroken) {
						validPlatforms.add(platform);
					}
				}
			}

			Collections.shuffle(validPlatforms);
			return validPlatforms;
		}

		public void clear() {
			mPlatformHashMap.clear();
		}
	}

	public void dealPercentageAndCorruptionDamage(Player player, double percentDamage, String cause) {
		BossUtils.bossDamagePercent(mBoss, player, DepthsParty.getAscensionScaledDamage(percentDamage * crystalDamageMultiplier(), mParty), cause, false);
		if (mMonuPlugin.mEffectManager.hasEffect(player, VoidCorruption.class)) {
			mMonuPlugin.mEffectManager.getEffects(player, VoidCorruption.class).last().addCorruption(Math.max(1, (int) Math.round(percentDamage * 10 * 5)));
		}
	}

	public void dealCorruptionDamage(Player player, int corruptionAmount) {
		if (mMonuPlugin.mEffectManager.hasEffect(player, VoidCorruption.class)) {
			mMonuPlugin.mEffectManager.getEffects(player, VoidCorruption.class).last().addCorruption(Math.max(1, corruptionAmount));
		}
	}

	private TextComponent obfuscate(String s, int num, NamedTextColor color) {
		TextComponent result = Component.text("");
		char[] chars = s.toCharArray();
		int length = chars.length;
		List<Integer> places = new ArrayList<>();
		for (int i = 0; i < num; i++) {
			places.add(FastUtils.RANDOM.nextInt(length));
		}
		for (int i = 0; i < length; i++) {
			if (chars[i] != ' ' && places.contains(i)) {
				result = result.append(Component.text(chars[i], color, TextDecoration.OBFUSCATED));
			} else {
				result = result.append(Component.text(chars[i], color));
			}
		}
		return result;
	}

	public int corruptionEqulibrium() {
		return switch (mPhase) {
			case 2 -> 10;
			case 3 -> 15;
			case 4 -> 20;
			case 5 -> 25;
			case 6 -> 100;
			default -> 0;
		};
	}

	public void resummonAllEyes() {
		for (int i = 0; i < 6; i += 1) {
			double summonAngle = EYE_ORDER[i];
			int finalI = i;
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				summonEyes(summonAngle, false, finalI + 1);
			}, i * 5L);
		}
	}

	public void removeAllEyes() {
		for (BukkitRunnable eyeRunnable : mEyesRunnable) {
			eyeRunnable.cancel();
		}
	}

	public void summonEyes(double followAngle, boolean flash, int index) {
		double r = 1.25;
		double yaw = Math.toRadians(mBoss.getLocation().getYaw());
		double pitch = Math.toRadians(followAngle);

		double x = r * Math.cos(pitch) * Math.cos(yaw);
		double z = r * Math.cos(pitch) * Math.sin(yaw);
		double y = r * Math.sin(pitch);

		ArmorStand eyes = mBoss.getWorld().spawn(LocationUtils.getEntityCenter(mBoss).clone().add(x, y + ARMOR_STAND_BLOCK_OFFSET, z), ArmorStand.class);
		eyes.setVisible(false);
		eyes.setGravity(false);
		eyes.setMarker(true);
		eyes.setCollidable(false);
		ItemStack enderEye = new ItemStack(Material.ENDER_EYE);
		ItemUtils.setPlainName(enderEye, "Vesperidys Eye " + index);

		eyes.getEquipment().setHelmet(enderEye);

		mEyes.add(eyes);

		if (flash) {
			new PartialParticle(Particle.FLASH, eyes.getLocation().add(0, ARMOR_STAND_PARTICLES_OFFSET, 0))
				.spawnAsBoss();
		}
		new PartialParticle(Particle.SQUID_INK, eyes.getLocation().add(0, ARMOR_STAND_PARTICLES_OFFSET, 0), 5, 0.25, 0.25, 0.25)
			.spawnAsBoss();

		BukkitRunnable eyesRunnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public synchronized void cancel() {
				super.cancel();
				if (flash) {
					new PartialParticle(Particle.FLASH, eyes.getLocation().add(0, ARMOR_STAND_PARTICLES_OFFSET, 0))
						.spawnAsBoss();
				}
				new PartialParticle(Particle.SQUID_INK, eyes.getLocation().add(0, ARMOR_STAND_PARTICLES_OFFSET, 0), 5, 0.25, 0.25, 0.25)
					.spawnAsBoss();
				mEyes.remove(eyes);
				eyes.remove();
			}

			@Override
			public void run() {
				if (mTeleportSpell.mTeleporting || mBoss.isDead() || eyes.isDead() || mDefeated) {
					this.cancel();
					return;
				}

				double r = 1.25;
				double yaw = Math.toRadians(mBoss.getLocation().getYaw());
				double pitch = Math.toRadians(followAngle);

				double x = r * Math.cos(pitch) * Math.cos(yaw);
				double z = r * Math.cos(pitch) * Math.sin(yaw);
				double y = r * Math.sin(pitch) + 0.25 * Math.sin(mTicks / 60.0);

				eyes.teleport(LocationUtils.getEntityCenter(mBoss).add(x, y + ARMOR_STAND_BLOCK_OFFSET, z));
				eyes.setRotation(mBoss.getLocation().getYaw(), 0);

				new PartialParticle(Particle.REDSTONE, eyes.getLocation().add(0, ARMOR_STAND_PARTICLES_OFFSET, 0), 1, 0, 0, 0)
					.data(new Particle.DustOptions(Color.fromRGB(128, 0, 0), 0.8f))
					.spawnAsBoss();

				mTicks++;
			}
		};
		eyesRunnable.runTaskTimer(mPlugin, 0, 1);

	}

	public void clearAllAdds() {
		// Kill all adds
		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
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
		}.runTaskTimer(mMonuPlugin, 0, 1);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (event.getBossSpellName() != null) {
			event.setDamage(DepthsParty.getAscensionScaledDamage(event.getDamage() * crystalDamageMultiplier(), mParty));
		}
	}

	public double crystalDamageMultiplier() {
		// Look for mobs with the Crystal Tag
		List<LivingEntity> livingEntities = EntityUtils.getNearbyMobs(mBoss.getLocation(), 100);
		int count = 0;
		for (LivingEntity e : livingEntities) {
			Set<String> tags = e.getScoreboardTags();
			if (tags.contains("VoidCrystal")) {
				count++;
			}
		}
		return 1 + 0.1 * count;
	}

	public double crystalResistanceMultiplier() {
		// Look for mobs with the Crystal Tag
		List<LivingEntity> livingEntities = EntityUtils.getNearbyMobs(mBoss.getLocation(), 100);
		int count = 0;
		for (LivingEntity e : livingEntities) {
			Set<String> tags = e.getScoreboardTags();
			if (tags.contains("VoidCrystal")) {
				count++;
			}
		}
		return 1 / (1 + 0.1 * count);
	}

	public Team createTeams() {
		return ScoreboardUtils.getExistingTeamOrCreate("vesperidysCasting", NamedTextColor.DARK_RED);
	}

	public void voiceOfVesperidys(int voiceID) {
		World world = mBoss.getWorld();
		Location loc = mBoss.getLocation();
		switch (voiceID) {
			case 0:
				// Roar
				world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 2, 0.5f);
				break;
			case 1:
			case 5:
				// Vex
				world.playSound(loc, Sound.ENTITY_WITHER_AMBIENT, 2, 1f);
				break;
			case 2:
				// Creeper
				world.playSound(loc, Sound.ENTITY_CREEPER_HURT, 2, 0.75f);
				break;
			case 3:
				// Blaze
				world.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, 2, 2f);
				break;
			case 4:
				// Ghast Scream
				world.playSound(loc, Sound.ENTITY_GHAST_AMBIENT, 2, 1.5f);
				break;
			case 6:
				world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 2, 0.5f);
				world.playSound(loc, Sound.ENTITY_WITHER_AMBIENT, 2, 1f);
				world.playSound(loc, Sound.ENTITY_CREEPER_HURT, 2, 0.75f);
				world.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, 2, 2f);
				world.playSound(loc, Sound.ENTITY_GHAST_AMBIENT, 2, 1.5f);
				break;
			case 7:
				world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 2, 0.5f);
				world.playSound(loc, Sound.ENTITY_WITHER_DEATH, 2, 1f);
				world.playSound(loc, Sound.ENTITY_CREEPER_DEATH, 2, 0.75f);
				world.playSound(loc, Sound.ENTITY_BLAZE_DEATH, 2, 2f);
				world.playSound(loc, Sound.ENTITY_GHAST_DEATH, 2, 1.5f);
				break;
			default:
				break;
		}
	}
}
