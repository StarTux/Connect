# Connect
Connect Bukkit servers via a simple, decentralized message sending protocol based TCP. It works with or without Bungee and can connect Spigot servers across versions, as well as other modded servers, provided they follow the protocol. This plugin should only be of interest for server admins and the technically minded.

## Features
Connect provides a few out of the box, namely player listing. At its core, it is a library to send and receive packets from remote servers.
- Player listing. All connected servers exchange player lists, allowing cross server player listing without Bungee support.
- Send packets to any server via the API.
- Broadcast packets to all servers via the API.

## Concepts
Each server has a name which is unique in the network, as well as a display name. They all read from the same configuration file which lists each server, its name and display name, and the port they use to connect. Said port has to be different from the Minecraft port, in fact, it has to be unused.

What is exchanged is an arbitrary payload with the caveat that it will be serialized to a JSON String. Any object which is not JSON-friendly will not transport properly. This includes UUIDs in particular. If in doubt, stick with List, Map, String, Boolean, and numerical types. At the other end, the JSON String will arrive in deserialized form.

## Configuration
The main plugin configuration file only has a few entries:
```yaml
ServerName: yourservername
ServerConfig: /path/to/servers/file
```
The aforementioned servers file could look like this:
```
SomePassword

# Name  Port  Display Name
yourservername     9991    Main Server
hubserver          9992    The Main Hub
creative           9993    Creative Server
```
The first line is just a password to provide the absolute minimum protection against invaders, although it is recommended to have other safeguards in place to protect your ports.
What follows is a list of all servers, with name, port, and display name, separated by tabs or spaces.


## Commands
There are a flurry of debug commands which shall not all be discussed here in detail. If in doubt, consult the source code.
- `/connect status` - See connection status of all servers in the network.
- `/connect reload` - Reload configuration files and reconnect all connections.
- `/connect ping` - Ping a server in the network.
- `/connect players` - List online players on each server.
