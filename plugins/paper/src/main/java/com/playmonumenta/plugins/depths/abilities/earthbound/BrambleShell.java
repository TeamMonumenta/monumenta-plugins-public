package com.playmonumenta.plugins.depths.abilities.earthbound;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class BrambleShell extends DepthsAbility {

	public static final String ABILITY_NAME = "Bramble Shell";
	public static final double[] BRAMBLE_DAMAGE = {5, 6, 7, 8, 9, 12};

	public static final DepthsAbilityInfo<BrambleShell> INFO =
		new DepthsAbilityInfo<>(BrambleShell.class, ABILITY_NAME, BrambleShell::new, DepthsTree.EARTHBOUND, DepthsTrigger.PASSIVE)
			.linkedSpell(ClassAbility.BRAMBLE_SHELL)
			.displayItem(Material.SWEET_BERRIES)
			.descriptions(BrambleShell::getDescription)
			.singleCharm(false);

	private final double mDamage;

	public BrambleShell(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.BRAMBLE_SHELL_DAMAGE.mEffectName, BRAMBLE_DAMAGE[mRarity - 1]);
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (source != null
			    && (event.getType() == DamageType.MELEE || event.getType() == DamageType.PROJECTILE)
			    && !event.isBlocked()) {
			Location loc = source.getLocation();
			World world = mPlayer.getWorld();
			new PartialParticle(Particle.BLOCK_CRACK, loc.add(0, source.getHeight() / 2, 0), 25, 0.5, 0.5, 0.5, 0.125, Bukkit.createBlockData(Material.SWEET_BERRY_BUSH)).spawnAsPlayerPassive(mPlayer);
			world.playSound(loc, Sound.BLOCK_SWEET_BERRY_BUSH_BREAK, SoundCategory.PLAYERS, 1, 0.8f);

			DamageUtils.damage(mPlayer, source, DamageType.MELEE_SKILL, mDamage, mInfo.getLinkedSpell(), true);
		}
	}

	private static Description<BrambleShell> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<BrambleShell>(color)
			.add("Whenever an enemy deals melee or projectile damage to you, they take ")
			.addDepthsDamage(a -> a.mDamage, BRAMBLE_DAMAGE[rarity - 1], true)
			.add(" melee damage.");
	}
}
