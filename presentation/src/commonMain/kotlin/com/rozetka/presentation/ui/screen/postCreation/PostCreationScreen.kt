package com.rozetka.presentation.ui.screen.postCreation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rozetka.presentation.mvi.PostCreationContract

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCreationScreen(
    viewModel: PostCreationViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Post") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        IconButton(onClick = { viewModel.handleEvent(PostCreationContract.Event.OnSubmitClicked) }) {
                            Icon(Icons.Default.Check, contentDescription = "Submit")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Subreddit Selection (Simplified as a text field for now)
            OutlinedTextField(
                value = state.selectedSubreddit,
                onValueChange = { viewModel.handleEvent(PostCreationContract.Event.OnSubredditSelected(it)) },
                label = { Text("Subreddit") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. androiddev") }
            )

            // Post Type Toggle
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                PostCreationContract.PostType.values().forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = state.postType == type,
                        onClick = { viewModel.handleEvent(PostCreationContract.Event.OnPostTypeChanged(type)) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = PostCreationContract.PostType.values().size)
                    ) {
                        Text(type.name)
                    }
                }
            }

            // Title
            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.handleEvent(PostCreationContract.Event.OnTitleChanged(it)) },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            // Content (Text, Link, or Image selection)
            when (state.postType) {
                PostCreationContract.PostType.TEXT -> {
                    OutlinedTextField(
                        value = state.content,
                        onValueChange = { viewModel.handleEvent(PostCreationContract.Event.OnContentChanged(it)) },
                        label = { Text("Text (Optional)") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                        minLines = 5
                    )
                }
                PostCreationContract.PostType.LINK -> {
                    OutlinedTextField(
                        value = state.content,
                        onValueChange = { viewModel.handleEvent(PostCreationContract.Event.OnContentChanged(it)) },
                        label = { Text("URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                PostCreationContract.PostType.IMAGE -> {
                    Button(
                        onClick = { /* In a real app, launch image picker and call OnMediaSelected */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.mediaUri == null) "Select Image" else "Image Selected")
                    }
                    state.mediaUri?.let {
                        Text("Selected: $it", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
