package eu.thesimplecloud.base.manager.startup

import com.mongodb.MongoClient
import eu.thesimplecloud.api.CloudAPI
import eu.thesimplecloud.api.directorypaths.DirectoryPaths
import eu.thesimplecloud.api.screen.ICommandExecutable
import eu.thesimplecloud.base.MongoBuilder
import eu.thesimplecloud.base.MongoController
import eu.thesimplecloud.base.manager.config.MongoConfigLoader
import eu.thesimplecloud.base.manager.config.TemplatesConfigLoader
import eu.thesimplecloud.base.manager.filehandler.CloudServiceGroupFileHandler
import eu.thesimplecloud.base.manager.filehandler.WrapperFileHandler
import eu.thesimplecloud.base.manager.impl.CloudAPIImpl
import eu.thesimplecloud.base.manager.ingamecommands.IngameCommandUpdater
import eu.thesimplecloud.base.manager.listener.CloudListener
import eu.thesimplecloud.base.manager.listener.ModuleEventListener
import eu.thesimplecloud.base.manager.mongo.MongoServerInformation
import eu.thesimplecloud.base.manager.packet.IPacketRegistry
import eu.thesimplecloud.base.manager.packet.PacketRegistry
import eu.thesimplecloud.base.manager.player.IOfflineCloudPlayerHandler
import eu.thesimplecloud.base.manager.player.OfflineCloudPlayerHandler
import eu.thesimplecloud.base.manager.player.PlayerUnregisterScheduler
import eu.thesimplecloud.base.manager.service.ServiceHandler
import eu.thesimplecloud.base.manager.setup.mongo.MongoDBUseEmbedSetup
import eu.thesimplecloud.base.manager.startup.server.CommunicationConnectionHandlerImpl
import eu.thesimplecloud.base.manager.startup.server.ServerHandlerImpl
import eu.thesimplecloud.base.manager.startup.server.TemplateConnectionHandlerImpl
import eu.thesimplecloud.clientserverapi.lib.packet.IPacket
import eu.thesimplecloud.clientserverapi.server.INettyServer
import eu.thesimplecloud.clientserverapi.server.NettyServer
import eu.thesimplecloud.launcher.application.ApplicationClassLoader
import eu.thesimplecloud.launcher.application.ICloudApplication
import eu.thesimplecloud.launcher.exception.module.IModuleHandler
import eu.thesimplecloud.launcher.exception.module.ModuleHandler
import eu.thesimplecloud.launcher.extension.sendMessage
import eu.thesimplecloud.launcher.external.module.ModuleClassLoader
import eu.thesimplecloud.launcher.startup.Launcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.concurrent.thread

class Manager : ICloudApplication {

    val ingameCommandUpdater: IngameCommandUpdater
    val cloudServiceGroupFileHandler = CloudServiceGroupFileHandler()
    val wrapperFileHandler = WrapperFileHandler()
    val templatesConfigLoader = TemplatesConfigLoader()
    val serviceHandler: ServiceHandler = ServiceHandler()

    // only set when embed mongodb is used
    var mongoController: MongoController? = null
        private set
    val mongoClient: MongoClient

    val offlineCloudPlayerHandler: IOfflineCloudPlayerHandler

    val communicationServer: INettyServer<ICommandExecutable>
    val templateServer: INettyServer<ICommandExecutable>
    val packetRegistry: IPacketRegistry = PacketRegistry()
    val playerUnregisterScheduler = PlayerUnregisterScheduler()
    val cloudModuleHandler: IModuleHandler = ModuleHandler()
    val appClassLoader: ApplicationClassLoader

    companion object {
        @JvmStatic
        lateinit var instance: Manager
            private set
    }

    init {
        Logger.getLogger("org.mongodb.driver").level = Level.SEVERE
        instance = this
        CloudAPIImpl()
        CloudAPI.instance.getEventManager().registerListener(this, CloudListener())
        CloudAPI.instance.getEventManager().registerListener(this, ModuleEventListener())
        this.appClassLoader = this::class.java.classLoader as ApplicationClassLoader
        this.appClassLoader.moduleHandler = this.cloudModuleHandler
        this.cloudModuleHandler.setCreateModuleClassLoader { urls, name -> ModuleClassLoader(urls, this.appClassLoader, name, this.cloudModuleHandler) }
        this.ingameCommandUpdater = IngameCommandUpdater()
        if (!MongoConfigLoader().doesConfigFileExist()) {
            Launcher.instance.setupManager.queueSetup(MongoDBUseEmbedSetup())
            Launcher.instance.setupManager.waitFroAllSetups()
        }
        val mongoConfig = MongoConfigLoader().loadConfig()
        if (mongoConfig.embedMongo)
            mongoController = startMongoDBServer(mongoConfig.mongoServerInformation)

        val launcherConfig = Launcher.instance.launcherConfigLoader.loadConfig()
        this.communicationServer = NettyServer<ICommandExecutable>(launcherConfig.host, launcherConfig.port, CommunicationConnectionHandlerImpl(), ServerHandlerImpl())
        val baseAndLauncherLoader = Launcher.instance.getNewClassLoaderWithLauncherAndBase()
        this.communicationServer.setPacketSearchClassLoader(baseAndLauncherLoader)
        this.communicationServer.setClassLoaderToSearchObjectPacketClasses(appClassLoader)
        this.communicationServer.setPacketClassConverter { moveToApplicationClassLoader(it) }
        this.templateServer = NettyServer<ICommandExecutable>(launcherConfig.host, launcherConfig.port + 1, TemplateConnectionHandlerImpl(), ServerHandlerImpl())
        this.templateServer.setPacketSearchClassLoader(baseAndLauncherLoader)
        this.templateServer.setClassLoaderToSearchObjectPacketClasses(appClassLoader)
        this.templateServer.setPacketClassConverter { moveToApplicationClassLoader(it) }
        this.communicationServer.addPacketsByPackage("eu.thesimplecloud.api.network.packets")
        this.communicationServer.addPacketsByPackage("eu.thesimplecloud.base.manager.network.packets")
        this.templateServer.addPacketsByPackage("eu.thesimplecloud.base.manager.network.packets.template")
        thread(start = true, isDaemon = false) { communicationServer.start() }
        createDirectories()
        Launcher.instance.logger.console("Waiting for MongoDB...")
        this.mongoController?.startedPromise?.awaitUninterruptibly()
        mongoClient = mongoConfig.mongoServerInformation.createMongoClient()
        Launcher.instance.logger.console("Connected to MongoDB")

        this.offlineCloudPlayerHandler = OfflineCloudPlayerHandler(mongoConfig.mongoServerInformation)

        this.templateServer.getDirectorySyncManager().setTmpZipDirectory(File(DirectoryPaths.paths.zippedTemplatesPath))
        this.templateServer.getDirectorySyncManager().createDirectorySync(File(DirectoryPaths.paths.templatesPath), DirectoryPaths.paths.templatesPath)
        this.templateServer.getDirectorySyncManager().createDirectorySync(File(DirectoryPaths.paths.modulesPath), DirectoryPaths.paths.modulesPath)
        this.serviceHandler.startThread()
        thread(start = true, isDaemon = false) { templateServer.start() }
        this.playerUnregisterScheduler.startScheduler()
    }

    private fun moveToApplicationClassLoader(clazz: Class<out IPacket>): Class<out IPacket> {
        if (appClassLoader.isThisClassLoader(clazz)) return clazz
        val loadedClass = appClassLoader.loadClass(clazz.name)
        appClassLoader.setCachedClass(loadedClass)
        return loadedClass as Class<out IPacket>
    }

    private fun startMongoDBServer(mongoServerInformation: MongoServerInformation): MongoController {
        val mongoController = MongoController(MongoBuilder()
                .setHost(mongoServerInformation.host)
                .setPort(mongoServerInformation.port)
                .setAdminUserName(mongoServerInformation.adminUserName)
                .setAdminPassword(mongoServerInformation.adminPassword)
                .setDatabase(mongoServerInformation.databaseName)
                .setDirectory(".mongo")
                .setUserName(mongoServerInformation.userName)
                .setUserPassword(mongoServerInformation.password))
        mongoController.start()
        return mongoController
    }

    override fun onEnable() {
        GlobalScope.launch { Launcher.instance.commandManager.registerAllCommands(instance, appClassLoader, "eu.thesimplecloud.base.manager.commands") }
        Launcher.instance.setupManager.waitFroAllSetups()
        this.wrapperFileHandler.loadAll().forEach { CloudAPI.instance.getWrapperManager().update(it) }
        this.cloudServiceGroupFileHandler.loadAll().forEach { CloudAPI.instance.getCloudServiceGroupManager().updateGroup(it) }
        this.templatesConfigLoader.loadConfig().templates.forEach { CloudAPI.instance.getTemplateManager().updateTemplate(it) }

        if (CloudAPI.instance.getWrapperManager().getAllCachedObjects().isNotEmpty()) {
            Launcher.instance.consoleSender.sendMessage("manager.startup.loaded.wrappers", "Loaded following wrappers:")
            CloudAPI.instance.getWrapperManager().getAllCachedObjects().forEach { Launcher.instance.consoleSender.sendMessage("- ${it.obj.getName()}") }
        }

        if (CloudAPI.instance.getTemplateManager().getAllTemplates().isNotEmpty()) {
            Launcher.instance.consoleSender.sendMessage("manager.startup.loaded.templates", "Loaded following templates:")
            CloudAPI.instance.getTemplateManager().getAllTemplates().forEach { Launcher.instance.consoleSender.sendMessage("- ${it.getName()}") }
        }

        if (CloudAPI.instance.getCloudServiceGroupManager().getAllGroups().isNotEmpty()) {
            Launcher.instance.consoleSender.sendMessage("manager.startup.loaded.groups", "Loaded following groups:")
            CloudAPI.instance.getCloudServiceGroupManager().getAllGroups().forEach { Launcher.instance.consoleSender.sendMessage("- ${it.getName()}") }
        }
        thread(start = true, isDaemon = false) {
            this.cloudModuleHandler.loadAllUnloadedModules()
        }
    }


    private fun createDirectories() {
        for (file in listOf(
                File(DirectoryPaths.paths.storagePath),
                File(DirectoryPaths.paths.wrappersPath),
                File(DirectoryPaths.paths.minecraftJarsPath),
                File(DirectoryPaths.paths.serverGroupsPath),
                File(DirectoryPaths.paths.lobbyGroupsPath),
                File(DirectoryPaths.paths.proxyGroupsPath),
                File(DirectoryPaths.paths.languagesPath),
                File(DirectoryPaths.paths.modulesPath),
                File(DirectoryPaths.paths.templatesPath),
                File(DirectoryPaths.paths.templatesPath + "EVERY"),
                File(DirectoryPaths.paths.templatesPath + "EVERY_SERVER"),
                File(DirectoryPaths.paths.templatesPath + "EVERY_PROXY")
        )) {
            file.mkdirs()
        }
    }

    override fun onDisable() {
        this.cloudModuleHandler.unloadAllModules()
        this.mongoClient.close()
        this.mongoController?.stop()?.awaitUninterruptibly()
    }


}