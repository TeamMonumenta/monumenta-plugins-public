package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.parameters.phases.Trigger.TriggerOperation;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class Phase {

	private final List<Trigger> mTriggers;

	private final List<Action> mActions;

	private String mName = "Phase-" + UUID.randomUUID();

	private boolean mIsReusable = false;

	public Phase(String name, boolean reusable, List<Trigger> triggers, List<Action> actions) {
		mName = name;
		mIsReusable = reusable;
		mTriggers = triggers;
		mActions = actions;
	}

	public void setName(String name) {
		mName = name;
	}

	public String getName() {
		return mName;
	}

	public void setReusable(boolean reusable) {
		mIsReusable = reusable;
	}

	public boolean isReusable() {
		return mIsReusable;
	}

	public boolean onSpawn(LivingEntity boss) {
		List<Trigger> temp = new ArrayList<>();
		for (Trigger trigger : mTriggers) {
			if (trigger.onSpawn(boss)) {
				temp.add(trigger);
			}
		}


		if (!temp.isEmpty()) {
			runTest(temp, boss);
			return true;
		}
		return false;
	}

	public boolean onDeath(LivingEntity boss) {
		List<Trigger> temp = new ArrayList<>();
		for (Trigger trigger : mTriggers) {
			if (trigger.onDeath(boss)) {
				temp.add(trigger);
			}
		}

		if (!temp.isEmpty()) {
			runTest(temp, boss);
			return true;
		}
		return false;
	}

	public boolean onDamage(LivingEntity boss, LivingEntity damagee, DamageEvent event) {
		List<Trigger> temp = new ArrayList<>();
		for (Trigger trigger : mTriggers) {
			if (trigger.onDamage(boss, damagee, event)) {
				temp.add(trigger);
			}
		}

		if (!temp.isEmpty()) {
			runTest(temp, boss);
			return true;
		}
		return false;
	}

	public boolean onHurt(LivingEntity boss, @Nullable LivingEntity damager, DamageEvent event) {
		List<Trigger> temp = new ArrayList<>();
		for (Trigger trigger : mTriggers) {
			if (trigger.onHurt(boss, damager, event)) {
				temp.add(trigger);
			}
		}

		if (!temp.isEmpty()) {
			runTest(temp, boss);
			return true;
		}
		return false;
	}


	public boolean onBossCastAbility(LivingEntity boss, SpellCastEvent event) {
		List<Trigger> temp = new ArrayList<>();
		for (Trigger trigger : mTriggers) {
			if (trigger.onBossCastAbility(boss, event)) {
				temp.add(trigger);
			}
		}

		if (!temp.isEmpty()) {
			runTest(temp, boss);
			return true;
		}
		return false;
	}

	public boolean tick(LivingEntity boss, int tick) {
		List<Trigger> temp = new ArrayList<>();
		for (Trigger trigger : mTriggers) {
			if (trigger.tick(boss, tick)) {
				temp.add(trigger);
			}
		}

		if (!temp.isEmpty()) {
			runTest(temp, boss);
			return true;
		}
		return false;
	}

	public boolean onCustom(LivingEntity boss, String key) {
		List<Trigger> temp = new ArrayList<>();
		for (Trigger trigger : mTriggers) {
			if (trigger.custom(boss, key)) {
				temp.add(trigger);
			}
		}

		if (!temp.isEmpty()) {
			runTest(temp, boss);
			return true;
		}
		return false;

	}

	public boolean onFlag(LivingEntity boss, String key, boolean state) {
		List<Trigger> temp = new ArrayList<>();
		for (Trigger trigger : mTriggers) {
			if (trigger.flag(boss, key, state)) {
				temp.add(trigger);
			}
		}

		if (!temp.isEmpty()) {
			runTest(temp, boss);
			return true;
		}
		return false;

	}

	public boolean onShoot(LivingEntity boss) {
		List<Trigger> temp = mTriggers.stream()
			.filter(trigger -> trigger.onShoot(boss))
			.toList();

		if (!temp.isEmpty()) {
			runTest(temp, boss);
			return true;
		}
		return false;
	}

	private void runTest(List<Trigger> triggers, LivingEntity boss) {
		boolean runActions = true;
		TriggerOperation opp = null;

		for (Trigger trigger : mTriggers) {
			if (!triggers.contains(trigger)) {
				if (opp == null) {
					runActions = trigger.realTest(boss);
				} else if (opp == TriggerOperation.AND) {
					runActions &= trigger.realTest(boss);
				} else if (opp == TriggerOperation.OR) {
					runActions |= trigger.realTest(boss);
				} else if (opp == TriggerOperation.XOR) {
					runActions ^= trigger.realTest(boss);
				}
			} else {
				if (opp != TriggerOperation.AND) {
					if (opp == TriggerOperation.XOR) {
						runActions ^= true;
					} else {
						runActions = true;
					}
				}
			}
			opp = trigger.getOperation();
		}

		if (runActions) {
			if (isReusable()) {
				resetTriggers(boss);
			}
			runAction(boss);
		}
	}

	private void resetTriggers(LivingEntity boss) {
		for (Trigger trigger : mTriggers) {
			trigger.reset(boss);
		}
	}

	private void runAction(LivingEntity boss) {
		for (Action action : mActions) {
			action.runAction(boss);
		}
	}
}
