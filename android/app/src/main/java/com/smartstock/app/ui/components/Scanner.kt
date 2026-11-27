package com.smartstock.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Camera barcode scanning through Google Play services. The scanner UI runs in Play services, so the
 * app needs no camera permission.
 */
@Composable
fun ScanButton(onError: (String) -> Unit = {}, onScanned: (String) -> Unit) {
	val context = LocalContext.current
	val scanned by rememberUpdatedState(onScanned)
	val failed by rememberUpdatedState(onError)
	IconButton(onClick = {
		// Created on tap so a scanner failure reports an error instead of breaking the screen.
		runCatching {
			GmsBarcodeScanning.getClient(context, GmsBarcodeScannerOptions.Builder().enableAutoZoom().build()).startScan()
		}.onSuccess { scan ->
			scan.addOnSuccessListener { barcode -> barcode.rawValue?.trim()?.takeIf(String::isNotEmpty)?.let(scanned) }
				.addOnFailureListener { failed(it.message ?: "Scanner unavailable") }
		}.onFailure { failed("Scanner unavailable") }
	}) {
		Icon(Icons.Outlined.QrCodeScanner, "Scan barcode")
	}
}
