package com.playmonumenta.plugins.abilities.mage.arcanist;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.AstralOmenBonusDamage;
import com.playmonumenta.plugins.effects.AstralOmenStacks;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.NavigableSet;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.NavigableSet;

public class AstralOmen extends Ability {
	public static final String NAME = "Astral Omen";
	public static final ClassAbility ABILITY = ClassAbility.ASTRAL_OMEN;
	public static final String STACKS_SOURCE = "AstralOmenStacks";
	public static final String BONUS_DAMAGE_SOURCE = "AstralOmenBonusDamage";
	private static final Particle.DustOptions COLOR_PURPLE = AstralOmenStacks.COLOR_PURPLE;
	public static final String DAMAGED_THIS_TICK_METAKEY = "AstralOmenDamagedThisTick";

	public static final int DAMAGE = 6;
	public static final int SIZE = 3;
	public static final double BONUS_MULTIPLIER_1 = 0.2;
	public static final double BONUS_MULTIPLIER_2 = 0.3;
	public static final int STACK_TICKS = 10 * Constants.TICKS_PER_SECOND;
	public static final int BONUS_TICKS = 8 * Constants.TICKS_PER_SECOND;
	public static final float PULL_SPEED = 0.175f;
	public static final int STACK_THRESHOLD = 3;

	private final double mLevelBonusMultiplier;
	private final boolean mDoPull;

	public AstralOmen(Plugin plugin, @Nullable Player player) {
		super(plugin, player, NAME);
		mInfo.mLinkedSpell = ABILITY;

		mInfo.mScoreboardId = "AstralOmen";
		mInfo.mShorthandName = "AO";
		mInfo.mDescriptions.add(
				String.format(
						"Dealing spell damage to an enemy marks its fate, giving it an astral omen. If an enemy hits %s omens, its fate is sealed, clearing its omens and causing a magical implosion that deals %s magic damage to it and all enemies in a %s-block cube around it. It then takes %s%% more damage from you for %ss. An enemy loses all its omens after %ss of it not gaining another omen. That implosion's damage ignores iframes and itself cannot apply omens.",
						STACK_THRESHOLD,
						DAMAGE,
						SIZE,
						StringUtils.multiplierToPercentage(BONUS_MULTIPLIER_1),
						StringUtils.ticksToSeconds(BONUS_TICKS),
						StringUtils.ticksToSeconds(STACK_TICKS)
				)
		);
		mInfo.mDescriptions.add(
			String.format(
				"The implosion now pulls all enemies inwards. Bonus damage taken is increased from %s%% to %s%%.",
				StringUtils.multiplierToPercentage(BONUS_MULTIPLIER_1),
				StringUtils.multiplierToPercentage(BONUS_MULTIPLIER_2)
			)
		);
		mDisplayItem = new ItemStack(Material.NETHER_STAR, 1);

		mLevelBonusMultiplier = isLevelOne() ? BONUS_MULTIPLIER_1 : BONUS_MULTIPLIER_2;
		mDoPull = isLevelTwo();
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getAbility() == null || event.getAbility() == mInfo.mLinkedSpell || event.getAbility() == ClassAbility.SPELLSHOCK) {
			return false;
		}

		NavigableSet<Effect> stacks = mPlugin.mEffectManager.getEffects(enemy, STACKS_SOURCE);
		int level = stacks == null ? 0 : (int) stacks.last().getMagnitude();

		if (stacks != null) {
			mPlugin.mEffectManager.clearEffects(enemy, STACKS_SOURCE);
		}

		if (level >= STACK_THRESHOLD - 1) { // Adding 1 more stack would hit threshold, which removes all stacks anyway, so don't bother adding then removing
			World world = enemy.getWorld();
			float spellDamage = SpellPower.getSpellDamage(mPlugin, mPlayer, DAMAGE);
			for (LivingEntity mob : EntityUtils.getNearbyMobs(enemy.getLocation(), SIZE)) {
				if (MetadataUtils.checkOnceThisTick(mPlugin, mob, DAMAGED_THIS_TICK_METAKEY)) {
					DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, spellDamage, mInfo.mLinkedSpell, true);
					if (mDoPull) {
						MovementUtils.pullTowards(enemy, mob, PULL_SPEED);
					}

					new PartialParticle(Particle.REDSTONE, enemy.getLocation(), 10, 0.2, 0.2, 0.2, 0.1, COLOR_PURPLE).spawnAsPlayerActive(mPlayer);
				}
			}

			mPlugin.mEffectManager.addEffect(enemy, BONUS_DAMAGE_SOURCE, new AstralOmenBonusDamage(BONUS_TICKS, mLevelBonusMultiplier, mPlayer));

			Location loc = enemy.getLocation();
			new PartialParticle(Particle.ENCHANTMENT_TABLE, loc, 80, 0, 0, 0, 4).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.REDSTONE, loc, 20, 3, 3, 3, 0.1, COLOR_PURPLE).spawnAsPlayerActive(mPlayer);
			world.playSound(loc, Sound.ENTITY_BLAZE_HURT, 1.3f, 1.5f);
		} else {
			// Effect implements Comparable in compareTo(), which uses the internal magnitude
			// When EffectManager does addEffect(), it add()s to NavigableSet, which presumably uses compareTo()
			// So, seems effects of the same magnitude will not have multiple added!
			//
			// We just work with 1 effect at all times, of the magnitude (level) we want,
			// which will handle stack decay times appropriately & not have conflicting magnitudes
			mPlugin.mEffectManager.addEffect(enemy, STACKS_SOURCE, new AstralOmenStacks(STACK_TICKS, ++level));
		}
		return false; // Needs to apply to all damaged mobs. Uses an internal check to prevent recursion on dealing damage.
	}
}
