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
import com.launium.mcping.database.ServerManager
import com.launium.mcping.databinding.FragmentHomeBinding
import com.launium.mcping.databinding.ServerItemBinding
import com.launium.mcping.server.MinecraftServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import java.util.concurrent.atomic.AtomicReference

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var adapter = Adapter(this)

    private val addServerActivity =
        registerForActivityResult(
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

    private var onGoingRefreshJob = AtomicReference<Job?>(null)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // set recycler view
        val linearLayoutManager = LinearLayoutManager(requireContext())
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        binding.container.layoutManager = linearLayoutManager
        binding.container.adapter = adapter

        binding.swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch {
                val semaphore = Semaphore(5)
                adapter.servers.mapIndexed { i, server ->
                    semaphore.acquire()
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val changed = server.requestStatus()
                            if (changed) {
                                adapter.notifyItemChanged(i)
                            }
                        } catch (_: Exception) {
                        }
                        semaphore.release()
                    }
                }.joinAll()
                onGoingRefreshJob.set(null)
                binding.swipeRefreshLayout.isRefreshing = false
            }.let {
                onGoingRefreshJob.set(it)
            }
        }

        binding.fabAddNewServer.setOnClickListener {
            addServerActivity.launch(Intent(requireContext(), AddServerActivity::class.java))
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.container.visibility = View.GONE
        _binding = null
        //addServerActivity.unregister()
    }

    private class Adapter(val home: HomeFragment) : RecyclerView.Adapter<ServerView>() {

        lateinit var servers: List<MinecraftServer>

        fun updateServerList() {
            home.onGoingRefreshJob?.get()?.cancel()
            servers = ServerManager.serverDao.list()
        }

        init {
            updateServerList()
        }

        /*init {
            servers = listOf(
                MinecraftServer("first", "hello"),
                MinecraftServer("second", "world"),
                MinecraftServer("third", "!"),
                MinecraftServer("this", ""),
                MinecraftServer("is good!", ""),
            )
        }*/

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerView {
            return ServerView(
                ServerItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ), this
            )
        }

        override fun getItemCount(): Int {
            if (!::servers.isInitialized) {
                return 0
            }
            return servers.size
        }

        override fun onBindViewHolder(holder: ServerView, position: Int) {
            holder.bind(servers[position])
        }

    }

    private class ServerView(val binding: ServerItemBinding, val adapter: Adapter) :
        RecyclerView.ViewHolder(binding.root) {

        lateinit var server: MinecraftServer

        fun bind(server: MinecraftServer) {
            this.server = server
            binding.button.text = server.name
            setLatency(binding.root.context, binding.pingText, server.latestPing)
            server.icon?.let { // use pack.png as default, do not clear the content
                binding.serverIcon.setImageDrawable(it)
            }
            binding.button.setOnClickListener {
                ServerSheetDialog(binding.root.context, server).apply {
                    setOnCancelListener {
                        setLatency(binding.root.context, binding.pingText, server.latestPing)
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