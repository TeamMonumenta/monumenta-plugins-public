package com.playmonumenta.plugins.bosses.spells.intruder;

import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellCooldownManager;
import com.playmonumenta.plugins.bosses.spells.SpellSlashAttack;
import com.playmonumenta.plugins.events.DamageEvent;
import org.bukkit.Color;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;


public class SpellTwistedSwipe extends SpellSlashAttack {
	private final SpellCooldownManager mSpellCooldownManager;

	public SpellTwistedSwipe(LivingEntity boss, boolean enhanced) {
		super(com.playmonumenta.plugins.Plugin.getInstance(), boss, 0, enhanced ? 22 : 18, enhanced ? 20 : 30,
			2.5, 8, 8, "Twisted Swipe", 8, 10,
			170, 0.2,
			Color.BLACK,
			Color.fromRGB(0x810121),
			Color.fromRGB(0xff3333),
			false, true, false, true, new Vector(1, 0.5, 1), true, 1,
			false, true, 0.5, -1, DamageEvent.DamageType.MELEE,
			SoundsList.builder()
				.add(new SoundsList.CSound(Sound.ENTITY_WITHER_SKELETON_AMBIENT, 2.0f, 0.1f))
				.build(),
			SoundsList.builder()
				.add(new SoundsList.CSound(Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, 0.4f))
				.add(new SoundsList.CSound(Sound.ENTITY_WITHER_SKELETON_HURT, 0.8f, 0.1f))
				.build(),
			SoundsList.EMPTY,
			SoundsList.EMPTY,
			true, 2, 15, false,
			false, 1, false
		);

		mSpellCooldownManager = new SpellCooldownManager(enhanced ? 4 * 20 : 5 * 20, 2 * 20, boss::isValid, boss::hasAI);
	}

	@Override
	public void run() {
		if (!canRun()) {
			return;
		}
		mSpellCooldownManager.setOnCooldown();
		super.run();
	}

	@Override
	public void bossCastAbility(SpellCastEvent event) {
		if (event.getSpell() instanceof SpellScreamroom) {
			cancel();
		}
	}

	@Override
	public boolean canRun() {
		return !mSpellCooldownManager.onCooldown() && mBoss.hasAI();
	}

	@Override
	public int cooldownTicks() {
		return 2;
	}
}
