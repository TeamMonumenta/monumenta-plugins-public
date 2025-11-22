package com.playmonumenta.plugins.abilities;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.util.HSVLike;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * The AbilityInfo class contains static information about an ability.
 * Each {@link Ability} should have a constant field of this type.
 *
 * @author FirelordWeaponry (Fire), Njol
 */
public class AbilityInfo<T extends Ability> {
	private final Class<T> mAbilityClass;
	private final BiFunction<Plugin, Player, T> mConstructor;
	// Ability name as shown in-game
	private final @Nullable String mDisplayName;
	// Ability name shorthand (for statistic purposes; no use in-game. Should be the fewest characters that identifies this)
	private @Nullable String mShorthandName;
	// Ability Abbreviated Name for AbilityHotbar
	private @Nullable String mHotbarName;
	// Description provider
	private List<Description<T>> mDescriptions = List.of((a, p) -> Component.empty());
	// Simplified description
	private @Nullable String mSimpleDescription = null;
	// Color used by action bar messages
	private TextColor mActionBarColor;
	// Secret message during quest216, if this ability has one
	private @Nullable String mQuest216Message = null;

	// If the ability does not require a scoreboardID and just a classId, leave this as null.
	private @Nullable String mScoreboardId = null;

	private @Nullable ClassAbility mLinkedSpell = null;
	private final List<AbilityTriggerInfo<T>> mTriggers = new ArrayList<>();

	private @Nullable Material mDisplayItem;

	private double mPriorityAmount = 1000;

	private boolean mIgnoresSilence = false;

	//This is in ticks; order: [level 1, level 2, level 1 enhanced, level 2 enhanced]; for depths this is one entry per rarity (so 6 in total)
	public @Nullable List<Integer> mCooldowns;
	public @Nullable String mCharmCooldown;
	public @Nullable String mCharmCooldown2;

	private Predicate<Player> mCanUse = player -> mScoreboardId != null && getLevelScore(player) > 0;

	private Consumer<Player> mRemove = player -> {
	};

	public AbilityInfo(Class<T> abilityClass, @Nullable String displayName, BiFunction<Plugin, Player, T> constructor) {
		mAbilityClass = abilityClass;
		mDisplayName = displayName;
		mConstructor = constructor;
		if (displayName == null) {
			mActionBarColor = NamedTextColor.YELLOW;
		} else {
			int hash = displayName.hashCode();
			float hue = (hash & 0xff) / 255.0f;
			float saturation = 0.7f + 0.3f * (hash >> 8 & 0xff) / 255.0f;
			float value = 0.7f + 0.3f * (hash >> 16 & 0xff) / 255.0f;
			mActionBarColor = TextColor.color(HSVLike.hsvLike(hue, saturation, value));
		}
	}

	// builder methods

	public AbilityInfo<T> linkedSpell(ClassAbility linkedSpell) {
		mLinkedSpell = linkedSpell;
		return this;
	}

	public AbilityInfo<T> scoreboardId(String scoreboardId) {
		mScoreboardId = scoreboardId;
		return this;
	}

	public AbilityInfo<T> shorthandName(String shorthandName) {
		mShorthandName = shorthandName;
		return this;
	}

	public AbilityInfo<T> hotbarName(String hotbarName) {
		mHotbarName = hotbarName;
		return this;
	}

	public AbilityInfo<T> cooldown(int cooldown) {
		mCooldowns = List.of(cooldown, cooldown, cooldown, cooldown);
		return this;
	}

	public AbilityInfo<T> cooldown(int... cooldowns) {
		mCooldowns = IntList.of(cooldowns);
		return this;
	}

	public AbilityInfo<T> cooldown(int cooldown1, int cooldown2) {
		mCooldowns = List.of(cooldown1, cooldown2, cooldown1, cooldown2);
		return this;
	}

	public AbilityInfo<T> cooldown(int cooldown, String charmCooldown) {
		mCooldowns = List.of(cooldown, cooldown, cooldown, cooldown);
		mCharmCooldown = charmCooldown;
		return this;
	}

	public AbilityInfo<T> cooldown(int cooldown, String charmCooldown, String charmCooldown2) {
		mCooldowns = List.of(cooldown, cooldown, cooldown, cooldown);
		mCharmCooldown = charmCooldown;
		mCharmCooldown2 = charmCooldown2;
		return this;
	}

	public AbilityInfo<T> cooldown(int cooldown1, int cooldown2, String charmCooldown) {
		mCooldowns = List.of(cooldown1, cooldown2, cooldown1, cooldown2);
		mCharmCooldown = charmCooldown;
		return this;
	}

	public AbilityInfo<T> cooldown(int cooldown1, int cooldown2, String charmCooldown, String charmCooldown2) {
		mCooldowns = List.of(cooldown1, cooldown2, cooldown1, cooldown2);
		mCharmCooldown = charmCooldown;
		mCharmCooldown2 = charmCooldown2;
		return this;
	}

	public AbilityInfo<T> cooldown(int cooldown1, int cooldown2, int cooldownEnhanced, String charmCooldown) {
		mCooldowns = List.of(cooldown1, cooldown2, cooldownEnhanced, cooldownEnhanced);
		mCharmCooldown = charmCooldown;
		return this;
	}

	public AbilityInfo<T> cooldown(int cooldown1, int cooldown2, int cooldownEnhanced1, int cooldownEnhanced2, String charmCooldown) {
		mCooldowns = List.of(cooldown1, cooldown2, cooldownEnhanced1, cooldownEnhanced2);
		mCharmCooldown = charmCooldown;
		return this;
	}

	public AbilityInfo<T> cooldown(String charmCooldown, int... cooldowns) {
		mCooldowns = IntList.of(cooldowns);
		mCharmCooldown = charmCooldown;
		return this;
	}

	public AbilityInfo<T> displayItem(Material displayItem) {
		mDisplayItem = displayItem;
		return this;
	}

	public AbilityInfo<T> descriptions(String level1, String level2) {
		mDescriptions = Stream.of(level1, level2).map(Component::text).map(c -> (Description<T>) (a, p) -> c).toList();
		return this;
	}

	public AbilityInfo<T> descriptions(String level1, String level2, String enhancement) {
		mDescriptions = Stream.of(level1, level2, enhancement).map(Component::text).map(c -> (Description<T>) (a, p) -> c).toList();
		return this;
	}

	public AbilityInfo<T> descriptions(Description<T> level1, Description<T> level2) {
		mDescriptions = Arrays.asList(level1, level2);
		return this;
	}

	public AbilityInfo<T> descriptions(Description<T> level1, Description<T> level2, Description<T> enhancement) {
		mDescriptions = Arrays.asList(level1, level2, enhancement);
		return this;
	}

	public AbilityInfo<T> descriptions(IntFunction<Description<T>> supplier, int levels) {
		mDescriptions = IntStream.range(1, levels + 1).mapToObj(supplier).toList();
		return this;
	}

	public AbilityInfo<T> description(Description<T> description) {
		mDescriptions = List.of(description);
		return this;
	}

	public AbilityInfo<T> description(String description) {
		mDescriptions = List.of((a, p) -> Component.text(description));
		return this;
	}

	public AbilityInfo<T> simpleDescription(String desc) {
		mSimpleDescription = desc;
		return this;
	}

	public AbilityInfo<T> quest216Message(String message) {
		mQuest216Message = message;
		return this;
	}

	public AbilityInfo<T> addTrigger(AbilityTriggerInfo<T> trigger) {
		if (mLinkedSpell == null) {
			throw new IllegalStateException("Missing linked spell for ability with trigger");
		}
		mTriggers.add(trigger);
		return this;
	}

	public AbilityInfo<T> actionBarColor(TextColor color) {
		mActionBarColor = color;
		return this;
	}

	/**
	 * Priority order in event handling, with lower values being handled earlier than higher ones.
	 * <p>
	 * Some references:
	 * <ul>
	 * <li>Default is 1000
	 * <li>Delve modifiers are around 2000
	 * <li>Abilities that need a final damage amount are around 5000
	 * <li>Lifeline abilities are around 10000
	 * </ul>
	 *
	 * @return the priority order
	 */
	public AbilityInfo<T> priorityAmount(double priorityAmount) {
		mPriorityAmount = priorityAmount;
		return this;
	}

	public AbilityInfo<T> ignoresSilence(boolean ignoresSilence) {
		mIgnoresSilence = ignoresSilence;
		return this;
	}

	/**
	 * By default, players can only use abilities if the ability has a scoreboard defined and their value is nonzero.
	 * For different conditions, provide a predicate here.
	 */
	public AbilityInfo<T> canUse(Predicate<Player> canUse) {
		mCanUse = canUse;
		return this;
	}

	/**
	 * Effect to be executed when the player loses this ability, e.g. cleaning up any added attributes.
	 */
	public AbilityInfo<T> remove(Consumer<Player> remove) {
		mRemove = remove;
		return this;
	}

	// other methods

	public int getBaseCooldown(Player player, int score) {
		if (mCooldowns == null) {
			return 0;
		}
		if (!ServerProperties.getAbilityEnhancementsEnabled(player) && score > 2) {
			score -= 2;
		}
		return mCooldowns.get(Math.min(score - 1, mCooldowns.size() - 1));
	}

	public int getModifiedCooldown(Player player, int score) {
		int baseCooldown = getBaseCooldown(player, score);
		return getCharmCooldown(player, baseCooldown);
	}

	public int getCharmCooldown(Player player, int baseCooldown) {
		if (mCharmCooldown != null) {
			int cooldown = CharmManager.getCooldown(player, mCharmCooldown, baseCooldown);
			if (mCharmCooldown2 != null) {
				return CharmManager.getCooldown(player, mCharmCooldown2, cooldown);
			}
			return cooldown;
		}
		return baseCooldown;
	}

	public double getPriorityAmount() {
		return mPriorityAmount;
	}

	public @Nullable String getScoreboard() {
		return mScoreboardId;
	}

	public @Nullable String getDisplayName() {
		return mDisplayName;
	}

	public @Nullable String getHotbarName() {
		if (mHotbarName != null) {
			return mHotbarName;
		} else if (mShorthandName != null) {
			return mShorthandName;
		} else {
			return mDisplayName;
		}
	}

	@Nullable
	public ClassAbility getLinkedSpell() {
		return mLinkedSpell;
	}

	public Class<T> getAbilityClass() {
		return mAbilityClass;
	}

	public T newInstance(Plugin plugin, Player player) {
		return mConstructor.apply(plugin, player);
	}

	public List<AbilityTriggerInfo<T>> getTriggers() {
		return Collections.unmodifiableList(mTriggers);
	}

	public @Nullable AbilityTriggerInfo<T> getTrigger(String id) {
		return mTriggers.stream().filter(t -> t.getId().equals(id)).findFirst().orElse(null);
	}

	public boolean doesIgnoreSilence() {
		return mIgnoresSilence;
	}

	public boolean testCanUse(Player player) {
		return mCanUse.test(player);
	}

	public void onRemove(Player player) {
		mRemove.accept(player);
	}

	public @Nullable Material getDisplayItem() {
		return mDisplayItem == null ? null : mDisplayItem;
	}

	public List<Component> getDescriptions() {
		return getDescriptions(null, null);
	}

	public List<Component> getDescriptions(@Nullable Player player, @Nullable T ability) {
		return mDescriptions.stream().map(d -> d.get(ability, player)).toList();
	}

	public Component getDescription(int level, @Nullable Player player, boolean useAbility) {
		return getDescription(level, player, useAbility ? getPlayerAbility(Plugin.getInstance(), player) : null);
	}

	public Component getDescription(int level, @Nullable Player player, @Nullable T ability) {
		return mDescriptions.get(level - 1).get(ability, player);
	}

	public Component getFormattedDescription(@Nullable Player player, int skillLevel, boolean enabled) throws IndexOutOfBoundsException {
		Component description = getDescription(skillLevel, player, true);

		String displayName = mDisplayName;
		if (displayName == null) {
			displayName = "NULL! Set name properly!";
		}

		boolean coloured = enabled;

		String skillHeader;
		if (skillLevel <= 2) {
			skillHeader = "[" + displayName.toUpperCase(Locale.getDefault()) + " Level " + skillLevel + "] : ";
		} else {
			coloured &= ServerProperties.getAbilityEnhancementsEnabled(player);
			skillHeader = "[" + displayName.toUpperCase(Locale.getDefault()) + " Enhancement] " + (enabled && !ServerProperties.getAbilityEnhancementsEnabled(player) ? "(disabled in this region) " : "") + ": ";
		}

		return Component.text("")
			.append(Component.text(skillHeader, coloured ? NamedTextColor.GREEN : NamedTextColor.GRAY, TextDecoration.BOLD))
			.append(description.color(coloured ? NamedTextColor.YELLOW : NamedTextColor.GRAY));
	}

	public Component getFormattedDescriptions(Player player) {
		int score = getLevelScore(player);
		return getFormattedDescriptions(player, ((score - 1) % 2) + 1, score > 2, !Plugin.getInstance().mAbilityManager.getDisabledSpecAbilities().contains(this), ServerProperties.getAbilityEnhancementsEnabled(player));
	}

	public Component getFormattedDescriptions(@Nullable Player player, int level, boolean isEnhanced, boolean enabled, boolean enhancementEnabled) {
		List<Component> descriptions = getDescriptions();
		Component component = Component.text("");
		component = component.append(getFormattedDescription(player, 1, enabled));
		if (level > 1 && descriptions.size() > 1) {
			component = component.append(Component.newline()).append(getFormattedDescription(player, 2, enabled));
		}
		if (isEnhanced && descriptions.size() > 2) {
			component = component.append(Component.newline()).append(getFormattedDescription(player, 3, enabled && enhancementEnabled));
		}
		return component;
	}

	public @Nullable String getSimpleDescription() {
		return mSimpleDescription;
	}

	public @Nullable String getQuest216Message() {
		return mQuest216Message;
	}

	/*
	 * Returns null if a hover message could not be created
	 */
	public @Nullable Component getLevelHover(@Nullable Player player, int skillLevel, boolean useShorthand, boolean enabled) {
		int level = skillLevel;
		boolean isEnhanced = false;
		if (skillLevel > 2) {
			level -= 2;
			isEnhanced = true;
		}

		String hoverableString;
		if (useShorthand) {
			if (mShorthandName == null) {
				return null;
			}
			hoverableString = mShorthandName + level;
		} else {
			if (mDisplayName == null) {
				return null;
			}
			hoverableString = mDisplayName.toUpperCase(Locale.getDefault()) + " Level " + level;
		}
		if (isEnhanced) {
			hoverableString += "*";
		}
		return Component.text(hoverableString, NamedTextColor.YELLOW)
			.hoverEvent(getFormattedDescriptions(player, level, isEnhanced, enabled, ServerProperties.getAbilityEnhancementsEnabled(player)));
	}

	public TextColor getActionBarColor() {
		return mActionBarColor;
	}

	public void sendDescriptions(CommandSender sender) {
		sender.sendMessage(getFormattedDescriptions(sender instanceof Player player ? player : null, 2, false, true, true));
	}

	public @Nullable T getPlayerAbility(Plugin plugin, @Nullable Player player) {
		return plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, getAbilityClass());
	}

	public int getLevelScore(Player player) {
		return mScoreboardId == null ? 0 : ScoreboardUtils.getScoreboardValue(player, mScoreboardId).orElse(0);
	}

	public boolean hasCooldown() {
		return mCooldowns != null && !mCooldowns.isEmpty();
	}

	public JsonObject toJson() {
		JsonObject info = new JsonObject();
		if (mScoreboardId != null) {
			info.addProperty("scoreboardId", mScoreboardId);
		}
		if (mLinkedSpell != null) {
			info.addProperty("name", mLinkedSpell.getName());
		}
		if (mDisplayName != null) {
			info.addProperty("displayName", mDisplayName);
		}
		if (mShorthandName != null) {
			info.addProperty("shortName", mShorthandName);
		}
		if (mSimpleDescription != null) {
			info.addProperty("simpleDescription", mSimpleDescription);
		}
		List<Component> descriptionList = getDescriptions();
		if (!descriptionList.isEmpty()) {
			JsonArray descriptions = new JsonArray();
			for (Component description : descriptionList) {
				descriptions.add(MessagingUtils.plainText(description));
			}
			info.add("descriptions", descriptions);
		}
		if (!mTriggers.isEmpty()) {
			JsonArray descriptions = new JsonArray();
			for (AbilityTriggerInfo<T> trigger : mTriggers) {
				descriptions.add(trigger.toJson());
			}
			info.add("triggers", descriptions);
		}
		if (mCooldowns != null) {
			JsonArray cooldowns = new JsonArray();
			for (Integer cooldown : mCooldowns) {
				cooldowns.add(cooldown);
			}
			info.add("cooldowns", cooldowns);
		}
		info.addProperty("actionBarColor", mActionBarColor.asHexString());
		return info;
	}

	@Override
	public String toString() {
		return "AbilityInfo{" +
			"mAbilityClass=" + mAbilityClass +
			'}';
	}
}
