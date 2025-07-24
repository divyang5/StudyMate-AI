package com.example.studymateai.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. Introduction
            SectionTitle("1. Introduction")
            Text(
                text = "StudyMate AI is committed to protecting your privacy. This policy explains how we collect, use, and safeguard your data when you use our mobile application (\"App\").",
                style = MaterialTheme.typography.bodyMedium
            )

            // 2. Data We Collect
            SectionTitle("2. Data We Collect")
            Text(
                text = "A. Personal Data",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            BulletPoint("Account Information: Email address.")
            BulletPoint("User Content: Notes, scanned documents, and quizzes you create.")

            Text(
                text = "B. Automatically Collected Data",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            BulletPoint("Usage Data: Time spent on features, quiz performance, and error logs (Firebase Analytics).")
            BulletPoint("Device Data: Android version, device model (for compatibility).")

            Text(
                text = "C. Permissions",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            BulletPoint("Camera: For scanning notes (processed locally via ML Kit OCR).")
            BulletPoint("Storage: For getting images for scanning and processing into notes.")

            // 3. How We Use Your Data
            SectionTitle("3. How We Use Your Data")
            DataUsageTable(
                headers = listOf("Purpose", "Data Used", "Legal Basis"),
                rows = listOf(
                    listOf("Provide core features (quiz generation)", "Notes, OCR content", "Contractual necessity"),
                    listOf("Improve app performance", "Usage data, crash logs", "Legitimate interest"),
                    listOf("Personalize recommendations", "Quiz history, weak topics", "User consent")
                )
            )

            // 4. Data Sharing & Third Parties
            SectionTitle("4. Data Sharing & Third Parties")
            Text(
                text = "We do not sell your data. Limited sharing occurs with:",
                style = MaterialTheme.typography.bodyMedium
            )
            BulletPoint("Firebase (Google): For authentication and analytics (Firebase Privacy).")
            BulletPoint("Gemini API (Google AI): Only sends note text for quiz generation (no user identifiers).")

            // 5. Data Security
            SectionTitle("5. Data Security")
            BulletPoint("Encryption: Notes in transit (HTTPS) and at rest (AES-256 in Room DB).")
            BulletPoint("Access Control: Firebase Auth enforces strict role-based access.")

            // 6. User Rights
            SectionTitle("6. User Rights")
            Text(
                text = "You can:",
                style = MaterialTheme.typography.bodyMedium
            )
            BulletPoint("Delete data: Clear notes/quizzes in-app by using delete functionality.")
            BulletPoint("Account deletion: Remove your account anytime while removing all data.")

            // 7. Children's Privacy
            SectionTitle("7. Children's Privacy")
            BulletPoint("Not intended for users under 13 (COPPA compliant).")
            BulletPoint("No knowingly collected data from children.")

            // 8. Policy Updates
            SectionTitle("8. Policy Updates")
            BulletPoint("Notify users via in-app alerts for material changes.")

            // 9. Contact Us
            SectionTitle("9. Contact Us")
            Text(
                text = "For GDPR/CCPA requests or questions:",
                style = MaterialTheme.typography.bodyMedium
            )
            BulletPoint("Email: privacy@studymateai.com")
            BulletPoint("Address: divyangsumesara4@gmail.com")

            // Compliance
            SectionTitle("Key Compliance")
            BulletPoint("Google Play Requirements")
            BulletPoint("GDPR: Articles 13-15")
            BulletPoint("CCPA: \"Do Not Sell My Info\"")

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp),
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.padding(start = 16.dp, top = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DataUsageTable(headers: List<String>, rows: List<List<String>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(8.dp)
        ) {
            headers.forEach { header ->
                Text(
                    text = header,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Data rows
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                row.forEach { cell ->
                    Text(
                        text = cell,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Divider()
        }
    }
}