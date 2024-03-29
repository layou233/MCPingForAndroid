package com.launium.mcping.ui.home

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.Editable
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.launium.mcping.R
import com.launium.mcping.common.minecraft.parseMinecraftColor
import com.launium.mcping.common.toArrayList
import com.launium.mcping.database.ServerManager
import com.launium.mcping.databinding.ServerSheetBinding
import com.launium.mcping.server.MinecraftServer
import com.launium.mcping.ui.ErrorActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

@SuppressLint("SetTextI18n")
class ServerSheetDialog(
    context: Context,
    private val server: MinecraftServer,
    private val home: Fragment?,
    val isLocal: Boolean = true
) :
    BottomSheetDialog(context) {

    companion object {
        private const val TAG = "ServerSheetDialog"
    }

    private lateinit var binding: ServerSheetBinding

    override fun onStart() {
        super.onStart()
        binding = ServerSheetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            binding.serverSheetName.text = server.name
            binding.serverSheetAddress.text =
                Editable.Factory.getInstance().newEditable(server.address)
            setLatency(context, binding.serverSheetLatencyText, server.latestPing)
            server.icon?.let { binding.serverSheetImage.setImageBitmap(it) }
            binding.serverSheetMotd.text = parseMinecraftColor(server.description)
            binding.serverSheetPlayersText.text = "${server.online}/${server.maxOnline}"
            binding.serverSheetPlayersCard.setOnClickListener {
                home?.startActivity(
                    Intent(context, PlayerListActivity::class.java)
                        .putParcelableArrayListExtra(
                            PlayerListActivity.KEY_PLAYER_LIST,
                            server.players.toArrayList()
                        )
                )
            }
            binding.serverSheetVersionText.text = server.version
            binding.serverSheetVersionCard.setOnClickListener {
                MaterialAlertDialogBuilder(context).apply {
                    setTitle(R.string.description_version)
                    setMessage(server.version)
                    setIcon(R.drawable.ic_update_24dp)
                    setCancelable(true)
                }.show()
            }
            binding.serverSheetTestLatency.setOnClickListener { ping() }
            if (isLocal) {
                binding.serverSheetDeleteServer.setOnClickListener {
                    MaterialAlertDialogBuilder(context).apply {
                        setTitle(R.string.title_confirm_delete)
                        setMessage(R.string.dialog_delete)
                        setIcon(R.drawable.ic_delete_forever_24dp)
                        setCancelable(true)
                        setPositiveButton(R.string.description_delete) { dialog, _ ->
                            try {
                                ServerManager.serverDao.delete(server)
                                server.removed = true
                                dialog.dismiss()
                                onBackPressedDispatcher.onBackPressed()
                            } catch (e: Exception) {
                                val stackTrace = Log.getStackTraceString(e)
                                Log.e(TAG, stackTrace)
                                home?.startActivity(
                                    Intent(
                                        context,
                                        ErrorActivity::class.java
                                    ).putExtra(ErrorActivity.KEY_ERROR, stackTrace)
                                )
                            }
                        }
                        setNegativeButton(R.string.description_cancel) { dialog, _ ->
                            dialog.cancel()
                        }
                    }.show()
                }
            } else {
                binding.serverSheetDeleteServer.isEnabled = false
            }
            binding.serverSheetShareServer.setOnClickListener {
                val clipboardManager =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboardManager.setPrimaryClip(
                    ClipData.newPlainText("ServerAddress", server.uri)
                )
                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT)
                    .show()
            }

            binding.serverSheetCopyIcon.setOnClickListener {
                val clipboardManager =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboardManager.setPrimaryClip(
                    ClipData.newPlainText("ServerMOTDIcon", server.motdIcon)
                )
                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: Exception) {
            val stackTrace = Log.getStackTraceString(e)
            Log.e(TAG, stackTrace)
            home?.startActivity(
                Intent(
                    context,
                    ErrorActivity::class.java
                ).putExtra(ErrorActivity.KEY_ERROR, stackTrace)
            )
        }

        ping()
    }

    private fun ping() {
        lifecycleScope.launch {
            try {
                val changed = GlobalScope.async {
                    server.requestStatus()
                }.await()
                if (changed) {
                    setLatency(context, binding.serverSheetLatencyText, server.latestPing)
                    server.icon?.let { binding.serverSheetImage.setImageBitmap(it) }
                    binding.serverSheetMotd.text = parseMinecraftColor(server.description)
                    binding.serverSheetPlayersText.text = "${server.online}/${server.maxOnline}"
                    binding.serverSheetVersionText.text = server.version
                    if (isLocal) {
                        ServerManager.serverDao.update(server)
                    }
                    Log.d(TAG, "Loaded MOTD: " + server.description)
                }
            } catch (_: CancellationException) {
            } catch (e: Exception) {
                val stackTrace = Log.getStackTraceString(e)
                Log.e(TAG, stackTrace)
                home?.startActivity(
                    Intent(
                        context,
                        ErrorActivity::class.java
                    ).putExtra(ErrorActivity.KEY_ERROR, stackTrace)
                )
            }
        }
    }

}