package com.playmonumenta.plugins.infinitytower.mobs;

import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.AvengerBoss;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.ForceBoss;
import com.playmonumenta.plugins.bosses.bosses.LaserBoss;
import com.playmonumenta.plugins.bosses.bosses.NovaBoss;
import com.playmonumenta.plugins.bosses.bosses.ProjectileBoss;
import com.playmonumenta.plugins.bosses.bosses.RejuvenationBoss;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.infinitytower.TowerConstants;
import com.playmonumenta.plugins.infinitytower.TowerFileUtils;
import com.playmonumenta.plugins.infinitytower.TowerGame;
import com.playmonumenta.plugins.infinitytower.TowerManager;
import com.playmonumenta.plugins.infinitytower.TowerMob;
import com.playmonumenta.plugins.infinitytower.mobs.abilities.AdvancingShadowTowerAbility;
import com.playmonumenta.plugins.infinitytower.mobs.abilities.AncestralRejuvenationTowerAbility;
import com.playmonumenta.plugins.infinitytower.mobs.abilities.AutoAttackTowerAbility;
import com.playmonumenta.plugins.infinitytower.mobs.abilities.CurseOfTheJungleTowerAbility;
import com.playmonumenta.plugins.infinitytower.mobs.abilities.DarkSummonerTowerAbility;
import com.playmonumenta.plugins.infinitytower.mobs.abilities.EarthquakeTowerAbility;
import com.playmonumenta.plugins.infinitytower.mobs.abilities.EarthsWrathTowerAbility;
import com.playmonumenta.plugins.infinitytower.mobs.abilities.FoolsGoldTowerAbility;
import com.playmonumenta.plugins.infinitytower.mobs.abilities.ForcefulGridTowerAbility;
import com.playmonumenta.plugins.infinitytower.mobs.abilities.GenericTowerMob;
import com.playmonumenta.plugins.infinitytower.mobs.abilities.GoldCacheTowerAbility;
import com.playmonumenta.plugins.infinitytower.mobs.abilities.GreatswordSlamTowerAbility;
import com.playmonumenta.plugins.infinitytower.mobs.abilities.ImmortalTowerAbility;
import com.playmonumenta.plugins.infinitytower.mobs.abilities.MeteorSlamTowerAbility;
import com.playmonumenta.plugins.infinitytower.mobs.abilities.PhylacteryTowerAbility;
import com.playmonumenta.plugins.infinitytower.mobs.abilities.RandomTeleportTowerAbility;
import com.playmonumenta.plugins.infinitytower.mobs.abilities.SvalgotGhalkorTowerAbility;
import com.playmonumenta.plugins.infinitytower.mobs.abilities.TowerAbility;
import com.playmonumenta.plugins.infinitytower.mobs.abilities.VolcanicDemiseTowerAbility;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class TowerMobAbility {

	public static final List<Tuple> ABILITIES = new ArrayList<>();

	static {
		ABILITIES.add(new Tuple(
			"Arcanum Leviosa",
			"Applies a short levitation on hit.",
			null
		));

		ABILITIES.add(new Tuple(
			"Taunt",
			"Taunt enemy hit by this mob attacks",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				if (BossManager.getInstance() != null) {
					BossManager.getInstance().createBossInternal(mob,
						new BossAbilityGroup(TowerManager.mPlugin, "Taunt", mob) {

							@Override
							public void onDamage(DamageEvent event, LivingEntity damagee) {
								if (damagee instanceof Mob mob && damagee.getScoreboardTags().contains(TowerConstants.MOB_TAG)) {
									GenericTowerMob towerMob = BossManager.getInstance().getBoss(mob, GenericTowerMob.class);
									if (towerMob != null) {
										//this should always be true.
										towerMob.mLastTarget = mBoss;
										towerMob.mCanChangeTarget = false;
									}
									mob.setTarget(mBoss);
								}
							}
						}
					);
				}
			}
		));

		ABILITIES.add(new Tuple(
			"Shellcracker",
			"Increases damage taken by 15% to the mob it attacks.",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				if (BossManager.getInstance() != null) {
					BossManager.getInstance().createBossInternal(mob,
							new BossAbilityGroup(TowerManager.mPlugin, "Shellcracker", mob) {

								@Override
								public void onDamage(DamageEvent event, LivingEntity damagee) {
									if (EffectManager.getInstance() != null) {
										EffectManager.getInstance().addEffect(damagee, "TIincreseDamage", new PercentDamageReceived(60, 0.15));
									}
								}

						}
					);
				}
			}
		));

		ABILITIES.add(new Tuple(
			"Jungle's Vengenance",
			"Leaves a lingering damage field on death that deals 2 damage every second to enemies in it, lasting 8 seconds.",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				//On death summmon
				if (BossManager.getInstance() != null) {
					BossManager.getInstance().createBossInternal(mob,
						new TowerAbility(TowerManager.mPlugin, "Jungle's Vengenance", mob, game, towerMob, playerMob) {

							final EntityTargets mTargets = new EntityTargets(EntityTargets.TARGETS.MOB, 5, false, EntityTargets.Limit.DEFAULT, List.of(), new EntityTargets.TagsListFiter(Set.of(mIsPlayerMob ? TowerConstants.MOB_TAG_FLOOR_TEAM : TowerConstants.MOB_TAG_PLAYER_TEAM)));

							@Override
							public void death(@Nullable EntityDeathEvent event) {
								new BukkitRunnable() {
									int mTimer = 0;
									final Location mLocation = mBoss.getLocation();
									final World mWorld = mLocation.getWorld();

									@Override
									public void run() {
										if (mGame.isTurnEnded()) {
											cancel();
											return;
										}

										if (mTimer > 160) {
											cancel();
										}

										if (isCancelled()) {
											return;
										}

										//Summon particles
										new PartialParticle(Particle.REDSTONE, mLocation, 20, 2, 0, 2, 0.05, new DustOptions(Color.RED, 1f)).spawnAsEntityActive(mBoss);

										if (mTimer % 20 == 0) {
											for (LivingEntity target : mTargets.getTargetsListByLocation(mBoss, mLocation)) {
												DamageUtils.damage(mBoss, target, DamageEvent.DamageType.MAGIC, 2, null, true, false);
											}
										}

										mTimer++;
									}
								}.runTaskTimer(TowerManager.mPlugin, 0, 1);

								super.death(event);
							}
						}
					);
				}
			}
		));

		ABILITIES.add(new Tuple(
			"Frost Nova",
			"Unleashes a channeled aoe attack that deals 12 damage around itself and gives 10% slowness for 4s. 6s cooldown",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				//Frost nova ability
				mob.addScoreboardTag(NovaBoss.identityTag);
				if (playerMob) {
					mob.addScoreboardTag(NovaBoss.identityTag + "[" + "targets=[MOB,6,true,limit=(ALL,RANDOM),filters=[],tags=[\"" + TowerConstants.MOB_TAG_FLOOR_TEAM + "\"]]" + "]");
				} else {
					mob.addScoreboardTag(NovaBoss.identityTag + "[" + "targets=[MOB,6,true,limit=(ALL,RANDOM),filters=[],tags=[\"" + TowerConstants.MOB_TAG_PLAYER_TEAM + "\"]]" + "]");
				}
				mob.addScoreboardTag(NovaBoss.identityTag + "[detection=-1,needlineofsight=false,delay=" + ((int) (FastUtils.RANDOM.nextDouble() * 100) + 20) + ",damage=10,duration=80,cooldown=120,effects=[(SLOW,80,0)]]");
				mob.addScoreboardTag(NovaBoss.identityTag + "[soundcharge=ENTITY_SNOWBALL_THROW,soundcast=[(BLOCK_GLASS_BREAK,1.5,0.65)]]");
				mob.addScoreboardTag(NovaBoss.identityTag + "[particleair=[(CLOUD,2,4.5,4.5,4.5,0.05)],particleload=[(SNOWBALL,1,0.25,0.25,0.25,0.1)],particleexplode=[(CLOUD,1,0.1,0.1,0.1,0.3),(SNOWBALL,2,0.25,0.25,0.25,0.1)]]");
				BossManager.createBoss(null, mob, NovaBoss.identityTag);
			}
		));

		ABILITIES.add(new Tuple(
			"Rejuvination",
			"Heals 3 hp to itself and allies close to it every 2s",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				//Frost nova ability
				mob.addScoreboardTag(RejuvenationBoss.identityTag);
				if (!playerMob) {
					mob.addScoreboardTag(RejuvenationBoss.identityTag + "[targets=[MOB,3,false,limit=(ALL,RANDOM),filters=[],tags=[\"" + TowerConstants.MOB_TAG_FLOOR_TEAM + "\"]]]");
				} else {
					mob.addScoreboardTag(RejuvenationBoss.identityTag + "[targets=[MOB,3,false,limit=(ALL,RANDOM),filters=[],tags=[\"" + TowerConstants.MOB_TAG_PLAYER_TEAM + "\"]]]");
				}
				mob.addScoreboardTag(RejuvenationBoss.identityTag + "[detection=-1,delay=" + ((int) (FastUtils.RANDOM.nextDouble() * 20) + 10) + ",heal=3,duration=20,cooldown=40,particleradius=4]");
				BossManager.createBoss(null, mob, RejuvenationBoss.identityTag);
			}
		));

		ABILITIES.add(new Tuple(
			"Meteor Slam",
			"Slams on top of an enemy, dealing 15 aoe damage and knocking up enemies. 8s cooldown",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				//Meteor Slam
				if (BossManager.getInstance() != null) {
					BossManager.getInstance().createBossInternal(mob, new MeteorSlamTowerAbility(TowerManager.mPlugin, "Meteor Slam", mob, game, towerMob, playerMob));
				}
			}
		));

		ABILITIES.add(new Tuple(
			"Mana Arrow",
			"Shoots a projectile that deals 15 damage. 4s cooldown",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				//Mana Arrow ability
				mob.addScoreboardTag(ProjectileBoss.identityTag);
				if (playerMob) {
					mob.addScoreboardTag(ProjectileBoss.identityTag + "[targets=[MOB,30,true,limit=(1,RANDOM),filters=[],tags=[\"" + TowerConstants.MOB_TAG_FLOOR_TEAM + "\"]]]");
				} else {
					mob.addScoreboardTag(ProjectileBoss.identityTag + "[targets=[MOB,30,true,limit=(1,RANDOM),filters=[],tags=[\"" + TowerConstants.MOB_TAG_PLAYER_TEAM + "\"]]]");
				}
				mob.addScoreboardTag(ProjectileBoss.identityTag + "[damage=12,distance=32,speed=0.7,spelldelay=20,delay=" + ((int) (FastUtils.RANDOM.nextDouble() * 40) + 20) + ",cooldown=80,turnradius=0.12,detection=-1]");
				mob.addScoreboardTag(ProjectileBoss.identityTag + "[soundstart=[(ENTITY_FIREWORK_ROCKET_LAUNCH,0.1,1)],soundlaunch=[(ENTITY_FIREWORK_ROCKET_LAUNCH,1,1.5)],soundprojectile=[],soundhit=[(ENTITY_FIREWORK_ROCKET_TWINKLE,0.5,1.5)]]");
				mob.addScoreboardTag(ProjectileBoss.identityTag + "[particlelaunch=[],particleprojectile=[(FIREWORKS_SPARK,5,0.1,0.1,0.1,0.05),(CRIT_MAGIC,20,0.2,0.2,0.2,0.1)],particlehit=[(FIREWORKS_SPARK,30,0,0,0,0.25)]]");
				BossManager.createBoss(null, mob, ProjectileBoss.identityTag);
			}
		));

		ABILITIES.add(new Tuple(
			"Frost Bolt",
			"Shoots a projectile that deals 6 damage and slow by 15%. 4s cooldown",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				//Mana Arrow ability
				mob.addScoreboardTag(ProjectileBoss.identityTag);
				if (playerMob) {
					mob.addScoreboardTag(ProjectileBoss.identityTag + "[targets=[MOB,30,true,limit=(1,RANDOM),filters=[],tags=[\"" + TowerConstants.MOB_TAG_FLOOR_TEAM + "\"]]]");
				} else {
					mob.addScoreboardTag(ProjectileBoss.identityTag + "[targets=[MOB,30,true,limit=(1,RANDOM),filters=[],tags=[\"" + TowerConstants.MOB_TAG_PLAYER_TEAM + "\"]]]");
				}
				mob.addScoreboardTag(ProjectileBoss.identityTag + "[damage=6,distance=48,speed=0.7,spelldelay=20,delay=" + ((int) (FastUtils.RANDOM.nextDouble() * 40) + 20) + ",cooldown=80,turnradius=0.12,detection=-1,effects=[(SLOW,20,0)]]");
				mob.addScoreboardTag(ProjectileBoss.identityTag + "[soundstart=[(ENTITY_FIREWORK_ROCKET_LAUNCH,0.1,1)],soundlaunch=[(ENTITY_FIREWORK_ROCKET_LAUNCH,1,1.5)],soundprojectile=[],soundhit=[(ENTITY_FIREWORK_ROCKET_TWINKLE,0.5,1.5)]]");
				mob.addScoreboardTag(ProjectileBoss.identityTag + "[particlelaunch=[],particleprojectile=[(CLOUD,5,0.1,0.1,0.1,0.05),(CRIT_MAGIC,20,0.2,0.2,0.2,0.1)],particlehit=[(CLOUD,30,0,0,0,0.25)]]");
				BossManager.createBoss(null, mob, ProjectileBoss.identityTag);
			}
		));

		ABILITIES.add(new Tuple(
			"Force",
			"Unleashes an aoe attack that knocks away enemies and slows them by 30% for 6s, 6s cooldown",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				mob.addScoreboardTag(ForceBoss.identityTag);
				if (playerMob) {
					mob.addScoreboardTag(ForceBoss.identityTag + "[targets=[MOB,10,true,limit=(ALL,RANDOM),filters=[],tags=[\"" + TowerConstants.MOB_TAG_FLOOR_TEAM + "\"]]]");
				} else {
					mob.addScoreboardTag(ForceBoss.identityTag + "[targets=[MOB,10,true,limit=(ALL,RANDOM),filters=[],tags=[\"" + TowerConstants.MOB_TAG_PLAYER_TEAM + "\"]]]");
				}
				mob.addScoreboardTag(ForceBoss.identityTag + "[effectsnear=[(pushforce,3.0),(SLOW,120,1)],effectsmiddle=[(pushforce,2.1),(SLOW,120,1)],effectslimit=[(pushforce,1.2),(SLOW,120,1)]]");
				mob.addScoreboardTag(ForceBoss.identityTag + "[detection=-1,delay=" + ((int) (FastUtils.RANDOM.nextDouble() * 100) + 20) + ",needplayers=false,cooldown=120]");
				BossManager.createBoss(null, mob, ForceBoss.identityTag);
			}
		));

		ABILITIES.add(new Tuple(
			"Gold Cache",
			"At the end of the round, remove this unit from play and gain 2 gold.",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				if (BossManager.getInstance() != null) {
					BossManager.getInstance().createBossInternal(mob, new GoldCacheTowerAbility(TowerManager.mPlugin, "Gold Cache", mob, game, towerMob, playerMob));
				}
			}
		));

		ABILITIES.add(new Tuple(
			"Fool's Gold I",
			"At the end of the round, gain 1 extra gold.",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				if (BossManager.getInstance() != null) {
					BossManager.getInstance().createBossInternal(mob, new FoolsGoldTowerAbility(TowerManager.mPlugin, "Fool's Gold I", mob, game, towerMob, playerMob, 1));
				}
			}
		));

		ABILITIES.add(new Tuple(
			"Fool's Gold II",
			"At the end of the round, gain 2 extra gold.",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				//Meteor Slam
				if (BossManager.getInstance() != null) {
					BossManager.getInstance().createBossInternal(mob, new FoolsGoldTowerAbility(TowerManager.mPlugin, "Fool's Gold II\"", mob, game, towerMob, playerMob, 2));
				}
			}
		));

		ABILITIES.add(new Tuple(
			"Fool's Gold IV",
			"At the end of the round, gain 4 extra gold.",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				//Meteor Slam
				if (BossManager.getInstance() != null) {
					BossManager.getInstance().createBossInternal(mob, new FoolsGoldTowerAbility(TowerManager.mPlugin, "Fool's Gold IV", mob, game, towerMob, playerMob, 4));
				}
			}
		));

		ABILITIES.add(new Tuple(
			"Forceful Grip",
			"Throws a hook to the farthest enemy that deals 20 damage, silences for 5s and pulls the target to him, then Varcosa switches aggro to it. 8s cooldown",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				//Frost nova ability
				if (BossManager.getInstance() != null) {
					BossManager.getInstance().createBossInternal(mob, new ForcefulGridTowerAbility(TowerManager.mPlugin, "Forceful Grip", mob, game, towerMob, playerMob));
				}
			}
		));

		ABILITIES.add(new Tuple(
			"Advancing Shadows",
			"Teleports behind the lowest health enemy and teleports again when the target dies",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				//Frost nova ability
				if (BossManager.getInstance() != null) {
					BossManager.getInstance().createBossInternal(mob, new AdvancingShadowTowerAbility(TowerManager.mPlugin, "Advancing Shadows", mob, game, towerMob, playerMob));
				}
			}
		));

		ABILITIES.add(new Tuple(
			"Avenger",
			"Each time a unit on the arena dies, this unit gains 5% attack and movement speed for the rest of the fight and heals 10% of its max hp",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				mob.addScoreboardTag(AvengerBoss.identityTag);
				mob.addScoreboardTag(AvengerBoss.identityTag + "[damagepercentincrement=0.05,speedpercentincrement=0.05,healpercent=0.1,maxstacks=20]");
				BossManager.createBoss(null, mob, AvengerBoss.identityTag);
			}
		));

		ABILITIES.add(new Tuple(
			"Ancestral Rejuvenation",
			"Heals 6 hp to the entire team every 4s",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				if (BossManager.getInstance() != null) {
					BossManager.getInstance().createBossInternal(mob, new AncestralRejuvenationTowerAbility(TowerManager.mPlugin, "Ancestral Rejuvenation", mob, game, towerMob, playerMob, 6.0));
				}
			}
		));


		ABILITIES.add(new Tuple(
			"Curse of The Jungle",
			"Constant AoE 20% slowness, 20% weaken and 1 damage every second debuff all enemy",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				if (BossManager.getInstance() != null) {
					BossManager.getInstance().createBossInternal(mob, new CurseOfTheJungleTowerAbility(TowerManager.mPlugin, "Curse of The Jungle", mob, game, towerMob, playerMob));
				}
			}
		));

		ABILITIES.add(new Tuple(
			"Soulspeaker",
			"Channels a beam to the highest health enemy, and at the end of the channel, it deals 27 damage, 4s cooldown",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				mob.addScoreboardTag(LaserBoss.identityTag);
				if (playerMob) {
					mob.addScoreboardTag(LaserBoss.identityTag + "[targets=[MOB,40,true,limit=(1,HIGHER_HP),filters=[],tags=[\"" + TowerConstants.MOB_TAG_FLOOR_TEAM + "\"]]]");
				} else {
					mob.addScoreboardTag(LaserBoss.identityTag + "[targets=[MOB,40,true,limit=(1,HIGHER_HP),filters=[],tags=[\"" + TowerConstants.MOB_TAG_PLAYER_TEAM + "\"]]]");
				}
				mob.addScoreboardTag(LaserBoss.identityTag + "[particlelaser=[(SMOKE_NORMAL,1,0.02,0.02,0.02,0),(SMOKE_LARGE,1,0.02,0.02,0.02,0),(SPELL_MOB,1,0.02,0.02,0.02,1)]]");
				mob.addScoreboardTag(LaserBoss.identityTag + "[damage=27,cooldown=80,detection=-1,delay=" + ((int) (FastUtils.RANDOM.nextDouble() * 40) + 20) + ",duration=40]");
				BossManager.createBoss(null, mob, LaserBoss.identityTag);
			}
		));

		ABILITIES.add(new Tuple(
			"Dark Summoner",
			"Summons one demon, and it attacks the same targets as this unit. 6s cooldown",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				if (BossManager.getInstance() != null) {
					BossManager.getInstance().createBossInternal(mob, new DarkSummonerTowerAbility(TowerManager.mPlugin, "Dark Summoner", mob, game, towerMob, playerMob));
				}
			}
		));

		ABILITIES.add(new Tuple(
			"Immortal",
			"This unit cannot die until it's the last on the battlefield of it's side. (Other Immortals do not count as units for this ability)",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				if (BossManager.getInstance() != null) {
					BossManager.getInstance().createBossInternal(mob, new ImmortalTowerAbility(TowerManager.mPlugin, "Immortal", mob, game, towerMob, playerMob));
				}
			}
		));

		ABILITIES.add(new Tuple(
			"Earthquake",
			"Summons earthquakes under enemies that deal 16 aoe damage. 5s cooldown",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				if (BossManager.getInstance() != null) {
					BossManager.getInstance().createBossInternal(mob, new EarthquakeTowerAbility(TowerManager.mPlugin, "Earthquake", mob, game, towerMob, playerMob));
				}
			}
		));

		ABILITIES.add(new Tuple(
			"Svalgot & Ghalkor",
			"At the start of the turn, summon Svalgot & Ghalkor",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				if (BossManager.getInstance() != null) {
					BossManager.getInstance().createBossInternal(mob, new SvalgotGhalkorTowerAbility(TowerManager.mPlugin, "Svalgot & Ghalkor", mob, game, towerMob, playerMob));
				}
			}
		));

		ABILITIES.add(new Tuple(
			"The Void Walker",
			"Svalgot summons an orb that moves slowly to a target, that then explodes dealing massive damage in an area based on their max health",
			null
			//fake ability, Svalgot will get this ability when summoned by "Svalgot & Ghalkor"
		));

		ABILITIES.add(new Tuple(
			"The Forgemaster",
			"Ghalkor dashes quickly, leaving a flaming trail on the ground, that damages enemies that pass on it.",
			null
			//fake ability, Ghalkor will get this ability when summoned by "Svalgot & Ghalkor"
		));

		ABILITIES.add(new Tuple(
			"Great Sword Slam",
			"Eldrask jumps and slams the ground with his greatsword, generating a wave that knocks up in a cone enemies in front of him, and then leaving ice on the ground which slows enemies by 30%",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				if (BossManager.getInstance() != null) {
					BossManager.getInstance().createBossInternal(mob, new GreatswordSlamTowerAbility(TowerManager.mPlugin, "Great Sword Slam", mob, game, towerMob, playerMob));
				}
			}
		));

		ABILITIES.add(new Tuple(
			"Void Gazer",
			"Hekawt teleports around instead of moving, dealing 20 damage if it teleports at the location of an enemy.",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				if (BossManager.getInstance() != null) {
					BossManager.getInstance().createBossInternal(mob, new RandomTeleportTowerAbility(TowerManager.mPlugin, "Void Gazer", mob, game, towerMob, playerMob));
				}
			}
		));

		ABILITIES.add(new Tuple(
			"Ancestral Sorcery",
			"Hekawt, instead of using his normal attacks, has 2 unique attacks, depending on the range of the target : Ranged attack shoots a projectile that deals 13 damage and increases damage taken on the target by 10% permanently, hitting the same target again will increase the damage taken further. Melee attack : charges a cone attack in front of him that deals 15 damage and pushes enemies away. 2.5s cooldown.",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				if (BossManager.getInstance() != null) {
					BossManager.getInstance().createBossInternal(mob, new AutoAttackTowerAbility(TowerManager.mPlugin, "Ancestral Sorcery", mob, game, towerMob, playerMob));
				}
			}
		));

		ABILITIES.add(new Tuple(
			"Phylactery",
			"When Hekawt dies, it revives after 6s.",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				if (BossManager.getInstance() != null) {
					BossManager.getInstance().createBossInternal(mob, new PhylacteryTowerAbility(TowerManager.mPlugin, "Pylacetry", mob, game, towerMob, playerMob));
				}
			}
		));


		ABILITIES.add(new Tuple(
			"Earth's Wrath",
			"Generates an aoe attack in every direction that deals 20 damage and roots every mob that goes into it for 3s. ",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				if (BossManager.getInstance() != null) {
					BossManager.getInstance().createBossInternal(mob, new EarthsWrathTowerAbility(TowerManager.mPlugin, "Earth's Wrath", mob, game, towerMob, playerMob));
				}
			}
		));

		ABILITIES.add(new Tuple(
			"Volcanic Demise",
			"Summons meteor strikes that target the entire arena and deal 20 damage, Kaul doesn't attack or move during this attack. cooldown 8s.",
			(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) -> {
				if (BossManager.getInstance() != null) {
					BossManager.getInstance().createBossInternal(mob, new VolcanicDemiseTowerAbility(TowerManager.mPlugin, "Volcanic Demise", mob, game, towerMob, playerMob));
				}
			}
		));


	}


	public final String mName;
	public final String mDescription;
	private final @Nullable BuildMobAbility mBuilder;

	private TowerMobAbility(String name) {
		mName = name;
		for (Tuple tuple : ABILITIES) {
			if (tuple.mName.equals(mName)) {
				mDescription = tuple.mDescription;
				mBuilder = tuple.mBuilder;
				return;
			}
		}

		TowerFileUtils.warning("No ability match for: " + name);
		mDescription = "FAIL! please contact a mod";
		mBuilder = null;
	}

	public void applyAbility(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) {
		if (mBuilder != null) {
			try {
				mBuilder.build(towerMob, mob, game, playerMob);
			} catch (Exception e) {
				TowerFileUtils.warning("Exception during applying ability: " + mName + " Reason:" + e.getMessage());
			}
		}
	}

	public static TowerMobAbility fromString(String name) {
		return new TowerMobAbility(name);
	}


	@FunctionalInterface
	public interface BuildMobAbility {
		void build(TowerMob towerMob, LivingEntity mob, TowerGame game, boolean playerMob) throws Exception;
	}


	public static class Tuple {
		public final String mName;
		public final String mDescription;
		public final @Nullable BuildMobAbility mBuilder;

		public Tuple(String name, String description, @Nullable BuildMobAbility buildAbility) {
			mName = name;
			mDescription = description;
			mBuilder = buildAbility;
		}
	}
}
