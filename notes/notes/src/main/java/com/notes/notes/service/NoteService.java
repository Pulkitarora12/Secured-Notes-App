package com.notes.notes.service;

import com.notes.notes.entity.Note;

import java.util.List;

public interface NoteService {

    Note createNoteForUser(String content, String username);
    Note updateNoteForUser(Long id, String content);
    void deleteNoteForUser(Long id, String userName);
    List<Note> getNotesForUser(String username);
}
