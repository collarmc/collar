# collar

Online services for minecraft mods.

## Features
* Create long running groups or short lived parties of players to share coordinates, waypoints and chat securely
* Encrypted private waypoints
* Server stored friend lists
* Uploading and distributing player textures, such as capes and avatars
* Custom message formats for private, group and nearby messaging (build your own mod!)

## Security
* Messaging and coordinte sharing is end to end encrypted using an implementation of [Signal's](https://signal.org) encryption protocol
* Profile data, such as your private waypoints, is encrypted before sending to the collar server.

## Coming soon
* emotes using the [Emotecraft](https://github.com/KosmX/emotes) format.

## Discord
[Join the Discord](https://discord.gg/EG2e9dkPBf)

## Building
To build run you will need to have Maven 3 installed and JDK 15.

Run in the command line:
`mvn clean install`

## Building a mod?

Checkout [team-catgirl/collar-mod](https://github.com/team-catgirl/collar-mod) for more information.
