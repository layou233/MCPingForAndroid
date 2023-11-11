package com.launium.mcping.ui.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.launium.mcping.R
import com.launium.mcping.database.ServerManager
import com.launium.mcping.databinding.FragmentHomeBinding
import com.launium.mcping.databinding.ServerItemBinding
import com.launium.mcping.server.MinecraftServer
import com.launium.mcping.ui.settings.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var adapter = Adapter(this)

    private val addServerActivity = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        when (it.resultCode) {
            Activity.RESULT_OK -> {
                val newPosition = adapter.servers.size
                adapter.updateServerList()
                adapter.notifyItemInserted(newPosition)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // set recycler view
        binding.container.layoutManager = LinearLayoutManager(requireContext()).apply {
            orientation = LinearLayoutManager.VERTICAL
        }
        lifecycleScope.launch(Dispatchers.IO) {
            adapter.updateServerList()
            binding.container.adapter = adapter
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch(Dispatchers.Default) {
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
                                ServerManager.serverDao.update(server)
                            }
                        }
                    }
                }.joinAll()
            }.invokeOnCompletion {
                _binding?.swipeRefreshLayout?.isRefreshing = false
                _binding?.swipeRefreshLayout?.clearAnimation()
            }
        }

        binding.fabAddNewServer.setOnClickListener {
            addServerActivity.launch(Intent(requireContext(), AddServerActivity::class.java))
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
        //addServerActivity.unregister()
    }

    private class Adapter(val home: Fragment) : RecyclerView.Adapter<ServerView>() {

        var servers: List<MinecraftServer> = listOf()

        fun updateServerList() {
            servers = ServerManager.serverDao.list()
        }

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
                ServerSheetDialog(binding.root.context, server, adapter.home).apply {
                    setOnCancelListener {
                        setLatency(binding.root.context, binding.pingText, server.latestPing)
                        if (server.icon == null) {
                            binding.serverIcon.setImageResource(R.mipmap.pack)
                        } else {
                            binding.serverIcon.setImageBitmap(server.icon)
                        }
                        if (server.removed) {
                            adapter.updateServerList()
                            adapter.notifyItemRemoved(adapterPosition)
                        }
                    }
                }.show()
            }
        }

    }

}