package models;

public class Note {
    private int noteId; 
    private String message; 
    private String timestamp; 

    // Constructor
    public Note(int noteId, String message, String timestamp) {
        this.noteId = noteId;
        this.message = message;
        this.timestamp = timestamp;
    }

    // Getter para el ID de la nota
    public int getNoteId() {
        return noteId;
    }

    // Getter para el mensaje
    public String getMessage() {
        return message;
    }

    // Getter para la marca de tiempo
    public String getTimestamp() {
        return timestamp;
    }

    // Método para mostrar el mensaje (opcional)
    @Override
    public String toString() {
        return "Nota ID: " + noteId + ", Mensaje: " + message + ", Fecha: " + timestamp;
    }
}
