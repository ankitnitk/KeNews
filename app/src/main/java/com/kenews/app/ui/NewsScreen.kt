package com.kenews.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kenews.app.data.api.Article
import kotlin.math.abs

@Composable
fun NewsScreen(vm: NewsViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val categories by vm.categories.collectAsState()
    val selectedCategory by vm.selectedCategory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopBar(onRefresh = vm::refresh)

        CategoryRow(
            categories = categories,
            selected = selectedCategory,
            onSelect = vm::selectCategory,
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when (val s = state) {
                is UiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is UiState.Error -> ErrorView(s.message, vm::refresh)
                is UiState.Success -> SwipeableCardStack(
                    articles = s.articles,
                    onNeedMore = vm::loadMore,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(onRefresh: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                "KeNews",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@Composable
private fun CategoryRow(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(categories) { cat ->
            FilterChip(
                selected = cat == selected,
                onClick = { onSelect(cat) },
                label = { Text(cat) },
            )
        }
    }
}

@Composable
private fun SwipeableCardStack(articles: List<Article>, onNeedMore: () -> Unit) {
    if (articles.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No articles found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    var currentIndex by remember(articles) { mutableIntStateOf(0) }

    LaunchedEffect(currentIndex, articles.size) {
        if (currentIndex >= articles.size - 5) onNeedMore()
    }

    if (currentIndex >= articles.size) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("You're all caught up!", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("${articles.size} articles read", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    Box(Modifier.fillMaxSize().padding(16.dp)) {
        // Show next card peeking behind
        if (currentIndex + 1 < articles.size) {
            ArticleCard(
                article = articles[currentIndex + 1],
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp)
                    .graphicsLayer { scaleX = 0.95f; scaleY = 0.95f; alpha = 0.6f },
                onSwipeLeft = {},
                onSwipeRight = {},
            )
        }

        ArticleCard(
            article = articles[currentIndex],
            modifier = Modifier.fillMaxSize(),
            onSwipeLeft = { currentIndex++ },
            onSwipeRight = { currentIndex++ },
        )

        // Progress indicator
        Text(
            "${currentIndex + 1} / ${articles.size}",
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ArticleCard(
    article: Article,
    modifier: Modifier = Modifier,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(durationMillis = 100),
        label = "card_offset",
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                translationX = animatedOffset
                rotationZ = animatedOffset / 30f
            }
            .pointerInput(article.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            offsetX < -200 -> onSwipeLeft()
                            offsetX > 200 -> onSwipeRight()
                        }
                        offsetX = 0f
                    },
                    onDragCancel = { offsetX = 0f },
                    onHorizontalDrag = { _, delta -> offsetX += delta },
                )
            },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (article.imageUrl != null) {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                // Gradient overlay for readability
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                                startY = 200f,
                            )
                        )
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
            ) {
                CategoryBadge(article.category)
                Spacer(Modifier.height(8.dp))
                Text(
                    article.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (article.imageUrl != null) Color.White else MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    article.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (article.imageUrl != null) Color.White.copy(0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    article.source,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (article.imageUrl != null) Color.White.copy(0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun CategoryBadge(category: String) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            category.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
            letterSpacing = 0.8.sp,
        )
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Couldn't load news", style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}
