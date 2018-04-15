package de.intektor.kentai_http_server

import de.intektor.kentai_http_common.tcp.*
import de.intektor.kentai_http_common.tcp.client_to_server.*
import de.intektor.kentai_http_common.tcp.server_to_client.Status
import de.intektor.kentai_http_common.tcp.server_to_client.UserChange
import de.intektor.kentai_http_common.tcp.server_to_client.UserStatusChangePacketToClient
import de.intektor.kentai_http_common.tcp.server_to_client.UserViewChatPacketToClient
import de.intektor.kentai_http_server.tcp.handlers.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/**
 * @author Intektor
 */
object DirectConnector {

    @Volatile
    private var serverSocket: ServerSocket? = null

    val clientMap: ConcurrentHashMap<UUID, Socket> = ConcurrentHashMap()
    val socketMap: ConcurrentHashMap<Socket, UUID> = ConcurrentHashMap()

    val preferenceMap: ConcurrentHashMap<Socket, MutableList<UUID>> = ConcurrentHashMap()
    val interestedMap: ConcurrentHashMap<UUID, MutableList<Socket>> = ConcurrentHashMap()

    val threadMap: ConcurrentHashMap<Socket, ConnectionThread> = ConcurrentHashMap()

    private val userToChatMap: ConcurrentHashMap<UUID, UUID> = ConcurrentHashMap()
    private val chatToUserMap: ConcurrentHashMap<UUID, MutableSet<UUID>> = ConcurrentHashMap()

    fun start() {
        KentaiTCPOperator.packetRegistry.apply {
            registerHandler(IdentificationPacketToServer::class.java, IdentificationPacketToServerHandler())
            registerHandler(CloseConnectionPacketToServer::class.java, CloseConnectionPacketToServerHandler())
            registerHandler(UserPreferencePacketToServer::class.java, UserPreferencePacketToServerHandler())
            registerHandler(AddUserPreferencePacketToServer::class.java, AddUserPreferencePacketToServerHandler())
            registerHandler(HeartbeatPacketToServer::class.java, HeartbeatPacketToServerHandler())
            registerHandler(TypingPacketToServer::class.java, TypingPacketToServerHandler())
            registerHandler(ViewChatPacketToServer::class.java, ViewChatPacketToServerHandler())
        }

        thread {
            val serverSocket = ServerSocket(17348)
            this.serverSocket = serverSocket

            serverSocket.soTimeout = 0

            while (serverSocket.isBound) {
                val clientSocket = serverSocket.accept()

                val dataIn = DataInputStream(clientSocket.getInputStream())

                val thread = ConnectionThread(clientSocket, dataIn)
                thread.start()

                threadMap[clientSocket] = thread
            }
        }
    }

    @Synchronized
    fun registerClient(userUUID: UUID, socket: Socket) {
        clientMap[userUUID] = socket
        socketMap[socket] = userUUID

        if (!interestedMap.containsKey(userUUID)) interestedMap[userUUID] = mutableListOf()

        for (clientSocket in interestedMap[userUUID]!!) {
            try {
                sendPacket(UserStatusChangePacketToClient(mutableListOf(UserChange(userUUID, Status.ONLINE, System.currentTimeMillis()))),
                        DataOutputStream(clientSocket.getOutputStream()))
            } catch (ignored: Throwable) {
            }
        }
    }

    @Synchronized
    fun unregisterClient(userUUID: UUID, properlyClosed: Boolean) {
        val socket = clientMap[userUUID]
        val thread = if (socket != null) threadMap[socket] else null
        clientMap.remove(userUUID)

        if (socket != null) {
            socketMap.remove(socket)
            threadMap.remove(socket)

            for (uuid in preferenceMap[socket] ?: emptyList<UUID>()) {
                interestedMap[uuid]?.remove(socket)
            }
        }

        thread?.keepConnection = false

        DatabaseConnection.ds.connection.use { connection ->
            connection.prepareStatement("DELETE FROM kentai.user_status_table WHERE user_uuid = ?").use { statement ->
                statement.setString(1, userUUID.toString())
                statement.execute()
            }

            connection.prepareStatement("INSERT INTO kentai.user_status_table (user_uuid, last_time_online, type_closed) VALUES (?, ?, ?)").use { statement ->
                statement.setString(1, userUUID.toString())
                statement.setLong(2, System.currentTimeMillis())
                statement.setInt(3, if (properlyClosed) LastOnlineType.CONNECTION_CLOSED.ordinal else LastOnlineType.CONNECTION_ERROR.ordinal)
                statement.execute()
            }
        }

        for (clientSocket in interestedMap[userUUID]!!) {
            try {
                sendPacket(UserStatusChangePacketToClient(mutableListOf(UserChange(userUUID, if (properlyClosed) Status.OFFLINE_CLOSED else Status.OFFLINE_EXCEPTION, System.currentTimeMillis()))),
                        DataOutputStream(clientSocket.getOutputStream()))
            } catch (ignored: Throwable) {
            }
        }

        val chatUUID = userToChatMap[userUUID]
        if (chatUUID != null) {
            userViewChat(userUUID, chatUUID, false)
        }
    }

    class ConnectionThread(private val clientSocket: Socket, private val dataIn: DataInputStream) : Thread() {

        @Volatile
        var lastTimeHeartbeat: Long = System.currentTimeMillis()

        @Volatile
        var keepConnection: Boolean = true

        override fun run() {
            thread {
                while (keepConnection) {
                    Thread.sleep(10000L)
                    if (System.currentTimeMillis() - 10000L > lastTimeHeartbeat && keepConnection) {
                        clientSocket.close()
                        val uuid = socketMap[clientSocket]!!
                        unregisterClient(uuid, false)
                    }
                }
            }

            try {
                while (keepConnection) {
                    val packet = readPacket(dataIn, KentaiTCPOperator.packetRegistry, Side.SERVER)
                    handlePacket(packet, clientSocket)
                }
            } catch (t: Throwable) {
                val uuid = socketMap[clientSocket]!!

                for (socket in interestedMap[uuid]!!) {
                    try {
                        sendPacket(UserStatusChangePacketToClient(mutableListOf(UserChange(uuid, Status.OFFLINE_EXCEPTION, System.currentTimeMillis()))),
                                DataOutputStream(clientSocket.getOutputStream()))
                    } catch (ignored: Throwable) {
                    }
                }

                unregisterClient(socketMap[clientSocket]!!, false)
            }
        }
    }

    @Synchronized
    fun userViewChat(userUUID: UUID, chatUUID: UUID, view: Boolean) {
        if (!chatToUserMap.containsKey(chatUUID)) chatToUserMap[chatUUID] = hashSetOf()

        val users = chatToUserMap[chatUUID]!!

        val viewPacket = UserViewChatPacketToClient(userUUID, chatUUID, view)
        for (user in users) {
            val socket = clientMap[user] ?: continue
            try {
                sendPacket(viewPacket, DataOutputStream(socket.getOutputStream()))
            } catch (t: Throwable) {
                unregisterClient(user, false)
            }
        }

        userToChatMap[userUUID] = chatUUID
        if (view) {
            val dataOut = DataOutputStream(clientMap[userUUID]!!.getOutputStream())
            for (user in users) {
                sendPacket(UserViewChatPacketToClient(user, chatUUID, true), dataOut)
            }

            users += userUUID
        } else {
            users -= userUUID
            if (users.isEmpty()) {
                chatToUserMap.remove(chatUUID)
            }
        }
    }
}
