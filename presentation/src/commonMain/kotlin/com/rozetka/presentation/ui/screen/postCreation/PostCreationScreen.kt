package com.rozetka.presentation.ui.screen.postCreation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mohamedrejeb.calf.io.readByteArray
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import com.rozetka.presentation.generated.resources.*
import com.rozetka.presentation.mvi.PostCreationContract
import com.rozetka.presentation.ui.screen.postCreation.components.PostTypeTab
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCreationScreen(
    viewModel: PostCreationViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scope = rememberCoroutineScope()

    val pickerLauncher = rememberFilePickerLauncher(
        type = FilePickerFileType.Image,
        selectionMode = FilePickerSelectionMode.Single,
        onResult = { files ->
            files.firstOrNull()?.let { file ->
                scope.launch {
                    val bytes = file.readByteArray()
                    viewModel.handleEvent(
                        PostCreationContract.Event.OnMediaSelected(
                            bytes,
                            "image.jpg"
                        )
                    )
                }
            }
        }
    )

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onBack()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.create_post),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.handleEvent(PostCreationContract.Event.OnSubmitClicked) },
                        enabled = !state.isLoading && state.selectedSubreddit.isNotBlank() && state.title.isNotBlank(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.38f
                            )
                        )
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(Res.string.post), fontWeight = FontWeight.Bold)
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Groups,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(Res.string.community),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextField(
                            value = state.selectedSubreddit,
                            onValueChange = {
                                viewModel.handleEvent(
                                    PostCreationContract.Event.OnSubredditSelected(
                                        it
                                    )
                                )
                            },
                            placeholder = {
                                Text(
                                    stringResource(Res.string.subreddit_placeholder),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PostTypeTab(
                    modifier = Modifier.weight(1f),
                    title = stringResource(Res.string.text_type),
                    icon = Icons.AutoMirrored.Filled.Article,
                    isSelected = state.postType == PostCreationContract.PostType.TEXT,
                    onClick = {
                        viewModel.handleEvent(
                            PostCreationContract.Event.OnPostTypeChanged(
                                PostCreationContract.PostType.TEXT
                            )
                        )
                    }
                )
                PostTypeTab(
                    modifier = Modifier.weight(1f),
                    title = stringResource(Res.string.link_type),
                    icon = Icons.Default.Link,
                    isSelected = state.postType == PostCreationContract.PostType.LINK,
                    onClick = {
                        viewModel.handleEvent(
                            PostCreationContract.Event.OnPostTypeChanged(
                                PostCreationContract.PostType.LINK
                            )
                        )
                    }
                )
                PostTypeTab(
                    modifier = Modifier.weight(1f),
                    title = stringResource(Res.string.image_type),
                    icon = Icons.Default.Image,
                    isSelected = state.postType == PostCreationContract.PostType.IMAGE,
                    onClick = {
                        viewModel.handleEvent(
                            PostCreationContract.Event.OnPostTypeChanged(
                                PostCreationContract.PostType.IMAGE
                            )
                        )
                    }
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = 28.dp,
                    topEnd = 28.dp,
                    bottomStart = 8.dp,
                    bottomEnd = 8.dp
                ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(Res.string.title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextField(
                        value = state.title,
                        onValueChange = {
                            viewModel.handleEvent(
                                PostCreationContract.Event.OnTitleChanged(
                                    it
                                )
                            )
                        },
                        placeholder = {
                            Text(
                                stringResource(Res.string.title_placeholder),
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = 8.dp,
                    topEnd = 8.dp,
                    bottomStart = 28.dp,
                    bottomEnd = 28.dp
                ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                AnimatedContent(
                    targetState = state.postType,
                    label = "post_content_animation",
                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                ) { type ->
                    Box(modifier = Modifier.padding(16.dp)) {
                        when (type) {
                            PostCreationContract.PostType.TEXT -> {
                                TextField(
                                    value = state.content,
                                    onValueChange = {
                                        viewModel.handleEvent(
                                            PostCreationContract.Event.OnContentChanged(
                                                it
                                            )
                                        )
                                    },
                                    placeholder = { Text(stringResource(Res.string.body_placeholder)) },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    )
                                )
                            }
                            PostCreationContract.PostType.LINK -> {
                                TextField(
                                    value = state.content,
                                    onValueChange = {
                                        viewModel.handleEvent(
                                            PostCreationContract.Event.OnContentChanged(
                                                it
                                            )
                                        )
                                    },
                                    placeholder = { Text(stringResource(Res.string.link_placeholder)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Link, null) },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    singleLine = true
                                )
                            }
                            PostCreationContract.PostType.IMAGE -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .clickable { pickerLauncher.launch() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (state.mediaBytes != null) {
                                        AsyncImage(
                                            model = state.mediaBytes,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Surface(
                                            modifier = Modifier.align(Alignment.TopEnd)
                                                .padding(8.dp),
                                            shape = CircleShape,
                                            color = Color.Black.copy(alpha = 0.5f)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.handleEvent(
                                                        PostCreationContract.Event.OnMediaSelected(
                                                            null,
                                                            null
                                                        )
                                                    )
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Default.AddPhotoAlternate,
                                                null,
                                                modifier = Modifier.size(48.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                stringResource(Res.string.add_image),
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = state.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, null)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            state.error ?: stringResource(Res.string.unknown_error),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
