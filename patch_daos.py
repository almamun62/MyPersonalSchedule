import re

with open('app/src/main/java/com/example/data/local/Daos.kt', 'r') as f:
    content = f.read()

target = """    @Query("SELECT * FROM notes WHERE courseId = :courseId ORDER BY timestampMillis DESC")
    fun getNotesForCourse(courseId: Long): Flow<List<NoteEntity>>"""

replacement = """    @Query("SELECT * FROM notes WHERE courseId = :courseId ORDER BY timestampMillis DESC")
    fun getNotesForCourse(courseId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY timestampMillis DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/data/local/Daos.kt', 'w') as f:
    f.write(content)
print("Updated Daos.kt")
