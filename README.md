# collar

Online services for minecraft mods.

Including:
* Coordinate sharing
* Waypoints
* Friend lists

## To-do's

### Short term
* ~~End to end encryption and session check~~
* Tests for `GroupManager` as it is somewhat complicated
* Group waypoints
* Friend lists

### Long term
* End-to-end functional tests (before switching to redis for state)
* Scale from 100s to 1000s of players by using Redis to handle sessions and group state.

## Building
To build run you will need to have Maven 3 installed. 

Execute:
`mvn clean install`

## Using in Forge

We need to shadow the client dependency in your Jar and relocate the `team.catgirl.collar.coordshare` package
in order to avoid conflicts with other mods.

```
repositories {
  mavenLocal()
}
dependencies {
  compile group: 'team.catgirl.collar.collar', name: 'client', version: '1.0-SNAPSHOT'
}
apply plugin: 'com.github.johnrengelman.shadow'
shadowJar {
  // Only shadow fluent-hc
  dependencies {
    include(dependency('team.catgirl.collar:client:.*'))
  }

  // Replace com.yourpackage with your mods package
  relocate 'team.catgirl.collar', 'com.yourpackage.team.catgirl.collar'

  classifier '' // Replace the default JAR
}
reobf {
  shadowJar {} // Reobfuscate the shadowed JAR
}
```
