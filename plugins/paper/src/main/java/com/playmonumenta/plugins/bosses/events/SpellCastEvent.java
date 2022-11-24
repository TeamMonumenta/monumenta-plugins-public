package com.playmonumenta.plugins.bosses.events;

import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SpellCastEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final LivingEntity mBoss;
	private final BossAbilityGroup mBossAbilityGroup;
	private final Spell mSpell;

	public SpellCastEvent(LivingEntity boss, BossAbilityGroup bossAbilityGroup, Spell spell) {
		mBoss = boss;
		mBossAbilityGroup = bossAbilityGroup;
		mSpell = spell;
	}

	public LivingEntity getBoss() {
		return mBoss;
	}

	public BossAbilityGroup getBossAbilityGroup() {
		return mBossAbilityGroup;
	}

	public Spell getSpell() {
		return mSpell;
	}

	// Mandatory Event Methods (If you remove these, I'm 99% sure the event will break)

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

}
