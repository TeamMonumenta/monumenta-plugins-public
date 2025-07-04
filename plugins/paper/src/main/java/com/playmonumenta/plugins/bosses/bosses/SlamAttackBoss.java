package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellColossalBruteForce;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public final class SlamAttackBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_slamattack";

	public static class Parameters extends BossParameters {
		@BossParam(help = "not written")
		public int RADIUS = 3;
		@BossParam(help = "not written")
		public int DAMAGE = 20;
		@BossParam(help = "Time it takes for the spell to charge")
		public int DELAY = 10;
		@BossParam(help = "not written")
		public int COOLDOWN = 8 * 20;
		@BossParam(help = "Whether or not the nova attack can be blocked by a shield. (Default = true)")
		public boolean CAN_BLOCK = true;
		@BossParam(help = "Amount of consecutive slams done")
		public int COMBOS = 1;
		@BossParam(help = "Delay in ticks between each slam in the combo")
		public int COMBO_DELAY = 10;
		@BossParam(help = "not written")
		public double DAMAGE_PERCENTAGE = 0.0;
		@BossParam(help = "Effect applied to players hit by the nova")
		public EffectsList EFFECTS = EffectsList.EMPTY;
		@BossParam(help = "not written")
		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET;
		@BossParam(help = "If the boss can move while charging the attack")
		public boolean CAN_MOVE_WHEN_CHARGING = false;
		@BossParam(help = "If the boss can move while unleashing the attack")
		public boolean CAN_MOVE_WHEN_CASTING = false;
		@BossParam(help = "Sound played while charging")
		public SoundsList CHARGE_SOUND = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_BLAZE_BURN, 0.5f, 0.2f))
			.build();
	}

	public SlamAttackBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		SlamAttackBoss.Parameters p = BossParameters.getParameters(boss, identityTag, new SlamAttackBoss.Parameters());

		Spell spell = new SpellColossalBruteForce(plugin, boss, p.TARGETS, p.DELAY, p.COOLDOWN,
			p.RADIUS, p.COMBOS, p.COMBO_DELAY, p.DAMAGE, p.DAMAGE_PERCENTAGE, p.CAN_BLOCK, p.EFFECTS,
			p.CAN_MOVE_WHEN_CHARGING, p.CAN_MOVE_WHEN_CASTING, p.CHARGE_SOUND);

		super.constructBoss(spell, (int) p.TARGETS.getRange(), null, 0);
	}
}