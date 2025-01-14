<p align="center">
  <img src="https://i.imgur.com/eTQJ1IX.png" alt="Logo">
</p>

<p>
  <p align="center">
    A simple alternative to other minecraft cloud systems
    <br />
    <a href="https://www.spigotmc.org/resources/simplecloud-simplify-your-network.79466/">SpigotMC</a>
    ·
    <a href="https://repo.thesimplecloud.eu/ui/repos/tree/General/artifactory-build-info">Repository</a>
    ·
    <a href="http://dashboard-nossl.thesimplecloud.eu">Dashboard</a>
    ·
    <a href="https://ci.thesimplecloud.eu/job/SimpleCloudOrganization/job/SimpleCloud/">Jenkins</a>
    ·
    <a href="https://discord.gg/EzGVHXG3GE">Discord</a>
    ·
    <a href="https://ts3server://thesimplecloud.eu">Teamspeak</a>
  </p>

</p>

<br />
<br />

## Information
This is a fork of the official [theSimpleCloud](https://github.com/theSimpleCloud/SimpleCloud). The idea of this fork is to add support for old minecraft versions again, because the official cloud no longer supports them. In addition, functions such as custom service version template are added to make it easier to manage older versions with newer minecraft versions. Please note that this fork works, but is not 100 percent stable.

## Supported Versions
  • 1.13-1.18 | [officially](https://github.com/theSimpleCloud/SimpleCloud) supported <br />
  • 1.8 | support added by this fork <br />
  • 1.9-1.12 | you need to patch your server.jar with [simplecloud-dependency-fixer
](https://github.com/SmashGames/simplecloud-dependency-fixer) <br />

## Different java versions / Custom start commands
Just create your own service version with `create serviceversion` and enter e.g. the path to your java version as java start command

## Different service template 
For example, 1.8 servers need a different Worldedit Jar than 1.18 server. That is why there is a template folder for every service version. This is used as a template for all groups with the corresponding service version. Simply create a template (with `create template`) which is called `EVERY_VERSION_PAPER_1_8_8`. Replaces `PAPER_1_8_8` for your service version.

## Update from official to this fork

Video: [YouTube](https://youtu.be/iFwnzjxeDCQ)

!! Backup your cloud !!
1. Download the latest [release](https://github.com/InkaruNET/SimpleCloud/releases). 
2. Delete following files:<br />
  • launcher.json<br />
  • launcher.jar<br />
  • runner.jar<br />
  • storage/base.jar<br />
  • storage/onlineServiceVersions.json<br />
  • storage/minecraftJars<br />
  • storage/dependency-loader.jar
3. Move the contents of the ZIP into the cloud folder (replace already existing files)<br>
  • !! If you want to use your old module configs, you dont need to replace the modules folder from the zip !!
