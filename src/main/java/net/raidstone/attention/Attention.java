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
    
    private String bumpCommand = "bump";
    private String pingCommand = "chatping";
    
    private String bumpPerm = "attention.bump";
    private String pingPerm = "attention.ping";
    private String coolPerm = "attention.nocooldown";
    
    private String prefix = "§7[§4§lAttention§7] ";
    private String serverName = "Server";
    private String message_color = "§f";
    private String bump_sent = prefix + message_color + "You bumped $bumpedplayers !";
    private String bump_received = "§c§l[$sender] §f§lHey !";
    private String noperm = prefix + "§cYou don't have the permission to use that command.";
    private String no_players_to_bump = prefix + message_color + "No player was found with that name.";
    private String no_players_in_cmd = prefix + message_color + "You must append the name of at least one player.";
    private String cooldown_msg = prefix + "§cDon't ping players too often !";
    private String wrong_usage = prefix + message_color + "You must either choose [on] or [off]. Alternatively, you can simply use the command with no argument to toggle between on and off.";
    private String receive_pings = prefix + message_color + "You are now receiving pings !";
    private String no_more_pings = prefix + message_color + "You are no longer receiving pings.";
    
    private final Set<UUID> cooledDown = new HashSet<>();
    private final Set<UUID> wantsPing = new HashSet<>();
    private String translateColors(String message)
    {
        if(message==null)
            return "";
        String m = message.replace("$prefix ", prefix).replace("$color ", message_color);
        return ChatColor.translateAlternateColorCodes('§', m);
    }
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
        
        prefix = getConfig().getString("messages.plugin-prefix");
        message_color = getConfig().getString("messages.message-color");
        
        bump_sent = translateColors(getConfig().getString("messages.bump-sent"));
        bump_received = translateColors(getConfig().getString("messages.bump-received"));
        noperm = translateColors(getConfig().getString("messages.noperm"));
        no_players_in_cmd = translateColors(getConfig().getString("messages.no-players-in-cmd"));
        no_players_to_bump = translateColors(getConfig().getString("messages.no-players-to-bump"));
        cooldown_msg = translateColors(getConfig().getString("messages.cooldown"));
        wrong_usage = translateColors(getConfig().getString("messages.wrong-usage"));
        receive_pings = translateColors(getConfig().getString("messages.receives-pings"));
        no_more_pings = translateColors(getConfig().getString("messages.no-more-pings"));
        serverName = translateColors(getConfig().getString("messages.name-of-console"));
        
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
            if(console) Bukkit.getLogger().info(no_players_to_bump);
            else orig.sendMessage(no_players_to_bump);
            return;
        }
        String msg = bump_sent;
        StringBuilder pl = new StringBuilder("");
        for (Player p : toPing) {
            pl.append(p.getDisplayName());
            pl.append(", ");
            p.sendMessage(bump_received.replace("$sender", console?serverName:orig.getDisplayName()));
        }
        pl=pl.replace(pl.length()-2, pl.length(), "");
        if(console)
            Bukkit.getLogger().info(msg.replace("$bumpedplayers", pl.toString()));
        else
            orig.sendMessage(msg.replace("$bumpedplayers", pl.toString()));
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
                    Bukkit.getLogger().info(no_players_in_cmd);
                    return;
                }
                //Console can bypass cooldowns and everything.
                bump(args, consoleUuid);
                return;
            }
            sender.sendMessage(prefix+"You can't use that command in console.");
            return;
        }
        
        //Now cast to player
        Player send = (Player) sender;
        UUID uuid = send.getUniqueId();
        if(label.equalsIgnoreCase(bumpCommand))
        {
            if(!send.hasPermission(bumpPerm))
            {
                send.sendMessage(noperm);
                return;
            }
            if(args.length==0) {
                send.sendMessage(no_players_in_cmd);
                return;
            }
            
            if(cooledDown.contains(uuid))
            {
                send.sendMessage(cooldown_msg);
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
                        send.sendMessage(noperm);
                        return;
                    }
                    enable(send);
                }
                else if(onoff.equalsIgnoreCase("off")) {
                    disable(send);
                }
                else
                    send.sendMessage(wrong_usage);
                return;
            }
            
            if(wantsPing.contains(uuid))
                disable(send);
            else {
                if (!send.hasPermission(pingPerm)) {
                    send.sendMessage(noperm);
                    return;
                }
                enable(send);
            }
        }
    }
    private void enable(Player p)
    {
        wantsPing.add(p.getUniqueId());
        p.sendMessage(receive_pings);
        List<String> confUuid = getConfig().getStringList("wantsping");
        if(!confUuid.contains(p.getUniqueId().toString()))
            confUuid.add(p.getUniqueId().toString());
        getConfig().set("wantsping", confUuid);
        saveConfig();
    }
    private void disable(Player p)
    {
        wantsPing.remove(p.getUniqueId());
        p.sendMessage(no_more_pings);
        List<String> confUuid = getConfig().getStringList("wantsping");
        confUuid.remove(p.getUniqueId().toString());
        getConfig().set("wantsping", confUuid);
        saveConfig();
    }
}
