package com.soyadrianyt001.mypet.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI moderno de Mypet. Cada botón tiene una "acción" en texto:
 *   open:<PANTALLA>   abre otra pantalla
 *   cmd:<comando>     ejecuta un comando como el jugador (sin "/")
 *   prompt:rename     pide un nombre por chat y ejecuta /mypet rename
 *   prompt:create:<mob>  pide un nombre por chat y ejecuta /mypet create
 *   close             cierra el menú
 */
public final class PetGui {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private PetGui() {
    }

    public enum Screen { MAIN, CREATE, MODE, JOBS, CONFIRM }

    /** Comandos que usan los botones. Cámbialos si en tu plugin se llaman distinto. */
    public static final class Cmd {
        public static final String REMOVE = "mypet remove";
        public static final String CREATOR = "mypet creator";
        public static final String HELP = "mypet help";
        public static final String CALL = "mypet call";
        public static final String FOLLOW = "mypet follow";
        public static final String ATTACK_ON = "mypet attack on";
        public static final String ATTACK_OFF = "mypet attack off";
        public static final String ATTACK_DEFEND = "mypet attack defend";
        public static final String JOB = "mypet job "; // + id del trabajo

        private Cmd() {
        }
    }

    public static final class Holder implements InventoryHolder {
        public final Map<Integer, String> actions = new HashMap<>();
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private record Mob(String id, String label, Material icon) {
    }

    /** Lista de mascotas del selector (máximo 28). Edítala a tu gusto. */
    private static final List<Mob> MOBS = List.of(
            new Mob("cow", "Vaca", Material.COW_SPAWN_EGG),
            new Mob("pig", "Cerdo", Material.PIG_SPAWN_EGG),
            new Mob("sheep", "Oveja", Material.SHEEP_SPAWN_EGG),
            new Mob("chicken", "Gallina", Material.CHICKEN_SPAWN_EGG),
            new Mob("wolf", "Lobo", Material.WOLF_SPAWN_EGG),
            new Mob("cat", "Gato", Material.CAT_SPAWN_EGG),
            new Mob("rabbit", "Conejo", Material.RABBIT_SPAWN_EGG),
            new Mob("fox", "Zorro", Material.FOX_SPAWN_EGG),
            new Mob("panda", "Panda", Material.PANDA_SPAWN_EGG),
            new Mob("parrot", "Loro", Material.PARROT_SPAWN_EGG),
            new Mob("horse", "Caballo", Material.HORSE_SPAWN_EGG),
            new Mob("llama", "Llama", Material.LLAMA_SPAWN_EGG),
            new Mob("axolotl", "Ajolote", Material.AXOLOTL_SPAWN_EGG),
            new Mob("goat", "Cabra", Material.GOAT_SPAWN_EGG),
            new Mob("mooshroom", "Vaca hongo", Material.MOOSHROOM_SPAWN_EGG),
            new Mob("bee", "Abeja", Material.BEE_SPAWN_EGG),
            new Mob("camel", "Camello", Material.CAMEL_SPAWN_EGG),
            new Mob("zombie", "Zombie", Material.ZOMBIE_SPAWN_EGG),
            new Mob("skeleton", "Esqueleto", Material.SKELETON_SPAWN_EGG),
            new Mob("creeper", "Creeper", Material.CREEPER_SPAWN_EGG),
            new Mob("spider", "Araña", Material.SPIDER_SPAWN_EGG),
            new Mob("enderman", "Enderman", Material.ENDERMAN_SPAWN_EGG),
            new Mob("blaze", "Blaze", Material.BLAZE_SPAWN_EGG),
            new Mob("slime", "Slime", Material.SLIME_SPAWN_EGG),
            new Mob("witch", "Bruja", Material.WITCH_SPAWN_EGG),
            new Mob("iron_golem", "Golem de hierro", Material.IRON_BLOCK),
            new Mob("snow_golem", "Golem de nieve", Material.CARVED_PUMPKIN),
            new Mob("ender_dragon", "Dragón", Material.DRAGON_HEAD));

    private static final int[] MOB_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43};

    // ------------------------------------------------------------------ API

    /** Compatible con tu código anterior. */
    public static void openMainMenu(Player player) {
        open(player, Screen.MAIN);
    }

    public static void open(Player p, Screen screen) {
        switch (screen) {
            case MAIN -> main(p);
            case CREATE -> create(p);
            case MODE -> mode(p);
            case JOBS -> jobs(p);
            case CONFIRM -> confirm(p);
        }
    }

    // -------------------------------------------------------------- screens

    private static void main(Player p) {
        Holder h = make(6, "<gradient:#00c6ff:#9d50bb><bold>✦ Mypet · Panel de Control ✦</bold></gradient>");

        put(h, 4, item(Material.NETHER_STAR, "<gradient:#00c6ff:#9d50bb><bold>Mypet</bold></gradient>", true,
                "<gray>Plugin de mascotas profesional",
                "<gray>Autor: <white>soyadrianyt001",
                "",
                "<yellow>▶ Click para ver info"), "cmd:" + Cmd.CREATOR);

        put(h, 20, item(Material.EMERALD, "<green><bold>✦ Crear Mascota", false,
                "<gray>Elige un tipo y ponle nombre.",
                "",
                "<yellow>▶ Click para elegir"), "open:CREATE");
        put(h, 22, item(Material.NAME_TAG, "<aqua><bold>✦ Renombrar", false,
                "<gray>Cambia el nombre de tu mascota",
                "<gray>escribiéndolo en el chat.",
                "",
                "<yellow>▶ Click para renombrar"), "prompt:rename");
        put(h, 24, item(Material.TNT, "<red><bold>✦ Eliminar Mascota", false,
                "<gray>Borra tu mascota para siempre.",
                "",
                "<yellow>▶ Click para eliminar"), "open:CONFIRM");

        put(h, 29, item(Material.DIAMOND_SWORD, "<red><bold>✦ Modo de Combate", false,
                "<gray>Decide si tu mascota ataca,",
                "<gray>defiende o se mantiene pasiva.",
                "",
                "<yellow>▶ Click para configurar"), "open:MODE");
        put(h, 31, item(Material.IRON_PICKAXE, "<gold><bold>✦ Trabajos", false,
                "<gray>Asigna un trabajo: recolectar,",
                "<gray>cultivar o minar.",
                "",
                "<yellow>▶ Click para elegir"), "open:JOBS");
        put(h, 33, item(Material.LEAD, "<light_purple><bold>✦ Seguir / Quedarse", false,
                "<gray>Activa o desactiva que",
                "<gray>tu mascota te siga.",
                "",
                "<yellow>▶ Click para cambiar"), "cmd:" + Cmd.FOLLOW);

        put(h, 38, item(Material.ENDER_PEARL, "<dark_aqua><bold>✦ Llamar Mascota", false,
                "<gray>La teletransporta a tu lado.",
                "",
                "<yellow>▶ Click para llamarla"), "cmd:" + Cmd.CALL);
        put(h, 40, item(Material.BOOK, "<white><bold>✦ Ayuda", false,
                "<gray>Lista de comandos disponibles.",
                "",
                "<yellow>▶ Click para ver"), "cmd:" + Cmd.HELP);
        put(h, 42, item(Material.BONE, "<yellow><bold>✦ Consejo", false,
                "<gray>Haz <white>clic derecho<gray> sobre tu",
                "<gray>mascota para abrir este menú."), null);

        put(h, 49, item(Material.BARRIER, "<red><bold>✦ Cerrar", false,
                "<gray>Cierra este menú."), "close");
        show(p, h, Sound.BLOCK_ENDER_CHEST_OPEN);
    }

    private static void create(Player p) {
        Holder h = make(6, "<gradient:#11998e:#38ef7d><bold>✦ Elige tu Mascota ✦</bold></gradient>");
        for (int i = 0; i < MOBS.size() && i < MOB_SLOTS.length; i++) {
            Mob m = MOBS.get(i);
            put(h, MOB_SLOTS[i], item(m.icon(), "<green><bold>" + m.label(), false,
                    "<gray>Tipo: <white>" + m.label(),
                    "",
                    "<yellow>▶ Click para elegirla",
                    "<dark_gray>Luego escribe su nombre en el chat."), "prompt:create:" + m.id());
        }
        put(h, 49, item(Material.ARROW, "<yellow><bold>← Volver", false), "open:MAIN");
        show(p, h, Sound.UI_BUTTON_CLICK);
    }

    private static void mode(Player p) {
        Holder h = make(5, "<gradient:#ff416c:#ff4b2b><bold>✦ Modo de Combate ✦</bold></gradient>");
        put(h, 20, item(Material.DIAMOND_SWORD, "<red><bold>Atacar", true,
                "<gray>Ataca a todos los monstruos",
                "<gray>cercanos.",
                "",
                "<yellow>▶ Click para activar"), "cmd:" + Cmd.ATTACK_ON);
        put(h, 22, item(Material.SHIELD, "<gold><bold>Defender", false,
                "<gray>Solo ataca a quien",
                "<gray>te haga daño.",
                "",
                "<yellow>▶ Click para activar"), "cmd:" + Cmd.ATTACK_DEFEND);
        put(h, 24, item(Material.FEATHER, "<aqua><bold>Pasivo", false,
                "<gray>No ataca a nadie.",
                "",
                "<yellow>▶ Click para activar"), "cmd:" + Cmd.ATTACK_OFF);
        put(h, 40, item(Material.ARROW, "<yellow><bold>← Volver", false), "open:MAIN");
        show(p, h, Sound.UI_BUTTON_CLICK);
    }

    private static void jobs(Player p) {
        Holder h = make(5, "<gradient:#f7971e:#ffd200><bold>✦ Trabajos ✦</bold></gradient>");
        job(h, 20, Material.HOPPER, "Recolector", "collector", "Recoge los ítems del suelo.");
        job(h, 22, Material.GOLDEN_HOE, "Granjero", "farmer", "Cosecha y replanta cultivos.");
        job(h, 24, Material.IRON_PICKAXE, "Minero", "miner", "Busca minerales cercanos.");
        put(h, 31, item(Material.RED_BED, "<gray><bold>Descansar", false,
                "<gray>Sin trabajo, solo te acompaña.",
                "",
                "<yellow>▶ Click para elegir"), "cmd:" + Cmd.JOB + "none");
        put(h, 40, item(Material.ARROW, "<yellow><bold>← Volver", false), "open:MAIN");
        show(p, h, Sound.UI_BUTTON_CLICK);
    }

    private static void job(Holder h, int slot, Material m, String name, String id, String desc) {
        put(h, slot, item(m, "<gold><bold>" + name, false,
                "<gray>" + desc,
                "",
                "<yellow>▶ Click para asignar"), "cmd:" + Cmd.JOB + id);
    }

    private static void confirm(Player p) {
        Holder h = make(3, "<dark_red><bold>✦ ¿Eliminar mascota? ✦</bold>");
        put(h, 11, item(Material.LIME_CONCRETE, "<green><bold>✔ Sí, eliminar", false,
                "<gray>Esta acción no se puede",
                "<gray>deshacer."), "cmd:" + Cmd.REMOVE);
        put(h, 13, item(Material.TNT, "<red><bold>¡Cuidado!", true,
                "<gray>Perderás tu mascota, su",
                "<gray>nivel y su progreso."), null);
        put(h, 15, item(Material.RED_CONCRETE, "<red><bold>✘ Cancelar", false), "open:MAIN");
        show(p, h, Sound.ENTITY_VILLAGER_NO);
    }

    // -------------------------------------------------------------- helpers

    private static Holder make(int rows, String title) {
        Holder h = new Holder();
        h.inventory = Bukkit.createInventory(h, rows * 9, MM.deserialize(title));
        frame(h.inventory);
        return h;
    }

    private static void put(Holder h, int slot, ItemStack item, String action) {
        h.inventory.setItem(slot, item);
        if (action != null) h.actions.put(slot, action);
    }

    private static void show(Player p, Holder h, Sound sound) {
        p.openInventory(h.inventory);
        p.playSound(p.getLocation(), sound, 0.6f, 1.0f);
    }

    /** Borde en ajedrez cian/morado y fondo negro. */
    private static void frame(Inventory inv) {
        int size = inv.getSize();
        ItemStack dark = pane(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack cyan = pane(Material.CYAN_STAINED_GLASS_PANE);
        ItemStack purple = pane(Material.PURPLE_STAINED_GLASS_PANE);
        for (int i = 0; i < size; i++) {
            boolean edge = i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8;
            if (edge) inv.setItem(i, (i + i / 9) % 2 == 0 ? cyan : purple);
            else inv.setItem(i, dark);
        }
    }

    private static ItemStack pane(Material m) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(" "));
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack item(Material m, String name, boolean glow, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(MM.deserialize("<!italic>" + name));
        if (lore.length > 0) {
            List<Component> l = new ArrayList<>();
            for (String s : lore) l.add(MM.deserialize("<!italic>" + (s.isEmpty() ? " " : s)));
            meta.lore(l);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        if (glow) meta.setEnchantmentGlintOverride(true);
        it.setItemMeta(meta);
        return it;
    }
}
