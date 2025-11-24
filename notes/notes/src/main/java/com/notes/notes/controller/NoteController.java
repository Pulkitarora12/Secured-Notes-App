package com.notes.notes.controller;

import com.notes.notes.entity.Note;
import com.notes.notes.service.NoteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping
    public ResponseEntity<Note> saveNote(@RequestBody String content, @AuthenticationPrincipal UserDetails userDetails) {
        String userName = userDetails.getUsername();
        Note savedNote = noteService.createNoteForUser(content, userName);
        return ResponseEntity.ok(savedNote);
    }

    @PutMapping("/{noteId}")
    public ResponseEntity<Note> updateNote(@PathVariable Long noteId, @RequestBody String content,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        List<Note> notes = getAllNotes(userDetails).getBody();
        boolean found = false;
        for (Note note : notes) {
            if (note.getId().equals(noteId)) {
                found = true;
                break;
            }
        }

        if (!found) {
            return ResponseEntity.notFound().build();
        } else {
            Note updatedNote = noteService.updateNoteForUser(noteId, content);
            return ResponseEntity.ok(updatedNote);
        }
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long noteId, @AuthenticationPrincipal UserDetails userDetails) {
        String userName = userDetails.getUsername();
        List<Note> notes = getAllNotes(userDetails).getBody();
        boolean found = false;
        for (Note note : notes) {
            if (note.getId().equals(noteId)) {
                found = true;
                break;
            }
        }
        if (found) {
            noteService.deleteNoteForUser(noteId, userName);
            return ResponseEntity.noContent().build();  // 204 No Content
        }
        return ResponseEntity.notFound().build();  //404
    }

    @GetMapping()
    public ResponseEntity<List<Note>> getAllNotes(@AuthenticationPrincipal UserDetails userDetails) {
        String userName = userDetails.getUsername();
        List<Note> notes = noteService.getNotesForUser(userName);
        return ResponseEntity.ok(notes);
    }

}
