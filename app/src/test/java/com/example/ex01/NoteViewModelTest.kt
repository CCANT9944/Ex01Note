package com.example.ex01

import kotlinx.coroutines.flow.first
// ...existing code...
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

// A very small in-memory fake DAO for unit testing NoteViewModel.
class FakeNoteDao : NoteDao {
    private val foldersList = mutableListOf<Folder>()
    private val notesList = mutableListOf<Note>()
    private val itemsList = mutableListOf<NoteItem>()

    override fun getAllFolders() = kotlinx.coroutines.flow.flow { emit(foldersList.toList()) }
    override fun getRootFolders() = kotlinx.coroutines.flow.flow { emit(foldersList.filter { it.parentFolderId == null }) }
    override fun getFoldersByParent(parentFolderId: Int) = kotlinx.coroutines.flow.flow { emit(foldersList.filter { it.parentFolderId == parentFolderId }) }
    override suspend fun getFoldersByParentOnce(parentFolderId: Int) = foldersList.filter { it.parentFolderId == parentFolderId }
    override suspend fun insertFolder(folder: Folder) { foldersList.add(folder.copy(id = (foldersList.size + 1))) }
    override suspend fun updateFolder(folder: Folder) {
        val idx = foldersList.indexOfFirst { it.id == folder.id }
        if (idx >= 0) foldersList[idx] = folder
    }
    override suspend fun deleteFolder(folder: Folder) { foldersList.removeAll { it.id == folder.id } }

    override fun getAllNotes() = kotlinx.coroutines.flow.flow { emit(notesList.toList()) }
    override fun getNotesByFolder(folderId: Int) = kotlinx.coroutines.flow.flow { emit(notesList.filter { it.folderId == folderId }) }
    override fun getUnfolderedNotes() = kotlinx.coroutines.flow.flow { emit(notesList.filter { it.folderId == null }) }
    override fun getNoteById(id: Int) = kotlinx.coroutines.flow.flow { emit(notesList.firstOrNull { it.id == id }) }
    override suspend fun insertNote(note: Note): Long {
        val newId = notesList.size + 1
        notesList.add(note.copy(id = newId))
        return newId.toLong()
    }
    override suspend fun deleteNotesByFolder(folderId: Int) { notesList.removeAll { it.folderId == folderId } }
    override suspend fun updateNote(note: Note) {
        val idx = notesList.indexOfFirst { it.id == note.id }
        if (idx >= 0) notesList[idx] = note
    }
    override suspend fun deleteNote(note: Note) { notesList.removeAll { it.id == note.id } }

    override fun getItemsForNote(noteId: Int) = kotlinx.coroutines.flow.flow { emit(itemsList.filter { it.noteId == noteId }) }
    override suspend fun insertItem(item: NoteItem) { itemsList.add(item.copy(id = (itemsList.size + 1))) }
    override suspend fun updateItem(item: NoteItem) {
        val idx = itemsList.indexOfFirst { it.id == item.id }
        if (idx >= 0) itemsList[idx] = item
    }
    override suspend fun deleteItem(item: NoteItem) { itemsList.removeAll { it.id == item.id } }
}

class NoteViewModelTest {
    private lateinit var dao: FakeNoteDao
    private lateinit var vm: NoteViewModel

    @Before
    fun setup() {
        dao = FakeNoteDao()
        vm = NoteViewModel(dao)
    }

    @Test
    fun dao_insertFolder_and_getAllFolders() = runBlocking {
        dao.insertFolder(Folder(name = "Inbox"))
        val folders = dao.getAllFolders().first()
        assertEquals(1, folders.size)
        assertEquals("Inbox", folders[0].name)
    }

    @Test
    fun root_and_child_folders_are_filtered() = runBlocking {
        dao.insertFolder(Folder(name = "Root"))
        dao.insertFolder(Folder(name = "Child", parentFolderId = 1))

        val roots = dao.getRootFolders().first()
        val children = dao.getFoldersByParent(1).first()

        assertEquals(1, roots.size)
        assertEquals("Root", roots[0].name)
        assertEquals(1, children.size)
        assertEquals("Child", children[0].name)
    }

    @Test
    fun viewModel_reflects_dao_changes() = runBlocking {
        // Insert a folder and a note via DAO, then observe via ViewModel flows
        dao.insertFolder(Folder(name = "Work"))
        val folderId = dao.getAllFolders().first().firstOrNull()?.id ?: 1

        dao.insertNote(Note(title = "Task 1"))

        val unassigned = vm.unassignedNotes.first()
        assertEquals(1, unassigned.size)
        assertEquals("Task 1", unassigned[0].title)

        // Move note by updating it in DAO and verify ViewModel flow for foldered notes
        val noteId = dao.getAllNotes().first().first().id
        dao.updateNote(Note(id = noteId, title = "Task 1", folderId = folderId))

        val notesInFolder = vm.getNotesByFolder(folderId).first()
        assertEquals(1, notesInFolder.size)
        assertEquals("Task 1", notesInFolder[0].title)
    }
}

