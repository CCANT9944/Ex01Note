package com.example.ex01

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ex01.widget.NotesWidget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import androidx.glance.appwidget.updateAll

class NoteViewModel(application: Application, private val dao: NoteDao) : AndroidViewModel(application) {
    val folders = dao.getAllFolders()
    val rootFolders = dao.getRootFolders()
    val unassignedNotes = dao.getUnfolderedNotes()
    val deletedFolders = dao.getDeletedFolders()
    val deletedNotes = dao.getDeletedNotes()

    fun getNotesByFolder(folderId: Int) = dao.getNotesByFolder(folderId)
    fun getFoldersByParent(folderId: Int) = dao.getFoldersByParent(folderId)
    fun getNote(id: Int): Flow<Note?> = dao.getNoteById(id)
    fun getItems(noteId: Int): Flow<List<NoteItem>> = dao.getItemsForNote(noteId)

    fun addFolder(name: String, parentFolderId: Int? = null) {
        viewModelScope.launch { 
            dao.insertFolder(Folder(name = name, parentFolderId = parentFolderId)) 
            refreshWidget()
        }
    }

    fun addNote(
        title: String,
        folderId: Int? = null,
        kind: String = NoteKinds.CHECKLIST,
        listStyle: String = NoteListStyles.CHECKLIST
    ) {
        viewModelScope.launch {
            dao.insertNote(Note(title = title, folderId = folderId, kind = kind, listStyle = listStyle))
            refreshWidget()
        }
    }

    fun moveNoteToFolder(noteId: Int, folderId: Int?) {
        viewModelScope.launch {
            val note = dao.getNoteById(noteId).firstOrNull()
            note?.let {
                dao.updateNote(it.copy(folderId = folderId))
                refreshWidget()
            }
        }
    }

    private fun refreshWidget() {
        val appContext = getApplication<Application>()
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // Restore a minimal 200ms delay to prevent SQLite from racing against the Widget update flow
            kotlinx.coroutines.delay(200)
            try {
                NotesWidget().updateAll(appContext)
                val widgetManager = android.appwidget.AppWidgetManager.getInstance(appContext)
                val widgetIds = widgetManager.getAppWidgetIds(android.content.ComponentName(appContext, com.example.ex01.widget.NotesWidgetReceiver::class.java))
                if (widgetIds.isNotEmpty()) {
                    val updateIntent = android.content.Intent(appContext, com.example.ex01.widget.NotesWidgetReceiver::class.java).apply {
                        action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                    }
                    appContext.sendBroadcast(updateIntent)
                }
            } catch (e: Exception) {}
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            dao.updateFolder(folder.copy(isDeleted = true))
            refreshWidget()
        }
    }

    fun restoreFolder(folder: Folder) {
        viewModelScope.launch {
            dao.updateFolder(folder.copy(isDeleted = false))
            refreshWidget()
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            dao.updateNote(note.copy(isDeleted = true))
            refreshWidget()
        }
    }

    fun restoreNote(note: Note) {
        viewModelScope.launch {
            dao.updateNote(note.copy(isDeleted = false))
            refreshWidget()
        }
    }

    fun permanentlyDeleteFolder(folder: Folder) {
        viewModelScope.launch {
            deleteFolderRecursively(folder.id)
            dao.deleteNotesByFolder(folder.id)
            dao.deleteFolder(folder)
            refreshWidget()
        }
    }

    private suspend fun deleteFolderRecursively(folderId: Int) {
        dao.getFoldersByParentOnce(folderId).forEach { child ->
            deleteFolderRecursively(child.id)
            dao.deleteNotesByFolder(child.id)
            dao.deleteFolder(child)
        }
    }

    fun triggerWidgetUpdate() {
        refreshWidget()
    }

    fun updateFolder(folder: Folder) {
        viewModelScope.launch { 
            dao.updateFolder(folder) 
            refreshWidget()
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            dao.updateNote(note)
            refreshWidget()
        }
    }

    suspend fun updateNoteSync(note: Note) {
        dao.updateNote(note)
    }

    fun updateNoteListStyle(note: Note, listStyle: String) {
        viewModelScope.launch {
            val currentItems = dao.getItemsForNote(note.id).firstOrNull().orEmpty()

            if (note.kind == NoteKinds.FREE_TEXT) {
                val isLeavingPlainBodyMode = note.listStyle == NoteListStyles.CHECKLIST && listStyle != NoteListStyles.CHECKLIST
                val isReturningToPlainBodyMode = note.listStyle != NoteListStyles.CHECKLIST && listStyle == NoteListStyles.CHECKLIST

                if (isLeavingPlainBodyMode && currentItems.isEmpty()) {
                    val bodyLines = splitNotePages(note.body)
                        .flatMap { pageBody -> pageBody.lineSequence().toList() }
                        .map { it.trim() }
                        .filter { it.isNotBlank() }

                    val sourceTexts = if (bodyLines.isNotEmpty()) bodyLines else listOf(note.body.trim()).filter { it.isNotBlank() }
                    sourceTexts.forEach { text ->
                        dao.insertItem(NoteItem(noteId = note.id, text = text))
                    }
                }

                if (isReturningToPlainBodyMode) {
                    val restoredBody = currentItems.joinToString("\n") { it.text }
                    dao.updateNote(note.copy(listStyle = listStyle, body = restoredBody))
                    refreshWidget()
                    return@launch
                }
            }

            dao.updateNote(note.copy(listStyle = listStyle))
            refreshWidget()
        }
    }

    fun permanentlyDeleteNote(note: Note) {
        viewModelScope.launch {
            dao.deleteNote(note)
            refreshWidget()
        }
    }

    fun updateItem(item: NoteItem) {
        viewModelScope.launch {
            dao.updateItem(item)
            refreshWidget()
        }
    }

    suspend fun updateItemSync(item: NoteItem) {
        dao.updateItem(item)
    }

    fun deleteItem(item: NoteItem) {
        viewModelScope.launch {
            dao.deleteItem(item)
            refreshWidget()
        }
    }

    suspend fun deleteItemSync(item: NoteItem) {
        dao.deleteItem(item)
    }

    fun addItem(noteId: Int, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            dao.insertItem(NoteItem(noteId = noteId, text = text))
            refreshWidget()
        }
    }

    suspend fun addItemSync(noteId: Int, text: String) {
        if (text.isBlank()) return
        dao.insertItem(NoteItem(noteId = noteId, text = text))
    }
}

class NoteViewModelFactory(
    private val application: Application,
    private val dao: NoteDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(application, dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
