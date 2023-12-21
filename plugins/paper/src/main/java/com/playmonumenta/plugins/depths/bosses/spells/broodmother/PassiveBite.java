package com.playmonumenta.plugins.depths.bosses.spells.broodmother;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseAbstractRectangleAttack;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.bosses.Broodmother;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.structures.StructuresAPI;
import java.util.Collection;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

public class PassiveBite extends Spell {

	public static final String SPELL_NAME = "Bite";
	public static final int CAST_DELAY = 60;
	public static final int CAST_DELAY_A15_DECREASE = 10;
	public static final int ANIMATION_TIME = 10;
	public static final int ANIMATION_LINGER_TIME = 15;
	public static final int TELEGRAPH_DURATION = 10;
	public static final int RECOVERY_TIME = 180;
	public static final int TELEGRAPH_UNITS = 25;
	public static final int TELEGRAPH_PULSES = 4;
	public static final double PARTICLE_SPEED = 1.8;
	public static final double DAMAGE = 70;

	private final Broodmother mBroodmother;
	private final LivingEntity mBoss;
	private final @Nullable DepthsParty mParty;
	private final Plugin mPlugin;
	private final SpellBaseAbstractRectangleAttack.RectangleInfo mBiteInfo;
	private final SpellBaseAbstractRectangleAttack mBiteAttack;
	private final int mFinalRecoveryTime;
	private final int mFinalCastDelay;

	private int mTimer = 0;

	public PassiveBite(Broodmother broodmother, LivingEntity boss, @Nullable DepthsParty party) {
		mBroodmother = broodmother;
		mBoss = boss;
		mParty = party;
		mPlugin = Plugin.getInstance();

		mFinalRecoveryTime = DepthsParty.getAscensionEightCooldown(RECOVERY_TIME, party);
		mFinalCastDelay = getCastDelay(party);

		mBiteInfo = new SpellBaseAbstractRectangleAttack.RectangleInfo(mBoss.getLocation().clone().add(2.5, -1, -6.5), -13, 13);

		mBiteAttack = new SpellBaseAbstractRectangleAttack(
			mBiteInfo, TELEGRAPH_UNITS, TELEGRAPH_PULSES, TELEGRAPH_DURATION, PARTICLE_SPEED, Particle.SCRAPE, DamageEvent.DamageType.MELEE, 0,
			true, true, SPELL_NAME, Particle.CRIT,
			mPlugin, mBoss,
			(bosss) -> bosss.getWorld().playSound(bosss.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.HOSTILE, 3, 2)
		);
	}

	@Override
	public void run() {
		if (mTimer >= mFinalRecoveryTime) {
			// Activate the spell if a player got in the bounding box of the attack.
			BoundingBox box = BoundingBox.of(mBiteInfo.getCenter(), mBiteInfo.getHalfDx(), 10, mBiteInfo.getHalfDz());
			Collection<Player> players = mBoss.getLocation().getNearbyPlayers(20);
			for (Player player : players) {
				if (box.overlaps(player.getBoundingBox())) {
					mTimer = 0;
					doBite();
					break;
				}
			}
		} else if (!mBroodmother.isCastingDisruptiveSpell()) {
			mTimer++;
		}
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}

	public void doBite() {
		// Warning sound
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 1.5f, 2.0f);
		mBiteAttack.run();
		// Bite Damage and Animation
		mActiveTasks.add(
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				// The damage has to happen some time after the end of the telegraph.
				// Create a new bounding box
				Hitbox hitbox = new Hitbox.AABBHitbox(mBoss.getWorld(), BoundingBox.of(mBiteInfo.getCenter(), mBiteInfo.getHalfDx(), 30, mBiteInfo.getHalfDz()));
				// Hit the players in it
				List<Player> hitPlayers = hitbox.getHitPlayers(true);
				for (Player hitPlayer : hitPlayers) {
					DamageUtils.damage(mBoss, hitPlayer, DamageEvent.DamageType.MELEE, DAMAGE, null, true, true, "Bite");
					PotionUtils.applyPotion(mBoss, hitPlayer, new PotionEffect(PotionEffectType.WITHER, 100, 2));
					if (mParty != null && mParty.getAscension() >= 4) {
						EntityUtils.applyVulnerability(Plugin.getInstance(), 100, Broodmother.getVulnerabilityAmount(mParty), hitPlayer);
					}
				}
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.HOSTILE, 3, 2);
				StructuresAPI.loadAndPasteStructure("BikeSpiderBite0", mBoss.getLocation().clone().add(-8, -1, -12), false, false);
				// Reset Mouth
				mActiveTasks.add(
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						StructuresAPI.loadAndPasteStructure("BikeSpiderBiteReset", mBoss.getLocation().clone().add(-8, -1, -12), false, false);
					}, ANIMATION_LINGER_TIME)
				);
			}, mFinalCastDelay - ANIMATION_TIME)
		);
	}

	private int getCastDelay(@Nullable DepthsParty party) {
		int castDelay = CAST_DELAY;
		if (party != null && party.getAscension() >= 15) {
			castDelay -= CAST_DELAY_A15_DECREASE;
		}
		return castDelay;
	}
}
