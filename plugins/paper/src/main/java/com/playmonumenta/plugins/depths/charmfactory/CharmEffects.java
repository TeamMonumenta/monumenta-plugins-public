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
import com.playmonumenta.plugins.depths.abilities.dawnbringer.Sundrops;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.TotemOfSalvation;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.WardOfLight;
import com.playmonumenta.plugins.depths.abilities.earthbound.BrambleShell;
import com.playmonumenta.plugins.depths.abilities.earthbound.Bulwark;
import com.playmonumenta.plugins.depths.abilities.earthbound.CrushingEarth;
import com.playmonumenta.plugins.depths.abilities.earthbound.DepthsToughness;
import com.playmonumenta.plugins.depths.abilities.earthbound.EarthenCombos;
import com.playmonumenta.plugins.depths.abilities.earthbound.EarthenWrath;
import com.playmonumenta.plugins.depths.abilities.earthbound.Earthquake;
import com.playmonumenta.plugins.depths.abilities.earthbound.Entrench;
import com.playmonumenta.plugins.depths.abilities.earthbound.StoneSkin;
import com.playmonumenta.plugins.depths.abilities.earthbound.Taunt;
import com.playmonumenta.plugins.depths.abilities.flamecaller.Apocalypse;
import com.playmonumenta.plugins.depths.abilities.flamecaller.Detonation;
import com.playmonumenta.plugins.depths.abilities.flamecaller.Fireball;
import com.playmonumenta.plugins.depths.abilities.flamecaller.FlameSpirit;
import com.playmonumenta.plugins.depths.abilities.flamecaller.Flamestrike;
import com.playmonumenta.plugins.depths.abilities.flamecaller.PrimordialMastery;
import com.playmonumenta.plugins.depths.abilities.flamecaller.Pyroblast;
import com.playmonumenta.plugins.depths.abilities.flamecaller.Pyromania;
import com.playmonumenta.plugins.depths.abilities.flamecaller.RingOfFlames;
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
import com.playmonumenta.plugins.depths.abilities.shadow.ShadowSlam;
import com.playmonumenta.plugins.depths.abilities.steelsage.DepthsSharpshooter;
import com.playmonumenta.plugins.depths.abilities.steelsage.DepthsSplitArrow;
import com.playmonumenta.plugins.depths.abilities.steelsage.DepthsVolley;
import com.playmonumenta.plugins.depths.abilities.steelsage.FireworkBlast;
import com.playmonumenta.plugins.depths.abilities.steelsage.FocusedCombos;
import com.playmonumenta.plugins.depths.abilities.steelsage.Metalmancy;
import com.playmonumenta.plugins.depths.abilities.steelsage.ProjectileMastery;
import com.playmonumenta.plugins.depths.abilities.steelsage.RapidFire;
import com.playmonumenta.plugins.depths.abilities.steelsage.Scrapshot;
import com.playmonumenta.plugins.depths.abilities.steelsage.Sidearm;
import com.playmonumenta.plugins.depths.abilities.steelsage.SteelStallion;
import com.playmonumenta.plugins.depths.abilities.windwalker.Aeromancy;
import com.playmonumenta.plugins.depths.abilities.windwalker.DepthsDodging;
import com.playmonumenta.plugins.depths.abilities.windwalker.DepthsWindWalk;
import com.playmonumenta.plugins.depths.abilities.windwalker.GuardingBolt;
import com.playmonumenta.plugins.depths.abilities.windwalker.HowlingWinds;
import com.playmonumenta.plugins.depths.abilities.windwalker.LastBreath;
import com.playmonumenta.plugins.depths.abilities.windwalker.OneWithTheWind;
import com.playmonumenta.plugins.depths.abilities.windwalker.RestoringDraft;
import com.playmonumenta.plugins.depths.abilities.windwalker.Skyhook;
import com.playmonumenta.plugins.depths.abilities.windwalker.Slipstream;
import com.playmonumenta.plugins.depths.abilities.windwalker.Whirlwind;
import com.playmonumenta.plugins.depths.abilities.windwalker.WindsweptCombos;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

public enum CharmEffects {

	// Dawnbringer
	BOTTLED_SUNLIGHT_COOLDOWN(BottledSunlight.CHARM_COOLDOWN, BottledSunlight.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	BOTTLED_SUNLIGHT_ABSORPTION_HEALTH("Bottled Sunlight Absorption Health", BottledSunlight.INFO, true, false, 1.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	BOTTLED_SUNLIGHT_ABSORPTION_DURATION("Bottled Sunlight Absorption Duration", BottledSunlight.INFO, true, false, 2.0, 20.0, new double[] {2.0, 4.0, 6.0, 8.0, 10.0}),
	BOTTLED_SUNLIGHT_BOTTLE_VELOCITY("Bottled Sunlight Bottle Velocity", BottledSunlight.INFO, true, true, 20.0, 100.0, new double[] {0.0, 0.0, 0.0, 30.0, 50.0}),
	DIVINE_BEAM_COOLDOWN(DivineBeam.CHARM_COOLDOWN, DivineBeam.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	DIVINE_BEAM_HEALING("Divine Beam Healing", DivineBeam.INFO, true, true, 5, 50, new double[] {5, 10, 15, 20, 25}),
	DIVINE_BEAM_STUN_DURATION("Divine Beam Stun Duration", DivineBeam.INFO, true, false, 0.25, 2.0, new double[] {0.25, 0.5, 0.75, 1.0, 1.25}),
	DIVINE_BEAM_MAX_TARGETS_BONUS("Divine Beam Max Targets Bonus", DivineBeam.INFO, true, false, 0.0, 5.0, new double[] {0.0, 0.0, 1.0, 2.0, 3.0}),
	DIVINE_BEAM_COOLDOWN_REDUCTION("Divine Beam Cooldown Reduction", DivineBeam.INFO, true, false, 0.0, 5.0, new double[] {0.0, 1.0, 2.0, 3.0, 4.0}),
	DIVINE_BEAM_MAX_ABSORPTION("Divine Beam Max Absorption", TotemOfSalvation.INFO, true, false, 1.0, 6.0, new double[] {0.0, 0.0, 1.0, 2.0, 3.0}),
	DIVINE_BEAM_ABSORPTION_DURATION("Divine Beam Absorption Duration", TotemOfSalvation.INFO, true, false, 1.0, 10.0, new double[] {0.0, 0.0, 2.0, 4.0, 6.0}),
	ENLIGHTENMENT_XP_MULTIPLIER("Enlightenment Experience Multiplier", Enlightenment.INFO, true, true, 10.0, 100.0, new double[] {10.0, 20.0, 30.0, 40.0, 50.0}),
	ENLIGHTENMENT_RARITY_INCREASE("Enlightenment Rarity Increase Chance", Enlightenment.INFO, true, true, 1.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	ETERNAL_SAVIOR_COOLDOWN(EternalSavior.CHARM_COOLDOWN, EternalSavior.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	ETERNAL_SAVIOR_HEALING("Eternal Savior Healing", EternalSavior.INFO, true, true, 10.0, 100.0, new double[] {10.0, 20.0, 30.0, 40.0, 50.0}),
	ETERNAL_SAVIOR_RADIUS("Eternal Savior Radius", EternalSavior.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	ETERNAL_SAVIOR_ABSORPTION("Eternal Savior Absorption", EternalSavior.INFO, false, false, 1.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	ETERNAL_SAVIOR_ABSORPTION_DURATION("Eternal Savior Absorption Duration", EternalSavior.INFO, false, false, 2.0, 20.0, new double[] {2.0, 4.0, 6.0, 8.0, 10.0}),
	LIGHTNING_BOTTLE_DAMAGE("Lightning Bottle Damage", LightningBottle.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	LIGHTNING_BOTTLE_RADIUS("Lightning Bottle Radius", LightningBottle.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	LIGHTNING_BOTTLE_SLOW_AMPLIFIER("Lightning Bottle Slowness Amplifier", LightningBottle.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	LIGHTNING_BOTTLE_VULN_AMPLIFIER("Lightning Bottle Vulnerability Amplifier", LightningBottle.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	LIGHTNING_BOTTLE_DURATION("Lightning Bottle Debuff Duration", LightningBottle.INFO, true, false, 0.5, 5.0, new double[] {0.5, 1.0, 1.5, 2.0, 2.5}),
	LIGHTNING_BOTTLE_MAX_STACKS("Lightning Bottle Max Stacks", LightningBottle.INFO, true, false, 1.0, 10.0, new double[] {0.0, 0.0, 2.0, 4.0, 6.0}),
	LIGHTNING_BOTTLE_KILLS_PER_BOTTLE("Lightning Bottle Kills Per Bottle", LightningBottle.INFO, true, false, 0.0, -1.0, new double[] {0.0, 0.0, 0.0, 0.0, -1.0}),
	RADIANT_BLESSING_COOLDOWN(RadiantBlessing.CHARM_COOLDOWN, RadiantBlessing.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	RADIANT_BLESSING_RADIUS("Radiant Blessing Radius", RadiantBlessing.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	RADIANT_BLESSING_DAMAGE_AMPLIFIER("Radiant Blessing Strength Amplifier", RadiantBlessing.INFO, false, true, 2.0, 20.0, new double[] {2.0, 3.0, 4.0, 5.0, 6.0}),
	RADIANT_BLESSING_RESISTANCE_AMPLIFIER("Radiant Blessing Resistance Amplifier", RadiantBlessing.INFO, true, true, 2.0, 15.0, new double[] {0.0, 0.0, 2.0, 4.0, 6.0}),
	RADIANT_BLESSING_BUFF_DURATION("Radiant Blessing Buff Duration", RadiantBlessing.INFO, true, false, 1.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	REJUVENATION_HEAL_RADIUS("Rejuvenation Radius", DepthsRejuvenation.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	REJUVENATION_HEALING("Rejuvenation Healing", DepthsRejuvenation.INFO, true, true, 20, 100, new double[] {0.0, 0.0, 20, 40, 60}),
	SOOTHING_COMBOS_HIT_REQUIREMENT("Soothing Combos Hit Requirement", SoothingCombos.INFO, true, false, 0.0, -1.0, new double[] {0.0, 0.0, 0.0, 0.0, -1.0}),
	SOOTHING_COMBOS_RANGE("Soothing Combos Range", SoothingCombos.INFO, false, true, 20, 100, new double[] {20, 30, 40, 50, 60}),
	SOOTHING_COMBOS_HEALING("Soothing Combos Healing", TotemOfSalvation.INFO, true, true, 5, 50, new double[] {5, 10, 15, 20, 25}),
	SOOTHING_COMBOS_SPEED_AMPLIFIER("Soothing Combos Speed Amplifier", SoothingCombos.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	SOOTHING_COMBOS_HASTE_LEVEL("Soothing Combos Haste Level", SoothingCombos.INFO, true, false, 0.0, 1.0, new double[] {0.0, 0.0, 0.0, 0.0, 1.0}),
	SOOTHING_COMBOS_DURATION("Soothing Combos Buff Duration", SoothingCombos.INFO, true, false, 1.0, 5.0, new double[] {0.5, 1.0, 1.5, 2.0, 2.5}),
	SUNDROPS_DROP_CHANCE("Sundrops Drop Chance", Sundrops.INFO, true, true, 5.0, 30.0, new double[] {3.0, 6.0, 9.0, 12.0, 15.0}),
	SUNDROPS_LINGER_TIME("Sundrops Linger Time", Sundrops.INFO, true, false, 2.0, 20.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	SUNDROPS_SPEED_AMPLIFIER("Sundrops Speed Amplifier", Sundrops.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	SUNDROPS_RESISTANCE_AMPLIFIER("Sundrops Resistance Amplifier", Sundrops.INFO, true, true, 3.0, 15.0, new double[] {0.0, 0.0, 0.0, 4.0, 8.0}),
	SUNDROPS_EFFECT_DURATION("Sundrops Buff Duration", Sundrops.INFO, true, false, 1.0, 8.0, new double[] {0.5, 1.0, 1.5, 2.0, 2.5}),
	TOTEM_OF_SALVATION_COOLDOWN(TotemOfSalvation.CHARM_COOLDOWN, TotemOfSalvation.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	TOTEM_OF_SALVATION_DURATION("Totem of Salvation Duration", TotemOfSalvation.INFO, true, false, 2.0, 15.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	TOTEM_OF_SALVATION_RADIUS("Totem of Salvation Radius", TotemOfSalvation.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	TOTEM_OF_SALVATION_HEALING("Totem of Salvation Healing", TotemOfSalvation.INFO, true, true, 10, 100, new double[] {10, 20, 30, 40, 50}),
	TOTEM_OF_SALVATION_MAX_ABSORPTION("Totem of Salvation Max Absorption", TotemOfSalvation.INFO, true, false, 1.0, 6.0, new double[] {0.0, 0.0, 0.0, 1.0, 2.0}),
	TOTEM_OF_SALVATION_ABSORPTION_DURATION("Totem of Salvation Absorption Duration", TotemOfSalvation.INFO, true, false, 1.0, 10.0, new double[] {0.0, 0.0, 0.0, 2.0, 4.0}),
	WARD_OF_LIGHT_COOLDOWN(WardOfLight.CHARM_COOLDOWN, WardOfLight.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	WARD_OF_LIGHT_HEALING("Ward of Light Healing", WardOfLight.INFO, true, true, 5, 50, new double[] {5, 10, 15, 20, 25}),
	WARD_OF_LIGHT_HEAL_RADIUS("Ward of Light Heal Radius", WardOfLight.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	WARD_OF_LIGHT_CONE_ANGLE("Ward of Light Cone Angle", WardOfLight.INFO, true, true, 10.0, 100.0, new double[] {0.0, 0.0, 0.0, 20.0, 40.0}),

	// Earthbound
	BRAMBLE_SHELL_DAMAGE("Bramble Shell Damage", BrambleShell.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	BULWARK_COOLDOWN(Bulwark.CHARM_COOLDOWN, Bulwark.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	CRUSHING_EARTH_COOLDOWN(CrushingEarth.CHARM_COOLDOWN, CrushingEarth.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	CRUSHING_EARTH_DAMAGE("Crushing Earth Damage", CrushingEarth.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	CRUSHING_EARTH_RANGE("Crushing Earth Range", CrushingEarth.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	CRUSHING_EARTH_STUN_DURATION("Crushing Earth Stun Duration", CrushingEarth.INFO, true, false, 0.25, 2.0, new double[] {0.25, 0.5, 0.75, 1.0, 1.25}),
	EARTHEN_COMBOS_HIT_REQUIREMENT("Earthen Combos Hit Requirement", EarthenCombos.INFO, true, false, 0.0, -1.0, new double[] {0.0, 0.0, 0.0, 0.0, -1.0}),
	EARTHEN_COMBOS_RESISTANCE_AMPLIFIER("Earthen Combos Resistance Amplifier", EarthenCombos.INFO, true, true, 2.0, 15.0, new double[] {2.0, 4.0, 6.0, 8.0, 10.0}),
	EARTHEN_COMBOS_EFFECT_DURATION("Earthen Combos Buff Duration", EarthenCombos.INFO, true, false, 1.0, 5.0, new double[] {0.5, 1.0, 1.5, 2.0, 2.5}),
	EARTHEN_COMBOS_ROOT_DURATION("Earthen Combos Root Duration", EarthenCombos.INFO, true, false, 0.25, 2.0, new double[] {0.25, 0.5, 0.75, 1.0, 1.25}),
	EARTHEN_WRATH_COOLDOWN(EarthenWrath.CHARM_COOLDOWN, EarthenWrath.INFO, false, true, 4.0, -30.0, new double[] {-5.0, -7.5, -10.0, -12.5, -15.0}),
	EARTHEN_WRATH_DAMAGE_REFLECTED("Earthen Wrath Damage Reflected", EarthenWrath.INFO, true, true, 10.0, 100.0, new double[] {10.0, 20.0, 30.0, 40.0, 50.0}),
	EARTHEN_WRATH_DAMAGE_REDUCTION("Earthen Wrath Damage Reduction", EarthenWrath.INFO, true, true, 2.0, 15.0, new double[] {2.0, 4.0, 6.0, 8.0, 10.0}),
	EARTHEN_WRATH_TRANSFER_RADIUS("Earthen Wrath Transfer Radius", EarthenWrath.INFO, false, true, 5.0, 50.0, new double[] {5.0, 10.0, 15.0, 20.0, 25.0}),
	EARTHEN_WRATH_RADIUS("Earthen Wrath Radius", EarthenWrath.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	EARTHEN_WRATH_DURATION("Earthen Wrath Duration", EarthenWrath.INFO, true, false, 0.5, 2.0, new double[] {0.0, 0.0, 0.0, 0.0, 1.5}),
	EARTHQUAKE_COOLDOWN(Earthquake.CHARM_COOLDOWN, Earthquake.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	EARTHQUAKE_DAMAGE("Earthquake Damage", Earthquake.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	EARTHQUAKE_RADIUS("Earthquake Radius", Earthquake.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	EARTHQUAKE_KNOCKBACK("Earthquake Knockback", Earthquake.INFO, true, true, 10, 100, new double[] {0.0, 0.0, 0.0, 20, 40}),
	EARTHQUAKE_SILENCE_DURATION("Earthquake Silence Duration", Earthquake.INFO, true, false, 0.5, 10.0, new double[] {0.5, 1.0, 1.5, 2.0, 2.5}),
	ENTRENCH_RADIUS("Entrench Radius", Entrench.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	ENTRENCH_ROOT_DURATION("Entrench Root Duration", Entrench.INFO, true, false, 0.25, 2.0, new double[] {0.25, 0.5, 0.75, 1.0, 1.25}),
	STONE_SKIN_COOLDOWN(StoneSkin.CHARM_COOLDOWN, StoneSkin.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	STONE_SKIN_RESISTANCE_AMPLIFIER("Stone Skin Resistance Amplifier", StoneSkin.INFO, true, true, 2.0, 15.0, new double[] {2.0, 4.0, 6.0, 8.0, 10.0}),
	STONE_SKIN_KNOCKBACK_RESISTANCE("Stone Skin Knockback Resistance", StoneSkin.INFO, true, false, 0.5, 5, new double[] {0.5, 1, 1.5, 2, 2.5}),
	STONE_SKIN_DURATION("Stone Skin Duration", StoneSkin.INFO, true, false, 1.0, 5.0, new double[] {0.5, 1.0, 1.5, 2.0, 2.5}),
	TAUNT_COOLDOWN(Taunt.CHARM_COOLDOWN, Taunt.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	TAUNT_ABSORPTION_PER_MOB("Taunt Absorption Per Mob", Taunt.INFO, true, false, 0.1, 1.0, new double[] {0.1, 0.2, 0.3, 0.4, 0.5}),
	TAUNT_MAX_ABSORPTION_MOBS("Taunt Max Absorption Mobs", Taunt.INFO, true, false, 1.0, 5.0, new double[] {0.0, 0.0, 0.0, 2.0, 4.0}),
	TAUNT_ABSORPTION_DURATION("Taunt Absorption Duration", Taunt.INFO, true, false, 1.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	TAUNT_RANGE("Taunt Range", Taunt.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	TOUGHNESS_MAX_HEALTH("Toughness Max Health", DepthsToughness.INFO, true, true, 3.0, 20.0, new double[] {2.0, 4.0, 6.0, 8.0, 10.0}),

	// Flamecaller
	APOCALYPSE_COOLDOWN(Apocalypse.CHARM_COOLDOWN, Apocalypse.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	APOCALYPSE_DAMAGE("Apocalypse Damage", Apocalypse.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	APOCALYPSE_RADIUS("Apocalypse Radius", Apocalypse.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	APOCALYPSE_HEALING("Apocalypse Healing", Apocalypse.INFO, true, true, 5.0, 50, new double[] {5, 10, 15, 20, 25}),
	APOCALYPSE_MAX_ABSORPTION("Apocalypse Max Absorption", Apocalypse.INFO, true, true, 5.0, 30.0, new double[] {0.0, 0.0, 0.0, 10.0, 20.0}),
	DETONATION_DAMAGE("Detonation Damage", Detonation.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	DETONATION_DAMAGE_RADIUS("Detonation Damage Radius", Detonation.INFO, false, true, 10.0, 200.0, new double[] {0, 0, 0, 25.0, 30.0}),
	DETONATION_DEATH_RADIUS("Detonation Death Radius", Detonation.INFO, false, true, 20.0, 100.0, new double[] {0, 0, 0, 50.0, 60.0}),
	FIREBALL_COOLDOWN(Fireball.CHARM_COOLDOWN, Fireball.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	FIREBALL_DAMAGE("Fireball Damage", Fireball.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	FIREBALL_RADIUS("Fireball Radius", Fireball.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	FIREBALL_RANGE("Fireball Range", Fireball.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	FIREBALL_FIRE_DURATION("Fireball Fire Duration", Fireball.INFO, false, false, 1.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	FLAME_SPIRIT_DAMAGE("Flame Spirit Damage", FlameSpirit.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	FLAME_SPIRIT_RADIUS("Flame Spirit Radius", FlameSpirit.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	FLAME_SPIRIT_FIRE_DURATION("Flame Spirit Fire Duration", FlameSpirit.INFO, false, false, 1.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	FLAME_SPIRIT_DURATION("Flame Spirit Duration", FlameSpirit.INFO, true, false, 0.0, 4.0, new double[] {0.0, 0.0, 0.0, 1.0, 2.0}),
	FLAMESTRIKE_COOLDOWN(Flamestrike.CHARM_COOLDOWN, Flamestrike.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	FLAMESTRIKE_DAMAGE("Flamestrike Damage", Flamestrike.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	FLAMESTRIKE_RANGE("Flamestrike Range", Flamestrike.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	FLAMESTRIKE_CONE_ANGLE("Flamestrike Cone Angle", Flamestrike.INFO, true, true, 10.0, 100.0, new double[] {0.0, 0.0, 0.0, 20.0, 40.0}),
	FLAMESTRIKE_FIRE_DURATION("Flamestrike Fire Duration", Flamestrike.INFO, false, false, 1.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	FLAMESTRIKE_KNOCKBACK("Flamestrike Knockback", Flamestrike.INFO, true, true, 10, 100, new double[] {0.0, 0.0, 0.0, 20, 40}),
	PRIMORDIAL_MASTERY_DAMAGE_MODIFIER("Primordial Mastery Damage Multiplier", PrimordialMastery.INFO, true, true, 2.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	PYROBLAST_COOLDOWN(Pyroblast.CHARM_COOLDOWN, Pyroblast.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	PYROBLAST_DAMAGE("Pyroblast Damage", Pyroblast.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	PYROBLAST_RADIUS("Pyroblast Radius", Pyroblast.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	PYROBLAST_FIRE_DURATION("Pyroblast Fire Duration", Pyroblast.INFO, false, false, 1.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	PYROMANIA_DAMAGE_PER_MOB("Pyromania Damage Per Mob", Pyromania.INFO, true, true, 1.0, 5.0, new double[] {0.5, 1.0, 1.5, 2.0, 2.5}),
	PYROMANIA_RADIUS("Pyromania Radius", Pyromania.INFO, false, true, 20.0, 100.0, new double[] {0, 0, 0, 50.0, 60.0}),
	RING_OF_FLAMES_COOLDOWN(RingOfFlames.CHARM_COOLDOWN, RingOfFlames.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	RING_OF_FLAMES_DAMAGE("Ring of Flames Damage", RingOfFlames.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	RING_OF_FLAMES_DURATION("Ring of Flames Duration", RingOfFlames.INFO, true, false, 1.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	RING_OF_FLAMES_FIRE_DURATION("Ring of Flames Fire Duration", RingOfFlames.INFO, false, false, 1.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	RING_OF_FLAMES_BLEED_AMPLIFIER("Ring of Flames Bleed Amplifier", RingOfFlames.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	VOLCANIC_COMBOS_HIT_REQUIREMENT("Volcanic Combos Hit Requirement", VolcanicCombos.INFO, true, false, 0.0, -1.0, new double[] {0.0, 0.0, 0.0, 0.0, -1.0}),
	VOLCANIC_COMBOS_DAMAGE("Volcanic Combos Damage", VolcanicCombos.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	VOLCANIC_COMBOS_RADIUS("Volcanic Combos Radius", VolcanicCombos.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	VOLCANIC_COMBOS_FIRE_DURATION("Volcanic Combos Fire Duration", VolcanicCombos.INFO, false, false, 1.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	VOLCANIC_METEOR_COOLDOWN(VolcanicMeteor.CHARM_COOLDOWN, VolcanicMeteor.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	VOLCANIC_METEOR_DAMAGE("Volcanic Meteor Damage", VolcanicMeteor.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	VOLCANIC_METEOR_RADIUS("Volcanic Meteor Radius", VolcanicMeteor.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	VOLCANIC_METEOR_FIRE_DURATION("Volcanic Meteor Fire Duration", VolcanicMeteor.INFO, false, false, 1.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),

	// Frostborn
	AVALANCHE_COOLDOWN(Avalanche.CHARM_COOLDOWN, Avalanche.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	AVALANCHE_DAMAGE("Avalanche Damage", Avalanche.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	AVALANCHE_RANGE("Avalanche Range", Avalanche.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	AVALANCHE_ROOT_DURATION("Avalanche Root Duration", Avalanche.INFO, true, false, 0.25, 2.0, new double[] {0.25, 0.5, 0.75, 1.0, 1.25}),
	CRYOBOX_COOLDOWN(Cryobox.CHARM_COOLDOWN, Cryobox.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	CRYOBOX_ABSORPTION_HEALTH("Cryobox Absorption Health", Cryobox.INFO, true, false, 1.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	CRYOBOX_ABSORPTION_DURATION("Cryobox Absorption Duration", Cryobox.INFO, true, false, 2.0, 20.0, new double[] {2.0, 4.0, 6.0, 8.0, 10.0}),
	CRYOBOX_ICE_DURATION("Cryobox Ice Duration", Cryobox.INFO, false, false, 2.0, 0.0, new double[] {2.0, 3.0, 4.0, 5.0, 6.0}),
	CRYOBOX_FROZEN_DURATION("Cryobox Frozen Duration", Cryobox.INFO, false, false, 1, 6, new double[] {1, 1.5, 2, 2.5, 3}),
	FROST_NOVA_COOLDOWN(DepthsFrostNova.CHARM_COOLDOWN, DepthsFrostNova.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	FROST_NOVA_DAMAGE("Frost Nova Damage", DepthsFrostNova.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	FROST_NOVA_RADIUS("Frost Nova Radius", DepthsFrostNova.INFO, false, true, 15.0, 75.0, new double[] {10.0, 20.0, 30.0, 40.0, 50.0}),
	FROST_NOVA_SLOW_AMPLIFIER("Frost Nova Slowness Amplifier", DepthsFrostNova.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	FROST_NOVA_SLOW_DURATION("Frost Nova Slow Duration", DepthsFrostNova.INFO, true, false, 0.5, 5.0, new double[] {0.5, 1.0, 1.5, 2.0, 2.5}),
	FROST_NOVA_ICE_DURATION("Frost Nova Ice Duration", DepthsFrostNova.INFO, false, false, 2.0, 0.0, new double[] {2.0, 3.0, 4.0, 5.0, 6.0}),
	FRIGID_COMBOS_HIT_REQUIREMENT("Frigid Combos Hit Requirement", FrigidCombos.INFO, true, false, 0.0, -1.0, new double[] {0.0, 0.0, 0.0, 0.0, -1.0}),
	FRIGID_COMBOS_DAMAGE("Frigid Combos Damage", FrigidCombos.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	FRIGID_COMBOS_RADIUS("Frigid Combos Radius", FrigidCombos.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	FRIGID_COMBOS_SLOW_AMPLIFIER("Frigid Combos Slowness Amplifier", FrigidCombos.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	FRIGID_COMBOS_SLOW_DURATION("Frigid Combos Slowness Duration", FrigidCombos.INFO, true, false, 0.25, 4, new double[] {0.5, 0.75, 1, 1.25, 1.5}),
	FROZEN_DOMAIN_HEALING("Frozen Domain Healing", FrozenDomain.INFO, true, true, 20, 100, new double[] {0.0, 0.0, 20, 40, 60}),
	FROZEN_DOMAIN_SPEED_AMPLIFIER("Frozen Domain Speed Amplifier", FrozenDomain.INFO, false, true, 3.0, 30.0, new double[] {0, 0, 4.0, 8.0, 12.0}),
	FROZEN_DOMAIN_DURATION("Frozen Domain Duration", FrozenDomain.INFO, true, false, 1.0, 5.0, new double[] {0.5, 1.0, 1.5, 2.0, 2.5}),
	ICE_BARRIER_COOLDOWN(IceBarrier.CHARM_COOLDOWN, IceBarrier.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	ICE_BARRIER_CAST_RANGE("Ice Barrier Cast Range", IceBarrier.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	ICE_BARRIER_MAX_LENGTH("Ice Barrier Max Length", IceBarrier.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	ICE_BARRIER_DAMAGE("Ice Barrier Damage", IceBarrier.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	ICE_BARRIER_ICE_DURATION("Ice Barrier Ice Duration", IceBarrier.INFO, false, false, 2.0, 0.0, new double[] {2.0, 3.0, 4.0, 5.0, 6.0}),
	ICEBREAKER_ICE_DAMAGE("Icebreaker Ice Damage Multiplier", Icebreaker.INFO, true, true, 2.5, 50, new double[] {2.5, 5, 7.5, 10, 12.5}),
	ICEBREAKER_DEBUFF_DAMAGE("Icebreaker Debuff Damage Multiplier", Icebreaker.INFO, true, true, 2.5, 25, new double[] {2.5, 5, 7.5, 10, 12.5}),
	ICE_LANCE_COOLDOWN(IceLance.CHARM_COOLDOWN, IceLance.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	ICE_LANCE_DAMAGE("Ice Lance Damage", IceLance.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	ICE_LANCE_RANGE("Ice Lance Range", IceLance.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	ICE_LANCE_DEBUFF_AMPLIFIER("Ice Lance Debuff Amplifier", IceLance.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	ICE_LANCE_DURATION("Ice Lance Debuff Duration", IceLance.INFO, true, false, 0.25, 4.0, new double[] {0.5, 0.75, 1.0, 1.25, 1.5}),
	ICE_LANCE_ICE_DURATION("Ice Lance Ice Duration", Permafrost.INFO, false, false, 2.0, 0.0, new double[] {2.0, 3.0, 4.0, 5.0, 6.0}),
	PERMAFROST_ICE_BONUS_DURATION("Permafrost Ice Bonus Duration", Permafrost.INFO, true, false, 1.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	PERMAFROST_ICE_DURATION("Permafrost Ice Duration", Permafrost.INFO, false, false, 2.0, 0.0, new double[] {2.0, 3.0, 4.0, 5.0, 6.0}),
	PERMAFROST_RADIUS("Permafrost Radius", Permafrost.INFO, false, true, 20, 100, new double[] {20, 30, 40, 50, 60}),
	PERMAFROST_TRAIL_DURATION("Permafrost Trail Duration", Permafrost.INFO, false, false, 2.0, 0.0, new double[] {2.0, 3.0, 4.0, 5.0, 6.0}),
	PERMAFROST_TRAIL_ICE_DURATION("Permafrost Trail Ice Duration", Permafrost.INFO, false, false, 2.0, 0.0, new double[] {2.0, 3.0, 4.0, 5.0, 6.0}),
	PIERCING_COLD_COOLDOWN(PiercingCold.CHARM_COOLDOWN, PiercingCold.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	PIERCING_COLD_DAMAGE("Piercing Cold Damage", PiercingCold.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	PIERCING_COLD_ICE_DURATION("Piercing Cold Ice Duration", PiercingCold.INFO, false, false, 2.0, 0.0, new double[] {2.0, 3.0, 4.0, 5.0, 6.0}),

	// Shadowdancer
	ADVANCING_SHADOWS_COOLDOWN(DepthsAdvancingShadows.CHARM_COOLDOWN, DepthsAdvancingShadows.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	ADVANCING_SHADOWS_RANGE("Advancing Shadows Range", DepthsAdvancingShadows.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	ADVANCING_SHADOWS_DAMAGE_MULTIPLIER("Advancing Shadows Damage Multiplier", DepthsAdvancingShadows.INFO, false, true, 10.0, 100, new double[] {10, 15, 20, 25, 30}),
	ADVANCING_SHADOWS_DURATION("Advancing Shadows Duration", DepthsAdvancingShadows.INFO, true, false, 0.5, 6.0, new double[] {1.0, 1.5, 2.0, 2.5, 3.0}),
	BLADE_FLURRY_COOLDOWN(BladeFlurry.CHARM_COOLDOWN, BladeFlurry.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	BLADE_FLURRY_DAMAGE("Blade Flurry Damage", BladeFlurry.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	BLADE_FLURRY_RADIUS("Blade Flurry Radius", BladeFlurry.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	BLADE_FLURRY_SILENCE_DURATION("Blade Flurry Silence Duration", BladeFlurry.INFO, true, false, 0.25, 4.0, new double[] {0.5, 0.75, 1.0, 1.25, 1.5}),
	BRUTALIZE_DAMAGE("Brutalize Damage", Brutalize.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	BRUTALIZE_RADIUS("Brutalize Radius", Brutalize.INFO, false, true, 10.0, 100.0, new double[] {0, 0, 10.0, 20.0, 30.0}),
	CHAOS_DAGGER_COOLDOWN(ChaosDagger.CHARM_COOLDOWN, ChaosDagger.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	CHAOS_DAGGER_DAMAGE_MULTIPLIER("Chaos Dagger Damage Multiplier", ChaosDagger.INFO, true, true, 10.0, 100, new double[] {10, 15, 20, 25, 30}),
	CHAOS_DAGGER_VELOCITY("Chaos Dagger Velocity", ChaosDagger.INFO, true, true, 20.0, 100.0, new double[] {0.0, 0.0, 0.0, 30.0, 50.0}),
	CHAOS_DAGGER_STUN_DURATION("Chaos Dagger Stun Duration", ChaosDagger.INFO, true, false, 0.25, 4.0, new double[] {0.5, 0.75, 1.0, 1.25, 1.5}),
	CHAOS_DAGGER_STEALTH_DURATION("Chaos Dagger Stealth Duration", ChaosDagger.INFO, true, false, 0.25, 4.0, new double[] {0.5, 0.75, 1.0, 1.25, 1.5}),
	CLOAK_OF_SHADOWS_COOLDOWN(CloakOfShadows.CHARM_COOLDOWN, CloakOfShadows.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	CLOAK_OF_SHADOWS_DAMAGE_MULTIPLIER("Cloak of Shadows Damage Multiplier", CloakOfShadows.INFO, false, true, 10.0, 100, new double[] {10, 15, 20, 25, 30}),
	CLOAK_OF_SHADOWS_STEALTH_DURATION("Cloak of Shadows Stealth Duration", CloakOfShadows.INFO, true, false, 0.25, 4.0, new double[] {0.5, 0.75, 1.0, 1.25, 1.5}),
	CLOAK_OF_SHADOWS_WEAKEN_AMPLIFIER("Cloak of Shadows Weaken Amplifier", CloakOfShadows.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	CLOAK_OF_SHADOWS_WEAKEN_DURATION("Cloak of Shadows Weaken Duration", CloakOfShadows.INFO, true, false, 0.5, 5.0, new double[] {0.5, 1.0, 1.5, 2.0, 2.5}),
	CLOAK_OF_SHADOWS_RADIUS("Cloak of Shadows Radius", CloakOfShadows.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	DARK_COMBOS_HIT_REQUIREMENT("Dark Combos Hit Requirement", DarkCombos.INFO, true, false, 0.0, -1.0, new double[] {0.0, 0.0, 0.0, 0.0, -1.0}),
	DARK_COMBOS_VULNERABILITY_AMPLIFIER("Dark Combos Vulnerability Amplifier", DarkCombos.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	DARK_COMBOS_DURATION("Dark Combos Vulnerability Duration", DarkCombos.INFO, true, false, 0.25, 3.0, new double[] {0.5, 0.75, 1.0, 1.25, 1.5}),
	DEADLY_STRIKE_DAMAGE_AMPLIFIER("Deadly Strike Damage Multiplier", DeadlyStrike.INFO, true, true, 2.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	DETHRONER_ELITE_DAMAGE_MULTIPLIER("Dethroner Elite Damage Multiplier", DepthsDethroner.INFO, false, true, 2.0, 20.0, new double[] {2.0, 3.0, 4.0, 5.0, 6.0}),
	DETHRONER_BOSS_DAMAGE_MULTIPLIER("Dethroner Boss Damage Multiplier", DepthsDethroner.INFO, false, true, 2.0, 20.0, new double[] {2.0, 3.0, 4.0, 5.0, 6.0}),
	DUMMY_DECOY_COOLDOWN(DummyDecoy.CHARM_COOLDOWN, DummyDecoy.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	DUMMY_DECOY_HEALTH("Dummy Decoy Health", DummyDecoy.INFO, true, true, 10.0, 0.0, new double[] {10.0, 20.0, 30.0, 40.0, 50.0}),
	DUMMY_DECOY_STUN_DURATION("Dummy Decoy Stun Duration", DummyDecoy.INFO, true, false, 0.25, 4.0, new double[] {0.5, 0.75, 1.0, 1.25, 1.5}),
	DUMMY_DECOY_MAX_LIFE_DURATION("Dummy Decoy Max Life Duration", DummyDecoy.INFO, true, false, 1.0, 5.0, new double[] {0.0, 0.0, 1.0, 2.0, 3.0}),
	DUMMY_DECOY_AGGRO_RADIUS("Dummy Decoy Aggro Radius", DummyDecoy.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	DUMMY_DECOY_STUN_RADIUS("Dummy Decoy Stun Radius", DummyDecoy.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	ESCAPE_ARTIST_COOLDOWN(EscapeArtist.CHARM_COOLDOWN, EscapeArtist.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	ESCAPE_ARTIST_STEALTH_DURATION("Escape Artist Stealth Duration", EscapeArtist.INFO, true, false, 0.25, 4.0, new double[] {0.5, 0.75, 1.0, 1.25, 1.5}),
	ESCAPE_ARTIST_MAX_TP_DISTANCE("Escape Artist Max Teleport Distance", EscapeArtist.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	ESCAPE_ARTIST_STUN_RADIUS("Escape Artist Stun Radius", EscapeArtist.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	ESCAPE_ARTIST_STUN_DURATION("Escape Artist Stun Duration", EscapeArtist.INFO, true, false, 0.25, 4.0, new double[] {0.5, 0.75, 1.0, 1.25, 1.5}),
	SHADOW_SLAM_DAMAGE("Shadow Slam Damage", ShadowSlam.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	SHADOW_SLAM_RADIUS("Shadow Slam Radius", ShadowSlam.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),

	// Steelsage
	FIREWORK_BLAST_COOLDOWN(FireworkBlast.CHARM_COOLDOWN, FireworkBlast.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	FIREWORK_BLAST_DAMAGE("Firework Blast Damage", FireworkBlast.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	FIREWORK_BLAST_DAMAGE_PER_BLOCK("Firework Blast Damage Per Block", FireworkBlast.INFO, true, true, 0.0, 5, new double[] {0.0, 0.0, 0.0, 1, 2}),
	FIREWORK_BLAST_DAMAGE_CAP("Firework Blast Damage Cap", FireworkBlast.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	FIREWORK_BLAST_RADIUS("Firework Blast Radius", FireworkBlast.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	FOCUSED_COMBOS_HIT_REQUIREMENT("Focused Combos Hit Requirement", FocusedCombos.INFO, true, false, 0.0, -1.0, new double[] {0.0, 0.0, 0.0, 0.0, -1.0}),
	FOCUSED_COMBOS_DAMAGE_MULTIPLIER("Focused Combos Damage Multiplier", FocusedCombos.INFO, true, true, 10, 30, new double[] {5, 10, 15, 20, 25}),
	FOCUSED_COMBOS_BLEED_AMPLIFIER("Focused Combos Bleed Amplifier", FocusedCombos.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	FOCUSED_COMBOS_BLEED_DURATION("Focused Combos Bleed Duration", FocusedCombos.INFO, true, false, 0.25, 4.0, new double[] {0.5, 0.75, 1.0, 1.25, 1.5}),
	METALMANCY_COOLDOWN(Metalmancy.CHARM_COOLDOWN, Metalmancy.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	METALMANCY_DAMAGE("Metalmancy Damage", Metalmancy.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	METALMANCY_DURATION("Metalmancy Duration", Metalmancy.INFO, true, false, 1.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	PROJECTILE_MASTERY_DAMAGE_MULTIPLIER("Projectile Mastery Damage Multiplier", ProjectileMastery.INFO, true, true, 2.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	RAPID_FIRE_COOLDOWN(RapidFire.CHARM_COOLDOWN, RapidFire.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	RAPID_FIRE_ARROWS("Rapid Fire Arrows", RapidFire.INFO, true, false, 0.0, 0.0, new double[] {0.0, 0.0, 1.0, 2.0, 3.0}),
	RAPID_FIRE_DAMAGE("Rapid Fire Damage", RapidFire.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	SCRAPSHOT_COOLDOWN(Scrapshot.CHARM_COOLDOWN, Scrapshot.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	SCRAPSHOT_DAMAGE("Scrapshot Damage", Scrapshot.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	SCRAPSHOT_RANGE("Scrapshot Range", Scrapshot.INFO, false, true, 20, 100, new double[] {20, 30, 40, 50, 60}),
	SCRAPSHOT_VELOCITY("Scrapshot Recoil Velocity", Scrapshot.INFO, true, true, 20.0, 100.0, new double[] {0.0, 0.0, 0.0, 30.0, 50.0}),
	SHARPSHOOTER_DAMAGE_PER_STACK("Sharpshooter Damage Per Stack", DepthsSharpshooter.INFO, false, true, 0.75, 10, new double[] {0.75, 0.875, 1, 1.25, 1.5}),
	SHARPSHOOTER_DECAY_TIMER("Sharpshooter Decay Timer", DepthsSharpshooter.INFO, true, false, 0.5, 4.0, new double[] {0.0, 0.0, 1.0, 1.5, 2.0}),
	SHARPSHOOTER_MAX_STACKS("Sharpshooter Max Stacks", DepthsSharpshooter.INFO, true, false, 0.0, 8.0, new double[] {0.0, 0.0, 0.0, 1.0, 2.0}),
	SIDEARM_COOLDOWN(Sidearm.CHARM_COOLDOWN, Sidearm.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	SIDEARM_DAMAGE("Sidearm Damage", Sidearm.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	SIDEARM_RANGE("Sidearm Range", Sidearm.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	SIDEARM_KILL_CDR("Sidearm Kill Cooldown Reduction", Sidearm.INFO, true, true, 0.0, 33.0, new double[] {0.0, 0.0, 0.0, 0.0, 33.0}),
	SPLIT_ARROW_DAMAGE("Split Arrow Damage", DepthsSplitArrow.INFO, true, true, 10, 40, new double[] {5, 10, 15, 20, 25}),
	SPLIT_ARROW_BOUNCES("Split Arrow Bounces", DepthsSplitArrow.INFO, true, false, 0.0, 1.0, new double[] {0.0, 0.0, 0.0, 0.0, 1.0}),
	SPLIT_ARROW_RANGE("Split Arrow Range", DepthsSplitArrow.INFO, false, true, 20, 100, new double[] {20, 30, 40, 50, 60}),
	STEEL_STALLION_COOLDOWN(SteelStallion.CHARM_COOLDOWN, SteelStallion.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	STEEL_STALLION_HEALTH("Steel Stallion Health", SteelStallion.INFO, true, true, 10.0, 0.0, new double[] {10.0, 20.0, 30.0, 40.0, 50.0}),
	STEEL_STALLION_HORSE_SPEED("Steel Stallion Horse Speed", SteelStallion.INFO, true, true, 5, 0.0, new double[] {3, 6, 9, 12, 15}),
	STEEL_STALLION_JUMP_STRENGTH("Steel Stallion Jump Strength", SteelStallion.INFO, true, false, 0.1, 0.0, new double[] {0.05, 0.1, 0.15, 0.2, 0.25}),
	STEEL_STALLION_DURATION("Steel Stallion Duration", SteelStallion.INFO, true, false, 2.0, 10.0, new double[] {2.0, 3.0, 4.0, 5.0, 6.0}),
	VOLLEY_COOLDOWN(DepthsVolley.CHARM_COOLDOWN, DepthsVolley.INFO, false, true, 5.0, -40.0, new double[] {-6.0, -9.0, -12.0, -15.0, -18.0}),
	VOLLEY_DAMAGE_MULTIPLIER("Volley Damage Multiplier", DepthsVolley.INFO, true, true, 5, 30, new double[] {0.0, 0.0, 5, 10, 15}),
	VOLLEY_ARROWS("Volley Arrows", DepthsVolley.INFO, true, false, 3.0, 0.0, new double[] {2.0, 4.0, 6.0, 8.0, 10.0}),
	VOLLEY_PIERCING("Volley Piercing", DepthsVolley.INFO, true, false, 0.0, 2.0, new double[] {0.0, 0.0, 0.0, 0.0, 1.0}),

	// Windwalker
	AEROMANCY_PLAYER_DAMAGE_AMP("Aeromancy Player Damage Multiplier", Aeromancy.INFO, false, true, 2.0, 20.0, new double[] {2.0, 3.0, 4.0, 5.0, 6.0}),
	AEROMANCY_MOB_DAMAGE_AMP("Aeromancy Mob Damage Multiplier", Aeromancy.INFO, false, true, 2.0, 20.0, new double[] {2.0, 3.0, 4.0, 5.0, 6.0}),
	DODGING_COOLDOWN(DepthsDodging.CHARM_COOLDOWN, DepthsDodging.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	GUARDING_BOLT_COOLDOWN(GuardingBolt.CHARM_COOLDOWN, GuardingBolt.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	GUARDING_BOLT_DAMAGE("Guarding Bolt Damage", GuardingBolt.INFO, false, true, 10.0, 200.0, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	GUARDING_BOLT_RADIUS("Guarding Bolt Radius", GuardingBolt.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	GUARDING_BOLT_STUN_DURATION("Guarding Bolt Stun Duration", GuardingBolt.INFO, true, false, 0.25, 4.0, new double[] {0.5, 0.75, 1.0, 1.25, 1.5}),
	GUARDING_BOLT_RANGE("Guarding Bolt Cast Range", GuardingBolt.INFO, false, true, 20.0, 100.0, new double[] {0, 0, 0, 50.0, 60.0}),
	HOWLING_WINDS_COOLDOWN(HowlingWinds.CHARM_COOLDOWN, HowlingWinds.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	HOWLING_WINDS_RADIUS("Howling Winds Radius", HowlingWinds.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	HOWLING_WINDS_DURATION("Howling Winds Duration", HowlingWinds.INFO, true, false, 0.5, 6.0, new double[] {1.0, 1.5, 2.0, 2.5, 3.0}),
	HOWLING_WINDS_VELOCITY("Howling Winds Velocity", HowlingWinds.INFO, true, true, 0.0, 40.0, new double[] {0.0, 0.0, 0.0, 0.0, 20.0}),
	HOWLING_WINDS_RANGE("Howling Winds Cast Range", HowlingWinds.INFO, false, true, 20, 100, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	LAST_BREATH_COOLDOWN(LastBreath.CHARM_COOLDOWN, LastBreath.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	LAST_BREATH_COOLDOWN_REDUCTION("Last Breath Cooldown Reduction", LastBreath.INFO, true, true, 10.0, 50.0, new double[] {5.0, 10.0, 15.0, 20.0, 25.0}),
	LAST_BREATH_SPEED_AMPLIFIER("Last Breath Speed Amplifier", LastBreath.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	LAST_BREATH_SPEED_DURATION("Last Breath Speed Duration", LastBreath.INFO, true, false, 0.5, 6.0, new double[] {1.0, 1.5, 2.0, 2.5, 3.0}),
	LAST_BREATH_RESISTANCE_DURATION("Last Breath Resistance Duration", LastBreath.INFO, true, false, 0.25, 3.0, new double[] {0.0, 0.0, 0.5, 0.75, 1.0}),
	LAST_BREATH_RADIUS("Last Breath Radius", LastBreath.INFO, false, true, 20, 100, new double[] {20, 30, 40, 50, 60}),
	ONE_WITH_THE_WIND_SPEED_AMPLIFIER("One with the Wind Speed Amplifier", OneWithTheWind.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	ONE_WITH_THE_WIND_RESISTANCE_AMPLIFIER("One with the Wind Resistance Amplifier", OneWithTheWind.INFO, true, true, 1.0, 10.0, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}),
	ONE_WITH_THE_WIND_RANGE("One with the Wind Range", OneWithTheWind.INFO, true, false, 0.0, -3.0, new double[] {0.0, 0.0, 0.0, -1.0, -2.0}),
	RESTORING_DRAFT_HEALING("Restoring Draft Healing", RestoringDraft.INFO, true, true, 20, 150, new double[] {15, 30, 45, 60, 75}),
	RESTORING_DRAFT_BLOCK_CAP("Restoring Draft Block Cap", RestoringDraft.INFO, true, false, 2.0, 0.0, new double[] {3.0, 6.0, 9.0, 12.0, 15.0}),
	SKYHOOK_COOLDOWN(Skyhook.CHARM_COOLDOWN, Skyhook.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	SKYHOOK_CDR_PER_BLOCK("Skyhook Cooldown Reduction Per Block", Skyhook.INFO, true, true, 0.0, 1.0, new double[] {0.0, 0.0, 0.0, 0.0, 1.0}),
	SLIPSTREAM_COOLDOWN(Slipstream.CHARM_COOLDOWN, Slipstream.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	SLIPSTREAM_DURATION("Slipstream Duration", Slipstream.INFO, true, false, 0.5, 6.0, new double[] {1.0, 1.5, 2.0, 2.5, 3.0}),
	SLIPSTREAM_RADIUS("Slipstream Radius", Slipstream.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	SLIPSTREAM_KNOCKBACK("Slipstream Knockback", Slipstream.INFO, true, true, 0.0, 40, new double[] {0.0, 0.0, 0.0, 10, 20}),
	SLIPSTREAM_SPEED_AMPLIFIER("Slipstream Speed Amplifier", Slipstream.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	SLIPSTREAM_JUMP_BOOST_AMPLIFIER("Slipstream Jump Boost Amplifier", Slipstream.INFO, true, false, 0.0, 1.0, new double[] {0.0, 0.0, 0.0, 0.0, 1.0}),
	WHIRLWIND_RADIUS("Whirlwind Radius", Whirlwind.INFO, false, true, 20.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	WHIRLWIND_KNOCKBACK("Whirlwind Knockback", Whirlwind.INFO, true, true, 0.0, 40, new double[] {0.0, 0.0, 0.0, 10, 20}),
	WHIRLWIND_SPEED_AMPLIFIER("Whirlwind Speed Amplifier", Whirlwind.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	WHIRLWIND_SPEED_DURATION("Whirlwind Speed Duration", Whirlwind.INFO, true, false, 0.5, 6.0, new double[] {1.0, 1.5, 2.0, 2.5, 3.0}),
	WINDSWEPT_COMBOS_HIT_REQUIREMENT("Windswept Combos Hit Requirement", WindsweptCombos.INFO, true, false, 0.0, -1.0, new double[] {0.0, 0.0, 0.0, 0.0, -1.0}),
	WINDSWEPT_COMBOS_COOLDOWN_REDUCTION("Windswept Combos Cooldown Reduction", WindsweptCombos.INFO, false, true, 2, 10, new double[] {0.0, 0.0, 2.0, 3.0, 5.0}),
	WINDSWEPT_COMBOS_RADIUS("Windswept Combos Radius", WindsweptCombos.INFO, false, true, 10.0, 100.0, new double[] {20.0, 30.0, 40.0, 50.0, 60.0}),
	WINDSWEPT_COMBOS_PULL_STRENGTH("Windswept Combos Pull Strength", WindsweptCombos.INFO, true, true, 0.0, 40, new double[] {10.0, 15.0, 20.0, 25.0, 30.0}),
	WIND_WALK_COOLDOWN(DepthsWindWalk.CHARM_COOLDOWN, DepthsWindWalk.INFO, false, true, 6.0, -50.0, new double[] {-6.0, -10.0, -14.0, -18.0, -22.0}),
	WIND_WALK_VELOCITY("Wind Walk Velocity", DepthsWindWalk.INFO, true, true, 20.0, 100.0, new double[] {0.0, 0.0, 0.0, 30.0, 50.0}),
	WIND_WALK_VULNERABILITY_AMPLIFIER("Wind Walk Vulnerability Amplifier", DepthsWindWalk.INFO, false, true, 5, 40, new double[] {5, 7.5, 10, 12.5, 15}),
	WIND_WALK_VULNERABILITY_DURATION("Wind Walk Vulnerability Duration", DepthsWindWalk.INFO, true, false, 0.5, 5.0, new double[] {0.5, 1.0, 1.5, 2.0, 2.5}),
	WIND_WALK_LEVITATION_DURATION("Wind Walk Levitation Duration", DepthsWindWalk.INFO, true, false, 0.25, 4.0, new double[] {0.5, 0.75, 1.0, 1.25, 1.5}),
	WIND_WALK_STUN_DURATION("Wind Walk Stun Duration", DepthsWindWalk.INFO, true, false, 0.25, 2.0, new double[] {0.25, 0.5, 0.75, 1.0, 1.25});


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
