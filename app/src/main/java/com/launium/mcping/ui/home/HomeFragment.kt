package com.launium.mcping.ui.home

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.color.MaterialColors
import com.google.android.material.resources.MaterialAttributes
import com.launium.mcping.R
import com.launium.mcping.databinding.FragmentHomeBinding
import com.launium.mcping.databinding.ServerItemBinding
import com.launium.mcping.server.MinecraftServer
import java.util.Random

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

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class Adapter : RecyclerView.Adapter<ServerView>() {

        lateinit var servers: List<MinecraftServer>

        init {
            servers = listOf(
                MinecraftServer("first", "hello"),
                MinecraftServer("second", "world"),
                MinecraftServer("third", "!"),
                MinecraftServer("this", ""),
                MinecraftServer("is good!", ""),
            )
        }

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

            binding.button.setOnClickListener {
                ServerSheetDialog(binding.root.context, server).show()
            }
        }

    }

}