package com.launium.mcping.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Spannable
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.TooltipCompat
import androidx.core.os.BundleCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.launium.mcping.R
import com.launium.mcping.common.minecraft.parseMinecraftColor
import com.launium.mcping.databinding.ActivityPlayerListBinding
import com.launium.mcping.databinding.PlayerItemBinding
import com.launium.mcping.server.MinecraftServerStatus
import com.launium.mcping.ui.AbstractActivity

class PlayerListActivity : AbstractActivity() {

    companion object {
        const val KEY_PLAYER_LIST = "player_list"
    }

    private lateinit var binding: ActivityPlayerListBinding

    private val adapter = Adapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val playerList = BundleCompat.getParcelableArrayList(
            intent.extras!!, KEY_PLAYER_LIST,
            MinecraftServerStatus.Player::class.java
        )
        if (playerList != null) {
            adapter.servers = playerList
        }

        binding = ActivityPlayerListBinding.inflate(layoutInflater)
        binding.container.layoutManager = LinearLayoutManager(this).apply {
            orientation = LinearLayoutManager.VERTICAL
        }
        binding.container.adapter = adapter

        setContentView(binding.root)
        supportActionBar?.let {
            it.setTitle(R.string.description_online)
            it.setDisplayHomeAsUpEnabled(true)
        }
        setupHomeButton()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private class Adapter : RecyclerView.Adapter<PlayerView>() {

        var servers: List<MinecraftServerStatus.Player> = listOf()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerView {
            return PlayerView(
                PlayerItemBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }

        override fun getItemCount(): Int {
            return servers.size
        }

        override fun onBindViewHolder(holder: PlayerView, position: Int) {
            holder.bind(servers[position])
        }

    }

    private class PlayerView(val binding: PlayerItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private lateinit var name: Spannable
        private lateinit var uuid: String

        init {
            binding.button.setOnClickListener {
                MaterialAlertDialogBuilder(binding.root.context).apply {
                    setTitle(R.string.description_player)
                    setMessage(name)
                    setIcon(R.drawable.ic_people_24dp)
                    setCancelable(true)
                    setPositiveButton(R.string.description_copy_name) { dialog, _ ->
                        val clipboardManager =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboardManager.setPrimaryClip(
                            ClipData.newPlainText("PlayerName", name)
                        )
                        Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT)
                            .show()
                        dialog.dismiss()
                    }
                    setNeutralButton(R.string.description_copy_uuid) { dialog, _ ->
                        val clipboardManager =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboardManager.setPrimaryClip(
                            ClipData.newPlainText("PlayerUUID", uuid)
                        )
                        Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT)
                            .show()
                        dialog.dismiss()
                    }
                }.show()
            }
        }

        fun bind(player: MinecraftServerStatus.Player) {
            name = parseMinecraftColor(player.name)
            binding.button.text = name
            uuid = player.uuid
            TooltipCompat.setTooltipText(binding.button, uuid)
        }

    }

}