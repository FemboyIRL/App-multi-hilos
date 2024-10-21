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
        helpMessage.append("2. /note - Deja una nota para que usted usuarios la vean. [-o] - manda la nota a otro usuario | [-s] - muestra mis notas y las notas enviadas | [-d] - borra una nota \n");
        helpMessage.append("3. /exit - Cierra la conexión del cliente.\n");
        helpMessage.append("4. /help - Muestra este mensaje de ayuda.\n");
        helpMessage.append("=========================\n");

        salida.writeUTF(helpMessage.toString());
    }

    private void handleDeleteNoteCommand(int noteID) throws IOException {
        List<Note> userNotes = NoteRepository.getNotesFromDB(this.user.getId());
        boolean noteFound = false;

        for (Note note : userNotes) {
            if (note.getNoteId() == noteID) {
                NoteRepository.deleteNoteById(noteID);
                noteFound = true;
                this.salida.writeUTF("Nota con ID " + noteID + " eliminada exitosamente.");
                break;
            }
        }

        if (!noteFound) {
            this.salida.writeUTF("No se encontró ninguna nota con el ID: " + noteID);
        }
    }

    private void handleNoteCommand(String noteDescription) throws IOException {
        if (noteDescription == null || noteDescription.trim().isEmpty()) {
            salida.writeUTF("La nota no puede estar vacía.");
            return;
        }

        String timestamp = java.time.LocalDateTime.now().toString();
        Note note = new Note(0, noteDescription, timestamp, this.user.getName());
        NoteRepository.registerNote(note, this.user.getId());

        salida.writeUTF("Nota guardada: " + noteDescription + ", Fecha: " + timestamp);
    }

    private void showUserNotes(int userId) throws IOException {
        List<Note> notes = NoteRepository.getNotesFromDB(userId);
        StringBuilder messageBuilder = new StringBuilder();

        if (notes.isEmpty()) {
            messageBuilder.append("No tienes notas guardadas.");
        } else {
            messageBuilder.append("Tienes " + notes.size() + " nota(s):\n");
            for (Note note : notes) {
                messageBuilder.append("Nota ID: ").append(note.getNoteId())
                        .append(", Mensaje: ").append(note.getMessage())
                        .append(", Fecha: ").append(note.getTimestamp())
                        .append(", De: ").append(note.getSourceUser())
                        .append("\n");
            }
        }

        salida.writeUTF(messageBuilder.toString());
    }

    private void showUserSentNotes(String name) throws IOException {
        List<Note> notes = NoteRepository.getSourceUserSentNotes(name);
        StringBuilder messageBuilder = new StringBuilder();

        if (notes.isEmpty()) {
            messageBuilder.append("No has enviado notas aun.");
        } else {
            messageBuilder.append("Has enviado " + notes.size() + " nota(s):\n");
            for (Note note : notes) {
                messageBuilder.append("Nota ID: ").append(note.getNoteId())
                        .append(", Mensaje: ").append(note.getMessage())
                        .append(", Fecha: ").append(note.getTimestamp())
                        .append(", De: ").append(note.getSourceUser())
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
                            if (partes.length > 1) {
                                if (partes[1].equals("-d")) {
                                    this.salida.writeUTF("Ingrese el ID de la nota a eliminar");
                                    String noteIDStr = this.entrada.readUTF();
                                    if (noteIDStr.equals("")) {
                                        this.salida.writeUTF("El ID de la nota no puede estar vacío. Inténtelo de nuevo.");
                                    } else {
                                        try {
                                            int noteID = Integer.parseInt(noteIDStr);
                                            handleDeleteNoteCommand(noteID);
                                            break;
                                        } catch (NumberFormatException e) {
                                            this.salida.writeUTF("El ID de la nota debe ser un número válido. Inténtelo de nuevo.");
                                            break;
                                        }
                                    }
                                    break;
                                } else if (partes[1].equals("-s")) {
                                    this.salida.writeUTF("============= MIS NOTAS =====================");
                                    showUserNotes(this.user.getId());
                                    this.salida.writeUTF("============= NOTAS ENVIADAS =====================");
                                    showUserSentNotes(this.user.getName());
                                    break;
                                } else if (partes[1].equals("-o")) {
                                    this.salida.writeUTF("Ingrese el nombre del usuario a dejar la nota");
                                    String userName = this.entrada.readUTF();
                                    User userFromDatabase = UserRepository.getUserFromDB(userName);
                                    if (userFromDatabase == null) {
                                        this.salida.writeUTF("No se encontro el usuario " + userName + " en la base de datos");
                                        break;
                                    }
                                    this.salida.writeUTF("Ingrese el mensaje de la nota para el usuario " + userFromDatabase.getName());
                                    String noteMessage = this.entrada.readUTF();
                                    String timestamp = java.time.LocalDateTime.now().toString();
                                    Note note = new Note(0, noteMessage, timestamp, this.user.getName());
                                    NoteRepository.registerNote(note, userFromDatabase.getId());
                                    this.salida.writeUTF("Nota para el usuario " + userFromDatabase.getName() + " registrada exitosamente");
                                    break;
                                } else if (partes[1].equals("-D")) {
                                    this.salida.writeUTF("Ingrese el ID de la nota a eliminar");
                                }
                            } else {
                                this.salida.writeUTF("Ingrese la nota");
                                String noteMessage = this.entrada.readUTF();
                                handleNoteCommand(noteMessage);
                                break;
                            }

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
