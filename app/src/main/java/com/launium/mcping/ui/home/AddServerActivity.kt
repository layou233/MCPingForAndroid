package com.launium.mcping.ui.home

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.launium.mcping.Application
import com.launium.mcping.R
import com.launium.mcping.database.ServerManager
import com.launium.mcping.databinding.ActivityAddServerBinding
import com.launium.mcping.server.MinecraftServer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AddServerActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AddServerActivity"
    }

    private var _binding: ActivityAddServerBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityAddServerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.let {
            it.setTitle(R.string.title_add_server)
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeAsUpIndicator(
                AppCompatResources.getDrawable(
                    this@AddServerActivity, R.drawable.ic_arrow_back_24dp
                )
            )
            //it.setHomeButtonEnabled(true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.add_server_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                return true
            }

            R.id.save_new_server -> {
                GlobalScope.launch {
                    try {
                        ServerManager.serverDao.add(
                            MinecraftServer(
                                binding.inputDisplayName.text.toString(),
                                binding.inputAddress.text.toString()
                            )
                        )
                    } catch (e: Exception) {
                        val stackTrace = Log.getStackTraceString(e)
                        Log.e(TAG, stackTrace)
                        runOnUiThread {
                            Toast.makeText(Application.instance, stackTrace, Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                }
                onBackPressedDispatcher.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}