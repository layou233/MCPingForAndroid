package com.launium.mcping.ui.discovery

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.launium.mcping.R
import com.launium.mcping.databinding.FragmentDiscoveryBinding
import com.launium.mcping.databinding.ServerItemBinding
import com.launium.mcping.server.MinecraftServer
import com.launium.mcping.ui.ErrorActivity
import com.launium.mcping.ui.home.ServerSheetDialog
import com.launium.mcping.ui.home.setLatency
import com.launium.mcping.ui.settings.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import java.net.URL

private const val SERVER_LIST_URL = "https://www.jsip.club/api/ajax.php?request=get_line_list"

class DiscoveryFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    companion object {
        private const val TAG = "DiscoveryFragment"
    }

    private var _binding: FragmentDiscoveryBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val adapter = Adapter(this)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoveryBinding.inflate(inflater, container, false)

        // set recycler view
        binding.container.layoutManager = LinearLayoutManager(requireContext()).apply {
            orientation = LinearLayoutManager.VERTICAL
        }
        binding.container.adapter = adapter

        binding.swipeRefreshLayout.setOnRefreshListener(this)
        if (adapter.servers.isEmpty()) {
            binding.swipeRefreshLayout.isRefreshing = true
            onRefresh()
        }

        return binding.root
    }

    override fun onPause() {
        super.onPause()

        binding.swipeRefreshLayout.isRefreshing = false
        binding.swipeRefreshLayout.clearAnimation()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        binding.swipeRefreshLayout.isRefreshing = false
        binding.swipeRefreshLayout.clearAnimation()
        _binding = null
    }

    override fun onRefresh() {
        lifecycleScope.launch(Dispatchers.Default) {
            val resultObject = try {
                val resultJSON = URL(SERVER_LIST_URL).readText()
                JSONObject.parseObject(resultJSON)
            } catch (e: Exception) {
                val stackTrace = Log.getStackTraceString(e)
                Log.e(TAG, stackTrace)
                startActivity(
                    Intent(
                        binding.root.context, ErrorActivity::class.java
                    ).putExtra(ErrorActivity.KEY_ERROR, stackTrace)
                )
                return@launch
            }
            if (resultObject["code"] != 200) {
                startActivity(
                    Intent(
                        binding.root.context, ErrorActivity::class.java
                    ).putExtra(ErrorActivity.KEY_ERROR, "Unexpected code: $resultObject")
                )
            } else {
                adapter.servers = (resultObject["data"] as JSONArray).map {
                    val serverObject = it as JSONObject
                    return@map MinecraftServer(
                        serverObject["name"] as String, serverObject["address"] as String
                    )
                }
                withContext(Dispatchers.Main) {
                    adapter.notifyDataSetChanged()
                }
                val semaphore = Semaphore(Preferences.maxConcurrentPings)
                adapter.servers.mapIndexedNotNull { i, server ->
                    semaphore.acquire()
                    if (server.removed) {
                        semaphore.release()
                        null
                    } else {
                        lifecycleScope.launch(Dispatchers.IO) {
                            val changed = try {
                                server.requestStatus()
                            } catch (_: Exception) {
                                false
                            }
                            semaphore.release()
                            if (changed) {
                                adapter.notifyItemChanged(i)
                            }
                        }
                    }
                }.joinAll()
            }
        }.invokeOnCompletion {
            _binding?.swipeRefreshLayout?.isRefreshing = false
            _binding?.swipeRefreshLayout?.clearAnimation()
        }
    }


    private class Adapter(val home: Fragment) : RecyclerView.Adapter<ServerView>() {

        var servers: List<MinecraftServer> = listOf()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerView {
            return ServerView(
                ServerItemBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                ), this
            )
        }

        override fun getItemCount(): Int {
            return servers.size
        }

        override fun onBindViewHolder(holder: ServerView, position: Int) {
            holder.bind(servers[position])
        }

    }

    private class ServerView(val binding: ServerItemBinding, val adapter: Adapter) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(server: MinecraftServer) {
            binding.button.text = server.name
            setLatency(binding.root.context, binding.pingText, server.latestPing)
            if (server.icon == null) {
                binding.serverIcon.setImageResource(R.mipmap.pack)
            } else {
                binding.serverIcon.setImageBitmap(server.icon)
            }
            binding.button.setOnClickListener {
                ServerSheetDialog(
                    binding.root.context, server, adapter.home, isLocal = false
                ).apply {
                    setOnCancelListener {
                        setLatency(binding.root.context, binding.pingText, server.latestPing)
                        if (server.icon == null) {
                            binding.serverIcon.setImageResource(R.mipmap.pack)
                        } else {
                            binding.serverIcon.setImageBitmap(server.icon)
                        }
                    }
                }.show()
            }
        }

    }

}