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
        String sql = "INSERT INTO notas(user_id, descripcion, timestamp) VALUES(?, ?, ?)";

        try (Connection conn = SQLiteConnectionMethods.connectToDatabase();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, note.getMessage());
            pstmt.setString(3, note.getTimestamp()); 
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
                notes.add(new Note(id, description, timestamp));
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener notas: " + e.getMessage());
        }
        return notes;
    }

}
