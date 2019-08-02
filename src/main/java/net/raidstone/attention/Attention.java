package net.raidstone.attention;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author weby@we-bb.com [Nicolas Glassey]
 * @version 1.0.0
 * @since 02/08/2019
 */
public class Attention extends JavaPlugin implements Listener {
    //Cooldown time
    private final UUID consoleUuid = UUID.randomUUID();
    private int cooldown = 5;
    
    private String bumpCommand = "bump";
    private String pingCommand = "chatping";
    
    private String bumpPerm = "attention.bump";
    private String pingPerm = "attention.ping";
    private String coolPerm = "attention.nocooldown";
    
    private final Set<UUID> cooledDown = new HashSet<>();
    private final Set<UUID> wantsPing = new HashSet<>();
    @Override
    public void onEnable()
    {
        getConfig().options().copyDefaults(true);
        saveConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        cooldown = getConfig().getInt("cooldown");
        bumpCommand = getConfig().getString("commands.bump");
        pingCommand = getConfig().getString("commands.ping");
        bumpPerm = getConfig().getString("permissions.bump");
        pingPerm = getConfig().getString("permissions.ping");
        coolPerm = getConfig().getString("permissions.cooldown");
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
        for(String word : getWords(m))
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
        
        if(toPing.size()==0)
        {
            if(console) Bukkit.getLogger().info("[Attention] No one to bump with that name.");
            else orig.sendMessage("[Attention] No one to bump with that name.");
            return;
        }
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
    
    private String[] getWords(String string)
    {
        return string.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");
    }
    @EventHandler
    public void sc(ServerCommandEvent event)
    {
        String command = event.getCommand();
        String[] words = getWords(command);
        String commandName = words[0];
        
        if(!commandName.equalsIgnoreCase(bumpCommand) && !commandName.equalsIgnoreCase(pingCommand))
            return;
        
        //We cancel the event to not have the "command doesn't exist" message.
        event.setCancelled(true);
    
        String[] args = new String[words.length-1];
        //Copy all the arguments in one array, save for the command name itself
        System.arraycopy(words, 1, args, 0, words.length - 1);
        executeCommand(event.getSender(), commandName, args);
    }
    @EventHandler
    public void oc(PlayerCommandPreprocessEvent event)
    {
        String command = event.getMessage().substring(1);
        String[] words = getWords(command);
        String commandName = words[0];
        
        if(!commandName.equalsIgnoreCase(bumpCommand) && !commandName.equalsIgnoreCase(pingCommand))
            return;
        
        //We cancel the event to not have the "command doesn't exist" message.
        event.setCancelled(true);
        
        //Copy all the arguments in one array, save for the command name itself
        String[] args = new String[words.length-1];
        System.arraycopy(words, 1, args, 0, words.length - 1);
        Player p = event.getPlayer();
        executeCommand(p, commandName, args);
    }
    
    
    private void executeCommand(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        //Console first
        if(sender instanceof ConsoleCommandSender)
        {
            if(label.equalsIgnoreCase(bumpCommand))
            {
                if(args.length==0) {
                    Bukkit.getLogger().info("[Attention] You must append the name of at least one player.");
                    return;
                }
                //Console can bypass cooldowns and everything.
                bump(args, consoleUuid);
                return;
            }
            sender.sendMessage("[Attention] Can't use that command in console.");
            return;
        }
        
        //Now cast to player
        Player send = (Player) sender;
        UUID uuid = send.getUniqueId();
        if(label.equalsIgnoreCase(bumpCommand))
        {
            if(!send.hasPermission(bumpPerm))
            {
                send.sendMessage("[Attention] You don't have the permission to turn this on.");
                return;
            }
            if(args.length==0) {
                send.sendMessage("[Attention] You must append the name of at least one player.");
                return;
            }
            
            if(cooledDown.contains(uuid))
            {
                send.sendMessage("[Attention] Don't ping players too often !");
                return;
            }
            
            if(!send.hasPermission(coolPerm))
                cooledDown.add(uuid);
            
            bump(args, uuid);
        }
        
        if(label.equalsIgnoreCase(pingCommand))
        {
            if(args.length>=1)
            {
                String onoff = args[0];
                if(onoff.equalsIgnoreCase("on")) {
                    if (!send.hasPermission(pingPerm)) {
                        send.sendMessage("[Attention] You don't have permission to turn this on.");
                        return;
                    }
                    enable(send);
                }
                else if(onoff.equalsIgnoreCase("off")) {
                    disable(send);
                }
                else
                    send.sendMessage("[Attention] You must choose between [on] and [off]. You can also only use the command to toggle between those modes.");
                return;
            }
            
            if(wantsPing.contains(uuid))
                disable(send);
            else {
                if (!send.hasPermission(pingPerm)) {
                    send.sendMessage("[Attention] You don't have permission to turn this on.");
                    return;
                }
                enable(send);
            }
        }
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
