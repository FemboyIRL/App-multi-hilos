package servidormulti;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import methods.NoteRepository;
import methods.UserRepository;
import models.Note;
import models.User;

class UnCliente implements Runnable {

    final DataOutputStream salida;
    final DataInputStream entrada;
    boolean running = true;
    User user;

    UnCliente(Socket s) throws IOException {
        salida = new DataOutputStream(s.getOutputStream());
        entrada = new DataInputStream(s.getInputStream());
    }

    @Override
    public void run() {
        while (running) {
            try {
                salida.writeUTF("Por favor, registre un nombre de usuario:");
                String nombre = entrada.readUTF();
                validateUser(nombre);
                listenForMessages();
            } catch (IOException e) {
                System.out.println("Error en el hilo: " + e.getMessage());
                e.printStackTrace();
                cerrarCliente();
            }
        }
        System.out.println("Hilo del cliente detenido.");
    }

    private void validateUser(String nombre) throws IOException {

        User userFromDB = UserRepository.getUserFromDB(nombre);

        if (userFromDB == null) {
            registerClient(nombre);
            return;
        }

        for (UnCliente clienteConectado : ServidorMulti.usuariosConectados) {
            if (clienteConectado.user.getName().equals(nombre)) {
                salida.writeUTF("El usuario " + nombre + " ya está conectado.");
                run();
            }
        }

        loginToServer(userFromDB);

    }

    private void loginToServer(User user) throws IOException {
        salida.writeUTF("Ingrese la contraseña para el usuario " + user.getName());
        String password = entrada.readUTF();

        if (password.equals(user.getPassword())) {
            salida.writeUTF("Inicio de sesión exitoso. Bienvenido, " + user.getName() + "!");
            this.user = user;
            synchronized (ServidorMulti.usuariosConectados) {
                ServidorMulti.usuariosConectados.add(this);
                notifyOtherUsers(this.user);
                showUserNotes(this.user.getId());
            }
        } else {
            salida.writeUTF("Contraseña incorrecta");
            run();
        }
    }

    private void registerClient(String nombre) throws IOException {
        salida.writeUTF("Por favor, registre una contraseña:");
        String password = entrada.readUTF();

        this.user = new User(0, nombre, password);

        UserRepository.registerUser(user);

        synchronized (ServidorMulti.usuariosConectados) {
            ServidorMulti.usuariosConectados.add(this);
            notifyOtherUsers(this.user);
        }

        salida.writeUTF("Registro exitoso. Bienvenido, " + nombre + "!");
    }

    private void cerrarCliente() {
        try {
            notificarDesconexion(this.user);

            if (entrada != null) {
                entrada.close();
            }
            if (salida != null) {
                salida.close();
            }

            synchronized (ServidorMulti.usuariosConectados) {
                ServidorMulti.usuariosConectados.remove(this);
            }

            running = false;

            System.out.println("Cliente " + user.getName() + " desconectado correctamente.");

        } catch (IOException e) {
            System.err.println("Error al cerrar el cliente " + user.getName() + ": " + e.getMessage());
        }
    }

    private void sendMessageToUser(User usuarioRecibido, String nombre, String mensaje) throws IOException {
        UnCliente clienteEncontrado = null;
        usuarioRecibido = UserRepository.getUserFromDB(nombre);
        if (usuarioRecibido == null) {
            salida.writeUTF("El usuario " + nombre + " no existe");
            return;
        }
        for (Map.Entry<Integer, UnCliente> entry : ServidorMulti.clientes.entrySet()) {
            UnCliente cliente = entry.getValue();
            synchronized (cliente) {
                if (cliente.user.getName().equals(usuarioRecibido.getName())) {
                    clienteEncontrado = cliente;
                    System.out.println(clienteEncontrado.user.getName());
                    System.out.println("Cliente encontrado en la posición: " + entry.getKey());
                    break;
                }
            }
        }
        if (clienteEncontrado == null) {
            salida.writeUTF("El usuario " + usuarioRecibido.getName() + " no esta conectado");
            return;
        }
        clienteEncontrado.salida.writeUTF("|" + user.getName() + "| te ha susurrado: " + mensaje);
        salida.writeUTF("Mensaje enviado exitosamente a " + clienteEncontrado.user.getName());
    }

    private void notifyOtherUsers(User newUser) throws IOException {
        for (UnCliente clienteConectado : ServidorMulti.usuariosConectados) {
            if (!clienteConectado.user.getName().equals(newUser.getName())) {
                clienteConectado.salida.writeUTF("El usuario " + newUser.getName() + " se ha conectado.");
            }
        }
    }

    private void notificarDesconexion(User newUser) throws IOException {
        for (UnCliente clienteConectado : ServidorMulti.usuariosConectados) {
            if (!clienteConectado.user.getName().equals(newUser.getName())) {
                clienteConectado.salida.writeUTF("El usuario " + newUser.getName() + " se ha desconectado.");
            }
        }
    }

    private void showHelpMessage() throws IOException {
        StringBuilder helpMessage = new StringBuilder();

        helpMessage.append("=== Mensaje de Ayuda ===\n");
        helpMessage.append("A continuación se muestran los comandos disponibles:\n");
        helpMessage.append("1. /msg [-v] <nombre_usuario | -v <usuario1,usuario2,...>> - Envía un mensaje a un usuario o varios usuarios.\n");
        helpMessage.append("2. /note - Deja una nota para que otros usuarios la vean.\n");
        helpMessage.append("3. /exit - Cierra la conexión del cliente.\n");
        helpMessage.append("4. /help - Muestra este mensaje de ayuda.\n");
        helpMessage.append("=========================\n");

        salida.writeUTF(helpMessage.toString());
    }

    private void handleNoteCommand(String noteDescription) throws IOException {
        if (noteDescription == null || noteDescription.trim().isEmpty()) {
            salida.writeUTF("La nota no puede estar vacía.");
            return;
        }

        String timestamp = java.time.LocalDateTime.now().toString();
        Note note = new Note(0, noteDescription, timestamp);
        NoteRepository.registerNote(note, this.user.getId());

        salida.writeUTF("Nota guardada: " + noteDescription + ", Fecha: " + timestamp);
    }

    private void showUserNotes(int userId) throws IOException {
        List<Note> notes = NoteRepository.getNotesFromDB(userId);
        StringBuilder messageBuilder = new StringBuilder();

        if (notes.isEmpty()) {
            messageBuilder.append("No tienes notas guardadas.");
        } else {
            messageBuilder.append("Tus notas:\n");
            for (Note note : notes) {
                messageBuilder.append("Nota ID: ").append(note.getNoteId())
                        .append(", Descripción: ").append(note.getMessage())
                        .append(", Fecha: ").append(note.getTimestamp())
                        .append("\n");
            }
        }

        salida.writeUTF(messageBuilder.toString());
    }

    private void listenForMessages() {
        String mensaje;
        while (true) {
            try {
                mensaje = entrada.readUTF();
                if (mensaje.startsWith("/")) {
                    String[] partes = mensaje.split(" ");
                    String comando = partes[0];
                    switch (comando) {
                        case "/msg":
                            salida.writeUTF("Ingrese el mensaje a enviar");
                            String mensajeEnviar = entrada.readUTF();
                            User usuarioRecibido = null;
                            if (partes[1].equals("-v")) {
                                String usuariosRecibiendo = partes[2];
                                String[] usuariosRecibiendoArray = usuariosRecibiendo.split(",");
                                for (String usuarioRecibiendoNombre : usuariosRecibiendoArray) {
                                    User usuarioRecibiendo = UserRepository.getUserFromDB(usuarioRecibiendoNombre);
                                    for (Map.Entry<Integer, UnCliente> entry : ServidorMulti.clientes.entrySet()) {
                                        UnCliente cliente = entry.getValue();
                                        synchronized (cliente) {
                                            if (cliente.user.getName().equals(usuarioRecibiendo.getName())) {
                                                System.out.println("Cliente encontrado en la posición: " + entry.getKey());
                                                sendMessageToUser(usuarioRecibido, usuarioRecibiendo.getName(), mensajeEnviar);
                                            }
                                        }
                                    }
                                }
                                break;
                            }
                            sendMessageToUser(usuarioRecibido, partes[1], mensajeEnviar);
                            break;
                        case "/help":
                            showHelpMessage();
                            break;
                        case "/exit":
                            this.salida.writeUTF("Cerrando conexion...");
                            cerrarCliente();
                            break;
                        case "/note":
                            this.salida.writeUTF("Ingrese la nota");
                            String noteMessage = this.entrada.readUTF();
                            handleNoteCommand(noteMessage);
                            break;
                        default:
                            this.salida.writeUTF("Comando no encontrado /ayuda para la lista de comandos");
                            break;
                    }
                } else {
                    for (UnCliente cliente : ServidorMulti.clientes.values()) {
                        cliente.salida.writeUTF("|" + user.getName() + "|:" + mensaje);
                    }
                }
            } catch (IOException ex) {
                System.out.println("Error al leer el mensaje: " + ex.getMessage());
                break;
            }
        }
    }
}
