package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.parameters.ParseResult;
import com.playmonumenta.plugins.bosses.parameters.StringReader;
import com.playmonumenta.plugins.bosses.parameters.phases.Trigger.TriggerOperation;
import com.playmonumenta.plugins.events.DamageEvent;
import dev.jorel.commandapi.Tooltip;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.LivingEntity;

public class Phase {

	private final List<Trigger> mTriggers;

	private final List<Action> mActions;

	private String mName = "Phase-" + UUID.randomUUID();

	private boolean mIsReusable = false;

	private Phase(List<Trigger> triggers, List<Action> actions) {
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

	public boolean onHurt(LivingEntity boss, LivingEntity damager, DamageEvent event) {
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



	protected static final Map<String, Trigger.TriggerBuilder> TRIGGER_BUILDER_MAP = new HashMap<>();
	protected static final Map<String, Action.ActionBuilder> ACTION_BUILDER_MAP = new HashMap<>();

	static {
		TRIGGER_BUILDER_MAP.put("ON_SPAWN", OnSpawnTrigger::fromReader);
		TRIGGER_BUILDER_MAP.put("ON_DEATH", OnDeathTrigger::fromReader);
		TRIGGER_BUILDER_MAP.put("ON_DAMAGE", OnDamageTrigger::fromReader);
		TRIGGER_BUILDER_MAP.put("ON_HURT", OnDamageTrigger::fromReader);
		TRIGGER_BUILDER_MAP.put("ON_CAST", BossCastTrigger::fromReader);
		TRIGGER_BUILDER_MAP.put("NEARBY_PLAYERS", NearbyPlayersTrigger::fromReader);
		TRIGGER_BUILDER_MAP.put("HEALTH", HealthTrigger::fromReader);
		TRIGGER_BUILDER_MAP.put("CUSTOM", CustomTrigger::fromReader);


		ACTION_BUILDER_MAP.put("ADD_ABILITY", AddAbilityAction::fromReader);
		ACTION_BUILDER_MAP.put("REMOVE_ABILITY", RemoveAbilityAction::fromReader);
		ACTION_BUILDER_MAP.put("FORCE_CAST", ForceCastAction::fromReader);
		ACTION_BUILDER_MAP.put("CUSTOM", CustomTriggerAction::fromReader);
		ACTION_BUILDER_MAP.put("DELAY_ACTION", DelayAction::fromReader);
		ACTION_BUILDER_MAP.put("COMMAND", CommandAction::fromReader);
	}


	public static ParseResult<Phase> fromReader(StringReader reader) {
		List<Trigger> triggerList = new ArrayList<>();
		List<Action> actionsList = new ArrayList<>();
		int lastThingRead = 0;
		boolean hasReadNegation = false;
		Trigger trigger = null;

		while (true) {
			if (lastThingRead % 2 == 0) {
				//read NOT or a Trigger or skip

				if (!triggerList.isEmpty()) {
					if (reader.advance("->")) {
						break;
					}
				}

				if (reader.advance("NOT")) {
					hasReadNegation = true;
				}
				String name = reader.readOneOf(TRIGGER_BUILDER_MAP.keySet());
				if (name == null) {
					List<Tooltip<String>> suggArgs = new ArrayList<>(TRIGGER_BUILDER_MAP.keySet().size() + 1);
					String soFar = reader.readSoFar();
					for (String valid : TRIGGER_BUILDER_MAP.keySet()) {
						suggArgs.add(Tooltip.of(soFar + valid, "hoverDescription")); //todo add a way to get custom descriptions
					}
					if (!hasReadNegation) {
						suggArgs.add(Tooltip.of(soFar + "NOT ", "negation"));
					}
					return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
				}
				ParseResult<Trigger> parseResult = TRIGGER_BUILDER_MAP.get(name).buildTrigger(reader);
				if (parseResult.getResult() == null) {
					return ParseResult.of(parseResult.getTooltip());
				}
				trigger = parseResult.getResult();
				trigger.setNegated(hasReadNegation);
				triggerList.add(trigger);
				hasReadNegation = false;
				lastThingRead++;

			}

			if (lastThingRead % 2 == 1) {
				if (reader.advance("->")) {
					break;
				}

				TriggerOperation opp = reader.readEnum(TriggerOperation.values());
				if (opp == null) {
					List<Tooltip<String>> suggArgs = new ArrayList<>(TriggerOperation.values().length);
					String soFar = reader.readSoFar();
					for (TriggerOperation valid : TriggerOperation.values()) {
						suggArgs.add(Tooltip.of(soFar + valid.name(), "hoverDescription"));
					}
					suggArgs.add(Tooltip.of(soFar + " ->", "negation"));
					return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
				}

				trigger.setOperation(opp);
				lastThingRead++;
				trigger = null;
			}
		}

		while (true) {
			String name = reader.readOneOf(ACTION_BUILDER_MAP.keySet());
			if (name == null) {
				List<Tooltip<String>> suggArgs = new ArrayList<>(ACTION_BUILDER_MAP.keySet().size() + 1);
				String soFar = reader.readSoFar();
				for (String valid : ACTION_BUILDER_MAP.keySet()) {
					suggArgs.add(Tooltip.of(soFar + valid + " ", "hoverDescription")); //todo add a way to get custom descriptions
				}
				return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
			}

			ParseResult<Action> parseResult = ACTION_BUILDER_MAP.get(name).buildAction(reader);
			if (parseResult.getResult() == null) {
				return ParseResult.of(parseResult.getTooltip());
			}

			actionsList.add(parseResult.getResult());

			if (!reader.advance(",")) {
				break;
			}
		}

		return ParseResult.of(new Phase(triggerList, actionsList));

	}


}
