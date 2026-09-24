import re

with open('app/src/main/java/com/example/data/model/Entities.kt', 'r') as f:
    content = f.read()

target = """    val type: NoteType = NoteType.TEXT,
    val timestampMillis: Long = System.currentTimeMillis()
)"""

replacement = """    val type: NoteType = NoteType.TEXT,
    val tags: String = "", // Comma-separated tags or category
    val timestampMillis: Long = System.currentTimeMillis()
)"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/example/data/model/Entities.kt', 'w') as f:
    f.write(content)
print("Updated NoteEntity")
