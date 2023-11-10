package com.launium.mcping.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.launium.mcping.R
import com.launium.mcping.database.ServerManager
import com.launium.mcping.databinding.ActivityAddServerBinding
import com.launium.mcping.server.MinecraftServer
import com.launium.mcping.ui.ErrorActivity
import java.net.URI

class AddServerActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AddServerActivity"
    }

    private lateinit var binding: ActivityAddServerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddServerBinding.inflate(layoutInflater)
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
                try {
                    URI.create("mc://${binding.inputAddress.text}")
                    //InetAddress.getByName(uri.host) // try resolve hostname
                } catch (e: Exception) {
                    val stackTrace = Log.getStackTraceString(e).substringBefore('\n')
                    Toast.makeText(
                        binding.root.context,
                        "Bad address: $stackTrace",
                        Toast.LENGTH_LONG
                    ).show()
                    return false
                }
                try {
                    ServerManager.serverDao.add(
                        MinecraftServer(
                            binding.inputDisplayName.text.toString(),
                            binding.inputAddress.text.toString()
                        ).apply {
                            ignoreSRVRedirect = binding.ignoreSrvRedirect.isChecked
                        }
                    )
                    setResult(RESULT_OK)
                } catch (e: Exception) {
                    val stackTrace = Log.getStackTraceString(e)
                    Log.e(TAG, stackTrace)
                    startActivity(
                        Intent(
                            binding.root.context,
                            ErrorActivity::class.java
                        ).putExtra(ErrorActivity.KEY_ERROR, stackTrace)
                    )
                }
                onBackPressedDispatcher.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}