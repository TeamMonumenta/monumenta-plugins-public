package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.VectorUtils;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class SacredConstructs extends Ability {

	private static final int COOLDOWN = 90 * 20;
	public static final Style CONSTRUCT_COLOR = Style.style(TextColor.color(0xE8E8D3));

	public static final AbilityInfo<SacredConstructs> INFO =
		new AbilityInfo<>(SacredConstructs.class, "Sacred Constructs", SacredConstructs::new)
			.linkedSpell(ClassAbility.SACRED_CONSTRUCTS)
			.scoreboardId("SacredConstructs")
			.shorthandName("SC")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("It's like that twisted ability")
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", SacredConstructs::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true).lookDirections(AbilityTrigger.LookDirection.DOWN)))
			.displayItem(Material.IRON_INGOT);


	public SacredConstructs(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();
		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 0.8f);
		for (int i = 0; i < 3; i++) {
			IronGolem golem = (IronGolem) LibraryOfSoulsIntegration.summon(mPlayer.getLocation().add(VectorUtils.rotateTargetDirection(new Vector(3, 0, 0), 120 * i, 0)), "SacredConstruct");
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				if (golem != null) {
					golem.remove();
				}
			}, 200);
		}
		return true;
	}

	private static Description<SacredConstructs> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addDashedLine();
	}

	private static Description<SacredConstructs> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addDashedLine();
	}

	private static Description<SacredConstructs> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addTrigger()
			.addDashedLine()
			.addLine("Make everyone on the server immortal for the next")
			.addLine("hour and summon %d *Sacred Constructs* to protect").statValues(stat(3)).styles(CONSTRUCT_COLOR)
			.addLine("you with %d hp which never flinches. If a hostile").statValues(stat(123))
			.addLine("monster is nearby, the *Constructs* will go Super").styles(CONSTRUCT_COLOR)
			.addLine("Twisted Tempest Mode on them and deal %d (s)").statValues(stat(750))
			.addLine("magic damage every second.")
			.addLine("The *Constructs* mimic all your Cleric abilities").styles(CONSTRUCT_COLOR)
			.addLine("and give cooldown reduction. When the *Constructs*").styles(CONSTRUCT_COLOR)
			.addLine("die, call upon the Architect's wrath and smite the")
			.addLine("enemies that killed them, for %d magic damage. But").statValues(stat(5000))
			.addLine("be careful... if they were killed by a player instead,")
			.addLine("that counts as team griefing and you get banned.")
			.addDashedLine();
	}

}
