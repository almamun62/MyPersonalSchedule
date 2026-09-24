with open('app/src/main/java/com/example/ui/theme/Translation.kt', 'r') as f:
    content = f.read()

content = content.replace('${currentConflict.course1.name}', '\\${currentConflict.course1.name}')
content = content.replace('${currentConflict.course2.name}', '\\${currentConflict.course2.name}')
content = content.replace('$ongoingCourseName', '\\$ongoingCourseName')

with open('app/src/main/java/com/example/ui/theme/Translation.kt', 'w') as f:
    f.write(content)
print("Fixed variables in Translation.kt")

with open('app/src/main/java/com/example/ui/components/DrawingCanvasDialog.kt', 'r') as f:
    drawing_content = f.read()

# Replace: val androidPath = androidx.compose.ui.graphics.asAndroidPath(p)
# With: val androidPath = androidx.compose.ui.graphics.asAndroidPath(p) is WRONG, it is p.asAndroidPath()
drawing_content = drawing_content.replace(
    'val androidPath = androidx.compose.ui.graphics.asAndroidPath(p)',
    'val androidPath = p.asAndroidPath()'
)
# Ensure import androidx.compose.ui.graphics.asAndroidPath is present
if 'import androidx.compose.ui.graphics.asAndroidPath' not in drawing_content:
    drawing_content = drawing_content.replace(
        'import androidx.compose.ui.graphics.Path',
        'import androidx.compose.ui.graphics.Path\nimport androidx.compose.ui.graphics.asAndroidPath'
    )

with open('app/src/main/java/com/example/ui/components/DrawingCanvasDialog.kt', 'w') as f:
    f.write(drawing_content)
print("Fixed DrawingCanvasDialog.kt")
