package eu.thesimplecloud.launcher.setups

import eu.thesimplecloud.launcher.config.LauncherConfig
import eu.thesimplecloud.launcher.console.setup.ISetup
import eu.thesimplecloud.launcher.console.setup.annotations.SetupQuestion
import eu.thesimplecloud.launcher.startup.Launcher
import java.io.File

class Java17Setup : ISetup{

    @SetupQuestion(0, "The cloud could not find the java 17 path. Please enter it manually. If you dont need java 17 for services, enter `java`")
    fun setupJava17(path: String): Boolean{
        if(path.equals("java")){
            Launcher.instance.consoleSender.sendMessage("Java 17 path skipped. Path was set to the default java path!")
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
            launcherConfig.javaVersions.java17 = path
            val config = LauncherConfig(launcherConfig.host, launcherConfig.port, launcherConfig.language, launcherConfig.directoryPaths, launcherConfig.javaVersions)
            Launcher.instance.replaceLauncherConfig(config)
            true
        }else {
            Launcher.instance.consoleSender.sendMessage("File does not exist!")
            false
        }
    }

}