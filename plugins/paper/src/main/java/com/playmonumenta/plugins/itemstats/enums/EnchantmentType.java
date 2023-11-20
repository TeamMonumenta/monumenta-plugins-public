package com.playmonumenta.plugins.itemstats.enums;

import com.playmonumenta.plugins.itemstats.ItemStat;
import com.playmonumenta.plugins.itemstats.enchantments.*;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.Nullable;

public enum EnchantmentType {
	// Region Scaling
	OFFHAND_MAINHAND_DISABLE(new OffhandMainhandDisable(), false, false, false, false),
	MAINHAND_OFFHAND_DISABLE(new MainhandOffhandDisable(), false, false, false, false),
	REGION_SCALING_DAMAGE_DEALT(new RegionScalingDamageDealt(), false, false, false, false),
	REGION_SCALING_DAMAGE_TAKEN(new RegionScalingDamageTaken(), false, false, false, false),
	SKT_DAMAGE_DEALT(new SKTQuestDamageDealt(), false, false, false, false),
	SKT_DAMAGE_TAKEN(new SKTQuestDamageTaken(), false, false, false, false),

	// Vanilla
	SWEEPING_EDGE(Enchantment.SWEEPING_EDGE, "Sweeping Edge", true, false, false, false),
	KNOCKBACK(Enchantment.KNOCKBACK, "Knockback", true, false, false, false),
	LOOTING(Enchantment.LOOT_BONUS_MOBS, "Looting", true, false, false, false),
	RIPTIDE(Enchantment.RIPTIDE, "Riptide", true, false, false, false),
	PUNCH(Enchantment.ARROW_KNOCKBACK, "Punch", true, false, false, false),
	QUICK_CHARGE(Enchantment.QUICK_CHARGE, "Quick Charge", true, false, false, false),
	MULTISHOT(Enchantment.MULTISHOT, "Multishot", false, false, false, false),
	INFINITY(Enchantment.ARROW_INFINITE, "Infinity", false, false, false, false),
	EFFICIENCY(Enchantment.DIG_SPEED, "Efficiency", true, false, false, false),
	FORTUNE(Enchantment.LOOT_BONUS_BLOCKS, "Fortune", true, false, false, false),
	SILK_TOUCH(Enchantment.SILK_TOUCH, "Silk Touch", false, false, false, false),
	LUCK_OF_THE_SEA(Enchantment.LUCK, "Luck of the Sea", true, false, false, false),
	LURE(Enchantment.LURE, "Lure", true, false, false, false),
	SOUL_SPEED(Enchantment.SOUL_SPEED, "Soul Speed", true, false, false, false),
	AQUA_AFFINITY(Enchantment.WATER_WORKER, "Aqua Affinity", false, false, false, false),
	RESPIRATION(Enchantment.OXYGEN, "Respiration", true, false, false, false),
	DEPTH_STRIDER(Enchantment.DEPTH_STRIDER, "Depth Strider", true, false, false, false),

	// Protections
	MELEE_PROTECTION(new MeleeProtection(), true, false, false, true),
	PROJECTILE_PROTECTION(new ProjectileProtection(), true, false, false, true),
	BLAST_PROTECTION(new BlastProtection(), true, false, false, true),
	MAGIC_PROTECTION(new MagicProtection(), true, false, false, true),
	FIRE_PROTECTION(new FireProtection(), true, false, false, true),
	FEATHER_FALLING(new FeatherFalling(), true, false, false, true),

	// Defense Scaling
	SHIELDING(new Shielding(), true, false, false, true),
	INURE(new Inure(), true, false, false, true),
	POISE(new Poise(), true, false, false, true),
	STEADFAST(new Steadfast(), true, false, false, true),
	GUARD(new Guard(), true, false, false, true),
	TEMPO(new Tempo(), true, false, false, true),
	REFLEXES(new Reflexes(), true, false, false, true),
	ETHEREAL(new Ethereal(), true, false, false, true),
	EVASION(new Evasion(), true, false, false, true),
	CLOAKED(new Cloaked(), true, false, false, true),
	ADAPTABILITY(new Adaptability(), false, false, false, true),

	// Custom Enchants
	ABYSSAL(new Abyssal(), true, false, false, true),
	ADRENALINE(new Adrenaline(), true, false, false, true),
	APTITUDE(new Aptitude(), true, false, false, true),
	ARCANE_THRUST(new ArcaneThrust(), true, false, false, true),
	ASHES_OF_ETERNITY(new AshesOfEternity(), false, false, false, true),
	BLEEDING(new Bleeding(), true, false, false, true),
	BROOMSTICK(new Broomstick(), false, false, false, false),
	CHAOTIC(new Chaotic(), true, false, false, true),
	DARKSIGHT(new Darksight(), false, false, false, false),
	DECAY(new Decay(), true, false, false, true),
	DRILLING(new Drilling(), true, false, false, true),
	DUELIST(new Duelist(), true, false, false, true),
	ERUPTION(new Eruption(), true, false, false, true),
	EXCAVATOR(new Excavator(), false, false, false, true),
	EXPLODING(new Explosive(), true, false, false, true),
	ICE_ASPECT(new IceAspect(), true, false, false, true),
	FIRE_ASPECT(new FireAspect(), true, false, false, true),
	THUNDER_ASPECT(new ThunderAspect(), true, false, false, true),
	WIND_ASPECT(new WindAspect(), true, false, false, true),
	EARTH_ASPECT(new EarthAspect(), true, false, false, true),
	FIRST_STRIKE(new FirstStrike(), true, false, false, true),
	FRACTAL(new Fractal(), true, false, false, true),
	GILLS(new Gills(), false, false, false, false),
	HEX_EATER(new HexEater(), true, false, false, true),
	INFERNO(new Inferno(), true, false, false, true),
	INTOXICATING_WARMTH(new IntoxicatingWarmth(), false, false, false, false),
	INSTANT_DRINK(new InstantDrink(), false, false, false, false),
	INTUITION(new Intuition(), false, false, false, true),
	JUNGLES_NOURISHMENT(new JunglesNourishment(), false, false, false, false),
	LIFE_DRAIN(new LifeDrain(), true, false, false, true),
	LIQUID_COURAGE(new LiquidCourage(), false, false, false, false),
	MULTILOAD(new Multiload(), true, false, false, true),
	MULTITOOL(new Multitool(), true, false, false, false),
	RADIANT(new Radiant(), false, false, false, false),
	REGENERATION(new Regeneration(), true, false, false, true),
	POINT_BLANK(new PointBlank(), true, false, false, true),
	PIERCING(new Piercing(), true, false, false, true),
	WORLDLY_PROTECTION(new WorldlyProtection(), true, false, false, false),
	QUAKE(new Quake(), true, false, false, true),
	RAGE_OF_THE_KETER(new RageOfTheKeter(), false, false, false, false),
	RECOIL(new Recoil(), true, false, false, false),
	REGICIDE(new Regicide(), true, false, false, true),
	RESURRECTION(new Resurrection(), false, false, false, false),
	RETRIEVAL(new Retrieval(), true, false, false, true),
	SAPPER(new Sapper(), true, false, false, true),
	SECOND_WIND(new SecondWind(), true, false, false, true),
	SLAYER(new Slayer(), true, false, false, true),
	SMITE(new Smite(), true, false, false, true),
	SNIPER(new Sniper(), true, false, false, true),
	REVERB(new Reverb(), true, false, false, true),
	STAMINA(new Stamina(), true, false, false, true),
	STARVATION(new Starvation(), false, true, false, false),
	SUSTENANCE(new Sustenance(), true, false, false, true),
	WEIGHTLESS(new Weightless(), false, false, false, false),
	TECHNIQUE(new Technique(), true, false, false, true),
	TEMPORAL_BENDER(new TemporalBender(), false, false, false, false),
	THROWING_KNIFE(new ThrowingKnife(), false, false, false, false),
	TRIAGE(new Triage(), true, false, false, true),
	TRIVIUM(new Trivium(), true, false, false, true),
	VERSATILITY(new Versatility(), true, false, false, true),

	// Curses
	CURSE_OF_ANEMIA(new CurseOfAnemia(), true, true, false, false),
	CURSE_OF_BINDING(Enchantment.BINDING_CURSE, "Curse of Binding", false, true, false, false),
	CURSE_OF_VANISHING(Enchantment.VANISHING_CURSE, "Curse of Vanishing", false, true, false, false),
	CURSE_OF_INSTABILITY(new CurseOfInstability(), false, true, false, false),
	CURSE_OF_IRREPARIBILITY(new CurseOfIrreparability(), false, true, false, false),
	CURSE_OF_CRIPPLING(new CurseOfCrippling(), true, true, false, false),
	CURSE_OF_CORRUPTION(new CurseOfCorruption(), false, true, false, false),
	CURSE_OF_EPHEMERALITY(new CurseOfEphemerality(), false, true, true, false),
	CURSE_OF_SHRAPNEL(new CurseOfShrapnel(), true, true, false, false),
	TWO_HANDED(new TwoHanded(), false, true, false, false),
	INEPTITUDE(new Ineptitude(), true, true, false, false),
	MELEE_FRAGILITY(new MeleeFragility(), true, true, false, false),
	BLAST_FRAGILITY(new BlastFragility(), true, true, false, false),
	PROJECTILE_FRAGILITY(new ProjectileFragility(), true, true, false, false),
	MAGIC_FRAGILITY(new MagicFragility(), true, true, false, false),
	FIRE_FRAGILITY(new FireFragility(), true, true, false, false),
	FALL_FRAGILITY(new FallFragility(), true, true, false, false),
	CUMBERSOME(new Cumbersome(), false, true, false, false),

	// Cosmetic Item Enchants
	BAAING(new Baaing(), false, true, true, false),
	CLUCKING(new Clucking(), false, true, true, false),
	DIVINE_AURA(new DivineAura(), false, false, false, false),
	OINKING(new Oinking(), false, true, true, false),
	MATERIAL(new MaterialEnch(), false, false, false, false),

	//Random Stuff
	PESTILENCE_TESSERACT(new PestilenceTesseract(), false, false, true, false),

	// Hidden enchantments that affect display
	HIDE_ATTRIBUTES(new HideAttributes(), false, false, false, false),
	HIDE_ENCHANTS(new HideEnchants(), false, false, false, false),
	HIDE_INFO(new HideInfo(), false, false, false, false),
	NO_GLINT(new NoGlint(), false, false, false, false),
	DELETE_ON_SHATTER(null, "DeleteOnShatter", false, false, false, false),

	// Crit Calcs (defaults to value of 1, always active. DO NOT GIVE TO PLAYERS VIA ENCHANT)
	ANTI_CRIT_SCALING(new AntiCritScaling(), false, false, false, false),
	CRIT_SCALING(new CritScaling(), false, false, false, false),
	STRENGTH_APPLY(new StrengthApply(), false, false, false, false),
	STRENGTH_CANCEL(new StrengthCancel(), false, false, false, false),

	// Durability
	UNBREAKING(Enchantment.DURABILITY, "Unbreaking", true, false, false, false),
	UNBREAKABLE(null, "Unbreakable", false, false, false, false),
	MENDING(Enchantment.MENDING, "Mending", false, false, false, false),

	// Item Tags (these are last)
	MAGIC_WAND(new MagicWandEnch(), false, false, false, false),
	ALCHEMICAL_ALEMBIC(null, "Alchemical Utensil", false, false, false, false);

	public static final Map<String, EnchantmentType> REVERSE_MAPPINGS = Arrays.stream(EnchantmentType.values())
		.collect(Collectors.toUnmodifiableMap(type -> type.getName().replace(" ", ""), type -> type));

	public static final Set<EnchantmentType> SPAWNABLE_ENCHANTMENTS = Arrays.stream(EnchantmentType.values())
		.filter(type -> type.mIsSpawnable)
		.collect(Collectors.toUnmodifiableSet());

	public static final Set<EnchantmentType> PROJECTILE_ENCHANTMENTS = Arrays.stream(EnchantmentType.values())
		.filter(type -> type.getItemStat() instanceof com.playmonumenta.plugins.itemstats.Enchantment ench && ench.getSlots().contains(Slot.PROJECTILE))
		.collect(Collectors.toUnmodifiableSet());

	public static final String KEY = "Enchantments";

	final @Nullable Enchantment mEnchantment;
	final @Nullable ItemStat mItemStat;
	final String mName;
	final boolean mUsesLevels;
	final boolean mIsCurse;
	final boolean mIsSpawnable;
	final boolean mIsRegionScaled;

	EnchantmentType(@Nullable Enchantment enchantment, String name, boolean usesLevels, boolean isCurse, boolean isSpawnable, boolean isRegionScaled) {
		mEnchantment = enchantment;
		mItemStat = null;
		mName = name;
		mUsesLevels = usesLevels;
		mIsCurse = isCurse;
		mIsSpawnable = isSpawnable;
		mIsRegionScaled = isRegionScaled;
	}

	EnchantmentType(ItemStat itemStat, boolean usesLevels, boolean isCurse, boolean isSpawnable, boolean isRegionScaled) {
		mEnchantment = null;
		mItemStat = itemStat;
		mName = itemStat.getName();
		mUsesLevels = usesLevels;
		mIsCurse = isCurse;
		mIsSpawnable = isSpawnable;
		mIsRegionScaled = isRegionScaled;
	}

	public @Nullable Enchantment getEnchantment() {
		return mEnchantment;
	}

	public @Nullable ItemStat getItemStat() {
		return mItemStat;
	}

	public String getName() {
		return mName;
	}

	public boolean isItemTypeEnchantment() {
		return this == MAGIC_WAND
			|| this == ALCHEMICAL_ALEMBIC;
	}

	public boolean isRegionScaled() {
		return mIsRegionScaled;
	}

	public boolean isHidden() {
		return this == MAINHAND_OFFHAND_DISABLE
			|| this == OFFHAND_MAINHAND_DISABLE
			|| this == HIDE_ATTRIBUTES
			|| this == HIDE_ENCHANTS
			|| this == HIDE_INFO
			|| this == NO_GLINT
			|| this == DELETE_ON_SHATTER;
	}

	public Component getDisplay(int level) {
		if (this == JUNGLES_NOURISHMENT) {
			return Component.text("Jungle's Nourishment", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
		} else if (isItemTypeEnchantment()) {
			return Component.text("* " + mName + " *", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false);
		} else {
			TextColor color = mIsCurse ? NamedTextColor.RED : NamedTextColor.GRAY;
			if (!mUsesLevels && level == 1) {
				return Component.text(mName, color).decoration(TextDecoration.ITALIC, false);
			} else {
				return Component.text(mName + " " + StringUtils.toRoman(level), color).decoration(TextDecoration.ITALIC, false);
			}
		}
	}

	public static @Nullable EnchantmentType getEnchantmentType(String name) {
		return REVERSE_MAPPINGS.get(name.replace(" ", ""));
	}
}
