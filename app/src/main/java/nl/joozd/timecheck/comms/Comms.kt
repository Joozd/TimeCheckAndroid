package nl.joozd.timecheck.comms

import timeCheckProtocol.Instructions
import timeCheckProtocol.Packet
import timeCheckProtocol.PacketBuilder
import timeCheckProtocol.TimeStampData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Comms {
    suspend fun getTimeStamp(): TimeStampData? = withContext (Dispatchers.IO) {
        Client().use{ client ->
            with(client){
                sendToServer(Packet.create(Instructions.GET_TIMESTAMP))
                return@withContext readFromServer()?.let {
                    TimeStampData.deserialize(it)} ?: println("read null").let {null}
            }
        }
    }

    suspend fun lookUpCode(code: String): TimeStampData? = withContext(Dispatchers.IO){
        Client().use{ client ->
            with(client){
                sendToServer(PacketBuilder(Instructions.GET_TIME_FROM_CODE).putExtra(code).build())
                readFromServer(){ println("LOOKUP: $it% received")}?.let {
                    TimeStampData.deserialize(it)} ?: println("read null").let {null}
            }
        }
    }
}