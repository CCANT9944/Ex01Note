package com.example.ex01

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

object NoteKinds {
    const val CHECKLIST = "CHECKLIST"
    const val FREE_TEXT = "FREE_TEXT"
    const val SNOTE = "SNOTE"
}

object NoteListStyles {
    const val CHECKLIST = "CHECKLIST"
    const val BULLETED = "BULLETED"
    const val NUMBERED = "NUMBERED"
}

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val parentFolderId: Int? = null,
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "notes",
    indices = [Index(value = ["folderId"])],
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val folderId: Int? = null,
    val kind: String = NoteKinds.CHECKLIST,
    val listStyle: String = NoteListStyles.CHECKLIST,
    val body: String = "",
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "note_items",
    indices = [Index(value = ["noteId"])],
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class NoteItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val noteId: Int,
    val text: String,
    val isChecked: Boolean = false
)

@Dao
interface NoteDao {
    @Query("SELECT * FROM folders WHERE isDeleted = 0")
    fun getAllFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE parentFolderId IS NULL AND isDeleted = 0")
    fun getRootFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE parentFolderId = :parentFolderId AND isDeleted = 0")
    fun getFoldersByParent(parentFolderId: Int): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE parentFolderId = :parentFolderId AND isDeleted = 0")
    suspend fun getFoldersByParentOnce(parentFolderId: Int): List<Folder>

    @Query("SELECT * FROM folders WHERE isDeleted = 1")
    fun getDeletedFolders(): Flow<List<Folder>>

    @Insert
    suspend fun insertFolder(folder: Folder)

    @Update
    suspend fun updateFolder(folder: Folder)

    @Delete
    suspend fun deleteFolder(folder: Folder)

    @Query("SELECT * FROM notes WHERE isDeleted = 0")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE folderId = :folderId AND isDeleted = 0")
    fun getNotesByFolder(folderId: Int): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE folderId IS NULL AND isDeleted = 0")
    fun getUnfolderedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1")
    fun getDeletedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteById(id: Int): Flow<Note?>

    @Insert
    suspend fun insertNote(note: Note): Long

    @Query("DELETE FROM notes WHERE folderId = :folderId")
    suspend fun deleteNotesByFolder(folderId: Int)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM note_items WHERE noteId = :noteId ORDER BY id ASC")
    fun getItemsForNote(noteId: Int): Flow<List<NoteItem>>

    @Insert
    suspend fun insertItem(item: NoteItem)

    @Update
    suspend fun updateItem(item: NoteItem)

    @Delete
    suspend fun deleteItem(item: NoteItem)
}

@Database(entities = [Folder::class, Note::class, NoteItem::class], version = 8, exportSchema = false)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_folderId` ON `notes` (`folderId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_items_noteId` ON `note_items` (`noteId`)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `kind` TEXT NOT NULL DEFAULT 'CHECKLIST'")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `body` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `folders` ADD COLUMN `parentFolderId` INTEGER")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `listStyle` TEXT NOT NULL DEFAULT 'CHECKLIST'")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `folders` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var INSTANCE: NoteDatabase? = null

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "note_database"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                .addMigrations(MIGRATION_5_6)
                .addMigrations(MIGRATION_6_7)
                .addMigrations(MIGRATION_7_8)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
