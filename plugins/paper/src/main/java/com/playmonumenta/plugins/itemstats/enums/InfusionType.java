package com.playmonumenta.plugins.itemstats.enums;

import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.itemstats.ItemStat;
import com.playmonumenta.plugins.itemstats.enchantments.Hexed;
import com.playmonumenta.plugins.itemstats.infusions.*;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.luckperms.api.model.group.Group;
import org.jetbrains.annotations.Nullable;

public enum InfusionType {
	// Infusions
	ACUMEN(new Acumen(), "", true, false, false, false, true, false, true, true),
	FOCUS(new Focus(), "", true, false, false, false, true, false, true, true),
	PERSPICACITY(new Perspicacity(), "", true, false, false, false, true, false, true, true),
	TENACITY(new Tenacity(), "", true, false, false, false, true, false, true, true),
	VIGOR(new Vigor(), "", true, false, false, false, true, false, true, true),
	VITALITY(new Vitality(), "", true, false, false, false, true, false, true, true),

	// Delve Infusions
	ANTIGRAV(new AntiGrav(), "", true, false, false, false, true, true, true, false),
	ARDOR(new Ardor(), "", true, false, false, false, true, true, true, false),
	AURA(new Aura(), "", true, false, false, false, true, true, true, false),
	CARAPACE(new Carapace(), "", true, false, false, false, true, true, true, false),
	CELERITY(new Celerity(), "", true, false, false, false, true, true, true, false),
	CELESTIAL(new Celestial(), "", true, false, false, false, true, true, true, false),
	CHOLER(new Choler(), "", true, false, false, false, true, true, true, false),
	DECAPITATION(new Decapitation(), "", true, false, false, false, true, true, true, false),
	EMPOWERED(new Empowered(), "", true, false, false, false, true, true, true, false),
	ENERGIZE(new Energize(), "", true, false, false, false, true, true, true, false),
	EPOCH(new Epoch(), "", true, false, false, false, true, true, true, false),
	EXECUTION(new Execution(), "", true, false, false, false, true, true, true, false),
	EXPEDITE(new Expedite(), "", true, false, false, false, true, true, true, false),
	FERVOR(new Fervor(), "", true, false, false, false, true, true, true, false),
	FUELED(new Fueled(), "", true, false, false, false, true, true, true, false),
	GALVANIC(new Galvanic(), "", true, false, false, false, true, true, true, false),
	GRACE(new Grace(), "", true, false, false, false, true, true, true, false),
	MITOSIS(new Mitosis(), "", true, false, false, false, true, true, true, false),
	NATANT(new Natant(), "", true, false, false, false, true, true, true, false),
	NUTRIMENT(new Nutriment(), "", true, false, false, false, true, true, true, false),
	ORBITAL(new Orbital(), "", true, false, false, false, true, true, true, false),
	PENNATE(new Pennate(), "", true, false, false, false, true, true, true, false),
	QUENCH(new Quench(), "", true, false, false, false, true, true, true, false),
	REFLECTION(new Reflection(), "", true, false, false, false, true, true, true, false),
	REFRESH(new Refresh(), "", true, false, false, false, true, true, true, false),
	SOOTHING(new Soothing(), "", true, false, false, false, true, true, true, false),
	STURDY(new Sturdy(), "", true, false, false, false, true, true, true, false),
	UNDERSTANDING(new Understanding(), "", true, false, false, false, true, true, true, false),
	UNYIELDING(new Unyielding(), "", true, false, false, false, true, true, true, false),
	USURPER(new Usurper(), "", true, false, false, false, true, true, true, false),
	VENGEFUL(new Vengeful(), "", true, false, false, false, true, true, true, false),

	// Other Added Tags
	LOCKED(new Locked(), "", false, false, false, false, false, false, false, false),
	ENLIGHTENING(new Enlightening(), "", false, false, false, false, true, false, false, false),
	BARKING(new Barking(), "", true, false, true, false, false, false, false, false),
	DEBARKING(new Debarking(), "", false, false, false, false, false, false, false, false),
	RUSTWORTHY(new Rustworthy(), "", true, true, false, false, false, false, false, false),
	UNRUSTWORTHY(new Unrustworthy(), "", false, false, false, false, false, false, false, false),
	WAX_ON(new WaxOn(), "", false, false, false, false, false, false, false, false),
	WAX_OFF(new WaxOff(), "", false, false, false, false, false, false, false, false),
	// Ichor Imbuements
	ICHOR_DAWNBRINGER(new IchorDawnbringer(), "", false, false, false, false, false, false, true, false),
	ICHOR_EARTHBOUND(new IchorEarthbound(), "", false, false, false, false, false, false, true, false),
	ICHOR_FLAMECALLER(new IchorFlamecaller(), "", false, false, false, false, false, false, true, false),
	ICHOR_FROSTBORN(new IchorFrostborn(), "", false, false, false, false, false, false, true, false),
	ICHOR_PRISMATIC(new IchorPrismatic(), "", false, false, false, false, false, false, true, false),
	ICHOR_SHADOWDANCER(new IchorShadowdancer(), "", false, false, false, false, false, false, true, false),
	ICHOR_STEELSAGE(new IchorSteelsage(), "", false, false, false, false, false, false, true, false),
	ICHOR_WINDWALKER(new IchorWindwalker(), "", false, false, false, false, false, false, true, false),
	// Boss/Special Infusions
	HOPE(new Hope(), "Hoped", false, false, true, false, false, false, true, false),
	COLOSSAL(new Colossal(), "Reinforced", false, false, false, false, false, false, true, false),
	PHYLACTERY(new Phylactery(), "Embalmed", false, false, false, false, false, false, true, false),
	REVELATION(new Revelation(), "Invoked", false, false, false, false, false, false, true, false),
	SOULBOUND(new Soulbound(), "Soulbound", false, false, false, false, false, false, false, false),
	FESTIVE(new Festive(), "Decorated", false, false, true, false, false, false, false, false),
	GILDED(new Gilded(), "Gilded", false, false, true, false, false, false, false, false),
	OWNED(new Owned(), "Owned", false, false, false, false, false, false, false, false),
	SHATTERED(new Shattered(), "", true, false, false, false, false, false, false, false),
	HEXED(new Hexed(), "", false, false, false, false, false, false, false, false),
	HUNT_TRACK(new HuntTrack(), "", false, false, false, false, false, false, false, false),
	// Stat tracking stuff
	STAT_TRACK(new StatTrack(), "Tracked", false, false, false, false, false, false, false, false),
	STAT_TRACK_KILLS(new StatTrackKills(), "", true, false, false, true, false, false, false, false),
	STAT_TRACK_DAMAGE(new StatTrackDamage(), "", true, false, false, true, false, false, false, false),
	STAT_TRACK_MELEE(new StatTrackMelee(), "", true, false, false, true, false, false, false, false),
	STAT_TRACK_PROJECTILE(new StatTrackProjectile(), "", true, false, false, true, false, false, false, false),
	STAT_TRACK_MAGIC(new StatTrackMagic(), "", true, false, false, true, false, false, false, false),
	STAT_TRACK_BOSS(new StatTrackBoss(), "", true, false, false, true, false, false, false, false),
	STAT_TRACK_SPAWNER(new StatTrackSpawners(), "", true, false, false, true, false, false, false, false),
	STAT_TRACK_CONSUMED(new StatTrackConsumed(), "", true, false, false, true, false, false, false, false),
	STAT_TRACK_BLOCKS(new StatTrackBlocks(), "", true, false, false, true, false, false, false, false),
	STAT_TRACK_RIPTIDE(new StatTrackRiptide(), "", true, false, false, true, false, false, false, false),
	STAT_TRACK_BLOCKS_BROKEN(new StatTrackBlocksBroken(), "", true, false, false, true, false, false, false, false),
	STAT_TRACK_SHIELD_BLOCKED(new StatTrackShielded(), "", true, false, false, true, false, false, false, false),
	STAT_TRACK_DEATH(new StatTrackDeath(), "", true, false, false, true, false, false, false, false),
	STAT_TRACK_REPAIR(new StatTrackRepair(), "", true, false, false, true, false, false, false, false),
	STAT_TRACK_CONVERT(new StatTrackConvert(), "", true, false, false, true, false, false, false, false),
	STAT_TRACK_DAMAGE_TAKEN(new StatTrackDamageTaken(), "", true, false, false, true, false, false, false, false),
	STAT_TRACK_HEALING_DONE(new StatTrackHealingDone(), "", true, false, false, true, false, false, false, false),
	STAT_TRACK_FISH_CAUGHT(new StatTrackFishCaught(), "", true, false, false, true, false, false, false, false),
	STAT_TRACK_CHEST_BROKEN(new StatTrackChests(), "", true, false, false, true, false, false, false, false);

	public static final Map<String, InfusionType> REVERSE_MAPPINGS = Arrays.stream(values())
		.collect(Collectors.toUnmodifiableMap(type -> type.getName().replace(" ", ""), type -> type));

	public static final Set<InfusionType> STAT_TRACK_OPTIONS = Arrays.stream(values())
		.filter(type -> type.mIsStatTrackOption)
		.collect(Collectors.toUnmodifiableSet());

	public static final Set<InfusionType> SPAWNABLE_INFUSIONS = Arrays.stream(values())
		.filter(type -> type.mIsSpawnable)
		.collect(Collectors.toUnmodifiableSet());

	public static final String KEY = "Infusions";

	final ItemStat mItemStat;
	final String mName;
	final String mMessage;
	final boolean mIsSpawnable;
	final boolean mHasLevels;
	final boolean mIsCurse;
	final boolean mIsStatTrackOption;
	final boolean mIsDisabledByShatter;
	final boolean mIsDelveInfusion;
	final boolean mIsDataCollected;
	final boolean mIsBaseInfusion;

	InfusionType(ItemStat itemStat, String message, boolean hasLevels, boolean isCurse, boolean isSpawnable, boolean isStatTrackOption, boolean isDisabledByShatter, boolean isDelveInfusion, boolean isDataCollected, boolean isBaseInfusion) {
		mItemStat = itemStat;
		mName = itemStat.getName();
		mIsSpawnable = isSpawnable;
		mHasLevels = hasLevels;
		mIsCurse = isCurse;
		mMessage = message;
		mIsStatTrackOption = isStatTrackOption;
		mIsDisabledByShatter = isDisabledByShatter;
		mIsDelveInfusion = isDelveInfusion;
		mIsDataCollected = isDataCollected;
		mIsBaseInfusion = isBaseInfusion;
	}

	public ItemStat getItemStat() {
		return mItemStat;
	}

	public String getName() {
		return mName;
	}

	public String getMessage() {
		return mMessage;
	}

	public boolean isDisabledByShatter() {
		return mIsDisabledByShatter;
	}

	public boolean isStatTrackOption() {
		return mIsStatTrackOption;
	}

	public boolean isDelveInfusion() {
		return mIsDelveInfusion;
	}

	public boolean isDataCollected() {
		return mIsDataCollected;
	}

	public boolean isBaseInfusion() {
		return mIsBaseInfusion;
	}

	public Component getDisplay(int level, @Nullable String infuser) {
		TextColor color = mIsCurse ? NamedTextColor.RED : NamedTextColor.GRAY;
		if (!mHasLevels) {
			if (mMessage.isEmpty() || infuser == null) {
				return Component.text(mName, color).decoration(TextDecoration.ITALIC, false);
			} else if (this == OWNED && infuser.contains("guild#")) {
				Long guildPlotId = Long.parseLong(infuser.substring(6));
				Group guild = LuckPermsIntegration.getLoadedGuildByPlotId(guildPlotId);
				if (guild == null) {
					return Component.text(mName, color).decoration(TextDecoration.ITALIC, false);
				}
				return Component.text(mName, color).decoration(TextDecoration.ITALIC, false).append(Component.text(" (" + mMessage + " by ", NamedTextColor.DARK_GRAY).append(Component.text("[" + LuckPermsIntegration.getGuildPlainTag(guild) + "]", LuckPermsIntegration.getGuildColor(guild))).append(Component.text(")", NamedTextColor.DARK_GRAY)));
			} else if (this == SOULBOUND) {
				return Component.text(mName, color).decoration(TextDecoration.ITALIC, false).append(Component.text(" (" + mMessage + " to " + infuser + ")", NamedTextColor.DARK_GRAY));
			} else {
				return Component.text(mName, color).decoration(TextDecoration.ITALIC, false).append(Component.text(" (" + mMessage + " by " + infuser + ")", NamedTextColor.DARK_GRAY));
			}
		} else if (mIsStatTrackOption) {
			return Component.text(mName + ": " + (level - 1), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
		} else {
			if (mMessage.isEmpty() || infuser == null) {
				return Component.text(mName + " " + StringUtils.toRoman(level), color).decoration(TextDecoration.ITALIC, false);
			} else {
				return Component.text(mName + " " + StringUtils.toRoman(level), color).decoration(TextDecoration.ITALIC, false).append(Component.text(" (" + mMessage + " by " + infuser + ")", NamedTextColor.DARK_GRAY));
			}
		}
	}

	public Component getDisplay(int level) {
		return getDisplay(level, null);
	}

	public boolean isHidden() {
		return this == SHATTERED;
	}

	public static @Nullable InfusionType getInfusionType(String name) {
		return REVERSE_MAPPINGS.get(name.replace(" ", ""));
	}
}
