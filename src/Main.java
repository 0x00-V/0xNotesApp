import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

import javax.smartcardio.Card;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().CreateUI());

    }
    private JFrame frame;
    private JPanel cards;

    public void CreateUI(){
        frame = new JFrame("0xNotes - Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        cards = new JPanel(new CardLayout());
        cards.add(new LoginPanel(this), "login");
        cards.add(new DashboardPanel(this), "dashboard");

        frame.add(cards);
        frame.setVisible(true);
        frame.setResizable(false);
        showPanel("login");
    }

    void showPanel(String name){
        CardLayout cl = (CardLayout) cards.getLayout();
        cl.show(cards, name);
    }

    public String cookieHeader = "null";
    class LoginPanel extends JPanel{
        LoginPanel(Main mainApp){
            setLayout(null);
            frame.setSize(400, 250);

            JLabel userLabel = new JLabel("Username:");
            userLabel.setBounds(3, 50, 80, 30);

            JTextField userField = new JTextField();
            userField.setBounds(80, 50, 300, 30);

            JLabel passwordLabel = new JLabel("Password:");
            passwordLabel.setBounds(3, 85, 80, 30);

            JPasswordField passwordField = new JPasswordField();
            passwordField.setBounds(80, 85, 300, 30);

            JButton loginButton = new JButton("Login");
            loginButton.setBounds(50, 130, 100, 30);

            add(userLabel);
            add(userField);
            add(passwordLabel);
            add(passwordField);
            add(loginButton);

            loginButton.addActionListener(e -> {
                try{
                    URL url = new URL("http://127.0.0.1:8000/api/login");
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("POST");
                    con.setDoOutput(true);
                    con.setRequestProperty("Content-Type", "application/json");

                    String json = "{\"username\":\"" + userField.getText() + "\",\"password\":\"" + (new String(passwordField.getPassword())) + "\"}";
                    try (OutputStream os = con.getOutputStream()) {
                        os.write(json.getBytes(StandardCharsets.UTF_8));
                    }

                    int status = con.getResponseCode();
                    System.out.println("Status: " + status);
                    switch (status){
                        case 401:
                            JOptionPane.showMessageDialog(this, "Invalid Credentials");
                            return;
                        case 402:
                            JOptionPane.showMessageDialog(this, "Backend Error");
                            return;
                        default:
                            break;
                    }
                    mainApp.cookieHeader = con.getHeaderField("Set-Cookie");
                    cookieHeader = con.getHeaderField("Set-Cookie");
                    if (cookieHeader != null) {
                        System.out.println("Cookie from server: " + mainApp.cookieHeader);
                    }

                    try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))){
                        String line;
                        StringBuilder response = new StringBuilder();
                        while ((line = in.readLine()) != null){
                            response.append(line);
                        }
                        System.out.println("Response JSON: " + response.toString());
                        userField.setText("");
                        passwordField.setText("");
                        frame.setSize(500,500);
                        frame.setTitle("0xNotes");
                        mainApp.showPanel("dashboard");
                        ((DashboardPanel) mainApp.cards.getComponent(1)).refresh(mainApp);
                    }
                    con.disconnect();
                } catch (Exception err){
                    JOptionPane.showMessageDialog(this, err);
                }
            });
        }
    }

    class Note {
        String title;
        String content;

        Note(String title, String content) {
            this.title = title;
            this.content = content;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    class DashboardPanel extends JPanel {
        private DefaultListModel<Note> listModel;
        private JList<Note> notesList;
        private JTextField titleField;
        private JTextArea contentArea;
        private int currentIndex = -1;

        DashboardPanel(Main mainApp) {
            setLayout(null);

            listModel = new DefaultListModel<>();
            notesList = new JList<>(listModel);
            JScrollPane listScrollPane = new JScrollPane(notesList);
            listScrollPane.setBounds(10, 50, 180, 400);
            add(listScrollPane);


            titleField = new JTextField();
            titleField.setBounds(200, 50, 380, 30);
            add(titleField);


            contentArea = new JTextArea();
            JScrollPane contentScrollPane = new JScrollPane(contentArea);
            contentScrollPane.setBounds(200, 90, 380, 360);
            add(contentScrollPane);


            JButton addBtn = new JButton("Add");
            JButton deleteBtn = new JButton("Delete");
            JButton logoutBtn = new JButton("Logout");

            addBtn.setBounds(10, 10, 80, 30);
            deleteBtn.setBounds(100, 10, 80, 30);
            logoutBtn.setBounds(190, 10, 100, 30);

            add(addBtn);
            add(deleteBtn);
            add(logoutBtn);


            notesList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    saveCurrentNote();
                    currentIndex = notesList.getSelectedIndex();
                    if (currentIndex >= 0) {
                        Note note = listModel.get(currentIndex);
                        titleField.setText(note.title);
                        contentArea.setText(note.content);
                    } else {
                        titleField.setText("");
                        contentArea.setText("");
                    }
                }
            });


            addBtn.addActionListener(e -> {
                String title = JOptionPane.showInputDialog(this, "Note title:");
                if (title != null) {
                    Note newNote = new Note(title, "");
                    listModel.addElement(newNote);
                    notesList.setSelectedIndex(listModel.size() - 1);
                }
            });

            deleteBtn.addActionListener(e -> {
                int idx = notesList.getSelectedIndex();
                if (idx >= 0) {
                    saveCurrentNote();
                    listModel.remove(idx);
                    titleField.setText("");
                    contentArea.setText("");
                    currentIndex = -1;
                }
            });


            logoutBtn.addActionListener(e -> {
                frame.setSize(400, 250);
                mainApp.showPanel("login");
                frame.setTitle("0xNotesApp - Login");
                listModel.clear();
                titleField.setText("");
                contentArea.setText("");

            });

            javax.swing.event.DocumentListener autoSaveListener = new javax.swing.event.DocumentListener() {
                private javax.swing.Timer timer = new javax.swing.Timer(1000, ev -> saveCurrentNote());
                { timer.setRepeats(false); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { restartTimer(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { restartTimer(); }
                public void insertUpdate(javax.swing.event.DocumentEvent e) { restartTimer(); }
                private void restartTimer() {
                    if (currentIndex >= 0) timer.restart();
                }
            };
            titleField.getDocument().addDocumentListener(autoSaveListener);
            contentArea.getDocument().addDocumentListener(autoSaveListener);
        }

        private void saveCurrentNote() {
            if (currentIndex >= 0) {
                Note note = listModel.get(currentIndex);
                note.title = titleField.getText();
                note.content = contentArea.getText();
                listModel.set(currentIndex, note);
                System.out.println("Saved: " + note.title + " | " + note.content);
            }
        }

        public void refresh(Main mainApp) {
            listModel.clear();
            listModel.addElement(new Note("Placeholder 1", "DB functionality will be added soon"));
            listModel.addElement(new Note("Placeholder 2", "DB functionality will be added soon"));
            listModel.addElement(new Note("Placeholder 3", "DB functionality will be added soon"));
        }
    }




}




/*

Example of what I'll use to get responses:

            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))){
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = in.readLine()) != null){
                    response.append(line);
                }
                System.out.println("Response JSON: " + response.toString());
            }
            con.disconnect();

            URL url2 = new URL("http://localhost:3000/dashboard");
            HttpURLConnection con2 = (HttpURLConnection) url2.openConnection();
            con2.setRequestMethod("GET");
            if (cookieHeader != null) {
                con2.setRequestProperty("Cookie", cookieHeader);
            }
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con2.getInputStream()))){
                String line;
                while ((line = in.readLine()) != null){
                    System.out.println(line);
                }
            }
        } catch (Exception e) {
            System.out.println("There was an error:"+e);
        }*/
