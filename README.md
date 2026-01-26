# Twitching Armor Stand

## How To Use

1. Load the game with the mod and its dependencies installed.
2. Open Cloth Config menu through `Mod Menu` or with a notepad in `.minecraft/config/twas.json`
3. Enter your credentials for twitch integration (reload game if using notepad).
4. Join any world/server with Armor Poser to start the integration!

Twitch Armor Stand provides following commands:

> /twas bound twitch_nickname

By executing this command while looking at an armor stand, you will bound it to a twitch user with specified nickname.

> /twas unbound twitch_nickname

By executing this command you will unbound any existing armor stands that were previously bounded to this twitch username.

You can also write `/twas unbound` without specifying the twitch username, but then you will have to look at the specific armor stand you want to unbound from its twitch user.

> /twas simulate twitch_nickname chat message with a lot of words

This command will simulate a message from specified user.

For example: `/twas simulate funnyperson !s hello` will simulate FunnyPerson's chat message "!s hello".

To interact with bounded armor stand your viewers will need to write following commands:
* !s hello
* !s twerk
* !s clap

Those are emotes that will not break nor stop any action
* !s jump

Bounded armor stand starts to jump infinitely until stopped by another action

* !s follow <entity name>


By entering <entity name> (for example, Player Name or some entitie's nametag) the bounded stand will start following specified entity. If the name was wrong, the stand will stop the following action.

* !s stop

Simply stops any action that was executing before.

## Dependencies

You have to install following mods to play with Twitching Armor Stand: 
* [Fabric API](https://modrinth.com/mod/fabric-api)
* [Armor Poser](https://modrinth.com/mod/armor-poser)
* [Cloth Config](https://modrinth.com/mod/cloth-config)
* [Integration API](https://modrinth.com/mod/integration-api)
* [Client Entities Lib](https://modrinth.com/mod/client-entities)

## License

This mod is available under the MIT license. Feel free to learn from it and incorporate it in your own projects.
