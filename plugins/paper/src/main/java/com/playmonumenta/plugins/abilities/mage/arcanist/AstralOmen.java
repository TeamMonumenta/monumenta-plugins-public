package com.playmonumenta.plugins.abilities.mage.arcanist;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.mage.arcanist.AstralOmenCS;
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
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class AstralOmen extends Ability {
	public static final String NAME = "Astral Omen";
	public static final ClassAbility ABILITY = ClassAbility.ASTRAL_OMEN;
	public static final String STACKS_SOURCE_ARCANE = "AstralOmenArcaneStacks";
	public static final String STACKS_SOURCE_FIRE = "AstralOmenFireStacks";
	public static final String STACKS_SOURCE_ICE = "AstralOmenIceStacks";
	public static final String STACKS_SOURCE_THUNDER = "AstralOmenThunderStacks";
	public static final String BONUS_DAMAGE_SOURCE = "AstralOmenBonusDamage";
	public static final String DAMAGED_THIS_TICK_METAKEY = "AstralOmenDamagedThisTick";

	public static final double DAMAGE_1 = 7;
	public static final double DAMAGE_2 = 7.5;
	public static final int RADIUS = 3;
	public static final double BONUS_MULTIPLIER = 0.15;
	public static final int STACK_TICKS = 10 * Constants.TICKS_PER_SECOND;
	public static final int BONUS_TICKS = 8 * Constants.TICKS_PER_SECOND;
	public static final float PULL_SPEED = 0.25f;
	public static final int STACK_THRESHOLD = 2;

	public static final String CHARM_DAMAGE = "Astral Omen Damage";
	public static final String CHARM_MODIFIER = "Astral Omen Damage Modifier";
	public static final String CHARM_STACK = "Astral Omen Stack Threshold";
	public static final String CHARM_RANGE = "Astral Omen Range";
	public static final String CHARM_PULL = "Astral Omen Pull Speed";

	private static final Map<ClassAbility, Type> mElementClassification;

	public static final AbilityInfo<AstralOmen> INFO =
		new AbilityInfo<>(AstralOmen.class, NAME, AstralOmen::new)
			.linkedSpell(ABILITY)
			.scoreboardId("AstralOmen")
			.shorthandName("AO")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Upon damaging a mob with multiple types of magic, deal damage to nearby mobs.")
			.displayItem(Material.NETHER_STAR);

	static {
		mElementClassification = new HashMap<>();
		// Arcane Types
		mElementClassification.put(ClassAbility.ARCANE_STRIKE, Type.ARCANE);
		mElementClassification.put(ClassAbility.ARCANE_STRIKE_ENHANCED, Type.ARCANE);
		mElementClassification.put(ClassAbility.MANA_LANCE, Type.ARCANE);
		mElementClassification.put(ClassAbility.COSMIC_MOONBLADE, Type.ARCANE);
		// Fire Types
		mElementClassification.put(ClassAbility.MAGMA_SHIELD, Type.FIRE);
		// Ice Types
		mElementClassification.put(ClassAbility.FROST_NOVA, Type.ICE);
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
		public final Particle.DustOptions mColor;

		Type(String source, Particle.DustOptions color) {
			mSource = source;
			mColor = color;
		}
	}

	private final double mLevelBonusMultiplier;
	private final double mDamage;
	private final int mStackThreshold;
	private final double mRadius;

	private final AstralOmenCS mCosmetic;

	public AstralOmen(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mLevelBonusMultiplier = BONUS_MULTIPLIER + CharmManager.getLevelPercentDecimal(player, CHARM_MODIFIER);
		mStackThreshold = STACK_THRESHOLD + (int) CharmManager.getLevel(mPlayer, CHARM_STACK);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelTwo() ? DAMAGE_2 : DAMAGE_1);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RANGE, RADIUS);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new AstralOmenCS());
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getAbility() == null || event.getAbility().isFake() || event.getAbility() == mInfo.getLinkedSpell() || event.getAbility() == ClassAbility.SPELLSHOCK) {
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

		if (combo >= mStackThreshold) { // Adding 1 more stack would hit threshold, which removes all stacks anyway, so don't bother adding then removing
			float spellDamage = SpellPower.getSpellDamage(mPlugin, mPlayer, (float) mDamage);
			Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(enemy), mRadius);
			for (LivingEntity mob : hitbox.getHitMobs()) {
				if (MetadataUtils.checkOnceThisTick(mPlugin, mob, DAMAGED_THIS_TICK_METAKEY)) {
					DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, spellDamage, mInfo.getLinkedSpell(), true);
					if (isLevelTwo()) {
						MovementUtils.pullTowards(enemy, mob, (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_PULL, PULL_SPEED));
					}
					for (Map.Entry<Type, Integer> entry : levels.entrySet()) {
						if (entry.getValue() > 0) {
							mCosmetic.clearEffect(mPlayer, enemy, entry, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RANGE, RADIUS));
						}
					}
				}
			}

			if (isLevelTwo()) {
				mPlugin.mEffectManager.addEffect(enemy, BONUS_DAMAGE_SOURCE, new AstralOmenBonusDamage(BONUS_TICKS, mLevelBonusMultiplier, mPlayer, mCosmetic).deleteOnAbilityUpdate(true));
			}

		} else {
			// Effect implements Comparable in compareTo(), which uses the internal magnitude
			// When EffectManager does addEffect(), it add()s to NavigableSet, which presumably uses compareTo()
			// So, seems effects of the same magnitude will not have multiple added!
			//
			// We just work with 1 effect at all times, of the magnitude (level) we want,
			// which will handle stack decay times appropriately & not have conflicting magnitudes
			for (Map.Entry<Type, Integer> e : levels.entrySet()) {
				Type type = e.getKey();
				int level = e.getValue();
				if (level > 0) {
					Effect effect;
					switch (type) {
						case FIRE ->
							effect = new AstralOmenFireStacks(STACK_TICKS, level, mPlayer, mCosmetic).deleteOnAbilityUpdate(true);
						case ICE ->
							effect = new AstralOmenIceStacks(STACK_TICKS, level, mPlayer, mCosmetic).deleteOnAbilityUpdate(true);
						case THUNDER ->
							effect = new AstralOmenThunderStacks(STACK_TICKS, level, mPlayer, mCosmetic).deleteOnAbilityUpdate(true);
						default ->
							effect = new AstralOmenArcaneStacks(STACK_TICKS, level, mPlayer, mCosmetic).deleteOnAbilityUpdate(true);
					}
					mPlugin.mEffectManager.addEffect(enemy, type.mSource, effect);
				}
			}
		}

		return false; // Needs to apply to all damaged mobs. Uses an internal check to prevent recursion on dealing damage.
	}

	private static Description<AstralOmen> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Dealing spell damage to an enemy marks its fate, giving it an omen based on the spell type (Arcane, Fire, Ice, Thunder). If an enemy hits ")
			.add(a -> a.mStackThreshold, STACK_THRESHOLD)
			.add(" omens of different types, its fate is sealed, clearing its omens and causing a magical implosion, dealing ")
			.add(a -> a.mDamage, DAMAGE_1, false, Ability::isLevelOne)
			.add(" magic damage to it and all enemies within ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks. An enemy loses all its omens after ")
			.addDuration(STACK_TICKS)
			.add(" seconds of it not gaining another omen. That implosion's damage cannot apply omens. Omens cannot be applied or sealed by Elemental Arrows.");
	}

	private static Description<AstralOmen> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The implosion now pulls all enemies inwards and deals ")
			.add(a -> a.mDamage, DAMAGE_2, false, Ability::isLevelTwo)
			.add(" damage. Enemies hit by the implosion now take ")
			.addPercent(a -> a.mLevelBonusMultiplier, BONUS_MULTIPLIER)
			.add(" more damage from you for ")
			.addDuration(BONUS_TICKS)
			.add(" seconds.");
	}
}
