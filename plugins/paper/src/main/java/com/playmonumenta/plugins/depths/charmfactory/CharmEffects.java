package com.playmonumenta.plugins.depths.charmfactory;

import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.BottledSunlight;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.DepthsRejuvenation;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.DivineBeam;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.Enlightenment;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.EternalSavior;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.LightningBottle;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.RadiantBlessing;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.SoothingCombos;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.SparkOfInspiration;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.Sundrops;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.WardOfLight;
import com.playmonumenta.plugins.depths.abilities.earthbound.BrambleShell;
import com.playmonumenta.plugins.depths.abilities.earthbound.Bulwark;
import com.playmonumenta.plugins.depths.abilities.earthbound.CrushingEarth;
import com.playmonumenta.plugins.depths.abilities.earthbound.DepthsToughness;
import com.playmonumenta.plugins.depths.abilities.earthbound.EarthenCombos;
import com.playmonumenta.plugins.depths.abilities.earthbound.EarthenWrath;
import com.playmonumenta.plugins.depths.abilities.earthbound.Earthquake;
import com.playmonumenta.plugins.depths.abilities.earthbound.Entrench;
import com.playmonumenta.plugins.depths.abilities.earthbound.IronGrip;
import com.playmonumenta.plugins.depths.abilities.earthbound.Taunt;
import com.playmonumenta.plugins.depths.abilities.flamecaller.Apocalypse;
import com.playmonumenta.plugins.depths.abilities.flamecaller.Detonation;
import com.playmonumenta.plugins.depths.abilities.flamecaller.Fireball;
import com.playmonumenta.plugins.depths.abilities.flamecaller.FlameSpirit;
import com.playmonumenta.plugins.depths.abilities.flamecaller.Flamestrike;
import com.playmonumenta.plugins.depths.abilities.flamecaller.IgneousRune;
import com.playmonumenta.plugins.depths.abilities.flamecaller.PrimordialMastery;
import com.playmonumenta.plugins.depths.abilities.flamecaller.Pyroblast;
import com.playmonumenta.plugins.depths.abilities.flamecaller.Pyromania;
import com.playmonumenta.plugins.depths.abilities.flamecaller.VolcanicCombos;
import com.playmonumenta.plugins.depths.abilities.flamecaller.VolcanicMeteor;
import com.playmonumenta.plugins.depths.abilities.frostborn.Avalanche;
import com.playmonumenta.plugins.depths.abilities.frostborn.Cryobox;
import com.playmonumenta.plugins.depths.abilities.frostborn.DepthsFrostNova;
import com.playmonumenta.plugins.depths.abilities.frostborn.FrigidCombos;
import com.playmonumenta.plugins.depths.abilities.frostborn.FrozenDomain;
import com.playmonumenta.plugins.depths.abilities.frostborn.IceBarrier;
import com.playmonumenta.plugins.depths.abilities.frostborn.IceLance;
import com.playmonumenta.plugins.depths.abilities.frostborn.Icebreaker;
import com.playmonumenta.plugins.depths.abilities.frostborn.Permafrost;
import com.playmonumenta.plugins.depths.abilities.frostborn.PiercingCold;
import com.playmonumenta.plugins.depths.abilities.shadow.BladeFlurry;
import com.playmonumenta.plugins.depths.abilities.shadow.Brutalize;
import com.playmonumenta.plugins.depths.abilities.shadow.ChaosDagger;
import com.playmonumenta.plugins.depths.abilities.shadow.CloakOfShadows;
import com.playmonumenta.plugins.depths.abilities.shadow.DarkCombos;
import com.playmonumenta.plugins.depths.abilities.shadow.DeadlyStrike;
import com.playmonumenta.plugins.depths.abilities.shadow.DepthsAdvancingShadows;
import com.playmonumenta.plugins.depths.abilities.shadow.DepthsDethroner;
import com.playmonumenta.plugins.depths.abilities.shadow.DummyDecoy;
import com.playmonumenta.plugins.depths.abilities.shadow.EscapeArtist;
import com.playmonumenta.plugins.depths.abilities.shadow.PhantomForce;
import com.playmonumenta.plugins.depths.abilities.shadow.ShadowSlam;
import com.playmonumenta.plugins.depths.abilities.steelsage.DepthsSharpshooter;
import com.playmonumenta.plugins.depths.abilities.steelsage.DepthsSplitArrow;
import com.playmonumenta.plugins.depths.abilities.steelsage.DepthsVolley;
import com.playmonumenta.plugins.depths.abilities.steelsage.FireworkBlast;
import com.playmonumenta.plugins.depths.abilities.steelsage.FocusedCombos;
import com.playmonumenta.plugins.depths.abilities.steelsage.GravityBomb;
import com.playmonumenta.plugins.depths.abilities.steelsage.PrecisionStrike;
import com.playmonumenta.plugins.depths.abilities.steelsage.RapidFire;
import com.playmonumenta.plugins.depths.abilities.steelsage.Scrapshot;
import com.playmonumenta.plugins.depths.abilities.steelsage.Sidearm;
import com.playmonumenta.plugins.depths.abilities.steelsage.SteelStallion;
import com.playmonumenta.plugins.depths.abilities.windwalker.Aeroblast;
import com.playmonumenta.plugins.depths.abilities.windwalker.Aeromancy;
import com.playmonumenta.plugins.depths.abilities.windwalker.DepthsDodging;
import com.playmonumenta.plugins.depths.abilities.windwalker.DepthsWindWalk;
import com.playmonumenta.plugins.depths.abilities.windwalker.GuardingBolt;
import com.playmonumenta.plugins.depths.abilities.windwalker.LastBreath;
import com.playmonumenta.plugins.depths.abilities.windwalker.OneWithTheWind;
import com.playmonumenta.plugins.depths.abilities.windwalker.RestoringDraft;
import com.playmonumenta.plugins.depths.abilities.windwalker.Skyhook;
import com.playmonumenta.plugins.depths.abilities.windwalker.ThundercloudForm;
import com.playmonumenta.plugins.depths.abilities.windwalker.Whirlwind;
import com.playmonumenta.plugins.depths.abilities.windwalker.WindsweptCombos;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

public enum CharmEffects {

	// Dawnbringer
	BOTTLED_SUNLIGHT_COOLDOWN(BottledSunlight.CHARM_COOLDOWN, BottledSunlight.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	BOTTLED_SUNLIGHT_ABSORPTION_HEALTH("Bottled Sunlight Absorption Health", BottledSunlight.INFO, true, false, 1.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	BOTTLED_SUNLIGHT_ABSORPTION_DURATION("Bottled Sunlight Absorption Duration", BottledSunlight.INFO, true, false, 2.0, 20.0, new double[] {2.0, 4.0, 6.0, 8.0, 10.0}),
	BOTTLED_SUNLIGHT_BOTTLE_VELOCITY("Bottled Sunlight Bottle Velocity", BottledSunlight.INFO, true, true, 0.0, 100.0, new double[] {0.0, 0.0, 20.0, 40.0, 60.0}),
	DIVINE_BEAM_COOLDOWN(DivineBeam.CHARM_COOLDOWN, DivineBeam.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	DIVINE_BEAM_SIZE("Divine Beam Size", DivineBeam.INFO, true, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	DIVINE_BEAM_HEALING("Divine Beam Healing", DivineBeam.INFO, true, true, 5, 50, new double[] {5, 10, 15, 20, 25}),
	DIVINE_BEAM_STUN_DURATION("Divine Beam Stun Duration", DivineBeam.INFO, true, false, 0.25, 2.0, new double[] {0.25, 0.5, 0.75, 1.0, 1.25}),
	DIVINE_BEAM_MAX_TARGETS_BONUS("Divine Beam Max Targets Bonus", DivineBeam.INFO, true, false, 0.0, 5.0, new double[] {0.0, 0.0, 1.0, 2.0, 3.0}),
	DIVINE_BEAM_MAX_ABSORPTION("Divine Beam Max Absorption", DivineBeam.INFO, true, false, 1.0, 6.0, new double[] {0.0, 0.0, 1.0, 2.0, 3.0}),
	DIVINE_BEAM_ABSORPTION_DURATION("Divine Beam Absorption Duration", DivineBeam.INFO, true, false, 1.0, 10.0, new double[] {0.0, 0.0, 2.0, 4.0, 6.0}),
	ENLIGHTENMENT_XP_MULTIPLIER("Enlightenment Experience Multiplier", Enlightenment.INFO, true, true, 5.0, 50.0, new double[] {5, 10, 15, 20, 25}),
	ENLIGHTENMENT_RARITY_INCREASE("Enlightenment Rarity Increase Chance", Enlightenment.INFO, true, true, 1.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	ETERNAL_SAVIOR_COOLDOWN(EternalSavior.CHARM_COOLDOWN, EternalSavior.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	ETERNAL_SAVIOR_HEALING("Eternal Savior Healing", EternalSavior.INFO, true, true, 5.0, 50.0, new double[] {5, 10, 15, 20, 25}),
	ETERNAL_SAVIOR_RADIUS("Eternal Savior Radius", EternalSavior.INFO, false, true, 5.0, 50.0, new double[] {10, 15, 20, 25, 30}),
	ETERNAL_SAVIOR_ABSORPTION("Eternal Savior Absorption", EternalSavior.INFO, false, false, 1.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	ETERNAL_SAVIOR_ABSORPTION_DURATION("Eternal Savior Absorption Duration", EternalSavior.INFO, false, false, 2.0, 20.0, new double[] {2.0, 4.0, 6.0, 8.0, 10.0}),
	ETERNAL_SAVIOR_STUN_DURATION("Eternal Savior Stun Duration", EternalSavior.INFO, true, false, 0.25, 2.0, new double[] {0.25, 0.5, 0.75, 1.0, 1.25}),
	LIGHTNING_BOTTLE_DAMAGE("Lightning Bottle Damage", LightningBottle.INFO, false, true, 5.0, 50.0, new double[] {5.0, 7.5, 10.0, 12.5, 15.0}),
	LIGHTNING_BOTTLE_RADIUS("Lightning Bottle Radius", LightningBottle.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	LIGHTNING_BOTTLE_SLOW_AMPLIFIER("Lightning Bottle Slowness Amplifier", LightningBottle.INFO, false, true, 2.0, 15.0, new double[] {4.0, 5.0, 6.0, 7.0, 8.0}),
	LIGHTNING_BOTTLE_VULN_AMPLIFIER("Lightning Bottle Vulnerability Amplifier", LightningBottle.INFO, false, true, 2.0, 15.0, new double[] {4.0, 5.0, 6.0, 7.0, 8.0}),
	LIGHTNING_BOTTLE_DURATION("Lightning Bottle Debuff Duration", LightningBottle.INFO, true, false, 0.5, 5.0, new double[] {0.5, 1.0, 1.5, 2.0, 2.5}),
	LIGHTNING_BOTTLE_MAX_STACKS("Lightning Bottle Max Stacks", LightningBottle.INFO, true, false, 1.0, 10.0, new double[] {0.0, 0.0, 2.0, 4.0, 6.0}),
	LIGHTNING_BOTTLE_KILLS_PER_BOTTLE("Lightning Bottle Kill Threshold", LightningBottle.INFO, true, false, 0.0, -1.0, new double[] {0.0, 0.0, 0.0, 0.0, -1.0}),
	RADIANT_BLESSING_COOLDOWN(RadiantBlessing.CHARM_COOLDOWN, RadiantBlessing.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	RADIANT_BLESSING_RADIUS("Radiant Blessing Radius", RadiantBlessing.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	RADIANT_BLESSING_DAMAGE_AMPLIFIER("Radiant Blessing Strength Amplifier", RadiantBlessing.INFO, false, true, 2.0, 20.0, new double[] {2.4, 3.6, 4.8, 6.0, 7.2}),
	RADIANT_BLESSING_RESISTANCE_AMPLIFIER("Radiant Blessing Resistance Amplifier", RadiantBlessing.INFO, true, true, 2.0, 15.0, new double[] {0.0, 0.0, 2.0, 4.0, 6.0}),
	RADIANT_BLESSING_BUFF_DURATION("Radiant Blessing Buff Duration", RadiantBlessing.INFO, true, false, 0.5, 5.0, new double[] {0.5, 1.0, 1.5, 2.0, 2.5}),
	REJUVENATION_HEAL_RADIUS("Rejuvenation Radius", DepthsRejuvenation.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	REJUVENATION_HEALING("Rejuvenation Healing", DepthsRejuvenation.INFO, true, true, 20, 100, new double[] {0.0, 0.0, 20, 40, 60}),
	SOOTHING_COMBOS_HIT_REQUIREMENT("Soothing Combos Hit Requirement", SoothingCombos.INFO, true, false, 0.0, -1.0, new double[] {0.0, 0.0, 0.0, 0.0, -1.0}),
	SOOTHING_COMBOS_RANGE("Soothing Combos Range", SoothingCombos.INFO, false, true, 5, 50, new double[] {10, 15, 20, 25, 30}),
	SOOTHING_COMBOS_HEALING("Soothing Combos Healing", SoothingCombos.INFO, true, true, 5, 50, new double[] {5, 10, 15, 20, 25}),
	SOOTHING_COMBOS_SPEED_AMPLIFIER("Soothing Combos Speed Amplifier", SoothingCombos.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	SOOTHING_COMBOS_HASTE_LEVEL("Soothing Combos Haste Level", SoothingCombos.INFO, true, false, 0.0, 1.0, new double[] {0.0, 0.0, 0.0, 0.0, 1.0}),
	SOOTHING_COMBOS_DURATION("Soothing Combos Buff Duration", SoothingCombos.INFO, true, false, 1.0, 5.0, new double[] {0.5, 1.0, 1.5, 2.0, 2.5}),
	SPARK_OF_INSPIRATION_COOLDOWN(SparkOfInspiration.CHARM_COOLDOWN, SparkOfInspiration.INFO, false, true, 4.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	SPARK_OF_INSPIRATION_RANGE("Spark of Inspiration Cast Range", SparkOfInspiration.INFO, true, true, 5.0, 50.0, new double[] {10, 15, 20, 25, 30}),
	SPARK_OF_INSPIRATION_CDR("Spark of Inspiration Cooldown Reduction Rate", SparkOfInspiration.INFO, false, true, 3.0, 40.0, new double[] {6.0, 9.0, 12.0, 15.0, 18.0}),
	SPARK_OF_INSPIRATION_STRENGTH("Spark of Inspiration Strength Amplifier", SparkOfInspiration.INFO, false, true, 2.0, 20.0, new double[] {3.0, 4.5, 6.0, 7.5, 9.0}),
	SPARK_OF_INSPIRATION_BUFF_DURATION("Spark of Inspiration Buff Duration", SparkOfInspiration.INFO, true, false, 0.5, 3.0, new double[] {0, 0, 0.5, 1.0, 1.5}),
	SPARK_OF_INSPIRATION_RESIST_DURATION("Spark of Inspiration Resistance Duration", SparkOfInspiration.INFO, true, false, 0.0, 2.0, new double[] {0, 0, 0.5, 0.75, 1.0}),
	SUNDROPS_DROP_CHANCE("Sundrops Drop Chance", Sundrops.INFO, true, true, 5.0, 30.0, new double[] {3.0, 6.0, 9.0, 12.0, 15.0}),
	SUNDROPS_LINGER_TIME("Sundrops Linger Time", Sundrops.INFO, true, false, 2.0, 20.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	SUNDROPS_SPEED_AMPLIFIER("Sundrops Speed Amplifier", Sundrops.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	SUNDROPS_RESISTANCE_AMPLIFIER("Sundrops Resistance Amplifier", Sundrops.INFO, true, true, 3.0, 15.0, new double[] {0.0, 0.0, 0.0, 4.0, 8.0}),
	SUNDROPS_EFFECT_DURATION("Sundrops Buff Duration", Sundrops.INFO, true, false, 1.0, 8.0, new double[] {0.5, 1.0, 1.5, 2.0, 2.5}),
	WARD_OF_LIGHT_COOLDOWN(WardOfLight.CHARM_COOLDOWN, WardOfLight.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	WARD_OF_LIGHT_HEALING("Ward of Light Healing", WardOfLight.INFO, true, true, 5, 50, new double[] {5, 10, 15, 20, 25}),
	WARD_OF_LIGHT_HEAL_RADIUS("Ward of Light Heal Radius", WardOfLight.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	WARD_OF_LIGHT_CONE_ANGLE("Ward of Light Cone Angle", WardOfLight.INFO, true, true, 0, 100.0, new double[] {0.0, 0.0, 20.0, 40.0, 60.0}),

	// Earthbound
	BRAMBLE_SHELL_DAMAGE("Bramble Shell Damage", BrambleShell.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	BULWARK_COOLDOWN(Bulwark.CHARM_COOLDOWN, Bulwark.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	CRUSHING_EARTH_COOLDOWN(CrushingEarth.CHARM_COOLDOWN, CrushingEarth.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	CRUSHING_EARTH_DAMAGE("Crushing Earth Damage", CrushingEarth.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	CRUSHING_EARTH_RANGE("Crushing Earth Range", CrushingEarth.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	CRUSHING_EARTH_STUN_DURATION("Crushing Earth Stun Duration", CrushingEarth.INFO, true, false, 0.25, 2.0, new double[] {0.25, 0.5, 0.75, 1.0, 1.25}),
	EARTHEN_COMBOS_HIT_REQUIREMENT("Earthen Combos Hit Requirement", EarthenCombos.INFO, true, false, 0.0, -1.0, new double[] {0.0, 0.0, 0.0, 0.0, -1.0}),
	EARTHEN_COMBOS_RESISTANCE_AMPLIFIER("Earthen Combos Resistance Amplifier", EarthenCombos.INFO, true, true, 2.0, 15.0, new double[] {2.0, 4.0, 6.0, 8.0, 10.0}),
	EARTHEN_COMBOS_EFFECT_DURATION("Earthen Combos Buff Duration", EarthenCombos.INFO, true, false, 0.5, 5.0, new double[] {0.5, 1.0, 1.5, 2.0, 2.5}),
	EARTHEN_COMBOS_ROOT_DURATION("Earthen Combos Root Duration", EarthenCombos.INFO, true, false, 0.25, 2.0, new double[] {0.25, 0.5, 0.75, 1.0, 1.25}),
	EARTHEN_WRATH_COOLDOWN(EarthenWrath.CHARM_COOLDOWN, EarthenWrath.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	EARTHEN_WRATH_DAMAGE_REFLECTED("Earthen Wrath Damage Reflected", EarthenWrath.INFO, true, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	EARTHEN_WRATH_DAMAGE_REDUCTION("Earthen Wrath Damage Reduction", EarthenWrath.INFO, true, true, 2.0, 15.0, new double[] {2.0, 4.0, 6.0, 8.0, 10.0}),
	EARTHEN_WRATH_TRANSFER_RADIUS("Earthen Wrath Transfer Radius", EarthenWrath.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	EARTHEN_WRATH_RADIUS("Earthen Wrath Radius", EarthenWrath.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	EARTHEN_WRATH_DURATION("Earthen Wrath Duration", EarthenWrath.INFO, true, false, 0.5, 2.0, new double[] {0.0, 0.0, 0.0, 0.0, 1.5}),
	EARTHQUAKE_COOLDOWN(Earthquake.CHARM_COOLDOWN, Earthquake.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	EARTHQUAKE_DAMAGE("Earthquake Damage", Earthquake.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	EARTHQUAKE_RADIUS("Earthquake Radius", Earthquake.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	EARTHQUAKE_KNOCKBACK("Earthquake Knockback", Earthquake.INFO, true, true, 10, 100, new double[] {0.0, 0.0, 0.0, 20, 40}),
	EARTHQUAKE_SILENCE_DURATION("Earthquake Silence Duration", Earthquake.INFO, true, false, 0.5, 10.0, new double[] {0.5, 1.0, 1.5, 2.0, 2.5}),
	ENTRENCH_RADIUS("Entrench Radius", Entrench.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	ENTRENCH_ROOT_DURATION("Entrench Root Duration", Entrench.INFO, true, false, 0.25, 2.0, new double[] {0.25, 0.5, 0.75, 1.0, 1.25}),
	IRON_GRIP_COOLDOWN(IronGrip.CHARM_COOLDOWN, IronGrip.INFO, false, true, 3.0, -30.0, new double [] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	IRON_GRIP_DAMAGE("Iron Grip Damage", IronGrip.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	IRON_GRIP_RADIUS("Iron Grip Radius", IronGrip.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	IRON_GRIP_CAST_RANGE("Iron Grip Cast Range", IronGrip.INFO, true, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	IRON_GRIP_ROOT_DURATION("Iron Grip Root Duration", IronGrip.INFO, true, false, 0.25, 2.0, new double[] {0.0, 0.0, 0.75, 1.0, 1.25}),
	IRON_GRIP_RESIST_AMPLIFIER("Iron Grip Resistance Amplifier", IronGrip.INFO, true, true, 2.0, 15.0, new double[] {1.5, 3.0, 4.5, 6.0, 7.5}),
	IRON_GRIP_RESIST_DURATION("Iron Grip Resistance Duration", IronGrip.INFO, true, false, 1.0, 5.0, new double[] {0.5, 1.0, 1.5, 2.0, 2.5}),
	TAUNT_COOLDOWN(Taunt.CHARM_COOLDOWN, Taunt.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	TAUNT_DAMAGE_BONUS("Taunt Damage Bonus", Taunt.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	TAUNT_RANGE("Taunt Range", Taunt.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	TAUNT_ABSORPTION_PER_MOB("Taunt Absorption Per Mob", Taunt.INFO, true, false, 0.1, 1.0, new double[] {0.1, 0.2, 0.3, 0.4, 0.5}),
	TAUNT_MAX_ABSORPTION_MOBS("Taunt Max Absorption Mobs", Taunt.INFO, true, false, 0.0, 5.0, new double[] {0.0, 1.0, 2.0, 3.0, 4.0}),
	TAUNT_ABSORPTION_DURATION("Taunt Absorption Duration", Taunt.INFO, true, false, 1.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	TOUGHNESS_MAX_HEALTH("Toughness Max Health", DepthsToughness.INFO, true, true, 3.0, 20.0, new double[] {2.0, 4.0, 6.0, 8.0, 10.0}),

	// Flamecaller
	APOCALYPSE_COOLDOWN(Apocalypse.CHARM_COOLDOWN, Apocalypse.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	APOCALYPSE_DAMAGE("Apocalypse Damage", Apocalypse.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	APOCALYPSE_RADIUS("Apocalypse Radius", Apocalypse.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	APOCALYPSE_HEALING("Apocalypse Healing", Apocalypse.INFO, true, true, 5.0, 50, new double[] {5, 10, 15, 20, 25}),
	APOCALYPSE_MAX_ABSORPTION("Apocalypse Max Absorption", Apocalypse.INFO, true, true, 5.0, 30.0, new double[] {0.0, 0.0, 0.0, 10.0, 20.0}),
	DETONATION_DAMAGE("Detonation Damage", Detonation.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	DETONATION_DAMAGE_RADIUS("Detonation Damage Radius", Detonation.INFO, false, true, 5.0, 50.0, new double[] {0.0, 0.0, 20.0, 25.0, 30.0}),
	DETONATION_DEATH_RADIUS("Detonation Death Radius", Detonation.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	FIREBALL_COOLDOWN(Fireball.CHARM_COOLDOWN, Fireball.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	FIREBALL_DAMAGE("Fireball Damage", Fireball.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	FIREBALL_RADIUS("Fireball Radius", Fireball.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	FIREBALL_VELOCITY("Fireball Velocity", Fireball.INFO, true, true, 10.0, 100.0, new double[] {10.0, 20.0, 30.0, 40.0, 50.0}),
	FIREBALL_FIRE_DURATION("Fireball Fire Duration", Fireball.INFO, false, false, 0.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	FLAME_SPIRIT_DAMAGE("Flame Spirit Damage", FlameSpirit.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	FLAME_SPIRIT_RADIUS("Flame Spirit Radius", FlameSpirit.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	FLAME_SPIRIT_FIRE_DURATION("Flame Spirit Fire Duration", FlameSpirit.INFO, false, false, 0.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	FLAME_SPIRIT_DURATION("Flame Spirit Duration", FlameSpirit.INFO, true, false, 0.0, 4.0, new double[] {0.0, 0.0, 0.0, 1.0, 2.0}),
	FLAMESTRIKE_COOLDOWN(Flamestrike.CHARM_COOLDOWN, Flamestrike.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	FLAMESTRIKE_DAMAGE("Flamestrike Damage", Flamestrike.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	FLAMESTRIKE_RANGE("Flamestrike Range", Flamestrike.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	FLAMESTRIKE_CONE_ANGLE("Flamestrike Cone Angle", Flamestrike.INFO, true, true, 0.0, 100.0, new double[] {0.0, 0.0, 20.0, 40.0, 60.0}),
	FLAMESTRIKE_FIRE_DURATION("Flamestrike Fire Duration", Flamestrike.INFO, false, false, 0.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	FLAMESTRIKE_KNOCKBACK("Flamestrike Knockback", Flamestrike.INFO, true, true, 0.0, 100.0, new double[] {0.0, 0.0, 20.0, 40.0, 60.0}),
	IGNEOUS_RUNE_COOLDOWN(IgneousRune.CHARM_COOLDOWN, IgneousRune.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	IGNEOUS_RUNE_DAMAGE("Igneous Rune Damage", IgneousRune.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	IGNEOUS_RUNE_RADIUS("Igneous Rune Radius", IgneousRune.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	IGNEOUS_RUNE_FIRE_DURATION("Igneous Rune Fire Duration", IgneousRune.INFO, false, false, 0.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	IGNEOUS_RUNE_BUFF_AMPLIFIER("Igneous Rune Buff Amplifier", IgneousRune.INFO, true, true, 2.0, 20.0, new double[] {2.0, 4.0, 6.0, 8.0, 10.0}),
	IGNEOUS_RUNE_BUFF_DURATION("Igneous Rune Buff Duration", IgneousRune.INFO, true, false, 0.5, 6.0, new double[] {1.0, 1.5, 2.0, 2.5, 3.0}),
	IGNEOUS_RUNE_ARMING_TIME("Igneous Rune Arming Time", IgneousRune.INFO, true, false, 0.0, -1.0, new double[] {0.0, 0.0, 0.0, -0.5, -1.0}),
	PRIMORDIAL_MASTERY_DAMAGE_MODIFIER("Primordial Mastery Damage Multiplier", PrimordialMastery.INFO, true, true, 2.0, 10.0, new double[] {1.0, 2.5, 4.0, 5.5, 7.5}),
	PYROBLAST_COOLDOWN(Pyroblast.CHARM_COOLDOWN, Pyroblast.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	PYROBLAST_DAMAGE("Pyroblast Damage", Pyroblast.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	PYROBLAST_RADIUS("Pyroblast Radius", Pyroblast.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	PYROBLAST_FIRE_DURATION("Pyroblast Fire Duration", Pyroblast.INFO, false, false, 0.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	PYROMANIA_DAMAGE_PER_MOB("Pyromania Damage Per Mob", Pyromania.INFO, true, true, 1.0, 8.0, new double[] {0.8, 1.6, 2.4, 3.2, 4.0}),
	PYROMANIA_RADIUS("Pyromania Radius", Pyromania.INFO, false, true, 5.0, 50.0, new double[] {0.0, 0.0, 0.0, 25.0, 30.0}),
	VOLCANIC_COMBOS_COOLDOWN(VolcanicCombos.CHARM_COOLDOWN, VolcanicCombos.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	VOLCANIC_COMBOS_DAMAGE("Volcanic Combos Damage", VolcanicCombos.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	VOLCANIC_COMBOS_RADIUS("Volcanic Combos Radius", VolcanicCombos.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	VOLCANIC_COMBOS_FIRE_DURATION("Volcanic Combos Fire Duration", VolcanicCombos.INFO, false, false, 0.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	VOLCANIC_METEOR_COOLDOWN(VolcanicMeteor.CHARM_COOLDOWN, VolcanicMeteor.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	VOLCANIC_METEOR_DAMAGE("Volcanic Meteor Damage", VolcanicMeteor.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	VOLCANIC_METEOR_RADIUS("Volcanic Meteor Radius", VolcanicMeteor.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	VOLCANIC_METEOR_FIRE_DURATION("Volcanic Meteor Fire Duration", VolcanicMeteor.INFO, false, false, 0.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),

	// Frostborn
	AVALANCHE_COOLDOWN(Avalanche.CHARM_COOLDOWN, Avalanche.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	AVALANCHE_DAMAGE("Avalanche Damage", Avalanche.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	AVALANCHE_RANGE("Avalanche Range", Avalanche.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	AVALANCHE_ROOT_DURATION("Avalanche Root Duration", Avalanche.INFO, true, false, 0.25, 2.0, new double[] {0.25, 0.5, 0.75, 1.0, 1.25}),
	CRYOBOX_COOLDOWN(Cryobox.CHARM_COOLDOWN, Cryobox.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	CRYOBOX_ABSORPTION_HEALTH("Cryobox Absorption Health", Cryobox.INFO, true, false, 1.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	CRYOBOX_ABSORPTION_DURATION("Cryobox Absorption Duration", Cryobox.INFO, true, false, 2.0, 20.0, new double[] {2.0, 4.0, 6.0, 8.0, 10.0}),
	CRYOBOX_ICE_DURATION("Cryobox Ice Duration", Cryobox.INFO, false, false, 0.0, 0.0, new double[] {2.0, 3.0, 4.0, 5.0, 6.0}),
	CRYOBOX_FROZEN_DURATION("Cryobox Frozen Duration", Cryobox.INFO, false, false, 1, 6, new double[] {1, 1.5, 2, 2.5, 3}),
	FROST_NOVA_COOLDOWN(DepthsFrostNova.CHARM_COOLDOWN, DepthsFrostNova.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	FROST_NOVA_DAMAGE("Frost Nova Damage", DepthsFrostNova.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	FROST_NOVA_RADIUS("Frost Nova Radius", DepthsFrostNova.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	FROST_NOVA_SLOW_AMPLIFIER("Frost Nova Slowness Amplifier", DepthsFrostNova.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	FROST_NOVA_SLOW_DURATION("Frost Nova Slow Duration", DepthsFrostNova.INFO, true, false, 0.5, 5.0, new double[] {0.5, 1.0, 1.5, 2.0, 2.5}),
	FROST_NOVA_ICE_DURATION("Frost Nova Ice Duration", DepthsFrostNova.INFO, false, false, 0.0, 0.0, new double[] {2.0, 3.0, 4.0, 5.0, 6.0}),
	FRIGID_COMBOS_COOLDOWN(FrigidCombos.CHARM_COOLDOWN, FrigidCombos.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	FRIGID_COMBOS_DAMAGE("Frigid Combos Damage", FrigidCombos.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	FRIGID_COMBOS_RADIUS("Frigid Combos Radius", FrigidCombos.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	FRIGID_COMBOS_SLOW_AMPLIFIER("Frigid Combos Slowness Amplifier", FrigidCombos.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	FRIGID_COMBOS_SLOW_DURATION("Frigid Combos Slowness Duration", FrigidCombos.INFO, true, false, 0.25, 4, new double[] {0.5, 0.75, 1, 1.25, 1.5}),
	FROZEN_DOMAIN_HEALING("Frozen Domain Healing", FrozenDomain.INFO, true, true, 20, 100, new double[] {0.0, 0.0, 20, 40, 60}),
	FROZEN_DOMAIN_SPEED_AMPLIFIER("Frozen Domain Speed Amplifier", FrozenDomain.INFO, false, true, 3.0, 30.0, new double[] {0, 0, 4.0, 8.0, 12.0}),
	FROZEN_DOMAIN_DURATION("Frozen Domain Duration", FrozenDomain.INFO, true, false, 1.0, 5.0, new double[] {0.5, 1.0, 1.5, 2.0, 2.5}),
	ICE_BARRIER_COOLDOWN(IceBarrier.CHARM_COOLDOWN, IceBarrier.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	ICE_BARRIER_CAST_RANGE("Ice Barrier Cast Range", IceBarrier.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	ICE_BARRIER_MAX_LENGTH("Ice Barrier Max Length", IceBarrier.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	ICE_BARRIER_DAMAGE("Ice Barrier Damage", IceBarrier.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	ICE_BARRIER_ICE_DURATION("Ice Barrier Ice Duration", IceBarrier.INFO, false, false, 0.0, 0.0, new double[] {2.0, 3.0, 4.0, 5.0, 6.0}),
	ICEBREAKER_ICE_DAMAGE("Icebreaker Ice Damage Multiplier", Icebreaker.INFO, true, true, 2.0, 20.0, new double[] {2.8, 4.2, 5.6, 7.0, 8.4}),
	ICEBREAKER_DEBUFF_DAMAGE("Icebreaker Debuff Damage Multiplier", Icebreaker.INFO, true, true, 2.0, 20.0, new double[] {2.8, 4.2, 5.6, 7.0, 8.4}),
	ICE_LANCE_COOLDOWN(IceLance.CHARM_COOLDOWN, IceLance.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	ICE_LANCE_DAMAGE("Ice Lance Damage", IceLance.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	ICE_LANCE_RANGE("Ice Lance Range", IceLance.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	ICE_LANCE_DEBUFF_AMPLIFIER("Ice Lance Debuff Amplifier", IceLance.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	ICE_LANCE_DURATION("Ice Lance Debuff Duration", IceLance.INFO, true, false, 0.25, 4.0, new double[] {0.5, 0.75, 1.0, 1.25, 1.5}),
	ICE_LANCE_ICE_DURATION("Ice Lance Ice Duration", Permafrost.INFO, false, false, 0.0, 0.0, new double[] {2.0, 3.0, 4.0, 5.0, 6.0}),
	PERMAFROST_ICE_BONUS_DURATION("Permafrost Ice Bonus Duration", Permafrost.INFO, true, false, 1.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	PERMAFROST_ICE_DURATION("Permafrost Ice Duration", Permafrost.INFO, false, false, 2.0, 0.0, new double[] {2.0, 3.0, 4.0, 5.0, 6.0}),
	PERMAFROST_RADIUS("Permafrost Radius", Permafrost.INFO, false, true, 10, 100, new double[] {20, 30, 40, 50, 60}),
	PERMAFROST_TRAIL_DURATION("Permafrost Trail Duration", Permafrost.INFO, false, false, 2.0, 0.0, new double[] {2.0, 3.0, 4.0, 5.0, 6.0}),
	PERMAFROST_TRAIL_ICE_DURATION("Permafrost Trail Ice Duration", Permafrost.INFO, false, false, 2.0, 0.0, new double[] {2.0, 3.0, 4.0, 5.0, 6.0}),
	PIERCING_COLD_COOLDOWN(PiercingCold.CHARM_COOLDOWN, PiercingCold.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	PIERCING_COLD_DAMAGE("Piercing Cold Damage", PiercingCold.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	PIERCING_COLD_ICE_DURATION("Piercing Cold Ice Duration", PiercingCold.INFO, false, false, 0.0, 0.0, new double[] {2.0, 3.0, 4.0, 5.0, 6.0}),

	// Shadowdancer
	ADVANCING_SHADOWS_COOLDOWN(DepthsAdvancingShadows.CHARM_COOLDOWN, DepthsAdvancingShadows.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	ADVANCING_SHADOWS_RANGE("Advancing Shadows Range", DepthsAdvancingShadows.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	ADVANCING_SHADOWS_DAMAGE_MULTIPLIER("Advancing Shadows Damage Multiplier", DepthsAdvancingShadows.INFO, false, true, 5.0, 50, new double[] {7.5, 11.25, 15.0, 18.75, 22.50}),
	ADVANCING_SHADOWS_DURATION("Advancing Shadows Duration", DepthsAdvancingShadows.INFO, true, false, 0.25, 4.0, new double[] {1.0, 1.25, 1.50, 1.75, 2.0}),
	BLADE_FLURRY_COOLDOWN(BladeFlurry.CHARM_COOLDOWN, BladeFlurry.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	BLADE_FLURRY_DAMAGE("Blade Flurry Damage", BladeFlurry.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	BLADE_FLURRY_RADIUS("Blade Flurry Radius", BladeFlurry.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	BLADE_FLURRY_SILENCE_DURATION("Blade Flurry Silence Duration", BladeFlurry.INFO, true, false, 0.25, 4.0, new double[] {0.5, 0.75, 1.0, 1.25, 1.5}),
	BRUTALIZE_DAMAGE("Brutalize Damage", Brutalize.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	BRUTALIZE_RADIUS("Brutalize Radius", Brutalize.INFO, false, true, 5.0, 50.0, new double[] {0, 0, 10.0, 20.0, 30.0}),
	CHAOS_DAGGER_COOLDOWN(ChaosDagger.CHARM_COOLDOWN, ChaosDagger.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	CHAOS_DAGGER_DAMAGE_MULTIPLIER("Chaos Dagger Damage Multiplier", ChaosDagger.INFO, true, true, 10.0, 80, new double[] {10, 15, 20, 25, 30}),
	CHAOS_DAGGER_VELOCITY("Chaos Dagger Velocity", ChaosDagger.INFO, true, true, 0.0, 100.0, new double[] {0.0, 0.0, 20.0, 40.0, 60.0}),
	CHAOS_DAGGER_STUN_DURATION("Chaos Dagger Stun Duration", ChaosDagger.INFO, true, false, 0.25, 4.0, new double[] {0.5, 0.75, 1.0, 1.25, 1.5}),
	CHAOS_DAGGER_STEALTH_DURATION("Chaos Dagger Stealth Duration", ChaosDagger.INFO, true, false, 0.25, 4.0, new double[] {0.5, 0.75, 1.0, 1.25, 1.5}),
	CLOAK_OF_SHADOWS_COOLDOWN(CloakOfShadows.CHARM_COOLDOWN, CloakOfShadows.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	CLOAK_OF_SHADOWS_DAMAGE_MULTIPLIER("Cloak of Shadows Damage Multiplier", CloakOfShadows.INFO, false, true, 10.0, 80, new double[] {10, 15, 20, 25, 30}),
	CLOAK_OF_SHADOWS_STEALTH_DURATION("Cloak of Shadows Stealth Duration", CloakOfShadows.INFO, true, false, 0.25, 4.0, new double[] {0.5, 0.75, 1.0, 1.25, 1.5}),
	CLOAK_OF_SHADOWS_WEAKEN_AMPLIFIER("Cloak of Shadows Weaken Amplifier", CloakOfShadows.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	CLOAK_OF_SHADOWS_WEAKEN_DURATION("Cloak of Shadows Weaken Duration", CloakOfShadows.INFO, true, false, 0.5, 5.0, new double[] {0.5, 1.0, 1.5, 2.0, 2.5}),
	CLOAK_OF_SHADOWS_RADIUS("Cloak of Shadows Radius", CloakOfShadows.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	DARK_COMBOS_HIT_REQUIREMENT("Dark Combos Hit Requirement", DarkCombos.INFO, true, false, 0.0, -1.0, new double[] {0.0, 0.0, 0.0, 0.0, -1.0}),
	DARK_COMBOS_VULNERABILITY_AMPLIFIER("Dark Combos Vulnerability Amplifier", DarkCombos.INFO, false, true, 5, 25, new double[] {6.5, 8.25, 10.0, 11.75, 13.5}),
	DARK_COMBOS_DURATION("Dark Combos Vulnerability Duration", DarkCombos.INFO, true, false, 0.25, 3.0, new double[] {0.5, 0.75, 1.0, 1.25, 1.5}),
	DEADLY_STRIKE_DAMAGE_AMPLIFIER("Deadly Strike Damage Multiplier", DeadlyStrike.INFO, true, true, 2.0, 10.0, new double[] {1.6, 3.2, 4.8, 6.4, 8.0}),
	DETHRONER_ELITE_DAMAGE_MULTIPLIER("Dethroner Elite Damage Multiplier", DepthsDethroner.INFO, false, true, 2.0, 20.0, new double[] {3.2, 4.8, 6.4, 8.0, 9.6}),
	DETHRONER_BOSS_DAMAGE_MULTIPLIER("Dethroner Boss Damage Multiplier", DepthsDethroner.INFO, false, true, 2.0, 20.0, new double[] {3.2, 4.8, 6.4, 8.0, 9.6}),
	DUMMY_DECOY_COOLDOWN(DummyDecoy.CHARM_COOLDOWN, DummyDecoy.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	DUMMY_DECOY_HEALTH("Dummy Decoy Health", DummyDecoy.INFO, true, true, 10.0, 0.0, new double[] {10.0, 20.0, 30.0, 40.0, 50.0}),
	DUMMY_DECOY_STUN_DURATION("Dummy Decoy Stun Duration", DummyDecoy.INFO, true, false, 0.25, 4.0, new double[] {0.5, 0.75, 1.0, 1.25, 1.5}),
	DUMMY_DECOY_MAX_LIFE_DURATION("Dummy Decoy Max Life Duration", DummyDecoy.INFO, true, false, 1.0, 5.0, new double[] {0.0, 0.0, 1.0, 2.0, 3.0}),
	DUMMY_DECOY_AGGRO_RADIUS("Dummy Decoy Aggro Radius", DummyDecoy.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	DUMMY_DECOY_STUN_RADIUS("Dummy Decoy Stun Radius", DummyDecoy.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	ESCAPE_ARTIST_COOLDOWN(EscapeArtist.CHARM_COOLDOWN, EscapeArtist.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	ESCAPE_ARTIST_STEALTH_DURATION("Escape Artist Stealth Duration", EscapeArtist.INFO, true, false, 0.25, 4.0, new double[] {0.5, 0.75, 1.0, 1.25, 1.5}),
	ESCAPE_ARTIST_MAX_TP_DISTANCE("Escape Artist Max Teleport Distance", EscapeArtist.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	ESCAPE_ARTIST_STUN_RADIUS("Escape Artist Stun Radius", EscapeArtist.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	ESCAPE_ARTIST_STUN_DURATION("Escape Artist Stun Duration", EscapeArtist.INFO, true, false, 0.25, 4.0, new double[] {0.5, 0.75, 1.0, 1.25, 1.5}),
	PHANTOM_FORCE_SPAWN_COUNT("Phantom Force Spawn Count", PhantomForce.INFO, true, false, 0.0, 3, new double[] {0, 0, 0, 1, 2}),
	PHANTOM_FORCE_DAMAGE("Phantom Force Damage", PhantomForce.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	PHANTOM_FORCE_WEAKEN_AMOUNT("Phantom Force Weaken Amplifier", PhantomForce.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	PHANTOM_FORCE_WEAKEN_DURATION("Phantom Force Weaken Duration", PhantomForce.INFO, true, false, 0.5, 5.0, new double[] {0.5, 1.0, 1.5, 2.0, 2.5}),
	PHANTOM_FORCE_VEX_DURATION("Phantom Force Vex Duration", PhantomForce.INFO, true, false, 0.0, 10.0, new double[] {0.0, 2.0, 3.0, 4.0, 5.0}),
	PHANTOM_FORCE_MOVEMENT_SPEED("Phantom Force Movement Speed", PhantomForce.INFO, true, true, 0.0, 50.0, new double[] {0.0, 15.0, 20.0, 25.0, 30.0}),
	SHADOW_SLAM_DAMAGE("Shadow Slam Damage", ShadowSlam.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	SHADOW_SLAM_RADIUS("Shadow Slam Radius", ShadowSlam.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),

	// Steelsage
	FIREWORK_BLAST_COOLDOWN(FireworkBlast.CHARM_COOLDOWN, FireworkBlast.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	FIREWORK_BLAST_DAMAGE("Firework Blast Damage", FireworkBlast.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	FIREWORK_BLAST_DAMAGE_PER_BLOCK("Firework Blast Damage Per Block", FireworkBlast.INFO, true, true, 0.0, 5, new double[] {0.0, 0.0, 0.0, 1, 2}),
	FIREWORK_BLAST_DAMAGE_CAP("Firework Blast Damage Cap", FireworkBlast.INFO, false, true, 10.0, 100.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	FIREWORK_BLAST_RADIUS("Firework Blast Radius", FireworkBlast.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	FOCUSED_COMBOS_HIT_REQUIREMENT("Focused Combos Hit Requirement", FocusedCombos.INFO, true, false, 0.0, -1.0, new double[] {0.0, 0.0, 0.0, 0.0, -1.0}),
	FOCUSED_COMBOS_DAMAGE_MULTIPLIER("Focused Combos Damage Multiplier", FocusedCombos.INFO, true, true, 10, 30, new double[] {5, 10, 15, 20, 25}),
	FOCUSED_COMBOS_BLEED_AMPLIFIER("Focused Combos Bleed Amplifier", FocusedCombos.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15.0}),
	FOCUSED_COMBOS_BLEED_DURATION("Focused Combos Bleed Duration", FocusedCombos.INFO, true, false, 0.25, 4.0, new double[] {0.5, 0.75, 1.0, 1.25, 1.5}),
	GRAVITY_BOMB_COOLDOWN(GravityBomb.CHARM_COOLDOWN, GravityBomb.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	GRAVITY_BOMB_DAMAGE("Gravity Bomb Damage", GravityBomb.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	GRAVITY_BOMB_RADIUS("Gravity Bomb Radius", GravityBomb.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	PRECISION_STRIKE_DAMAGE("Precision Strike Damage", PrecisionStrike.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	PRECISION_STRIKE_RANGE("Precision Strike Range Requirement", PrecisionStrike.INFO, false, true, 10.0, -50, new double[] {-5, -10, -15, -20, -25}),
	PRECISION_STRIKE_MAX_STACKS("Precision Strike Max Stacks", PrecisionStrike.INFO, true, false, 0, 3, new double[] {0, 0, 0, 0, 1}),
	RAPID_FIRE_COOLDOWN(RapidFire.CHARM_COOLDOWN, RapidFire.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	RAPID_FIRE_ARROWS("Rapid Fire Arrows", RapidFire.INFO, true, false, 0.0, 6.0, new double[] {0.0, 0.0, 1.0, 2.0, 3.0}),
	RAPID_FIRE_DAMAGE("Rapid Fire Damage", RapidFire.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	RAPID_FIRE_FIRERATE("Rapid Fire Firerate", RapidFire.INFO, true, true, 0.0, 50.0, new double[] {0.0, 0.0, 0.0, 0.0, 50.0}),
	SCRAPSHOT_COOLDOWN(Scrapshot.CHARM_COOLDOWN, Scrapshot.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	SCRAPSHOT_DAMAGE("Scrapshot Damage", Scrapshot.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	SCRAPSHOT_RANGE("Scrapshot Range", Scrapshot.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	SCRAPSHOT_VELOCITY("Scrapshot Recoil Velocity", Scrapshot.INFO, true, true, 0.0, 100.0, new double[] {0.0, 0.0, 20.0, 40.0, 60.0}),
	SCRAPSHOT_SHRAPNEL_CONE_ANGLE("Scrapshot Shrapnel Cone Angle", Scrapshot.INFO, true, true, 0.0, 100.0, new double[] {0.0, 0.0, 20.0, 40.0, 60.0}),
	SHARPSHOOTER_PASSIVE_DAMAGE("Sharpshooter Damage Multiplier", DepthsSharpshooter.INFO, true, true, 2.0, 10.0, new double[] {2.25, 4.5, 6.75, 9.0, 11.25}),
	SHARPSHOOTER_DAMAGE_PER_STACK("Sharpshooter Damage Per Stack", DepthsSharpshooter.INFO, false, true, 0.25, 3, new double[] {0.75, 1.125, 1.5, 1.875, 2.25}),
	SHARPSHOOTER_DECAY_TIMER("Sharpshooter Decay Timer", DepthsSharpshooter.INFO, true, false, 0.25, 2.0, new double[] {0.0, 0.0, 0.5, 0.75, 1.0}),
	SHARPSHOOTER_MAX_STACKS("Sharpshooter Max Stacks", DepthsSharpshooter.INFO, true, false, 0.0, 4.0, new double[] {0.0, 0.0, 0.0, 1.0, 2.0}),
	SIDEARM_CHARGES("Sidearm Charges", Sidearm.INFO, true, false, 0.0, 1.0, new double[] {0.0, 0.0, 0.0, 0.0, 1.0}),
	SIDEARM_COOLDOWN(Sidearm.CHARM_COOLDOWN, Sidearm.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	SIDEARM_DAMAGE("Sidearm Damage", Sidearm.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	SIDEARM_RANGE("Sidearm Range", Sidearm.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	SIDEARM_KILL_CDR("Sidearm Kill Cooldown Reduction", Sidearm.INFO, true, false, 0.0, 2.0, new double[] {0.0, 0.0, 0.0, 0.0, 2.0}),
	SPLIT_ARROW_DAMAGE("Split Arrow Damage", DepthsSplitArrow.INFO, true, true, 10, 40, new double[] {5, 10, 15, 20, 25}),
	SPLIT_ARROW_BOUNCES("Split Arrow Bounces", DepthsSplitArrow.INFO, true, false, 0.0, 1.0, new double[] {0.0, 0.0, 0.0, 0.0, 1.0}),
	SPLIT_ARROW_RANGE("Split Arrow Range", DepthsSplitArrow.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	STEEL_STALLION_COOLDOWN(SteelStallion.CHARM_COOLDOWN, SteelStallion.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	STEEL_STALLION_HEALTH("Steel Stallion Health", SteelStallion.INFO, true, true, 10.0, 0.0, new double[] {10.0, 20.0, 30.0, 40.0, 50.0}),
	STEEL_STALLION_HORSE_SPEED("Steel Stallion Horse Speed", SteelStallion.INFO, true, true, 5, 0.0, new double[] {3, 6, 9, 12, 15}),
	STEEL_STALLION_JUMP_STRENGTH("Steel Stallion Jump Strength", SteelStallion.INFO, true, false, 0.1, 0.0, new double[] {0.05, 0.1, 0.15, 0.2, 0.25}),
	STEEL_STALLION_DURATION("Steel Stallion Duration", SteelStallion.INFO, true, false, 2.0, 10.0, new double[] {2.0, 3.0, 4.0, 5.0, 6.0}),
	VOLLEY_COOLDOWN(DepthsVolley.CHARM_COOLDOWN, DepthsVolley.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	VOLLEY_DAMAGE_MULTIPLIER("Volley Damage Multiplier", DepthsVolley.INFO, true, true, 5, 30, new double[] {0.0, 0.0, 11.5, 23, 34.5}),
	VOLLEY_ARROWS("Volley Arrows", DepthsVolley.INFO, true, false, 3.0, 0.0, new double[] {2.0, 4.0, 6.0, 8.0, 10.0}),
	VOLLEY_PIERCING("Volley Piercing", DepthsVolley.INFO, true, false, 0.0, 2.0, new double[] {0.0, 0.0, 0.0, 0.0, 1.0}),

	// Windwalker
	AEROBLAST_COOLDOWN(Aeroblast.CHARM_COOLDOWN, Aeroblast.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	AEROBLAST_DAMAGE("Aeroblast Damage", Aeroblast.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	AEROBLAST_SIZE("Aeroblast Size", Aeroblast.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	AEROBLAST_KNOCKBACK("Aeroblast Knockback", Aeroblast.INFO, true, true, 10.0, 100.0, new double[] {20.0, 25.0, 30.0, 35.0, 40.0}),
	AEROBLAST_SPEED_DURATION("Aeroblast Speed Duration", Aeroblast.INFO, true, false, 0.5, 6.0, new double[] {1.0, 1.5, 2.0, 2.5, 3.0}),
	AEROBLAST_SPEED_AMPLIFIER("Aeroblast Speed Amplifier", Aeroblast.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	AEROMANCY_PLAYER_DAMAGE_AMP("Aeromancy Player Damage Multiplier", Aeromancy.INFO, false, true, 2.0, 20.0, new double[] {2.4, 3.6, 4.8, 6.0, 7.2}),
	AEROMANCY_MOB_DAMAGE_AMP("Aeromancy Mob Damage Multiplier", Aeromancy.INFO, false, true, 2.0, 20.0, new double[] {2.4, 3.6, 4.8, 6.0, 7.2}),
	DODGING_COOLDOWN(DepthsDodging.CHARM_COOLDOWN, DepthsDodging.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	GUARDING_BOLT_COOLDOWN(GuardingBolt.CHARM_COOLDOWN, GuardingBolt.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	GUARDING_BOLT_DAMAGE("Guarding Bolt Damage", GuardingBolt.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	GUARDING_BOLT_RADIUS("Guarding Bolt Radius", GuardingBolt.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	GUARDING_BOLT_STUN_DURATION("Guarding Bolt Stun Duration", GuardingBolt.INFO, true, false, 0.25, 4.0, new double[] {0.5, 0.75, 1.0, 1.25, 1.5}),
	GUARDING_BOLT_RANGE("Guarding Bolt Cast Range", GuardingBolt.INFO, false, true, 5.0, 50.0, new double[] {0, 0, 0, 25.0, 30.0}),
	LAST_BREATH_COOLDOWN(LastBreath.CHARM_COOLDOWN, LastBreath.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	LAST_BREATH_COOLDOWN_REDUCTION("Last Breath Cooldown Reduction", LastBreath.INFO, true, true, 10.0, 50.0, new double[] {5.0, 10.0, 15.0, 20.0, 25.0}),
	LAST_BREATH_SPEED_AMPLIFIER("Last Breath Speed Amplifier", LastBreath.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	LAST_BREATH_SPEED_DURATION("Last Breath Speed Duration", LastBreath.INFO, true, false, 0.5, 6.0, new double[] {1.0, 1.5, 2.0, 2.5, 3.0}),
	LAST_BREATH_RESISTANCE_DURATION("Last Breath Resistance Duration", LastBreath.INFO, true, false, 0.25, 3.0, new double[] {0.0, 0.0, 0.5, 0.75, 1.0}),
	LAST_BREATH_RADIUS("Last Breath Radius", LastBreath.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	ONE_WITH_THE_WIND_SPEED_AMPLIFIER("One with the Wind Speed Amplifier", OneWithTheWind.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	ONE_WITH_THE_WIND_RESISTANCE_AMPLIFIER("One with the Wind Resistance Amplifier", OneWithTheWind.INFO, true, true, 1.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	ONE_WITH_THE_WIND_RANGE("One with the Wind Range", OneWithTheWind.INFO, true, false, 0.0, -3.0, new double[] {0.0, 0.0, 0.0, -1.0, -2.0}),
	RESTORING_DRAFT_HEALING("Restoring Draft Healing", RestoringDraft.INFO, true, true, 20, 150, new double[] {15, 30, 45, 60, 75}),
	RESTORING_DRAFT_BLOCK_CAP("Restoring Draft Block Cap", RestoringDraft.INFO, true, false, 2.0, 0.0, new double[] {3.0, 6.0, 9.0, 12.0, 15.0}),
	SKYHOOK_COOLDOWN(Skyhook.CHARM_COOLDOWN, Skyhook.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	SKYHOOK_CDR_PER_BLOCK("Skyhook Cooldown Reduction Per Block", Skyhook.INFO, true, true, 0.0, 1.0, new double[] {0.0, 0.0, 0.0, 0.0, 1.0}),
	THUNDERCLOUD_FORM_COOLDOWN(ThundercloudForm.CHARM_COOLDOWN, ThundercloudForm.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	THUNDERCLOUD_FORM_DAMAGE("Thundercloud Form Damage", ThundercloudForm.INFO, false, true, 5.0, 80.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	THUNDERCLOUD_FORM_RADIUS("Thundercloud Form Radius", ThundercloudForm.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	THUNDERCLOUD_FORM_KNOCKBACK("Thundercloud Form Knockback", ThundercloudForm.INFO, true, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	THUNDERCLOUD_FORM_FLIGHT_SPEED("Thundercloud Form Flight Speed", ThundercloudForm.INFO, true, true, 0.0, 40.0, new double[] {5.0, 10.0, 15.0, 20.0, 25.0}),
	THUNDERCLOUD_FORM_FLIGHT_DURATION("Thundercloud Form Flight Duration", ThundercloudForm.INFO, true, false, 0.0, 4.0, new double[] {0.5, 1.0, 1.5, 2.0, 2.5}),
	WHIRLWIND_RADIUS("Whirlwind Radius", Whirlwind.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	WHIRLWIND_KNOCKBACK("Whirlwind Knockback", Whirlwind.INFO, true, true, 0.0, 40, new double[] {0.0, 0.0, 0.0, 10, 20}),
	WHIRLWIND_SPEED_AMPLIFIER("Whirlwind Speed Amplifier", Whirlwind.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	WHIRLWIND_SPEED_DURATION("Whirlwind Speed Duration", Whirlwind.INFO, true, false, 0.5, 6.0, new double[] {1.0, 1.5, 2.0, 2.5, 3.0}),
	WINDSWEPT_COMBOS_HIT_REQUIREMENT("Windswept Combos Hit Requirement", WindsweptCombos.INFO, true, false, 0.0, -1.0, new double[] {0.0, 0.0, 0.0, 0.0, -1.0}),
	WINDSWEPT_COMBOS_COOLDOWN_REDUCTION("Windswept Combos Cooldown Reduction", WindsweptCombos.INFO, false, true, 2, 10, new double[] {0.0, 0.0, 2.0, 3.0, 5.0}),
	WINDSWEPT_COMBOS_RADIUS("Windswept Combos Radius", WindsweptCombos.INFO, false, true, 5.0, 50.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	WINDSWEPT_COMBOS_PULL_STRENGTH("Windswept Combos Pull Strength", WindsweptCombos.INFO, true, true, 0.0, 40, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	WIND_WALK_COOLDOWN(DepthsWindWalk.CHARM_COOLDOWN, DepthsWindWalk.INFO, false, true, 3.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	WIND_WALK_VELOCITY("Wind Walk Velocity", DepthsWindWalk.INFO, true, true, 0.0, 100.0, new double[] {0.0, 0.0, 20.0, 40.0, 60.0}),
	WIND_WALK_VULNERABILITY_AMPLIFIER("Wind Walk Vulnerability Amplifier", DepthsWindWalk.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	WIND_WALK_VULNERABILITY_DURATION("Wind Walk Vulnerability Duration", DepthsWindWalk.INFO, true, false, 0.5, 5.0, new double[] {0.5, 1.0, 1.5, 2.0, 2.5}),
	WIND_WALK_LEVITATION_DURATION("Wind Walk Levitation Duration", DepthsWindWalk.INFO, true, false, 0.25, 4.0, new double[] {0.5, 0.75, 1.0, 1.25, 1.5}),
	WIND_WALK_STUN_DURATION("Wind Walk Stun Duration", DepthsWindWalk.INFO, true, false, 0.25, 2.0, new double[] {0.25, 0.5, 0.75, 1.0, 1.25});

	// certain stats should not appear at epic/legendary level, but we can't just set their values to 0 in the stats
	// as legacy charms may have those stats, and invalidating them leads to bad behavior.
	public static final Map<String, Integer> extraRarityCaps = Map.ofEntries(
		Map.entry("Bottled Sunlight Bottle Velocity", 4),
		Map.entry("Ward of Light Cone Angle", 4),
		Map.entry("Fireball Velocity", 4),
		Map.entry("Fireball Fire Duration", 3),
		Map.entry("Flame Spirit Fire Duration", 3),
		Map.entry("Flamestrike Cone Angle", 4),
		Map.entry("Flamestrike Fire Duration", 3),
		Map.entry("Flamestrike Knockback", 4),
		Map.entry("Igneous Rune Fire Duration", 3),
		Map.entry("Pyroblast Fire Duration", 3),
		Map.entry("Volcanic Combos Fire Duration", 3),
		Map.entry("Volcanic Meteor Fire Duration", 3),
		Map.entry("Cryobox Ice Duration", 3),
		Map.entry("Frost Nova Ice Duration", 3),
		Map.entry("Ice Barrier Cast Range", 4),
		Map.entry("Ice Barrier Max Length", 4),
		Map.entry("Ice Barrier Ice Duration", 3),
		Map.entry("Ice Lance Ice Duration", 3),
		Map.entry("Piercing Cold Ice Duration", 3),
		Map.entry("Chaos Dagger Velocity", 4),
		Map.entry("Focused Combos Bleed Amplifier", 4),
		Map.entry("Focused Combos Bleed Duration", 3),
		Map.entry("Scrapshot Recoil Velocity", 4),
		Map.entry("Scrapshot Shrapnel Cone Angle", 4),
		Map.entry("Steel Stallion Horse Speed", 4),
		Map.entry("Steel Stallion Jump Strength", 4),
		Map.entry("Wind Walk Velocity", 4)
	);

	public final String mEffectName;
	public final String mAbility;
	public final DepthsTree mTree;
	public final DepthsAbilityInfo<?> mInfo;
	public final boolean mIsOnlyPositive;
	public final double mVariance;
	public final double[] mRarityValues;
	public final boolean mIsPercent;
	public final double mEffectCap; // Uncapped if 0

	CharmEffects(String effectName, DepthsAbilityInfo<?> info, boolean isOnlyPositive, boolean isPercent, double variance, double effectCap, double[] rarityValues) {
		mEffectName = effectName;
		mInfo = info;
		mAbility = Objects.requireNonNull(info.getDisplayName());
		mTree = Objects.requireNonNull(info.getDepthsTree());
		mIsOnlyPositive = isOnlyPositive;
		mVariance = variance;
		mRarityValues = rarityValues;
		mIsPercent = isPercent;
		mEffectCap = effectCap;
	}

	public boolean isValidAtLevel(int level) {
		if (mRarityValues[level - 1] == 0.0) {
			return false;
		}
		return true;
	}

	// certain "useless" stats should be restricted from appearing at high rarities
	public boolean isNotRestrictedAtLevel(int level, boolean isNegative) {
		if (isNegative) {
			return true; // allow stats to be fine at negative legendary
		}
		if (extraRarityCaps.get(mEffectName) != null && level > extraRarityCaps.get(mEffectName)) {
			return false; // disallow it if it's something like Legendary Fire Duration
		}
		return true; // if it's not in the map, it's allowed
	}

	public static @Nullable CharmEffects getEffect(String effectName) {
		for (CharmEffects ce : CharmEffects.values()) {
			if (ce.mEffectName.equals(effectName)) {
				return ce;
			}
		}
		return null;
	}

	public String getEffectName() {
		return mEffectName;
	}
}
