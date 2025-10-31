package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class RegenerationPercentBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_regeneration_percent";

	@BossParam(help = "Enemy regenerates a percentage of its health every X ticks. Dealing damage to the mob can interrupt the healing.")
	public static class Parameters extends BossParameters {
		@BossParam(help = "Range in blocks that players must be in before this passive spell will run.")
		public int DETECTION = 32;

		@BossParam(help = "Percentage of mob health it will heal. Default 10.")
		public double HEAL_PERCENTAGE = 10;

		@BossParam(help = "Time in ticks before a mob heals again. Default 10, heals twice per second")
		public int TICKS_PER_HEAL = 10;

		@BossParam(help = "Time in ticks after the mob takes damage before it begins healing again. " +
			"Rounds down to the nearest TICKS_PER_HEAL. Default 0")
		public int INTERRUPTION_TICKS = 0;

		@BossParam(help = "Whether the healing is interrupted by DoT effects. Default true (DoT interrupts)")
		public boolean DOT_INTERRUPTS = true;
	}

	int interruptionCycles = 0;

	public void setInterruptionCycles(int cycles) {
		if (cycles <= 0) {
			// don't want it to be negative
			interruptionCycles = 0;
		} else {
			interruptionCycles = cycles;
		}
	}

	public RegenerationPercentBoss(final Plugin plugin, final LivingEntity boss) {
		super(plugin, identityTag, boss);
		final RegenerationPercentBoss.Parameters p = BossParameters.getParameters(mBoss, identityTag, new RegenerationPercentBoss.Parameters());
		List<Spell> passiveSpells = List.of(
			new SpellRunAction(() -> {
				if (!mBoss.isDead()) {
					if (interruptionCycles <= 0) {
						EntityUtils.healMobPercent(mBoss, p.HEAL_PERCENTAGE);
					} else {
						interruptionCycles--;
					}
				}
			})
		);
		super.constructBoss(SpellManager.EMPTY, passiveSpells, p.DETECTION, null, 0, p.TICKS_PER_HEAL);
	}

	private static final List<ClassAbility> DOT_ABILITY_LIST = List.of(
		ClassAbility.SPELLSHOCK_ARCANE,
		// need to take Chiinox's Reflexes code to make Panacea tick 1 count as interruption
		ClassAbility.PANACEA,
		ClassAbility.BRUTAL_ALCHEMY,
		ClassAbility.CURSED_WOUND,
		ClassAbility.SANCTIFIED_ARMOR,
		ClassAbility.INFERNO
	);

	private static final List<DamageEvent.DamageType> DOT_DAMAGE_LIST = List.of(
		DamageEvent.DamageType.THORNS,
		DamageEvent.DamageType.FIRE,
		DamageEvent.DamageType.AILMENT,
		DamageEvent.DamageType.POISON
	);

	@Override
	public void onHurt(DamageEvent event) {
		final RegenerationPercentBoss.Parameters p = BossParameters.getParameters(mBoss, identityTag, new RegenerationPercentBoss.Parameters());
		if (p.DOT_INTERRUPTS) {
			setInterruptionCycles(p.INTERRUPTION_TICKS / p.TICKS_PER_HEAL);
		} else {
			// Interrupt the healing for this many cycles if the damage is not DoT
			if (event.getAbility() == null) {
				// If it's not from an ability, check for thorns/fire/etc.
				if (!DOT_DAMAGE_LIST.contains(event.getType())) {
					setInterruptionCycles(p.INTERRUPTION_TICKS / p.TICKS_PER_HEAL);
				}
			} else if (!DOT_ABILITY_LIST.contains(event.getAbility())) {
				setInterruptionCycles(p.INTERRUPTION_TICKS / p.TICKS_PER_HEAL);
			}
		}
	}
}
