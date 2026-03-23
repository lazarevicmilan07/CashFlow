package com.moneytracker.simplebudget.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneytracker.simplebudget.R
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val description: String,
    val accentColor: Color,
    val secondaryColor: Color
)

@Composable
private fun rememberOnboardingPages(): List<OnboardingPage> {
    val page1Title = stringResource(R.string.onboarding_page1_title)
    val page1Subtitle = stringResource(R.string.onboarding_page1_subtitle)
    val page1Description = stringResource(R.string.onboarding_page1_description)
    val page2Title = stringResource(R.string.onboarding_page2_title)
    val page2Subtitle = stringResource(R.string.onboarding_page2_subtitle)
    val page2Description = stringResource(R.string.onboarding_page2_description)
    val page3Title = stringResource(R.string.onboarding_page3_title)
    val page3Subtitle = stringResource(R.string.onboarding_page3_subtitle)
    val page3Description = stringResource(R.string.onboarding_page3_description)
    val page4Title = stringResource(R.string.onboarding_page4_title)
    val page4Subtitle = stringResource(R.string.onboarding_page4_subtitle)
    val page4Description = stringResource(R.string.onboarding_page4_description)
    return remember(page1Title, page2Title, page3Title, page4Title) {
        listOf(
            OnboardingPage(
                icon = Icons.Filled.Receipt,
                title = page1Title,
                subtitle = page1Subtitle,
                description = page1Description,
                accentColor = Color(0xFF1A73E8),
                secondaryColor = Color(0xFF4FC3F7)
            ),
            OnboardingPage(
                icon = Icons.Filled.Assessment,
                title = page2Title,
                subtitle = page2Subtitle,
                description = page2Description,
                accentColor = Color(0xFF4CAF50),
                secondaryColor = Color(0xFF81C784)
            ),
            OnboardingPage(
                icon = Icons.Filled.AccountBalance,
                title = page3Title,
                subtitle = page3Subtitle,
                description = page3Description,
                accentColor = Color(0xFFFF9800),
                secondaryColor = Color(0xFFFFCC02)
            ),
            OnboardingPage(
                icon = Icons.Filled.CloudUpload,
                title = page4Title,
                subtitle = page4Subtitle,
                description = page4Description,
                accentColor = Color(0xFF9C27B0),
                secondaryColor = Color(0xFFCE93D8)
            )
        )
    }
}

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pages = rememberOnboardingPages()
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size - 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Skip button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                val skipAlpha by animateFloatAsState(
                    targetValue = if (isLastPage) 0f else 1f,
                    animationSpec = tween(200),
                    label = "skip_alpha"
                )
                TextButton(
                    onClick = onFinish,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .alpha(skipAlpha),
                    enabled = !isLastPage
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_skip),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                }
            }

            // Pager content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(
                    page = pages[page]
                )
            }

            // Bottom section: indicators + button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page indicators
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    pages.forEachIndexed { index, page ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 28.dp else 8.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "indicator_width"
                        )
                        val color by animateColorAsState(
                            targetValue = if (isSelected) page.accentColor else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            animationSpec = tween(300),
                            label = "indicator_color"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }

                // Action button
                val currentPage = pages[pagerState.currentPage]
                val buttonColor by animateColorAsState(
                    targetValue = currentPage.accentColor,
                    animationSpec = tween(300),
                    label = "button_color"
                )

                Button(
                    onClick = {
                        if (isLastPage) {
                            onFinish()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor
                    )
                ) {
                    Text(
                        text = if (isLastPage) stringResource(R.string.onboarding_get_started) else stringResource(R.string.onboarding_next),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    if (!isLastPage) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon with gradient background circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            page.accentColor.copy(alpha = 0.15f),
                            page.secondaryColor.copy(alpha = 0.15f)
                        )
                    )
                )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                page.accentColor,
                                page.secondaryColor
                            )
                        )
                    )
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Title
        Text(
            text = page.title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text(
            text = page.subtitle,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = page.accentColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Description
        Text(
            text = page.description,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}
