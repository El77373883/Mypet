package com.soyadrianyt001.mypet.listeners;

import com.soyadrianyt001.mypet.Mypet;
import com.soyadrianyt001.mypet.data.PetData;
import com.soyadrianyt001.mypet.gui.PetGui;
import com.soyadrianyt001.mypet.managers.PetManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Boss;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PetListener implements Listener {

    private static final Set<Material> CROPS = EnumSet.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS, Material.NETHER_WART);
    private static final Set<String> NEVER_ATTACK = Set.of(
            "ENDERMAN", "ZOMBIFIED_PIGLIN", "PIGLIN", "PIGLIN_BRUTE", "WARDEN", "ENDER_DRAGON", "WITHER");

    private record Hit(UUID target, long time) {
    }

    private final Mypet plugin;
    private final Map<UUID, Hit> lastHit = new HashMap<>();
    private final Map<UUID, Hit> lastOwnerHit = new HashMap<>();
    private final Map<UUID, Long> lastAttack = new HashMap<>();
    private final Map<UUID, Long> lastWork = new HashMap<>();
    private final Map<UUID, Long> lastRespawn = new HashMap<>();
    private long tickCount;

    public PetListener(Mypet plugin) {
        this.plugin = plugin;
        // por si el plugin se recarga con jugadores conectados
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) pm().spawnPet(p);
        });
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 10L);
    }

    private PetManager pm() {
        return plugin.getPetManager();
    }

    // ============================================================ bucle

    private void tick() {
        PetManager pm = pm();
        if (pm == null) return;
        tickCount++;
        for (Player player : Bukkit.getOnlinePlayers()) {
            PetData d = pm.getPet(player);
            if (d == null || player.isDead()) continue;
            try {
                tickPet(pm, player, d);
            } catch (Exception ex) {
                plugin.getLogger().warning("Error en la mascota de " + player.getName() + ": " + ex);
            }
        }
        if (tickCount % 60 == 0) pm.saveIfDirty();
    }

    private void tickPet(PetManager pm, Player owner, PetData d) {
        long now = System.currentTimeMillis();
        Mob pet = pm.getPetEntity(d);
        if (pet == null) {
            long last = lastRespawn.getOrDefault(owner.getUniqueId(), 0L);
            if (now - last > 5000) {
                lastRespawn.put(owner.getUniqueId(), now);
                pm.spawnPet(owner);
            }
            return;
        }

        Location ol = owner.getLocation();
        Location pl = pet.getLocation();
        boolean sameWorld = pl.getWorld().equals(ol.getWorld());
        double dist = sameWorld ? pl.distance(ol) : Double.MAX_VALUE;
        double tpDist = plugin.getConfig().getInt("pets.teleport-distance", 25);

        if (!sameWorld || (d.isFollowEnabled() && dist > tpDist) || pl.getY() < pl.getWorld().getMinHeight()) {
            pet.teleport(ol);
            return;
        }

        boolean busy = false;
        if (d.getAttackMode() != PetData.AttackMode.OFF) {
            busy = attack(pm, owner, d, pet);
        }
        if (!busy) {
            busy = switch (d.getCurrentJob()) {
                case "collector" -> collect(pm, owner, d, pet);
                case "farmer" -> farm(pm, owner, d, pet);
                case "miner" -> mine(pm, owner, d, pet);
                default -> false;
            };
        }
        if (!busy && d.isFollowEnabled() && dist > 4.0) {
            pet.getPathfinder().moveTo(ol, 1.25);
        }
        if (tickCount % 120 == 0) pm.addXp(owner, d, plugin.getConfig().getInt("pets.passive-xp-per-minute", 2));
    }

    // ========================================================== combate

    private boolean attack(PetManager pm, Player owner, PetData d, Mob pet) {
        double r = plugin.getConfig().getDouble("pets.attack.radius", 8.0);
        LivingEntity target = null;

        if (d.getAttackMode() == PetData.AttackMode.DEFEND) {
            // ataca a quien golpeó al dueño, o a quien el dueño golpeó
            UUID id = owner.getUniqueId();
            Hit h = newest(lastHit.get(id), lastOwnerHit.get(id));
            boolean pvp = plugin.getConfig().getBoolean("pets.attack.pvp", false);
            if (h != null && System.currentTimeMillis() - h.time() < 10_000) {
                Entity e = Bukkit.getEntity(h.target());
                if (e instanceof LivingEntity le && le.isValid() && !le.isDead()
                        && !le.getUniqueId().equals(id) && !pm.isPet(le)
                        && (pvp || !(le instanceof Player))
                        && le.getWorld().equals(pet.getWorld())
                        && le.getLocation().distanceSquared(owner.getLocation()) <= r * r * 4) {
                    target = le;
                }
            }
        } else {
            double best = Double.MAX_VALUE;
            for (Entity e : owner.getNearbyEntities(r, r / 2, r)) {
                if (!(e instanceof Enemy) || !(e instanceof LivingEntity le)) continue;
                if (!le.isValid() || le.isDead() || le instanceof Boss) continue;
                if (NEVER_ATTACK.contains(le.getType().name())) continue;
                double dd = le.getLocation().distanceSquared(pet.getLocation());
                if (dd < best) {
                    best = dd;
                    target = le;
                }
            }
        }
        if (target == null) return false;

        double dist = target.getLocation().distance(pet.getLocation());
        if (dist > 2.0) pet.getPathfinder().moveTo(target.getLocation(), 1.5);

        long now = System.currentTimeMillis();
        long last = lastAttack.getOrDefault(owner.getUniqueId(), 0L);
        if (dist <= 2.6 && now - last >= 900) {
            lastAttack.put(owner.getUniqueId(), now);
            double base = plugin.getConfig().getDouble("pets.attack.base-damage", 2.0);
            double per = plugin.getConfig().getDouble("pets.attack.damage-per-level", 0.4);
            double max = plugin.getConfig().getDouble("pets.attack.max-damage", 14.0);
            double dmg = Math.min(max, base + d.getLevel() * per);
            pet.swingMainHand();
            target.damage(dmg, pet);
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3);
            pm.addXp(owner, d, target.isDead() ? 8 : 1);
        }
        return true;
    }

    private static Hit newest(Hit a, Hit b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.time() >= b.time() ? a : b;
    }

    /** Registra combates del dueño y evita que la mascota lo dañe. */
    @EventHandler(ignoreCancelled = true)
    public void onCombat(EntityDamageByEntityEvent e) {
        Entity victim = e.getEntity();
        Entity damager = e.getDamager();
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Entity shooter) {
            damager = shooter;
        }
        long now = System.currentTimeMillis();

        // la mascota nunca daña a su dueño
        if (pm().isPet(damager) && victim.getUniqueId().equals(pm().getOwnerId(damager))) {
            e.setCancelled(true);
            return;
        }
        // alguien golpeó al dueño
        if (victim instanceof Player owner) {
            if (damager instanceof LivingEntity && !(damager instanceof Player) && !pm().isPet(damager)) {
                lastHit.put(owner.getUniqueId(), new Hit(damager.getUniqueId(), now));
            }
        }
        // el dueño golpeó a alguien
        if (damager instanceof Player attacker && victim instanceof LivingEntity && !pm().isPet(victim)) {
            lastOwnerHit.put(attacker.getUniqueId(), new Hit(victim.getUniqueId(), now));
        }
    }

    // ========================================================= trabajos

    private boolean cooldown(UUID owner, long ms) {
        long now = System.currentTimeMillis();
        if (now - lastWork.getOrDefault(owner, 0L) < ms) return true;
        lastWork.put(owner, now);
        return false;
    }

    private void give(Player owner, Collection<ItemStack> drops, Location at) {
        for (ItemStack drop : drops) {
            for (ItemStack rest : owner.getInventory().addItem(drop).values()) {
                at.getWorld().dropItemNaturally(at, rest);
            }
        }
    }

    private boolean collect(PetManager pm, Player owner, PetData d, Mob pet) {
        if (owner.getInventory().firstEmpty() == -1) return false;
        double r = plugin.getConfig().getDouble("pets.collect-radius", 6.0);
        Item best = null;
        double bd = Double.MAX_VALUE;
        for (Entity e : pet.getNearbyEntities(r, 3, r)) {
            if (!(e instanceof Item item) || !item.isValid() || item.getPickupDelay() > 0) continue;
            if (item.getThrower() != null) continue; // no recoge lo que soltó un jugador
            if (item.getLocation().distanceSquared(owner.getLocation()) > 400) continue;
            double dd = item.getLocation().distanceSquared(pet.getLocation());
            if (dd < bd) {
                bd = dd;
                best = item;
            }
        }
        if (best == null) return false;
        if (Math.sqrt(bd) > 1.6) {
            pet.getPathfinder().moveTo(best.getLocation(), 1.4);
            return true;
        }
        ItemStack stack = best.getItemStack();
        int before = stack.getAmount();
        Map<Integer, ItemStack> left = owner.getInventory().addItem(stack.clone());
        if (left.isEmpty()) {
            best.remove();
        } else {
            ItemStack rest = left.values().iterator().next();
            if (rest.getAmount() == before) return false;
            best.setItemStack(rest);
        }
        owner.playSound(owner.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);
        pm.addXp(owner, d, 1);
        return true;
    }

    private boolean farm(PetManager pm, Player owner, PetData d, Mob pet) {
        if (cooldown(owner.getUniqueId(), 2000)) return false;
        Location c = pet.getLocation();
        World w = c.getWorld();
        int r = plugin.getConfig().getInt("pets.farm-radius", 4);
        int done = 0;
        for (int x = -r; x <= r && done < 4; x++) {
            for (int z = -r; z <= r && done < 4; z++) {
                for (int y = -1; y <= 1 && done < 4; y++) {
                    Block b = w.getBlockAt(c.getBlockX() + x, c.getBlockY() + y, c.getBlockZ() + z);
                    if (!CROPS.contains(b.getType())) continue;
                    if (!(b.getBlockData() instanceof Ageable crop)) continue;
                    if (crop.getAge() < crop.getMaximumAge()) continue;

                    BlockBreakEvent ev = new BlockBreakEvent(b, owner); // respeta protecciones
                    Bukkit.getPluginManager().callEvent(ev);
                    if (ev.isCancelled()) continue;

                    Collection<ItemStack> drops = b.getDrops();
                    crop.setAge(0);
                    b.setBlockData(crop);
                    give(owner, drops, b.getLocation());
                    w.spawnParticle(Particle.HAPPY_VILLAGER, b.getLocation().add(0.5, 0.5, 0.5), 5, 0.3, 0.3, 0.3);
                    done++;
                }
            }
        }
        if (done > 0) pm.addXp(owner, d, 2 * done);
        return false;
    }

    private static boolean isOre(Material m) {
        return m.name().endsWith("_ORE") || m == Material.ANCIENT_DEBRIS;
    }

    private boolean mine(PetManager pm, Player owner, PetData d, Mob pet) {
        if (cooldown(owner.getUniqueId(), 1500)) return false;
        Location c = pet.getLocation();
        World w = c.getWorld();
        int r = 3;
        Block found = null;
        double best = Double.MAX_VALUE;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    Block b = w.getBlockAt(c.getBlockX() + x, c.getBlockY() + y, c.getBlockZ() + z);
                    if (!isOre(b.getType())) continue;
                    double dd = b.getLocation().add(0.5, 0.5, 0.5).distanceSquared(c);
                    if (dd < best) {
                        best = dd;
                        found = b;
                    }
                }
            }
        }
        if (found == null) return false;

        BlockBreakEvent ev = new BlockBreakEvent(found, owner); // respeta protecciones
        Bukkit.getPluginManager().callEvent(ev);
        if (ev.isCancelled()) return false;

        Collection<ItemStack> drops = found.getDrops(new ItemStack(Material.DIAMOND_PICKAXE));
        Location at = found.getLocation();
        found.setType(Material.AIR);
        give(owner, drops, at);
        w.playSound(at, Sound.BLOCK_STONE_BREAK, 0.8f, 1.0f);
        w.spawnParticle(Particle.CRIT, at.add(0.5, 0.5, 0.5), 8, 0.3, 0.3, 0.3);
        pm.addXp(owner, d, 3);
        return false;
    }

    // ===================================================== conexión

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) pm().spawnPet(p);
        }, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        pm().despawnPet(p);
        pm().saveIfDirty();
        UUID id = p.getUniqueId();
        lastHit.remove(id);
        lastOwnerHit.remove(id);
        lastAttack.remove(id);
        lastWork.remove(id);
        lastRespawn.remove(id);
    }

    // =================================================== anti-bugs

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (pm().isPet(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        if (pm().isPet(e.getEntity())) {
            e.getDrops().clear();
            e.setDroppedExp(0);
        }
    }

    @EventHandler
    public void onTarget(EntityTargetEvent e) {
        if (pm().isPet(e.getEntity()) || pm().isPet(e.getTarget())) e.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        Entity clicked = e.getRightClicked();
        if (!pm().isPet(clicked)) return;
        e.setCancelled(true); // sin ordeñar, esquilar, montar, alimentar...
        if (e instanceof PlayerInteractAtEntityEvent) return;
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        if (p.getUniqueId().equals(pm().getOwnerId(clicked))) {
            PetGui.openMainMenu(p);
        }
    }

    @EventHandler
    public void onShear(PlayerShearEntityEvent e) {
        if (pm().isPet(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler
    public void onLeash(PlayerLeashEntityEvent e) {
        if (pm().isPet(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler
    public void onDrop(EntityDropItemEvent e) {
        if (pm().isPet(e.getEntity())) e.setCancelled(true); // huevos de gallina...
    }

    @EventHandler
    public void onChangeBlock(EntityChangeBlockEvent e) {
        if (pm().isPet(e.getEntity())) e.setCancelled(true); // ovejas comiendo pasto...
    }

    @EventHandler
    public void onTransform(EntityTransformEvent e) {
        if (pm().isPet(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler
    public void onPortal(EntityPortalEvent e) {
        if (pm().isPet(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (pm().isPet(e.getEntity())) e.setCancelled(true);
    }

    /** Limpia mascotas huérfanas que hayan quedado guardadas en el mundo. */
    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent e) {
        for (Entity en : e.getEntities()) {
            if (pm().isPet(en) && !pm().isTracked(en)) en.remove();
        }
    }
}
