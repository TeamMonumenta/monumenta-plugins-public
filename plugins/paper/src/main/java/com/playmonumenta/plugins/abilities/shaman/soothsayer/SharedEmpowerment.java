package com.playmonumenta.plugins.abilities.shaman.soothsayer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.AbilityUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class SharedEmpowerment extends Ability {

	public static final AbilityInfo<SharedEmpowerment> INFO =
		new AbilityInfo<>(SharedEmpowerment.class, "Shared Empowerment", SharedEmpowerment::new)
			.linkedSpell(ClassAbility.SHARED_EMPOWERMENT)
			.scoreboardId("SharedEmpowerment")
			.shorthandName("SE")
			.descriptions(
				"All players within 10 blocks of you receive the benefits of your passive Totemic Empowerment, and you receive a 3% boost to the speed and damage reduction.",
				"You and all players within 10 blocks of you receive a 5% boost to the speed and damage reduction of your Totemic Empowerment.")
			.simpleDescription("Your Totemic Empowerment is now shared to those within a large radius of you.")
			.displayItem(Material.BEACON);

	public SharedEmpowerment(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AuditListener.logSevere(player.getName() + " has accessed shaman abilities incorrectly, class has been reset, please report to developers.");
			AbilityUtils.resetClass(player);
		}
	}
}
