package com.launium.mcping.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.launium.mcping.R
import com.launium.mcping.database.ServerManager
import com.launium.mcping.databinding.FragmentHomeBinding
import com.launium.mcping.databinding.ServerItemBinding
import com.launium.mcping.server.MinecraftServer

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var adapter = Adapter()

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

        binding.fabAddNewServer.setOnClickListener {
            startActivityForResult(Intent(requireContext(), AddServerActivity::class.java), 1)
        }

        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            1 -> adapter.updateServerList(true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.container.visibility = View.GONE
        _binding = null
    }

    private class Adapter : RecyclerView.Adapter<ServerView>() {

        lateinit var servers: List<MinecraftServer>

        fun updateServerList(notify: Boolean) {
            servers = ServerManager.serverDao.list()
            if (notify) {
                notifyDataSetChanged()
            }
        }

        init {
            updateServerList(false)
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
                ),
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

    private class ServerView(val binding: ServerItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        lateinit var server: MinecraftServer

        fun bind(server: MinecraftServer) {
            this.server = server
            binding.button.text = server.name
            binding.pingText.text
            binding.pingText.text = R.string.server_latency_message.toString()
                .format(System.currentTimeMillis())

            binding.button.setCompoundDrawablesWithIntrinsicBounds(
                server.icon, null, null, null
            )
            binding.button.setOnClickListener {
                ServerSheetDialog(binding.root.context, server).show()
            }
        }

    }

}