package com.playmonumenta.plugins.bosses.bosses;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellPurgeNegatives;
import com.playmonumenta.plugins.bosses.spells.frostgiant.ArmorOfFrost;
import com.playmonumenta.plugins.bosses.spells.frostgiant.RingOfFrost;
import com.playmonumenta.plugins.bosses.spells.frostgiant.Shatter;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellAirGolemStrike;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellFrostGiantBlockBreak;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellFrostRift;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellFrostbite;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellFrostedIceBreak;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellGlacialPrison;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellGreatswordSlam;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellHailstorm;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellSpinDown;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellTitanicRupture;
import com.playmonumenta.plugins.bosses.spells.frostgiant.UltimateSeismicRuin;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;

import de.tr7zw.nbtapi.NBTItem;
import net.md_5.bungee.api.ChatColor;

/* WARNING: Basically all the spell info in the comments is outdated.
 * Please use the Frost Giant Formal Write-up for up to date spell descriptions.
 */

/*
 *Frost Giant:
Block Break
Blizzard/Hailstorm - Creates a snowstorm in a circle that is 18 blocks
and beyond that passively deals 5% max health damage every half second
to players are in it and giving them slowness 3 for 2 seconds.

Heavy Blows - Frost Giant deals massive knockback to anyone he hits with
melee attacks and anyone within 4 blocks of that target, also deals 50%
of the damage done to the target to nearby enemies. The frost giant then
swaps targets.

Frostbite - Going above 4 blocks deals 5% max health damage to the player
and giving them slowness 3 for 2 seconds every 0.5s second(s)

Phase 1 Skills:
Shatter - All players within a 70 degree cone in front of the giant after
a 1 second charge up take 24 damage and are knocked back X blocks. If they
collide with a wall they take 10 additional damage and are stunned (Slowness 7,
Negative Jump Boost, weakness 10, maybe putting bows on cooldown, you get the
idea) for 2 seconds.

Glacial Prison - Traps ⅓ players in ice for 3 seconds, after those 3 seconds
the prison explodes dealing 20 damage and giving mining fatigue 3 for 10 seconds
and weakness 2 for 10 seconds.

Whirlwind - The Frost giant gains slowness 2 for 6 seconds while dealing 18
damage and knocking back players slightly if they are within 8 blocks for
those 6 seconds every half second

Phase 2 skills:
Shatter - All players within a 70 degree cone in front of the giant after a
1 second charge up take 24 damage and are knocked back X blocks. If they
collide with a wall they take 10 additional damage and are stunned (Slowness
7, Negative Jump Boost, weakness 10, maybe putting bows on cooldown, you get
the idea) for 2 seconds.

Rush Down - The giant pauses for 1 second, then rushes towards target player
dealing 30 damage to any player it passes through and knocking them up greatly,
once it reaches its destination it deals 12 damage in an 5 block radius.

Shield of Frost - The frost giant gains a shield that absorbs the next
(Same scaling as health scaling function with base 100) damage and applies
slowness 3 and deals knockback to the attacker for 5 seconds. If the shield
expires naturally it explodes dealing 28 damage in an 8 block radius. Expires
after 15 seconds.

Phase 3 Skills:
Shatter - All players within a 70 degree cone in front of the giant after a 1
second charge up take 24 damage and are knocked back X blocks. If they collide
with a wall they take 10 additional damage and are stunned (Slowness 7, Negative
Jump Boost, weakness 10, maybe putting bows on cooldown, you get the idea) for 2 seconds.

Summon the Moon Riders - Summons (Scaling based on players) horseback Archers
with swords in their offhand, if they are dismounted they swap to their swords.

Frost Rift: Targets ⅓  players. Afterwards, breaks the ground towards them,
creating a large rift of ice that deals 20 damage and applies Slowness 2,
Weakness 2, and Wither 3, for 8 seconds. This rift stays in place for 10 seconds.
If this rift collides with a target while rippling through the terrain, they
are knocked back and take 30 damage. This rift continues until it reaches the
edge of the Blizzard/Hailstorm.

Eclipse - The Giant stops moving, becomes invulnerable and causes 5 pulses of
energy to come towards it in a circle that extends 16 blocks out (think earth’s
wrath from Kaul but in reverse) these pulses deal 12 damage. give slowness 5
for 3 seconds and knock the player hit towards the Frost Giant, after the 5 pulses
the Frost Giant explodes dealing 60 damage in an 8 block radius. High CD, High DMG

 */


public class FrostGiant extends BossAbilityGroup {
	public static final String identityTag = "boss_frostgiant";
	public static final int detectionRange = 80;

	//Range of those who are actively in the fight from the center of the arena
	public static final int fighterRange = 36;

	public static FrostGiant mInstance = null;

	//DO NOT USE - boss spawns 42 blocks above actual arena center
	private final Location mSpawnLoc;

	private final Location mEndLoc;
	private boolean mCooldown;
	private static final String START_TAG = "FrostGiantStart";
	//Giants hitboxes are huge as hell. We need a custom melee method
	private double mAttackDamage = 30;
	private LivingEntity mStart;
	private Location mStartLoc;
	private boolean mCutsceneDone = false;

	//If the immune armor is active
	public boolean mFrostArmorActive = true;

	//If true, Frost GIant will stop trying to force target someone
	public boolean mPreventTargetting = false;

	//Directions for Seismic Ruin skill
	private static final String NORTH = "FrostGiantNorth";
	private static final String EAST = "FrostGiantEast";
	private static final String SOUTH = "FrostGiantSouth";
	private static final String WEST = "FrostGiantWest";

	private static final Particle.DustOptions BLUE_COLOR = new Particle.DustOptions(Color.fromRGB(66, 185, 245), 1.0f);
	private static final Particle.DustOptions LIGHT_BLUE_COLOR = new Particle.DustOptions(Color.fromRGB(0, 255, 247), 1.0f);

	private List<Character> mDirections = new ArrayList<>();
	private LivingEntity mNorthStand;
	private LivingEntity mEastStand;
	private LivingEntity mSouthStand;
	private LivingEntity mWestStand;

	private LivingEntity mTargeted;
	private Location mStuckLoc;

	private UltimateSeismicRuin mRuin;

	//Melee does damage
	private boolean mDoDamage = true;

	//Default: 3f, phase 3: 1f, phase 4: 0.5f
	private float mMeleeKnockback = 3f;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new FrostGiant(plugin, boss, spawnLoc, endLoc);
		});
	}

	public ItemStack[] mArmor = null;
	private ItemStack mMainhand = null;
	private ItemStack mOffhand = null;

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public FrostGiant(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;
		mCooldown = false;
		World world = boss.getWorld();
		mBoss.addScoreboardTag("Boss");
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 9999, 0));
		mStart = null;
		mInstance = this;

		//Gets starting position from an armor stand with START_TAG
		//And all icicle armor stands
		//And all directional armor stands
		for (Entity e : EntityUtils.getNearbyMobs(mSpawnLoc.clone().subtract(0, 44, 0), 75, EnumSet.of(EntityType.ARMOR_STAND))) {
			if (e instanceof LivingEntity) {
				Set<String> tags = e.getScoreboardTags();
				for (String tag : tags) {
					switch (tag) {
					default:
						break;
					case START_TAG:
						mStart = (LivingEntity) e;
						break;
					case NORTH:
						mNorthStand = (LivingEntity) e;
						break;
					case EAST:
						mEastStand = (LivingEntity) e;
						break;
					case SOUTH:
						mSouthStand = (LivingEntity) e;
						break;
					case WEST:
						mWestStand = (LivingEntity) e;
						break;
					}
				}
			}
		}

		mStartLoc = mStart.getLocation();

		//Adds directions of the arena that seismic ruin destroys
		mDirections.add('n');
		mDirections.add('e');
		mDirections.add('s');
		mDirections.add('w');

		mRuin = new UltimateSeismicRuin(mPlugin, mBoss, mDirections, mNorthStand, mEastStand, mSouthStand, mWestStand);

		//Prevents the boss from getting set on fire (does not prevent it from getting damaged by inferno)
		new BukkitRunnable() {

			@Override
			public void run() {
				mBoss.setFireTicks(0);
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);

		new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {
				if (!mCutsceneDone || mPreventTargetting) {
					return;
				}

				//Teleports boss back if too far in terms of x, y, or z
				if (mBoss.getLocation().getY() - mStartLoc.getY() < -6 || mStartLoc.distance(mBoss.getLocation()) > 36) {
					teleport(mStartLoc);
					mT = 0;
				}
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}


				if (mT >= 20 * 6 && mBoss.getLocation().distance(mStuckLoc) < 0.5 && ((Creature)mBoss).getTarget() != null) {
					teleport(mStartLoc);
					mT = 0;
				} else if (mStuckLoc == null || mBoss.getLocation().distance(mStuckLoc) > 0.5) {
					mStuckLoc = mBoss.getLocation().clone();
					mT = 0;
				}
				mT += 5;
			}

		}.runTaskTimer(mPlugin, 20 * 5, 5);

		//Custom Attack Method
		//Default Damage for Giant is 50
		//This one does 60% of the player's health and knocks them back
		//The ability also knocks back and damages players nearby
		Creature c = (Creature) mBoss;
		new BukkitRunnable() {

			@Override
			public void run() {
				if (c.getTarget() != null && mCutsceneDone) {
					LivingEntity target = c.getTarget();
					if (target.getBoundingBox().overlaps(mBoss.getBoundingBox().expand(0.5, 0, 0.5)) && !mCooldown && mDoDamage) {
						mCooldown = true;
						new BukkitRunnable() {

							@Override
							public void run() {
								mCooldown = false;
							}

						}.runTaskLater(mPlugin, 20);
						//Damages and knocks back player
						if (target instanceof Player) {
							BossUtils.bossDamagePercent(mBoss, (Player) target, 0.5);
						} else {
							target.damage(mAttackDamage, mBoss);
						}
						//Lessknockback = true for phases 3 and onward
						MovementUtils.knockAway(mBoss.getLocation(), target, mMeleeKnockback, 0.1f, false);

						world.playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.HOSTILE, 2, 0.1f);
						world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_HURT, SoundCategory.HOSTILE, 3, 0.5f);
						world.spawnParticle(Particle.EXPLOSION_NORMAL, target.getLocation(), 50, 2, 0.1, 2, 0.1);
						world.spawnParticle(Particle.LAVA, target.getLocation(), 15, 2, 0.1, 2, 0.1);
						world.spawnParticle(Particle.BLOCK_DUST, target.getLocation(), 40, 2, 0.35, 2, 0.25, Material.COARSE_DIRT.createBlockData());

						//List is farthest players in the beginning, and nearest players at the end
						for (Player p : EntityUtils.getNearestPlayers(mStartLoc, detectionRange)) {
							if (p.getGameMode() == GameMode.SURVIVAL) {
								c.setTarget(p);
								break;
							}
						}
					}
				} else {
					//List is farthest players in the beginning, and nearest players at the end
					for (Player p : EntityUtils.getNearestPlayers(mStartLoc, detectionRange)) {
						if (p.getGameMode() == GameMode.SURVIVAL) {
							c.setTarget(p);
							break;
						}
					}
				}
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
			}

		}.runTaskTimer(plugin, 0, 2);

		//Targetting system
		//Forcefully targets a nearby player if no target
		//After targetting the same player for 30 seconds, play a sound and change targets after 1/2 a second
		new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {
				if (!mCutsceneDone || mPreventTargetting) {
					return;
				}

				if (c.getTarget() == null || !c.getTarget().equals(mTargeted)) {
					mT = 0;
					mTargeted = c.getTarget();

					if (mTargeted instanceof Player) {
						((Player) mTargeted).playSound(mTargeted.getLocation(), Sound.ENTITY_POLAR_BEAR_WARNING, SoundCategory.HOSTILE, 5, 0);
					}
				} else if (mT >= 20 * 30 && c.getTarget().equals(mTargeted)) {
					new BukkitRunnable() {
						int mTicks = 0;
						@Override
						public void run() {
							if (mTicks >= 10) {
								mT = 0;

								//List is farthest players in the beginning, and nearest players at the end
								List<Player> players = EntityUtils.getNearestPlayers(mStartLoc, detectionRange);
								if (players == null || players.size() < 0) {
									return;
								}
								players.removeIf(p -> p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR);
								for (Player player : players) {
									if (!player.getUniqueId().equals(mTargeted.getUniqueId())) {
										c.setTarget(player);
										mTargeted = player;
										break;
									}
								}
								this.cancel();
							} else {
								mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_POLAR_BEAR_WARNING, SoundCategory.HOSTILE, 5, 0);
							}
							mTicks += 5;
						}

					}.runTaskTimer(mPlugin, 0, 5);
				}
				mT += 5;

				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}

			}

		}.runTaskTimer(mPlugin, 0, 5);

		new BukkitRunnable() {
			@Override
			public void run() {
				for (Player player : PlayerUtils.playersInRange(mStartLoc, detectionRange)) {
					if (player.isSleeping() && player.getGameMode() != GameMode.ADVENTURE) {
						BossUtils.bossDamage(mBoss, player, 22);
						player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 15, 1));
						player.sendMessage(ChatColor.DARK_AQUA + "YOU DARE MOCK OUR BATTLE?");
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_DEATH, 1, 0.85f);
					}
				}
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 5);

		SpellManager phase1Spells = new SpellManager(Arrays.asList(
				new SpellAirGolemStrike(mPlugin, mBoss, mStartLoc),
				new Shatter(mPlugin, mBoss, 2.5f),
				new SpellGlacialPrison(mPlugin, mBoss, fighterRange, mStartLoc),
				new RingOfFrost(mPlugin, mBoss, 12, mStartLoc)
				));

		SpellManager phase2Spells = new SpellManager(Arrays.asList(
				new Shatter(mPlugin, mBoss, 2.5f),
				new SpellAirGolemStrike(mPlugin, mBoss, mStartLoc),
				new SpellGreatswordSlam(mPlugin, mBoss, 60, 90),
				new SpellGreatswordSlam(mPlugin, mBoss, 60, 90),
				new SpellSpinDown(mPlugin, mBoss, mStartLoc),
				new SpellSpinDown(mPlugin, mBoss, mStartLoc)
				));

		SpellManager phase3Spells = new SpellManager(Arrays.asList(
				new Shatter(mPlugin, mBoss, 2f),
				new SpellTitanicRupture(mPlugin, mBoss, mStartLoc),
				new SpellFrostRift(mPlugin, mBoss, mStartLoc),
				new SpellGreatswordSlam(mPlugin, mBoss, 60, 90)
				));

		SpellManager phase4Spells = new SpellManager(Arrays.asList(
				new Shatter(mPlugin, mBoss, 1f),
				new SpellTitanicRupture(mPlugin, mBoss, mStartLoc),
				new SpellFrostRift(mPlugin, mBoss, mStartLoc),
				new SpellGreatswordSlam(mPlugin, mBoss, 30, 60)
				));

		List<Spell> phase1PassiveSpells = Arrays.asList(
				new ArmorOfFrost(mPlugin, mBoss, this, 3),
				new SpellPurgeNegatives(mBoss, 20 * 4),
				new SpellFrostGiantBlockBreak(mBoss, 5, 15, 5, mStartLoc),
				new SpellHailstorm(mPlugin, mBoss, 16, mStartLoc),
				new SpellFrostbite(mPlugin, mBoss, mStartLoc)
				);

		List<Spell> phase2PassiveSpells = Arrays.asList(
				new ArmorOfFrost(mPlugin, mBoss, this, 2),
				new SpellPurgeNegatives(mBoss, 20 * 3),
				new SpellFrostGiantBlockBreak(mBoss, 5, 15, 5, mStartLoc),
				new SpellHailstorm(mPlugin, mBoss, 16, mStartLoc),
				new SpellFrostbite(mPlugin, mBoss, mStartLoc),
				new SpellFrostedIceBreak(mBoss)
				);
		List<Spell> phase3PassiveSpells = Arrays.asList(
				new ArmorOfFrost(mPlugin, mBoss, this, 1),
				new SpellPurgeNegatives(mBoss, 20 * 2),
				new SpellFrostGiantBlockBreak(mBoss, 5, 15, 5, mStartLoc),
				new SpellHailstorm(mPlugin, mBoss, 16, mStartLoc),
				new SpellFrostbite(mPlugin, mBoss, mStartLoc),
				new SpellFrostedIceBreak(mBoss)
				);

		List<Spell> phase4PassiveSpells = Arrays.asList(
				new ArmorOfFrost(mPlugin, mBoss, this, 1, false),
				new SpellPurgeNegatives(mBoss, 20 * 2),
				new SpellFrostGiantBlockBreak(mBoss, 5, 15, 5, mStartLoc),
				new SpellHailstorm(mPlugin, mBoss, 16, mStartLoc),
				new SpellFrostbite(mPlugin, mBoss, mStartLoc),
				new SpellFrostedIceBreak(mBoss)
				);

		Map<Integer, BossHealthAction> events = new HashMap<Integer, BossHealthAction>();
		events.put(100, mBoss -> {
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"YOU... SHOULD HAVE NOT COME HERE... PERISH...\",\"color\":\"dark_aqua\"}]");
			mPreventTargetting = false;
			PlayerUtils.executeCommandOnNearbyPlayers(mStartLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"An armor forms around the boss that blocks all damage.\",\"color\":\"aqua\"}]");

			//Changes held weapon to bone wand
			ItemStack wand = new ItemStack(Material.BONE);
			if (mBoss.getEquipment().getItemInMainHand() != null && mBoss.getEquipment().getItemInMainHand().getType() != Material.AIR) {
				wand = mBoss.getEquipment().getItemInMainHand();
			} else {
				wand = modifyItemName(wand, "Frost Giant's Staff", ChatColor.AQUA + "" + ChatColor.BOLD + "" + ChatColor.UNDERLINE);
			}
			ItemMeta im = wand.getItemMeta();
			im.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier("generic.attack_damage", -100, AttributeModifier.Operation.ADD_NUMBER));
			wand.setItemMeta(im);
			mBoss.getEquipment().setItemInMainHand(wand);
		});

		events.put(66, mBoss -> {
			PlayerUtils.executeCommandOnNearbyPlayers(mStartLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"THE FROST WILL CONSUME YOU...\",\"color\":\"dark_aqua\"}]");
			//Manually cancel Armor of Frost's cooldown mechanic
			for (Spell sp : phase1PassiveSpells) {
				if (sp instanceof ArmorOfFrost) {
					((ArmorOfFrost) sp).stopSkill();
					break;
				}
			}
			changePhase(phase2Spells, phase2PassiveSpells, null);
			mFrostArmorActive = true;
			mPreventTargetting = false;
			mBoss.setAI(true);
			mRuin.run();
			teleport(mStartLoc);
			PlayerUtils.executeCommandOnNearbyPlayers(mStartLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"The permafrost shield reforms around the giant, blocking damage dealt once more.\",\"color\":\"aqua\"}]");

			//Changes held weapon to iron sword
			ItemStack sword = modifyItemName(new ItemStack(Material.IRON_SWORD), "Frost Giant's Greatsword", ChatColor.AQUA + "" + ChatColor.BOLD + "" + ChatColor.UNDERLINE);
			ItemMeta im = sword.getItemMeta();
			im.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier("generic.attack_damage", -100, AttributeModifier.Operation.ADD_NUMBER));
			sword.setItemMeta(im);
			mBoss.getEquipment().setItemInMainHand(sword);
		});

		//Phase 3
		events.put(33, mBoss -> {
			mMeleeKnockback = 1;

			PlayerUtils.executeCommandOnNearbyPlayers(mStartLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"THE SONG WILL PREVAIL... ALL WILL SUCCUMB TO THE BITTER COLD...\",\"color\":\"dark_aqua\"}]");
			//Manually cancel Armor of Frost's cooldown mechanic
			for (Spell sp : phase2PassiveSpells) {
				if (sp instanceof ArmorOfFrost) {
					((ArmorOfFrost) sp).stopSkill();
					break;
				}
			}
			changePhase(phase3Spells, phase3PassiveSpells, null);
			mFrostArmorActive = true;
			mPreventTargetting = false;
			mBoss.setAI(true);
			mRuin.run();
			teleport(mStartLoc);
			PlayerUtils.executeCommandOnNearbyPlayers(mStartLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"The permafrost shield once more reforms.\",\"color\":\"aqua\"}]");

			//Changes held weapon to iron axe
			ItemStack axe =  modifyItemName(new ItemStack(Material.IRON_AXE), "Frost Giant's Crusher", ChatColor.AQUA + "" + ChatColor.BOLD + "" + ChatColor.UNDERLINE);
			ItemMeta im = axe.getItemMeta();
			im.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier("generic.attack_damage", -100, AttributeModifier.Operation.ADD_NUMBER));
			axe.setItemMeta(im);
			mBoss.getEquipment().setItemInMainHand(axe);
		});

		//Third and fourth seismic ruin
		events.put(10, mBoss -> {

			PlayerUtils.executeCommandOnNearbyPlayers(mStartLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"I... WILL NOT... BE THE END... OF THE SONG!\",\"color\":\"dark_aqua\"}]");
			mFrostArmorActive = true;
			changePhase(phase4Spells, phase4PassiveSpells, null);
			mBoss.setAI(true);
			for (Spell sp : phase3PassiveSpells) {
				if (sp instanceof ArmorOfFrost) {
					((ArmorOfFrost) sp).stopSkill();
					break;
				}
			}
			mRuin.run();
			mRuin.run();
			teleport(mStartLoc);

			//Changes held weapon to iron scythe
			ItemStack scythe =  modifyItemName(new ItemStack(Material.IRON_HOE), "Frost Giant's Crescent", ChatColor.AQUA + "" + ChatColor.BOLD + "" + ChatColor.UNDERLINE);
			ItemMeta im = scythe.getItemMeta();
			im.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier("generic.attack_damage", -100, AttributeModifier.Operation.ADD_NUMBER));
			scythe.setItemMeta(im);
			mBoss.getEquipment().setItemInMainHand(scythe);
		});

		//Show hailstorm before fight starts
		new BukkitRunnable() {
			Creature mC = (Creature) mBoss;
			@Override
			public void run() {
				Location loc = mStartLoc.clone();
				World world = mBoss.getWorld();
				for (double degree = 0; degree < 360; degree += 8) {
					double radian = Math.toRadians(degree);
					double cos = FastUtils.cos(radian);
					double sin = FastUtils.sin(radian);
					loc.add(cos * (18 + 5), 2.5, sin * (18 + 5));
					world.spawnParticle(Particle.CLOUD, loc, 3, 3, 1, 4, 0.075);
					world.spawnParticle(Particle.CLOUD, loc, 3, 3, 4, 4, 0.075);
					world.spawnParticle(Particle.REDSTONE, loc, 3, 3, 4, 4, 0.075, BLUE_COLOR);
					world.spawnParticle(Particle.REDSTONE, loc, 3, 3, 1, 4, 0.075, BLUE_COLOR);
					loc.subtract(cos * (18 + 5), 2.5, sin * (18 + 5));
				}

				for (double degree = 0; degree < 360; degree++) {
					if (FastUtils.RANDOM.nextDouble() < 0.3) {
						double radian = Math.toRadians(degree);
						double cos = FastUtils.cos(radian);
						double sin = FastUtils.sin(radian);
						loc.add(cos * (18 + 2), 0.5, sin * (18 + 2));
						world.spawnParticle(Particle.REDSTONE, loc, 1, 0, 0, 0, LIGHT_BLUE_COLOR);
						loc.subtract(cos * (18 + 2), 0.5, sin * (18 + 2));
					}
				}

				mC.setTarget(null);
				if (mCutsceneDone) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 10);

		mBoss.setGravity(false);
		mBoss.setAI(false);
		mBoss.setInvulnerable(true);
		world.playSound(mStartLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 6, 0.5f);
		world.playSound(mStartLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 6, 0.5f);
		PlayerUtils.executeCommandOnNearbyPlayers(mStartLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"WHAT CHILDREN OF ALRIC DARE ENTER THIS PLACE...\",\"color\":\"dark_aqua\"}]");
		mBoss.setInvisible(true);


		new BukkitRunnable() {
			double mRadius = 0;
			Location mLoc = mStartLoc.clone();
			List<Player> mPlayers = PlayerUtils.playersInRange(mLoc, detectionRange);
			@Override
			public void run() {
				mRadius += 1.5;
				for (double degree = 0; degree < 360; degree += 5) {
					double radian = Math.toRadians(degree);
					mLoc.add(FastUtils.cos(radian) * mRadius, 1, FastUtils.sin(radian) * mRadius);
					world.spawnParticle(Particle.CLOUD, mLoc, 4, 1, 1, 1, 0.35);
					mLoc.subtract(FastUtils.cos(radian) * mRadius, 1, FastUtils.sin(radian) * mRadius);
				}
				for (Player player : mPlayers) {
					if (player.getLocation().toVector().isInSphere(mLoc.toVector(), mRadius)) {
						player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 9999, 1));
						if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
							player.setGameMode(GameMode.ADVENTURE);
						}
					}
				}
				if (mRadius >= 40) {
					this.cancel();

					//Grow the FG statue using growables
					com.playmonumenta.scriptedquests.Plugin scriptedQuestsPlugin;
					scriptedQuestsPlugin = (com.playmonumenta.scriptedquests.Plugin)Bukkit.getPluginManager().getPlugin("ScriptedQuests");
					try {
						scriptedQuestsPlugin.mGrowableManager.grow("FrostGiantStatue", mStartLoc, 1, 2, false);
					} catch (Exception e) {
						e.printStackTrace();
					}

					new BukkitRunnable() {
						int mTicks = 0;
						int mRotation = 0;
						@Override
						public void run() {
							mTicks += 2;
							if (mTicks >= 20 * 8) {
								this.cancel();
							}

							for (int y = 0; y <= 12; y += 3) {
								double rad1 = Math.toRadians(mRotation);
								Location loc1 = mStartLoc.clone().add(FastUtils.cos(rad1) * 3, y, FastUtils.sin(rad1));
								double rad2 = Math.toRadians(mRotation + 180);
								Location loc2 = mStartLoc.clone().add(FastUtils.cos(rad2) * 3, y, FastUtils.sin(rad2));

								world.spawnParticle(Particle.SPELL_INSTANT, loc1, 5, 0.1, 0.1, 0.1, 0);
								world.spawnParticle(Particle.SPELL_INSTANT, loc2, 5, 0.1, 0.1, 0.1, 0);
							}
							mRotation += 10;
							if (mRotation >= 360) {
								mRotation = 0;
							}

							if (mTicks % 20 == 0) {
								world.playSound(mStartLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 3, 0.25f);
							}

							if (mTicks % 10 == 0) {
								world.spawnParticle(Particle.BLOCK_DUST, mStartLoc, 40, 2, 0.35, 2, 0.25, Material.COARSE_DIRT.createBlockData());
								world.spawnParticle(Particle.BLOCK_DUST, mStartLoc, 75, 5, 0.35, 5, 0.25, Material.COARSE_DIRT.createBlockData());
								world.spawnParticle(Particle.EXPLOSION_NORMAL, mStartLoc, 15, 5, 0.35, 5, 0.15);
								world.spawnParticle(Particle.CLOUD, mStartLoc, 20, 5, 0.35, 5, 0.15);
							}
						}
					}.runTaskTimer(mPlugin, 0, 2);

					new BukkitRunnable() {
						@Override
						public void run() {
							world.playSound(mStartLoc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);


							new BukkitRunnable() {
								double mR = 0;
								Location mLoc = mStartLoc.clone();
								List<Player> mPlayers = PlayerUtils.playersInRange(mLoc, detectionRange);
								@Override
								public void run() {
									if (mR == 0) {
										mBoss.getEquipment().setArmorContents(mArmor);
										mBoss.getEquipment().setItemInMainHand(mMainhand);
										mBoss.getEquipment().setItemInOffHand(mOffhand);
										Location startLoc = mStartLoc;
										Location l = startLoc.clone();
										for (int y = 15; y >= 0; y--) {
											for (int x = -5; x <= 5; x++) {
												for (int z = -5; z <= 5; z++) {
													l.set(startLoc.getX() + x, startLoc.getY() + y, startLoc.getZ() + z);
													l.getBlock().setType(Material.AIR);
												}
											}
										}

										mBoss.removePotionEffect(PotionEffectType.INVISIBILITY);
										mBoss.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
										mCutsceneDone = true;
										world.playSound(mStartLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 6, 0.5f);
										world.playSound(mStartLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 6, 0.5f);
										PlayerUtils.executeCommandOnNearbyPlayers(mStartLoc, detectionRange, "title @s title [\"\",{\"text\":\"Eldrask\",\"color\":\"aqua\",\"bold\":true}]");
										PlayerUtils.executeCommandOnNearbyPlayers(mStartLoc, detectionRange, "title @s subtitle [\"\",{\"text\":\"The Waking Giant\",\"color\":\"blue\",\"bold\":true}]");
										PlayerUtils.executeCommandOnNearbyPlayers(mStartLoc, detectionRange, "playsound minecraft:entity.wither.spawn master @s ~ ~ ~ 10 0.75");
										BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.BLUE, BarStyle.SEGMENTED_10, events, false);
										constructBoss(phase1Spells, phase1PassiveSpells, detectionRange, bossBar, 20 * 10);

										mBoss.setGravity(true);
										mBoss.setAI(true);
										mBoss.setInvulnerable(false);
										mBoss.setInvisible(false);

										mBoss.teleport(mStartLoc);
									}
									mR++;
									for (double degree = 0; degree < 360; degree += 5) {
										double radian = Math.toRadians(degree);
										mLoc.add(FastUtils.cos(radian) * mR, 1, FastUtils.sin(radian) * mR);
										world.spawnParticle(Particle.CLOUD, mLoc, 4, 1, 1, 1, 0.35);
										mLoc.subtract(FastUtils.cos(radian) * mR, 1, FastUtils.sin(radian) * mR);
									}
									for (Player player : mPlayers) {
										if (player.getLocation().toVector().isInSphere(mLoc.toVector(), mR)) {
											if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
												player.setGameMode(GameMode.SURVIVAL);
											}
											player.removePotionEffect(PotionEffectType.SLOW);
										}
									}
									if (mR >= 40) {
										this.cancel();

									}
								}
							}.runTaskTimer(plugin, 20 * 2, 1);
						}
					}.runTaskLater(mPlugin, 20 * 8);
				}
			}
		}.runTaskTimer(mPlugin, 1, 1);
	}

	@Override
	public void death(EntityDeathEvent event) {
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange);
		if (players.size() <= 0) {
			return;
		}
		PlayerUtils.executeCommandOnNearbyPlayers(mStartLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"THIS EARTH... WAS OURS ONCE... WE SHAPED IT...\",\"color\":\"dark_aqua\"}]");

		for (Spell sp : getPassives()) {
			if (sp instanceof ArmorOfFrost) {
				((ArmorOfFrost) sp).stopSkill();
			}
		}

		changePhase(null, null, null);
		mBoss.setHealth(100);
		mBoss.setInvulnerable(true);
		mBoss.setAI(false);
		mBoss.setGravity(false);
		mBoss.setPersistent(true);
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 1000, 10));
		teleport(mStartLoc);
		World world = mBoss.getWorld();

		event.setCancelled(true);
		event.setReviveHealth(100);

		for (Player player : players) {
			player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
			player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 40, 10));
			player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 40, 1));
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.setGameMode(GameMode.ADVENTURE);
			}
		}

		Location loc = mBoss.getLocation().clone();
		for (double degree = 0; degree < 360; degree += 5) {
			double radian = Math.toRadians(degree);
			loc.add(FastUtils.cos(radian), 1, FastUtils.sin(radian));
			world.spawnParticle(Particle.CLOUD, loc, 4, 1, 1, 1, 0.35);
			loc.subtract(FastUtils.cos(radian), 1, FastUtils.sin(radian));
		}

		com.playmonumenta.scriptedquests.Plugin scriptedQuestsPlugin;
		scriptedQuestsPlugin = (com.playmonumenta.scriptedquests.Plugin)Bukkit.getPluginManager().getPlugin("ScriptedQuests");

		new BukkitRunnable() {
			@Override
			public void run() {
				mBoss.remove();

				//Instantly spawn the FG statue
				try {
					scriptedQuestsPlugin.mGrowableManager.grow("FrostGiantStatue", mStartLoc, 1, 300, false);
				} catch (Exception e) {
					e.printStackTrace();
				}

				PlayerUtils.executeCommandOnNearbyPlayers(mStartLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"DO NOT LET IT... PERISH WITH ME... THE SONG MUST NOT GO... UNSUNG...\",\"color\":\"dark_aqua\"}]");
				world.playSound(mStartLoc, Sound.ENTITY_WITHER_DEATH, SoundCategory.HOSTILE, 3, 0);

				//Initiate the growable "melt" which converts the blocks of the giant into barriers
				new BukkitRunnable() {
					@Override
					public void run() {
						try {
							scriptedQuestsPlugin.mGrowableManager.grow("FrostGiantStatueBarrier2", mStartLoc.clone().add(0, 13, 1), 1, 2, false);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}.runTaskLater(mPlugin, 20 * 1);
			}
		}.runTaskLater(mPlugin, 20 * 3);

		new BukkitRunnable() {
			double mRadius = 0;
			Location mLoc = mStartLoc.clone();
			@Override
			public void run() {
				mRadius += 1.5;
				for (double degree = 0; degree < 360; degree += 5) {
					double radian = Math.toRadians(degree);
					mLoc.add(FastUtils.cos(radian) * mRadius, 1, FastUtils.sin(radian) * mRadius);
					world.spawnParticle(Particle.CLOUD, mLoc, 4, 1, 1, 1, 0.35);
					mLoc.subtract(FastUtils.cos(radian) * mRadius, 1, FastUtils.sin(radian) * mRadius);
				}
				if (mRadius >= 40) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		new BukkitRunnable() {
			int mTicks = 0;
			float mPitch = 2f;
			int mRotation = 360;
			//The end totem green particle sequence
			boolean mEndingParticles = false;
			@Override
			public void run() {

				if (mTicks <= 20 * 2) {

					if (mTicks % 10 == 0) {
						world.playSound(mStartLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 0);
					}

					world.spawnParticle(Particle.EXPLOSION_LARGE, mStartLoc.clone().add(0, 5, 0), 1, 1, 5, 1);
				}

				if (mTicks >= 20 * 4 && mTicks <= 20 * 10 && mTicks % 2 == 0) {
					world.playSound(mStartLoc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.HOSTILE, 3, mPitch);

					for (int y = 0; y <= 12; y += 3) {
						double rad1 = Math.toRadians(mRotation);
						Location loc1 = mBoss.getLocation().clone().add(FastUtils.cos(rad1) * 3, y, FastUtils.sin(rad1));
						double rad2 = Math.toRadians(mRotation + 180);
						Location loc2 = mBoss.getLocation().clone().add(FastUtils.cos(rad2) * 3, y, FastUtils.sin(rad2));

						world.spawnParticle(Particle.SPELL_WITCH, loc1, 5, 0.1, 0.1, 0.1, 0);
						world.spawnParticle(Particle.SPELL_WITCH, loc2, 5, 0.1, 0.1, 0.1, 0);
					}
					mRotation -= 20;
					if (mRotation <= 0) {
						mRotation = 360;
					}
				}
				mPitch -= 0.025;

				Location startLoc = mStartLoc;

				if (mTicks >= 20 * 4 && mTicks <= 20 * 10 && mTicks % 10 == 0) {
					world.playSound(mStartLoc, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 2, FastUtils.RANDOM.nextFloat());
					world.playSound(mStartLoc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 2, mPitch);
					world.spawnParticle(Particle.BLOCK_DUST, startLoc, 100, 2, 0.35, 2, 0.25, Material.BLUE_ICE.createBlockData());
					world.spawnParticle(Particle.BLOCK_DUST, startLoc, 100, 2, 0.35, 2, 0.25, Material.IRON_TRAPDOOR.createBlockData());
					world.spawnParticle(Particle.DRAGON_BREATH, startLoc.clone().add(0, 1, 0), 25, 1, 1, 1, 0.25);
				}

				if (!mEndingParticles && mTicks >= 20 * 10 /*&& (startLoc.getBlock().getType() == Material.BARRIER || startLoc.getBlock().getType() == Material.AIR) */) {
					world.spawnParticle(Particle.VILLAGER_HAPPY, startLoc.clone().add(0, 5, 0), 300, 1, 5, 1, 0.25);
					mEndingParticles = true;
				}

				if (mTicks >= 20 * 14) {
					//Delete barriers after cutscene melt
					Location l = startLoc.clone();
					for (int y = 15; y >= 0; y--) {
						for (int x = -5; x <= 5; x++) {
							for (int z = -5; z <= 5; z++) {
								l.set(startLoc.getX() + x, startLoc.getY() + y, startLoc.getZ() + z);
								l.getBlock().setType(Material.AIR);
							}
						}
					}

					this.cancel();
					PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), fighterRange, "playsound minecraft:ui.toast.challenge_complete master @s ~ ~ ~ 100 0.8");
					PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), fighterRange, "title @s title [\"\",{\"text\":\"VICTORY\",\"color\":\"aqua\",\"bold\":true}]");
					PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), fighterRange, "title @s subtitle [\"\",{\"text\":\"Eldrask, The Waking Giant\",\"color\":\"dark_aqua\",\"bold\":true}]");
					mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);

					for (Player player : players) {
						player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
						player.removePotionEffect(PotionEffectType.REGENERATION);
						if (player.getGameMode() != GameMode.CREATIVE) {
							player.setGameMode(GameMode.SURVIVAL);
						}
					}
				}
				mTicks += 1;
			}
		}.runTaskTimer(mPlugin, 1, 1);
	}

	public static boolean testHitByIcicle(BoundingBox icicleBoundingBox) {
		if (mInstance != null && mInstance.mBoss.isValid() && !mInstance.mBoss.isDead()) {
			if (icicleBoundingBox.overlaps(mInstance.mBoss.getBoundingBox())) {
				for (Spell sp : mInstance.getPassives()) {
					if (sp instanceof ArmorOfFrost) {
						((ArmorOfFrost)sp).hitByIcicle();
						return true;
					}
				}
			}
		}
		return false;
	}

	//If cracked = true, convert armor to cracked variant
	//If cracked = false, convert armor to uncracked variant
	public static void changeArmorPhase(EntityEquipment equip, boolean cracked) {
		if (cracked) {
			equip.setChestplate(modifyItemName(equip.getChestplate(), "Cracked Frost Giant's Courage", ChatColor.AQUA + "" + ChatColor.BOLD + ""));
			equip.setLeggings(modifyItemName(equip.getLeggings(), "Cracked Frost Giant's Leggings", ChatColor.AQUA + "" + ChatColor.BOLD + ""));
			equip.setBoots(modifyItemName(equip.getBoots(), "Cracked Frost Giant's Boots", ChatColor.AQUA + "" + ChatColor.BOLD + ""));
		} else {
			equip.setChestplate(modifyItemName(equip.getChestplate(), "Frost Giant's Courage", ChatColor.AQUA + "" + ChatColor.BOLD + ""));
			equip.setLeggings(modifyItemName(equip.getLeggings(), "Frost Giant's Leggings", ChatColor.AQUA + "" + ChatColor.BOLD + ""));
			equip.setBoots(modifyItemName(equip.getBoots(), "Frost Giant's Boots", ChatColor.AQUA + "" + ChatColor.BOLD + ""));
		}
	}

	private static ItemStack modifyItemName(ItemStack item, String newName, String colors) {
		ItemMeta im = item.getItemMeta();
		im.setDisplayName(colors + newName);
		item.setItemMeta(im);

		NBTItem nbti = new NBTItem(item);
		if (nbti.hasKey("plain")) {
			nbti.removeKey("plain");
		}
		nbti.addCompound("plain").addCompound("display").setString("Name", newName);
		return nbti.getItem();
	}

	@Override
	public void entityPotionEffectEvent(EntityPotionEffectEvent event) {
		if (event.getModifiedType() == PotionEffectType.SLOW) {
			if (event.getNewEffect().getAmplifier() > 0) {
				event.getNewEffect().withAmplifier(event.getNewEffect().getAmplifier() - 1);
			} else {
				event.setCancelled(true);
			}
		}
	}

	@Override
	public void bossChangedTarget(EntityTargetEvent event) {
		if (!mCutsceneDone) {
			event.setCancelled(true);
			event.setTarget(null);
		}
	}

	@Override
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof Player && event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
			Player player = (Player) event.getEntity();
			if (player.isBlocking()) {
				player.setCooldown(Material.SHIELD, 20 * 30);
			}
		}
		//The "default" Giant attacks need to be cancelled so it does not trigger evasion
		if (event.getDamage() <= 0) {
			event.setCancelled(true);
		}
	}

	@Override
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		World world = mBoss.getWorld();
		if (event.getDamager() instanceof Mob && !(event.getDamager() instanceof Player)) {
			event.setCancelled(true);
		} else if (!mFrostArmorActive) {
			if (event.getDamager() instanceof Player) {
				((Player)event.getDamager()).playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.HOSTILE, 5, 0.75f);
			} else {
				world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.HOSTILE, 5, 0.75f);
			}
		}
		//Punch resist
		if (event.getDamager() instanceof Projectile) {
			new BukkitRunnable() {
				int mTicks = 0;
				@Override
				public void run() {
					mBoss.setVelocity(new Vector(0, 0, 0));
					mTicks += 1;
					if (mTicks > 2) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);

			Projectile proj = (Projectile) event.getDamager();

			//Check if arrow shot came from arena
			if (proj.getShooter() instanceof Player) {
				Player player = (Player) proj.getShooter();
				if (player.getLocation().distance(mStartLoc) > FrostGiant.fighterRange) {
					event.setCancelled(true);
				}
			}
		}
	}

	//Teleport with special effects
	private void teleport(Location loc) {
		World world = loc.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);
		world.spawnParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15);
		world.spawnParticle(Particle.CLOUD, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15);
		world.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1);
		mBoss.teleport(loc);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);
		world.spawnParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15);
		world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15);
		world.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1);

		//Both abilities delayed by 1.5s
		//Delays damage for melee
		delayDamage();

		//Delays damage for hailstorm
		if (getPassives() != null) {
			for (Spell sp : getPassives()) {
				if (sp instanceof SpellHailstorm) {
					((SpellHailstorm) sp).delayDamage();
				}
			}
		}
	}

	public void delayDamage() {
		mDoDamage = false;
		new BukkitRunnable() {
			@Override
			public void run() {
				mDoDamage = true;
			}
		}.runTaskLater(mPlugin, 30);
	}

	@Override
	public void init() {
		int bossTargetHp = 0;
		int playerCount = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange).size();
		int hpDel = 7012;
		int armor = (int)(Math.sqrt(playerCount * 2) - 1);

		/*
		 * New boss mechanic: The more players there are,
		 * the less invulnerability frames/no damage ticks it has.
		 * Note: A normal mob's maximum NoDamageTicks is 20, with 10 being when it can be damaged.
		 * It's really weird, but regardless, remember that its base will always be 20.
		 */
		int noDamageTicksTake = playerCount / 3;
		if (noDamageTicksTake > 5) {
			noDamageTicksTake = 5;
		}
		mBoss.setMaximumNoDamageTicks(mBoss.getMaximumNoDamageTicks() - noDamageTicksTake);
		bossTargetHp = (int) (hpDel * (1 + (1 - 1/Math.E) * Math.log(playerCount)));
		mBoss.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armor);
		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(bossTargetHp);
		mBoss.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(detectionRange);
		mBoss.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1);
		mBoss.setHealth(bossTargetHp);
		EntityEquipment equips = mBoss.getEquipment();
		mArmor = equips.getArmorContents();
		mMainhand = equips.getItemInMainHand();
		mOffhand = equips.getItemInOffHand();
		mBoss.getEquipment().clear();
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 9999, 0));
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 9999, 10));

		mBoss.setPersistent(true);

	}

	//Punch Resist
	@Override
	public void bossHitByProjectile(ProjectileHitEvent event) {
		mBoss.setVelocity(new Vector(0, 0, 0));
	}

	private static final String GOLEM_FREEZE_EFFECT_NAME = "FrostGiantGolemPercentSpeedEffect";

	//Golem Stun on certain ability casts from boss.
	public static void freezeGolems(LivingEntity mBoss) {
		mBoss.addScoreboardTag("GolemFreeze");
		Location loc = mBoss.getLocation();
		for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, FrostGiant.detectionRange)) {
			if (mob.getType() == EntityType.IRON_GOLEM) {
				com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mob, GOLEM_FREEZE_EFFECT_NAME,
						new PercentSpeed(20 * 20, -1, GOLEM_FREEZE_EFFECT_NAME));
				mob.addPotionEffect((new PotionEffect(PotionEffectType.GLOWING, 200, 10)));
			}
		}
	}

	public static void unfreezeGolems(LivingEntity mBoss) {
		if (mBoss.getScoreboardTags().contains("GolemFreeze")) {
			Location loc = mBoss.getLocation();
			mBoss.removeScoreboardTag("GolemFreeze");
			for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, FrostGiant.detectionRange)) {
				com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(mob, GOLEM_FREEZE_EFFECT_NAME);
				mob.removePotionEffect(PotionEffectType.GLOWING);
			}
		}
	}
}
