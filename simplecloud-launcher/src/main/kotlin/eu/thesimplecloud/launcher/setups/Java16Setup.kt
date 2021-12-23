package eu.thesimplecloud.launcher.setups

import eu.thesimplecloud.launcher.config.LauncherConfig
import eu.thesimplecloud.launcher.console.setup.ISetup
import eu.thesimplecloud.launcher.console.setup.annotations.SetupQuestion
import eu.thesimplecloud.launcher.startup.Launcher
import java.io.File

class Java16Setup : ISetup{

    @SetupQuestion(0, "The cloud could not find the java 16 path. Please enter it manually")
    fun setupJava16(path: String): Boolean{
        val filePath = File(path)
        return if(filePath.exists()){
            Launcher.instance.consoleSender.sendMessage("Path was set successfully!")
            val launcherConfig = Launcher.instance.launcherConfig
            launcherConfig.javaVersions.java16 = path
            val config = LauncherConfig(launcherConfig.host, launcherConfig.port, launcherConfig.language, launcherConfig.directoryPaths, launcherConfig.javaVersions)
            Launcher.instance.replaceLauncherConfig(config)
            true
        }else {
            Launcher.instance.consoleSender.sendMessage("File does not exist!")
            false
        }
    }

}