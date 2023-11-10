package com.launium.mcping.ui.settings

import android.content.Intent
import android.content.res.AssetManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.launium.mcping.Application
import com.launium.mcping.R
import com.launium.mcping.common.network.DnsServersDetector
import com.launium.mcping.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            binding.srvResolver.editText?.text = Editable.Factory.getInstance()
                .newEditable(getString(R.string.dns_srv_resolver_compatible))
            binding.srvResolver.isEnabled = false
        } else {
            binding.srvResolver.editText!!.text = Editable.Factory.getInstance().newEditable(
                getString(
                    when (Preferences.srvResolver) {
                        Preferences.SRVResolver.SYSTEM_API -> R.string.dns_srv_resolver_system_api
                        Preferences.SRVResolver.COMPATIBLE -> R.string.dns_srv_resolver_compatible
                    }
                )
            )
            (binding.srvResolver.editText as MaterialAutoCompleteTextView).setSimpleItems(R.array.srv_resolver)
            binding.srvResolver.editText!!.addTextChangedListener {
                Preferences.srvResolver = when (it.toString()) {
                    getString(R.string.dns_srv_resolver_system_api) -> Preferences.SRVResolver.SYSTEM_API
                    getString(R.string.dns_srv_resolver_compatible) -> Preferences.SRVResolver.COMPATIBLE
                    else -> Preferences.SRVResolver.SYSTEM_API
                }
            }
        }
        DnsServersDetector(Application.instance).serversNoFallback.let {
            if (it.isNullOrEmpty()) {
                binding.systemDnsList.setText(R.string.dns_no_system)
            } else {
                val dnsListBuilder = StringBuilder()
                it.forEach { dns ->
                    dnsListBuilder.appendLine(dns)
                }
                binding.systemDnsList.text = dnsListBuilder.toString()
            }
        }
        binding.maxConcurrentPings.editText!!.text =
            Editable.Factory.getInstance().newEditable(Preferences.maxConcurrentPings.toString())
        binding.maxConcurrentPings.editText!!.addTextChangedListener {
            val newConcurrency = it.toString().toIntOrNull()
            if (newConcurrency != null && newConcurrency > 0) {
                Preferences.maxConcurrentPings = newConcurrency
            } else {
                binding.maxConcurrentPings.editText!!.text = Editable.Factory.getInstance()
                    .newEditable(Preferences.maxConcurrentPings.toString())
                MaterialAlertDialogBuilder(requireContext()).apply {
                    setTitle(R.string.illegal_concurrency_number)
                    setMessage(R.string.dialog_illegal_concurrency_number)
                    setCancelable(true)
                }.show()
            }
        }
        binding.titleCard.setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW, Uri.parse("https://github.com/layou233/MCPingForAndroid")
                )
            )
        }
        binding.license.text =
            binding.root.context.assets.open("LICENSE", AssetManager.ACCESS_BUFFER).use {
                String(it.readBytes())
            }
        Linkify.addLinks(binding.license, Linkify.WEB_URLS)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}