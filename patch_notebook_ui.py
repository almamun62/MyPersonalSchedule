import re

with open('app/src/main/java/com/example/ui/components/ClassNotebookDialog.kt', 'r') as f:
    content = f.read()

# 1. Update the photoPickerLauncher to copy the file
picker_replacement = """        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                // Copy to internal storage so it survives app restarts
                val localPath = com.example.util.ImageStorageHelper.copyImageToInternalStorage(context, uri)
                if (localPath != null) {
                    viewModel.addNote(
                        NoteEntity(
                            courseId = course.id,
                            content = localPath,
                            type = NoteType.IMAGE
                        )
                    )
                    Toast.makeText(context, "Image saved!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                }
            }
        }"""
content = re.sub(r'        contract = ActivityResultContracts\.PickVisualMedia\(\),\n        onResult = \{ uri ->[\s\S]*?        \}', picker_replacement, content)

# 2. Remove dead-end affordances (Drawing and Voice)
dead_end_replacement = """                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            FilledTonalIconButton(onClick = { /* Already active text mode */ }) {
                                Icon(Icons.Default.Keyboard, contentDescription = "Type".tr)
                            }
                            FilledTonalIconButton(onClick = {
                                photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Image".tr)
                            }
                        }"""
content = re.sub(r'                        Row\(\n                            modifier = Modifier\.fillMaxWidth\(\),\n                            horizontalArrangement = Arrangement\.SpaceEvenly\n                        \) \{[\s\S]*?                            FilledTonalIconButton\(onClick = \{\n                                viewModel\.addNote\(NoteEntity\(courseId = course\.id, content = "\[🎙️ Voice Memo: 0:45s\]", type = NoteType\.VOICE\)\)\n                                Toast\.makeText\(context, "Voice memo saved!", Toast\.LENGTH_SHORT\)\.show\(\)\n                            \}\) \{\n                                Icon\(Icons\.Default\.Mic, contentDescription = "Voice"\.tr\)\n                            \}\n                        \}', dead_end_replacement, content)

# 3. Update NoteCard to display image using Coil
coil_imports = """import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
"""
content = content.replace('import java.util.*', 'import java.util.*\n' + coil_imports)

image_rendering = """                NoteType.IMAGE -> {
                    AsyncImage(
                        model = java.io.File(note.content),
                        contentDescription = "Attached Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }"""
content = re.sub(r'                NoteType\.IMAGE -> \{\n                    Text\("\[Image Attached\] \$\{note\.content\.take\(20\)\}\.\.\.", style = MaterialTheme\.typography\.bodyMedium, fontStyle = androidx\.compose\.ui\.text\.font\.FontStyle\.Italic\)\n                \}', image_rendering, content)

with open('app/src/main/java/com/example/ui/components/ClassNotebookDialog.kt', 'w') as f:
    f.write(content)
