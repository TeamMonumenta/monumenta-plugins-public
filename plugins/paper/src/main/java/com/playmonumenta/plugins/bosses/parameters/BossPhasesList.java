package com.playmonumenta.plugins.bosses.parameters;

import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.parameters.phases.Phase;
import com.playmonumenta.plugins.events.DamageEvent;
import dev.jorel.commandapi.SuggestionInfo;
import dev.jorel.commandapi.Tooltip;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class BossPhasesList {

	private final ConcurrentLinkedDeque<Phase> mPhases;

	public BossPhasesList(List<Phase> phases) {
		mPhases = new ConcurrentLinkedDeque<>(phases);
	}

	public void addBossPhases(BossPhasesList other) {
		mPhases.addAll(other.mPhases);
	}

	public void onSpawn(LivingEntity boss) {
		// Yes this works effectively as a for each and remove
		mPhases.removeIf(phase -> phase.onSpawn(boss) && !phase.isReusable());
	}

	public void onDeath(LivingEntity boss) {
		mPhases.removeIf(phase -> phase.onDeath(boss) && !phase.isReusable());
	}

	public void onDamage(LivingEntity boss, LivingEntity damagee, DamageEvent event) {
		mPhases.removeIf(phase -> phase.onDamage(boss, damagee, event) && !phase.isReusable());
	}

	public void onHurt(LivingEntity boss, @Nullable LivingEntity damager, DamageEvent event) {
		mPhases.removeIf(phase -> phase.onHurt(boss, damager, event) && !phase.isReusable());
	}

	public void onBossCastAbility(LivingEntity boss, SpellCastEvent event) {
		mPhases.removeIf(phase -> phase.onBossCastAbility(boss, event) && !phase.isReusable());
	}

	public void tick(LivingEntity boss, int ticks) {
		mPhases.removeIf(phase -> phase.tick(boss, ticks) && !phase.isReusable());
	}

	public void onCustom(LivingEntity boss, String key) {
		mPhases.removeIf(phase -> phase.onCustom(boss, key) && !phase.isReusable());
	}

	public void onFlag(LivingEntity boss, String key, boolean state) {
		mPhases.removeIf(phase -> phase.onFlag(boss, key, state) && !phase.isReusable());
	}

	public void onShoot(LivingEntity boss) {
		mPhases.removeIf(phase -> phase.onShoot(boss) && !phase.isReusable());
	}


	public static BossPhasesList emptyPhaseList() {
		return new BossPhasesList(new ArrayList<>());
	}

	public static Collection<Tooltip<String>> suggestionPhases(SuggestionInfo<CommandSender> info) {
		String currentArg = info.currentArg();

		try {
			Parser.parseTriggerActions(new Tokenizer(currentArg).getTokens());
		} catch (Parser.ParseError e) {
			return e.getSuggestions("");
		}

		return List.of();
	}
}
