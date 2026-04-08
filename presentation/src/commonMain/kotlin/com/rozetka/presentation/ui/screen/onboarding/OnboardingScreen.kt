package com.rozetka.presentation.ui.screen.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.rozetka.presentation.generated.resources.*
import com.rozetka.presentation.ui.screen.onboarding.components.OnboardingPagerContent
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

data class OnboardingPage(
    val title: StringResource,
    val description: StringResource,
    val icon: ImageVector
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            title = Res.string.onboarding_title_1,
            description = Res.string.onboarding_desc_1,
            icon = Icons.Default.Public
        ),
        OnboardingPage(
            title = Res.string.onboarding_title_2,
            description = Res.string.onboarding_desc_2,
            icon = Icons.Default.WifiOff
        ),
        OnboardingPage(
            title = Res.string.onboarding_title_3,
            description = Res.string.onboarding_desc_3,
            icon = Icons.AutoMirrored.Filled.List
        ),
        OnboardingPage(
            title = Res.string.onboarding_title_4,
            description = Res.string.onboarding_desc_4,
            icon = Icons.Default.Favorite
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.padding(bottom = 24.dp), horizontalArrangement = Arrangement.Center) {
                    repeat(pagerState.pageCount) { iteration ->
                        val color =
                            if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        Box(
                            modifier = Modifier.padding(horizontal = 4.dp).clip(CircleShape)
                                .background(color).size(8.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.lastIndex) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    pagerState.currentPage + 1
                                )
                            }
                        } else {
                            onComplete()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(
                        text = if (pagerState.currentPage == pages.lastIndex) stringResource(Res.string.onboarding_finish) else stringResource(
                            Res.string.onboarding_next
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) { page ->
            OnboardingPagerContent(
                title = pages[page].title,
                description = pages[page].description,
                icon = pages[page].icon
            )
        }
    }
}
