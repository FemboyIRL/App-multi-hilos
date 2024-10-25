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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import models.User;

/**
 *
 * @author luisr
 */
public class UserRepository {

    // Método para registrar un nuevo usuario en la base de datos
    public static void registerUser(User user) {
        String sql = "INSERT INTO usuarios(nombre, password, last_connection) VALUES(?, ?, ?)";

        try (Connection conn = SQLiteConnectionMethods.connectToDatabase();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getPassword());

            OffsetDateTime utcLastConnection = user.getLastConnection().withOffsetSameInstant(ZoneOffset.UTC);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX");
            String lastConnectionStr = utcLastConnection.format(formatter);
            pstmt.setString(3, lastConnectionStr);

            pstmt.executeUpdate();
            System.out.println("Usuario registrado exitosamente: " + user.getName());

        } catch (SQLException e) {
            System.out.println("Error al registrar usuario: " + e.getMessage());
        }
    }

    // Método para obtener un usuario de la base de datos usando su nombre
    public static User getUserFromDB(String name) {
        String sql = "SELECT * FROM usuarios WHERE nombre = ?";
        User user = null;

        try (Connection conn = SQLiteConnectionMethods.connectToDatabase();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("id");
                String password = rs.getString("password");
                String lastConnectionStr = rs.getString("last_connection");
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX");
                OffsetDateTime lastConnection = OffsetDateTime.parse(lastConnectionStr, formatter);
                long totalConnectedTime = rs.getLong("total_connected_time");
                user = new User(id, name, password, lastConnection, totalConnectedTime);

            } else {
                System.out.println("Usuario no encontrado.");
            }
            SQLiteConnectionMethods.closeConnection(conn);
        } catch (SQLException e) {
            System.out.println("Error al obtener usuario: " + e.getMessage());
        }
        return user;
    }

    public static boolean updateUserInDB(User user) {
        String sql = "UPDATE usuarios SET nombre = ?, password = ?, last_connection = ?, total_connected_time = ? WHERE id = ?";

        try (Connection conn = SQLiteConnectionMethods.connectToDatabase();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getPassword());

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX");
            OffsetDateTime lastConnection = user.getLastConnection();
            String lastConnectionStr = lastConnection.format(formatter);
            pstmt.setString(3, lastConnectionStr);

            pstmt.setLong(4, user.getTotalConnectedTime());
            pstmt.setInt(5, user.getId());

            int affectedRows = pstmt.executeUpdate();
            SQLiteConnectionMethods.closeConnection(conn);

            return affectedRows > 0;

        } catch (SQLException e) {
            System.out.println("Error al actualizar usuario: " + e.getMessage());
            return false;
        }
    }

}
