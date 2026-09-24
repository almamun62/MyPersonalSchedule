import re

with open('app/src/main/java/com/example/data/model/Entities.kt', 'r') as f:
    content = f.read()

note_entity = """
enum class NoteType {
    TEXT, DRAWING, IMAGE, VOICE
}

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: Long,
    val content: String, // Text content, or URI for image/voice/drawing data
    val type: NoteType = NoteType.TEXT,
    val timestampMillis: Long = System.currentTimeMillis()
)
"""

if "NoteEntity" not in content:
    with open('app/src/main/java/com/example/data/model/Entities.kt', 'a') as f:
        f.write(note_entity)

with open('app/src/main/java/com/example/data/local/Daos.kt', 'r') as f:
    daos_content = f.read()

note_dao = """
@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE courseId = :courseId ORDER BY timestampMillis DESC")
    fun getNotesForCourse(courseId: Long): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long
    
    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)
    
    @Query("DELETE FROM notes WHERE courseId = :courseId")
    suspend fun deleteNotesByCourse(courseId: Long)
}
"""

if "NoteDao" not in daos_content:
    with open('app/src/main/java/com/example/data/local/Daos.kt', 'a') as f:
        f.write(note_dao)

with open('app/src/main/java/com/example/data/local/AppDatabase.kt', 'r') as f:
    appdb_content = f.read()

# Add NoteType converter
if "fromNoteType" not in appdb_content:
    converters_ext = """
    @TypeConverter
    fun fromNoteType(type: NoteType): String = type.name

    @TypeConverter
    fun toNoteType(value: String): NoteType = try {
        NoteType.valueOf(value)
    } catch (e: Exception) {
        NoteType.TEXT
    }
"""
    appdb_content = re.sub(r'(class Converters \{)', r'\1' + converters_ext, appdb_content)

if "NoteEntity::class" not in appdb_content:
    appdb_content = appdb_content.replace("HolidayOverrideEntity::class", "HolidayOverrideEntity::class,\n        NoteEntity::class")
    appdb_content = appdb_content.replace("version = 1", "version = 2")
    
if "noteDao" not in appdb_content:
    appdb_content = appdb_content.replace("abstract fun holidayOverrideDao(): HolidayOverrideDao", "abstract fun holidayOverrideDao(): HolidayOverrideDao\n    abstract fun noteDao(): NoteDao")

with open('app/src/main/java/com/example/data/local/AppDatabase.kt', 'w') as f:
    f.write(appdb_content)

