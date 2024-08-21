package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseParticleAura;
import com.playmonumenta.plugins.bosses.spells.SpellBossBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellConditionalTeleport;
import com.playmonumenta.plugins.bosses.spells.SpellPlayerAction;
import com.playmonumenta.plugins.bosses.spells.SpellShieldStun;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellArachnopocolypse;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellEarthsWrath;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellGroundSurge;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellKaulsJudgement;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellLightningStorm;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellLightningStrike;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellPutridPlague;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellRaiseJungle;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellVolcanicDemise;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.EffectType;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
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
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;


/* Woah it's Kaul! */

/*
Phase 1 :
Attacks :
Raise Jungle
Arachnopocalypse
Putrid plague
Earth's Wrath


Phase 2 :
Earth's wrath
Putrid Plague
Raise Jungle
Kaul's Judgement

Phase 2.5 (50% health) :
Summons a powerful Primordial Elemental that is invulnerable and immovable until out of the ground. Players will have 15 seconds to prepare for the elemental's arrival. Kaul will not be attacking or casting any abilities (except for his passives) during this time. (512 health)

Elemental's Abilities:
Normal Block break passive
Raise Jungle (Kaul's ability), however the timer for raising them will be 30 seconds instead of 40.
Earthen Rupture: After charging for 2 seconds, the Elemental will cause a large rupture that spans out 5 blocks, knocking back all players, dealing 18 damage, and applying Slowness II for 10 seconds.
Stone Blast: After 1 second, fires at all players a powerful block breaking bolt. Intersecting with a player causes 15 damage and applies Weakness II and Slowness II. Intersecting with a block causes a TNT explosion to happen instead. The bolt will stop travelling if it hits a player or a block.
Once the elemental is dead, Kaul returns to the fight. The elemental will meld into the ground for later return in Phase 3.5

Phase 3:
Earthâ€™s Wrath
Putrid plague
Volcanic demise
Kaul's Judgement

Phase 3.5 (20% health [Let's make this even harder shall we?]) :
The Primordial Elemental from Phase 2.5 returns, however he is completely invulnerable to all attacks, and gains Strength I for the rest of the fight. The elemental will remain active until the end of the fight.
The elemental will lose his "Raise Jungle" ability, but will still possess the others.

 *
 */
/*
 * Base Spells:
 * /
 * Volcanic Demise (Magma cream): Kaul shoots at each player at the
 * same time, casting particle fireballs at each player with a large
 * hit radius. On contact with a player, deals 20 damage and ignites
 * the player for 10 seconds. If the player shields the fireball, the
 * shield takes 50% durability damage and is put on cooldown for 30
 * seconds (The player is still set on fire). On contact with a block,
 * explodes and leaves a fiery aura that deals 10 damage and ignites
 * for 5 seconds (+3 seconds for fire duration if a player is already on
 * fire) to players who stand in it. The aura lasts 5 seconds.
 * (Aka Disco Inferno)
 */

public class Kaul extends SerializedLocationBossAbilityGroup {
	public static final String identityTag = "boss_kaul";
	public static final int detectionRange = 50;
	public static final int ARENA_WIDTH = 111;
	// Barrier layer is from Y 62.0 to 64.0
	public static final int ARENA_MAX_Y = 62;

	private static final int LIGHTNING_STRIKE_COOLDOWN_SECONDS_1 = 18;
	private static final int LIGHTNING_STRIKE_COOLDOWN_SECONDS_2 = 12;
	private static final int LIGHTNING_STRIKE_COOLDOWN_SECONDS_3 = 10;
	private static final int LIGHTNING_STRIKE_COOLDOWN_SECONDS_4 = 6;
	private static final int MAX_HEALTH = 2048;
	private static final double SCALING_X = 0.7;
	private static final double SCALING_Y = 0.65;
	private static final String primordial = "PrimordialElemental";
	private static final String immortal = "ImmortalElemental";
	private static final String LIGHTNING_STORM_TAG = "KaulLightningStormTag";
	private static final String PUTRID_PLAGUE_TAG_RED = "KaulPutridPlagueRed";
	private static final String PUTRID_PLAGUE_TAG_BLUE = "KaulPutridPlagueBlue";
	private static final String PUTRID_PLAGUE_TAG_YELLOW = "KaulPutridPlagueYellow";
	private static final String PUTRID_PLAGUE_TAG_GREEN = "KaulPutridPlagueGreen";
	private static final Particle.DustOptions RED_COLOR = new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.0f);

	// At the centre of the Kaul shrine, upon the height of most of the arena's surface
	private LivingEntity mShrineMarker;
	private boolean mDefeated = false;
	private boolean mCooldown = false;
	private boolean mPrimordialPhase = false;
	private int mHits = 0;
	private int mPlayerCount;
	private double mDefenseScaling;

	public Kaul(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);

		mShrineMarker = boss;
		for (LivingEntity le : boss.getWorld().getLivingEntities()) {
			if (le.getScoreboardTags().contains(LIGHTNING_STORM_TAG)) {
				mShrineMarker = le;
				break;
			}
		}
		mPlayerCount = getArenaParticipants().size();
		mDefenseScaling = BossUtils.healthScalingCoef(mPlayerCount, SCALING_X, SCALING_Y);
		mBoss.setRemoveWhenFarAway(false);
		World world = boss.getWorld();
		mBoss.addScoreboardTag("Boss");

		new BukkitRunnable() {
			@Override
			public void run() {
				for (Player player : PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true)) {
					if (player.isSleeping()) {
						DamageUtils.damage(mBoss, player, DamageType.OTHER, 22);
						EffectType.applyEffect(EffectType.SLOW, player, 15 * 20, 0.3, "KaulAntiSleepSlowness", false);
						player.sendMessage(Component.text("THE JUNGLE FORBIDS YOU TO DREAM.", NamedTextColor.DARK_GREEN));
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_DEATH, SoundCategory.HOSTILE, 1, 0.85f);
					}
				}
				if (mDefeated || mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 5);

		/* These spells need to be shared between phases in this manner to prevent double casting on phase change
		 * I would do this for all of the spells here but I'll just thank Java for having no support for true pass by
		 * copy for objects instead
		 */
		SpellKaulsJudgement kaulsJudgement = new SpellKaulsJudgement(mBoss, this);
		SpellVolcanicDemise volcanicDemise = new SpellVolcanicDemise(plugin, mBoss, 40D, mShrineMarker.getLocation());

		SpellManager phase1Spells = new SpellManager(
			Arrays.asList(new SpellRaiseJungle(mPlugin, mBoss, 10, detectionRange, 20 * 9, 20 * 10, mShrineMarker.getLocation().getY()),
				new SpellPutridPlague(mPlugin, mBoss, this, false),
				new SpellEarthsWrath(mPlugin, mBoss, mShrineMarker.getLocation().getY()),
				new SpellArachnopocolypse(mPlugin, mBoss, detectionRange, mSpawnLoc)));

		SpellManager phase2Spells = new SpellManager(
			Arrays.asList(new SpellPutridPlague(mPlugin, mBoss, this, false),
				new SpellEarthsWrath(mPlugin, mBoss, mShrineMarker.getLocation().getY()),
				new SpellRaiseJungle(mPlugin, mBoss, 10, detectionRange, 20 * 8, 20 * 10, mShrineMarker.getLocation().getY()),
				new SpellArachnopocolypse(mPlugin, mBoss, detectionRange, mSpawnLoc),
				kaulsJudgement));

		SpellManager phase3Spells = new SpellManager(
			Arrays.asList(new SpellPutridPlague(mPlugin, mBoss, this, true),
				new SpellEarthsWrath(mPlugin, mBoss, mShrineMarker.getLocation().getY()),
				new SpellGroundSurge(mPlugin, mBoss, detectionRange),
				volcanicDemise, kaulsJudgement));

		SpellManager phase4Spells = new SpellManager(
			Arrays.asList(new SpellPutridPlague(mPlugin, mBoss, this, true),
				new SpellEarthsWrath(mPlugin, mBoss, mShrineMarker.getLocation().getY()),
				new SpellGroundSurge(mPlugin, mBoss, detectionRange),
				volcanicDemise));

		List<UUID> hit = new ArrayList<>();
		List<UUID> cd = new ArrayList<>();
		SpellPlayerAction action = new SpellPlayerAction(this::getArenaParticipants, (Player player) -> {
			Vector loc = player.getLocation().toVector();
			if (player.getLocation().getBlock().isLiquid() || !loc.isInSphere(mShrineMarker.getLocation().toVector(), 42)) {
				if (player.getLocation().getY() >= 61 || cd.contains(player.getUniqueId())) {
					return;
				}
				/* Damage has no direction so can't be blocked */
				if (BossUtils.bossDamagePercent(mBoss, player, 0.4)) {
					/* Player survived the damage */
					MovementUtils.knockAway(mSpawnLoc, player, -2.5f, 0.85f);
					world.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_DEATH, SoundCategory.HOSTILE, 1, 1.3f);
					new PartialParticle(Particle.SMOKE_NORMAL, player.getLocation().add(0, 1, 0), 80, 0.25, 0.45, 0.25, 0.15).spawnAsBoss();
					cd.add(player.getUniqueId());
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> cd.remove(player.getUniqueId()), 10);
				}
				if (player.getLocation().getBlock().isLiquid()) {
					if (!hit.contains(player.getUniqueId())) {
						hit.add(player.getUniqueId());
						player.sendMessage(Component.text("That hurt! It seems like the water is extremely corrosive. Best to stay out of it.", NamedTextColor.AQUA));
					}
				} else if (!loc.isInSphere(mShrineMarker.getLocation().toVector(), 42)) {
					player.sendMessage(Component.text("You feel a powerful force pull you back in fiercely. It seems there's no escape from this fight.", NamedTextColor.AQUA));
				}
			}
		});

		// These spells need to be shared between phases in this manner to prevent double casting on phase change
		SpellLightningStorm lightningStorm = new SpellLightningStorm(boss, this);
		SpellBossBlockBreak bossBlockBreak = new SpellBossBlockBreak(mBoss, 8, 1, 3, 1, true, true);
		SpellShieldStun shieldStun = new SpellShieldStun(30 * 20);
		SpellBaseParticleAura greenParticleAura = new SpellBaseParticleAura(boss, 1, (LivingEntity mBoss) ->
			new PartialParticle(Particle.FALLING_DUST, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0),
				8, 0.35, 0.45, 0.35, Material.GREEN_CONCRETE.createBlockData()).spawnAsBoss());
		SpellBaseParticleAura angryParticleAura = new SpellBaseParticleAura(boss, 1, (LivingEntity mBoss) -> {
			new PartialParticle(Particle.FALLING_DUST, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 2, 0.35,
				0.45, 0.35, Material.GREEN_CONCRETE.createBlockData()).spawnAsBoss();
			new PartialParticle(Particle.FLAME, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 2, 0.35, 0.45,
				0.35, 0.1).spawnAsBoss();
			new PartialParticle(Particle.REDSTONE, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 2, 0.35, 0.45,
				0.35, RED_COLOR).spawnAsBoss();
			new PartialParticle(Particle.FALLING_DUST, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 2, 0.35,
				0.45, 0.35, Material.BLUE_WOOL.createBlockData()).spawnAsBoss();
		});
		SpellConditionalTeleport conditionalTeleport = new SpellConditionalTeleport(mBoss, spawnLoc,
			b -> b.getLocation().getBlock().getType() == Material.BEDROCK
			  || b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK
			  || b.getLocation().getBlock().getType() == Material.LAVA
			  || b.getLocation().getBlock().getType() == Material.WATER);

		List<Spell> phase1PassiveSpells = Arrays.asList(
			new SpellLightningStrike(this, LIGHTNING_STRIKE_COOLDOWN_SECONDS_1, false, mShrineMarker.getLocation()),
			lightningStorm, bossBlockBreak, shieldStun, greenParticleAura, conditionalTeleport, action
		);

		List<Spell> phase2PassiveSpells = Arrays.asList(
			new SpellLightningStrike(this, LIGHTNING_STRIKE_COOLDOWN_SECONDS_2, true, mShrineMarker.getLocation()),
			lightningStorm, bossBlockBreak, shieldStun, greenParticleAura, conditionalTeleport, action
		);

		List<Spell> phase3PassiveSpells = Arrays.asList(
			new SpellLightningStrike(this, LIGHTNING_STRIKE_COOLDOWN_SECONDS_3, true, mShrineMarker.getLocation()),
			lightningStorm, bossBlockBreak, shieldStun, angryParticleAura, conditionalTeleport, action
		);

		List<Spell> phase4PassiveSpells = Arrays.asList(
			new SpellLightningStrike(this, LIGHTNING_STRIKE_COOLDOWN_SECONDS_4, true, mShrineMarker.getLocation()),
			lightningStorm, bossBlockBreak, shieldStun, angryParticleAura, conditionalTeleport, action
		);

		Map<Integer, BossHealthAction> events = new HashMap<>();
		events.put(100, mBoss -> {
			Collection<Player> players = getArenaParticipants();
			Component message;
			if (players.size() <= 1) {
				message = Component.text("THE JUNGLE WILL NOT ALLOW A LONE MORTAL LIKE YOU TO LIVE. PERISH, FOOLISH USURPER!", NamedTextColor.DARK_GREEN);
			} else {
				message = Component.text("THE JUNGLE WILL TAKE YOUR PRESENCE NO MORE. PERISH, USURPERS.", NamedTextColor.DARK_GREEN);
			}
			players.forEach(p -> p.sendMessage(message));
		});

		events.put(75, mBoss -> {
			if (getActiveSpells().isEmpty()) {
				MMLog.warning(() -> "[Kaul] Kaul somehow had no active spells when attempting to force cast Arachnopocolypse at 75% health! Overriding with phase 1 spells");
				changePhase(phase1Spells, phase1PassiveSpells, null);
			}
			forceCastSpell(SpellArachnopocolypse.class);
		});

		// Phase 2
		events.put(66, mBoss -> {
			MMLog.info(() -> "[Kaul] A Kaul fight got to 66% health");
			sendDialogue("THE JUNGLE WILL DEVOUR YOU. ALL RETURNS TO ROT.");
			knockback(plugin, 10);
			mBoss.setInvulnerable(true);
			mBoss.setAI(false);
			mBoss.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
			com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(mBoss, PercentDamageReceived.GENERIC_NAME);
			com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mBoss, PercentDamageReceived.GENERIC_NAME,
				new PercentDamageReceived(20 * 9999, -1.0));
			changePhase(SpellManager.EMPTY, Collections.emptyList(), null);

			new BukkitRunnable() {
				@Override
				public void run() {
					teleport(mSpawnLoc.clone().add(0, 5, 0));
					new BukkitRunnable() {
						final Location mLoc = mBoss.getLocation();
						float mJ = 0;
						double mRotation = 0;
						double mRadius = 10;

						@Override
						public void run() {
							mJ++;
							world.playSound(mBoss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 3, 0.5f + (mJ / 25));
							for (int i = 0; i < 5; i++) {
								double radian1 = Math.toRadians(mRotation + (72 * i));
								mLoc.add(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);
								new PartialParticle(Particle.SPELL_WITCH, mLoc, 6, 0.25, 0.25, 0.25, 0).spawnAsBoss();
								new PartialParticle(Particle.BLOCK_DUST, mLoc, 4, 0.25, 0.25, 0.25, 0.25,
									Material.COARSE_DIRT.createBlockData()).spawnAsBoss();
								mLoc.subtract(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);
							}
							new PartialParticle(Particle.SPELL_WITCH, mShrineMarker.getLocation().add(0, 3, 0), 20, 8, 5, 8,
								0).spawnAsBoss();
							mRotation += 8;
							mRadius -= 0.25;

							if (mBoss.isDead() || !mBoss.isValid()) {
								this.cancel();
							}

							if (mRadius <= 0) {
								this.cancel();
								Location loc = mShrineMarker.getLocation().subtract(0, 0.5, 0);
								changePhase(SpellManager.EMPTY, phase2PassiveSpells, null);
								new BukkitRunnable() {
									int mT = 0;
									final double mRotation = 0;
									double mRadius = 0;

									@Override
									public void run() {
										mT++;
										mRadius = mT;
										new PartialParticle(Particle.SPELL_WITCH, mShrineMarker.getLocation().add(0, 3, 0), 20, 8, 5, 8, 0).spawnAsBoss();
										new PartialParticle(Particle.SMOKE_NORMAL, mShrineMarker.getLocation().add(0, 3, 0), 10, 8, 5, 8, 0).spawnAsBoss();
										for (int i = 0; i < 36; i++) {
											double radian1 = Math.toRadians(mRotation + (10 * i));
											loc.add(FastUtils.cos(radian1) * mRadius, 1, FastUtils.sin(radian1) * mRadius);
											new PartialParticle(Particle.SPELL_WITCH, loc, 3, 0.4, 0.4, 0.4, 0).spawnAsBoss();
											new PartialParticle(Particle.BLOCK_DUST, loc, 2, 0.4, 0.4, 0.4, 0.25,
												Material.COARSE_DIRT.createBlockData()).spawnAsBoss();
											loc.subtract(FastUtils.cos(radian1) * mRadius, 1, FastUtils.sin(radian1) * mRadius);
										}
										for (Block block : LocationUtils.getEdge(loc.clone().subtract(mT, 0, mT),
											loc.clone().add(mT, 0, mT))) {
											if (FastUtils.RANDOM.nextInt(6) == 1 && block.getType() == Material.SMOOTH_SANDSTONE
												    && block.getLocation().add(0, 1.5, 0).getBlock()
													       .getType() == Material.AIR) {
												block.setType(Material.SMOOTH_RED_SANDSTONE);
											}
										}
										if (mT >= 40) {
											this.cancel();
										}
									}

								}.runTaskTimer(mPlugin, 0, 1);
								for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
									player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 1,
										0.75f);
								}
								new BukkitRunnable() {
									@Override
									public void run() {
										mBoss.setInvulnerable(false);
										mBoss.setAI(true);
										teleport(mSpawnLoc);
										com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(mBoss, PercentDamageReceived.GENERIC_NAME);
										new BukkitRunnable() {
											@Override
											public void run() {
												changePhase(phase2Spells, phase2PassiveSpells, null);
											}
										}.runTaskLater(mPlugin, 20 * 10);
									}
								}.runTaskLater(mPlugin, 20 * 2);
							}
						}
					}.runTaskTimer(mPlugin, 30, 1);
				}
			}.runTaskLater(mPlugin, 20 * 2);
		});

		// Forcecast Raise Jungle
		events.put(60, mBoss -> {
			if (getActiveSpells().isEmpty()) {
				MMLog.warning(() -> "[Kaul] Kaul somehow had no active spells when attempting to force cast Raise Jungle at 60% health! Overriding with phase 2 spells");
				changePhase(phase2Spells, phase2PassiveSpells, null);
			}
			forceCastSpell(SpellRaiseJungle.class);
		});

		// Phase 2.5
		events.put(50, mBoss -> {
			MMLog.info(() -> "[Kaul] A Kaul fight got to 50% health");
			sendDialogue("THE EARTH AND JUNGLE ARE ENTWINED. PRIMORDIAL, HEWN FROM SOIL AND STONE, END THEM.");
			knockback(plugin, 10);
			mBoss.setInvulnerable(true);
			mBoss.setAI(false);
			com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(mBoss, PercentDamageReceived.GENERIC_NAME);
			com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mBoss, PercentDamageReceived.GENERIC_NAME,
				new PercentDamageReceived(20 * 9999, -1.0));
			teleport(mSpawnLoc.clone().add(0, 5, 0));
			changePhase(SpellManager.EMPTY, phase1PassiveSpells, null);
			mPrimordialPhase = true;

			new BukkitRunnable() {
				final Location mLoc = mSpawnLoc;
				double mRotation = 0;
				double mRadius = 10;

				@Override
				public void run() {
					for (int i = 0; i < 5; i++) {
						double radian1 = Math.toRadians(mRotation + (72 * i));
						mLoc.add(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);
						new PartialParticle(Particle.SPELL_WITCH, mLoc, 3, 0.1, 0.1, 0.1, 0).spawnAsBoss();
						new PartialParticle(Particle.BLOCK_DUST, mLoc, 3, 0.1, 0.1, 0.1, 0.25,
							Material.DIRT.createBlockData()).spawnAsBoss();
						mLoc.subtract(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);
					}
					mRotation += 8;
					mRadius -= 0.15;
					if (mRadius <= 0) {
						this.cancel();
						world.playSound(mLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 2, 0);
						world.playSound(mLoc, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1, 0.75f);
						new PartialParticle(Particle.CRIT_MAGIC, mLoc, 50, 0.1, 0.1, 0.1, 1).spawnAsBoss();
						new PartialParticle(Particle.BLOCK_CRACK, mLoc, 150, 0.1, 0.1, 0.1, 0.5,
							Material.DIRT.createBlockData()).spawnAsBoss();
						LivingEntity miniboss = (LivingEntity) LibraryOfSoulsIntegration.summon(mLoc, primordial);
						MMLog.info(() -> "[Kaul] Kaul has summoned the Primordial Elemental");
						new BukkitRunnable() {
							@Override
							public void run() {
								if (miniboss == null) {
									MMLog.warning(() -> "[Kaul] Kaul tried to summon the Primordial Elemental, but the miniboss is null!");
									this.cancel();
								} else if (miniboss.isDead() || !miniboss.isValid()) {
									MMLog.info(() -> "[Kaul] Kaul's Primordial Elemental is dead or no longer valid. Entering next phase");
									this.cancel();
									mBoss.setInvulnerable(false);
									mBoss.setAI(true);
									teleport(mSpawnLoc);
									com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(mBoss, PercentDamageReceived.GENERIC_NAME);
									mPrimordialPhase = false;
									new BukkitRunnable() {
										@Override
										public void run() {
											changePhase(phase2Spells, phase2PassiveSpells, null);
										}
									}.runTaskLater(mPlugin, 20 * 10);
								}

								if (mBoss.isDead() || !mBoss.isValid()) {
									MMLog.warning(() -> "[Kaul] Kaul is somehow dead or no longer valid after summoning the Primordial Elemental! This is very bad!");
									this.cancel();
								}
							}
						}.runTaskTimer(mPlugin, 0, 20);
					}
					if (mBoss.isDead()) {
						MMLog.warning(() -> "[Kaul] Kaul is somehow dead after summoning the Primordial Elemental! This is very bad!");
						this.cancel();
					}
				}
			}.runTaskTimer(plugin, 0, 1);
		});

		// Force-cast Kaul's Judgement if it hasn't been cast yet.
		events.put(40, mBoss -> {
			if (getActiveSpells().isEmpty()) {
				MMLog.warning(() -> "[Kaul] Kaul somehow had no active spells when attempting to force cast Kaul's Judgement " + kaulsJudgement + " at 40% health! Overriding with phase 2 spells");
				changePhase(phase2Spells, phase2PassiveSpells, null);
			}
			forceCastSpell(kaulsJudgement.getClass());
		});

		// Phase 3
		events.put(33, mBoss -> {
			MMLog.info(() -> "[Kaul] A Kaul fight got to 33% health");
			sendDialogue("YOU ARE NOT ANTS, BUT PREDATORS. YET THE JUNGLE'S WILL IS MANIFEST; DEATH COMES TO ALL.");
			knockback(plugin, 10);
			mBoss.setInvulnerable(true);
			mBoss.setAI(false);
			com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(mBoss, PercentDamageReceived.GENERIC_NAME);
			com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mBoss, PercentDamageReceived.GENERIC_NAME,
				new PercentDamageReceived(20 * 9999, -1.0));
			changePhase(SpellManager.EMPTY, phase1PassiveSpells, null);

			new BukkitRunnable() {
				@Override
				public void run() {
					List<ArmorStand> points = new ArrayList<>();
					for (ArmorStand e : mBoss.getLocation().getNearbyEntitiesByType(ArmorStand.class, detectionRange, detectionRange, detectionRange)) {
						if (e.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_RED)
							     || e.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_BLUE)
							     || e.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_YELLOW)
							     || e.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_GREEN)) {
							points.add(e);
						}
					}

					if (!points.isEmpty()) {
						teleport(mSpawnLoc.clone().add(0, 5, 0));
						for (ArmorStand point : points) {
							world.playSound(mBoss.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 5, 0.75f);
							new BukkitRunnable() {
								final Location mLoc = point.getLocation().add(0, 15, 0);
								final Vector mDir = LocationUtils.getDirectionTo(mBoss.getLocation().add(0, 1, 0), mLoc);
								float mT = 0;

								@Override
								public void run() {
									mT++;
									if (mT % 2 == 0) {
										new PartialParticle(Particle.SPELL_WITCH, mShrineMarker.getLocation().add(0, 3, 0), 10, 8, 5, 9, 0).spawnAsBoss();
									}
									new PartialParticle(Particle.FLAME, mShrineMarker.getLocation().add(0, 3, 0), 10, 8, 5, 9, 0).spawnAsBoss();
									new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1.25, 0), 16, 0.35, 0.45, 0.35, 0).spawnAsBoss();
									new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1.25, 0), 1, 0.35, 0.45, 0.35, 0).spawnAsBoss();
									if (mT == 1) {
										mLoc.getWorld().createExplosion(mLoc, 6, true);
										mLoc.getWorld().createExplosion(mLoc.clone().subtract(0, 4, 0), 6, true);
									}
									mLoc.add(mDir.clone().multiply(0.35));
									if (point.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_BLUE)) {
										new PartialParticle(Particle.FALLING_DUST, mLoc, 9, 0.4, 0.4, 0.4, Material.BLUE_WOOL.createBlockData()).spawnAsBoss();
										new PartialParticle(Particle.BLOCK_DUST, mLoc, 5, 0.4, 0.4, 0.4, Material.BLUE_WOOL.createBlockData()).spawnAsBoss();
										new PartialParticle(Particle.EXPLOSION_NORMAL, mLoc, 2, 0.4, 0.4, 0.4, 0.1).spawnAsBoss();
									} else if (point.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_RED)) {
										new PartialParticle(Particle.REDSTONE, mLoc, 15, 0.4, 0.4, 0.4, RED_COLOR).spawnAsBoss();
										new PartialParticle(Particle.FALLING_DUST, mLoc, 10, 0.4, 0.4, 0.4, Material.RED_WOOL.createBlockData()).spawnAsBoss();
									} else if (point.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_YELLOW)) {
										new PartialParticle(Particle.FLAME, mLoc, 10, 0.3, 0.3, 0.3, 0.1).spawnAsBoss();
										new PartialParticle(Particle.SMOKE_LARGE, mLoc, 3, 0.4, 0.4, 0.4, 0).spawnAsBoss();
									} else if (point.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_GREEN)) {
										new PartialParticle(Particle.FALLING_DUST, mLoc, 9, 0.4, 0.4, 0.4, Material.GREEN_TERRACOTTA.createBlockData()).spawnAsBoss();
										new PartialParticle(Particle.BLOCK_DUST, mLoc, 5, 0.4, 0.4, 0.4, Material.GREEN_TERRACOTTA.createBlockData()).spawnAsBoss();
										new PartialParticle(Particle.EXPLOSION_NORMAL, mLoc, 2, 0.4, 0.4, 0.4, 0.1).spawnAsBoss();
									}
									if (mLoc.distance(mSpawnLoc.clone().add(0, 5, 0)) < 1.25 || mLoc.distance(mBoss.getLocation().add(0, 1, 0)) < 1.25) {
										this.cancel();
										mHits++;
									}

									if (mBoss.isDead() || !mBoss.isValid()) {
										MMLog.warning(() -> "[Kaul] Kaul is somehow dead or not valid after running the aesthetics for his final phase! This is very bad!");
										this.cancel();
									}

									if (mHits >= 4) {
										this.cancel();
										new PartialParticle(Particle.SPELL_WITCH, mShrineMarker.getLocation().add(0, 3, 0), 25, 6, 5, 6, 1).spawnAsBoss();
										new PartialParticle(Particle.FLAME, mShrineMarker.getLocation().add(0, 3, 0), 40, 6, 5, 6, 0.1).spawnAsBoss();
										EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MOVEMENT_SPEED, 0.02 + EntityUtils.getAttributeOrDefault(mBoss, Attribute.GENERIC_MOVEMENT_SPEED, 0));
										EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_ATTACK_DAMAGE, 3.0 + EntityUtils.getAttributeBaseOrDefault(mBoss, Attribute.GENERIC_ATTACK_DAMAGE, 3));
										changePhase(SpellManager.EMPTY, phase3PassiveSpells, null);
										new PartialParticle(Particle.FLAME, mBoss.getLocation().add(0, 1, 0), 200, 0, 0, 0, 0.175).spawnAsBoss();
										new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 75, 0, 0, 0, 0.25).spawnAsBoss();
										new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation().add(0, 1, 0), 75, 0, 0, 0, 0.25).spawnAsBoss();
										world.playSound(mBoss.getLocation().add(0, 1, 0), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 5, 0.9f);
										world.playSound(mBoss.getLocation().add(0, 1, 0), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 5, 0f);

										new BukkitRunnable() {
											final Location mLoc = mShrineMarker.getLocation().subtract(0, 0.5, 0);
											final double mRotation = 0;
											double mRadius = 0;
											int mT = 0;

											@Override
											public void run() {
												mT++;
												mRadius = mT;
												for (int i = 0; i < 36; i++) {
													double radian1 = Math.toRadians(mRotation + (10 * i));
													mLoc.add(FastUtils.cos(radian1) * mRadius, 1, FastUtils.sin(radian1) * mRadius);
													new PartialParticle(Particle.FLAME, mLoc, 2, 0.25, 0.25, 0.25, 0.1).spawnAsBoss();
													new PartialParticle(Particle.BLOCK_DUST, mLoc, 2, 0.25, 0.25, 0.25, 0.25, Material.COARSE_DIRT.createBlockData()).spawnAsBoss();
													mLoc.subtract(FastUtils.cos(radian1) * mRadius, 1, FastUtils.sin(radian1) * mRadius);
												}
												for (Block block : LocationUtils.getEdge(mLoc.clone().subtract(mT, 0, mT), mLoc.clone().add(mT, 0, mT))) {
													if (block.getType() == Material.SMOOTH_RED_SANDSTONE) {
														block.setType(Material.NETHERRACK);
														if (FastUtils.RANDOM.nextInt(3) == 1) {
															block.setType(Material.MAGMA_BLOCK);
														}
													} else if (block.getType() == Material.SMOOTH_SANDSTONE) {
														block.setType(Material.SMOOTH_RED_SANDSTONE);
													}
												}
												if (mT >= 40) {
													this.cancel();
												}
											}

										}.runTaskTimer(mPlugin, 0, 1);
										new BukkitRunnable() {
											@Override
											public void run() {
												mBoss.setInvulnerable(false);
												mBoss.setAI(true);
												teleport(mSpawnLoc);
												com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(mBoss, PercentDamageReceived.GENERIC_NAME);
												new BukkitRunnable() {
													@Override
													public void run() {
														// If the next phase change has already happened, don't do anything!
														if (getActiveSpells().isEmpty()) {
															changePhase(phase3Spells, phase3PassiveSpells, null);
														}
													}
												}.runTaskLater(mPlugin, 20 * 10);
											}
										}.runTaskLater(mPlugin, 20 * 3);
									}
								}
							}.runTaskTimer(mPlugin, 40, 1);
						}
					}
				}
			}.runTaskLater(mPlugin, 20 * 2);
		});

		// Phase 3.25
		//Summons a Immortal Elemental at 30% HP
		events.put(30, mBoss -> {
			sendDialogue("PRIMORDIAL, RETURN, NOW AS UNDYING AND EVERLASTING AS THE MOUNTAIN.");
			summonImmortal(plugin, world);
		});


		//Force-cast Kaul's Judgement if it hasn't been casted yet.
		events.put(25, mBoss -> {
			// If we get here really fast, we might not actually have the active spells yet
			if (getActiveSpells().isEmpty()) {
				MMLog.warning(() -> "[Kaul] Kaul somehow had no active spells when attempting to force cast Kaul's Judgement " + kaulsJudgement + " at 25% health! Overriding with phase 3 spells");
				changePhase(phase3Spells, phase3PassiveSpells, null);
			}
			forceCastSpell(kaulsJudgement.getClass());
		});

		events.put(10, mBoss -> {
			MMLog.info(() -> "[Kaul] A Kaul fight got to 10% health");
			sendDialogue("THE VALLEY RUNS RED WITH BLOOD TODAY. LET THIS BLASPHEMY END. PREDATORS, FACE THE FULL WILL OF THE JUNGLE. COME.");
			changePhase(phase4Spells, phase4PassiveSpells, null);
			// Force casting Volcanic Demise teleports Kaul back to his camp spot on top of the shrine and whatnot
			// See bossCastAbility() for more info
			forceCastSpell(volcanicDemise.getClass());
		});
		BossBarManager bossBar = new BossBarManager(boss, detectionRange + 30, BarColor.RED, BarStyle.SEGMENTED_10, events);

		//Construct the boss with a delay to prevent the passives from going off during the dialogue
		new BukkitRunnable() {
			@Override
			public void run() {
				constructBoss(phase1Spells, phase1PassiveSpells, detectionRange, bossBar, 20 * 10);

				// Advancements listeners

				mAdvancements.forEach(KaulAdvancementHandler::onBossSpawn);
				new BukkitRunnable() {
					@Override
					public void run() {
						mAdvancements.forEach(KaulAdvancementHandler::onTick);
						if (mDefeated || mBoss.isDead() || !mBoss.isValid()) {
							this.cancel();
						}
					}
				}.runTaskTimer(mPlugin, 0, 1);
			}
		}.runTaskLater(mPlugin, (20 * 10) + 1);
	}

	private void summonImmortal(Plugin plugin, World world) {
		new BukkitRunnable() {
			final Location mLoc = mSpawnLoc;
			double mRotation = 0;
			double mRadius = 5;

			@Override
			public void run() {
				for (int i = 0; i < 5; i++) {
					double radian1 = Math.toRadians(mRotation + (72 * i));
					mLoc.add(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);
					new PartialParticle(Particle.SPELL_WITCH, mLoc, 3, 0.1, 0.1, 0.1, 0).spawnAsBoss();
					new PartialParticle(Particle.BLOCK_DUST, mLoc, 4, 0.2, 0.2, 0.2, 0.25,
						Material.COARSE_DIRT.createBlockData()).spawnAsBoss();
					mLoc.subtract(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);
				}
				mRotation += 8;
				mRadius -= 0.25;
				if (mRadius <= 0) {
					this.cancel();
					world.playSound(mLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 2, 0);
					world.playSound(mLoc, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1, 0.75f);
					new PartialParticle(Particle.CRIT_MAGIC, mLoc, 150, 0.1, 0.1, 0.1, 1).spawnAsBoss();
					LivingEntity miniboss = (LivingEntity) LibraryOfSoulsIntegration.summon(mLoc, immortal);
					MMLog.info(() -> "[Kaul] Kaul has summoned the Immortal Elemental");
					if (miniboss != null) {
						new BukkitRunnable() {
							@Override
							public void run() {
								if (mBoss.isDead() || !mBoss.isValid() || mDefeated) {
									this.cancel();
									if (!miniboss.isDead()) {
										miniboss.setHealth(0);
									}
								}
							}
						}.runTaskTimer(mPlugin, 0, 20);
					}
				}
				if (mBoss.isDead()) {
					this.cancel();
				}
			}
		}.runTaskTimer(plugin, 0, 1);
	}

	private void knockback(Plugin plugin, double r) {
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2, 1);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2, 0.5f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 2, 0f);
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), r, true)) {
			MovementUtils.knockAway(mBoss.getLocation(), player, 0.55f, false);
			EffectType.applyEffect(EffectType.SLOW, player, 5 * 20, 0.3, "KaulKnockbackSlowness", false);
		}
		new BukkitRunnable() {
			double mRotation = 0;
			final Location mLoc = mBoss.getLocation();
			double mRadius = 0;
			double mY = 2.5;
			double mYminus = 0.35;

			@Override
			public void run() {
				mRadius += 1;
				for (int i = 0; i < 15; i += 1) {
					mRotation += 24;
					double radian1 = Math.toRadians(mRotation);
					mLoc.add(FastUtils.cos(radian1) * mRadius, mY, FastUtils.sin(radian1) * mRadius);
					new PartialParticle(Particle.BLOCK_DUST, mLoc, 4, 0.2, 0.2, 0.2, 0.25,
						Material.COARSE_DIRT.createBlockData()).spawnAsBoss();
					new PartialParticle(Particle.SMOKE_LARGE, mLoc, 3, 0.1, 0.1, 0.1, 0.1).spawnAsBoss();
					mLoc.subtract(FastUtils.cos(radian1) * mRadius, mY, FastUtils.sin(radian1) * mRadius);
				}
				mY -= mY * mYminus;
				mYminus += 0.02;
				if (mYminus >= 1) {
					mYminus = 1;
				}
				if (mRadius >= r) {
					this.cancel();
				}
			}
		}.runTaskTimer(plugin, 0, 1);
	}

	private void teleport(Location loc) {
		World world = loc.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);
		new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15).spawnAsBoss();
		new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15).spawnAsBoss();
		new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1).spawnAsBoss();
		mBoss.teleport(loc);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);
		new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15).spawnAsBoss();
		new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15).spawnAsBoss();
		new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1).spawnAsBoss();
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		/* Boss deals AoE damage when melee'ing a player */
		if (event.getType() == DamageType.MELEE && damagee.getLocation().distance(mBoss.getLocation()) <= 2) {
			if (!mCooldown) {
				mCooldown = true;
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> mCooldown = false, 20);
				UUID uuid = damagee.getUniqueId();
				for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), 4, true)) {
					if (!player.getUniqueId().equals(uuid)) {
						BossUtils.blockableDamage(mBoss, player, DamageType.MELEE, event.getDamage());
					}
				}
				World world = mBoss.getWorld();
				new PartialParticle(Particle.DAMAGE_INDICATOR, mBoss.getLocation(), 30, 2, 2, 2, 0.1).spawnAsBoss();
				new PartialParticle(Particle.SWEEP_ATTACK, mBoss.getLocation(), 10, 2, 2, 2, 0.1).spawnAsBoss();
				world.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1, 0);
			}
		}
	}

	@Override
	public void onHurt(DamageEvent event) {
		event.setDamage(event.getFlatDamage() / mDefenseScaling);
	}

	@Override
	public boolean hasNearbyPlayerDeathTrigger() {
		return true;
	}

	@Override
	public void nearbyPlayerDeath(PlayerDeathEvent event) {
		mPlayerCount = getArenaParticipants().size();
		mDefenseScaling = BossUtils.healthScalingCoef(mPlayerCount, SCALING_X, SCALING_Y);

		mAdvancements.forEach(a -> a.onPlayerDeath(event.getPlayer()));
	}

	@Override
	public void bossCastAbility(SpellCastEvent event) {
		Spell spell = event.getSpell();
		if (spell != null && spell.castTicks() > 0) {
			mBoss.setInvulnerable(true);
			com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(mBoss, PercentDamageReceived.GENERIC_NAME);
			com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mBoss, PercentDamageReceived.GENERIC_NAME,
				new PercentDamageReceived(20 * 9999, -1.0));
			mBoss.setAI(false);
			new BukkitRunnable() {
				@Override
				public void run() {
					teleport(mSpawnLoc.clone().add(0, 5, 0));
					new BukkitRunnable() {
						@Override
						public void run() {
							// If the Primordial Elemental is active, don't allow other abilities to turn Kaul's AI back on
							if (!mPrimordialPhase) {
								mBoss.setInvulnerable(false);
								mBoss.setAI(true);
								com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(mBoss, PercentDamageReceived.GENERIC_NAME);
								teleport(mSpawnLoc);
								List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
								if (players.size() > 0) {
									Player newTarget = players.get(FastUtils.RANDOM.nextInt(players.size()));
									((Mob) mBoss).setTarget(newTarget);
								}
							}
						}
					}.runTaskLater(mPlugin, spell.castTicks());
				}
			}.runTaskLater(mPlugin, 1);
		}
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		MMLog.info(() -> "[Kaul] Kaul's death method has been called. If the logs don't contain messages for health events and Primordial/Immortal Elemental, this is a problem!");
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
		if (players.isEmpty()) {
			return;
		}
		mDefeated = true;
		String[] dio = new String[] {
			"AS ALL RETURNS TO ROT, SO TOO HAS THIS ECHO FALLEN.",
			"DO NOT THINK THIS ABSOLVES YOUR BLASPHEMY. RETURN HERE AGAIN, AND YOU WILL PERISH.",
			"NOW... THE JUNGLE... MUST SLEEP..."
		};
		knockback(mPlugin, 10);
		mAdvancements.forEach(KaulAdvancementHandler::onBossDeath);
		changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
		BossUtils.endBossFightEffects(mBoss, players, 20 * 20, true, true);

		World world = mBoss.getWorld();
		for (Entity ent : mSpawnLoc.getNearbyLivingEntities(detectionRange)) {
			if (!ent.getUniqueId().equals(mBoss.getUniqueId()) && ent instanceof WitherSkeleton && !ent.isDead()) {
				ent.remove();
			}
		}
		new BukkitRunnable() {
			final Location mLoc = mShrineMarker.getLocation().subtract(0, 0.5, 0);
			final double mRotation = 0;
			double mRadius = 0;
			int mT = 0;

			@Override
			public void run() {
				mT++;
				mRadius = mT;
				for (int i = 0; i < 36; i++) {
					double radian1 = Math.toRadians(mRotation + (10 * i));
					mLoc.add(FastUtils.cos(radian1) * mRadius, 1, FastUtils.sin(radian1) * mRadius);
					new PartialParticle(Particle.CLOUD, mLoc, 3, 0.25, 0.25, 0.25, 0.025, null).spawnAsBoss();
					new PartialParticle(Particle.VILLAGER_HAPPY, mLoc, 5, 0.4, 0.25, 0.4, 0.25, null).spawnAsBoss();
					mLoc.subtract(FastUtils.cos(radian1) * mRadius, 1, FastUtils.sin(radian1) * mRadius);
				}
				for (Block block : LocationUtils.getEdge(mLoc.clone().subtract(mT, 0, mT), mLoc.clone().add(mT, 0, mT))) {
					if (block.getType() == Material.MAGMA_BLOCK) {
						block.setType(Material.OAK_LEAVES);
						if (FastUtils.RANDOM.nextInt(5) == 1) {
							block.setType(Material.GLOWSTONE);
						}
					} else if (block.getType() == Material.SMOOTH_RED_SANDSTONE || block.getType() == Material.NETHERRACK) {
						block.setType(Material.GRASS_BLOCK);
						if (FastUtils.RANDOM.nextInt(3) == 1) {
							Block b = block.getLocation().add(0, 1.5, 0).getBlock();
							if (!b.getType().isSolid()) {
								b.setType(Material.GRASS);
							}
						}
					}
				}
				if (mT >= 40) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				sendDialogue(dio[mT].toUpperCase(Locale.getDefault()));
				mT++;
				if (mT == dio.length) {
					this.cancel();
					teleport(mSpawnLoc);
					new BukkitRunnable() {
						int mT = 0;

						@Override
						public void run() {
							if (mT <= 0) {
								world.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 10, 1);
							}
							mT++;
							if (mT <= 60) {
								mBoss.teleport(mBoss.getLocation().subtract(0, 0.05, 0));
								new PartialParticle(Particle.BLOCK_DUST, mSpawnLoc, 7, 0.3, 0.1, 0.3, 0.25,
									Material.COARSE_DIRT.createBlockData()).spawnAsBoss();
							} else {
								EntityEquipment equipment = mBoss.getEquipment();
								if (equipment != null) {
									equipment.clear();
								}
								mBoss.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 27, 0));
								mBoss.setAI(false);
								mBoss.setSilent(true);
								mBoss.setInvulnerable(true);
								if (mT >= 100) {
									this.cancel();
									for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
										MessagingUtils.sendBoldTitle(player, Component.text("VICTORY", NamedTextColor.GREEN), Component.text("Kaul, Soul of the Jungle", NamedTextColor.DARK_GREEN));
										player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.HOSTILE, 100, 0.8f);
									}
									mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
									mBoss.remove();
								}
							}
						}
					}.runTaskTimer(mPlugin, 30, 1);
				}
			}
		}.runTaskTimer(mPlugin, 0, 20 * 6);
	}

	@Override
	public void init() {
		MMLog.info(() -> "[Kaul] A Kaul fight has been initialized.");
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, MAX_HEALTH);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, detectionRange);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1);
		mBoss.setHealth(MAX_HEALTH);

		GlowingManager.startGlowing(mBoss, NamedTextColor.DARK_GREEN, -1, GlowingManager.BOSS_SPELL_PRIORITY - 1);

		EntityEquipment equips = mBoss.getEquipment();
		ItemStack[] armorc = equips.getArmorContents();
		ItemStack m = equips.getItemInMainHand();
		ItemStack o = equips.getItemInOffHand();

		// Disable White Tesseract for the duration of the fight. The tag is cleared in SQ login/death files and the win mcfunction
		getArenaParticipants().forEach(player -> player.addScoreboardTag("WhiteTessDisabled"));
		new BukkitRunnable() {

			@Override
			public void run() {
				Objects.requireNonNull(mBoss.getEquipment()).clear();
				mBoss.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 100, 0));
				mBoss.setAI(false);
				mBoss.setSilent(true);
				mBoss.setInvulnerable(true);
				com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mBoss, PercentDamageReceived.GENERIC_NAME,
					new PercentDamageReceived(20 * 9999, -1.0));
				World world = mBoss.getWorld();
				world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 3, 0f);
				new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15).spawnAsBoss();
				new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15).spawnAsBoss();
				new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1).spawnAsBoss();
				String[] dio = new String[] {
					"THE JUNGLE'S WILL IS UNASSAILABLE, YET YOU SCURRY ACROSS MY SHRINE LIKE ANTS.",
					"IS THE DEFILEMENT OF THE DREAM NOT ENOUGH!?"
				};

				new BukkitRunnable() {
					int mT = 0;
					int mIndex = 0;

					@Override
					public void run() {
						if (mT == 0) {
							world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 3, 0f);
						}

						if (mT % (20 * 4) == 0) {
							if (mIndex < dio.length) {
								sendDialogue(dio[mIndex].toUpperCase(Locale.getDefault()));
								mIndex++;
							}
						}
						mT++;

						if (mT >= (20 * 8)) {
							this.cancel();
							mBoss.setAI(true);
							mBoss.setSilent(false);
							mBoss.setInvulnerable(false);
							mBoss.removePotionEffect(PotionEffectType.INVISIBILITY);
							com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(mBoss, PercentDamageReceived.GENERIC_NAME);
							com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mBoss, PercentDamageReceived.GENERIC_NAME,
								new PercentDamageReceived(20 * 9999, -0.2));
							EffectType.applyEffect(EffectType.VANILLA_GLOW, mBoss, 20 * 9999, 0, EffectType.VANILLA_GLOW.getName(), false);
							world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 3, 0f);
							new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15).spawnAsBoss();
							new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15).spawnAsBoss();
							new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1).spawnAsBoss();
							mBoss.getEquipment().setArmorContents(armorc);
							mBoss.getEquipment().setItemInMainHand(m);
							mBoss.getEquipment().setItemInOffHand(o);

							for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
								MessagingUtils.sendBoldTitle(player, Component.text("Kaul", NamedTextColor.DARK_GREEN), Component.text("Soul of the Jungle", NamedTextColor.GREEN));
								player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 2, false, true, true));
							}
							world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 5, 0f);
						}
					}
				}.runTaskTimer(mPlugin, 40, 1);
			}
		}.runTaskLater(mPlugin, 1);
	}

	public Collection<Player> getArenaParticipants() {
		return getArenaParticipantsY(0, ARENA_MAX_Y);
	}

	public Collection<Player> getArenaParticipantsY(int yStart, int yEnd) {
		if (mShrineMarker == null) {
			return Collections.emptyList();
		}

		Location arenaCenter = mShrineMarker.getLocation();
		arenaCenter.setY(yStart);

		// Cylinder from y 0 to 62
		Hitbox hb = new Hitbox.UprightCylinderHitbox(arenaCenter, yEnd, ARENA_WIDTH / 2d);

		return hb.getHitPlayers(true);
	}

	public LivingEntity getBoss() {
		return mBoss;
	}

	public static ChargeUpManager defaultChargeUp(LivingEntity boss, int chargeTime, String spellName) {
		return new ChargeUpManager(boss, chargeTime, Component.text("Charging ", NamedTextColor.GREEN).append(Component.text(spellName + "...", NamedTextColor.DARK_GREEN)), BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10, detectionRange);
	}

	public void sendDialogue(String message) {
		Component component = Component.text(message, NamedTextColor.DARK_GREEN);
		for (Player player : getArenaParticipants()) {
			player.sendMessage(component);
		}
	}

	public void judgementSuccess(Player p) {
		mAdvancements.forEach(a -> a.onJudgementSuccess(p));
	}

	private final List<KaulAdvancementHandler> mAdvancements = Arrays.asList(
		// MLOCI
		new KaulAdvancementHandler() {
			final HashSet<UUID> mHs = new HashSet<>();
			int mTick = 0;

			boolean hasFedora(Player p) {
				ItemStack helmet = p.getInventory().getHelmet();
				if (helmet == null) {
					return false;
				}
				Component name = helmet.getItemMeta().displayName();
				if (name == null) {
					return false;
				}
				return MessagingUtils.plainText(name).equals("Fedora");
			}

			@Override
			void onBossSpawn() {
				getArenaParticipants().forEach(p -> {
					if (hasFedora(p)) {
						mHs.add(p.getUniqueId());
					}
				});
			}

			@Override
			void onTick() {
				mTick++;
				if (mTick % 10 != 0) {
					return;
				}
				getArenaParticipants().forEach(p -> {
					 if (mHs.contains(p.getUniqueId()) && !hasFedora(p)) {
						mHs.remove(p.getUniqueId());
					 }
				});
			}

			@Override
			void onBossDeath() {
				getArenaParticipants().forEach(p -> {
					if (mHs.contains(p.getUniqueId()) && hasFedora(p)) {
						AdvancementUtils.grantAdvancement(p, "monumenta:challenges/r1/kaul/mloci");
					}
				});
			}
		},

		// CELEBRITY
		new KaulAdvancementHandler() {
			int mTick = 0;

			@Override
			void onTick() {
				mTick++;
				if (mTick % 10 == 0 && getArenaParticipantsY(ARENA_MAX_Y, 256).size() >= 15) {
					getArenaParticipants().forEach(p -> AdvancementUtils.grantAdvancement(p, "monumenta:challenges/r1/kaul/celebrity"));
				}
			}
		},

		// SO CLOSE
		new KaulAdvancementHandler() {
			final HashSet<UUID> mHs = new HashSet<>();

			@Override
			void onBossSpawn() {
				getArenaParticipants().forEach(p -> mHs.add(p.getUniqueId()));
			}

			@Override
			void onPlayerDeath(Player p) {
				if (mHs.remove(p.getUniqueId()) && mBoss.getHealth() / MAX_HEALTH <= 0.1) {
					AdvancementUtils.grantAdvancement(p, "monumenta:challenges/r1/kaul/soclose");
				}
			}
		},

		// UNLUCKY
		new KaulAdvancementHandler() {
			private final HashSet<UUID> mSucceededPlayersForAdvancement = new HashSet<>();

			@Override
			void onJudgementSuccess(Player p) {
				// If this is the player's second success, grant achievement. Otherwise mark that they have succeeded once.
				if (mSucceededPlayersForAdvancement.remove(p.getUniqueId())) {
					AdvancementUtils.grantAdvancement(p, "monumenta:challenges/r1/kaul/unlucky");
				} else {
					mSucceededPlayersForAdvancement.add(p.getUniqueId());
				}
			}
		},

		// Vanilla
		new KaulAdvancementHandler() {
		final HashSet<UUID> mHs = new HashSet<>();
		int mTick = 0;

		@Override
		void onBossSpawn() {
			getArenaParticipants().forEach(p -> {
				if (AbilityUtils.isClassless(p)) {
					mHs.add(p.getUniqueId());
				}
			});
		}

		@Override
		void onTick() {
			mTick++;
			if (mTick % 10 != 0) {
				return;
			}
			getArenaParticipants().forEach(p -> {
				if (mHs.contains(p.getUniqueId()) && !AbilityUtils.isClassless(p)) {
					mHs.remove(p.getUniqueId());
				}
			});
		}

		@Override
		void onBossDeath() {
			getArenaParticipants().forEach(p -> {
				if (mHs.contains(p.getUniqueId()) && AbilityUtils.isClassless(p)) {
					AdvancementUtils.grantAdvancement(p, "monumenta:challenges/r1/kaul/vanilla");
				}
			});
		}
	});

	private static class KaulAdvancementHandler {
		void onBossSpawn() {
		}

		void onBossDeath() {
		}

		void onPlayerDeath(Player p) {
		}

		void onTick() {
		}

		void onJudgementSuccess(Player p) {
		}
	}
}
