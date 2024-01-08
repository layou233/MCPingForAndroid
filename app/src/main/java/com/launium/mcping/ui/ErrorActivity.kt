package com.launium.mcping.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.launium.mcping.R
import com.launium.mcping.databinding.ActivityErrorBinding

class ErrorActivity : AbstractActivity() {

    companion object {
        const val KEY_ERROR = "error"
    }

    private lateinit var binding: ActivityErrorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityErrorBinding.inflate(layoutInflater)
        binding.errorMessage.text =
            Editable.Factory.getInstance().newEditable(
                intent.extras?.getString(KEY_ERROR, getString(R.string.error_unknown))
                    ?: getString(R.string.error_unknown)
            )

        setContentView(binding.root)
        supportActionBar?.let {
            it.setTitle(R.string.bummer)
            it.setDisplayHomeAsUpEnabled(true)
        }
        setupHomeButton()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.error_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                return true
            }

            R.id.error_copy -> {
                val clipboardManager =
                    getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboardManager.setPrimaryClip(
                    ClipData.newPlainText("ExceptionDetails", binding.errorMessage.text)
                )
                Toast.makeText(this, "Copied!", Toast.LENGTH_SHORT)
                    .show()
                return true
            }

            R.id.error_report_bug -> {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/layou233/MCPingForAndroid/issues")
                    )
                )
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}