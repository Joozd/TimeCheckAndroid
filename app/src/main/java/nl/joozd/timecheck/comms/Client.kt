package nl.joozd.timecheck.comms

import timeCheckProtocol.Packet
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLSocketFactory

class Client: Closeable {
    private val socket = try {
        SSLSocketFactory.getDefault().createSocket(
            SERVER_URL,
            SERVER_PORT
        )
    } catch (ioe: IOException) {
        Log.w(TAG, "Error creating SSLSocket:")
        Log.w(TAG, ioe.stackTrace.toString())
        null
    }

    /**
     * Send data to server. On success, returns the amount of bytes sent, on failure returns a negative Integer
     * TODO define return codes in [CloudFunctionResults]
     */
    suspend fun sendToServer(packet: Packet): Int {
        println("sending ${packet.payload.size} bytes")
        // Log.d("SendToServer:", packet.message.take(40).toByteArray().toString(Charsets.UTF_8))
        try {
            socket?.let {
                @Suppress("BlockingMethodInNonBlockingContext")
                withContext(Dispatchers.IO) {
                    val output = BufferedOutputStream(it.getOutputStream())
                    output.write(packet.rawData)
                    output.flush()
                }
                return packet.rawData.size
            }
            Log.e(TAG, "Error 0005: Socket is null")
            return -5
        } catch (he: UnknownHostException) {
            val exceptionString = "An exception 0001 occurred:\n ${he.printStackTrace()}"
            Log.e(TAG, exceptionString, he)
            return -1
        } catch (ioe: IOException) {
            val exceptionString = "An exception 0002 occurred:\n ${ioe.printStackTrace()}"
            Log.e(TAG, exceptionString, ioe)
            return -2
        } catch (ce: ConnectException) {
            val exceptionString = "An exception 0003 occurred:\n ${ce.printStackTrace()}"
            Log.e(TAG, exceptionString, ce)
            return -3
        } catch (se: SocketException) {
            val exceptionString = "An exception 0004 occurred:\n ${se.printStackTrace()}"
            Log.e(TAG, exceptionString, se)
            return -4
        }
    }

    /**
     * Runs listsner f with a 0-100 percentage completed value
     */
    suspend fun readFromServer(f: (Int) -> Unit = {}): ByteArray? {
        try {
            socket?.let {
                return try {
                     getInput(BufferedInputStream(withContext(Dispatchers.IO) { it.getInputStream() }), f).also{
                         println("got ${it.toString(Charsets.UTF_8)}")
                     }
                } catch (e: IOException) {
                    Log.e(TAG, "Error: $e, ${e.printStackTrace()}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error: $e, ${e.printStackTrace()}")
        }
        return null
    }

    /**
     * Runs listener f with a 0-100 percentage completed value
     */
    private suspend fun getInput(inputStream: BufferedInputStream, f: (Int) -> Unit = {}): ByteArray = withContext(Dispatchers.IO) {
        println("receiving packet")
        Packet.fromInputStream(inputStream, f).payload.also{
            println("Done!")
        }
    }


    override fun close() {
        socket?.close()
    }

    companion object {

        const val SERVER_URL = "joozd.nl"
        const val SERVER_PORT = 13337
        const val TAG = "comm.protocol.Client"
        const val MAX_MESSAGE_SIZE = Int.MAX_VALUE - 1

        /**
         * Returns an open instance if it is available
         * Client will be locked untill starting timeOut()
         */

    }
}