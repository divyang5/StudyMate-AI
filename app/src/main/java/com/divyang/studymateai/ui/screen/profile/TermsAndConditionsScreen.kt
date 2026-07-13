package com.divyang.studymateai.ui.screen.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.divyang.studymateai.ads.findActivity
import com.divyang.studymateai.navigation.Routes
import com.divyang.studymateai.ui.components.AppColors
import com.divyang.studymateai.ui.components.AppTopBar
import com.divyang.studymateai.ui.components.verticalScrollbar

/**
 * Terms of use. In gate mode (first launch / after a terms-version bump) the
 * app is unusable until the user accepts — declining exits the app. After
 * acceptance the same screen is reachable read-only from App settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndConditionsScreen(
    navController: NavController,
    requireAcceptance: Boolean,
    onAccepted: () -> Unit = {}
) {
    val activity = LocalContext.current.findActivity()

    if (requireAcceptance) {
        // No app to go "back" to before the terms are accepted.
        BackHandler { activity?.finish() }
    }

    Scaffold(
        topBar = {
            if (requireAcceptance) {
                AppTopBar(title = "Terms & Conditions", onBack = { activity?.finish() })
            } else {
                AppTopBar(title = "Terms & Conditions", onBack = { navController.popBackStack() })
            }
        },
        bottomBar = {
            if (requireAcceptance) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "By tapping Accept & Continue you agree to these Terms & Conditions " +
                                "and the Privacy Policy. If you don't agree, please don't use this app.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { activity?.finish() },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                            ) {
                                Text("Decline & Exit", color = MaterialTheme.colorScheme.error)
                            }
                            Button(
                                onClick = onAccepted,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppColors.Purple,
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                            ) {
                                Text("Accept & Continue", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScrollbar(listState),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Last updated: July 13, 2026",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            items(termsSections) { section ->
                Column {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = section.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                    )
                }
            }

            item {
                TextButton(onClick = { navController.navigate(Routes.PrivacyPolicy.route) }) {
                    Text("Read the Privacy Policy", color = AppColors.Purple)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private data class TermsSection(val title: String, val body: String)

private val termsSections = listOf(
    TermsSection(
        "1. Acceptance of these terms",
        "By creating an account or using StudyMate AI (the \"app\"), you agree to these " +
            "Terms & Conditions and to our Privacy Policy. If you do not agree with any part " +
            "of them, do not use the app."
    ),
    TermsSection(
        "2. What StudyMate AI does",
        "StudyMate AI is a study aid. You can import or scan documents into chapters, and the " +
            "app can generate quizzes, summaries, flashcards, and chapter titles from your " +
            "content using Google's Gemini AI service. An account and an internet connection " +
            "are required for AI features."
    ),
    TermsSection(
        "3. Your account",
        "You must provide accurate information when creating an account and keep your login " +
            "credentials secure. You must be at least 13 years old (or the age of digital " +
            "consent in your country) to use the app. We may suspend or terminate accounts " +
            "that violate these terms."
    ),
    TermsSection(
        "4. Your content",
        "You keep ownership of the documents and text you import. You confirm that you have " +
            "the right to use the material you upload — for example your own notes, or study " +
            "material you are permitted to copy. Your chapters and quiz history are stored in " +
            "your account so the app can provide its features. Do not upload unlawful content " +
            "or other people's personal information without their permission."
    ),
    TermsSection(
        "5. AI-generated content",
        "Quizzes, summaries, flashcards, and suggested titles are generated automatically by " +
            "Google Gemini. AI output can be inaccurate, incomplete, or misleading. It is " +
            "provided for study assistance only, with no guarantee of correctness, and is not " +
            "professional, medical, legal, or academic advice. Always verify important " +
            "information against your original material."
    ),
    TermsSection(
        "6. Your Gemini API key (optional)",
        "You may add your own Google Gemini API key for unlimited generations. If you do: " +
            "(a) the key is stored encrypted on your device only — it is never uploaded to our " +
            "servers or any cloud service, and it is deleted when you remove it or sign out; " +
            "(b) generation requests are sent directly to Google under your key, so usage, " +
            "rate limits, and any charges are strictly between you and Google and governed by " +
            "Google's API terms; (c) you are responsible for keeping your key confidential and " +
            "for all activity performed with it; (d) we are not liable for any cost, quota " +
            "consumption, suspension, or damage arising from the use of your key."
    ),
    TermsSection(
        "7. Free plan and advertising",
        "Without a personal API key, AI generations use the app's shared key and are limited " +
            "per day; the limit may change at any time. The app shows advertising, including " +
            "rewarded ads, provided by Google AdMob. Ads are shown whether or not you use a " +
            "personal API key."
    ),
    TermsSection(
        "8. Acceptable use",
        "You agree not to: use the app for unlawful purposes; attempt to bypass generation " +
            "limits or extract the app's shared API key; interfere with or disrupt the " +
            "service; use the AI features to generate harmful, abusive, or infringing " +
            "content; or reverse engineer the app except where the law expressly permits it."
    ),
    TermsSection(
        "9. Intellectual property",
        "The app, its design, branding, and code remain the property of the developer. Using " +
            "the app does not transfer any ownership rights to you, other than the rights you " +
            "already hold in your own content."
    ),
    TermsSection(
        "10. Termination",
        "You may stop using the app or delete your account at any time. We may suspend or " +
            "end your access if you breach these terms. Sections that by their nature should " +
            "survive termination (such as liability limits) will survive."
    ),
    TermsSection(
        "11. Disclaimers and limitation of liability",
        "The app is provided \"as is\" and \"as available\". To the maximum extent permitted " +
            "by law, we are not liable for indirect or consequential damages, loss of data, " +
            "inaccuracies in AI-generated content, or outages of third-party services the app " +
            "relies on (including Google Firebase, Google Gemini, and Google AdMob)."
    ),
    TermsSection(
        "12. Changes to these terms",
        "We may update these terms from time to time. When the changes are material, the app " +
            "will ask you to review and accept the new version before continuing to use it."
    ),
    TermsSection(
        "13. Contact",
        "Questions about these terms? Contact us at the support address listed on the app's " +
            "store page."
    )
)
