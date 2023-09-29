package com.launium.mcping.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.Editable
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.launium.mcping.R
import com.launium.mcping.database.ServerManager
import com.launium.mcping.databinding.ServerSheetBinding
import com.launium.mcping.server.MinecraftServer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class ServerSheetDialog(context: Context, private val server: MinecraftServer) :
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
            server.icon?.let { binding.serverSheetImage.setImageDrawable(it) }
            binding.serverSheetTestLatency.setOnClickListener { ping() }
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
                            onBackPressed()
                        } catch (e: Exception) {
                            val stackTrace = Log.getStackTraceString(e)
                            Log.e(TAG, stackTrace)
                            Toast.makeText(context, stackTrace, Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                    setNegativeButton(R.string.description_cancel) { dialog, _ ->
                        dialog.cancel()
                    }
                }.show()
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
            Toast.makeText(context, stackTrace, Toast.LENGTH_LONG)
                .show()
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
                    server.icon?.let { binding.serverSheetImage.setImageDrawable(it) }
                }
            } catch (_: CancellationException) {
            } catch (e: Exception) {
                val stackTrace = Log.getStackTraceString(e)
                Log.e(TAG, stackTrace)
                Toast.makeText(
                    context,
                    stackTrace.substringBefore('\n'),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

}