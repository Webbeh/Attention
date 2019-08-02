package net.raidstone.attention;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author weby@we-bb.com [Nicolas Glassey]
 * @version 1.0.0
 * @since 02/08/2019
 */
public class Attention extends JavaPlugin implements Listener {
    //Cooldown time
    private final UUID consoleUuid = UUID.randomUUID();
    private int cooldown = 5;
    private final Set<UUID> cooledDown = new HashSet<>();
    private final Set<UUID> wantsPing = new HashSet<>();
    @Override
    public void onEnable()
    {
        getConfig().options().copyDefaults(true);
        saveConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        cooldown = getConfig().getInt("cooldown");
        List<String> uuidStrings = getConfig().getStringList("wantsping");
        for(String uuidString : uuidStrings)
        {
            UUID uuid = UUID.fromString(uuidString);
            wantsPing.add(uuid);
        }
    }
    
    @Override
    public void onDisable()
    {
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChatMessage(AsyncPlayerChatEvent event)
    {
        String m = event.getMessage();
        for(String word : m.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+"))
        {
            Player p = Bukkit.getPlayerExact(word);
            if(p==null) continue;
            if(!wantsPing.contains(p.getUniqueId())) continue;
            pingPlayer(p);
        }
    }
    private void pingPlayer(Player p)
    {
        new BukkitRunnable() {
            @Override
            public void run() {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1F, 2F);
            }
        }.runTaskAsynchronously(this);
        new BukkitRunnable() {
            @Override
            public void run() {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1F, 1.6F);
            }
        }.runTaskLaterAsynchronously(this, 6);
    }
    private void bumpPlayers(final Set<Player> toPing, final UUID origin)
    {
        boolean console = origin==consoleUuid;
        Player orig = Bukkit.getPlayer(origin);
        if(orig==null && !console) return;
        StringBuilder msg = new StringBuilder("You bumped ");
        for (Player p : toPing) {
            msg.append(p.getDisplayName());
            msg.append(" ");
            p.sendMessage(ChatColor.BOLD+"["+(console?"Server":orig.getName())+"] "+ChatColor.DARK_RED+""+ChatColor.BOLD+"Hey !");
        }
        if(console)
            Bukkit.getLogger().info("[Attention] "+msg.toString());
        else
            orig.sendMessage(msg.toString());
        new BukkitRunnable() {
            @Override
            public void run()
            {
                toPing.forEach(p -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1F, 1F));
            }
        }.runTaskAsynchronously(this);
    
        new BukkitRunnable() {
            @Override
            public void run()
            {
                toPing.forEach(p -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1F, 1.5F));
            }
        }.runTaskLaterAsynchronously(this, 6);
    
        new BukkitRunnable() {
            @Override
            public void run()
            {
                toPing.forEach(p -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1F, 2F));
            }
        }.runTaskLaterAsynchronously(this, 12);
        
        
        new BukkitRunnable() {
            @Override
            public void run()
            {
                cooledDown.remove(origin);
            }
        }.runTaskLaterAsynchronously(this, cooldown * 20);
    
    }
    
    private void bump(String[] args, UUID uuid)
    {
        final Set<Player> toPing = new HashSet<>();
    
        for (String s : args) {
            Player p = Bukkit.getPlayer(s);
            if (p == null) continue;
            toPing.add(p);
        }
    
        bumpPlayers(toPing, uuid);
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        //Console first
        if(sender instanceof ConsoleCommandSender)
        {
            if(label.equalsIgnoreCase("bump"))
            {
                //Console can bypass cooldowns and everything.
                bump(args, consoleUuid);
                return true;
            }
            sender.sendMessage("Can't use that command in console.");
            return true;
        }
        
        //Now cast to player
        Player send = (Player) sender;
        UUID uuid = send.getUniqueId();
        if(label.equalsIgnoreCase("bump"))
        {
            if(args.length==0)
                return false;
            
            if(cooledDown.contains(uuid))
            {
                send.sendMessage("[Attention] Don't ping players too often !");
                return true;
            }
            
            if(!send.hasPermission("attention.nocooldown"))
                cooledDown.add(uuid);
            
            bump(args, uuid);
        }
        
        if(label.equalsIgnoreCase("chatping"))
        {
            if(args.length>=1)
            {
                String onoff = args[0];
                if(onoff.equalsIgnoreCase("on")) {
                    enable(send);
                }
                else if(onoff.equalsIgnoreCase("off")) {
                    disable(send);
                }
                else
                    return false;
                return true;
            }
            
            if(wantsPing.contains(uuid))
                disable(send);
            else
                enable(send);
        }
        return true;
    }
    private void enable(Player p)
    {
        wantsPing.add(p.getUniqueId());
        p.sendMessage("[Attention] You are now receiving pings !");
        List<String> confUuid = getConfig().getStringList("wantsping");
        if(!confUuid.contains(p.getUniqueId().toString()))
            confUuid.add(p.getUniqueId().toString());
        getConfig().set("wantsping", confUuid);
        saveConfig();
    }
    private void disable(Player p)
    {
        wantsPing.remove(p.getUniqueId());
        p.sendMessage("[Attention] You are no longer receiving pings.");
        List<String> confUuid = getConfig().getStringList("wantsping");
        confUuid.remove(p.getUniqueId().toString());
        getConfig().set("wantsping", confUuid);
        saveConfig();
    }
}
