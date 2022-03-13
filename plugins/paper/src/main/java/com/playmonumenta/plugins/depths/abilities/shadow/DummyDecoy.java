package com.playmonumenta.plugins.depths.abilities.shadow;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.abilities.DummyDecoyBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.aspects.BowAspect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class DummyDecoy extends DepthsAbility {

	public static final String ABILITY_NAME = "Dummy Decoy";

	public static final int COOLDOWN = 25 * 20;
	public static final String DUMMY_NAME = "AlluringShadow";
	public static final int[] HEALTH = {30, 35, 40, 45, 50, 60};
	public static final int[] STUN_TICKS = {20, 25, 30, 35, 40, 50};
	public static final int MAX_TICKS = 10 * 20;
	public static final int AGGRO_RADIUS = 8;
	public static final int STUN_RADIUS = 4;
	public static final String DUMMY_DECOY_ARROW_METADATA = "DummyDecoyArrow";

	public DummyDecoy(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayMaterial = Material.ARMOR_STAND;
		mTree = DepthsTree.SHADOWS;
		mInfo.mLinkedSpell = ClassAbility.DUMMY_DECOY;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mIgnoreCooldown = true;
	}

	public void execute(AbstractArrow arrow) {

		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, 1, 1.4f);

		arrow.setPierceLevel(0);
		arrow.setCritical(true);
		arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);
		arrow.setMetadata(DUMMY_DECOY_ARROW_METADATA, new FixedMetadataValue(mPlugin, 0));

		mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.SPELL_WITCH);

		new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {

				if (mT > MAX_TICKS || !arrow.isValid() || !arrow.hasMetadata(DUMMY_DECOY_ARROW_METADATA)) {
					arrow.remove();
					this.cancel();
					return;
				}

				if (arrow.getVelocity().length() < .05 || arrow.isOnGround()) {
					spawnDecoy(arrow, arrow.getLocation());
					this.cancel();
					return;
				}
				mT++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.PROJECTILE && event.getDamager() instanceof AbstractArrow arrow && arrow.isValid() && arrow.hasMetadata(DUMMY_DECOY_ARROW_METADATA)) {
			spawnDecoy(arrow, enemy.getLocation());
		}
		return false; // prevents multiple calls itself
	}

	private void spawnDecoy(Entity arrow, Location loc) {
		arrow.removeMetadata(DUMMY_DECOY_ARROW_METADATA, mPlugin);
		arrow.remove();

		LivingEntity e = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, DUMMY_NAME);
		EntityUtils.setAttributeBase(e, Attribute.GENERIC_MAX_HEALTH, HEALTH[mRarity - 1]);
		e.setHealth(HEALTH[mRarity - 1]);

		BossManager bossManager = BossManager.getInstance();
		if (bossManager != null) {
			List<BossAbilityGroup> abilities = bossManager.getAbilities(e);
			if (abilities != null) {
				for (BossAbilityGroup ability : abilities) {
					if (ability instanceof DummyDecoyBoss dummyDecoyBoss) {
						dummyDecoyBoss.spawn(STUN_TICKS[mRarity - 1]);
						break;
					}
				}
			}
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				if (e != null && e.isValid() && !e.isDead()) {
					DamageUtils.damage(null, e, DamageType.OTHER, 10000);
				}
			}
		}.runTaskLater(mPlugin, MAX_TICKS);
	}

	@Override
	public boolean playerShotArrowEvent(AbstractArrow arrow) {
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
			return true;
		}

		if (mPlayer.isSneaking()) {
			mInfo.mCooldown = (int) (COOLDOWN * BowAspect.getCooldownReduction(mPlayer));
			putOnCooldown();
			execute(arrow);
		}

		return true;
	}

	@Override
	public String getDescription(int rarity) {
		return "Shooting a bow while sneaking fires a cursed arrow. When the arrow lands, it spawns a dummy decoy at the location with " + DepthsUtils.getRarityColor(rarity) + HEALTH[rarity - 1] + ChatColor.WHITE + " health that lasts for up to " + MAX_TICKS / 20 + " seconds. The decoy aggros mobs within " + AGGRO_RADIUS + " blocks on a regular interval. On death, the decoy explodes, stunning mobs in a " + STUN_RADIUS + " block radius for " + DepthsUtils.getRarityColor(rarity) + DepthsUtils.roundDouble(STUN_TICKS[rarity - 1] / 20.0) + ChatColor.WHITE + " seconds. Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.SHADOWS;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SHIFT_BOW;
	}
}

