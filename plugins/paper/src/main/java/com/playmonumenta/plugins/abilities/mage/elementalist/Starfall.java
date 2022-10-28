package com.playmonumenta.plugins.abilities.mage.elementalist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.mage.elementalist.StarfallCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Starfall extends Ability {
	public static final String NAME = "Starfall";
	public static final ClassAbility ABILITY = ClassAbility.STARFALL;

	public static final int DAMAGE_1 = 13;
	public static final int DAMAGE_2 = 23;
	public static final int SIZE = 6;
	public static final int DISTANCE = 25;
	public static final int FIRE_SECONDS = 5;
	public static final int FIRE_TICKS = FIRE_SECONDS * 20;
	public static final float KNOCKBACK = 0.7f;
	public static final int COOLDOWN_SECONDS = 18;
	public static final int COOLDOWN_TICKS = COOLDOWN_SECONDS * 20;
	public static final String CHARM_DAMAGE = "Starfall Damage";
	public static final String CHARM_RANGE = "Starfall Range";
	public static final String CHARM_COOLDOWN = "Starfall Cooldown";
	public static final String CHARM_FIRE = "Starfall Fire Duration";

	private final float mLevelDamage;
	private final StarfallCS mCosmetic;

	public Starfall(Plugin plugin, @Nullable Player player) {
		super(plugin, player, NAME);
		mInfo.mLinkedSpell = ABILITY;

		mInfo.mScoreboardId = NAME;
		mInfo.mShorthandName = "SF";
		mInfo.mDescriptions.add(
			String.format(
				"While holding a wand, pressing the swap key marks where you're looking, up to %s blocks away." +
					" You summon a falling meteor above the mark that lands strongly, dealing %s fire magic damage to all enemies in a %s block radius around it," +
					" setting them on fire for %ss, and knocking them away. Cooldown: %ss.",
				DISTANCE,
				DAMAGE_1,
				SIZE,
				FIRE_SECONDS,
				COOLDOWN_SECONDS
			)
		);
		mInfo.mDescriptions.add(
			String.format(
				"Damage is increased from %s to %s.",
				DAMAGE_1,
				DAMAGE_2
			)
		);
		mInfo.mCooldown = CharmManager.getCooldown(player, CHARM_COOLDOWN, COOLDOWN_TICKS);
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.MAGMA_BLOCK, 1);

		mLevelDamage = (float) CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new StarfallCS(), StarfallCS.SKIN_LIST);
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		if (mPlugin.mItemStatManager.getPlayerItemStats(mPlayer).getItemStats().get(ItemStatUtils.EnchantmentType.MAGIC_WAND) > 0) {
			event.setCancelled(true);

			if (!isTimerActive()
				    && !mPlayer.isSneaking()) {
				putOnCooldown();

				Location loc = mPlayer.getEyeLocation();
				World world = mPlayer.getWorld();

				ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
				float damage = SpellPower.getSpellDamage(mPlugin, mPlayer, mLevelDamage);
				mCosmetic.starfallCastEffect(world, mPlayer);
				Vector dir = loc.getDirection().normalize();
				for (int i = 0; i < DISTANCE; i++) {
					loc.add(dir);

					mCosmetic.starfallCastTrail(loc, mPlayer);
					int size = EntityUtils.getNearbyMobs(loc, 2, mPlayer).size();
					if (!loc.isChunkLoaded() || loc.getBlock().getType().isSolid() || i >= DISTANCE - 1 || size > 0) {
						launchMeteor(loc, playerItemStats, damage);
						break;
					}
				}
			}
		}
	}

	private void launchMeteor(final Location loc, final ItemStatManager.PlayerItemStats playerItemStats, final float damage) {
		Location ogLoc = loc.clone();
		loc.add(0, 40, 0);

		new BukkitRunnable() {
			double mT = 0;

			@Override
			public void run() {
				mT += 1;
				World world = mPlayer.getWorld();
				for (int i = 0; i < 8; i++) {
					loc.subtract(0, 0.25, 0);
					if (!loc.isChunkLoaded() || loc.getBlock().getType().isSolid()) {
						if (loc.getY() - ogLoc.getY() <= 2) {
							mCosmetic.starfallLandEffect(world, mPlayer, loc);
							this.cancel();

							Hitbox hitbox = new Hitbox.SphereHitbox(loc, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RANGE, SIZE));
							for (LivingEntity e : hitbox.getHitMobs()) {
								EntityUtils.applyFire(mPlugin, FIRE_TICKS + CharmManager.getExtraDuration(mPlayer, CHARM_FIRE), e, mPlayer, playerItemStats);
								DamageUtils.damage(mPlayer, e, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.mLinkedSpell, playerItemStats), damage, true, true, false);
								MovementUtils.knockAway(loc, e, KNOCKBACK, true);
							}
							break;
						}
					}
				}
				mCosmetic.starfallFallEffect(world, mPlayer, loc);

				if (mT >= 50) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}
}
