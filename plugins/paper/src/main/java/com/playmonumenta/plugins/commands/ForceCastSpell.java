package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.bukkit.entity.Entity;

public class ForceCastSpell {

	public static void register() {
		new CommandAPICommand("forcecastspell")
			.withPermission("monumenta.command.forcecastspell")
			.withArguments(
				new EntitySelectorArgument.OneEntity("boss"),
				new StringArgument("spell class")
					.replaceSuggestions(ArgumentSuggestions.strings(info -> getSpellClasses((Entity) info.previousArgs()[0])))
			)
			.executes((sender, args) -> {
				execute((Entity) args[0], (String) args[1]);
			})
			.register();
	}

	private static String[] getSpellClasses(Entity e) {
		return Stream.concat(Stream.of("random"), BossManager.getInstance().getAbilities(e).stream()
				.flatMap(a -> a.getActiveSpells().stream().map(s -> s.getClass().getSimpleName())))
			.toArray(String[]::new);
	}

	private static void execute(Entity boss, String spellClassName) {
		if (spellClassName.equals("random")) {
			List<BossAbilityGroup> abilities = BossManager.getInstance().getAbilities(boss);
			Collections.shuffle(abilities);
			for (BossAbilityGroup bossAbilityGroup : abilities) {
				if (!bossAbilityGroup.getActiveSpells().isEmpty()) {
					bossAbilityGroup.forceCastRandomSpell();
					return;
				}
			}
			return;
		}
		for (BossAbilityGroup bossAbilityGroup : BossManager.getInstance().getAbilities(boss)) {
			for (Spell spell : bossAbilityGroup.getActiveSpells()) {
				if (spell.getClass().getSimpleName().equals(spellClassName)) {
					bossAbilityGroup.forceCastSpell(spell.getClass());
					return;
				}
			}
		}

	}

}
