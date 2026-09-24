import re

with open('app/src/main/java/com/example/ui/components/ClassNotebookDialog.kt', 'r') as f:
    content = f.read()

# Add a boolean state for drawing dialog
dialog_state_target = """    var editModeNote by remember { mutableStateOf<NoteEntity?>(null) }"""
dialog_state_replacement = """    var editModeNote by remember { mutableStateOf<NoteEntity?>(null) }
    var showDrawingCanvas by remember { mutableStateOf(false) }"""

content = content.replace(dialog_state_target, dialog_state_replacement)

# Add drawing canvas dialog rendering
canvas_rendering_target = """    Dialog(
        onDismissRequest = onDismiss,"""
canvas_rendering_replacement = """    if (showDrawingCanvas) {
        DrawingCanvasDialog(
            onSave = { path ->
                showDrawingCanvas = false
                viewModel.addNote(
                    NoteEntity(
                        courseId = course.id,
                        content = path,
                        type = NoteType.DRAWING
                    )
                )
            },
            onDismiss = { showDrawingCanvas = false }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,"""

content = content.replace(canvas_rendering_target, canvas_rendering_replacement)

# Add drawing icon button in editor bottom bar
bottom_bar_target = """                            FilledTonalIconButton(onClick = {
                                photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Image".tr)
                            }
                        }"""
bottom_bar_replacement = """                            FilledTonalIconButton(onClick = {
                                photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Image".tr)
                            }
                            FilledTonalIconButton(onClick = {
                                showDrawingCanvas = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Draw".tr)
                            }
                        }"""

content = content.replace(bottom_bar_target, bottom_bar_replacement)

# Fix NoteCard to display drawing
card_target = """                NoteType.DRAWING -> {
                    Text("[Handwritten Canvas]", style = MaterialTheme.typography.bodyMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }"""
card_replacement = """                NoteType.DRAWING -> {
                    AsyncImage(
                        model = java.io.File(note.content),
                        contentDescription = "Handwritten Canvas",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White),
                        contentScale = ContentScale.Fit
                    )
                }"""

content = content.replace(card_target, card_replacement)

with open('app/src/main/java/com/example/ui/components/ClassNotebookDialog.kt', 'w') as f:
    f.write(content)
print("Patched ClassNotebookDialog")
