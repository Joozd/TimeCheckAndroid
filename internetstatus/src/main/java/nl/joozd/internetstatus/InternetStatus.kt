package nl.joozd.internetstatus

import android.Manifest.permission.ACCESS_NETWORK_STATE
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Gives observable variables about internet status.
 * Coroutines used to insert LiveData updating onto main thread.
 *
 * create with getInstance(context)
 *  (singleton implementation because this should be observed only once per app)
 *
 * Observable variables:
 * [internetAvailableLiveData]: true if at least one network available with NET_CAPABILITY_INTERNET
 */

@Suppress("unused", "MemberVisibilityCanBePrivate")
class InternetStatus @RequiresPermission(ACCESS_NETWORK_STATE) constructor(context: Context) {
    /*******************************************************************************************
     * Observables
     * This is the part you'r program will look at
     *******************************************************************************************/

    // This is the one to watch. Immutable version of mutableInternetAvailable.
    val internetAvailableLiveData: LiveData<Boolean>
        get() = mutableInternetAvailable

    //If you need a snapshot
    // gives null if no callback received from connectivityManager yet
    val internetAvailable: Boolean
        get() = onlineNetworks.isNotEmpty()

    fun addCallback(callback: Callback){
        registeredCallbacks.add(callback)
    }

    fun removeCallback(callback: Callback){
        registeredCallbacks.remove(callback)
    }

    /*******************************************************************************************
     * Private parts
     *******************************************************************************************/

    // Registered callbacks
    private val registeredCallbacks = mutableListOf<Callback>()

    // context. Can get it wherever you want, I get it from my App class
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager


    /**
     * Mutable livedata
     */
    // This is the one that gets changed, immutable one is exposed for observing as `internetAvailable`
    // wanted to name it _internetAvailable, but compiler complained.
    private val mutableInternetAvailable = MutableLiveData<Boolean>()

    /**
     * Local variables
     */
    private val onlineNetworks = mutableListOf<Network>()

    /**
     * Register listeners
     */
    init{
        val request = NetworkRequest.Builder().apply{
            addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }.build()

        connectivityManager.registerNetworkCallback(request, object: ConnectivityManager.NetworkCallback(){
            override fun onAvailable(network: Network) {
                onlineNetworks.add(network)
                onlineNetworks.isNotEmpty().let { isOnline ->
                    mutableInternetAvailable.postValue(isOnline)
                    registeredCallbacks.forEach {
                        it.onInternetStatusChanged(isOnline)
                    }
                }
            }

            override fun onLost(network: Network) {
                onlineNetworks.remove(network)
                onlineNetworks.isNotEmpty().let { isOnline ->
                    mutableInternetAvailable.postValue(isOnline)
                    registeredCallbacks.forEach {
                        it.onInternetStatusChanged(isOnline)
                    }
                }
            }
        })
    }

    fun interface Callback{
        fun onInternetStatusChanged(online: Boolean)
    }
}