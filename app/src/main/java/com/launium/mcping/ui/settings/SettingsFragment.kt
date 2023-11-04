package com.launium.mcping.ui.settings

import android.content.Intent
import android.content.res.AssetManager
import android.net.Uri
import android.os.Bundle
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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

        binding.titleCard.setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/layou233/MCPingForAndroid")
                )
            )
        }
        binding.license.text =
            binding.root.context.assets.open("LICENSE", AssetManager.ACCESS_BUFFER).use {
                String(it.readBytes())
            }
        Linkify.addLinks(binding.license, Linkify.WEB_URLS)
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

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}