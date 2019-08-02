# Attention
*A plugin for your ego.*

## Description
This plugin allows any player having the permission `attention.bump` to **bump any other player** on the server, through the use of the command `/bump <player1> [player2] ...`

In addition, every player has the opportunity to **get a ping when their name is mentioned** in the chat. This is off by default for every player, and they can enable it with `/chatping [on|off]`.

## Configuration
A config file is included. You can change pretty much everything in the plugin, from the permissions to use to the status and error messages, and even the commands !

If you mess up the configuration file, [you can always download the original one here](https://github.com/Webbeh/Attention/blob/master/src/main/resources/config.yml).

## Permissions
- `attention.bump` : Bump any other player with the `/bump` command.
- `attention.ping` : Get a ping whenever your name is mentioned in chat.
- `attention.nocooldown` : Disables the cooldown altogether

## Usage
- `/bump <player> [player2] ...` : Bump one or several players using their (case-insensitive) nickname. *Works from console.*
- `/chatping [on|off]` : Toggle, enable or disable the ping when your name is mentioned.

## Source code
The source code for this project is located in Github : https://github.com/Webbeh/Attention/

## Spigot page
The spigot page of this project is located here : https://www.spigotmc.org/resources/attention.69953/

## License
This is released under the [Beerware](https://raidstone.net/beerware.txt) license.