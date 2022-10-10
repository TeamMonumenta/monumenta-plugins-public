package com.playmonumenta.plugins.abilities.mage.arcanist;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.mage.ElementalArrows;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.AstralOmenArcaneStacks;
import com.playmonumenta.plugins.effects.AstralOmenBonusDamage;
import com.playmonumenta.plugins.effects.AstralOmenFireStacks;
import com.playmonumenta.plugins.effects.AstralOmenIceStacks;
import com.playmonumenta.plugins.effects.AstralOmenThunderStacks;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AstralOmen extends Ability {
	public static final String NAME = "Astral Omen";
	public static final ClassAbility ABILITY = ClassAbility.ASTRAL_OMEN;
	public static final String STACKS_SOURCE_ARCANE = "AstralOmenArcaneStacks";
	public static final String STACKS_SOURCE_FIRE = "AstralOmenFireStacks";
	public static final String STACKS_SOURCE_ICE = "AstralOmenIceStacks";
	public static final String STACKS_SOURCE_THUNDER = "AstralOmenThunderStacks";
	public static final String BONUS_DAMAGE_SOURCE = "AstralOmenBonusDamage";
	public static final String DAMAGED_THIS_TICK_METAKEY = "AstralOmenDamagedThisTick";

	public static final int DAMAGE = 8;
	public static final int SIZE = 3;
	public static final double BONUS_MULTIPLIER = 0.2;
	public static final double BOW_MULTIPLIER = 0.4;
	public static final int STACK_TICKS = 10 * Constants.TICKS_PER_SECOND;
	public static final int BONUS_TICKS = 8 * Constants.TICKS_PER_SECOND;
	public static final float PULL_SPEED = 0.25f;
	public static final int STACK_THRESHOLD = 2;
	public static final String CHARM_DAMAGE = "Astral Omen Damage";
	public static final String CHARM_MODIFIER = "Astral Omen Damage Modifier";
	public static final String CHARM_STACK = "Astral Omen Stack Threshold";
	public static final String CHARM_RANGE = "Astral Omen Range";

	private final double mLevelBonusMultiplier;

	private @Nullable ElementalArrows mElementalArrows;

	private static final Map<ClassAbility, Type> mElementClassification;

	static {
		mElementClassification = new HashMap<>();
		// Arcane Types
		mElementClassification.put(ClassAbility.ARCANE_STRIKE, Type.ARCANE);
		mElementClassification.put(ClassAbility.MANA_LANCE, Type.ARCANE);
		mElementClassification.put(ClassAbility.COSMIC_MOONBLADE, Type.ARCANE);
		// Fire Types
		mElementClassification.put(ClassAbility.MAGMA_SHIELD, Type.FIRE);
		mElementClassification.put(ClassAbility.ELEMENTAL_ARROWS_FIRE, Type.FIRE);
		// Ice Types
		mElementClassification.put(ClassAbility.FROST_NOVA, Type.ICE);
		mElementClassification.put(ClassAbility.ELEMENTAL_ARROWS_ICE, Type.ICE);
		// Thunder Types
		mElementClassification.put(ClassAbility.THUNDER_STEP, Type.THUNDER);
		mElementClassification.put(ClassAbility.ELEMENTAL_ARROWS, Type.THUNDER);
	}

	public enum Type {
		ARCANE(STACKS_SOURCE_ARCANE, AstralOmenArcaneStacks.COLOR),
		FIRE(STACKS_SOURCE_FIRE, AstralOmenFireStacks.COLOR),
		ICE(STACKS_SOURCE_ICE, AstralOmenIceStacks.COLOR),
		THUNDER(STACKS_SOURCE_THUNDER, AstralOmenThunderStacks.COLOR);

		private final String mSource;
		private final Particle.DustOptions mColor;

		Type(String source, Particle.DustOptions color) {
			mSource = source;
			mColor = color;
		}
	}

	public AstralOmen(Plugin plugin, @Nullable Player player) {
		super(plugin, player, NAME);
		mInfo.mLinkedSpell = ABILITY;

		mInfo.mScoreboardId = "AstralOmen";
		mInfo.mShorthandName = "AO";
		mInfo.mDescriptions.add(
			String.format(
				"Dealing spell damage to an enemy marks its fate, giving it an omen based on the spell type (Arcane, Fire, Ice, Thunder). If an enemy hits %s omens of different types, its fate is sealed, clearing its omens and causing a magical implosion, dealing %s magic damage to it and all enemies in a %s-block cube around it. If the spell was %s, the implosion instead does %s%% of the bow's original damage. An enemy loses all its omens after %ss of it not gaining another omen. That implosion's damage ignores iframes and itself cannot apply omens.",
				STACK_THRESHOLD,
				DAMAGE,
				SIZE,
				ElementalArrows.NAME,
				StringUtils.multiplierToPercentage(BOW_MULTIPLIER),
				StringUtils.ticksToSeconds(STACK_TICKS)
			)
		);
		mInfo.mDescriptions.add(
			String.format(
				"The implosion now pulls all enemies inwards. Enemies hit by the implosion now take %s%% more damage from you for %ss.",
				StringUtils.multiplierToPercentage(BONUS_MULTIPLIER),
				StringUtils.ticksToSeconds(BONUS_TICKS)
			)
		);
		mDisplayItem = new ItemStack(Material.NETHER_STAR, 1);
		mLevelBonusMultiplier = (isLevelTwo() ? BONUS_MULTIPLIER : 0) + CharmManager.getLevelPercentDecimal(player, CHARM_MODIFIER);

		Bukkit.getScheduler().runTask(plugin, () -> {
			mElementalArrows = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, ElementalArrows.class);
		});
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (mPlayer == null || event.getAbility() == null || event.getAbility() == mInfo.mLinkedSpell || event.getAbility() == ClassAbility.SPELLSHOCK) {
			return false;
		}

		ClassAbility ability = event.getAbility();

		HashMap<Type, Integer> levels = new HashMap<>();
		int combo = 0;

		for (Type type : Type.values()) {
			String source = type.mSource;

			NavigableSet<Effect> stacks = mPlugin.mEffectManager.getEffects(enemy, source);

			int level = (stacks == null ? 0 : (int) stacks.last().getMagnitude())
				+ (mElementClassification.getOrDefault(ability, null) == type ? 1 : 0);
			levels.put(type, level);

			if (level > 0) {
				combo++;
			}

			if (stacks != null) {
				mPlugin.mEffectManager.clearEffects(enemy, source);
			}
		}

		int stacksThreshold = STACK_THRESHOLD + (int) CharmManager.getLevel(mPlayer, CHARM_STACK);
		if (combo >= stacksThreshold) { // Adding 1 more stack would hit threshold, which removes all stacks anyway, so don't bother adding then removing
			World world = enemy.getWorld();
			float baseDamage = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, DAMAGE);
			float spellDamage;
			if (ability == ClassAbility.ELEMENTAL_ARROWS || ability == ClassAbility.ELEMENTAL_ARROWS_FIRE || ability == ClassAbility.ELEMENTAL_ARROWS_ICE) {
				if (mElementalArrows == null) {
					// how?
					MMLog.warning("Dealt Elemental Arrows damage with Astral Omen despite not finding Elemental Arrows (player: " + mPlayer.getName() + ")");
					spellDamage = baseDamage;
				} else {
					spellDamage = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, mElementalArrows.getLastDamage() * BOW_MULTIPLIER);
				}
			} else {
				spellDamage = SpellPower.getSpellDamage(mPlugin, mPlayer, baseDamage);
			}
			for (LivingEntity mob : EntityUtils.getNearbyMobs(enemy.getLocation(), CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RANGE, SIZE))) {
				if (MetadataUtils.checkOnceThisTick(mPlugin, mob, DAMAGED_THIS_TICK_METAKEY)) {
					DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, spellDamage, mInfo.mLinkedSpell, true);
					if (isLevelOne()) {
						MovementUtils.pullTowards(enemy, mob, PULL_SPEED);
					}
					for (Type type : levels.keySet()) {
						if (levels.get(type) > 0) {
							new PartialParticle(Particle.REDSTONE, enemy.getLocation(), levels.get(type) * 5, 0.2, 0.2, 0.2, 0.1, type.mColor).spawnAsPlayerActive(mPlayer);
						}
					}
				}
			}

			if (mLevelBonusMultiplier > 0) {
				mPlugin.mEffectManager.addEffect(enemy, BONUS_DAMAGE_SOURCE, new AstralOmenBonusDamage(BONUS_TICKS, mLevelBonusMultiplier, mPlayer));
			}

			Location loc = enemy.getLocation();
			new PartialParticle(Particle.ENCHANTMENT_TABLE, loc, 80, 0, 0, 0, 4).spawnAsPlayerActive(mPlayer);
			for (Type type : levels.keySet()) {
				if (levels.get(type) > 0) {
					new PartialParticle(Particle.REDSTONE, loc, levels.get(type) * 5, 0.2, 0.2, 0.2, 0.1, type.mColor).spawnAsPlayerActive(mPlayer);
				}
			}
			world.playSound(loc, Sound.ENTITY_BLAZE_HURT, 1.3f, 1.5f);
		} else {
			// Effect implements Comparable in compareTo(), which uses the internal magnitude
			// When EffectManager does addEffect(), it add()s to NavigableSet, which presumably uses compareTo()
			// So, seems effects of the same magnitude will not have multiple added!
			//
			// We just work with 1 effect at all times, of the magnitude (level) we want,
			// which will handle stack decay times appropriately & not have conflicting magnitudes
			for (Type type : levels.keySet()) {
				int level = levels.get(type);
				if (level > 0) {
					Effect effect;
					switch (type) {
						case FIRE -> effect = new AstralOmenFireStacks(STACK_TICKS, level);
						case ICE -> effect = new AstralOmenIceStacks(STACK_TICKS, level);
						case THUNDER -> effect = new AstralOmenThunderStacks(STACK_TICKS, level);
						default -> effect = new AstralOmenArcaneStacks(STACK_TICKS, level);
					}
					mPlugin.mEffectManager.addEffect(enemy, type.mSource, effect);
				}
			}
		}

		return false; // Needs to apply to all damaged mobs. Uses an internal check to prevent recursion on dealing damage.
	}
}
