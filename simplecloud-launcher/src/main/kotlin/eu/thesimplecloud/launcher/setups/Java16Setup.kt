package eu.thesimplecloud.launcher.setups

import eu.thesimplecloud.launcher.config.LauncherConfig
import eu.thesimplecloud.launcher.console.setup.ISetup
import eu.thesimplecloud.launcher.console.setup.annotations.SetupQuestion
import eu.thesimplecloud.launcher.startup.Launcher
import java.io.File

class Java16Setup : ISetup{

    @SetupQuestion(0, "The cloud could not find the java 16 path. Please enter it manually. If you dont need java 16 for services, enter `java`")
    fun setupJava16(path: String): Boolean{
        if(path.equals("java")){
            Launcher.instance.consoleSender.sendMessage("Java 16 path skipped. Path was set to the default java path!")
            val launcherConfig = Launcher.instance.launcherConfig
            launcherConfig.javaVersions.java8 = "java"
            val config = LauncherConfig(launcherConfig.host, launcherConfig.port, launcherConfig.language, launcherConfig.directoryPaths, launcherConfig.javaVersions)
            Launcher.instance.replaceLauncherConfig(config)
            return true
        }
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