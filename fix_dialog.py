import re

with open('app/src/main/java/com/example/ui/components/ClassNotebookDialog.kt', 'r') as f:
    content = f.read()

bad_block = """        contract = ActivityResultContracts.PickVisualMedia(),
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
        }
        }
    )"""

good_block = """        contract = ActivityResultContracts.PickVisualMedia(),
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
        }
    )"""

content = content.replace(bad_block, good_block)

with open('app/src/main/java/com/example/ui/components/ClassNotebookDialog.kt', 'w') as f:
    f.write(content)
