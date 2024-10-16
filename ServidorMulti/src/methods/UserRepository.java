/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package methods;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import models.User;

/**
 *
 * @author luisr
 */
public class UserRepository {

    // Método para registrar un nuevo usuario en la base de datos
    public static void registerUser(User user) {
        String sql = "INSERT INTO usuarios(nombre, password) VALUES(?, ?)";

        try (Connection conn = SQLiteConnectionMethods.connectToDatabase();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getPassword());
            pstmt.executeUpdate();
            System.out.println("Usuario registrado exitosamente: " + user.getName());
            SQLiteConnectionMethods.closeConnection(conn);
        } catch (SQLException e) {
            System.out.println("Error al registrar usuario: " + e.getMessage());
        }
    }

    // Método para obtener un usuario de la base de datos usando su nombre
    public static User getUserFromDB(String name) {
        String sql = "SELECT id, nombre, password FROM usuarios WHERE nombre = ?";
        User user = null;

        try (Connection conn = SQLiteConnectionMethods.connectToDatabase();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("id");
                String password = rs.getString("password");
                user = new User(id, name, password);

            } else {
                System.out.println("Usuario no encontrado.");
            }
            SQLiteConnectionMethods.closeConnection(conn);
        } catch (SQLException e) {
            System.out.println("Error al obtener usuario: " + e.getMessage());
        }

        return user;
    }
}
