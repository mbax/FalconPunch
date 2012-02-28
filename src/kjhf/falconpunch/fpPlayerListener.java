package kjhf.falconpunch;
import java.util.Random;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.util.Vector;

public class fpPlayerListener implements Listener {
    
    private FalconPunch plugin;
    
    public fpPlayerListener(FalconPunch plugin){
        this.plugin=plugin;
    }
        
    @EventHandler
    public void onPlayerInteractEntity (PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (player.getItemInHand().getType() != Material.AIR) {
            return;
        }

        if (!player.hasPermission("plugin.punch")) {
            return;
        }

        Entity targetEntity = event.getRightClicked();
        
        if (targetEntity instanceof Player && !plugin.AllowPVP) {
            return;
        }

        if (!(targetEntity instanceof Player) && plugin.OnlyPVP) {
            return;
        }

        if (targetEntity instanceof Vehicle) {
            if (targetEntity.isEmpty()) {
                if (targetEntity instanceof Pig) {
                    if (((Pig) targetEntity).hasSaddle()) {
                        return; // The target is a pig with a saddle and no passenger. Puncher might want to ride the pig.
                    }
                } else {
                    return; // The boat/minecart/vehicle is empty, the puncher might want to get inside it?
                }
            }
        }

        if (targetEntity instanceof Wolf) {
            Wolf wolf = (Wolf) targetEntity;
            if (wolf.isTamed()) {
                if (wolf.getOwner() instanceof Player) {
                    Player owner = (Player) wolf.getOwner();
                    if (player == owner) {
                        return;
                    }
                }
            }
        }

        if (targetEntity instanceof Player) {
            Player targetplayer = (targetEntity instanceof Player) ? (Player) targetEntity : null;
            if (targetplayer!=null && targetplayer.hasPermission("plugin.immune") && !plugin.NoImmunity) {
                player.sendMessage(ChatColor.GOLD + "[FalconPunch] " + ChatColor.RED + "That person cannot be Falcon Punched. They have immune permission.");
                return;
            }
        }

        Random random = new Random();
        int i = random.nextInt(99) + 1;

        if (i <= plugin.FailChance) {
            // The punch failed. Let's decide what we're going to do.

            if ((plugin.FailNothingChance + plugin.FailFireChance + plugin.FailLightningChance) <= 0) {
                plugin.getLogger().warning("Logic error. Please check fail probability in config for negative chances. Defaulting to no side-effect.");
                player.sendMessage(ChatColor.DARK_AQUA + "FALCON... Fail?!");
                return;
            }

            random = new Random();
            i = random.nextInt(plugin.FailNothingChance + plugin.FailFireChance + plugin.FailLightningChance) + 1;
            if (0 < i && i <= plugin.FailNothingChance) {
                // Show the Fail nothing message.
                player.sendMessage(ChatColor.DARK_AQUA + "FALCON... Fail?!");
                return;
            } else if (plugin.FailNothingChance < i && i <= (plugin.FailNothingChance + plugin.FailFireChance)) {
                // Show the Fail fire message.
                player.setFireTicks(200);
                player.sendMessage(ChatColor.DARK_AQUA + "FALCON... Fail? [Burn Hit! Oh Noes!]");
                return;

            } else if ((plugin.FailNothingChance + plugin.FailFireChance) < i && i <= (plugin.FailNothingChance + plugin.FailFireChance + plugin.FailLightningChance)) {
                // Show the Fail lightning message.
                player.getWorld().strikeLightningEffect(player.getLocation());
                if (!player.getGameMode().equals(GameMode.CREATIVE)) {
                    player.setHealth(0);
                }
                player.sendMessage(ChatColor.DARK_AQUA + "FALCON... Fail? [YOU HAVE BEEN SMITTEN!]");
                return;
            } else {
                // Logic error, show the Fail nothing message.
                plugin.getLogger().warning("Logic error. Please check fail probability config. Defaulting to no side-effect.");
                plugin.getLogger().warning("Generated num: " + i + ". FailNothingChance: " + plugin.FailNothingChance + ". FailFireChance: " + plugin.FailFireChance + ". FailLightningChance: " + plugin.FailLightningChance);
                player.sendMessage(ChatColor.DARK_AQUA + "FALCON... Fail?!");
                return;
            }
        }

        double crit = 2.0;
        if (!plugin.UseContinuousSystem) {
            if (plugin.CriticalsChance > 0) {
                random = new Random();
                i = random.nextInt(99) + 1;
                if (plugin.CriticalsChance >= i) {
                    crit = 4;
                }
            }
        } else {
            random = new Random();
            i = (random.nextInt(59) + 1);
            crit = (double) i/10; // crit is between 0.1 and 6.0
        }

        boolean burncrit = false;

        if (plugin.BurnChance > 0) {
            random = new Random();
            i = random.nextInt(99) + 1;
            if (i <= plugin.BurnChance) {
                burncrit = true;
                targetEntity.setFireTicks(200);
            }
        }

        Vector direction = player.getLocation().getDirection();
        Vector additionalverticle = null;
        if (direction.getY() >= -0.5 && direction.getY() < 0.6) {
            additionalverticle = new Vector(0, 0.5, 0);
        } else {
            additionalverticle = new Vector(0, 0, 0);
        }
        Vector velocity = new Vector(0, 0, 0);
        if (player.getVelocity() != null) {
            velocity = player.getVelocity().add(direction).add(additionalverticle).multiply(5).multiply(crit);
        } else {
            velocity = velocity.add(direction).add(additionalverticle).multiply(5).multiply(crit);
        }
        targetEntity.setVelocity(velocity);

        String message = ChatColor.DARK_AQUA + "FALCON... PAUNCH! ";
        if (!plugin.UseContinuousSystem) {
            if (burncrit) {
                if (crit == 4) {
                    message += "[" + ChatColor.RED + "Burn " + ChatColor.DARK_AQUA + "+" + ChatColor.RED + " Critical Hit! " + ChatColor.DARK_AQUA + "]";
                } else {
                    message += "[" + ChatColor.RED + "Burn Hit!" + ChatColor.DARK_AQUA + "]";
                }
            } else {
                if (crit == 4) {
                    message += "[" + ChatColor.RED + "Critical Hit!" + ChatColor.DARK_AQUA + "]";
                }
                //else {
                //  message = ChatColor.DARK_AQUA + "FALCON... PAUNCH!";
                //}
            }
        } else {            
            message += "[";
            
            if (crit > 5.75) {
                message += ChatColor.RED + "||||||" + ChatColor.GOLD + "||||||" + ChatColor.YELLOW + "||||||" + ChatColor.GREEN + "||||||" + ChatColor.WHITE + "||";
            } else if (crit > 5.5) {
                message += ChatColor.RED + "||||||" + ChatColor.GOLD + "||||||" + ChatColor.YELLOW + "||||||" + ChatColor.GREEN + "||||||" + ChatColor.WHITE + "|" + ChatColor.BLACK + "|"; 
            } else if (crit > 5.25) {
                message += ChatColor.RED + "||||||" + ChatColor.GOLD + "||||||" + ChatColor.YELLOW + "||||||" + ChatColor.GREEN + "||||||" + ChatColor.BLACK + "||"; 
            } else if (crit > 5.0) {
                message += ChatColor.RED + "||||||" + ChatColor.GOLD + "||||||" + ChatColor.YELLOW + "||||||" + ChatColor.GREEN + "|||||" + ChatColor.BLACK + "|||"; 
            } else if (crit > 4.75) {
                message += ChatColor.RED + "||||||" + ChatColor.GOLD + "||||||" + ChatColor.YELLOW + "||||||" + ChatColor.GREEN + "||||" + ChatColor.BLACK + "||||"; 
            } else if (crit > 4.5) {
                message += ChatColor.RED + "||||||" + ChatColor.GOLD + "||||||" + ChatColor.YELLOW + "||||||" + ChatColor.GREEN + "|||" + ChatColor.BLACK + "|||||"; 
            } else if (crit > 4.25) {
                message += ChatColor.RED + "||||||" + ChatColor.GOLD + "||||||" + ChatColor.YELLOW + "||||||" + ChatColor.GREEN + "||" + ChatColor.BLACK + "||||||"; 
            } else if (crit > 4.0) {
                message += ChatColor.RED + "||||||" + ChatColor.GOLD + "||||||" + ChatColor.YELLOW + "||||||" + ChatColor.GREEN + "|" + ChatColor.BLACK + "|||||||"; 
            } else if (crit > 3.75) {
                message += ChatColor.RED + "||||||" + ChatColor.GOLD + "||||||" + ChatColor.YELLOW + "||||||" + ChatColor.BLACK + "||||||||"; 
            } else if (crit > 3.5) {
                message += ChatColor.RED + "||||||" + ChatColor.GOLD + "||||||" + ChatColor.YELLOW + "|||||" + ChatColor.BLACK + "|||||||||"; 
            } else if (crit > 3.25) {
                message += ChatColor.RED + "||||||" + ChatColor.GOLD + "||||||" + ChatColor.YELLOW + "||||" + ChatColor.BLACK + "||||||||||";
            } else if (crit > 3.0) {
                message += ChatColor.RED + "||||||" + ChatColor.GOLD + "||||||" + ChatColor.YELLOW + "|||" + ChatColor.BLACK + "|||||||||||";
            } else if (crit > 2.75) {
                message += ChatColor.RED + "||||||" + ChatColor.GOLD + "||||||" + ChatColor.YELLOW + "||" + ChatColor.BLACK + "||||||||||||";
            } else if (crit > 2.5) {
                message += ChatColor.RED + "||||||" + ChatColor.GOLD + "||||||" + ChatColor.YELLOW + "|" + ChatColor.BLACK + "|||||||||||||";
            } else if (crit > 2.25) {
                message += ChatColor.RED + "||||||" + ChatColor.GOLD + "||||||" + ChatColor.BLACK + "||||||||||||||";
            } else if (crit > 2.0) {
                message += ChatColor.RED + "||||||" + ChatColor.GOLD + "|||||" + ChatColor.BLACK + "|||||||||||||||";
            } else if (crit > 1.75) {
                message += ChatColor.RED + "||||||" + ChatColor.GOLD + "||||" + ChatColor.BLACK + "||||||||||||||||";
            } else if (crit > 1.5) {
                message += ChatColor.RED + "||||||" + ChatColor.GOLD + "|||" + ChatColor.BLACK + "|||||||||||||||||";
            } else if (crit > 1.25) {
                message += ChatColor.RED + "||||||" + ChatColor.GOLD + "||" + ChatColor.BLACK + "||||||||||||||||||";
            } else if (crit > 1.0) {
                message += ChatColor.RED + "||||||" + ChatColor.GOLD + "|" + ChatColor.BLACK + "|||||||||||||||||||";
            } else if (crit > 0.8) {
                message += ChatColor.RED + "||||||" + ChatColor.BLACK + "||||||||||||||||||||";
            } else if (crit > 0.6) {
                message += ChatColor.RED + "|||||" + ChatColor.BLACK + "|||||||||||||||||||||";
            } else if (crit > 0.4) {
                message += ChatColor.RED + "||||" + ChatColor.BLACK + "||||||||||||||||||||||";
            } else if (crit > 0.3) {
                message += ChatColor.RED + "|||" + ChatColor.BLACK + "|||||||||||||||||||||||";
            } else if (crit > 0.2) {
                message += ChatColor.RED + "||" + ChatColor.BLACK + "||||||||||||||||||||||||";
            } else {
                message += ChatColor.RED + "|" + ChatColor.BLACK + "|||||||||||||||||||||||||";
            }
            message += ChatColor.DARK_AQUA + "]";
            
            if (burncrit) {
                message += " [" + ChatColor.RED + "Burn!" + ChatColor.DARK_AQUA + "] ";
            }
        }
        player.sendMessage(message);
    }
}