package eu.thesimplecloud.base.manager.commands

import eu.thesimplecloud.api.command.ICommandSender
import eu.thesimplecloud.launcher.console.command.CommandType
import eu.thesimplecloud.launcher.console.command.ICommandHandler
import eu.thesimplecloud.launcher.console.command.annotations.Command
import eu.thesimplecloud.launcher.console.command.annotations.CommandSubPath
import eu.thesimplecloud.launcher.startup.Launcher

@Command("version", CommandType.CONSOLE_AND_INGAME, "cloud.command.version")
class VersionCommand : ICommandHandler {

    @CommandSubPath("", "Display the version")
    fun handleClear(commandSender: ICommandSender) {
        commandSender.sendMessage("Cloud-Version: " + Launcher.instance.getCurrentVersion() + " | Fork-Version: " + Launcher.instance.forkVersion)
    }
}