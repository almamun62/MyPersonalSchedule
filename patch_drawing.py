import re

with open('app/src/main/java/com/example/ui/components/DrawingCanvasDialog.kt', 'r') as f:
    content = f.read()

content = content.replace('@Composable\nfun DrawingCanvasDialog', '@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun DrawingCanvasDialog')

with open('app/src/main/java/com/example/ui/components/DrawingCanvasDialog.kt', 'w') as f:
    f.write(content)
