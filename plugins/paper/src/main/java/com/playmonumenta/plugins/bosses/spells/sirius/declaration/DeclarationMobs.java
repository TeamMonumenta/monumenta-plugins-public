package com.playmonumenta.plugins.bosses.spells.sirius.declaration;

import com.playmonumenta.plugins.bosses.bosses.sirius.Sirius;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.sirius.SpellSummonTheStars;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public class DeclarationMobs extends Spell {
	private final SpellSummonTheStars mSpawner;
	private final Sirius mSirius;
	private static final int DURATION = 15 * 20;
	private static final double MOBSPERPLAYER = 1.5;

	public DeclarationMobs(SpellSummonTheStars spawner, Sirius sirius) {
		mSpawner = spawner;
		mSirius = sirius;
	}

	@Override
	public void run() {
		for (Player p : mSirius.getPlayers()) {
			MessagingUtils.sendNPCMessage(p, "Sirius", Component.text("Come forth bearers of starlight!", NamedTextColor.AQUA, TextDecoration.BOLD));
			p.playSound(p, Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.HOSTILE, 1f, 0.7f);
			p.playSound(p, Sound.ENTITY_POLAR_BEAR_WARNING, SoundCategory.HOSTILE, 0.4f, 0.8f);
		}
		mSpawner.mobDeclaration(DURATION, MOBSPERPLAYER);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
