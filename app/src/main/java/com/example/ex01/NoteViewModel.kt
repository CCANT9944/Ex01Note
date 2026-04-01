package com.example.ex01

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class NoteViewModel(private val dao: NoteDao) : ViewModel() {
    val folders = dao.getAllFolders()
    val rootFolders = dao.getRootFolders()
    val unassignedNotes = dao.getUnfolderedNotes()

    fun getNotesByFolder(folderId: Int) = dao.getNotesByFolder(folderId)
    fun getFoldersByParent(folderId: Int) = dao.getFoldersByParent(folderId)
    fun getNote(id: Int): Flow<Note?> = dao.getNoteById(id)
    fun getItems(noteId: Int): Flow<List<NoteItem>> = dao.getItemsForNote(noteId)

    fun addFolder(name: String, parentFolderId: Int? = null) {
        viewModelScope.launch { dao.insertFolder(Folder(name = name, parentFolderId = parentFolderId)) }
    }

    fun addNote(
        title: String,
        folderId: Int? = null,
        kind: String = NoteKinds.CHECKLIST,
        listStyle: String = NoteListStyles.CHECKLIST
    ) {
        viewModelScope.launch {
            dao.insertNote(Note(title = title, folderId = folderId, kind = kind, listStyle = listStyle))
        }
    }

    fun moveNoteToFolder(noteId: Int, folderId: Int?) {
        viewModelScope.launch {
            val note = dao.getNoteById(noteId).firstOrNull()
            note?.let {
                dao.updateNote(it.copy(folderId = folderId))
            }
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            deleteFolderRecursively(folder.id)
            dao.deleteNotesByFolder(folder.id)
            dao.deleteFolder(folder)
        }
    }

    private suspend fun deleteFolderRecursively(folderId: Int) {
        dao.getFoldersByParentOnce(folderId).forEach { child ->
            deleteFolderRecursively(child.id)
            dao.deleteNotesByFolder(child.id)
            dao.deleteFolder(child)
        }
    }

    fun updateFolder(folder: Folder) {
        viewModelScope.launch { dao.updateFolder(folder) }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch { dao.updateNote(note) }
    }

    fun updateNoteListStyle(note: Note, listStyle: String) {
        viewModelScope.launch {
            val currentItems = dao.getItemsForNote(note.id).firstOrNull().orEmpty()

            if (note.kind == NoteKinds.FREE_TEXT) {
                val isLeavingPlainBodyMode = note.listStyle == NoteListStyles.CHECKLIST && listStyle != NoteListStyles.CHECKLIST
                val isReturningToPlainBodyMode = note.listStyle != NoteListStyles.CHECKLIST && listStyle == NoteListStyles.CHECKLIST

                if (isLeavingPlainBodyMode && currentItems.isEmpty()) {
                    val bodyLines = note.body.lineSequence()
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .toList()

                    val sourceTexts = if (bodyLines.isNotEmpty()) bodyLines else listOf(note.body.trim()).filter { it.isNotBlank() }
                    sourceTexts.forEach { text ->
                        dao.insertItem(NoteItem(noteId = note.id, text = text))
                    }
                }

                if (isReturningToPlainBodyMode) {
                    val restoredBody = currentItems.joinToString("\n") { it.text }
                    dao.updateNote(note.copy(listStyle = listStyle, body = restoredBody))
                    return@launch
                }
            }

            dao.updateNote(note.copy(listStyle = listStyle))
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { dao.deleteNote(note) }
    }

    fun updateItem(item: NoteItem) {
        viewModelScope.launch { dao.updateItem(item) }
    }

    fun deleteItem(item: NoteItem) {
        viewModelScope.launch { dao.deleteItem(item) }
    }

    fun addItem(noteId: Int, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            dao.insertItem(NoteItem(noteId = noteId, text = text))
        }
    }
}

class NoteViewModelFactory(private val dao: NoteDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

