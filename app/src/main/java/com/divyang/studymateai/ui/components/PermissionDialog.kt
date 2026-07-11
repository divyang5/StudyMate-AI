package com.divyang.studymateai.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

//@Composable
//fun PermissionDialog(
//    title: String = "Permissions Needed",
//    text: String = "StudyMate AI needs camera and gallery access to scan documents.",
//    confirmText: String = "Allow",
//    onDismiss: () -> Unit,
//    onConfirm: () -> Unit
//) {
//    AlertDialog(
//        onDismissRequest = onDismiss,
//        title            = { Text(title, style = MaterialTheme.typography.titleMedium) },
//        text             = { Text(text, style = MaterialTheme.typography.bodyMedium) },
//        confirmButton    = {
//            Button(onClick = onConfirm) { Text(confirmText) }
//        },
//        dismissButton    = {
//            TextButton(onClick = onDismiss) {
//                Text("Not Now", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
//            }
//        }
//    )
//}


@Composable
fun PermissionDialog(
    text: String,
    confirmText: String = "Allow",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("Permissions Needed", style = MaterialTheme.typography.titleMedium) },
        text             = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        confirmButton    = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text(confirmText, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton    = {
            TextButton(onClick = onDismiss) {
                Text("Not Now", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}