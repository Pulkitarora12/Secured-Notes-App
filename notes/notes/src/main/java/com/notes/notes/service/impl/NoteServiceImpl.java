package com.notes.notes.service.impl;

import com.notes.notes.entity.Note;
import com.notes.notes.repository.NoteRepository;
import com.notes.notes.service.NoteService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoteServiceImpl implements NoteService {

    private final NoteRepository noteRepository;

    public NoteServiceImpl(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    @Override
    public Note createNoteForUser(String content, String username) {
        Note note = new Note();
        note.setContent(content);
        note.setOwnerName(username);
        return noteRepository.save(note);
    }

    @Override
    public Note updateNoteForUser(Long id, String content) {
        Note note = noteRepository.findById(id).orElseThrow(() -> new RuntimeException("Note not found"));
        note.setContent(content);
        return noteRepository.save(note);
    }

    @Override
    public void deleteNoteForUser(Long id, String userName) {
        Note note = noteRepository.findById(id).orElseThrow(() -> new RuntimeException("Note not found"));
        noteRepository.delete(note);
    }

    @Override
    public List<Note> getNotesForUser(String username) {
        return noteRepository.findByOwnerName(username);
    }
}
