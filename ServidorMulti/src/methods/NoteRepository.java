/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package methods;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import models.Note;

/**
 *
 * @author michi
 */
public class NoteRepository {

    public static void registerNote(Note note, int userId) {
        String sql = "INSERT INTO notas(user_id, descripcion, timestamp, source_user) VALUES(?, ?, ?, ?)";

        try (Connection conn = SQLiteConnectionMethods.connectToDatabase();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, note.getMessage());
            pstmt.setString(3, note.getTimestamp());
            pstmt.setString(4, note.getSourceUser());
            pstmt.executeUpdate();
            System.out.println("Nota registrada exitosamente para el usuario ID: " + userId);
        } catch (SQLException e) {
            System.out.println("Error al registrar nota: " + e.getMessage());
        }
    }

    public static List<Note> getNotesFromDB(int userId) {
        String sql = "SELECT * FROM notas WHERE user_id = ?";
        List<Note> notes = new ArrayList<>();

        try (Connection conn = SQLiteConnectionMethods.connectToDatabase();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                String description = rs.getString("descripcion");
                String timestamp = rs.getString("timestamp");
                String sourceUser = rs.getString("source_user");
                notes.add(new Note(id, description, timestamp, sourceUser));
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener notas: " + e.getMessage());
        }
        return notes;
    }
    
    public static List<Note> getMyNotesFromDB(int userId, String sourceName) {
        String sql = "SELECT * FROM notas WHERE user_id = ? AND source_user = ?";
        List<Note> notes = new ArrayList<>();

        try (Connection conn = SQLiteConnectionMethods.connectToDatabase();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, sourceName);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                String description = rs.getString("descripcion");
                String timestamp = rs.getString("timestamp");
                String sourceUser = rs.getString("source_user");
                notes.add(new Note(id, description, timestamp, sourceUser));
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener notas: " + e.getMessage());
        }
        return notes;
    }

    public static List<Note> getSourceUserSentNotes(String sourceUser) {
        String sql = "SELECT * FROM notas WHERE source_user = ?";
        List<Note> notes = new ArrayList<>();

        try (Connection conn = SQLiteConnectionMethods.connectToDatabase();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, sourceUser); 
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                String description = rs.getString("descripcion");
                String timestamp = rs.getString("timestamp");
                String sourceUserFromDB = rs.getString("source_user");

                notes.add(new Note(id, description, timestamp, sourceUserFromDB));
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener las notas enviadas por el usuario: " + e.getMessage());
        }

        return notes; // Devolvemos la lista de notas
    }

    public static void deleteNoteById(int noteId) {
        String sql = "DELETE FROM notas WHERE id = ?";

        try (Connection conn = SQLiteConnectionMethods.connectToDatabase();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, noteId);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Nota eliminada exitosamente con ID: " + noteId);
            } else {
                System.out.println("No se encontró ninguna nota con el ID: " + noteId);
            }
        } catch (SQLException e) {
            System.out.println("Error al eliminar la nota: " + e.getMessage());
        }
    }

}
